(ns code-quality.check-unused-reframe.data
  (:require
    [babashka.fs :as fs]
    [clojure.edn :as edn]))

;; Keywords flagged as unused from clojure-lsp diagnostics
(def unused-keywords
  [;; Admin frontend subs/events
   {:keyword ":admin/login-events-logs-loading?"
    :file "src/app/admin/frontend/subs/login_events.cljs"
    :type :subscription}
   {:keyword ":admin/login-events-logs-error"
    :file "src/app/admin/frontend/subs/login_events.cljs"
    :type :subscription}
   {:keyword ":admin/hide-admin-details"
    :file "src/app/admin/frontend/events/admins.cljs"
    :type :event}
   {:keyword ":admin/fetch-admins-entities"
    :file "src/app/admin/frontend/events/admins.cljs"
    :type :event}
   {:keyword ":admin/admins-count"
    :file "src/app/admin/frontend/subs/admins.cljs"
    :type :subscription}
   {:keyword ":admin/active-admins"
    :file "src/app/admin/frontend/subs/admins.cljs"
    :type :subscription}
   {:keyword ":admin/can-delete-admin?"
    :file "src/app/admin/frontend/subs/admins.cljs"
    :type :subscription}
   {:keyword ":admin/can-change-role?"
    :file "src/app/admin/frontend/subs/admins.cljs"
    :type :subscription}
   {:keyword ":admin/navigated"
    :file "src/app/admin/frontend/events/auth.cljs"
    :type :event}
   {:keyword ":admin/set-success-message"
    :file "src/app/admin/frontend/events/auth.cljs"
    :type :event}
   {:keyword ":admin/set-error-message"
    :file "src/app/admin/frontend/events/auth.cljs"
    :type :event}
   {:keyword ":admin/clear-password-reset-state"
    :file "src/app/admin/frontend/events/password.cljs"
    :type :event}
   {:keyword ":admin/init-reset-password"
    :file "src/app/admin/frontend/events/password.cljs"
    :type :event}
   {:keyword ":admin/show-delete-confirmation"
    :file "src/app/admin/frontend/events/users/template/delete_handlers.cljs"
    :type :event}
   {:keyword ":admin/batch-actions-visible?"
    :file "src/app/admin/frontend/handlers/generic.cljs"
    :type :subscription}
   {:keyword ":admin/show-batch-login-event-actions"
    :file "src/app/admin/frontend/events/login_events.cljs"
    :type :event}
   {:keyword ":admin/filterable-columns"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":admin/view-options"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   
   ;; Admin frontend subs/config.cljs
   {:keyword ":app.admin.frontend.subs.config/column-visible?"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.subs.config/column-label"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.subs.config/columns-customized?"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.subs.config/visible-column-count"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.subs.config/hidden-column-count"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.subs.config/entity-spec"
    :file "src/app/admin/frontend/subs/config.cljs"
    :type :subscription}
   
   ;; Admin frontend events/config.cljs
   {:keyword ":app.admin.frontend.events.config/load-config"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.config/load-saved-column-config"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.config/reorder-columns"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.config/reset-columns-to-default"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.config/save-column-config"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.config/clear-saved-column-config"
    :file "src/app/admin/frontend/events/config.cljs"
    :type :event}
   
   ;; Admin frontend events/settings.cljs
   {:keyword ":app.admin.frontend.events.settings/set-column-defaults-bulk"
    :file "src/app/admin/frontend/events/settings/view_options.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.settings/update-entity-setting"
    :file "src/app/admin/frontend/events/settings/view_options.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.settings/remove-entity-setting"
    :file "src/app/admin/frontend/events/settings/view_options.cljs"
    :type :event}
   
   ;; Admin frontend events/user_settings.cljs
   {:keyword ":app.admin.frontend.events.user-settings/set-column-defaults-bulk"
    :file "src/app/admin/frontend/events/user_settings/view_options.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.user-settings/saved"
    :file "src/app/admin/frontend/events/user_settings/subs.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.events.user-settings/set-form-field-list-draft"
    :file "src/app/admin/frontend/events/user_settings/form_fields.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.user-settings/entities-config"
    :file "src/app/admin/frontend/events/user_settings/subs.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.events.user-settings/form-fields-config"
    :file "src/app/admin/frontend/events/user_settings/subs.cljs"
    :type :subscription}
   
   ;; Admin frontend events/unified_settings.cljs  
   {:keyword ":app.admin.frontend.events.unified-settings/toggle-mode"
    :file "src/app/admin/frontend/events/unified_settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.unified-settings/set-tab"
    :file "src/app/admin/frontend/events/unified_settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.unified-settings/tab"
    :file "src/app/admin/frontend/events/unified_settings.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.events.unified-settings/overview-configs"
    :file "src/app/admin/frontend/events/unified_settings.cljs"
    :type :subscription}
   
   ;; Admin frontend adapters/audit.cljs
   {:keyword ":app.admin.frontend.adapters.audit/audit-log-delete-failed"
    :file "src/app/admin/frontend/adapters/audit.cljs"
    :type :event}
   
   ;; Admin frontend subs/audit.cljs
   {:keyword ":admin/audit-filters"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-pagination"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-current-page"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-total-pages"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-sort"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/batch-audit-actions-visible?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/batch-selected-audit-ids"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-deleting?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-bulk-deleting?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-exporting?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-logs-formatted"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-entity-types"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-actions"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-stats"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-filtered-count"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/can-delete-audit-logs?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/can-export-audit-logs?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   {:keyword ":admin/audit-logs-loading?"
    :file "src/app/admin/frontend/subs/audit.cljs"
    :type :subscription}
   
   ;; Admin frontend events/audit.cljs
   {:keyword ":admin/export-selected-audit-logs"
    :file "src/app/admin/frontend/events/audit.cljs"
    :type :event}
   {:keyword ":admin/audit-change-page"
    :file "src/app/admin/frontend/events/audit.cljs"
    :type :event}
   {:keyword ":admin/audit-change-page-size"
    :file "src/app/admin/frontend/events/audit.cljs"
    :type :event}
   {:keyword ":admin/audit-sort-by"
    :file "src/app/admin/frontend/events/audit.cljs"
    :type :event}
   
   ;; Admin frontend events/users messages
   {:keyword ":admin/hide-form-modal"
    :file "src/app/admin/frontend/events/users/template/messages.cljs"
    :type :event}
   {:keyword ":admin/has-success-message?"
    :file "src/app/admin/frontend/events/users/template/messages.cljs"
    :type :subscription}
   {:keyword ":admin/has-error-message?"
    :file "src/app/admin/frontend/events/users/template/messages.cljs"
    :type :subscription}
   
   ;; Admin frontend events/users bulk_operations
   {:keyword ":admin/bulk-update-user-status"
    :file "src/app/admin/frontend/events/users/bulk_operations.cljs"
    :type :event}
   {:keyword ":admin/bulk-update-user-role"
    :file "src/app/admin/frontend/events/users/bulk_operations.cljs"
    :type :event}
   {:keyword ":admin/export-users"
    :file "src/app/admin/frontend/events/users/bulk_operations.cljs"
    :type :event}
   
   ;; Admin frontend subs/expenses.cljs
   {:keyword ":admin/expenses-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/expenses-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/receipts-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/receipts-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/suppliers-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/suppliers-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/payers-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/payers-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/articles-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/articles-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/article-aliases-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/article-aliases-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/price-observations-loading?"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":admin/price-observations-error"
    :file "src/app/admin/frontend/subs/expenses.cljs"
    :type :subscription}
   
   ;; Admin frontend specs
   {:keyword ":entity-specs/users"
    :file "src/app/admin/frontend/specs/generic.cljs"
    :type :subscription}
   {:keyword ":admin/form-entity-specs-by-name"
    :file "src/app/admin/frontend/specs/generic.cljs"
    :type :subscription}
   {:keyword ":admin/visible-fields-for-record"
    :file "src/app/admin/frontend/specs/conditional.cljs"
    :type :subscription}
   
   ;; Template frontend subs
   {:keyword ":subscription/available-tiers"
    :file "src/app/template/frontend/subs/subscription.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.list/entity-ids"
    :file "src/app/template/frontend/subs/list.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.list/filter-modal"
    :file "src/app/template/frontend/subs/list.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.list/entity-config"
    :file "src/app/template/frontend/subs/list.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.list/batch-edit-popup"
    :file "src/app/template/frontend/subs/list.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.form/submitting?"
    :file "src/app/template/frontend/subs/form.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.ui/resolved-display-settings"
    :file "src/app/template/frontend/subs/ui.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.core/editing-id"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.core/show-add-form"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":app.template.frontend.subs.core/get-db"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":user-can?"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":password-reset/message"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":password-reset/token"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":change-password/message"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":user/has-expenses-access?"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   {:keyword ":user/is-unassigned?"
    :file "src/app/template/frontend/subs/core.cljs"
    :type :subscription}
   
   ;; Template frontend events
   {:keyword ":app.template.frontend.events.form/default-create-success"
    :file "src/app/template/frontend/events/form.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.form/default-update-success"
    :file "src/app/template/frontend/events/form.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.auth/set-auth-error"
    :file "src/app/template/frontend/events/auth/utils.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.auth/clear-change-password-state"
    :file "src/app/template/frontend/events/auth/change_password.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.auth/clear-password-reset-state"
    :file "src/app/template/frontend/events/auth/change_password.cljs"
    :type :event}
   {:keyword ":auth/fetch-verification-status"
    :file "src/app/template/frontend/pages/email_verification.cljs"
    :type :event}
   {:keyword ":auth/verification-error"
    :file "src/app/template/frontend/pages/email_verification.cljs"
    :type :subscription}
   {:keyword ":subscription/update-usage"
    :file "src/app/template/frontend/events/subscription.cljs"
    :type :event}
   {:keyword ":user-expenses/update-expense"
    :file "src/app/template/frontend/events/user_expenses/crud.cljs"
    :type :event}
   {:keyword ":user-expenses/upload-receipt-failure"
    :file "src/app/template/frontend/events/user_expenses/lookups.cljs"
    :type :event}
   {:keyword ":user-expenses/by-month-error"
    :file "src/app/template/frontend/subs/user_expenses.cljs"
    :type :subscription}
   {:keyword ":user-expenses/by-supplier-error"
    :file "src/app/template/frontend/subs/user_expenses.cljs"
    :type :subscription}
   {:keyword ":user-expenses/recent-total"
    :file "src/app/template/frontend/subs/user_expenses.cljs"
    :type :subscription}
   {:keyword ":user-expenses/recent-limit"
    :file "src/app/template/frontend/subs/user_expenses.cljs"
    :type :subscription}
   {:keyword ":user-expenses/recent-offset"
    :file "src/app/template/frontend/subs/user_expenses.cljs"
    :type :subscription}
   
   ;; Domain frontend subs
   {:keyword ":expenses/entries"
    :file "src/app/domain/frontend/expenses/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":expenses/entries-loading?"
    :file "src/app/domain/frontend/expenses/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":expenses/form-error"
    :file "src/app/domain/frontend/expenses/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":expenses/form-last-created"
    :file "src/app/domain/frontend/expenses/subs/expenses.cljs"
    :type :subscription}
   {:keyword ":expenses/suppliers-loading?"
    :file "src/app/domain/frontend/expenses/subs/suppliers.cljs"
    :type :subscription}
   {:keyword ":expenses/suppliers-error"
    :file "src/app/domain/frontend/expenses/subs/suppliers.cljs"
    :type :subscription}
   {:keyword ":expenses/payers-loading?"
    :file "src/app/domain/frontend/expenses/subs/payers.cljs"
    :type :subscription}
   {:keyword ":expenses/payers-error"
    :file "src/app/domain/frontend/expenses/subs/payers.cljs"
    :type :subscription}
   {:keyword ":expenses/receipts"
    :file "src/app/domain/frontend/expenses/subs/receipts.cljs"
    :type :subscription}
   {:keyword ":expenses/receipts-loading?"
    :file "src/app/domain/frontend/expenses/subs/receipts.cljs"
    :type :subscription}
   {:keyword ":expenses/receipts-error"
    :file "src/app/domain/frontend/expenses/subs/receipts.cljs"
    :type :subscription}
   {:keyword ":expenses/receipt"
    :file "src/app/domain/frontend/expenses/subs/receipts.cljs"
    :type :subscription}
   {:keyword ":expenses/receipt-detail-loading?"
    :file "src/app/domain/frontend/expenses/subs/receipts.cljs"
    :type :subscription}
   
   ;; Vendor fork
   {:keyword ":fork.re-frame/server-set-waiting"
    :file "vendor/fork/re_frame.cljs"
    :type :event}])


(defn- admin-entity-keys
  "Load admin entity keys from entities.edn so we can detect dynamic subscription usage."
  []
  (let [p "src/app/admin/frontend/config/entities.edn"]
    (when (fs/exists? p)
      (try
        (let [m (edn/read-string (slurp p))]
          (->> (keys m)
            (filter keyword?)
            set))
        (catch Exception _
          #{})))))

(defn admin-dynamic-subscription-keyword-strings
  "Subscriptions referenced dynamically by app.template.frontend.utils.shared/use-entity-state.

  use-entity-state builds (keyword admin (str (name entity) -loading?)) and similarly for -error.
  We treat those as used for all entities declared in admin entities.edn."
  []
  (let [entities (admin-entity-keys)
        suffixes ["loading?" "error"]]
    (into #{}
      (for [e entities
            s suffixes]
        (str ":admin/" (name e) "-" s)))))
