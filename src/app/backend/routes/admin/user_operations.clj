(ns app.backend.routes.admin.user-operations
  "Admin advanced user operations handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.shared.field-metadata :as field-meta]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn update-user-role-handler
  "Update user role"
  [db models]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id request]
          (let [{:keys [role]} (:body request)
                role-str (some-> role str)
                allowed-roles (set (field-meta/get-enum-choices models :users :role))
                {:keys [ip-address user-agent admin]} (utils/extract-request-context request)]
            (if (or (nil? role-str) (str/blank? role-str))
              (utils/error-response "Missing role" :status 400 :details {:allowed (vec allowed-roles)})
              (if-not (contains? allowed-roles role-str)
                (utils/error-response "Invalid role value" :status 400 :details {:allowed (vec allowed-roles) :value role-str})
                (do
                  (admin-service/update-user-role! db user-id role-str (:id admin) ip-address user-agent)

                  (utils/log-admin-action "update_user_role" (:id admin)
                    "user" user-id {:role role-str})

                  (utils/success-response))))))))
    "Failed to update user role"))

(defn force-verify-email-handler
  "Force verify user email"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id request]
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)]
            (admin-service/force-verify-email! db user-id
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
                result (admin-service/reset-user-password! db user-id
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
                activity (admin-service/get-user-activity db user-id pagination)]
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
                result (admin-service/create-user-impersonation-session! db user-id
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "impersonate_user" (:id admin)
              "user" user-id {})

            (if (:success result)
              (utils/json-response result)
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
                     :role (:role params)
                     :auth-provider (:auth-provider params)
                     :sort-by (:sort-by params)
                     :sort-order (when (:sort-order params)
                                   (keyword (:sort-order params)))}
            users (admin-service/search-users-advanced db (merge filters pagination))]
        (utils/json-response {:users users})))
    "Failed to search users"))

;; Route definitions
(defn routes
  "Advanced user operations route definitions"
  [db service-container]
  (let [models (:models-data service-container)]
    [""
     ["/role/:id" {:put (update-user-role-handler db models)}]
     ["/verify-email/:id" {:post (force-verify-email-handler db)}]
     ["/reset-password/:id" {:post (reset-user-password-handler db)}]
     ["/activity/:id" {:get (get-user-activity-handler db)}]
     ["/impersonate/:id" {:post (impersonate-user-handler db)}]
     ["/search" {:get (advanced-user-search-handler db)}]]))
