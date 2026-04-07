(ns app.template.backend.routes.admin.entities
  "Admin entity CRUD endpoints for cross-tenant operations"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.routes.admin.entities.backlog :as backlog]
    [app.admin.backend.services.admin.users :as admin-users]
    [app.admin.backend.services.admin.users.deletion :as user-deletion]
    [app.admin.backend.services.admin.users.validation :as user-validation]
    [app.template.backend.security.email :as email-privacy]
    [app.shared.adapters.database :as shared-db]
    [taoensso.timbre :as log]))

(defn- redact-email-fields
  [payload]
  (if-not (map? payload)
    payload
    (cond-> (dissoc payload :email)
      (:email payload) (merge (email-privacy/redact-email-change (:email payload))))))

(defn delete-entity-handler
  "Delete entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity id]} (:path-params request)
            admin (:admin request)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])
            ;; Accept flags from either JSON body or query string for flexibility
            body (:body request)
            query-params (:query-params request)
            ;; JSON body booleans come in as true/false; query params as strings
            force-delete (boolean (or (true? (:force-delete body))
                                    (= true (:force-delete query-params))
                                    (= "true" (:force-delete query-params))))
            dry-run (boolean (or (true? (:dry-run body))
                               (= true (:dry-run query-params))
                               (= "true" (:dry-run query-params))))]

        (log/info "Admin" (:email admin) "attempting to delete" entity "with id" id
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :entity-id id
           :force-delete force-delete
           :dry-run dry-run
           :source-flags {:body (select-keys body [:force-delete :dry-run])}
           :query (select-keys query-params [:force-delete :dry-run])})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       ;; Use the new comprehensive user deletion function
                       (try
                         (let [deletion-result (user-deletion/delete-user!
                                                 db
                                                 (utils/parse-uuid-custom id)
                                                 (:id admin)
                                                 ip-address
                                                 user-agent
                                                 :force-delete force-delete
                                                 :dry-run dry-run)]
                           deletion-result)
                         (catch clojure.lang.ExceptionInfo e
                           (let [error-data (ex-data e)
                                 status (:status error-data 400)]
                             (log/warn "User deletion constraint violation:"
                               "reason:" (:reason error-data)
                               "message:" (ex-message e))
                             ;; Return detailed error information to help admin understand the constraint
                             {:error true
                              :status status
                              :message (ex-message e)
                              :error-details error-data})))

                       "backlog"
                       (let [backlog-number (backlog/parse-backlog-number! id)
                             deleted-row (backlog/delete-backlog-item! db backlog-number)]
                         (if deleted-row
                           {:success true
                            :message "Backlog item deleted successfully"
                            :deleted-at (java.time.Instant/now)
                            :backlog deleted-row}
                           {:error true
                            :status 404
                            :message "Backlog item not found"}))

                       ;; Default case for unsupported entity types
                       {:error true
                        :status 501
                        :message "Delete operation not supported for this entity type"
                        :supported-entities ["users" "backlog"]
                        :requested-entity entity})]

          (cond
            ;; Handle errors from the deletion operation
            (:error result)
            (utils/error-response
              (:message result)
              :status (:status result)
              :details (dissoc result :error :message :status))

            ;; Handle dry-run success
            (:dry-run result)
            (do
              (log/info "Dry-run deletion analysis completed for" entity id
                {:impact (:impact-summary result)})
              (let [entity-key (case entity
                                 "users" :user
                                 nil)
                    payload (cond-> {:message (:message result)
                                     :dry-run true
                                     :can-delete? true
                                     :constraints []
                                     :warnings []
                                     :impact-analysis (:impact-summary result)}
                              entity-key (assoc entity-key (get result entity-key)))]
                ;; Normalize response so FE constraint checker can consume it easily
                (utils/success-response
                  (shared-db/to-app payload))))

            ;; Handle successful deletion
            (:success result)
            (do
              (log/info "Successfully deleted entity"
                {:admin-id (:id admin)
                 :admin-ref (email-privacy/admin-ref (:id admin))
                 :entity entity
                 :entity-id id})
              ;; Additional audit logging is already handled in the service layer
              (let [entity-key (case entity
                                 "users" :deleted-user
                                 "backlog" :deleted-backlog
                                 nil)
                    payload (cond-> {:message (:message result)
                                     :deleted-entity entity
                                     :deleted-id id
                                     :deleted-at (:deleted-at result)
                                     :impact-summary (:impact-summary result)}
                              entity-key (assoc entity-key (get result (case entity
                                                                         "users" :user
                                                                         "backlog" :backlog
                                                                         nil))))]
                (utils/success-response
                  (shared-db/to-app payload))))

            ;; Fallback for unexpected result format
            :else
            (do
              (log/error "Unexpected result format from deletion operation:" result)
              (utils/error-response "Unexpected error during deletion" :status 500))))))
    "Failed to delete entity"))

