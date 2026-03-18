(ns app.template.frontend.settings.resolver
  "Unified settings resolver for display, list-config, table-columns, and form-fields.

   Provides:
   - Route-aware config source resolution (admin vs domain configs)
   - Display settings resolution with precedence (locks > prefs > defaults)
   - List-config resolution (form-display, action-gates)
   - Shared column utilities (label resolution, key normalization)

   All subscriptions that need route-aware config lookup should call
   `resolve-config-source` rather than independently checking admin-route?."
  (:require
    [app.shared.keywords :as kw]
    [app.shared.labels :as labels]
    [app.shared.model-naming :as model-naming]
    [app.shared.pagination :as pagination]
    [app.template.frontend.i18n :as i18n]
    [clojure.string :as str]))

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

;; ============================================================================
;; Route-Aware Config Source Resolution
;; ============================================================================

(defn resolve-config-source
  "Route-aware config source resolution.

   Admin routes prefer admin config with domain fallback.
   User routes prefer domain config with admin fallback.

   Used for table-columns, form-fields, view-options, and any other per-entity
   config that has separate admin and domain definitions.

   This is the single point of truth for admin-vs-domain config selection.
   All subscriptions should call this rather than independently checking
   admin-route? and doing (if admin-route? admin-config domain-config)."
  [admin-route? admin-config domain-config]
  (if admin-route?
    (or admin-config domain-config)
    (or domain-config admin-config)))

;; ============================================================================
;; Shared Column Utilities
;;
;; These functions are used by entity_specs.cljs, generic.cljs, and ui.cljs
;; for column key normalization and label resolution. Extracted here to
;; eliminate duplication and ensure consistent behavior.
;; ============================================================================

(defn app-col->db-col
  "Best-effort conversion from app/kebab-case keyword to db/snake_case keyword.

  Used for looking up computed field metadata in table-columns config, where
  keys are often authored in snake_case (e.g. :supplier_display_name)."
  [col-kw]
  (when col-kw
    (-> col-kw name (str/replace "-" "_") keyword)))

(defn column-key-candidates
  "Return all plausible lookup keys for a column identifier.

   Covers keyword/string, app-case/db-case variants so lookups succeed
   regardless of which convention the config map uses."
  [column-key]
  (let [app-kw (some-> column-key model-naming/ensure-app-keyword)
        app-name (some-> app-kw name)
        db-kw (some-> app-kw app-col->db-col)
        db-name (some-> db-kw name)]
    (->> [column-key
          (when (keyword? column-key) (name column-key))
          (when (string? column-key) (keyword column-key))
          app-kw
          app-name
          db-kw
          db-name]
      (remove nil?)
      distinct
      vec)))

(defn lookup-column-entry
  "Look up a value in a map using all plausible key variants for a column.

   Returns the first matching value, or nil if none found."
  [m column-key]
  (let [sentinel ::not-found]
    (some (fn [k]
            (let [v (get m k sentinel)]
              (when-not (= v sentinel) v)))
      (column-key-candidates column-key))))

(defn translate-label-key
  "Translate an i18n label-key to a localized string.

   Returns the translated string, or nil if translation fails or matches
   the key itself (indicating no translation exists)."
  [locale label-key]
  (when-let [translation-key (some-> label-key kw/ensure-keyword)]
    (let [translated (try
                       (i18n/translate locale translation-key)
                       (catch :default _
                         nil))
          translated-str (some-> translated str str/trim)]
      (when (and (seq translated-str)
              (not= translated translation-key)
              (not= translated-str (str translation-key)))
        translated-str))))

(defn resolve-column-label-override
  "Resolve a display label override for a column from table-columns metadata.

   Checks column-metadata for :label (explicit admin override) and :label-key (i18n).
   An explicit :label takes priority over :label-key because it represents a
   deliberate admin customization, while :label-key is a default i18n mapping.
   Returns the resolved label, or nil if no override exists."
  [locale table-config column-key]
  (let [{:keys [label label-key]} (lookup-column-entry (:column-metadata table-config) column-key)
        static-label (some-> label str str/trim)
        translated-label (translate-label-key locale label-key)]
    (or (when (seq static-label)
          static-label)
      translated-label)))

(defn apply-column-label-override
  "Apply a label override to a field-spec if one exists in table-columns metadata."
  [locale table-config column-key field-spec]
  (if-let [label (resolve-column-label-override locale table-config column-key)]
    (assoc field-spec :label label)
    field-spec))

(defn computed-field-spec
  "Build a field spec for a computed column (one defined in table-columns
   :computed-fields but not in the backend model).

   Falls back to labels/field-name->label when no label override exists."
  [locale table-config col-kw]
  (let [m (lookup-column-entry (:computed-fields table-config) col-kw)
        label (or (resolve-column-label-override locale table-config col-kw)
                (:label m)
                (labels/field-name->label col-kw))]
    {:id    (name col-kw)
     :label label
     :type  (or (:type m) :string)
     :admin (merge
              {:visible-in-table? true
               :filterable? true
               :sortable? true}
              (:admin m))}))
