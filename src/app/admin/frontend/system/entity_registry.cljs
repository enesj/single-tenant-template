(ns app.admin.frontend.system.entity-registry
  (:require
    [app.admin.frontend.adapters.users :as users-adapter]
    [app.admin.frontend.adapters.audit :as audit-adapter]
    [app.admin.frontend.adapters.login-events :as login-events-adapter]
    [app.admin.frontend.adapters.admins :as admins-adapter]
    [app.admin.frontend.adapters.expenses :as expenses-adapter]
    [app.admin.frontend.events.admins] ; Required for side effects (event registration)
    [app.admin.frontend.events.entity-sync] ; Sync domain entities into template store
    [app.admin.frontend.subs.admins] ; Required for side effects (subscription registration)
    [app.admin.frontend.components.enhanced-action-buttons :as enhanced-actions]
    [app.admin.frontend.components.user-actions :as user-actions]
    [app.admin.frontend.components.user-activity-modal :as user-activity-modal]
    [app.admin.frontend.components.user-details-modal :as user-details-modal]
    [app.admin.frontend.components.audit-actions :as audit-actions]
    [app.admin.frontend.components.audit-details-modal :as audit-details-modal]
    [app.admin.frontend.components.admin-actions :as admin-actions]))

(def entity-registry
  "Registry mapping entity keywords to adapter init functions and UI components (single-tenant)."
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

   ;; Expenses domain admin entities
   :expenses
   {:init-fn expenses-adapter/init-expenses-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :receipts
   {:init-fn expenses-adapter/init-receipts-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :suppliers
   {:init-fn expenses-adapter/init-suppliers-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :payers
   {:init-fn expenses-adapter/init-payers-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :articles
   {:init-fn expenses-adapter/init-articles-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :article-aliases
   {:init-fn expenses-adapter/init-article-aliases-adapter!
    :actions enhanced-actions/enhanced-action-buttons}

   :price-observations
   {:init-fn expenses-adapter/init-price-observations-adapter!
    :actions enhanced-actions/enhanced-action-buttons}})

(defonce registered-entities (atom {}))

(defn register-entities!
  "Store preloaded entity metadata. Single-tenant keeps this local to avoid dynamic discovery."
  [entities]
  (swap! registered-entities merge entities))

(defn load-entity-configs!
  "Return a resolved promise to match previous async API while doing nothing."
  []
  (js/Promise.resolve @registered-entities))