(defn create-entity-handler
  "Create entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-validation-error-handling
    (fn [request]
      (let [{:keys [entity]} (:path-params request)
            admin (:admin request)
            data (:body request)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])]

        (log/info "Admin creating entity"
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :data (redact-email-fields data)})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (try
                         (admin-users/create-user! db data (:id admin) ip-address user-agent)
                         (catch Exception e
                           (let [data (ex-data e)]
                             (if (= 400 (:status data))
                               (throw e)
                               (throw (ex-info "Failed to create entity"
                                        {:status 500 :original-error (.getMessage e)}))))))

                       "backlog"
                       (backlog/create-backlog-item! db (backlog/normalize-backlog-create-payload data))

                       ;; For other entity types, add cases here
                       ;; "tenants" (TODO: implement tenant create handler)
                       nil)]

          (if result
            (do
              (log/info "Successfully created entity"
                {:admin-id (:id admin)
                 :admin-ref (email-privacy/admin-ref (:id admin))
                 :entity entity})
              ;; Log admin action with request context
              (utils/log-admin-action-with-context
                "create_entity" (:id admin) entity (or (:id result) (:number result))
                {:entity-type entity :data (redact-email-fields data) :result result}
                ip-address user-agent)
              (utils/success-response
                (shared-db/to-app result)))
            (utils/error-response (str "Failed to create " entity) :status 400)))))
    "Failed to create entity"))

(defn get-entity-handler
  "Get entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity id]} (:path-params request)
            admin (:admin request)]

        (log/info "Admin requesting entity"
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :entity-id id})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (admin-users/get-user-details db (utils/parse-uuid-custom id))

                       "backlog"
                       (backlog/get-backlog-item db (backlog/parse-backlog-number! id))

                       ;; For other entity types, add cases here
                       ;; "tenants" (TODO: implement tenant details handler)
                       nil)]

          (if result
            (utils/success-response
              (shared-db/to-app result))
            (utils/error-response (str (name entity) " not found") :status 404)))))
    "Failed to get entity"))

(defn update-entity-handler
  "Update entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-validation-error-handling
    (fn [request]
      (let [{:keys [entity id]} (:path-params request)
            admin (:admin request)
            updates (:body request)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])]

        (log/info "Admin updating entity"
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :entity-id id
           :updates (redact-email-fields updates)})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (try
                         (admin-users/update-user! db (utils/parse-uuid-custom id) updates
                           (:id admin) ip-address user-agent)
                         (catch Exception e
                           (let [data (ex-data e)]
                             (if (= 404 (:status data))
                               (throw e)
                               (throw (ex-info "Failed to update entity"
                                        {:status 500 :original-error (.getMessage e)}))))))

                       "backlog"
                       (backlog/update-backlog-item!
                         db
                         (backlog/parse-backlog-number! id)
                         (backlog/normalize-backlog-update-payload updates))

                       ;; For other entity types, add cases here
                       ;; "tenants" (TODO: implement tenant update handler)
                       nil)]

          (if result
            (do
              (log/info "Successfully updated entity"
                {:admin-id (:id admin)
                 :admin-ref (email-privacy/admin-ref (:id admin))
                 :entity entity
                 :entity-id id})
              ;; Log admin action with request context
              (utils/log-admin-action-with-context
                "update_entity" (:id admin) entity id
                {:entity-type entity :changes (redact-email-fields updates) :result result}
                ip-address user-agent)
              (utils/success-response
                (shared-db/to-app result)))
            (utils/error-response (str (name entity) " not found") :status 404)))))
    "Failed to update entity"))

