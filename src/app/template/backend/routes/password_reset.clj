(ns app.template.backend.routes.password-reset
  "Password reset and change API routes for users"
  (:require
    [app.backend.services.gmail-smtp :as gmail-smtp]
    [app.template.backend.auth.password-reset :as pwd-reset]
    [app.template.backend.routes.utils :as route-utils]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Request Handlers
;; ============================================================================

(defn forgot-password-handler
  "Handle forgot password request for users.
   This is a PUBLIC endpoint (no auth required)."
  [db-adapter email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "forgot-password"
      (let [{:keys [email]} (:body-params req)]
        (if (empty? email)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Email is required"})}
          
          (let [send-email-fn (when email-service
                                (fn [to-email token reset-url full-name]
                                  (gmail-smtp/send-password-reset-email
                                    (:smtp-config email-service)
                                    (:from-email email-service)
                                    to-email token reset-url full-name)))
                result (pwd-reset/request-password-reset! 
                         db-adapter email :user send-email-fn base-url)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string result)}))))))

(defn verify-reset-token-handler
  "Verify reset token is valid (for showing reset form).
   This is a PUBLIC endpoint (no auth required)."
  [db-adapter]
  (fn [req]
    (route-utils/with-error-handling "verify-reset-token"
      (let [token (or (get-in req [:query-params "token"])
                      (get-in req [:params :token]))]
        (if (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Token is required"})}
          
          (let [result (pwd-reset/verify-reset-token db-adapter token)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:valid (:valid? result)
                                          :error (:error result)})}))))))

(defn reset-password-handler
  "Handle password reset with token.
   This is a PUBLIC endpoint (no auth required)."
  [db-adapter email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "reset-password"
      (let [{:keys [token password new-password newPassword]} (:body-params req)
            ;; Support multiple naming conventions
            effective-password (or password new-password newPassword)]
        (cond
          (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Reset token is required"})}
          
          (empty? effective-password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [result (pwd-reset/reset-password! db-adapter token effective-password)]
            (if (:success result)
              (do
                ;; Optionally send password changed confirmation email
                (when email-service
                  (try
                    (let [{:keys [principal-type principal-id]} result
                          principal (pwd-reset/find-principal-by-id db-adapter principal-type principal-id)
                          email (or (:email principal) 
                                    (:users/email principal)
                                    (:admins/email principal))
                          full-name (or (:full_name principal)
                                        (:users/full_name principal)
                                        (:admins/full_name principal))]
                      (gmail-smtp/send-password-changed-email
                        (:smtp-config email-service)
                        (:from-email email-service)
                        email full-name base-url))
                    (catch Exception e
                      (log/warn "Failed to send password changed email:" (.getMessage e)))))
                
                {:status 200
                 :headers {"Content-Type" "application/json"}
                 :body (json/generate-string {:success true 
                                              :message "Password reset successfully"})})
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})})))))))

(defn change-password-handler
  "Handle password change for authenticated users.
   This is a PROTECTED endpoint (requires auth)."
  [db-adapter email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "change-password"
      (let [auth-session (get-in req [:session :auth-session])
            user (:user auth-session)
            {:keys [current-password new-password currentPassword newPassword]} (:body-params req)
            ;; Support both naming conventions
            current-pwd (or current-password currentPassword)
            new-pwd (or new-password newPassword)]
        
        (cond
          (nil? user)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}
          
          (empty? current-pwd)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Current password is required"})}
          
          (empty? new-pwd)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [user-id (or (:id user) (when (string? (:id user)) 
                                          (java.util.UUID/fromString (:id user))))
                result (pwd-reset/change-password! 
                         db-adapter :user user-id current-pwd new-pwd)]
            (if (:success result)
              (do
                ;; Optionally send password changed confirmation email
                (when email-service
                  (try
                    (gmail-smtp/send-password-changed-email
                      (:smtp-config email-service)
                      (:from-email email-service)
                      (:email user) (:full_name user) base-url)
                    (catch Exception e
                      (log/warn "Failed to send password changed email:" (.getMessage e)))))
                
                {:status 200
                 :headers {"Content-Type" "application/json"}
                 :body (json/generate-string {:success true 
                                              :message "Password changed successfully"})})
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})})))))))

;; ============================================================================
;; Route Creation
;; ============================================================================

(defn create-password-reset-routes
  "Create password reset/change route handlers for users.
   
   Returns a map of handlers that can be mounted in the API routes."
  [db-adapter email-service base-url]
  {:forgot-password-handler (forgot-password-handler db-adapter email-service base-url)
   :verify-reset-token-handler (verify-reset-token-handler db-adapter)
   :reset-password-handler (reset-password-handler db-adapter email-service base-url)
   :change-password-handler (change-password-handler db-adapter email-service base-url)})
