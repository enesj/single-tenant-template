(ns app.domain.backend.expenses.services.articles
  "Article management and alias mapping for expense items."
  (:require
    [app.domain.backend.expenses.services.price-history :as price-history]
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
  [db {:keys [canonical_name category] :as data}]
  (when-not canonical_name
    (throw (ex-info "canonical_name is required" {:data data})))
  (let [normalized (normalize-article-key canonical_name)
        row {:id (UUID/randomUUID)
             :canonical_name canonical_name
             :normalized_key normalized
             :category category}
        sql-map {:insert-into :articles
                 :values [row]
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-article
  [db id]
  (jdbc/execute-one! db
    (sql/format {:select [:*] :from [:articles] :where [:= :id id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-articles
  "List articles with optional search/pagination."
  [db {:keys [search limit offset order-by order-dir]
       :or {limit 50 offset 0 order-by :canonical_name order-dir :asc}}]
  (let [base {:select [:*]
              :from [:articles]
              :order-by [[order-by order-dir]]
              :limit limit
              :offset offset}
        query (cond-> base
                search (assoc :where [:or
                                      [:ilike :canonical_name (str "%" search "%")]
                  [:ilike :normalized_key (str "%" search "%")]]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-article!
  "Update a canonical article. Recomputes normalized_key when canonical_name is provided."
  [db id {:keys [canonical_name category] :as data}]
  (let [update-map (cond-> {}
                     canonical_name (assoc :canonical_name canonical_name
                                      :normalized_key (normalize-article-key canonical_name))
                     (contains? data :category) (assoc :category category)
                     true (assoc :updated_at [:now]))]
    (when (seq update-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :articles
                     :set update-map
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-article!
  "Delete article by id. Returns true when a row was removed."
  [db id]
  (pos?
    (jdbc/execute-one!
      db
      (sql/format {:delete-from :articles
                   :where [:= :id id]}))))

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

;; ============================================================================
;; Aliases
;; ============================================================================

(defn find-article-by-alias
  "Find article by supplier-specific alias."
  [db supplier-id raw-label]
  (when (and supplier-id raw-label)
    (let [normalized (normalize-alias-label raw-label)
          query {:select [[:a.*] [:aa.confidence] [:aa.raw_label_normalized]]
                 :from [[:article_aliases :aa]]
                 :join [[:articles :a] [:= :a.id :aa.article_id]]
                 :where [:and
                         [:= :aa.supplier_id supplier-id]
                         [:= :aa.raw_label_normalized normalized]]
                 :limit 1}]
      (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

(defn create-alias!
  "Create or update an article alias for a supplier."
  [db supplier-id raw-label article-id & [{:keys [confidence]}]]
  (let [normalized (normalize-alias-label raw-label)
        row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label_normalized normalized
             :article_id article-id
             :confidence (or confidence 100)}
        sql-map {:insert-into :article_aliases
                 :values [row]
                 :on-conflict [:supplier_id :raw_label_normalized]
                 :do-update-set {:article_id article-id
                                 :confidence (or confidence 100)}
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
  [db {:keys [supplier-id article-id raw-label-normalized confidence]}]
  (let [row {:id (UUID/randomUUID)
             :supplier_id supplier-id
             :raw_label_normalized raw-label-normalized
             :article_id article-id
             :confidence (or confidence 100)}]
    (jdbc/execute-one!
      db
      (sql/format {:insert-into :article_aliases
                   :values [row]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- update-alias-article!
  [db alias-id {:keys [article-id confidence]}]
  (jdbc/execute-one!
    db
    (sql/format {:update :article_aliases
                 :set {:article_id article-id
                       :confidence (or confidence 100)}
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
  [db {:keys [supplier-id article-id raw-labels allow-reassign? confidence]
       :or {allow-reassign? false confidence 100}}]
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
                                           :raw-label-normalized raw-label-normalized
                                           :confidence confidence})]
                           (update acc* :created conj inserted))

                         (= (:article_id existing) article-id)
                         (update acc* :skipped conj {:raw-label raw-label
                                                     :raw-label-normalized raw-label-normalized
                                                     :reason :already-present
                                                     :alias-id (:id existing)})

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
                                          :confidence confidence})]
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

;; ============================================================================
;; Unmapped items queue
;; ============================================================================

(defn list-unmapped-items
  "Return expense items without an article_id for review.

  Includes supplier display name for admin UX."
  [db {:keys [supplier-id limit offset] :or {limit 50 offset 0}}]
  (jdbc/execute!
    db
    (sql/format
      (cond-> {:select [:ei.*
                        [:e.supplier_id]
                        [:s.display_name :supplier_display_name]
                        [:e.currency]
                        [:e.purchased_at]]
               :from [[:expense_items :ei]]
               :join [[:expenses :e] [:= :e.id :ei.expense_id]
                      [:suppliers :s] [:= :s.id :e.supplier_id]]
               :where [:and
                       [:is :ei.article_id nil]
                       [:is :ei.deleted_at nil]
                       [:is :e.deleted_at nil]]
               :order-by [[:ei.created_at :desc]]
               :limit limit
               :offset offset}
        supplier-id (update :where conj [:= :e.supplier_id supplier-id])))
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn map-item-to-article!
  "Attach an article to an expense item and optionally create an alias.

  opts:
  - :create-alias? (default false)
  - :allow-alias-reassign? (default false): if true, alias conflicts will be reassigned

  Returns:
  {:expense-item <updated-row>
   :alias-result <batch-create-aliases! result, when requested>}"
  [db item-id article-id {:keys [create-alias? allow-alias-reassign?]
                          :or {create-alias? false
                               allow-alias-reassign? false}}]
  (jdbc/with-transaction [tx db]
    (let [item-with-expense (jdbc/execute-one!
                              tx
                              (sql/format {:select [:ei.*
                                                    [:e.supplier_id]
                                                    [:e.currency]
                                                    [:e.purchased_at]]
                                           :from [[:expense_items :ei]]
                                           :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                           :where [:and
                                                   [:= :ei.id item-id]
                                                   [:is :ei.deleted_at nil]
                                                   [:is :e.deleted_at nil]]})
                              {:builder-fn rs/as-unqualified-lower-maps})]
      (when-not item-with-expense
        (throw (ex-info "Expense item not found" {:id item-id :status 404})))

      ;; Update item
      (let [updated (jdbc/execute-one!
                      tx
                      (sql/format {:update :expense_items
                                   :set {:article_id article-id}
                                   :where [:and
                                           [:= :id item-id]
                                           [:is :deleted_at nil]]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-lower-maps})
            alias-result (when create-alias?
                           (batch-create-aliases!
                             tx
                             {:supplier-id (:supplier_id item-with-expense)
                              :article-id article-id
                              :raw-labels [(:raw_label item-with-expense)]
                              :allow-reassign? (true? allow-alias-reassign?)}))]

        ;; Record price observation for future comparisons
        (price-history/record-observation!
          tx {:article_id article-id
              :supplier_id (:supplier_id item-with-expense)
              :expense_item_id (:id updated)
              :qty (:qty updated)
              :unit_price (:unit_price updated)
              :line_total (:line_total updated)
              :currency (:currency item-with-expense)
              :observed_at (:purchased_at item-with-expense)})

        {:expense-item updated
         :alias-result alias-result}))))
