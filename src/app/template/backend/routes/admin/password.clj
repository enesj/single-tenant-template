(ns app.template.backend.routes.admin.password
  "Admin password management routes.
   Provides:
   - Forgot password (public endpoint)
   - Reset password with token (public endpoint)
   - Change password (protected endpoint)"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.services.gmail-api :as gmail-api]
    [app.template.backend.services.gmail-smtp :as gmail-smtp]
    [app.template.backend.auth.password-reset :as pwd-reset]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Email dispatch helpers
;; ============================================================================

(defn- make-send-reset-fn
  "Build a send-email-fn that dispatches to SMTP or Gmail API."
  [email-service]
  (let [db   (:db email-service)
        opts (when db {:db db})
        api-cfg (cond-> (:gmail-api-config email-service) db (assoc :db db))]
    (cond
      (:smtp-config email-service)
      (fn [to-email token reset-url full-name]
        (gmail-smtp/send-password-reset-email
          (:smtp-config email-service)
          (:from-email email-service)
          to-email token reset-url full-name opts))

      (:gmail-api-config email-service)
      (fn [to-email token reset-url full-name]
        (gmail-api/send-password-reset-email
          api-cfg
          (:from-email email-service)
          to-email token reset-url full-name)))))

(defn- send-password-changed!
  "Send a password-changed confirmation email via the appropriate transport."
  [email-service to-email full-name base-url]
  (let [db   (:db email-service)
        opts (when db {:db db})
        api-cfg (cond-> (:gmail-api-config email-service) db (assoc :db db))]
    (cond
      (:smtp-config email-service)
      (gmail-smtp/send-password-changed-email
        (:smtp-config email-service)
        (:from-email email-service)
        to-email full-name base-url opts)

      (:gmail-api-config email-service)
      (gmail-api/send-password-changed-email
        api-cfg
        (:from-email email-service)
        to-email full-name base-url))))

;; ============================================================================
;; Public Endpoints (No Auth Required)
;; ============================================================================

(defn forgot-password-handler
  "Handle forgot password request for admins.
   PUBLIC endpoint - no authentication required."
  [db email-service base-url]
  (fn [req]
    (try
      (let [{:keys [email]} (:body req)]
        (if (empty? email)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Email is required"})}

          (let [send-email-fn (when email-service (make-send-reset-fn email-service))
                result        (pwd-reset/request-password-reset!
                                db email :admin send-email-fn base-url)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string result)})))
      (catch Exception e
        (log/error e "Admin forgot password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn verify-reset-token-handler
  "Verify reset token validity.
   PUBLIC endpoint - no authentication required."
  [db]
  (fn [req]
    (try
      (let [token (or (get-in req [:query-params "token"])
                    (get-in req [:params :token]))]
        (if (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Token is required"})}

          (let [result (pwd-reset/verify-reset-token db token)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:valid (:valid? result)
                                          :error (:error result)})})))
      (catch Exception e
        (log/error e "Admin verify reset token error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn reset-password-handler
  "Handle password reset with token for admins.
   PUBLIC endpoint - no authentication required."
  [db email-service base-url]
  (fn [req]
    (try
      (let [{:keys [token password]} (:body req)]
        (cond
          (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Reset token is required"})}

          (empty? password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}

          :else
          (let [result (pwd-reset/reset-password! db token password)]
            (if (:success result)
              (do
                ;; Send password changed confirmation email
                (when email-service
                  (try
                    (let [{:keys [principal-id]} result
                          admin     (pwd-reset/find-principal-by-id db :admin principal-id)
                          email     (:email admin)
                          full-name (:full_name admin)]
                      (send-password-changed! email-service email full-name base-url))
                    (catch Exception e
                      (log/warn "Failed to send password changed email:" (.getMessage e)))))

                {:status 200
                 :headers {"Content-Type" "application/json"}
                 :body (json/generate-string {:success true})})
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})}))))
      (catch Exception e
        (log/error e "Admin reset password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

;; ============================================================================
;; Protected Endpoints (Auth Required)
;; ============================================================================

(defn change-password-handler
  "Handle password change for authenticated admins.
   PROTECTED endpoint - requires authentication."
  [db email-service base-url]
  (fn [req]
    (try
      (let [admin (:admin req)
            {:keys [current-password new-password currentPassword newPassword]} (:body req)
            ;; Support both naming conventions
            current-pwd (or current-password currentPassword)
            new-pwd (or new-password newPassword)]

        (cond
          (nil? admin)
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
          (let [admin-id (:id admin)
                admin-name (:full_name admin)
                result (pwd-reset/change-password!
                         db :admin admin-id current-pwd new-pwd)]
            (if (:success result)
              (do
                ;; Log the action
                (utils/log-admin-action "change_own_password" admin-id
                  "admin" admin-id
                  {:action "password_changed"})

                ;; Send password changed confirmation email
                (when email-service
                  (try
                    (let [delivery-admin (pwd-reset/find-principal-by-id db :admin admin-id)]
                      (send-password-changed! email-service
                        (:email delivery-admin)
                        (or (:full_name delivery-admin) admin-name)
                        base-url))
                    (catch Exception e
                      (log/warn "Failed to send password changed email:" (.getMessage e)))))

                {:status 200
                 :headers {"Content-Type" "application/json"}
                 :body (json/generate-string {:success true})})
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})}))))
      (catch Exception e
        (log/error e "Admin change password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

;; ============================================================================
;; Route Definitions
;; ============================================================================

(defn public-routes
  "Public admin password routes (no authentication required).
   These are mounted at /admin/auth/ path."
  [db email-service base-url]
  [""
   ["/forgot-password" {:post (forgot-password-handler db email-service base-url)}]
   ["/reset-password" {:post (reset-password-handler db email-service base-url)}]
   ["/verify-reset-token" {:get (verify-reset-token-handler db)}]])

(defn protected-routes
  "Protected admin password routes (authentication required).
   These are mounted at /admin/api/auth/ path."
  [db email-service base-url]
  [""
   ["/change-password" {:post (change-password-handler db email-service base-url)}]])
