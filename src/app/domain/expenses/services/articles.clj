(ns app.domain.expenses.services.articles
  "Article management and alias mapping for expense items."
  (:require
    [app.domain.expenses.services.price-history :as price-history]
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
  [db {:keys [canonical_name barcode category] :as data}]
  (when-not canonical_name
    (throw (ex-info "canonical_name is required" {:data data})))
  (let [normalized (normalize-article-key canonical_name)
        row {:id (UUID/randomUUID)
             :canonical_name canonical_name
             :normalized_key normalized
             :barcode barcode
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
                                      [:ilike :normalized_key (str "%" search "%")]
                                      [:ilike :barcode (str "%" search "%")]]))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

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

;; ============================================================================
;; Unmapped items queue
;; ============================================================================

(defn list-unmapped-items
  "Return expense items without an article_id for review."
  [db {:keys [limit offset] :or {limit 50 offset 0}}]
  (jdbc/execute!
    db
    (sql/format {:select [:ei.* [:e.supplier_id] [:e.currency] [:e.purchased_at]]
                 :from [[:expense_items :ei]]
                 :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                 :where [:and
                         [:is :ei.article_id nil]
                         [:is :e.deleted_at nil]]
                 :order-by [[:ei.created_at :desc]]
                 :limit limit
                 :offset offset})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn map-item-to-article!
  "Attach an article to an expense item and optionally create an alias.
   opts: {:create-alias? true/false}"
  [db item-id article-id {:keys [create-alias?] :or {create-alias? false}}]
  (jdbc/with-transaction [tx db]
    (let [item-with-expense (jdbc/execute-one!
                              tx
                              (sql/format {:select [:ei.* [:e.supplier_id] [:e.currency] [:e.purchased_at]]
                                           :from [[:expense_items :ei]]
                                           :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                           :where [:= :ei.id item-id]})
                              {:builder-fn rs/as-unqualified-lower-maps})]
      (when-not item-with-expense
        (throw (ex-info "Expense item not found" {:id item-id})))

      ;; Update item
      (let [updated (jdbc/execute-one!
                      tx
                      (sql/format {:update :expense_items
                                   :set {:article_id article-id}
                                   :where [:= :id item-id]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-lower-maps})]
        (when create-alias?
          (create-alias! tx (:supplier_id item-with-expense) (:raw_label item-with-expense) article-id))

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

        updated))))
