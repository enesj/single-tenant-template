#!/usr/bin/env bb
;; Check unused re-frame subscriptions and events
;; This script analyzes re-frame keywords flagged as unused and checks if they're actually used

(ns code-quality.check-unused-reframe
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]))

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
    :file "src/app/admin/frontend/events/settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.settings/update-entity-setting"
    :file "src/app/admin/frontend/events/settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.settings/remove-entity-setting"
    :file "src/app/admin/frontend/events/settings.cljs"
    :type :event}
   
   ;; Admin frontend events/user_settings.cljs
   {:keyword ":app.admin.frontend.events.user-settings/set-column-defaults-bulk"
    :file "src/app/admin/frontend/events/user_settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.user-settings/saved"
    :file "src/app/admin/frontend/events/user_settings.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.events.user-settings/set-form-field-list-draft"
    :file "src/app/admin/frontend/events/user_settings.cljs"
    :type :event}
   {:keyword ":app.admin.frontend.events.user-settings/entities-config"
    :file "src/app/admin/frontend/events/user_settings.cljs"
    :type :subscription}
   {:keyword ":app.admin.frontend.events.user-settings/form-fields-config"
    :file "src/app/admin/frontend/events/user_settings.cljs"
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
    :file "src/app/template/frontend/events/auth.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.auth/clear-change-password-state"
    :file "src/app/template/frontend/events/auth.cljs"
    :type :event}
   {:keyword ":app.template.frontend.events.auth/clear-password-reset-state"
    :file "src/app/template/frontend/events/auth.cljs"
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
    :file "src/app/template/frontend/events/user_expenses.cljs"
    :type :event}
   {:keyword ":user-expenses/upload-receipt-failure"
    :file "src/app/template/frontend/events/user_expenses.cljs"
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

(defn- safe-slurp [file-path]
  (try
    (slurp file-path)
    (catch Exception _
      nil)))

(defn search-in-file
  "Search for a regex pattern in a file, return line numbers where found.

  pattern can be either a java.util.regex.Pattern or a string (compiled to a regex)."
  [file-path pattern]
  (when (fs/exists? file-path)
    (when-let [content (safe-slurp file-path)]
      (let [re (if (instance? java.util.regex.Pattern pattern)
                 pattern
                 (re-pattern (java.util.regex.Pattern/quote (str pattern))))
            lines (str/split-lines content)]
        (->> lines
          (map-indexed
            (fn [idx line]
              (when (re-find re line)
                {:line (inc idx)
                 :content (str/trim line)})))
          (filter some?)
          vec)))))

(defn- parse-keyword-str
  "Parse a keyword string like ':admin/foo' or ':my.ns/bar' or ':foo'."
  [keyword-str]
  (let [s (cond-> keyword-str (str/starts-with? keyword-str ":") (subs 1))
        parts (str/split s #"/" 2)]
    (if (= 2 (count parts))
      {:qualified? true
       :ns (first parts)
       :name (second parts)
       :full s}
      {:qualified? false
       :name (first parts)
       :full s})))

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

(defn- admin-dynamic-subscription-keyword-strings
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

(defn get-search-patterns
  "Convert keyword string to regex patterns.

  Handles:
  - literal keywords: :admin/foo
  - dispatch/subscribe vectors: [:admin/foo ...]
  - keyword in strings: admin/foo
  - auto-resolved keywords used via ::kw or ::alias/kw for qualified keywords.
    Example: :app.admin.frontend.subs.config/column-visible? may be written as
    ::column-visible? (inside that ns) or ::admin-subs/column-visible? (outside).
  "
  [keyword-str]
  (let [{:keys [qualified? ns name full]} (parse-keyword-str keyword-str)
        escaped-full (java.util.regex.Pattern/quote (str ":" full))
        ;; For unqualified keywords, full is the name.
        escaped-unqualified (java.util.regex.Pattern/quote (str ":" (if qualified? full name)))
        base (if qualified? escaped-full escaped-unqualified)
        ;; Auto-resolved forms for qualified keywords.
        escaped-name (when qualified? (java.util.regex.Pattern/quote name))]
    (cond->
      [;; literal keyword
       (re-pattern base)
       ;; dispatch/subscribe vector prefix
       (re-pattern (str "\\[" base))
       ;; string form without leading colon
       (re-pattern (str "\"" (java.util.regex.Pattern/quote (if qualified? full name)) "\""))]

      qualified?
      (into
        [;; ::name (in defining ns)
         (re-pattern (str "::" escaped-name "\\b"))
         ;; ::alias/name (in other namespaces)
         (re-pattern (str "::[A-Za-z0-9_.-]+/" escaped-name "\\b"))]))))

(defn search-codebase
  "Search for keyword usage across the codebase (src + test).

  Returns matches in *all* files (including the definition file), because internal dependencies
  between subscriptions/events matter when deciding what can be safely commented out." 
  [keyword-info]
  (let [keyword-str (:keyword keyword-info)
    patterns (get-search-patterns keyword-str)
    files (concat
        (fs/glob "src" "**/*.{cljs,cljc,clj,edn}")
        (fs/glob "test" "**/*.{cljs,cljc,clj,edn}"))]
    (for [file files
      :let [file-path (str file)]
      pattern patterns
      :let [matches (search-in-file file-path pattern)]
      :when (seq matches)]
  {:file file-path
   :pattern (str pattern)
   :matches matches})))

(defn analyze-keyword [keyword-info]
  "Analyze a single keyword for usage"
  (let [usages (search-codebase keyword-info)
        dynamic-admin-subs (admin-dynamic-subscription-keyword-strings)
        dynamic? (and (= (:type keyword-info) :subscription)
                   (contains? dynamic-admin-subs (:keyword keyword-info)))
        def-file (:file keyword-info)
        matches-in-def-file (->> usages
                              (filter (fn [u]
                                        ;; Normalize paths for comparison: keyword-info has src/... style.
                                        (= (:file u) def-file)))
                              vec)
        external-usages (->> usages
                         (remove (fn [u] (= (:file u) def-file)))
                         vec)]
    (assoc keyword-info
      :usages (vec usages)
      :external-usages external-usages
      :definition-usages matches-in-def-file
      :used-dynamically? dynamic?
      :used? (or dynamic? (boolean (seq external-usages))))))

(defn format-results [results]
  "Format results for display"
  (let [unused (filter (complement :used?) results)
        used (filter :used? results)
        dynamic (filter :used-dynamically? results)]
    {:summary {:total (count results)
               :used (count used)
               :used-dynamically (count dynamic)
               :unused (count unused)}
     :used (map #(select-keys % [:keyword :file :type :usages :used-dynamically?]) used)
     :unused (map #(select-keys % [:keyword :file :type]) unused)}))

(defn print-results [formatted]
  "Print results to console"
  (println "\n=== Re-frame Unused Keyword Analysis ===\n")
  (println (format "Total keywords analyzed: %d" (get-in formatted [:summary :total])))
  (println (format "Actually used: %d" (get-in formatted [:summary :used])))
  (println (format "Used dynamically (heuristic): %d" (get-in formatted [:summary :used-dynamically])))
  (println (format "Truly unused: %d" (get-in formatted [:summary :unused])))
  
  (println "\n--- USED KEYWORDS (can keep) ---")
  (doseq [item (:used formatted)]
    (println (format "\n✓ %s (%s)" (:keyword item) (:type item)))
    (println (format "  Defined in: %s" (:file item)))
    (println "  Used in:")
    (doseq [usage (:usages item)]
      (println (format "    - %s" (:file usage)))
      (doseq [match (:matches usage)]
        (println (format "      Line %d: %s" 
                        (:line match) 
                        (subs (:content match) 0 (min 80 (count (:content match)))))))))
  
  (println "\n\n--- UNUSED KEYWORDS (candidates for removal) ---")
  (doseq [item (:unused formatted)]
    (println (format "\n✗ %s (%s)" (:keyword item) (:type item)))
    (println (format "  Defined in: %s" (:file item)))))

(defn group-by-file [unused-items]
  "Group unused items by their definition file"
  (->> unused-items
       (group-by :file)
       (sort-by key)))

(defn- keyword-source-forms
  "Return possible textual keyword forms used in source for a keyword string.

  - Always includes the fully-qualified literal form (e.g. :admin/foo or :my.ns/bar).
  - For qualified keywords, also includes the auto-resolved local form ::name (common in its defining ns)."
  [keyword-str]
  (let [{:keys [qualified? ns name full]} (parse-keyword-str keyword-str)
        full-form (str ":" full)
        ;; Only include ::name when the keyword namespace looks like a fully-qualified
        ;; Clojure namespace (contains dots). For simple namespaces like :admin/foo,
        ;; ::foo would resolve to the *current* namespace and is unrelated.
        include-auto? (and qualified?
                        (string? ns)
                        (str/includes? ns "."))]
    (cond-> [full-form]
      include-auto?
      (conj (str "::" name)))))

(defn- candidate-reg-macros
  "Possible rf registration macros for a given keyword type." 
  [kw-type]
  (case kw-type
    :subscription ["reg-sub"]
    :event ["reg-event-db" "reg-event-fx" "reg-event"]
    ;; fallback
    ["reg-sub" "reg-event-db" "reg-event-fx" "reg-event"]))

(defn- find-and-comment-registration
  "Given file content, try to comment out exactly one rf registration form for keyword.

  Uses #_ reader macro inserted after indentation (so clojure-lsp and the reader ignore it).

  Returns {:status :changed, :content <new>} or {:status :skipped, :reason ...}"
  [{:keys [keyword type] :as kw-info} content]
  (if-not (string? content)
    {:status :skipped
     :reason :unreadable-content
     :keyword keyword}
    (let [kw-forms (keyword-source-forms keyword)
      macros (candidate-reg-macros type)
      ;; Build regexes that match either multiline form:
      ;; (rf/reg-sub\n  :kw ...)
      ;; or same-line: (rf/reg-sub :kw ...)
      patterns (for [m macros
         kwf kw-forms
         :let [kwq (java.util.regex.Pattern/quote kwf)
               ;; Important: use horizontal whitespace (\\h) so the same-line matcher does NOT
               ;; also match multiline forms (\\s includes newlines).
                               ;; Match any re-frame namespace alias (commonly rf, but sometimes re-frame.core).
                               ns-prefix "(?:[A-Za-z0-9_.-]+/)"
                               mline (re-pattern (str "(?ms)^(\\h*)(?!#_)(\\(" ns-prefix m "\\h*\\R\\h*" kwq "(?:\\s|\\R|\\))" ")"))
                               sline (re-pattern (str "(?m)^(\\h*)(?!#_)(\\(" ns-prefix m "\\h+" kwq "(?:\\s|$|\\))" ")"))
                               ;; Detect already-commented registrations (idempotent apply).
                               mline-commented (re-pattern (str "(?ms)^\\h*#_\\(" ns-prefix m "\\h*\\R\\h*" kwq "(?:\\s|\\R|\\))"))
                               sline-commented (re-pattern (str "(?m)^\\h*#_\\(" ns-prefix m "\\h+" kwq "(?:\\s|$|\\))"))]]
         {:macro m
          :kw-form kwf
          :regexes [mline sline]
          :commented-regexes [mline-commented sline-commented]})
      matches (->> patterns
        (mapcat
          (fn [{:keys [macro kw-form regexes] :as p}]
        (for [re regexes
          :let [m (re-find re content)]
          :when m]
          (assoc p :re re :match m))))
        vec)]
  (cond
    (empty? matches)
    (let [already-commented?
          (some true?
            (for [{:keys [commented-regexes]} patterns
                  re commented-regexes]
              (boolean (re-find re content))))]
      (if already-commented?
        {:status :unchanged
         :reason :already-commented
         :keyword keyword}
        {:status :skipped
         :reason :not-found
         :keyword keyword}))

    (> (count matches) 1)
    {:status :skipped
     :reason :ambiguous
     :keyword keyword
     :matches (mapv #(select-keys % [:macro :kw-form]) matches)}

    :else
    (let [{:keys [re match]} (first matches)
      ;; match is [full-match indent+form]
      indent (nth match 1)
      form-start (nth match 2)
      replacement (str indent "#_" form-start)
      new-content (str/replace-first content re (java.util.regex.Matcher/quoteReplacement replacement))]
      {:status :changed
       :keyword keyword
       :content new-content})))))

(defn- apply-comment-outs!
  "Apply #_ comment-outs for all truly-unused items.

  Returns a report map with :changed and :skipped entries." 
  [unused-items]
  (let [candidates (->> unused-items
                     ;; never touch vendor code
                     (remove (fn [{:keys [file]}] (str/starts-with? (or file "") "vendor/")))
                     ;; only files that exist
                     (filter (fn [{:keys [file]}] (and file (fs/exists? file))))
                     vec)
        grouped (group-by :file candidates)]
    (reduce-kv
      (fn [acc file items]
        (let [orig (safe-slurp file)]
          (if-not (string? orig)
            (update acc :skipped into (mapv (fn [item]
                                             {:file file
                                              :keyword (:keyword item)
                                              :reason :unreadable-file})
                                           items))
            (let [{:keys [content changes skips]}
                  (reduce
                    (fn [{:keys [content changes skips] :as st} item]
                      (let [{:keys [status] :as result} (find-and-comment-registration item content)]
                        (case status
                          :changed {:content (:content result)
                                    :changes (conj changes (select-keys result [:keyword]))
                                    :skips skips}
                          :unchanged st
                          :skipped {:content content
                                    :changes changes
                                    :skips (conj skips (select-keys result [:keyword :reason :matches]))}
                          st)))
                    {:content orig :changes [] :skips []}
                    items)]
              (when (not= orig content)
                (spit file content))
              (-> acc
                (update :changed into (map (fn [c] (assoc c :file file)) changes))
                (update :skipped into (map (fn [s] (assoc s :file file)) skips)))))))
      {:changed [] :skipped []}
      grouped)))

(defn -main [& args]
  (let [verbose? (some #{"--verbose" "-v"} args)
  apply? (some #{"--apply" "--fix"} args)
        results (mapv analyze-keyword unused-keywords)
        formatted (format-results results)]
    
    (if verbose?
      (print-results formatted)
      (do
        (println "\n=== Re-frame Unused Keyword Analysis ===\n")
        (println (format "Total keywords analyzed: %d" (get-in formatted [:summary :total])))
        (println (format "Actually used: %d" (get-in formatted [:summary :used])))
        (println (format "Used dynamically (heuristic): %d" (get-in formatted [:summary :used-dynamically])))
        (println (format "Truly unused: %d" (get-in formatted [:summary :unused])))
        
        (println "\n--- TRULY UNUSED (grouped by file) ---")
        (doseq [[file items] (group-by-file (:unused formatted))]
          (println (format "\n%s:" file))
          (doseq [item items]
            (println (format "  - %s (%s)" (:keyword item) (name (:type item))))))
        
        (println "\n--- ACTUALLY USED (summary) ---")
        (doseq [item (:used formatted)]
          (println (format "  ✓ %s - used in %d files" 
                          (:keyword item) 
                          (count (:usages item)))))))

    (when apply?
      (let [unused-items (:unused formatted)
            report (apply-comment-outs! unused-items)
            changed (:changed report)
            skipped (:skipped report)]
        (println "\n--- APPLY MODE ---")
        (println (format "Commented out: %d registrations" (count changed)))
        (println (format "Skipped: %d registrations" (count skipped)))
        (when (seq skipped)
          (println "\nSkipped details (first 25):")
          (doseq [s (take 25 skipped)]
            (println (format "  - %s (%s) in %s" (:keyword s) (name (:reason s)) (:file s)))))))
    
    ;; Return exit code based on results
    (System/exit 0)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
