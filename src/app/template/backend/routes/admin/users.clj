(ns app.template.backend.routes.admin.users
  "Admin basic user management handlers"
  (:require
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.identity-reveal :as identity-reveal]
    [app.admin.backend.services.admin.users :as admin-users]
    [app.admin.backend.services.admin.users.deletion :as user-deletion]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.tenant :as tenant-svc]
    [taoensso.timbre :as log]))

(defn list-users-handler
  "List all users (single-tenant)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            sort (utils/extract-sort-params params)
            date-ranges (utils/extract-date-range-params params
                          [:created-at :updated-at :last-login-at])
            filters (merge {:search (:search params)
                            :status (:status params)
                            :full-name (:full-name params)
                            :email-verified (utils/parse-boolean-param params :email-verified)}
                      date-ranges)
            {:keys [users total limit offset]}
            (admin-users/list-all-users-page db (merge filters pagination sort))]
        (log/info "👥 Admin list-users returned" (count users) "users"
          {:filters filters :pagination pagination :total total})
        (let [converted-users (shared-db/to-app users)]
          (utils/json-response {:users converted-users
                                :total total
                                :limit limit
                                :offset offset}))))
    "Failed to retrieve users"))

(defn get-user-details-handler
  "Get detailed user information, enriched with tenant memberships."
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [user-id _request]
          (if-let [user (admin-users/get-user-details db user-id)]
            (let [converted-user (shared-db/to-app user)
                  memberships (try
                                (->> (tenant-svc/get-user-memberships db user-id)
                                  (mapv email-privacy/routine-membership-view)
                                  shared-db/to-app)
                                (catch Exception e
                                  (log/warn "Failed to fetch memberships for user" user-id (.getMessage e))
                                  []))]
              (utils/json-response {:user converted-user
                                    :memberships memberships}))
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
                               (:user-agent context))
                audit-updates (cond-> (dissoc updates :email)
                                (:email updates) (merge (email-privacy/redact-email-change (:email updates))))]

            (utils/log-admin-action "update_user" (-> context :admin :id)
              "user" user-id audit-updates)

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
            user-data (:body request)
            audit-user-data (cond-> (dissoc user-data :email)
                              (:email user-data) (merge (email-privacy/redact-email-change (:email user-data))))]

        (log/info "Admin create user request"
          {:email-masked (email-privacy/mask-email (:email user-data))
           :status (:status user-data)})

        (let [created-user (admin-users/create-user! db user-data
                             (:id admin)
                             ip-address
                             user-agent)]

          (utils/log-admin-action "create_user" (:id admin) "user"
            (:id created-user) audit-user-data)

          (let [converted-user (shared-db/to-app created-user)]
            (utils/json-response {:user converted-user})))))
    "Failed to create user"))

(defn batch-delete-users-handler
  "Batch delete users; validates each and collects per-user results.
  Returns {:data {:deleted-count n :deleted-ids [...] :errors [...]}}
  or 400 when all deletions failed."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent admin]} (utils/extract-request-context request)
            body (:body request)
            request-params (:params request)
            query-params (:query-params request)
            raw-ids* (or (:ids body)
                       (:user_ids body)
                       (:user-ids body)
                       (:userIds body)
                       (get body "ids")
                       (get body "user_ids")
                       (get body "user-ids")
                       (get body "userIds")

                       (:ids request-params)
                       (:user_ids request-params)
                       (:user-ids request-params)
                       (:userIds request-params)
                       (get request-params "ids")
                       (get request-params "user_ids")
                       (get request-params "user-ids")
                       (get request-params "userIds")

                       (:ids query-params)
                       (:user_ids query-params)
                       (:user-ids query-params)
                       (:userIds query-params)
                       (get query-params "ids")
                       (get query-params "user_ids")
                       (get query-params "user-ids")
                       (get query-params "userIds"))
            raw-ids (cond
                      (nil? raw-ids*) []
                      (sequential? raw-ids*) raw-ids*
                      (string? raw-ids*) [raw-ids*]
                      :else [raw-ids*])
            user-ids (->> raw-ids (map utils/parse-uuid-custom) (filter some?) vec)
            force-delete (boolean (:force-delete body))
            dry-run (boolean (:dry-run body))]
        (cond
          (empty? raw-ids)
          (utils/error-response "No user ids provided" :status 400)

          (empty? user-ids)
          (utils/error-response "One or more user ids are invalid" :status 400)

          :else
          (let [deleted-ids (atom [])
                errors (atom [])]
            (doseq [user-id user-ids]
              (try
                (let [result (user-deletion/delete-user!
                               db
                               user-id
                               (:id admin)
                               ip-address
                               user-agent
                               :force-delete force-delete
                               :dry-run dry-run)]
                  (cond
                    (:error result)
                    (swap! errors conj {:id (str user-id)
                                        :error (:message result)
                                        :details (dissoc result :error :message :status)})

                    (:dry-run result)
                    (swap! errors conj {:id (str user-id)
                                        :error "dry-run"
                                        :details (select-keys result [:message :impact-summary])})

                    (:success result)
                    (swap! deleted-ids conj (str user-id))

                    :else
                    (swap! errors conj {:id (str user-id)
                                        :error "Unexpected deletion result"})))
                (catch clojure.lang.ExceptionInfo e
                  (let [data (ex-data e)
                        status (:status data 400)]
                    (swap! errors conj {:id (str user-id)
                                        :error (ex-message e)
                                        :status status
                                        :details data})))
                (catch Exception e
                  (swap! errors conj {:id (str user-id)
                                      :error (.getMessage e)}))))

            (utils/log-admin-action "batch_delete_users" (:id admin)
              "users" nil {:count (count @deleted-ids)
                           :force-delete force-delete
                           :dry-run dry-run
                           :ids (vec @deleted-ids)})

            (if (and (empty? @deleted-ids) (seq @errors))
              (utils/error-response (-> @errors first :error) :status 400
                :details {:errors (vec @errors)})
              (utils/success-response
                {:data {:deleted-count (count @deleted-ids)
                        :deleted-ids (vec @deleted-ids)
                        :errors (vec @errors)}}))))))
    "Failed to batch delete users"))

(defn reveal-user-email-handler
  "Reveal a user's email for exceptional owner-only break-glass workflows.

   Requires a structured reason code plus details and always emits an audit entry."
  [db]
  (utils/with-validation-error-handling
    (fn [request]
      (utils/handle-uuid-body-request request :id
        (fn [user-id body context _request]
          (let [result (identity-reveal/reveal-email! db :user user-id
                         {:admin-id (-> context :admin :id)
                          :reason-code (or (:reason-code body) (:reason_code body))
                          :reason (:reason body)
                          :ip-address (:ip-address context)
                          :user-agent (:user-agent context)})]
            (utils/json-response result)))))
    "Failed to reveal user email"))

;; Route definitions
(defn routes
  "Basic user management route definitions"
  [db]
  [""
   ["" {:get (list-users-handler db)
        :post (create-user-handler db)}]
   ["/batch" {:delete (batch-delete-users-handler db)}]
   ["/:id"
    {:get (get-user-details-handler db)
     :put (update-user-handler db)}]
   ["/:id/reveal-email"
    {:post {:middleware [#(admin-mw/wrap-admin-role % :owner)]
            :handler (reveal-user-email-handler db)}}]])
