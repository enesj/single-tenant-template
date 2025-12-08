(ns app.domain.expenses.routes.expenses
  "Admin API routes for expense records."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.expenses :as expenses]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
      db-adapter/convert-pg-objects
      db-adapter/convert-db-keys->app-keys))

(defn list-expenses-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:from (:from qp)
                  :to (:to qp)
                  :supplier-id (or (utils/parse-uuid-custom (:supplier_id qp))
                                   (utils/parse-uuid-custom (:supplier-id qp)))
                  :payer-id (or (utils/parse-uuid-custom (:payer_id qp))
                                (utils/parse-uuid-custom (:payer-id qp)))
                  :is-posted? (utils/parse-boolean-param qp :is_posted)
                  :limit (utils/parse-int-param qp :limit 50)
                  :offset (utils/parse-int-param qp :offset 0)
                  :order-dir (keyword (or (:order-dir qp) "desc"))}
            rows (expenses/list-expenses db opts)]
        (utils/success-response {:expenses (to-app rows)})))
    "Failed to list expenses"))

(defn create-expense-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            items (vec (:items body))
            expense-data (dissoc body :items)
            expense (expenses/create-expense! db expense-data items)]
        (utils/success-response {:expense (to-app expense)})))
    "Failed to create expense"))

(defn get-expense-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [expense (expenses/get-expense-with-items db id)]
          (utils/success-response {:expense (to-app expense)})
          (utils/error-response "Expense not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch expense"))

(defn update-expense-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [updated (expenses/update-expense! db id (:body request))]
          (if updated
            (utils/success-response {:expense (to-app updated)})
            (utils/error-response "Expense not found" :status 404)))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update expense"))

(defn delete-expense-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [deleted (expenses/soft-delete-expense! db id)]
          (if deleted
            (utils/success-response {:expense (to-app deleted)})
            (utils/error-response "Expense not found" :status 404)))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete expense"))

(defn routes [db]
  ["/entries"
   ["" {:get (list-expenses-handler db)
        :post (create-expense-handler db)}]
   ["/:id" {:get (get-expense-handler db)
            :put (update-expense-handler db)
            :delete (delete-expense-handler db)}]])
