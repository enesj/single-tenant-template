(ns app.backend.routes.admin.auth
  "Admin authentication handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [taoensso.timbre :as log]))

(defn login-handler
  "Handle admin login"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [{:keys [email password]} (:body request)
            {:keys [ip-address user-agent]} (utils/extract-request-context request)]
        (if-let [admin (admin-service/authenticate-admin db email password)]
          (let [admin-id (or (:id admin) (:admins/id admin))
                admin-email (or (:email admin) (:admins/email admin))
                admin-name (or (:full_name admin) (:admins/full_name admin))
                admin-role (or (:role admin) (:admins/role admin))
                session (admin-service/create-admin-session! db admin-id ip-address user-agent)]

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
              (assoc-in [:session :admin-token] (:token session))))
          (do
            ;; Record failed login attempt when we can resolve admin id
            (when-let [admin-row (admin-service/find-admin-by-email db email)]
              (let [admin-id (or (:id admin-row) (:admins/id admin-row))]
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
            admin-id (utils/get-admin-id request)]
        (when token
          (admin-service/invalidate-session! db token))

        (when admin-id
          (utils/log-admin-action "logout" admin-id "admin" admin-id {}))

        (-> (utils/success-response)
          (assoc :session nil))))
    "Failed to process admin logout"))

;; Route definitions
(defn routes
  "Authentication route definitions"
  [db]
  [""
   ["/login" {:post (login-handler db)}]
   ["/logout" {:post (logout-handler db)}]])
