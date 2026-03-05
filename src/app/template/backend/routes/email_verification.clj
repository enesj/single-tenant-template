(ns app.template.backend.routes.email-verification
  "Email verification API routes"
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.template.backend.auth.email-verification :as email-verify]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.tenant :as tenant-svc]
    [app.shared.data :as shared-data]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn- maybe-build-session-update
  [req db-conn config result]
  (let [session-user    (get-in req [:session :auth-session :user])
        session-user-id (or (:id session-user) (:users/id session-user))
        verified-user-id (:user-id result)]
    (when (and session-user verified-user-id
            (= (str session-user-id) (str verified-user-id)))
      (let [merged-user  (cond-> session-user
                           (:email result) (assoc :email (:email result))
                           (:full_name result) (assoc :full_name (:full_name result))
                           true (assoc :email_verified true))
            tenant-ctx   (tenant-auth/resolve-tenant-context db-conn config merged-user)
            auth-session (shared-data/sanitize-for-serialization
                           (tenant-auth/build-auth-session {:user merged-user} tenant-ctx))
            redirect-url (case (:action tenant-ctx)
                           :selection-required "/tenant-select"
                           :no-tenant "/tenant-select"
                           (domain-registry/get-post-login-path))]
        {:session (assoc (or (:session req) {}) :auth-session auth-session)
         :redirect-url redirect-url}))))

(defn verify-email-handler
  "Handle email verification from URL token.
   On success, provisions a workspace if the user has no existing memberships."
  [db email-service config]
  (fn [req]
    (try
      (let [token   (get-in req [:query-params "token"])
            ;; `db` is a db-adapter (PostgresAdapter) in DI wiring; tenant service
            ;; uses raw next.jdbc and needs the underlying DataSource/connection.
            db-conn (or (:connection db) db)]
        (if (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Missing verification token"})}

          (let [result         (email-verify/verify-email-token! db token)
                already-used?  (= :token-already-used (:error result))
                should-proceed? (or (:success result) already-used?)
                user-id        (:user-id result)
                user-email     (or (:email result)
                                 (get-in req [:session :auth-session :user :email]))
                user-full-name (or (:full_name result)
                                 (get-in req [:session :auth-session :user :full_name]))]
            (if should-proceed?
              (do
                (if already-used?
                  (log/info "Email verification link already used for user" user-id)
                  (log/info "Email verification successful for user" user-id))

                ;; Ensure workspace exists for verified users with no memberships.
                ;; This makes the verification link idempotent (safe to click again).
                (let [memberships (when user-id
                                    (tenant-svc/get-user-memberships db-conn user-id))]
                  (when (and user-id (empty? memberships))
                    (if-not user-email
                      (do
                        (log/error "Cannot provision workspace after email verification: user email missing"
                          {:user-id user-id})
                        (throw (ex-info "Verified user email missing"
                                 {:type :workspace-provisioning-failed
                                  :user-id user-id})))
                      (try
                        (tenant-svc/provision-tenant! db-conn config
                          {:id user-id
                           :email user-email
                           :full_name user-full-name})
                        (log/info "Provisioned workspace for newly verified user" user-email)
                        (catch Exception e
                          (log/error e "Failed to provision workspace after email verification")
                          (throw (ex-info "Workspace provisioning failed"
                                   {:type :workspace-provisioning-failed
                                    :user-id user-id}
                                   e)))))))

                ;; Send success notification email (non-critical) only on first-time verification
                (when (and (:success result) email-service user-email)
                  (try
                    (let [email-result (email-verify/send-verification-success-email
                                         email-service
                                         {:email user-email
                                          :full_name user-full-name})]
                      (if (:success email-result)
                        (log/info "Success notification email sent")
                        (log/warn "Failed to send verification success email (non-critical)"
                          email-result)))
                    (catch Exception e
                      (log/warn "Failed to send verification success email (non-critical):" (.getMessage e)))))

                (let [{:keys [session redirect-url]} (maybe-build-session-update req db-conn config
                                                    (assoc result
                                                      :email user-email
                                                      :full_name user-full-name))]
                  (cond-> {:status 302
                           :headers {"Location" (or redirect-url "/email-verified?success=true")}}
                    session (assoc :session session))))

              {:status 302
               :headers {"Location" (str "/email-verified?error=" (name (:error result)))}}))))

      (catch clojure.lang.ExceptionInfo e
        (if (= :workspace-provisioning-failed (:type (ex-data e)))
          {:status 302
           :headers {"Location" "/email-verified?error=workspace-provisioning-failed"}}
          (do
            (log/error e "Error in verify-email handler")
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:error "Internal server error"})})))
      (catch Exception e
        (log/error e "Error in verify-email handler")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn resend-verification-handler
  "Handle resending verification email"
  [db email-service]
  (fn [req]
    (route-utils/with-error-handling "resend-verification"
      (let [auth-session (get-in req [:session :auth-session])
            user (:user auth-session)]

        (cond
          (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (:email_verified user)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Email already verified"})}

          :else
          (try
            (let [token (email-verify/resend-verification-token! db (:id user))]

              ;; Mark user as pending verification
              (email-verify/mark-user-verification-pending! db (:id user))

              ;; Send verification email
              (let [email-result (email-verify/send-verification-email
                                   email-service
                                   user
                                   token)]
                (if (:success email-result)
                  {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body (json/generate-string {:success true :message "Verification email sent"})}

                  {:status 500
                   :headers {"Content-Type" "application/json"}
                   :body (json/generate-string {:error "Failed to send verification email"})})))

            (catch Exception e
              (log/error e "Error resending verification email for user" (:id user))
              {:status 500
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error "Internal server error"})})))))))

(defn verification-status-handler
  "Get current verification status for authenticated user"
  [db]
  (fn [req]
    (route-utils/with-error-handling "verification-status"
      (let [auth-session (get-in req [:session :auth-session])
            user (:user auth-session)]

        (if (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (let [status (email-verify/get-user-verification-status db (:id user))]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string
                     {:email-verified (:email-verified status)
                      :verification-status (:verification-status status)
                      :needs-verification (email-verify/user-needs-verification? user)})}))))))
