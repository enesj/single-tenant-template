(ns app.backend.routes.admin.entities
  "Admin entity CRUD endpoints for cross-tenant operations"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.shared.adapters.database :as db-adapter]
    [taoensso.timbre :as log]))

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
          {:force-delete force-delete :dry-run dry-run :source-flags {:body (select-keys body [:force-delete :dry-run])}
           :query (select-keys query-params [:force-delete :dry-run])})

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       ;; Use the new comprehensive user deletion function
                       (try
                         (let [deletion-result (admin-service/delete-user!
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

                       ;; Default case for unsupported entity types
                       {:error true
                        :status 501
                        :message "Delete operation not supported for this entity type"
                        :supported-entities ["users"]
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
                  (-> payload
                    db-adapter/convert-pg-objects
                    db-adapter/convert-db-keys->app-keys))))

            ;; Handle successful deletion
            (:success result)
            (do
              (log/info "Successfully deleted" entity id "by admin" (:email admin))
              ;; Additional audit logging is already handled in the service layer
              (let [entity-key (case entity
                                 "users" :deleted-user
                                 nil)
                    payload (cond-> {:message (:message result)
                                     :deleted-entity entity
                                     :deleted-id id
                                     :deleted-at (:deleted-at result)
                                     :impact-summary (:impact-summary result)}
                              entity-key (assoc entity-key (get result (case entity
                                                                         "users" :user
                                                                         nil))))]
                (utils/success-response
                  (-> payload
                    db-adapter/convert-pg-objects
                    db-adapter/convert-db-keys->app-keys))))

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
            data (:body request)  ; FIXED: Use :body instead of :body-params
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])]

        (log/info "Admin" (:email admin) "creating" entity "with data:" data)

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (try
                         (admin-service/create-user! db data (:id admin) ip-address user-agent)
                         (catch Exception e
                           (let [data (ex-data e)]
                             (if (= 400 (:status data))
                               (throw e)
                               (throw (ex-info "Failed to create entity"
                                        {:status 500 :original-error (.getMessage e)}))))))

                       ;; For other entity types, add cases here
                       ;; "tenants" (admin-service/create-tenant! db data (:id admin) ip-address user-agent)
                       nil)]

          (if result
            (do
              (log/info "Successfully created" entity "by admin" (:email admin))
              ;; Log admin action with request context
              (utils/log-admin-action-with-context
                "create_entity" (:id admin) entity (:id result)
                {:entity-type entity :data data :result result}
                ip-address user-agent)
              (utils/success-response
                (-> result
                  db-adapter/convert-pg-objects
                  db-adapter/convert-db-keys->app-keys)))
            (utils/error-response (str "Failed to create " entity) :status 400)))))
    "Failed to create entity"))

(defn get-entity-handler
  "Get entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [entity id]} (:path-params request)
            admin (:admin request)]

        (log/info "Admin" (:email admin) "requesting" entity "with id" id)

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (admin-service/get-user-details db (utils/parse-uuid-custom id))

                       ;; For other entity types, add cases here
                       ;; "tenants" (admin-service/get-tenant-details db id)
                       nil)]

          (if result
            (utils/success-response
              (-> result
                db-adapter/convert-pg-objects
                db-adapter/convert-db-keys->app-keys))
            (utils/error-response (str (name entity) " not found") :status 404)))))
    "Failed to get entity"))

(defn update-entity-handler
  "Update entity with admin privileges (cross-tenant)"
  [db _crud-service]
  (utils/with-validation-error-handling
    (fn [request]
      (let [{:keys [entity id]} (:path-params request)
            admin (:admin request)
            updates (:body request)  ; FIXED: Use :body instead of :body-params
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])]

        (log/info "Admin" (:email admin) "updating" entity id "with:" updates)

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (try
                         (admin-service/update-user! db (utils/parse-uuid-custom id) updates
                           (:id admin) ip-address user-agent)
                         (catch Exception e
                           (let [data (ex-data e)]
                             (if (= 404 (:status data))
                               (throw e)
                               (throw (ex-info "Failed to update entity"
                                        {:status 500 :original-error (.getMessage e)}))))))

                       ;; For other entity types, add cases here
                       ;; "tenants" (admin-service/update-tenant! db id updates (:id admin) ip-address user-agent)
                       nil)]

          (if result
            (do
              (log/info "Successfully updated" entity id "by admin" (:email admin))
              ;; Log admin action with request context
              (utils/log-admin-action-with-context
                "update_entity" (:id admin) entity id
                {:entity-type entity :changes updates :result result}
                ip-address user-agent)
              (utils/success-response
                (-> result
                  db-adapter/convert-pg-objects
                  db-adapter/convert-db-keys->app-keys)))
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

        (log/info "Admin" (:email admin) "listing" entity "with filters:" query-params)

        ;; Route to appropriate admin service based on entity type
        (let [result (case entity
                       "users"
                       (let [pagination (utils/extract-pagination-params query-params)
                             filters {:search (:search query-params)
                                      :status (:status query-params)
                                      :email-verified (utils/parse-boolean-param query-params :email-verified)}]
                         (admin-service/list-all-users db (merge filters pagination)))

                       ;; Default case - return empty for unsupported entities
                       [])]

          (utils/success-response
            (-> (if (= entity "users")
                  {:users result}
                  result)
              db-adapter/convert-pg-objects
              db-adapter/convert-db-keys->app-keys)))))
    "Failed to list entities"))

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
            _ (log/info "Admin" (:email admin) "batch checking deletion constraints for" entity
                {:count (count entity-ids)})
           result (case entity
                    "users" (admin-service/check-users-deletion-constraints-batch db entity-ids)
                    {:error true
                     :status 501
                     :message "Batch deletion constraints not supported for this entity type"
                      :supported-entities ["users"]
                      :requested-entity entity})]
        (if (:error result)
          (utils/error-response (:message result) :status (:status result))
          ;; Ensure FE can access results under :data consistently
          (utils/json-response
            (-> {:success true :data {:results (:results result)}}
              db-adapter/convert-pg-objects
              db-adapter/convert-db-keys->app-keys)))))
    "Failed to batch-check deletion constraints"))

(defn routes
  "Admin entity CRUD route definitions"
  [db crud-service]
  ["/entities"
   ["/:entity/deletion-constraints/batch"
    {:post (check-deletion-constraints-batch-handler db crud-service)}]
   ["/:entity"
    {:get (list-entities-handler db crud-service)
     :post (create-entity-handler db crud-service)}]

   ["/:entity/:id"
    {:get    (get-entity-handler db crud-service)
     :put    (update-entity-handler db crud-service)
     :delete (delete-entity-handler db crud-service)}]])
