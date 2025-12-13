(ns app.template.frontend.subs.ui
  "UI state subscriptions for the template frontend.
   
   DISPLAY SETTINGS ARCHITECTURE (Simplified):
   ===========================================
   
   Uses the unified resolver at `app.template.frontend.settings.resolver`.
   
   The `::resolved-display-settings` subscription returns:
   - :effective — final computed values for UI
   - :locked    — map of locked setting-key → locked-value
   - :defaults  — resolved defaults for 'reset' UX
   
   The legacy `::entity-display-settings` subscription returns just the effective
   values for backward compatibility.
   
   PRECEDENCE (highest → lowest):
   1. Locks from feature constraints (read-only, batch-ops-disabled)
   2. Locks from view-options.edn / admin settings API
   3. User preferences from [:ui :entity-prefs]
   4. Entity config defaults from entities.edn
   5. Fallback defaults (in-code)"
  (:require
    [app.template.frontend.settings.resolver :as resolver]
    [clojure.string :as str]
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
;; Default Display Settings (kept for backward compatibility)
;; ============================================================================

(def default-display-settings
  "Default values for all display settings.
   DEPRECATED: Use resolver/fallback-defaults instead."
  resolver/fallback-defaults)

;; ============================================================================
;; Unified Display Settings Resolution
;; ============================================================================

(defn- gather-resolver-sources
  "Gather all sources needed for the display settings resolver from app-db.

  NOTE: We support two independent config sources:
  - Admin routes: [:admin :config] (loaded via /admin/api/*)
  - User routes:  [:domain :config] (preloaded domain-owned config)

  This keeps admin vs user defaults separate even when they share an entity name
  (e.g. :expenses appears on both /admin/* and /expenses/*).

  User preferences ([:ui :entity-prefs]) still apply to both."
  [db entity-kw]
  (let [handler (get-in db [:routing :handler])
        admin-route? (and handler (str/starts-with? (name handler) "admin"))

        settings-view-options (get-in db [:admin :settings :view-options entity-kw])
        config-view-options (get-in db [:admin :config :view-options entity-kw])
        domain-view-options (get-in db [:domain :config :view-options entity-kw])

        view-options (if admin-route?
                       (merge config-view-options settings-view-options)
                       domain-view-options)

        entity-config (if admin-route?
                      ;; Entity config from entities.edn (admin registry)
                        (or (get-in db [:admin :entity-registry entity-kw]) {})
                      ;; Domain-owned entities.edn (user routes)
                        (or (get-in db [:domain :config :entities entity-kw]) {}))

        user-prefs (get-in db [:ui :entity-prefs entity-kw :display])
        legacy-prefs (get-in db [:ui :entity-configs entity-kw])]
    {:view-options view-options
     :entity-config entity-config
     :user-prefs user-prefs
     :legacy-prefs legacy-prefs}))

;; Returns the full resolved settings including :effective, :locked, and :defaults.
;; This is the primary subscription for components that need lock information.
(rf/reg-sub
  ::resolved-display-settings
  (fn [db [_ entity-name]]
    (if entity-name
      (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
        (resolver/resolve-display-settings
          entity-kw
          (gather-resolver-sources db entity-kw)))
      ;; Return default structure if no entity name
      {:effective resolver/fallback-defaults
       :locked {}
       :defaults resolver/fallback-defaults})))

;; Returns just the effective display settings for backward compatibility.
;; Components that need lock information should use ::resolved-display-settings.
(rf/reg-sub
  ::entity-display-settings
  (fn [db [_ entity-name]]
    (if entity-name
      (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
        (:effective (resolver/resolve-display-settings
                      entity-kw
                      (gather-resolver-sources db entity-kw))))
      ;; Return fallback defaults if no entity name
      resolver/fallback-defaults)))

;; Returns the map of locked settings (setting-key → locked-value).
;; Use this to determine which toggles should be hidden in the settings panel.
(rf/reg-sub
  ::locked-display-settings
  (fn [db [_ entity-name]]
    (if entity-name
      (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
        (:locked (resolver/resolve-display-settings
                   entity-kw
                   (gather-resolver-sources db entity-kw))))
      ;; Return empty map if no entity name provided
      {})))

(rf/reg-sub
  ::show-add-form
  (fn [db _]
    (get-in db [:ui :show-add-form])))

(rf/reg-sub
  ::editing
  (fn [db _]
    (get-in db [:ui :editing])))

;; Subscription to get all locked view-options for an entity
;; This is used by the settings panel to hide controls that cannot be changed
;; DEPRECATED: Use ::locked-display-settings instead.
;; Returns locked settings for backward compatibility with settings panel.
(rf/reg-sub
  ::hardcoded-view-options
  (fn [[_ entity-name]]
    (rf/subscribe [::locked-display-settings entity-name]))
  (fn [locked _]
    ;; Return as-is or empty map; the locked map now includes feature constraint locks too
    (or locked {})))

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
    ;; Precedence:
    ;; 1) Per-user prefs ([:ui :entity-prefs])
    ;; 2) Legacy prefs ([:ui :entity-configs])
    ;; 3) Config defaults (admin or domain table-columns)
    (let [entity-kw (cond
                      (nil? entity-name) nil
                      (keyword? entity-name) entity-name
                      :else (keyword entity-name))
          explicit (when entity-kw
                     (get-in db [:ui :entity-prefs entity-kw :columns :visible]))
          legacy (when entity-kw
                   (get-in db [:ui :entity-configs entity-kw :visible-columns]))
          handler (get-in db [:routing :handler])
          admin-route? (and handler (str/starts-with? (name handler) "admin"))
          table-config (when entity-kw
                         (if admin-route?
                           (get-in db [:admin :config :table-columns entity-kw])
                           (get-in db [:domain :config :table-columns entity-kw])))
          normalize-col (fn [k]
                          (cond
                            (nil? k) nil
                            (keyword? k) k
                            (string? k) (keyword k)
                            :else (keyword (str k))))
          available (->> (or (:available-columns table-config) [])
                      (keep normalize-col)
                      vec)
          hidden (cond
                   (seq (:default-hidden-columns table-config))
                   (->> (:default-hidden-columns table-config)
                     (keep normalize-col)
                     vec)

                   ;; Vector-config shape: derive hidden = available - default-visible
                   (and (seq available) (seq (:default-visible-columns table-config)))
                   (let [visible-set (into #{} (keep normalize-col) (:default-visible-columns table-config))]
                     (->> available
                       (remove visible-set)
                       vec))

                   :else nil)
          defaults (when (seq hidden)
                     ;; Provide explicit false entries for hidden columns.
                     ;; Rendering treats missing keys as visible.
                     (into {} (map (fn [k] [k false]) hidden)))]
      (or explicit legacy defaults nil))))
;; Note: We don't provide a default true-map here.
;; Rendering treats missing keys as visible, so we only emit explicit false entries.
