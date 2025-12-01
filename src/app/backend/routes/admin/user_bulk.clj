(ns app.backend.routes.admin.user-bulk
  "Admin bulk user operations handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

(defn bulk-update-user-status-handler
  "Bulk update user status"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [user_ids status]} (:body request)
            {:keys [ip-address user-agent admin]} (utils/extract-request-context request)]
        (if (and user_ids status)
          (let [result (admin-service/bulk-update-user-status! db user_ids status
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "bulk_update_user_status" (:id admin)
              "users" user_ids {:status status})

            (if (:success result)
              (utils/json-response result)
              (utils/json-response result :status 400)))
          (utils/error-response "Missing user_ids or status" :status 400))))
    "Failed to bulk update user status"))

(defn bulk-update-user-role-handler
  "Bulk update user role"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [user_ids role]} (:body request)
            {:keys [ip-address user-agent admin]} (utils/extract-request-context request)]
        (if (and user_ids role)
          (let [result (admin-service/bulk-update-user-role! db user_ids role
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "bulk_update_user_role" (:id admin)
              "users" user_ids {:role role})

            (if (:success result)
              (utils/json-response result)
              (utils/json-response result :status 400)))
          (utils/error-response "Missing user_ids or role" :status 400))))
    "Failed to bulk update user role"))

(defn export-users-handler
  "Export users to CSV"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [user_ids]} (:body request)
            admin-id (utils/get-admin-id request)]

        (utils/log-admin-action "export_users" admin-id
          "users" user_ids {})

        (let [result (admin-service/export-users-csv db user_ids)]
          (if (:success result)
            (-> (response/response (:content result))
              (response/content-type "text/csv")
              (response/header "Content-Disposition"
                (str "attachment; filename=" (:filename result))))
            (utils/json-response result :status 400)))))
    "Failed to export users"))

(defn batch-update-users-handler
  "Generic batch update handler for user fields"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            admin-id (:id admin)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])
            body (:body request)
            items (:items body)]
        (log/info "Admin" (:email admin) "performing batch user update"
          {:item-count (count items)})

        (if (empty? items)
          (utils/error-response "No items provided for batch update")
          (let [results (for [item items
                              :let [user-id (utils/parse-uuid-custom (:id item))
                                    updates (dissoc item :id :created-at  :updated-at)]
                              :when (and user-id (seq updates))]
                          (try
                            (log/info "Batch updating user" user-id "with" updates)
                            (admin-service/update-user! db user-id updates admin-id ip-address user-agent)
                            (catch Exception e
                              (log/error e "Failed to update user in batch" user-id)
                              {:error true :user-id user-id :message (.getMessage e)})))
                successful-updates (remove :error results)
                failed-updates (filter :error results)]
            (log/info "Batch update completed:"
              {:successful (count successful-updates)
               :failed (count failed-updates)})

            (utils/json-response
              {:success true
               :data {:results successful-updates
                      :failures failed-updates
                      :summary {:total (count items)
                                :successful (count successful-updates)
                                :failed (count failed-updates)}}})))))
    "Failed to perform batch user update"))

;; Route definitions
(defn routes
  "Bulk user operations route definitions"
  [db]
  [""
   ["/bulk-status" {:put (bulk-update-user-status-handler db)}]
   ["/bulk-role" {:put (bulk-update-user-role-handler db)}]
   ["/batch" {:put (batch-update-users-handler db)}]  ; Generic batch endpoint
   ["/export" {:post (export-users-handler db)}]])
