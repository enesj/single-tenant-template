(ns app.template.backend.routes.admin.users
  "Admin basic user management handlers"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.users :as admin-users]
    [app.admin.backend.services.admin.users.deletion :as user-deletion]
    [app.shared.adapters.database :as shared-db]
    [taoensso.timbre :as log]))

(defn list-users-handler
  "List all users (single-tenant)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            filters {:search (:search params)
                     :status (:status params)
                     :email-verified (utils/parse-boolean-param params :email-verified)}
            users (admin-users/list-all-users db (merge filters pagination))]
        (log/info "👥 Admin list-users returned" (count users) "users"
          {:filters filters :pagination pagination})
        (let [converted-users (shared-db/to-app users)]
          (utils/json-response {:users converted-users}))))
    "Failed to retrieve users"))

(defn get-user-details-handler
  "Get detailed user information"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id _request]
          (if-let [user (admin-users/get-user-details db user-id)]
            (let [converted-user (shared-db/to-app user)]
              (utils/json-response {:user converted-user}))
            (utils/error-response "User not found" :status 404)))))
    "Failed to retrieve user details"))

(defn update-user-handler
  "Update user information"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [user-id updates context _request]
          (let [updated-user (admin-users/update-user! db user-id updates
                               (-> context :admin :id)
                               (:ip-address context)
                               (:user-agent context))]

            (utils/log-admin-action "update_user" (-> context :admin :id)
              "user" user-id updates)

            ;; Return the updated user data for frontend processing
            (let [converted-user (shared-db/to-app updated-user)]
              (utils/json-response converted-user))))))
    "Failed to update user"))

(defn create-user-handler
  "Create a new user in admin context"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
            user-data (:body request)]

        (log/info "Admin create user request:" user-data)

        (let [created-user (admin-users/create-user! db user-data
                 (:id admin)
                 ip-address
                 user-agent)]

          (utils/log-admin-action "create_user" (:id admin) "user"
            (:id created-user) user-data)

          (let [converted-user (shared-db/to-app created-user)]
            (utils/json-response {:user converted-user})))))
    "Failed to create user"))

(defn delete-user-handler
  "Delete user with comprehensive validation and audit logging"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id _request]
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
                {:keys [force-delete]} (:body request)
                result (user-deletion/delete-user! db user-id
                         (:id admin)
                         ip-address
                         user-agent
                         :force-delete force-delete)]

            (utils/log-admin-action "delete_user" (:id admin)
              "user" user-id {:force-delete force-delete})

            (if (:success result)
              (utils/success-response {:message (:message result)
                                       :user (:user result)
                                       :deleted-at (:deleted-at result)})
              (utils/error-response (:message result) :status 400))))))
    "Failed to delete user"))

;; Route definitions
(defn routes
  "Basic user management route definitions"
  [db]
  [""
   ["" {:get (list-users-handler db)
        :post (create-user-handler db)}]
   ["/:id"
    {:get (get-user-details-handler db)
     :put (update-user-handler db)
     :delete (delete-user-handler db)}]])
