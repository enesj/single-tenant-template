(ns app.domain.backend.expenses.handlers.user-expense-categories
  "User-facing expense category endpoints.

  These endpoints are mounted under /api/v1/expenses/expense-categories and are
  intended for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.expense-categories :as expense-categories]
    [app.shared.adapters.database :as shared-db]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access this page."))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-expense-categories-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (or (some-> (h/get-param qp :limit) parse-long) 200)
                offset (or (some-> (h/get-param qp :offset) parse-long) 0)
                search (h/get-param qp :search)
                rows (to-app ((:list expense-categories/service) db {:limit limit
                                                                     :offset offset
                                                                     :search search}))
                rows (cond-> rows (sequential? rows) vec)]
            (h/json-response {:data rows :limit limit :offset offset}))
          (catch Exception e
            (log/error e "Failed to list expense categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list expense categories"} 500)))))))

(defn create-expense-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                payload {:name (:name body)}
                expense-category (to-app ((:create! expense-categories/service) db payload))]
            (h/json-response {:data expense-category} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating expense category" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create expense category" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create expense category"} 500)))))))

(defn update-expense-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [expense-category-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                                      (get-in request [:parameters :path :id])))]
          (if-not expense-category-id
            (h/json-response {:error "Invalid expense category id"} 400)
            (try
              (let [body (h/read-body-params request)
                    updates (select-keys body [:name])
                    updated (some-> ((:update! expense-categories/service) db expense-category-id updates)
                              to-app)]
                (if updated
                  (h/json-response {:data updated})
                  (h/not-found-response "Expense category not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating expense category" {:error (ex-message e)
                                                                        :data (ex-data e)
                                                                        :expense-category-id (str expense-category-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update expense category" {:message (.getMessage e)
                                                                  :expense-category-id (str expense-category-id)})
                (h/json-response {:error "Failed to update expense category"} 500)))))))))

(defn batch-delete-expense-categories-handler
  "Batch delete expense categories (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:expense_category_ids body)
                          (:expense-category-ids body)
                          (:expenseCategoryIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No expense category ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more expense category ids are invalid"} 400)

              :else
              (let [delete! (:delete! expense-categories/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [expense-category-id ids]
                  (try
                    (if (boolean (delete! db expense-category-id))
                      (swap! deleted-ids conj (str expense-category-id))
                      (swap! errors conj {:id (str expense-category-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str expense-category-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str expense-category-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete expense categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete expense categories"} 500)))))))
