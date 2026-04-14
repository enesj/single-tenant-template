(ns app.domain.backend.expenses.handlers.search.context-suggestions
  "Context suggestions — suppliers, stores, and categories historically
   associated with selected articles."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn- context-suggestions-suppliers
  "Suppliers historically associated with the given articles, ranked by
   article coverage then by expense frequency. Only suppliers where ALL
   queried articles have been purchased are returned."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:s.id :id]
                [:s.display_name :label]
                [[:count [:distinct :aa.article_id]] :article_coverage]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:suppliers :s] [:= :s.id :e.supplier_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.supplier_id nil]]
       :group-by [:s.id :s.display_name]
       :having [:= [:count [:distinct :aa.article_id]] (count article-ids)]
       :order-by [[:article_coverage :desc] [:frequency :desc] [:s.display_name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- context-suggestions-stores
  "Stores historically associated with the given articles, ranked by
   article coverage then by expense frequency. Only stores where ALL
   queried articles have been purchased are returned."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:st.id :id]
                [:st.display_name :label]
                [:st.address :address]
                [:st.supplier_id :supplier_id]
                [:sup.display_name :supplier_display_name]
                [[:count [:distinct :aa.article_id]] :article_coverage]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:stores :st] [:= :st.id :e.store_id]
              [:suppliers :sup] [:= :sup.id :st.supplier_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.store_id nil]]
       :group-by [:st.id :st.display_name :st.address :st.supplier_id :sup.display_name]
       :having [:= [:count [:distinct :aa.article_id]] (count article-ids)]
       :order-by [[:article_coverage :desc] [:frequency :desc] [:st.display_name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- context-suggestions-categories
  "Expense categories historically associated with the given articles, ranked by
   article coverage then by expense frequency. Only categories where ALL
   queried articles have been purchased are returned."
  [db article-ids tenant-id limit]
  (jdbc/execute!
    db
    (sql/format
      {:select [[:ec.id :id]
                [:ec.name :label]
                [[:count [:distinct :aa.article_id]] :article_coverage]
                [[:count [:distinct :e.id]] :frequency]]
       :from [[:expenses :e]]
       :join [[:expense_items :ei] [:= :ei.expense_id :e.id]
              [:article_aliases :aa] [:= :aa.id :ei.alias_id]
              [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
       :where [:and
               [:= :e.tenant_id tenant-id]
               [:in :aa.article_id article-ids]
               [:is-not :e.expense_category_id nil]]
       :group-by [:ec.id :ec.name]
       :having [:= [:count [:distinct :aa.article_id]] (count article-ids)]
       :order-by [[:article_coverage :desc] [:frequency :desc] [:ec.name :asc]]
       :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn context-suggestions-handler
  "Returns suppliers, stores, and categories historically associated with the given article IDs.

  Query params:
  - article_ids: comma-separated UUIDs of selected articles
  - limit: optional (default 5, max 10)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              raw-ids (h/get-param (:query-params request) :article_ids)
              article-ids (->> (str/split (str raw-ids) #",")
                            (keep #(some-> (str/trim %) not-empty h/try-parse-uuid))
                            vec)
              limit (min (or (some-> (h/get-param (:query-params request) :limit)
                               parse-long) 5) 10)]
          (if (or (empty? article-ids) (nil? tenant-id))
            (h/json-response {:suppliers [] :stores [] :categories []})
            (try
              (let [suppliers (context-suggestions-suppliers db article-ids tenant-id limit)
                    stores (context-suggestions-stores db article-ids tenant-id limit)
                    categories (context-suggestions-categories db article-ids tenant-id limit)]
                (h/json-response {:suppliers (vec suppliers)
                                  :stores (vec stores)
                                  :categories (vec categories)}))
              (catch Exception e
                (log/error e "Error fetching context suggestions" {:article-ids article-ids})
                (h/json-response {:error "Context suggestions failed"} 500))))))
      (h/unauthorized-response))))
