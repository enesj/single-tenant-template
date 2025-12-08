(ns app.admin.frontend.core
  "Core namespace for admin frontend - single-tenant setup"
  (:require
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
    [app.admin.frontend.events.users.template.success-handlers]
    [app.admin.frontend.security.wrapper]
    [app.admin.frontend.specs.generic]
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.config]
    [app.admin.frontend.subs.dashboard]
    [app.admin.frontend.subs.audit]
    [app.admin.frontend.subs.login-events]
    [app.admin.frontend.subs.users]
    [app.domain.expenses.frontend.core :as expenses-domain]
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
    [re-frame.core :as rf]))

(defn init-admin!
  "Initialize admin module - ensures all events and subscriptions are registered and theme is applied"
  []
  ;; Initialize the theme when admin module loads
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-theme])
  ;; Load config and models-data if not already loaded
  (rf/dispatch [:app.template.frontend.events.config/fetch-config])
  ;; Load admin UI configurations
  (rf/dispatch [:admin/load-ui-configs])
  ;; Ensure expenses domain front-end namespaces are loaded
  (expenses-domain/init!))
