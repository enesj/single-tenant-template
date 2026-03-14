(ns app.domain.backend.expenses.handlers.search.cross-entity
  "Cross-entity search handlers — fan out across all tables in parallel via pmap."
  (:require
    [app.domain.backend.expenses.handlers.search.entity-queries :as eq]
    [app.domain.backend.expenses.handlers.search.helpers :as sh]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

(defn user-search-handler
  "Handler for user search — results are scoped to the user's tenant."
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
              (let [results (into {}
                              (pmap
                                (fn [[k f]] [k (sh/search-entity f)])
                                {:payers         #(eq/search-payers db q limit tenant-id)
                                 :expense-cats   #(eq/search-expense-cats db q limit tenant-id)
                                 :suppliers      #(eq/search-suppliers db q limit tenant-id)
                                 :stores         #(eq/search-stores db q limit tenant-id)
                                 :articles       #(eq/search-articles db q limit tenant-id)
                                 :categories     #(eq/search-categories db q limit tenant-id)
                                 :subcategories  #(eq/search-subcategories db q limit tenant-id)
                                 :manufacturers  #(eq/search-manufacturers db q limit tenant-id)
                                 :cities         #(eq/search-cities db q limit tenant-id)}))]
                (h/json-response {:results results}))
              (catch Exception e
                (log/error e "Error executing user search" {:q q :tenant-id tenant-id})
                (h/json-response {:error "Search failed"} 500)))
            (h/json-response {:results {}}))))
      (h/unauthorized-response))))

(defn admin-search-handler
  "Handler for admin search — results are global (no tenant filter)."
  [db]
  (fn [request]
    (let [q (get-in request [:query-params "q"])
          limit 5]
      (if (and q (>= (count q) 2))
        (try
          (let [results (into {}
                          (pmap
                            (fn [[k f]] [k (sh/search-entity f)])
                            {:payers         #(eq/search-payers db q limit nil)
                             :expense-cats   #(eq/search-expense-cats db q limit nil)
                             :suppliers      #(eq/search-suppliers db q limit nil)
                             :stores         #(eq/search-stores db q limit nil)
                             :articles       #(eq/search-articles db q limit nil)
                             :categories     #(eq/search-categories db q limit nil)
                             :subcategories  #(eq/search-subcategories db q limit nil)
                             :manufacturers  #(eq/search-manufacturers db q limit nil)
                             :cities         #(eq/search-cities db q limit nil)}))]
            (h/json-response {:results results}))
          (catch Exception e
            (log/error e "Error executing admin search" {:q q})
            (h/json-response {:error "Search failed"} 500)))
        (h/json-response {:results {}})))))
