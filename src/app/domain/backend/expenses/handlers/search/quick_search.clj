(ns app.domain.backend.expenses.handlers.search.quick-search
  "Lightweight scored search for the smart expense form dropdown."
  (:require
    [app.domain.backend.expenses.handlers.search.helpers :as sh]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn- quick-search-suppliers
  "Global supplier search with word_similarity score."
  [db term limit]
  (let [p (sh/pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:display_name :label]
                            [[:greatest
                              [:word_similarity term :display_name]
                              [:word_similarity term [:coalesce :address [:inline ""]]]]
                             :score]]
                   :from [:suppliers]
                   :where (sh/fuzzy-text-where [:display_name :address] term p)
                   :order-by [[:score :desc] [:display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-stores
  "Global store search with score. Includes supplier info for auto-fill."
  [db term limit]
  (let [p (sh/pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [[:st.id :id]
                            [:st.display_name :label]
                            [:st.supplier_id :supplier_id]
                            [:s.display_name :supplier_display_name]
                            [[:greatest
                              [:word_similarity term :st.display_name]
                              [:word_similarity term [:coalesce :st.address [:inline ""]]]]
                             :score]]
                   :from [[:stores :st]]
                   :left-join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                   :where (sh/fuzzy-text-where [:st.display_name :st.address] term p)
                   :order-by [[:score :desc] [:st.display_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-articles
  "Global article search with score."
  [db term limit]
  (let [p (sh/pattern term)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:canonical_name :label]
                            [[:word_similarity term :canonical_name] :score]]
                   :from [:articles]
                   :where (sh/fuzzy-text-where [:canonical_name] term p)
                   :order-by [[:score :desc] [:canonical_name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- quick-search-expense-cats
  "Tenant-scoped expense category search with score."
  [db term limit tenant-id]
  (let [p (sh/pattern term)
        text-where (sh/fuzzy-text-where [:name] term p)
        where (if tenant-id
                [:and text-where [:= :tenant_id tenant-id]]
                text-where)]
    (jdbc/execute!
      db
      (sql/format {:select [:id
                            [:name :label]
                            [[:word_similarity term :name] :score]]
                   :from [:expense_categories]
                   :where where
                   :order-by [[:score :desc] [:name :asc]]
                   :limit limit})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn quick-search-handler
  "Lightweight global catalog search for the smart expense form.
   Searches suppliers, stores, articles globally and expense_categories by tenant.
   Returns a flat scored list for cross-type ranking."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              q (get-in request [:query-params "q"])
              limit 5]
          (if (and q (>= (count q) 2))
            (try
              (let [raw (into {}
                          (pmap
                            (fn [[k f]] [k (sh/search-entity f)])
                            {:supplier  #(quick-search-suppliers db q limit)
                             :store     #(quick-search-stores db q limit)
                             :article   #(quick-search-articles db q limit)
                             :category  #(quick-search-expense-cats db q limit tenant-id)}))
                    tagged (mapcat
                             (fn [[entity-type results]]
                               (map #(assoc % :entity_type (name entity-type)) results))
                             raw)
                    sorted (->> tagged
                             (sort-by :score >)
                             (take 12)
                             vec)]
                (h/json-response {:results sorted}))
              (catch Exception e
                (log/error e "Error executing quick search" {:q q})
                (h/json-response {:error "Search failed"} 500)))
            (h/json-response {:results []}))))
      (h/unauthorized-response))))
