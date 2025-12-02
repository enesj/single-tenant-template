(ns app.template.frontend.subs.ui
  (:require
    [re-frame.core :as rf]
    [app.admin.frontend.config.loader :as config-loader]))

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

;; Helper functions to get entity-specific or default settings
(defn get-entity-setting
  "Get entity-specific setting or fallback to defaults, then globals, then provided default."
  [db entity-name path default-value]
  (let [sentinel ::not-found
        entity-path (into [:ui :entity-configs entity-name] path)
        default-path (into [:ui :defaults] path)
        global-path (into [:ui] path)
        entity-value (get-in db entity-path sentinel)
        default-value-from-defaults (get-in db default-path sentinel)
        global-value (get-in db global-path sentinel)]
    (cond
      (and (not= entity-value sentinel) (some? entity-value)) entity-value
      (and (not= default-value-from-defaults sentinel) (some? default-value-from-defaults)) default-value-from-defaults
      (and (not= global-value sentinel) (some? global-value)) global-value
      :else default-value)))

(defn- get-display-setting
  "Get a display setting value, checking hardcoded view-options first.
   Hardcoded settings in view-options.edn take precedence over user preferences.
   
   Priority order:
   1. Re-frame db path [:admin :settings :view-options] (updated via Settings page API)
   2. Re-frame db path [:admin :config :view-options] (loaded on app init)
   3. User preferences from entity-configs
   4. Default value
   
   NOTE: We intentionally do NOT check config-loader cache because it contains
   compile-time values that may be stale. The async-loaded config in the db is
   the authoritative source for hardcoded settings."
  [db entity-name setting-key default-value]
  (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
        ;; First check re-frame db for updated view-options (from Settings page API saves)
        settings-view-options (get-in db [:admin :settings :view-options entity-kw])
        settings-has-key? (and settings-view-options (contains? settings-view-options setting-key))
        settings-value (when settings-has-key? (get settings-view-options setting-key))
        ;; Then check re-frame db for config view-options (loaded on app init)
        config-db-view-options (get-in db [:admin :config :view-options entity-kw])
        config-db-has-key? (and config-db-view-options (contains? config-db-view-options setting-key))
        config-db-value (when config-db-has-key? (get config-db-view-options setting-key))
        ;; Check if the key is explicitly hardcoded (exists in view-options)
        is-hardcoded? (or settings-has-key? config-db-has-key?)
        ;; Get user preference - use entity-kw for consistent path lookup
        user-value (get-entity-setting db entity-kw [setting-key] default-value)]
    (if is-hardcoded?
      (or settings-value config-db-value)
      user-value)))

(rf/reg-sub
  ::entity-display-settings
  (fn [db [_ entity-name]]
    (let [show-timestamps? (get-display-setting db entity-name :show-timestamps? true)]
      {:show-timestamps? show-timestamps?
       :show-edit? (get-display-setting db entity-name :show-edit? true)
       :show-delete? (get-display-setting db entity-name :show-delete? true)
       :show-highlights? (get-display-setting db entity-name :show-highlights? true)
       :show-select? (get-display-setting db entity-name :show-select? false)
       :show-filtering? (get-display-setting db entity-name :show-filtering? true)
       :show-pagination? (get-display-setting db entity-name :show-pagination? true)
       :controls {:show-timestamps-control? (get-entity-setting db entity-name [:controls :show-timestamps-control?] true)
                  :show-edit-control? (get-entity-setting db entity-name [:controls :show-edit-control?] true)
                  :show-delete-control? (get-entity-setting db entity-name [:controls :show-delete-control?] true)
                  :show-highlights-control? (get-entity-setting db entity-name [:controls :show-highlights-control?] true)
                  :show-select-control? (get-entity-setting db entity-name [:controls :show-select-control?] true)
                  :show-filtering-control? (get-entity-setting db entity-name [:controls :show-filtering-control?] true)
                  :show-invert-selection? (get-entity-setting db entity-name [:controls :show-invert-selection?] true)
                  :show-delete-selected? (get-entity-setting db entity-name [:controls :show-delete-selected?] true)}})))

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
          ;; Check multiple sources for hardcoded settings (same priority as get-display-setting)
          settings-view-options (get-in db [:admin :settings :view-options entity-kw])
          config-db-view-options (get-in db [:admin :config :view-options entity-kw])
          config-loader-view-options (config-loader/get-view-options entity-kw)]
      ;; Merge all sources, with settings taking precedence
      (merge {}
        config-loader-view-options
        config-db-view-options
        settings-view-options))))

;; Subscription to get the list of filterable fields for an entity
;; Uses vector-config only (no legacy fallback)
(rf/reg-sub
  ::filterable-fields
  (fn [_ [_ entity-name]]
    (when-let [vector-config (config-loader/get-table-config entity-name)]
      (:filterable-columns vector-config))))
;; Note: We intentionally do not fall back to [:ui :entity-configs]
;; to avoid mixing legacy settings with vector-config.

;; Subscription to get the list of visible columns for an entity
(rf/reg-sub
  ::visible-columns
  (fn [db [_ entity-name]]
    ;; Get the visible columns map from the entity config, or nil if not set
    (get-entity-setting db entity-name [:visible-columns] nil)))
;; Note: We don't provide a default map here, instead we handle defaults at rendering time
;; This allows us to properly track which columns have been explicitly set vs. using defaults
