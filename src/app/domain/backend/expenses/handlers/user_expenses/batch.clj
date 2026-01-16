(ns app.domain.backend.expenses.handlers.user-expenses.batch
  "Batch update/delete handlers for user expenses."
  (:require
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

(defn batch-update-expenses-handler
  "Handler factory for batch-updating multiple user expenses.

  Expects JSON body like:
  {:items [{:id \" <uuid> \" :supplier_id ... :notes ...} ...]}

  Returns:
  {:updated n :results [...] :errors [...]}"
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-write-roles "Only members, admins, and owners can modify expenses")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-items (or (:items body) [])
                items (cond
                        (vector? raw-items) raw-items
                        (sequential? raw-items) (vec raw-items)
                        :else [])
                parsed-items (mapv (fn [item]
                                     (if (map? item)
                                       (update item :id h/try-parse-uuid)
                                       item))
                               items)]
            (if (empty? items)
              (h/json-response {:error "No items provided"} 400)
              (h/json-response (user-expenses/batch-update-user-expenses! db user-id parsed-items))))
          (catch Exception e
            (log/error e "Error batch updating user expenses"
              {:user-id user-id
               :path-params (:path-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to update expenses"} 500))))
      (h/unauthorized-response))))

(defn batch-delete-expenses-handler
  "Handler factory for soft-deleting multiple user expenses.

  Expects JSON body like:
  {:ids [\"<uuid>\" ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :not-found-ids [...]}}"
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-write-roles "Only members, admins, and owners can modify expenses")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:expense_ids body)
                          (:expense-ids body)
                          [])
                ids (cond
                      (vector? raw-ids) raw-ids
                      (set? raw-ids) (vec raw-ids)
                      (sequential? raw-ids) (vec raw-ids)
                      :else [])
                parsed-ids (mapv h/try-parse-uuid ids)]
            (cond
              (empty? ids)
              (h/json-response {:error "No expense ids provided"} 400)

              (some nil? parsed-ids)
              (h/json-response {:error "One or more expense ids are invalid"} 400)

              :else
              (let [result (user-expenses/soft-delete-user-expenses! db user-id parsed-ids)]
                (h/json-response {:data result}))))
          (catch Exception e
            (log/error e "Error batch deleting user expenses"
              {:user-id user-id
               :path-params (:path-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to delete expenses"} 500))))
      (h/unauthorized-response))))
