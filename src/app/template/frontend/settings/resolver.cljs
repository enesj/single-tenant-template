(ns app.template.frontend.settings.resolver
  "Unified display settings resolver.
   
   This is the SINGLE source of truth for resolving display settings.
   It produces:
   - :effective — the final computed values used by UI
   - :locked    — map of setting-key → locked-value for settings that cannot be changed
   - :defaults  — the resolved defaults (for reset to default UX)
   
   PRECEDENCE (highest to lowest):
   1. Locks from feature constraints (read-only, batch-ops-disabled)
   2. Locks from view-options.edn / admin settings API
   3. User preferences (new path: [:ui :entity-prefs])
   4. Organization defaults from view-options.edn (if using new schema)
   5. Entity config defaults from entities.edn
   6. Fallback defaults (in-code)
   
   NOTE: Resolver supports both:
   - New explicit schema (:display-defaults / :display-locks)
   - Legacy admin schema where presence of :show-*? keys means 'locked'

   Prefer the explicit schema for new configs.")

;; ============================================================================
;; Fallback defaults (in-code)
;; ============================================================================

(def fallback-defaults
  "Fallback default values for all display settings.
   Used when no other source provides a value."
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

(def all-setting-keys
  "All known display setting keys."
  [:show-timestamps? :show-edit? :show-delete? :show-highlights?
   :show-select? :show-filtering? :show-pagination? :show-add-button?
   :show-batch-edit? :show-batch-delete?])

;; ============================================================================
;; Feature constraint → locks conversion
;; ============================================================================

(defn feature-constraints->locks
  "Convert entity feature flags to locked settings.
   
   Business rules:
   - read-only? → locks edit/delete/add to false
   - batch-operations? false → locks select/batch-edit/batch-delete to false"
  [features]
  (let [{:keys [read-only? batch-operations?]} features]
    (cond-> {}
      read-only?
      (merge {:show-edit? false
              :show-delete? false
              :show-add-button? false})

      (false? batch-operations?)
      (merge {:show-select? false
              :show-batch-edit? false
              :show-batch-delete? false}))))

;; ============================================================================
;; View-options parsing
;; ============================================================================

(defn parse-view-options
  "Parse view-options for an entity.

   Supported schemas:

   1) New schema (Phase 2):
      - :display-defaults — map of defaults
      - :display-locks    — map or set of locked values

   2) Domain schema (user-facing configs):
      - :display-settings — map of defaults (no locks)

   3) Legacy admin schema:
      - flat map where presence of :show-*? keys means 'locked'

   Returns {:defaults {...} :locks {...}}"
  [view-options]
  (cond
    ;; New schema (Phase 2)
    (or (:display-defaults view-options) (:display-locks view-options))
    {:defaults (or (:display-defaults view-options) {})
     :locks (or (:display-locks view-options) {})}

    ;; Domain schema: treat :display-settings as defaults (never locks)
    (map? (:display-settings view-options))
    {:defaults (or (:display-settings view-options) {})
     :locks {}}

    ;; Legacy admin schema: presence = locked
    :else
    (let [display-keys (filter #(and (keyword? %)
                                  (re-matches #"show-.*\?" (name %)))
                         (keys view-options))]
      {:defaults {}
       :locks (select-keys view-options display-keys)})))

;; ============================================================================
;; Main resolver
;; ============================================================================

(defn resolve-display-settings
  "Resolve effective display settings for an entity.
   
   Arguments:
   - entity-key: keyword identifying the entity
   - sources: map containing all data sources:
     - :view-options      — from view-options.edn / admin settings API (merged)
     - :entity-config     — from entities.edn (includes :display-settings, :features)
     - :user-prefs        — from [:ui :entity-prefs entity :display]
     - :legacy-prefs      — from [:ui :entity-configs entity] (deprecated)
   
   Returns:
   {:effective {...}  ; final values for UI
    :locked    {...}  ; keys that are locked (and their locked values)
    :defaults  {...}} ; resolved defaults for 'reset' UX"
  [_entity-key {:keys [view-options entity-config user-prefs legacy-prefs]}]
  (let [;; 1. Parse view-options (locks + defaults)
        {:keys [defaults locks]} (parse-view-options view-options)
        view-options-defaults defaults
        view-options-locks locks

        ;; 2. Get feature-based locks
        features (:features entity-config)
        feature-locks (feature-constraints->locks features)

        ;; 3. Merge all locks (feature locks take precedence, then view-options)
        all-locks (merge view-options-locks feature-locks)

        ;; 4. Build defaults chain:
        ;;    fallback < entity-config < view-options-defaults
        entity-defaults (:display-settings entity-config)
        resolved-defaults (merge fallback-defaults
                            entity-defaults
                            view-options-defaults)

        ;; 5. Build effective values for each setting
        effective (reduce
                    (fn [acc setting-key]
                      (let [;; Check if locked
                            locked? (contains? all-locks setting-key)
                            locked-value (get all-locks setting-key)
                            ;; User preference (new path > legacy)
                            ;; Use contains? instead of or to properly handle false values
                            user-value (cond
                                         (contains? user-prefs setting-key) (get user-prefs setting-key)
                                         (contains? legacy-prefs setting-key) (get legacy-prefs setting-key)
                                         :else nil)
                            ;; Default value
                            default-value (get resolved-defaults setting-key)]
                        (assoc acc setting-key
                          (cond
                            locked? locked-value
                            (some? user-value) user-value
                            :else default-value))))
                    {}
                    all-setting-keys)]

    {:effective effective
     :locked all-locks
     :defaults resolved-defaults}))

;; ============================================================================
;; Convenience accessors
;; ============================================================================

(defn setting-locked?
  "Check if a setting is locked for the given resolved settings."
  [resolved-settings setting-key]
  (contains? (:locked resolved-settings) setting-key))

(defn get-effective
  "Get the effective value for a setting."
  [resolved-settings setting-key]
  (get-in resolved-settings [:effective setting-key]))

(defn get-all-effective
  "Get all effective settings as a flat map."
  [resolved-settings]
  (:effective resolved-settings))
