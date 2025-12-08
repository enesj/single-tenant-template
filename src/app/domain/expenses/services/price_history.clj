(ns app.domain.expenses.services.price-history
  "Record and query article price observations over time."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Persistence
;; ============================================================================

(defn record-observation!
  "Insert a price observation. Expects keys:
   :article_id, :supplier_id, :expense_item_id (optional),
   :observed_at (timestamp, defaults to now), :qty, :unit_price,
   :line_total, :currency.
   Returns inserted row (as unqualified lower-case keys)."
  [db {:keys [article_id supplier_id expense_item_id observed_at qty unit_price line_total currency] :as obs}]
  (when (and article_id supplier_id line_total)
    (let [row {:id (UUID/randomUUID)
               :article_id article_id
               :supplier_id supplier_id
               :expense_item_id expense_item_id
               :observed_at (or observed_at [:now])
               :qty qty
               :unit_price unit_price
               :line_total line_total
               :currency (when currency [:cast currency :currency])}
          sql-map {:insert-into :price_observations
                   :values [row]
                   :returning [:*]}]
      (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))))

;; ============================================================================
;; Queries
;; ============================================================================

(defn get-price-history
  "Return recent price observations for an article.
   opts: :supplier-id (filter), :limit (default 50)."
  [db article-id {:keys [supplier-id limit] :or {limit 50}}]
  (let [base {:select [:*]
              :from [:price_observations]
              :where [:and [:= :article_id article-id]]}
        query (cond-> base
                supplier-id (update :where conj [:= :supplier_id supplier-id])
                true (assoc :order-by [[:observed_at :desc]]
                            :limit limit))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-latest-prices
  "Return the latest price observation per supplier for the given article.
   Post-processes a sorted query to avoid DISTINCT ON complexity."
  [db article-id]
  (->> (jdbc/execute! db
         (sql/format {:select [:*]
                      :from [:price_observations]
                      :where [:= :article_id article-id]
                      :order-by [[:supplier_id :asc] [:observed_at :desc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
       (reduce (fn [acc obs]
                 (if (contains? acc (:supplier_id obs))
                   acc
                   (assoc acc (:supplier_id obs) obs)))
               {})
       vals
       vec))

(defn get-price-comparison
  "Return price observations for an article across suppliers within optional :from timestamp."
  [db article-id {:keys [from limit] :or {limit 100}}]
  (let [where-clause (cond-> [:and [:= :article_id article-id]]
                       from (conj [:>= :observed_at from]))
        query {:select [:*]
               :from [:price_observations]
               :where where-clause
               :order-by [[:observed_at :desc]]
               :limit limit}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))
