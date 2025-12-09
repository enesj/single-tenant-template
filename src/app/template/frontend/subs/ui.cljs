(ns app.template.frontend.subs.ui
  "UI state subscriptions for the template frontend.
   
   DISPLAY SETTINGS ARCHITECTURE:
   ==============================
   
   The `::entity-display-settings` subscription is the SINGLE AUTHORITATIVE SOURCE
   for display settings. It handles all merging logic with the following precedence:
   
   1. Hardcoded settings from [:admin :settings :view-options] (API-updated)
   2. Hardcoded settings from [:admin :config :view-options] (app init)
   3. User preferences from [:ui :entity-prefs entity-name :display] (NEW PATH)
   4. Legacy user prefs from [:ui :entity-configs entity-name] (DEPRECATED)
   5. Default values from [:ui :defaults]
   6. Global defaults from [:ui]
   7. Fallback default values (defined in default-display-settings)
   
   USER PREFERENCES PATH STRUCTURE:
   ================================
   New path: [:ui :entity-prefs <entity> :display :show-*]
   Legacy path: [:ui :entity-configs <entity> :show-*] (deprecated, read-only)
   
   RECOMMENDED USAGE:
   - Components should use `app.template.frontend.hooks.display-settings/use-display-settings`
     which wraps this subscription with a cleaner API
   - The hook provides convenience functions like `use-show-select?`, `use-action-visibility`
   - Direct subscription is still supported for backward compatibility"
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  ::recently-updated-entities
  (fn [db [_ entity-type]]
    (let [updated-ids (get-in db [:ui :recently-updated entity-type])]
      updated-ids)))

(rf/reg-sub
  ::recently-created-entities
  (fn [db [_ entity-type]]
    (let [created-ids (get-in db [:ui :recently-created entity-type])]
      created-ids)))

;; ============================================================================
;; Default Display Settings
;; ============================================================================

(def default-display-settings
  "Default values for all display settings.
   These are used as fallback when no other source provides a value."
  {:show-timestamps?    true
   :show-edit?          true
   :show-delete?        true
   :show-highlights?    true
   :show-select?        true
   :show-filtering?     true
   :show-pagination?    true
   :show-add-button?    true
   :show-batch-edit?    false
   :show-batch-delete?  false})

(def default-control-settings
  "Default values for control visibility settings.
   Controls determine whether users can toggle display settings."
  {:show-timestamps-control?  true
   :show-edit-control?        true
   :show-delete-control?      true
   :show-highlights-control?  true
   :show-select-control?      true
   :show-filtering-control?   true
   :show-invert-selection?    true
   :show-delete-selected?     true})

(rf/reg-sub
  ::entity-display-settings
  (fn [db [_ entity-name]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))

          ;; Get hardcoded view-options from all sources (merged in priority order)
          settings-view-options (get-in db [:admin :settings :view-options entity-kw])
          config-view-options (get-in db [:admin :config :view-options entity-kw])
          hardcoded (merge config-view-options settings-view-options)

          ;; Helper to get a setting value with proper precedence
          get-setting (fn [setting-key]
                        (let [;; Check hardcoded first
                              hardcoded-value (get hardcoded setting-key)
                              ;; If hardcoded, that's the value (can't be overridden)
                              ;; Otherwise, check user preferences (new path first, then legacy)
                              new-path-value (get-in db [:ui :entity-prefs entity-kw :display setting-key])
                              legacy-value (get-in db [:ui :entity-configs entity-kw setting-key])
                              defaults-value (get-in db [:ui :defaults setting-key])
                              global-value (get-in db [:ui setting-key])
                              default-value (get default-display-settings setting-key)]
                          (cond
                            ;; Hardcoded takes absolute precedence
                            (contains? hardcoded setting-key) hardcoded-value
                            ;; New path user preferences
                            (some? new-path-value) new-path-value
                            ;; Legacy path user preferences
                            (some? legacy-value) legacy-value
                            ;; UI defaults
                            (some? defaults-value) defaults-value
                            ;; Global UI value
                            (some? global-value) global-value
                            ;; Fallback default
                            :else default-value)))

          ;; Helper to get control visibility (not hardcoded, just user preferences)
          get-control (fn [control-key]
                        (let [new-path-value (get-in db [:ui :entity-prefs entity-kw :display :controls control-key])
                              legacy-value (get-in db [:ui :entity-configs entity-kw :controls control-key])
                              default-value (get default-control-settings control-key)]
                          (cond
                            (some? new-path-value) new-path-value
                            (some? legacy-value) legacy-value
                            :else default-value)))]

      {:show-timestamps?   (get-setting :show-timestamps?)
       :show-edit?         (get-setting :show-edit?)
       :show-delete?       (get-setting :show-delete?)
       :show-highlights?   (get-setting :show-highlights?)
       :show-select?       (get-setting :show-select?)
       :show-filtering?    (get-setting :show-filtering?)
       :show-pagination?   (get-setting :show-pagination?)
       :show-add-button?   (get-setting :show-add-button?)
       :show-batch-edit?   (get-setting :show-batch-edit?)
       :show-batch-delete? (get-setting :show-batch-delete?)
       :controls {:show-timestamps-control?  (get-control :show-timestamps-control?)
                  :show-edit-control?        (get-control :show-edit-control?)
                  :show-delete-control?      (get-control :show-delete-control?)
                  :show-highlights-control?  (get-control :show-highlights-control?)
                  :show-select-control?      (get-control :show-select-control?)
                  :show-filtering-control?   (get-control :show-filtering-control?)
                  :show-invert-selection?    (get-control :show-invert-selection?)
                  :show-delete-selected?     (get-control :show-delete-selected?)}})))

(rf/reg-sub
  ::show-add-form
  (fn [db _]
    (get-in db [:ui :show-add-form])))

(rf/reg-sub
  ::editing
  (fn [db _]
    (get-in db [:ui :editing])))

;; Subscription to get all hardcoded view-options for an entity
;; This is used by the settings panel to hide controls that are admin-locked
(rf/reg-sub
  ::hardcoded-view-options
  (fn [db [_ entity-name]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          ;; Read from app-db sources only (no config-loader cache reads at runtime)
          ;; Priority: settings (API-updated) > config (app init)
          settings-view-options (get-in db [:admin :settings :view-options entity-kw])
          config-db-view-options (get-in db [:admin :config :view-options entity-kw])]
      ;; Merge with settings taking precedence
      (merge {}
        config-db-view-options
        settings-view-options))))

;; Subscription to get the list of filterable fields for an entity
;; Reads from app-db (table-columns config loaded at init)
(rf/reg-sub
  ::filterable-fields
  (fn [db [_ entity-name]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
      ;; Read from app-db, not config-loader cache
      (get-in db [:admin :config :table-columns entity-kw :filterable-columns]))))
;; Note: We intentionally do not fall back to [:ui :entity-configs]
;; to avoid mixing legacy settings with vector-config.

;; Subscription to get the list of visible columns for an entity
(rf/reg-sub
  ::visible-columns
  (fn [db [_ entity-name]]
    ;; Read from new path first, fall back to legacy
    ;; New: [:ui :entity-prefs <entity> :columns :visible]
    ;; Legacy: [:ui :entity-configs <entity> :visible-columns]
    (or (get-in db [:ui :entity-prefs entity-name :columns :visible])
      (get-in db [:ui :entity-configs entity-name :visible-columns])
      nil)))
;; Note: We don't provide a default map here, instead we handle defaults at rendering time
;; This allows us to properly track which columns have been explicitly set vs. using defaults
