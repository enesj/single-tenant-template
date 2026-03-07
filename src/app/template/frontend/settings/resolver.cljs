(ns app.template.frontend.settings.resolver
  "Unified display and list-config resolver.

   Display settings remain the source of truth for list-view toggles.
   List-config resolves structural list behavior that previously lived in page props
   (for example modal-vs-inline and disallowed action mode)."
  (:require
    [app.shared.pagination :as pagination]))

;; ============================================================================
;; Fallback defaults (in-code)
;; ============================================================================

(def fallback-defaults
  "Fallback default values for all display settings.
   Used when no other source provides a value."
  {:show-timestamps? true
   :show-edit? true
   :show-delete? true
   :show-highlights? true
   :show-select? true
   :show-filtering? true
   :show-pagination? true
   :show-add-button? true
   :show-batch-edit? false
   :show-batch-delete? false
   :show-selected-rows? true
   :show-unselected-rows? true
   :per-page pagination/default-page-size})

(def fallback-list-config
  "Fallback defaults for declarative list behavior."
  {:form-display :inline
   :disallowed-action-mode :hide
   :action-gates {}})

(def all-setting-keys
  "All known display setting keys."
  [:show-timestamps? :show-edit? :show-delete? :show-highlights?
   :show-select? :show-filtering? :show-pagination? :show-add-button?
   :show-batch-edit? :show-batch-delete?
   :show-selected-rows? :show-unselected-rows? :per-page])

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

(defn- normalize-list-config-key
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else nil))

(defn- normalize-list-config-value
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else x))

(defn parse-list-config
  "Normalize list-config values from view-options.

   Strings are accepted for JSON/API round-trips and canonicalized to keywords.
   Returns the fallback shape when no list-config exists."
  [view-options]
  (let [list-config (or (:list-config view-options) {})
        action-gates (into {}
                       (keep (fn [[k v]]
                               (when-let [kk (normalize-list-config-key k)]
                                 [kk (normalize-list-config-value v)])))
                       (or (:action-gates list-config) {}))]
    (merge fallback-list-config
      (cond-> {}
        (contains? list-config :form-display)
        (assoc :form-display (normalize-list-config-value (:form-display list-config)))

        (contains? list-config :disallowed-action-mode)
        (assoc :disallowed-action-mode
          (normalize-list-config-value (:disallowed-action-mode list-config)))

        (seq action-gates)
        (assoc :action-gates action-gates)))))

;; ============================================================================
;; Main resolvers
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
  (let [{:keys [defaults locks]} (parse-view-options view-options)
        view-options-defaults defaults
        view-options-locks locks
        features (:features entity-config)
        feature-locks (feature-constraints->locks features)
        all-locks (merge view-options-locks feature-locks)
        entity-defaults (:display-settings entity-config)
        resolved-defaults (merge fallback-defaults
                            entity-defaults
                            view-options-defaults)
        effective (reduce
                    (fn [acc setting-key]
                      (let [locked? (contains? all-locks setting-key)
                            locked-value (get all-locks setting-key)
                            user-value (cond
                                         (contains? user-prefs setting-key) (get user-prefs setting-key)
                                         (contains? legacy-prefs setting-key) (get legacy-prefs setting-key)
                                         :else nil)
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

(defn resolve-list-config
  "Resolve normalized list-config for an entity.

   list-config is view-options-owned. It does not participate in browser-local
   prefs and is intentionally separate from display toggle resolution."
  [_entity-key {:keys [view-options]}]
  (parse-list-config view-options))
