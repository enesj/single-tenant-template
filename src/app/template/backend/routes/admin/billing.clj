(ns app.template.backend.routes.admin.billing
  "Admin billing routes for payment-provider account links."
  (:require
    [app.admin.backend.services.admin.billing :as billing]
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as utils]))

(defn list-provider-links-handler
  "List payment-provider account links for admin billing operations."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            sort (utils/extract-sort-params params)
            filters {:account-kind (or (:account-kind params) (get params "account-kind"))
                     :provider (or (:provider params) (get params "provider"))
                     :status (or (:status params) (get params "status"))
                     :search (or (:search params) (get params "search"))}
            {:keys [links total limit offset]}
            (billing/list-provider-links-page db (merge filters pagination sort))]
        (utils/json-response {:links links
                              :total total
                              :limit limit
                              :offset offset})))
    "Failed to retrieve payment provider links"))

(defn create-provider-link-handler
  "Create a payment-provider account link without relying on email identity."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
            body (:body request)
            account-id (or (utils/parse-uuid-custom (:account-id body))
                         (utils/parse-uuid-custom (:account_id body)))
            result (billing/create-provider-link!
                     db
                     {:account-kind (or (:account-kind body) (:account_kind body))
                      :account-id account-id
                      :provider (:provider body)
                      :provider-customer-ref (or (:provider-customer-ref body)
                                               (:provider_customer_ref body))
                      :status (:status body)}
                     (:id admin)
                     ip-address
                     user-agent)]
        (utils/json-response {:link result} :status 201)))
    "Failed to create payment provider link"))

(defn update-provider-link-status-handler
  "Update the status of a payment-provider account link."
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [link-id body context _request]
          (let [result (billing/update-provider-link-status!
                         db
                         link-id
                         (:status body)
                         (-> context :admin :id)
                         (:ip-address context)
                         (:user-agent context))]
            (utils/json-response {:link result})))))
    "Failed to update payment provider link status"))

(defn routes
  "Payment-provider billing route definitions."
  [db]
  ["/billing"
   ["/provider-links"
    {:get (list-provider-links-handler db)
     :post {:middleware [#(admin-mw/wrap-admin-role % :support)]
            :handler (create-provider-link-handler db)}}]
   ["/provider-links/:id/status"
    {:put {:middleware [#(admin-mw/wrap-admin-role % :support)]
           :handler (update-provider-link-status-handler db)}}]])
