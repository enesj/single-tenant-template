(ns app.backend.routes.admin.users
  "Admin basic user management handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.shared.adapters.database :as db-adapter]
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
            users (admin-service/list-all-users db (merge filters pagination))]
        (log/info "👥 Admin list-users returned" (count users) "users"
          {:filters filters :pagination pagination})
        (let [converted-users (-> users
                                db-adapter/convert-pg-objects
                                db-adapter/convert-db-keys->app-keys)]
          (utils/json-response {:users converted-users}))))
    "Failed to retrieve users"))

(defn get-user-details-handler
  "Get detailed user information"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id _request]
          (if-let [user (admin-service/get-user-details db user-id)]
            (let [converted-user (-> user
                                   db-adapter/convert-pg-objects
                                   db-adapter/convert-db-keys->app-keys)]
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
          (let [updated-user (admin-service/update-user! db user-id updates
                               (-> context :admin :id)
                               (:ip-address context)
                               (:user-agent context))]

            (utils/log-admin-action "update_user" (-> context :admin :id)
              "user" user-id updates)

            ;; Return the updated user data for frontend processing
            (let [converted-user (-> updated-user
                                   db-adapter/convert-pg-objects
                                   db-adapter/convert-db-keys->app-keys)]
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

        (let [created-user (admin-service/create-user! db user-data
                             (:id admin)
                             ip-address
                             user-agent)]

          (utils/log-admin-action "create_user" (:id admin) "user"
            (:id created-user) user-data)

          (let [converted-user (-> created-user
                                 db-adapter/convert-pg-objects
                                 db-adapter/convert-db-keys->app-keys)]
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
                result (admin-service/delete-user! db user-id
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
