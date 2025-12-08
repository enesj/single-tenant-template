(ns app.domain.expenses.services.price-observations
  "CRUD helpers for price observations."
  (:require
    [app.domain.expenses.services.price-history :as price-history]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private allowed-order-by
  "Whitelist of order-by fields mapped from kebab-case UI keys to fully qualified columns."
  {:article-canonical-name :articles.canonical_name
   :article_canonical_name :articles.canonical_name
   :supplier-display-name  :suppliers.display_name
   :supplier_display_name  :suppliers.display_name
   :observed-at            :po.observed_at
   :observed_at            :po.observed_at
   :created-at             :po.created_at
   :created_at             :po.created_at
   :unit-price             :po.unit_price
   :unit_price             :po.unit_price
   :line-total             :po.line_total
   :line_total             :po.line_total
   :qty                    :po.qty
   :currency               :po.currency})

(defn- sanitize-order-by
  [order-by]
  (get allowed-order-by order-by :observed_at))

(defn- sanitize-order-dir
  [order-dir]
  (if (= :asc order-dir) :asc :desc))

(defn list-price-observations
  "List price observations with optional filtering.
   opts: :limit, :offset, :article-id, :supplier-id, :order-by, :order-dir"
  [db {:keys [limit offset article-id supplier-id order-by order-dir]
       :or {limit 50 offset 0 order-by :observed-at order-dir :desc}}]
  (let [where (cond-> [:and]
                article-id (conj [:= :po.article_id article-id])
                supplier-id (conj [:= :po.supplier_id supplier-id]))
        order-col (sanitize-order-by order-by)
        order-dir (sanitize-order-dir order-dir)
        base {:select [[:po.*]
                       [:articles.canonical_name :article_canonical_name]
                       [:suppliers.display_name :supplier_display_name]]
              :from [[:price_observations :po]]
              :left-join [:articles [:= :articles.id :po.article_id]
                          :suppliers [:= :suppliers.id :po.supplier_id]]
              :order-by [[order-col order-dir]]
              :limit limit
              :offset offset}
        query (if (> (count where) 1)
                (assoc base :where where)
                base)]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-price-observation
  [db id]
  (jdbc/execute-one!
    db
    (sql/format {:select [[:po.*]
                          [[:a.canonical_name] :article_canonical_name]
                          [[:s.display_name] :supplier_display_name]]
                 :from [[:price_observations :po]]
                 :left-join [[:articles :a] [:= :a.id :po.article_id]
                             [:suppliers :s] [:= :s.id :po.supplier_id]]
                 :where [:= :po.id id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn create-price-observation!
  "Create a price observation using price-history helper."
  [db {:keys [article_id supplier_id expense_item_id observed_at qty unit_price line_total currency] :as obs}]
  (price-history/record-observation!
    db (merge obs {:article_id article_id
                   :supplier_id supplier_id
                   :expense_item_id expense_item_id
                   :observed_at observed_at
                   :qty qty
                   :unit_price unit_price
                   :line_total line_total
                   :currency currency})))

(defn update-price-observation!
  "Update editable fields for a price observation. Returns updated row or nil if not found."
  [db id {:keys [article_id supplier_id observed_at qty unit_price line_total currency]}]
  (let [update-map (cond-> {}
                     article_id (assoc :article_id article_id)
                     supplier_id (assoc :supplier_id supplier_id)
                     observed_at (assoc :observed_at observed_at)
                     qty (assoc :qty qty)
                     unit_price (assoc :unit_price unit_price)
                     line_total (assoc :line_total line_total)
                     currency (assoc :currency currency))]
    (when (seq update-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :price_observations
                     :set update-map
                     :where [:= :id id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-price-observation!
  [db id]
  (pos?
    (jdbc/execute-one!
      db
      (sql/format {:delete-from :price_observations
                   :where [:= :id id]}))))
