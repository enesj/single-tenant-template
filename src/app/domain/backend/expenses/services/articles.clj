(ns app.domain.backend.expenses.services.articles
  "Article management and alias mapping for expense items."
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Normalization
;; ============================================================================

(defn normalize-article-key
  "Normalize a canonical article name for deduplication."
  [name]
  (when name
    (-> name
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))

(defn normalize-alias-label
  "Normalize raw line-item labels for alias lookup."
  [label]
  (when label
    (-> label
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))

;; ============================================================================
;; CRUD
;; ============================================================================

(defn create-article!
  "Create a canonical article."
  [db {:keys [canonical_name link manufacturer_id subcategory_id] :as data}]
  (when-not canonical_name
    (throw (ex-info "canonical_name is required" {:data data})))
  (let [normalized (normalize-article-key canonical_name)
        row (cond-> {:id (UUID/randomUUID)
                     :canonical_name canonical_name
                     :normalized_key normalized}

              (contains? data :subcategory_id) (assoc :subcategory_id subcategory_id)
              (contains? data :link) (assoc :link link)
              (contains? data :manufacturer_id) (assoc :manufacturer_id manufacturer_id))
        sql-map {:insert-into :articles
                 :values [row]
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-article-by-normalized-key
  "Fetch an article by its `normalized_key`."
  [db normalized-key]
  (when (seq (some-> normalized-key str str/trim))
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:articles]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-or-create-article-by-canonical-name!
  "Find or create an article based on the normalized key derived from `canonical-name`.

  This is safe under concurrency because the `articles.normalized_key` unique index
  enforces deduplication.

  Notes:
  - We do not overwrite existing canonical data if the row already exists.
  - When a unique violation happens, we fetch and return the existing row."
  [db canonical-name]
  (let [canonical-name* (some-> canonical-name str str/trim not-empty)]
    (when-not canonical-name*
      (throw (ex-info "canonical_name is required" {:canonical-name canonical-name})))
    (let [normalized (normalize-article-key canonical-name*)]
      (try
        (create-article! db {:canonical_name canonical-name*})
        (catch org.postgresql.util.PSQLException e
          (if (= "23505" (.getSQLState e))
            (or (get-article-by-normalized-key db normalized)
              (throw e))
            (throw e)))))))

(defn get-article
  [db id]
  (jdbc/execute-one! db
    (sql/format {:select [[:a.*]
                          [:m.display_name :manufacturer_display_name]
                          [:s.name :subcategory_name]
                          [:c.name :category_name]]
                 :from [[:articles :a]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                             [:subcategories :s] [:= :s.id :a.subcategory_id]
                             [:categories :c] [:= :c.id :s.category_id]]
                 :where [:= :a.id id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-articles
  "List articles with optional search/pagination."
  [db {:keys [search limit offset order-by order-dir]
       :or {limit 50 offset 0 order-by :canonical_name order-dir :asc}}]
  (let [base {:select [[:a.*]
                       [:m.display_name :manufacturer_display_name]
                       [:s.name :subcategory_name]
                       [:c.name :category_name]]
              :from [[:articles :a]]
              :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]
                          [:subcategories :s] [:= :s.id :a.subcategory_id]
                          [:categories :c] [:= :c.id :s.category_id]]
              :order-by [[(keyword "a" (name order-by)) order-dir]]
              :limit limit
              :offset offset}
        query (cond-> base
                search (assoc :where [:or
                                      [:ilike :a.canonical_name (str "%" search "%")]
                                      [:ilike :a.normalized_key (str "%" search "%")]]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-article!
  "Update a canonical article. Recomputes normalized_key when canonical_name is provided."
  [db id {:keys [canonical_name subcategory_id link manufacturer_id] :as data}]
  (let [update-map (cond-> {}
                     canonical_name (assoc :canonical_name canonical_name
                                      :normalized_key (normalize-article-key canonical_name))

                     (contains? data :subcategory_id) (assoc :subcategory_id subcategory_id)
                     (contains? data :link) (assoc :link link)
                     (contains? data :manufacturer_id) (assoc :manufacturer_id manufacturer_id))]
    (when (seq update-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :articles
                     :set update-map
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- update-count
  "Extract next.jdbc update count from a DML result map."
  [result]
  (or (:next.jdbc/update-count result)
    (:update-count result)
    0))

(defn delete-article!
  "Delete article by id. Returns true when a row was removed."
  [db id]
  (pos?
    (update-count
      (jdbc/execute-one!
        db
        (sql/format {:delete-from :articles
                     :where [:= :id id]})))))

(defn count-articles
  "Count total articles, optionally with search filter."
  [db & [search]]
  (let [base-query {:select [[[:count :*] :total]]
                    :from [:articles]}
        final-query (if search
                      (assoc base-query :where [:or
                                                [:ilike :canonical_name (str "%" search "%")]
                                                [:ilike :normalized_key (str "%" search "%")]])
                      base-query)]
    (:total (jdbc/execute-one! db (sql/format final-query)
              {:builder-fn rs/as-unqualified-lower-maps}))))

(defn search-articles
  "Search articles for autocomplete."
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [search-pattern (str "%" query "%")]
      (jdbc/execute!
        db
        (sql/format {:select [:*]
                     :from [:articles]
                     :where [:or
                             [:ilike :canonical_name search-pattern]
                             [:ilike :normalized_key search-pattern]]
                     :order-by [[:canonical_name :asc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- normalize-related-type
  [related-type]
  (let [raw (cond
              (keyword? related-type) (name related-type)
              (string? related-type) related-type
              :else nil)
        related-type* (some-> raw str/trim str/lower-case keyword)]
    (case related-type*
      :supplier :providers
      :suppliers :providers
      :provider :providers
      :manufacturer :manufacturers
      :subcategory :subcategories
      related-type*)))

(defn- clamp-related-limit
  [limit]
  (-> (or limit 100)
    (max 1)
    (min 500)))

(defn- article-item-linkage-where
  [article-id]
  [:or
   [:= :ei.article_id article-id]
   [:= :aa.article_id article-id]])

(defn- merge-related-rows
  "Merge ordered related-record result sets, de-duplicating by `:id` while
  preserving the order of higher-signal sources passed first."
  [limit & row-groups]
  (->> row-groups
    (apply concat)
    (reduce (fn [{:keys [seen rows] :as acc} row]
              (let [row-id (:id row)]
                (if (contains? seen row-id)
                  acc
                  {:seen (conj seen row-id)
                   :rows (conj rows row)})))
      {:seen #{}
       :rows []})
    :rows
    (take limit)
    vec))

(defn- list-related-expenses
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:e.id :id]
                                   [:e.purchased_at :purchased_at]
                                   [:e.total_amount :total_amount]
                                   [:e.currency :currency]
                                   [:e.notes :notes]
                                   [:e.receipt_id :receipt_id]
                                   [:e.created_at :created_at]
                                   [:s.display_name :supplier_display_name]
                                   [:st.display_name :store_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :e.supplier_id]
                             [:stores :st] [:= :st.id :e.store_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:e.purchased_at :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts-expense-linked
  [db article-id limit]
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
                                   [:e.id :expense_id]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:receipts :r] [:= :r.id :e.receipt_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :e.supplier_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:r.created_at :desc]
                            [:r.id :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts-alias-linked
  [db article-id limit]
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
                                   [:r.expense_id :expense_id]
                                   [[:coalesce :store_supplier.display_name :supplier_alias_supplier.display_name]
                                    :supplier_display_name]]
                 :from [[:receipts :r]]
                 :left-join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                             [:stores :st] [:= :st.id :sta.store_id]
                             [:suppliers :store_supplier] [:= :store_supplier.id :st.supplier_id]
                             [:supplier_aliases :sa] [:= :sa.id :r.supplier_alias_id]
                             [:suppliers :supplier_alias_supplier] [:= :supplier_alias_supplier.id :sa.supplier_id]]
                 :where [:exists {:select [1]
                                  :from [[:article_aliases :aa]]
                                  :where [:and
                                          [:= :aa.article_id article-id]
                                          [:or
                                           [:= :aa.supplier_id :sa.supplier_id]
                                           [:= :aa.supplier_id :st.supplier_id]]]}]
                 :order-by [[:r.created_at :desc]
                            [:r.id :desc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-receipts
  [db article-id limit]
  (merge-related-rows
    limit
    (list-related-receipts-expense-linked db article-id limit)
    (list-related-receipts-alias-linked db article-id limit)))

(defn- list-related-providers
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:s.id :id]
                          [:s.display_name :display_name]
                          [:s.normalized_key :normalized_key]
                          [:s.address :address]
                          [:s.created_at :created_at]]
                 :from [[:suppliers :s]]
                 :where [:or
                         [:exists {:select [1]
                                   :from [[:article_aliases :aa]]
                                   :where [:and
                                           [:= :aa.article_id article-id]
                                           [:= :aa.supplier_id :s.id]]}]
                         [:exists {:select [1]
                                   :from [[:expense_items :ei]]
                                   :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                   :where [:and
                                           [:= :ei.article_id article-id]
                                           [:= :e.supplier_id :s.id]]}]]
                 :order-by [[:s.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores-expense-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:st.id :id]
                                   [:st.display_name :display_name]
                                   [:st.normalized_key :normalized_key]
                                   [:st.address :address]

                                   [:st.created_at :created_at]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]
                        [:stores :st] [:= :st.id :e.store_id]]
                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                             [:suppliers :s] [:= :s.id :st.supplier_id]]
                 :where (article-item-linkage-where article-id)
                 :order-by [[:st.display_name :asc]
                            [:st.id :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores-alias-linked
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:st.id :id]
                                   [:st.display_name :display_name]
                                   [:st.normalized_key :normalized_key]
                                   [:st.address :address]

                                   [:st.created_at :created_at]
                                   [:s.display_name :supplier_display_name]]
                 :from [[:stores :st]]
                 :left-join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                 :where [:exists {:select [1]
                                  :from [[:article_aliases :aa]]
                                  :where [:and
                                          [:= :aa.article_id article-id]
                                          [:= :aa.supplier_id :st.supplier_id]]}]
                 :order-by [[:st.display_name :asc]
                            [:st.id :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-stores
  [db article-id limit]
  (merge-related-rows
    limit
    (list-related-stores-expense-linked db article-id limit)
    (list-related-stores-alias-linked db article-id limit)))

(defn- list-related-manufacturers
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:m.id :id]
                                   [:m.display_name :display_name]
                                   [:m.normalized_key :normalized_key]
                                   [:m.created_at :created_at]
                                   [:m.updated_at :updated_at]]
                 :from [[:articles :a]]
                 :join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                 :where [:= :a.id article-id]
                 :order-by [[:m.display_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-related-subcategories
  [db article-id limit]
  (jdbc/execute!
    db
    (sql/format {:select-distinct [[:s.id :id]
                                   [:s.name :name]
                                   [:s.description :description]
                                   [:s.category_id :category_id]
                                   [:s.created_at :created_at]
                                   [:s.updated_at :updated_at]
                                   [:c.name :category_name]]
                 :from [[:articles :a]]
                 :join [[:subcategories :s] [:= :s.id :a.subcategory_id]]
                 :left-join [[:categories :c] [:= :c.id :s.category_id]]
                 :where [:= :a.id article-id]
                 :order-by [[:s.name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  "List records related to an article by type.

  Supported types: expenses, receipts, providers (suppliers), stores,
  manufacturers, subcategories."
  [db article-id {:keys [type limit]}]
  (when-not article-id
    (throw (ex-info "article-id is required" {:status 400})))
  (let [related-type (normalize-related-type type)
        related-limit (clamp-related-limit limit)]
    (case related-type
      :expenses (list-related-expenses db article-id related-limit)
      :receipts (list-related-receipts db article-id related-limit)
      :providers (list-related-providers db article-id related-limit)
      :stores (list-related-stores db article-id related-limit)
      :manufacturers (list-related-manufacturers db article-id related-limit)
      :subcategories (list-related-subcategories db article-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: expenses, receipts, providers, stores, manufacturers, subcategories."
               {:status 400 :type type})))))

;; ============================================================================
;; Aliases
;; ============================================================================

(defn find-article-by-alias
  "Find article by supplier-specific alias."
  [db supplier-id raw-label]
  (when (and supplier-id raw-label)
    (let [normalized (normalize-alias-label raw-label)
          query {:select [[:a.*] [:aa.raw_label_normalized]]
                 :from [[:article_aliases :aa]]
                 :join [[:articles :a] [:= :a.id :aa.article_id]]
                 :where [:and
                         [:= :aa.supplier_id supplier-id]
                         [:= :aa.raw_label_normalized normalized]]
                 :limit 1}]
      (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

(defn create-alias!
  "Create or update an article alias for a supplier."
  [db supplier-id raw-label article-id]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (normalize-alias-label raw-label*)
        row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label raw-label*
             :raw_label_normalized normalized
             :article_id article-id}
        sql-map {:insert-into :article_aliases
                 :values [row]
                 :on-conflict [:supplier_id :raw_label_normalized]
                 :do-update-set {:article_id article-id
                                 :raw_label :excluded/raw_label}
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(def ^:private min-alias-normalized-length
  "Minimum length of a normalized alias label to be considered valid.

  We keep this small but non-trivial to avoid adding garbage aliases like empty
  strings or a single dash.
  "
  2)

(defn- raw-labels->seq
  "Accept raw labels in either vector form or as a newline-separated string."
  [raw-labels]
  (cond
    (nil? raw-labels) []
    (string? raw-labels) (->> (str/split-lines raw-labels)
                           (map str/trim)
                           (remove str/blank?))
    (sequential? raw-labels) (->> raw-labels (map (fn [x] (when x (str x)))))
    :else []))

(defn- get-alias-by-normalized
  [db supplier-id raw-label-normalized]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:article_aliases]
                 :where [:and
                         [:= :supplier_id supplier-id]
                         [:= :raw_label_normalized raw-label-normalized]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- insert-alias!
  [db {:keys [supplier-id article-id raw-label raw-label-normalized]}]
  (let [row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label raw-label
             :raw_label_normalized raw-label-normalized
             :article_id article-id}]
    (jdbc/execute-one!
      db
      (sql/format {:insert-into :article_aliases
                   :values [row]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- update-alias-article!
  [db alias-id {:keys [article-id raw-label]}]
  (jdbc/execute-one!
    db
    (sql/format {:update :article_aliases
                 :set (cond-> {:article_id article-id}
                        (some? raw-label) (assoc :raw_label raw-label))
                 :where [:= :id alias-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn batch-create-aliases!
  "Batch-create aliases for a single supplier -> single article.

  Input:
  - supplier-id (UUID)
  - article-id (UUID)
  - raw-labels (seq of strings, or a newline-separated string)

  Options:
  - allow-reassign? (default false): when true, existing aliases that point to a
    different article will be updated to the new article.

  Returns:
  {:created [...]
   :skipped [...]
   :conflicts [...]
   :reassigned [...]}

  Notes:
  - Skips blanks/invalid labels.
  - Dedupes by normalized key.
  - Does NOT silently reassign conflicts unless allow-reassign? is true.
  "
  [db {:keys [supplier-id article-id raw-labels allow-reassign?]
       :or {allow-reassign? false}}]
  (when-not supplier-id
    (throw (ex-info "supplier-id is required" {:status 400})))
  (when-not article-id
    (throw (ex-info "article-id is required" {:status 400})))

  (jdbc/with-transaction [tx db]
    (let [inputs (raw-labels->seq raw-labels)
          ;; Keep both the original raw label and normalized key for user feedback.
          ;; Normalize early so we can dedupe.
          normalized (map (fn [raw]
                            (let [raw* (when raw (str/trim raw))
                                  n (normalize-alias-label raw*)]
                              {:raw-label raw*
                               :raw-label-normalized n}))
                       inputs)
          ;; Validate + dedupe by normalized key.
          step (reduce
                 (fn [{:keys [seen] :as acc} {:keys [raw-label raw-label-normalized]}]
                   (cond
                     (str/blank? raw-label)
                     (update acc :skipped conj {:raw-label raw-label
                                                :reason :blank})

                     (str/blank? raw-label-normalized)
                     (update acc :skipped conj {:raw-label raw-label
                                                :reason :normalizes-to-blank})

                     (< (count raw-label-normalized) min-alias-normalized-length)
                     (update acc :skipped conj {:raw-label raw-label
                                                :raw-label-normalized raw-label-normalized
                                                :reason :too-short})

                     (contains? seen raw-label-normalized)
                     (update acc :skipped conj {:raw-label raw-label
                                                :raw-label-normalized raw-label-normalized
                                                :reason :duplicate})

                     :else
                     (let [existing (get-alias-by-normalized tx supplier-id raw-label-normalized)
                           acc* (update acc :seen conj raw-label-normalized)]
                       (cond
                         (nil? existing)
                         (let [inserted (insert-alias!
                                          tx
                                          {:supplier-id supplier-id
                                           :article-id article-id
                                           :raw-label raw-label
                                           :raw-label-normalized raw-label-normalized})]
                           (update acc* :created conj inserted))

                         (= (:article_id existing) article-id)
                         (do
                           (update-alias-article!
                             tx
                             (:id existing)
                             {:article-id article-id
                              :raw-label raw-label})
                           (update acc* :skipped conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :reason :already-present
                                                       :alias-id (:id existing)}))

                         (false? allow-reassign?)
                         (update acc* :conflicts conj {:raw-label raw-label
                                                       :raw-label-normalized raw-label-normalized
                                                       :alias-id (:id existing)
                                                       :existing-article-id (:article_id existing)
                                                       :article-id article-id})

                         :else
                         (let [updated (update-alias-article!
                                         tx
                                         (:id existing)
                                         {:article-id article-id
                                          :raw-label raw-label})]
                           (update acc* :reassigned conj {:alias-id (:id updated)
                                                          :raw-label-normalized raw-label-normalized
                                                          :existing-article-id (:article_id existing)
                                                          :article-id article-id}))))))
                 {:seen #{}
                  :created []
                  :skipped []
                  :conflicts []
                  :reassigned []}
                 normalized)
          {:keys [created skipped conflicts reassigned]} step]
      {:created created
       :skipped skipped
       :conflicts conflicts
       :reassigned reassigned})))

