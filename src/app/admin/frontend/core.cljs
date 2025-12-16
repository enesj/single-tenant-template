(ns app.admin.frontend.core
  "Core namespace for admin frontend - single-tenant setup"
  (:require
    [app.admin.frontend.adapters.expenses]
    [app.admin.frontend.adapters.users]
    [app.admin.frontend.config.preload]
    [app.admin.frontend.events.config]
    [app.admin.frontend.events.auth]
    [app.admin.frontend.events.dashboard]
    [app.admin.frontend.events.audit]
    [app.admin.frontend.events.login-events]
    [app.admin.frontend.events.users]
    [app.admin.frontend.events.users.core]
    [app.admin.frontend.events.users.security]
    [app.admin.frontend.events.users.template.form-interceptors]
    [app.admin.frontend.events.users.template.messages]
    [app.admin.frontend.security.wrapper]
    [app.admin.frontend.specs.generic]
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.config]
    [app.admin.frontend.subs.dashboard]
    [app.admin.frontend.subs.audit]
    [app.admin.frontend.subs.login-events]
    [app.admin.frontend.subs.users]
    [app.admin.frontend.subs.expenses]
    [app.domain.frontend.expenses.core :as expenses-domain]
    [app.template.frontend.events.core]
    [app.template.frontend.events.form]
    [app.template.frontend.events.list.batch]
    [app.template.frontend.events.list.crud]
    [app.template.frontend.events.list.filters]
    [app.template.frontend.events.list.selection]
    [app.template.frontend.events.list.ui-state]
    [app.template.frontend.subs.entity]
    [app.template.frontend.subs.list]
    [app.template.frontend.subs.ui]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [taoensso.timbre :as log]))

(defonce ^:private admin-initialized? (atom false))

;; =============================================================================
;; Dev-only diagnostics
;; =============================================================================

(defonce ^:private unified-debug-installed? (atom false))
(defonce ^:private unified-debug-last-snapshot (atom nil))

(defn- unified-settings-snapshot
  "Small snapshot of app-db that helps debug unexpected resets of the unified
  settings editor (e.g. jumping from :edit back to :view).

  Keep this intentionally tiny to avoid noisy logs."
  [db]
  (let [route (or (:current-route db) {})
        route-name (or (get-in route [:data :name]) (:name route))
        admin? (contains? db :admin)
        mode (get-in db [:admin :unified-settings :mode])
        selected (get-in db [:admin :unified-settings :selected-entity])
        fixed-scope (get-in db [:admin :unified-settings :fixed-scope])
        scope (get-in db [:admin :unified-settings :scope])
        admin-view-options (get-in db [:admin :settings :view-options])
        admin-view-options-entities (when (map? admin-view-options) (keys admin-view-options))]
    {:route-name route-name
     :models-data? (boolean (:models-data db))
     :has-admin? (boolean admin?)
     :mode mode
     :scope scope
     :fixed-scope fixed-scope
     :selected-entity selected
     :admin-view-options? (boolean (map? admin-view-options))
     :admin-view-options-entities-count (count admin-view-options-entities)}))

(defn- install-unified-settings-debug!
  "In dev, log when the unified-settings-related portion of app-db changes.

  This avoids relying on Chrome's expandable object rendering (which isn't
  preserved in MCP console captures)."
  []
  (when (and ^boolean goog.DEBUG (compare-and-set! unified-debug-installed? false true))
    (rf/add-post-event-callback
      :admin/unified-settings-snapshot
      (fn [event _]
        (let [db @rf-db/app-db
              snapshot (unified-settings-snapshot db)
              prev @unified-debug-last-snapshot]
          (when (not= snapshot prev)
            (log/info "Unified settings snapshot changed"
              {:event event
               :prev prev
               :next snapshot}))
          (reset! unified-debug-last-snapshot snapshot))))))

(defn init-admin!
  "Initialize admin module - ensures all events and subscriptions are registered and theme is applied.

  Safe to call multiple times; runs only once per page load."
  []
  (when (compare-and-set! admin-initialized? false true)
    (install-unified-settings-debug!)
    ;; Initialize authentication persistence first (before any auth checks)
    (rf/dispatch [:admin/init-auth-persistence])
    ;; Initialize the theme when admin module loads
    (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-theme])
    ;; Load config and models-data if not already loaded
    (rf/dispatch [:app.template.frontend.events.config/fetch-config])
    ;; Load admin UI configurations (hits /admin/api/*)
    (rf/dispatch [:admin/load-ui-configs])
    ;; Ensure expenses domain front-end namespaces are loaded
    (expenses-domain/init!)))
