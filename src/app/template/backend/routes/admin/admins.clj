(ns app.template.backend.routes.admin.admins
  "Admin management API handlers - allows owners to manage other admins.
   
   Routes:
   - GET  /admin/api/admins       - List all admins
   - POST /admin/api/admins       - Create a new admin
   - GET  /admin/api/admins/:id   - Get admin details
   - PUT  /admin/api/admins/:id   - Update admin info
   - DELETE /admin/api/admins/:id - Delete admin
   - PUT  /admin/api/admins/:id/role   - Update admin role
   - PUT  /admin/api/admins/:id/status - Update admin status"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.admins :as admin-admins]
    [app.shared.adapters.database :as shared-db]
    [taoensso.timbre :as log]))

;; ============================================================================
;; List Admins
;; ============================================================================

(defn list-admins-handler
  "List all admins with optional filtering"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            filters {:search (:search params)
                     :status (:status params)
                     :role (:role params)}
            admins (admin-admins/list-all-admins db (merge filters pagination))
            total (admin-admins/get-admin-count db filters)]
        (log/info "👥 Admin list-admins returned" (count admins) "admins"
          {:filters filters :pagination pagination})
        (let [converted-admins (shared-db/to-app admins)]
          (utils/json-response {:admins converted-admins
                                :total total}))))
    "Failed to retrieve admins"))

;; ============================================================================
;; Get Admin Details
;; ============================================================================

(defn get-admin-details-handler
  "Get detailed admin information"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [admin-id _request]
          (if-let [admin (admin-admins/get-admin-details db admin-id)]
            (let [converted-admin (shared-db/to-app admin)]
              (utils/json-response {:admin converted-admin}))
            (utils/error-response "Admin not found" :status 404)))))
    "Failed to retrieve admin details"))

;; ============================================================================
;; Create Admin
;; ============================================================================

(defn create-admin-handler
  "Create a new admin"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
            admin-data (:body request)
            requested-role (some-> (:role admin-data) str)]

        (log/info "Admin create request:" {:email (:email admin-data) :role requested-role})

        ;; Basic validation
        (when-not (:email admin-data)
          (throw (ex-info "Email is required" {:status 400 :field :email})))
        (when-not (:password admin-data)
          (throw (ex-info "Password is required" {:status 400 :field :password})))
        (when (and requested-role
                (not (#{"admin" "support" "owner"} requested-role)))
          (throw (ex-info "Invalid role. Must be one of: admin, support, owner"
                   {:status 400 :field :role :allowed ["admin" "support" "owner"]})))

        (let [created-admin (admin-admins/create-admin! db admin-data
                              (:id admin)
                              ip-address
                              user-agent)]

          (utils/log-admin-action "create_admin" (:id admin) "admin"
            (:id created-admin) (dissoc admin-data :password))

          (let [converted-admin (shared-db/to-app created-admin)]
            (utils/json-response {:admin converted-admin} :status 201)))))
    "Failed to create admin"))

;; ============================================================================
;; Update Admin
;; ============================================================================

(defn update-admin-handler
  "Update admin information (non-sensitive fields)"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [admin-id updates context _request]
          (let [updated-admin (admin-admins/update-admin! db admin-id updates
                                (-> context :admin :id)
                                (:ip-address context)
                                (:user-agent context))]

            (utils/log-admin-action "update_admin" (-> context :admin :id)
              "admin" admin-id updates)

            (let [converted-admin (shared-db/to-app updated-admin)]
              (utils/json-response {:admin converted-admin}))))))
    "Failed to update admin"))

;; ============================================================================
;; Delete Admin
;; ============================================================================

(defn delete-admin-handler
  "Delete an admin"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [admin-id _request]
          (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
                result (admin-admins/delete-admin! db admin-id
                         (:id admin)
                         ip-address
                         user-agent)]

            (utils/log-admin-action "delete_admin" (:id admin)
              "admin" admin-id nil)

            (if (:success result)
              (utils/success-response {:message (:message result)})
              (utils/error-response (:message result) :status 400))))))
    "Failed to delete admin"))