(defn list-entities-handler
  "List entities with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity]} (:path-params request)
            admin (:admin request)
            query-params (:query-params request)]

        (log/info "Admin listing entities"
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :filters (redact-email-fields query-params)})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (let [pagination (utils/extract-pagination-params query-params)
                             filters {:search (:search query-params)
                                      :status (:status query-params)
                                      :email-verified (utils/parse-boolean-param query-params :email-verified)}]
                         (admin-users/list-all-users db (merge filters pagination)))

                       "backlog"
                       (backlog/list-backlog-items db)

                       ;; Default case - return empty for unsupported entities
                       [])]

          (utils/success-response
            (shared-db/to-app
              (case entity
                "users" {:users result}
                "backlog" {:backlog result}
                result))))))
    "Failed to list entities"))

(defn batch-delete-entities-handler
  "Batch delete entities with admin privileges.
   Currently supports entity = \"backlog\" with JSON body {:ids [..]}"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity]} (:path-params request)
            admin (:admin request)
            ids (or (get-in request [:body :ids]) [])]
        (log/info "Admin batch deleting entities"
          {:admin-id (:id admin)
           :admin-ref (email-privacy/admin-ref (:id admin))
           :entity entity
           :count (count ids)})

        (let [result (case entity
                       "backlog"
                       (let [numbers (->> ids
                                       (map backlog/parse-backlog-number!)
                                       distinct
                                       vec)
                             deleted-count (backlog/batch-delete-backlog-items! db numbers)]
                         {:success true
                          :message (str deleted-count " backlog items deleted successfully")
                          :deleted-count deleted-count})

                       {:error true
                        :status 501
                        :message "Batch delete operation not supported for this entity type"
                        :supported-entities ["backlog"]
                        :requested-entity entity})]
          (if (:error result)
            (utils/error-response
              (:message result)
              :status (:status result)
              :details (dissoc result :error :message :status))
            (utils/success-response
              (shared-db/to-app result))))))
    "Failed to batch delete entities"))

(defn check-deletion-constraints-batch-handler
  "Batch-check deletion constraints for given entity type.
   Currently supports entity = \"users\". Expects JSON body {:user_ids [uuid-str ...]}"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity]} (:path-params request)
            admin (:admin request)
            body (:body request)
            ids-raw (case entity
                      "users" (:user_ids body)
                      (:user_ids body))
            entity-ids (->> ids-raw (map utils/parse-uuid-custom) (filter some?) vec)
            _ (log/info "Admin batch checking deletion constraints"
              {:admin-id (:id admin)
               :admin-ref (email-privacy/admin-ref (:id admin))
               :entity entity
               :count (count entity-ids)})
            result (case entity
                     "users" (user-validation/check-users-deletion-constraints-batch db entity-ids)
                     {:error true
                      :status 501
                      :message "Batch deletion constraints not supported for this entity type"
                      :supported-entities ["users"]
                      :requested-entity entity})]
        (if (:error result)
          (utils/error-response (:message result) :status (:status result))
          ;; Ensure FE can access results under :data consistently
          (utils/json-response
            (shared-db/to-app {:success true :data {:results (:results result)}})))))
    "Failed to batch-check deletion constraints"))

(defn routes
  "Admin entity CRUD route definitions"
  [db crud-service]
  ["/entities"
   ["/:entity/deletion-constraints/batch"
    {:post (check-deletion-constraints-batch-handler db crud-service)}]

   ["/:entity/batch"
    {:delete (batch-delete-entities-handler db crud-service)}]

   ["/:entity"
    {:get (list-entities-handler db crud-service)
     :post (create-entity-handler db crud-service)}]

   ["/:entity/:id"
    {:get (get-entity-handler db crud-service)
     :put (update-entity-handler db crud-service)
     :delete (delete-entity-handler db crud-service)}]])
