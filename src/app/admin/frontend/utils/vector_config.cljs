(ns app.admin.frontend.utils.vector-config
  "Frontend utilities for vector-based column configuration"
  (:require
    [taoensso.timbre :as log]))

(defn vector-config->boolean-map
  "Convert vector config to boolean map format for existing UI compatibility"
  [vector-config available-columns]
  (let [visible-set (set vector-config)]
    (into {} (map (fn [col]
                    [col (contains? visible-set col)])
               available-columns))))

(defn boolean-map->vector-config
  "Convert boolean map back to ordered vector, preserving original order"
  [boolean-map original-order]
  (filterv #(get boolean-map % false) original-order))

(defn merge-config-with-overrides
  "Merge default vector config with user boolean overrides"
  [default-vector user-overrides available-columns]
  (let [boolean-defaults (vector-config->boolean-map default-vector available-columns)
        merged-boolean (merge boolean-defaults user-overrides)]
    (boolean-map->vector-config merged-boolean available-columns)))

(defn get-user-preferences
  "Get user preferences from localStorage"
  [entity-keyword admin-id]
  (try
    (when (and entity-keyword admin-id)
      (let [entity-name (if (keyword? entity-keyword)
                          (name entity-keyword)
                          (str entity-keyword))
            key (str "admin-table-prefs-" entity-name "-" admin-id)
            stored (js/localStorage.getItem key)]
        (when stored
          (js->clj (.parse js/JSON stored) :keywordize-keys true))))
    (catch js/Error e
      (log/warn "Failed to load user preferences:" e "for entity:" entity-keyword "admin-id:" admin-id)
      nil)))

(defn save-user-preferences!
  "Save user preferences to localStorage"
  [entity-keyword admin-id preferences]
  (try
    (when (and entity-keyword admin-id)
      (let [entity-name (if (keyword? entity-keyword)
                          (name entity-keyword)
                          (str entity-keyword))
            key (str "admin-table-prefs-" entity-name "-" admin-id)
            json (.stringify js/JSON (clj->js preferences))]
        (js/localStorage.setItem key json)
        true))
    (catch js/Error e
      (log/error "Failed to save user preferences:" e "for entity:" entity-keyword "admin-id:" admin-id)
      false)))

(defn get-effective-column-config
  "Get effective column configuration with three-layer merge:
   1. Default config from files
   2. User preferences from localStorage
   3. Final runtime state"
  [entity-keyword default-config admin-id]
  (let [user-prefs (get-user-preferences entity-keyword admin-id)
        available-columns (:available-columns default-config)
        default-visible (:default-visible-columns default-config)
        always-visible (set (:always-visible default-config []))

        ;; Apply user overrides if they exist
        final-visible (if user-prefs
                        (merge-config-with-overrides
                          default-visible
                          (:visible-columns user-prefs)
                          available-columns)
                        default-visible)

        ;; Ensure always-visible columns are included
        final-with-always (vec (concat (filter always-visible available-columns)
                                 (remove always-visible final-visible)))]

    {:visible-columns final-with-always
     :available-columns available-columns
     :always-visible (:always-visible default-config [])
     :column-config (:column-config default-config {})
     :computed-fields (:computed-fields default-config {})}))

(defn toggle-column-visibility
  "Toggle column visibility and return updated config"
  [current-config column-key always-visible]
  (let [visible-columns (:visible-columns current-config)
        always-visible-set (set always-visible)]

    ;; Don't allow toggling always-visible columns
    (if (contains? always-visible-set column-key)
      (do
        (log/warn "Cannot hide always-visible column:" column-key)
        current-config)

      ;; Toggle visibility
      (let [currently-visible? (some #(= % column-key) visible-columns)
            updated-visible (if currently-visible?
                              (filterv #(not= % column-key) visible-columns)
                              (conj visible-columns column-key))]
        (assoc current-config :visible-columns updated-visible)))))

(defn reorder-columns
  "Reorder columns based on drag and drop"
  [current-config from-index to-index]
  (let [visible-columns (:visible-columns current-config)
        reordered (vec (concat (take to-index visible-columns)
                         [(nth visible-columns from-index)]
                         (drop (inc to-index) (concat (take from-index visible-columns)
                                                (drop (inc from-index) visible-columns)))))]
    (assoc current-config :visible-columns reordered)))

(defn reset-to-defaults
  "Reset column configuration to defaults"
  [default-config]
  {:visible-columns (:default-visible-columns default-config)
   :available-columns (:available-columns default-config)
   :always-visible (:always-visible default-config [])
   :column-config (:column-config default-config {})
   :computed-fields (:computed-fields default-config {})})

(defn prepare-for-template-system
  "Convert vector config to format expected by template system"
  [vector-config available-columns]
  (let [boolean-map (vector-config->boolean-map vector-config available-columns)
        display-order (into {} (map-indexed (fn [idx col] [col (inc idx)]) vector-config))]
    {:field-visibility boolean-map
     :display-order display-order
     :visible-columns vector-config}))