(defn batch-delete-admins-handler
  "Delete multiple admins.

  Expects JSON body like:
  {:ids [<uuid-str> ...]}"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
            body (:body request)
            ids (or (:ids body) [])
            admin-ids (->> ids (map utils/parse-uuid-custom) (filter some?) vec)]
        (if (empty? admin-ids)
          (utils/error-response "No valid IDs provided" :status 400)
          (let [deleted-ids (atom [])
                errors (atom [])]
            (doseq [admin-id admin-ids]
              (try
                (let [result (admin-admins/delete-admin! db admin-id
                               (:id admin)
                               ip-address
                               user-agent)]
                  (if (:success result)
                    (swap! deleted-ids conj (str admin-id))
                    (swap! errors conj {:id (str admin-id)
                                        :error (:message result)})))
                (catch Exception e
                  (swap! errors conj {:id (str admin-id)
                                      :error (.getMessage e)}))))

            (utils/log-admin-action "batch_delete_admins" (:id admin)
              "admins" nil {:count (count @deleted-ids)
                            :ids (vec @deleted-ids)})

            (utils/success-response
              {:data {:deleted-count (count @deleted-ids)
                      :deleted-ids (vec @deleted-ids)
                      :errors (vec @errors)}})))))
    "Failed to batch delete admins"))

;; ============================================================================
;; Update Admin Role
;; ============================================================================

(defn update-admin-role-handler
  "Update an admin's role"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [admin-id body context _request]
          (let [new-role (:role body)]
            (when-not new-role
              (throw (ex-info "Role is required" {:status 400 :field :role})))

            (when-not (#{"admin" "support" "owner"} new-role)
              (throw (ex-info "Invalid role. Must be one of: admin, support, owner"
                       {:status 400 :field :role :allowed ["admin" "support" "owner"]})))

            (let [updated-admin (admin-admins/update-admin-role! db admin-id new-role
                                  (-> context :admin :id)
                                  (:ip-address context)
                                  (:user-agent context))]

              (utils/log-admin-action "update_admin_role" (-> context :admin :id)
                "admin" admin-id {:new-role new-role})

              (let [converted-admin (shared-db/to-app updated-admin)]
                (utils/json-response {:admin converted-admin})))))))
    "Failed to update admin role"))

;; ============================================================================
;; Update Admin Status
;; ============================================================================

(defn update-admin-status-handler
  "Update an admin's status"
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [admin-id body context _request]
          (let [new-status (:status body)]
            (when-not new-status
              (throw (ex-info "Status is required" {:status 400 :field :status})))

            (when-not (#{"active" "suspended"} new-status)
              (throw (ex-info "Invalid status. Must be one of: active, suspended"
                       {:status 400 :field :status :allowed ["active" "suspended"]})))

            (let [updated-admin (admin-admins/update-admin-status! db admin-id new-status
                                  (-> context :admin :id)
                                  (:ip-address context)
                                  (:user-agent context))]

              (utils/log-admin-action "update_admin_status" (-> context :admin :id)
                "admin" admin-id {:new-status new-status})

              (let [converted-admin (shared-db/to-app updated-admin)]
                (utils/json-response {:admin converted-admin})))))))
    "Failed to update admin status"))

;; ============================================================================
;; Route Definitions
;; ============================================================================

(defn routes
  "Admin management route definitions"
  [db]
  ["/admins"
   ["" {:get (list-admins-handler db)
        :post (create-admin-handler db)}]
   ["/batch" {:delete (batch-delete-admins-handler db)}]
   ["/:id"
    {:get (get-admin-details-handler db)
     :put (update-admin-handler db)}]
   ["/:id/role"
    {:put (update-admin-role-handler db)}]
   ["/:id/status"
    {:put (update-admin-status-handler db)}]])
