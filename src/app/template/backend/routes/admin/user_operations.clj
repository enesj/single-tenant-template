(ns app.template.backend.routes.admin.user-operations
  "Admin advanced user operations handlers"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.users :as admin-users]
    [app.admin.backend.services.admin.users.bulk :as admin-users-bulk]
    [app.admin.backend.services.admin.users.security :as user-security]))

(defn force-verify-email-handler
  "Force verify user email"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id request]
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)]
            (user-security/force-verify-email! db user-id
              (:id admin)
              ip-address
              user-agent)

            (utils/log-admin-action "force_verify_email" (:id admin)
              "user" user-id {})

            (utils/success-response {:message "Email verified"})))))
    "Failed to verify email"))

(defn reset-user-password-handler
  "Reset user password"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id request]
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
                result (user-security/reset-user-password! db user-id
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "reset_user_password" (:id admin)
              "user" user-id {})

            (if (:success result)
              (utils/json-response result)
              (utils/json-response result :status 400))))))
    "Failed to reset password"))

(defn get-user-activity-handler
  "Get user activity and analytics"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id request]
          (let [params (:params request)
                pagination (utils/extract-pagination-params params)
                activity (admin-users/get-user-activity db user-id pagination)]
            (if (:error activity)
              (utils/json-response activity :status 500)
              (utils/json-response {:activity activity}))))))
    "Failed to get user activity"))

(defn impersonate-user-handler
  "Create user impersonation session"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [user-id (utils/extract-uuid-param request :id)]
        (if user-id
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
                result (admin-users-bulk/create-user-impersonation-session! db user-id
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "impersonate_user" (:id admin)
              "user" user-id {})

            (if (:success result)
              (let [existing-session (or (:session request) {})
                    ;; IMPORTANT: merge with existing session to avoid wiping :admin-token.
                    new-session (assoc existing-session :auth-session (:auth-session result))]
                (-> (utils/json-response (dissoc result :auth-session))
                  (assoc :session new-session)))
              (utils/json-response result :status 400)))
          (utils/error-response "Invalid user ID" :status 400))))
    "Failed to impersonate user"))

(defn advanced-user-search-handler
  "Advanced user search with multiple criteria"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            filters {:search (:search params)
                     :status (:status params)
                     :email-verified (utils/parse-boolean-param params :email-verified)
                     :auth-provider (:auth-provider params)
                     :sort-by (:sort-by params)
                     :sort-order (when (:sort-order params)
                                   (keyword (:sort-order params)))}
            users (admin-users/search-users-advanced db (merge filters pagination))]
        (utils/json-response {:users users})))
    "Failed to search users"))

;; Route definitions
(defn routes
  "Advanced user operations route definitions"
  [db _service-container]
  [""
   ["/verify-email/:id" {:post (force-verify-email-handler db)}]
   ["/reset-password/:id" {:post (reset-user-password-handler db)}]
   ["/activity/:id" {:get (get-user-activity-handler db)}]
   ["/impersonate/:id" {:post (impersonate-user-handler db)}]
   ["/search" {:get (advanced-user-search-handler db)}]])
