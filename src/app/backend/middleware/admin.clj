(ns app.backend.middleware.admin
  (:require
    [app.backend.services.admin :as admin-service]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))


(defn wrap-admin-authentication
  "Middleware to check for valid admin session"
  [handler db]
  (fn [request]
    (let [token (or (get-in request [:headers "x-admin-token"])
                  (get-in request [:session :admin-token])
                  (get-in request [:cookies "admin-token" :value]))]
      (log/info "🔐 ADMIN AUTH CHECK:" {:uri (:uri request)}
        :method (:request-method request)
        :token-present (boolean token)
        :token-preview (when token (str (subs token 0 (min 8 (count token))) "...")))
      (if token
        (if-let [admin-raw (admin-service/get-admin-by-session db token)]
          (let [;; Handle both namespaced and non-namespaced keys
                admin {:id (or (:id admin-raw) (:admins/id admin-raw))
                       :email (or (:email admin-raw) (:admins/email admin-raw))
                       :role (or (:role admin-raw) (:admins/role admin-raw))
                       :full_name (or (:full_name admin-raw) (:admins/full_name admin-raw))
                       :status (or (:status admin-raw) (:admins/status admin-raw))}]
            (log/info "✅ ADMIN AUTH SUCCESS:" {:admin-id (:id admin) :admin-role (:role admin)})
            ;; Update session activity
            (admin-service/update-session-activity! db token)
            ;; Add normalized admin info to request
            (handler (assoc request :admin admin)))
          (do
            (log/warn "❌ ADMIN AUTH FAILED: Invalid or expired session" {:token-preview (str (subs token 0 (min 8 (count token))) "...")})
            (-> (response/response {:error "Invalid or expired admin session"})
              (response/status 401)
              (response/content-type "application/json"))))
        (do
          (log/warn "❌ ADMIN AUTH FAILED: No token provided" {:uri (:uri request)})
          (-> (response/response {:error "Admin authentication required"})
            (response/status 401)
            (response/content-type "application/json")))))))

(defn wrap-admin-audit
  "Middleware to automatically log admin actions"
  [handler db]
  (fn [request]
    (let [response (handler request)
          admin (:admin request)]
      (when (and admin
              (#{:post :put :patch :delete} (:request-method request))
              (<= 200 (:status response) 299))
        (try
          (admin-service/log-audit! db
            {:admin_id (:id admin)
             :action (str (name (:request-method request)) " " (:uri request))
             :entity-type "admin_action"              ; Provide a default entity type
             :entity-id nil                           ; Can be nil for general actions
             :changes nil                             ; Can be nil for simple actions
             :ip-address (or (get-in request [:headers "x-forwarded-for"])
                           (:remote-addr request))
             :user-agent (get-in request [:headers "user-agent"])})
          (catch Exception e
            (log/error e "Failed to log admin audit"))))
      response)))

(defn wrap-admin-role
  "Middleware to check admin role permissions"
  [handler required-role]
  (fn [request]
    (let [admin (:admin request)
          admin-role (keyword (:role admin))
          role-hierarchy {:support 1
                          :admin 2
                          :super_admin 3}]
      (if (>= (get role-hierarchy admin-role 0)
            (get role-hierarchy required-role 0))
        (handler request)
        (-> (response/response {:error "Insufficient permissions"})
          (response/status 403)
          (response/content-type "application/json"))))))
