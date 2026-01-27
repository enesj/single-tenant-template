(ns app.template.backend.routes.admin.auth
  "Admin authentication handlers"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]))

(defn login-handler
  "Handle admin login"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [email password]} (:body request)
            {:keys [ip-address user-agent]} (utils/extract-request-context request)
            existing-session (or (:session request) {})]
        (if-let [admin (admin-auth/authenticate-admin db email password)]
          (let [admin-id (:id admin)
                admin-email (:email admin)
                admin-name (:full_name admin)
                admin-role (:role admin)
                session (admin-auth/create-admin-session! db admin-id ip-address user-agent)]

            ;; Record successful admin login in monitoring table
            (login-monitoring/record-login-event! db
              {:principal-type :admin
               :principal-id admin-id
               :success true
               :reason nil
               :ip ip-address
               :user-agent user-agent})

            ;; Log to audit trail for admin actions
            (utils/log-admin-action "login" admin-id "admin" admin-id
              {:email admin-email :role admin-role})

            (-> (utils/json-response
                  {:success true
                   :admin {:id admin-id
                           :email admin-email
                           :full_name admin-name
                           :role admin-role}
                   :token (:token session)})
              ;; IMPORTANT: merge with existing session to avoid wiping user auth-session.
              (assoc :session (assoc existing-session :admin-token (:token session)))))

          (do
            ;; Record failed login attempt when we can resolve admin id
            (when-let [admin-row (admin-auth/find-admin-by-email db email)]
              (let [admin-id (:id admin-row)]
                (when admin-id
                  (login-monitoring/record-login-event! db
                    {:principal-type :admin
                     :principal-id admin-id
                     :success false
                     :reason "invalid_credentials"
                     :ip ip-address
                     :user-agent user-agent}))))
            (utils/error-response "Invalid credentials" :status 401)))))
    "Failed to process admin login"))

(defn logout-handler
  "Handle admin logout"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [token (or (get-in request [:session :admin-token])
                    (get-in request [:headers "x-admin-token"]))
            admin-id (utils/get-admin-id request)
            existing-session (or (:session request) {})
            new-session (dissoc existing-session :admin-token)]
        (when token
          (admin-auth/invalidate-session! db token))

        (when admin-id
          (utils/log-admin-action "logout" admin-id "admin" admin-id {}))

        (-> (utils/success-response)
          ;; IMPORTANT: keep any existing user session data; only remove admin-token.
          (assoc :session (when (seq new-session) new-session)))))
    "Failed to process admin logout"))

;; Route definitions
(defn routes
  "Authentication route definitions"
  [db]
  [""
   ["/login" {:post (login-handler db)}]
   ["/logout" {:post (logout-handler db)}]])
