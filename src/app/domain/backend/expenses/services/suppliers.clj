(ns app.domain.backend.expenses.services.suppliers
  "Supplier CRUD services using factory pattern."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.related-records :as rr]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.text Normalizer Normalizer$Form]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :supplier))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; NOTE: We intentionally avoid legacy alias vars like `get-supplier`/`create-supplier!`.
;; Admin routes and user handlers resolve operations via the `service` map or
;; explicit overrides in this namespace.

;; NOTE: Suppliers are hard-deleted. Deletion is blocked by FK RESTRICT when
;; expenses exist for the supplier.

;; ============================================================================
;; Normalization (re-exported for external use)
;; ============================================================================

(def normalize-supplier-key configs/normalize-supplier-key)

;; ============================================================================
;; Similarity helpers (Places scoring)
;; ============================================================================

(defn- strip-diacritics
  [value]
  (when value
    (-> value
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d"))))

(defn- join-single-letter-runs
  [tokens]
  (if (seq tokens)
    (let [{:keys [acc run]}
          (reduce
            (fn [{:keys [acc run]} token]
              (if (= 1 (count token))
                {:acc acc
                 :run (conj run token)}
                {:acc (cond-> acc
                        (seq run) (conj (apply str run))
                        true (conj token))
                 :run []}))
            {:acc [] :run []}
            tokens)
          acc (cond-> acc (seq run) (conj (apply str run)))]
      acc)
    []))

(def ^:private legal-suffix-tokens-sim
  ;; Uppercase variants for similarity tokenization.
  #{"DOO" "DD" "AD" "LLC" "LTD" "INC" "GMBH" "AG"})

(defn- canonicalize-company-tokens
  "Drop legal suffix tokens (and anything after them) so OCR strings like
  \"HOŠE-KOMERC d.o.o. Sarajevo\" compare well against Places candidates like
  \"Hoše komerc\"."
  [tokens]
  (if (seq tokens)
    (let [idx (->> tokens
                (map-indexed vector)
                (keep (fn [[i t]] (when (contains? legal-suffix-tokens-sim t) i)))
                first)]
      ;; Only strip when the suffix appears after at least one “real” token.
      (if (and (some? idx) (pos? (long idx)))
        (subvec (vec tokens) 0 (long idx))
        (vec tokens)))
    []))

(defn- normalize-for-similarity-tokens
  [value]
  (when-let [value* (some-> value str str/trim not-empty)]
    (let [normalized (-> value*
                       strip-diacritics
                       str/upper-case
                       (str/replace #"[^A-Z0-9]+" " ")
                       (str/replace #"\s+" " ")
                       str/trim)
          tokens (->> (str/split normalized #"\s+")
                   (remove str/blank?)
                   vec)]
      (-> tokens
        join-single-letter-runs
        canonicalize-company-tokens))))

(defn- normalize-for-similarity
  [value]
  (when-let [tokens (normalize-for-similarity-tokens value)]
    (when (seq tokens)
      (str/join " " tokens))))

(defn- normalize-for-prefix
  [value]
  (some-> value
    normalize-for-similarity
    (str/replace #"\s+" "")))

(defn- normalized-length
  [value]
  (count (str/replace (or value "") #"\s+" "")))

(defn- digits-seq
  [value]
  (let [digits (some->> (re-seq #"\d+" (str value)) (str/join "") not-empty)]
    digits))

(defn- next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                         diagonal
                         (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn- levenshtein-distance
  [a b]
  (let [a* (seq (or a ""))
        b* (seq (or b ""))]
    (cond
      (empty? a*) (count b*)
      (empty? b*) (count a*)
      :else
      (peek
        (reduce
          (fn [previous current]
            (next-row previous current b*))
          (vec (range (inc (count b*))))
          a*)))))

(defn- levenshtein-ratio
  [a b]
  (let [max-len (max (count (or a "")) (count (or b "")))]
    (if (zero? max-len)
      0.0
      (- 1.0 (/ (double (levenshtein-distance a b)) (double max-len))))))

(defn- candidate-accepted?
  [input-len input-prefix candidate-prefix ratio]
  (cond
    (<= input-len 2)
    (and (seq input-prefix)
      (str/starts-with? candidate-prefix input-prefix))

    (<= input-len 4) (>= ratio 0.80)
    (<= input-len 7) (>= ratio 0.70)
    :else (>= ratio 0.60)))

(defn- score-candidate
  [input-norm input-prefix input-len input-digits {:keys [name] :as place}]
  (let [candidate-norm (normalize-for-similarity name)
        candidate-prefix (normalize-for-prefix name)
        candidate-digits (digits-seq name)
        ratio (levenshtein-ratio input-norm candidate-norm)]
    (when (and (seq candidate-norm)
            (or (nil? input-digits)
              (and candidate-digits (str/includes? candidate-digits input-digits)))
            (candidate-accepted? input-len input-prefix candidate-prefix ratio))
      {:place place
       :normalized candidate-norm
       :score (if (<= input-len 2) 1.0 ratio)})))

(defn- best-place-candidate
  [input places]
  (let [input-norm (normalize-for-similarity input)
        input-prefix (normalize-for-prefix input)
        input-len (normalized-length input-norm)
        input-digits (digits-seq input)]
    (when (and (seq input-norm) (seq places))
      (->> places
        (keep (partial score-candidate input-norm input-prefix input-len input-digits))
        (sort-by :score >)
        first))))

;; ==========================================================================
;; Archived supplier-aware operations
;; ==========================================================================

(def ^:private supplier-order-by
  {;; Accept both kebab-case and snake_case query values.
   :display-name :display_name
   :display_name :display_name
   :normalized-key :normalized_key
   :normalized_key :normalized_key
   :address :address
   :created-at :created_at
   :created_at :created_at
   :updated-at :updated_at
   :updated_at :updated_at})

(defn- suppliers-where
  [{:keys [search]}]
  (let [search-filter (when (seq search)
                        [:or
                         [:ilike :display_name (str "%" search "%")]
                         [:ilike :normalized_key (str "%" search "%")]])
        filters (->> [search-filter]
                  (remove nil?))]
    (when (seq filters)
      (into [:and] filters))))

(defn list-suppliers
  "List suppliers.

  Options:
  - :limit/:offset
  - :order-by (keyword)
  - :order-dir (:asc|:desc)
  - :search (string)
    "
  [db {:keys [limit offset order-by order-dir search]
       :or {limit 50 offset 0 order-dir :asc}}]
  (let [order-column (get supplier-order-by order-by :display_name)
        order-direction (if (= :asc order-dir) :asc :desc)
        where (suppliers-where {:search search})]
    (jdbc/execute!
      db
      (sql/format
        (cond-> {:select [:*]
                 :from [:suppliers]
                 :limit limit
                 :offset offset
                 :order-by [[order-column order-direction]]}
          where (assoc :where where)))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-suppliers
  "Count suppliers.

  Accepts the same filtering keys as `list-suppliers` (notably :search)."
  [db {:keys [search]}]
  (let [where (suppliers-where {:search search})]
    (:total
     (jdbc/execute-one!
       db
       (sql/format
         (cond-> {:select [[[:count :*] :total]]
                  :from [:suppliers]}
           where (assoc :where where)))
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn search-suppliers
  "Autocomplete supplier search (2+ chars)."
  [db query {:keys [limit]
             :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [where (suppliers-where {:search query})]
      (jdbc/execute!
        db
        (sql/format
          (cond-> {:select [:*]
                   :from [:suppliers]
                   :order-by [[:display_name :asc]]
                   :limit limit}
            where (assoc :where where)))
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-supplier!
  "Hard delete a supplier. Returns the deleted row or nil."
  [db supplier-id]
  (jdbc/execute-one!
    db
    (sql/format {:delete-from :suppliers
                 :where [:= :id supplier-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn find-by-normalized-key
  "Find supplier by normalized key for deduplication."
  [db normalized-key]
  (when normalized-key
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:suppliers]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(def ^:private legal-suffix-tokens
  ;; Must stay in sync with `service-configs/normalize-supplier-key`.
  #{"doo" "dd" "ad" "llc" "ltd" "inc" "gmbh" "ag"})

(def ^:private descriptor-joiner-tokens
  #{"i" "and" "with"})

(defn- legacy-suffix-like-clauses
  "Return LIKE clauses that match normalized_key containing any legal suffix token.

  We match both an interior token (e.g. %-doo-%) and a terminal token (e.g. %-doo)."
  []
  (->> legal-suffix-tokens
    (mapcat (fn [token]
              [[:like :normalized_key (str "%-" token "-%")]
               [:like :normalized_key (str "%-" token)]]))
    vec))

(defn- find-by-canonical-key-with-legacy-suffix
  "Best-effort lookup for legacy supplier keys that were normalized with legal suffix
  and/or location suffix tokens.

  Example:
  - canonical-key: hose-komerc
  - legacy stored key: hose-komerc-doo-sarajevo

  This function only considers keys that:
  - start with canonical-key + \"-\"
  - and contain a legal suffix token like \"-doo-\".

  Returns one row or nil."
  [db canonical-key]
  (when (and (seq canonical-key)
          (>= (count (str canonical-key)) 5))
    (let [suffix-clauses (legacy-suffix-like-clauses)]
      (when (seq suffix-clauses)
        (jdbc/execute-one!
          db
          (sql/format
            {:select [:*]
             :from [:suppliers]
             :where [:and
                     [:like :normalized_key (str canonical-key "-%")]
                     (into [:or] suffix-clauses)]
             :order-by [[:created_at :asc]]
             :limit 1})
          {:builder-fn rs/as-unqualified-lower-maps})))))

(def ^:private branch-suffix-head-tokens
  #{"pj" "podruznica" "poslovnica" "ogranak" "filijala"})

(def ^:private branch-suffix-display-re
  #"(?iu)\b(?:pj\.?\s*\d*|podružnica|podruznica|poslovnica|ogranak|filijala)\b.*$")

(defn- strip-branch-suffix
  [display-name]
  (when-let [value (some-> display-name str str/trim not-empty)]
    (let [stripped (some-> value
                     (str/replace branch-suffix-display-re "")
                     (str/replace #"[\s,;:\.\-]+$" "")
                     str/trim
                     not-empty)]
      (or stripped value))))

(defn- normalized-key-tokens
  [normalized-key]
  (when-let [key* (some-> normalized-key str str/trim not-empty)]
    (->> (str/split key* #"-")
      (remove str/blank?)
      vec)))

(defn- location-or-branch-suffix?
  [prefix-key candidate-key]
  (let [prefix-tokens (normalized-key-tokens prefix-key)
        candidate-tokens (normalized-key-tokens candidate-key)
        prefix-count (count (or prefix-tokens []))
        suffix-tokens (vec (drop prefix-count (or candidate-tokens [])))]
    (boolean
      (and (seq prefix-tokens)
        (seq suffix-tokens)
        (= prefix-tokens (vec (take prefix-count candidate-tokens)))
        (or
          (contains? branch-suffix-head-tokens (first suffix-tokens))
          (and (>= prefix-count 2)
            (<= (count suffix-tokens) 3)
            (>= (count suffix-tokens) 2)
            (every? #(re-matches #"[a-z0-9]+" %) suffix-tokens)
            (every? #(or (re-matches #"\d+" %) (>= (count %) 3)) suffix-tokens)
            (not-any? legal-suffix-tokens suffix-tokens)))))))

(defn- descriptor-tail-suffix?
  [prefix-key candidate-key]
  (let [prefix-tokens (normalized-key-tokens prefix-key)
        candidate-tokens (normalized-key-tokens candidate-key)
        prefix-count (count (or prefix-tokens []))
        suffix-tokens (vec (drop prefix-count (or candidate-tokens [])))]
    (boolean
      (and (seq prefix-tokens)
        (>= prefix-count 3)
        (>= (count (or prefix-key "")) 16)
        (seq suffix-tokens)
        (= prefix-tokens (vec (take prefix-count candidate-tokens)))
        (contains? descriptor-joiner-tokens (first suffix-tokens))
        (>= (count suffix-tokens) 3)
        (not-any? legal-suffix-tokens suffix-tokens)
        (every? #(re-matches #"[a-z0-9]+" %) suffix-tokens)))))

(defn- find-by-normalized-key-with-location-suffix
  [db normalized-key]
  (when (and (seq normalized-key) (str/includes? normalized-key "-"))
    (let [rows (jdbc/execute!
                 db
                 ["SELECT * FROM suppliers WHERE ? LIKE normalized_key || '-%' ORDER BY length(normalized_key) DESC, created_at ASC LIMIT 20"
                  normalized-key]
                 {:builder-fn rs/as-unqualified-lower-maps})]
      (some (fn [row]
              (let [prefix-key (some-> (:normalized_key row) str str/trim not-empty)]
                (when (and prefix-key
                        (location-or-branch-suffix? prefix-key normalized-key))
                  row)))
        rows))))

(defn- find-by-normalized-key-with-descriptor-suffix
  [db normalized-key]
  (when (and (seq normalized-key) (str/includes? normalized-key "-"))
    (let [rows (jdbc/execute!
                 db
                 ["SELECT * FROM suppliers WHERE normalized_key LIKE ? ORDER BY length(normalized_key) ASC, created_at ASC LIMIT 25"
                  (str normalized-key "-%")]
                 {:builder-fn rs/as-unqualified-lower-maps})
          matches (->> rows
                    (filter (fn [row]
                              (let [candidate-key (some-> (:normalized_key row) str str/trim not-empty)]
                                (and candidate-key
                                  (descriptor-tail-suffix? normalized-key candidate-key)))))
                    vec)]
      ;; Match only when this heuristic is unambiguous.
      (when (= 1 (count matches))
        (first matches)))))

(defn find-unique-descriptor-suffix-supplier
  "When `normalized-key` looks like a base supplier key, try to find exactly one
  existing supplier whose key extends it with a descriptor tail (e.g. `-i-...`).

  Returns a supplier row or nil."
  [db normalized-key]
  (find-by-normalized-key-with-descriptor-suffix db normalized-key))

(defn- find-by-normalized-key-or-legacy
  [db normalized-key]
  (or (find-by-normalized-key db normalized-key)
    (find-by-canonical-key-with-legacy-suffix db normalized-key)
    (find-by-normalized-key-with-location-suffix db normalized-key)
    (find-by-normalized-key-with-descriptor-suffix db normalized-key)))

(defn- legacy-normalize-supplier-key-v0
  "Legacy supplier key normalizer used before we started folding diacritics.

  This intentionally does NOT strip diacritics first, so characters like Š/š are
  dropped by the ASCII whitelist regex (e.g. \"Hoše\" -> \"hoe\")."
  [name]
  (when-let [name* (some-> name str str/trim not-empty)]
    (-> name*
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" " ")
      str/trim
      (str/replace #" " "-"))))

(defn- find-by-normalized-keys-or-legacy
  [db normalized-keys]
  (->> normalized-keys
    (map #(some-> % str str/trim not-empty))
    (remove nil?)
    distinct
    (some #(find-by-normalized-key-or-legacy db %))))

(defn- maybe-unescape-existing-display-name!
  [db {:keys [id display_name] :as supplier}]
  (let [dn (some-> display_name str)
        dn* (some-> dn configs/unescape-html-entities str str/trim not-empty)
        looks-escaped? (boolean (and dn (re-find #"&(#\d+|#x[0-9A-Fa-f]+|[A-Za-z]+);?" dn)))]
    (if (and looks-escaped? dn* (not= dn dn*))
      (or ((:update! service) db id {:display_name dn*}) supplier)
      supplier)))

(defn find-or-create-supplier!
  "Find supplier by normalized name or create new one.
   Returns {:existing? bool :supplier {...}}"
  [db display-name & [{:keys [address]}]]
  (let [display-name (some-> display-name configs/unescape-html-entities str str/trim not-empty)
        display-name (strip-branch-suffix display-name)
        normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key-or-legacy db normalized)]
      {:existing? true
       :supplier (maybe-unescape-existing-display-name! db existing)}
      {:existing? false
       :supplier ((:create! service) db {:display_name display-name
                                         :address address})})))

(defn- unique-violation?
  [^java.sql.SQLException e]
  (= "23505" (.getSQLState e)))

(defn- create-supplier-idempotent!
  [db display-name]
  (let [display-name (some-> display-name str str/trim not-empty)
        display-name (strip-branch-suffix display-name)]
    (try
      ((:create! service) db {:display_name display-name})
      (catch java.sql.SQLException e
        (if (unique-violation? e)
          (or (find-by-normalized-key-or-legacy db (normalize-supplier-key display-name))
            (throw e))
          (throw e))))))

(defn- choose-create-display-name
  "Choose which display name to persist when Places suggests a candidate.

  We keep the OCR/display guess when it already normalizes to the same supplier key
  as the Places name (preserves promoted brand names). Otherwise we persist the
  Places candidate to avoid canonicalizing obvious OCR typos as new suppliers."
  [ocr-display-name candidate-name]
  (let [ocr-display-name (some-> ocr-display-name configs/unescape-html-entities str str/trim not-empty)
        candidate-name (some-> candidate-name configs/unescape-html-entities str str/trim not-empty)
        ocr-normalized (normalize-supplier-key ocr-display-name)
        candidate-normalized (normalize-supplier-key candidate-name)]
    (cond
      (and (seq ocr-display-name)
        (seq candidate-name)
        (seq ocr-normalized)
        (seq candidate-normalized)
        (not= ocr-normalized candidate-normalized))
      candidate-name

      (seq ocr-display-name) ocr-display-name
      :else candidate-name)))

(defn resolve-or-create-supplier-with-places!
  "Resolve or create a supplier using Places as a canonicalizer.

  Returns {:supplier <row> :source :db|:places-api|:ocr-fallback}."
  [db ocr-guess & [opts]]
  (let [display-name (some-> ocr-guess configs/unescape-html-entities str str/trim not-empty)
        display-name (strip-branch-suffix display-name)]
    (if-not display-name
      {:supplier nil :source :ocr-fallback}
      (let [normalized (normalize-supplier-key display-name)
            legacy-normalized (legacy-normalize-supplier-key-v0 display-name)]
        (if-let [existing (find-by-normalized-keys-or-legacy db [normalized legacy-normalized])]
          {:supplier (maybe-unescape-existing-display-name! db existing) :source :db}
          (let [places-cfg (:places-cfg opts)
                input-norm (normalize-for-similarity display-name)
                use-bias? (and (<= (normalized-length input-norm) 4)
                            (get-in places-cfg [:location-bias]))
                search-opts {:region-code (or (:user-region opts) (:region-code places-cfg))
                             :language-code (:language-code places-cfg)
                             :max-results (:max-results places-cfg)
                             :location-bias (when use-bias? (:location-bias places-cfg))}
                places-res (when (and (map? places-cfg) (seq (:api-key places-cfg)))
                             (places-api/search-text! places-cfg display-name search-opts))
                candidate (best-place-candidate display-name (:places places-res))]
            (if-let [{:keys [place]} candidate]
              (let [candidate-name (some-> (:name place) configs/unescape-html-entities)
                    candidate-normalized (normalize-supplier-key candidate-name)
                    candidate-legacy-normalized (legacy-normalize-supplier-key-v0 candidate-name)
                    create-display-name (choose-create-display-name display-name candidate-name)]
                (if-let [existing (find-by-normalized-keys-or-legacy
                                    db
                                    [candidate-normalized candidate-legacy-normalized])]
                  {:supplier (maybe-unescape-existing-display-name! db existing) :source :places-api}
                  {:supplier (create-supplier-idempotent! db create-display-name)
                   :source :places-api}))
              {:supplier (create-supplier-idempotent! db display-name)
               :source :ocr-fallback})))))))

;; ============================================================================
;; Related Records
;; ============================================================================

(defn- list-related-expenses
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:e.id :id]
                          [:e.purchased_at :purchased_at]
                          [:e.total_amount :total_amount]
                          [:e.currency :currency]
                          [:e.notes :notes]
                          [:e.created_at :created_at]
                          [:st.display_name :store_display_name]
                          [[:raw "json_agg(json_build_object('raw_label', aa.raw_label, 'canonical_name', a.canonical_name, 'qty', ei.qty, 'unit_price', ei.unit_price, 'line_total', ei.line_total) ORDER BY ei.created_at) FILTER (WHERE ei.id IS NOT NULL)"]
                           :items]]
                 :from [[:expenses :e]]
                 :left-join [[:stores :st] [:= :st.id :e.store_id]
                             [:expense_items :ei] [:= :ei.expense_id :e.id]
                             [:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:articles :a] [:= :a.id :ei.article_id]]
                 :where [:= :e.supplier_id supplier-id]
                 :group-by [:e.id :st.display_name]
                 :order-by [[:e.purchased_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:r.id :id]
                                   [:r.original_filename :original_filename]
                                   [:r.status :status]
                                   [:r.supplier_guess :supplier_guess]
                                   [:r.total_amount_guess :total_amount_guess]
                                   [:r.currency_guess :currency_guess]
                                   [:r.created_at :created_at]
                                   [:r.updated_at :updated_at]
                                   [:e.id :expense_id]]
                 :from [[:expenses :e]]
                 :join [[:receipts :r] [:= :r.id :e.receipt_id]]
                 :where [:= :e.supplier_id supplier-id]
                 :order-by [[:r.created_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-via-aliases
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:s.name :subcategory_name]]
                 :from [[:article_aliases :aa]]
                 :join [[:articles :a] [:= :a.id :aa.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :where [:and
                         [:= :aa.supplier_id supplier-id]
                         [:is-not :aa.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles-via-expenses
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:a.id :id]
                                   [:a.canonical_name :canonical_name]
                                   [:a.normalized_key :normalized_key]
                                   [:a.link :link]
                                   [:a.created_at :created_at]
                                   [:m.display_name :manufacturer_display_name]
                                   [:s.name :subcategory_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:articles :a] [:= :a.id :ei.article_id]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :where [:and
                         [:= :e.supplier_id supplier-id]
                         [:is-not :ei.article_id nil]]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-articles
  [db supplier-id limit]
  (rr/merge-related-rows
    limit
    (list-related-articles-via-aliases db supplier-id limit)
    (list-related-articles-via-expenses db supplier-id limit)))

(defn- list-related-stores
  [db supplier-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:st.id :id]
                          [:st.display_name :display_name]
                          [:st.normalized_key :normalized_key]
                          [:st.address :address]
                          [:st.created_at :created_at]
                          [:c.name :city]]
                 :from [[:stores :st]]
                 :left-join [[:cities :c] [:= :c.id :st.city_id]]
                 :where [:= :st.supplier_id supplier-id]
                 :order-by [[:st.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  "List records related to a supplier by type.

  Supported types: expenses, receipts, articles, stores."
  [db supplier-id {:keys [type limit]}]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))
  (let [related-type (rr/normalize-related-type type)
        related-limit (rr/clamp-related-limit limit)]
    (case related-type
      :expenses (list-related-expenses db supplier-id related-limit)
      :receipts (list-related-receipts db supplier-id related-limit)
      :articles (list-related-articles db supplier-id related-limit)
      :stores (list-related-stores db supplier-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: expenses, receipts, articles, stores."
               {:status 400 :type type})))))
