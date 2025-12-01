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

(defn audit-user-operation
  "Wrapper to log user management operations for audit trail"
  [operation-name]
  (fn [original-handler]
    (fn [& args]
      (let [admin-session @(rf/subscribe [:admin/session])
            operation-details {:operation operation-name
                               :args args
                               :timestamp (js/Date.)
                               :admin-id (:id admin-session)}]
        (log/info "Admin user operation:" operation-details)
        ;; Dispatch audit event
        (rf/dispatch [:admin/log-operation operation-details])
        ;; Execute original operation
        (apply original-handler args)))))

;; Secure wrappers for user management operations
(def secure-load-users
  (-> (fn [params] (rf/dispatch [:admin/load-users params]))
    ((audit-user-operation "load-users"))
    ensure-admin-session))

(def secure-update-user-status
  (-> (fn [user-id new-status] (rf/dispatch [:admin/update-user-status user-id new-status]))
    ((audit-user-operation "update-user-status"))
    ensure-admin-session))

(def secure-view-user-details
  (-> (fn [user-id] (rf/dispatch [:admin/view-user-details user-id]))
    ((audit-user-operation "view-user-details"))
    ensure-admin-session))

(def secure-impersonate-user
  (-> (fn [user-id] (rf/dispatch [:admin/impersonate-user user-id]))
    ((audit-user-operation "impersonate-user"))
    ensure-admin-session))

;; Role-based permission checks
(defn has-permission?
  "Check if current admin has permission for specific operation"
  [required-permission]
  (let [admin-session @(rf/subscribe [:admin/session])
        admin-role (keyword (:role admin-session))
        role-permissions {:support #{:view-users :view-user-details}
                          :admin #{:view-users :view-user-details :update-user-status}
                          :super-admin #{:view-users :view-user-details :update-user-status :impersonate-user :delete-user}}]
    (contains? (get role-permissions admin-role #{}) required-permission)))

(defn require-permission
  "Wrapper that requires specific permission for operation"
  [required-permission]
  (fn [handler]
    (fn [& args]
      (if (has-permission? required-permission)
        (apply handler args)
        (do
          (log/warn "Admin operation denied - insufficient permissions"
            {:required required-permission
             :admin-role (:role @(rf/subscribe [:admin/session]))})
          (rf/dispatch [:admin/show-error "Insufficient permissions for this operation"])
          nil)))))

;; Permission-aware secure operations
(def secure-update-user-status-with-perms
  (-> secure-update-user-status
    ((require-permission :update-user-status))))

(def secure-impersonate-user-with-perms
  (-> secure-impersonate-user
    ((require-permission :impersonate-user))))

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

  ;; Set up global error handler for admin operations
  (rf/reg-event-fx
    :admin/show-error
    (fn [{:keys [db]} [_ error-message]]
      (log/error "Admin error:" error-message)
      {:db (assoc db :admin/error error-message)}))

  ;; Set up audit logging event
  (rf/reg-event-db
    :admin/log-operation
    (fn [db [_ operation-details]]
      (let [audit-logs (or (:admin/audit-log db) [])
            updated-logs (conj audit-logs operation-details)]
        ;; Keep only last 100 operations in memory
        (assoc db :admin/audit-log (take-last 100 updated-logs)))))

  (log/info "Admin security wrapper initialized"))
