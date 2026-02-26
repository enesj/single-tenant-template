(ns app.template.backend.middleware.admin
  "Admin auth middleware; update authentication checks here."
  (:require
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.template.backend.routes.admin.utils :as utils]
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
        (if-let [admin-raw (admin-auth/get-admin-by-session db token)]
          (let [admin {:id (:id admin-raw)
                       :email (:email admin-raw)
                       :role (:role admin-raw)
                       :full_name (:full_name admin-raw)
                       :status (:status admin-raw)}]
            (log/info "✅ ADMIN AUTH SUCCESS:" {:admin-id (:id admin) :admin-role (:role admin)})
            ;; Update session activity
            (admin-auth/update-session-activity! db token)
            ;; Add normalized admin info to request
            (handler (assoc request :admin admin)))
          (do
            (log/warn "❌ ADMIN AUTH FAILED: Invalid or expired session" {:token-preview (str (subs token 0 (min 8 (count token))) "...")})
            (utils/error-response "Invalid or expired admin session" :status 401)))
        (do
          (log/warn "❌ ADMIN AUTH FAILED: No token provided" {:uri (:uri request)})
          (utils/error-response "Admin authentication required" :status 401))))))

(defn wrap-admin-role
  "Middleware to check admin role permissions.

  Roles are ordered by privilege. Support is lowest; admin and owner are
  elevated. We keep :super_admin for forward compatibility if present."
  [handler required-role]
  (fn [request]
    (let [admin (:admin request)
          admin-role (keyword (:role admin))
          ;; Map known roles to privilege levels. Higher = more privileges.
          role-hierarchy {:support 1
                          :admin 2
                          :owner 3
                          :super_admin 4}]
      (if (>= (get role-hierarchy admin-role 0)
            (get role-hierarchy required-role 0))
        (handler request)
        (utils/error-response "Insufficient permissions" :status 403)))))
