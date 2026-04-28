(ns app.admin.frontend.security.wrapper
  "Security wrapper ensuring admin operations maintain proper authentication and audit"
  (:require
    [app.admin.frontend.adapters.users :as user-adapter]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn ensure-admin-session
  "Middleware to ensure admin session is valid before operations"
  [handler]
  (fn [& args]
    (let [admin-authenticated? @(rf/subscribe [:admin/authenticated?])
          admin-session @(rf/subscribe [:admin/session])]
      (if admin-authenticated?
        (do
          (log/info "Admin operation authorized for session:" (:id admin-session))
          (apply handler args))
        (do
          (log/warn "Attempted admin operation without valid session")
          (rf/dispatch [:admin/redirect-to-login])
          nil)))))

;; Secure wrappers for user management operations

;; Role-based permission checks
(defn has-permission?
  "Check if current admin has permission for specific operation"
  [required-permission]
  (let [admin-session @(rf/subscribe [:admin/session])
        admin-role (keyword (:role admin-session))
        role-permissions {:support #{:view-users :view-user-details}
                          :admin #{:view-users :view-user-details :update-user-status}
                          :super-admin #{:view-users :view-user-details :update-user-status :delete-user}}]
    (contains? (get role-permissions admin-role #{}) required-permission)))

;; Permission-aware secure operations

;; Template system security integration
(defn secure-template-operation
  "Wrapper for template system operations to ensure admin security"
  [operation-type]
  (fn [original-event]
    (fn [cofx event-vector]
      (let [entity-type (second event-vector)]
        (when (= entity-type :users)
          (log/info "Securing template operation:" operation-type "for entity:" entity-type)
          ;; Ensure admin session is valid
          (when-not @(rf/subscribe [:admin/authenticated?])
            (rf/dispatch [:admin/redirect-to-login])
            (throw (js/Error. "Admin authentication required")))
          ;; Log the operation
          (rf/dispatch [:admin/log-operation {:operation operation-type
                                              :entity entity-type
                                              :timestamp (js/Date.)}]))
        ;; Execute original operation
        (original-event cofx event-vector)))))

;; Initialize security wrapper
(defn init-security-wrapper!
  "Initialize security wrapper for admin operations"
  []
  (log/info "Initializing admin security wrapper")

  ;; Initialize user adapter with security
  (user-adapter/init-users-adapter!)

  ;; Set up audit logging event
  (rf/reg-event-db
    :admin/log-operation
    (fn [db [_ operation-details]]
      (let [audit-logs (or (:admin/audit-log db) [])
            updated-logs (conj audit-logs operation-details)]
        ;; Keep only last 100 operations in memory
        (assoc db :admin/audit-log (take-last 100 updated-logs)))))

  (log/info "Admin security wrapper initialized"))
