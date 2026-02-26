(ns app.admin.frontend.system.entity-registry
  (:require
    [app.admin.frontend.adapters.users :as users-adapter]
    [app.admin.frontend.adapters.audit :as audit-adapter]
    [app.admin.frontend.adapters.login-events :as login-events-adapter]
    [app.admin.frontend.adapters.admins :as admins-adapter]
    [app.admin.frontend.adapters.backlog :as backlog-adapter]
    [app.admin.frontend.events.admins] ; Required for side effects (event registration)
    [app.admin.frontend.events.entity-sync] ; Sync domain entities into template store
    [app.admin.frontend.subs.admins] ; Required for side effects (subscription registration)
    [app.admin.frontend.components.enhanced-action-buttons :as enhanced-actions]
    [app.admin.frontend.components.user-actions :as user-actions]
    [app.admin.frontend.components.user-activity-modal :as user-activity-modal]
    [app.admin.frontend.components.user-details-modal :as user-details-modal]
    [app.admin.frontend.components.audit-actions :as audit-actions]
    [app.admin.frontend.components.audit-details-modal :as audit-details-modal]
    [app.admin.frontend.components.admin-actions :as admin-actions]
    ;; Domain registry - no direct domain imports
    [app.domain.frontend.registry :as domain-registry]))

(def entity-registry
  "Registry mapping entity keywords to adapter init functions and UI components (single-tenant).
   Template/admin entities are defined here; domain entities are merged from domain-registry."
  (merge
    ;; Template/admin entities (core infrastructure)
    {:users
     {:init-fn users-adapter/init-users-adapter!
      :actions enhanced-actions/enhanced-action-buttons
      :custom-actions user-actions/admin-user-actions
      :modals [user-details-modal/user-details-modal
               user-activity-modal/user-activity-modal]}

     :audit-logs
     {:init-fn audit-adapter/init-audit-adapter!
      :actions enhanced-actions/enhanced-action-buttons
      :custom-actions audit-actions/admin-audit-actions
      :modals [audit-details-modal/audit-details-modal]}

     :login-events
     {:init-fn login-events-adapter/init-login-events-adapter!
      :actions enhanced-actions/enhanced-action-buttons}

     :admins
     {:init-fn admins-adapter/init-admins-adapter!
      :actions enhanced-actions/enhanced-action-buttons
      :custom-actions admin-actions/admin-admin-actions}

     :backlog
     {:init-fn backlog-adapter/init-backlog-adapter!
      :actions enhanced-actions/enhanced-action-buttons}}

    ;; Domain entities from registry (decoupled from template/admin)
    ;; Domain registry provides :init-fn for each entity; we add default :actions
    (into {}
      (for [[entity-key entity-config] (domain-registry/all-admin-entities)]
        [entity-key (merge {:actions enhanced-actions/enhanced-action-buttons}
                      entity-config)]))))

(defonce registered-entities (atom {}))

(defn register-entities!
  "Store preloaded entity metadata. Single-tenant keeps this local to avoid dynamic discovery."
  [entities]
  (swap! registered-entities merge entities))


