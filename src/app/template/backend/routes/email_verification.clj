(ns app.template.backend.routes.email-verification
  "Email verification API routes"
  (:require
    [app.template.backend.auth.email-verification :as email-verify]
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.tenant :as tenant-svc]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

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
                user-id        (:user-id result)]
            (if should-proceed?
              (do
                (if already-used?
                  (log/info "Email verification link already used for user" user-id)
                  (log/info "Email verification successful for user" user-id))

                ;; Ensure workspace exists for verified users with no memberships.
                ;; This makes the verification link idempotent (safe to click again).
                (when user-id
                  (let [memberships (tenant-svc/get-user-memberships db-conn user-id)]
                    (when (empty? memberships)
                      (try
                        (let [user {:id user-id :email (:email result)}]
                          (tenant-svc/provision-tenant! db-conn config user)
                          (log/info "Provisioned workspace for newly verified user" (:email result)))
                        (catch Exception e
                          (log/error e "Failed to provision workspace after email verification"))))))

                ;; Send success notification email (non-critical) only on first-time verification
                (when (and (:success result) email-service)
                  (try
                    (email-verify/send-verification-success-email
                      email-service
                      {:email (:email result)})
                    (log/info "Success notification email sent")
                    (catch Exception e
                      (log/warn "Failed to send verification success email (non-critical):" (.getMessage e)))))

                {:status 302
                 :headers {"Location" "/email-verified?success=true"}})

              {:status 302
               :headers {"Location" (str "/email-verified?error=" (name (:error result)))}}))))

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
