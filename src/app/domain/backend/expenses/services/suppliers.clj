(ns app.domain.backend.expenses.services.suppliers
  "Supplier CRUD services using factory pattern."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
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

(defn active-expenses-counts-by-supplier
  "Return a map of supplier-id -> expenses count."
  [db supplier-ids]
  (if (seq supplier-ids)
    (let [rows
          (jdbc/execute!
            db
            (sql/format {:select [:supplier_id [[:count :*] :active_expenses_count]]
                         :from [:expenses]
                         :where [:and
                                 [:in :supplier_id supplier-ids]]
                         :group-by [:supplier_id]})
            {:builder-fn rs/as-unqualified-lower-maps})]
      (reduce (fn [acc {:keys [supplier_id active_expenses_count]}]
                (assoc acc supplier_id (long (or active_expenses_count 0))))
        {}
        rows))
    {}))

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

(defn- find-by-normalized-key-or-legacy
  [db normalized-key]
  (or (find-by-normalized-key db normalized-key)
    (find-by-canonical-key-with-legacy-suffix db normalized-key)))

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
        normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key db normalized)]
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
  (try
    ((:create! service) db {:display_name display-name})
    (catch java.sql.SQLException e
      (if (unique-violation? e)
        (or (find-by-normalized-key db (normalize-supplier-key display-name))
          (throw e))
        (throw e)))))

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
  (let [display-name (some-> ocr-guess configs/unescape-html-entities str str/trim not-empty)]
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

(defn search-suppliers-autocomplete
  "Search suppliers for autocomplete with fuzzy matching."
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [search-pattern (str "%" query "%")]
      (jdbc/execute!
        db
        (sql/format {:select [:*]
                     :from [:suppliers]
                     :where [:or
                             [:ilike :display_name search-pattern]
                             [:ilike :normalized_key search-pattern]]
                     :order-by [[:display_name :asc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))
