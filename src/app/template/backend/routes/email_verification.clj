(ns app.template.backend.routes.email-verification
  "Email verification API routes"
  (:require
    [app.template.backend.auth.email-verification :as email-verify]
    [app.template.backend.routes.utils :as route-utils]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn verify-email-handler
  "Handle email verification from URL token"
  [db email-service]
  (fn [req]
    (try
      (let [token (get-in req [:query-params "token"])]
        (if (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Missing verification token"})}

          (let [result (email-verify/verify-email-token! db token)]
            (if (:success result)
              (do
                ;; Log the successful verification
                (log/info "Email verification successful for user" (:user-id result))

                ;; Try to send success notification email, but don't fail if it doesn't work
                (when email-service
                  (try
                    (email-verify/send-verification-success-email
                      email-service
                      {:email (:email result)})
                    (log/info "Success notification email sent")
                    (catch Exception e
                      (log/warn "Failed to send verification success email (non-critical):" (.getMessage e)))))

                ;; Redirect to success page
                {:status 302
                 :headers {"Location" "/email-verified?success=true"}})

              ;; Redirect to error page with error type
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

(defn create-email-verification-routes
  "Create email verification routes"
  [db email-service]
  {:verify-email-handler (verify-email-handler db email-service)
   :resend-verification-handler (resend-verification-handler db email-service)
   :verification-status-handler (verification-status-handler db)})
