(ns app.template.frontend.interceptors.persistence
  "Persistence interceptor for UI preferences.
   
   Provides localStorage persistence for user preferences stored at
   [:ui :entity-prefs]. Automatically saves preferences after any
   event that modifies them.
   
   Also provides migration utilities for legacy localStorage keys."
  (:require
    [app.shared.model-naming :as model-naming]
    [cljs.reader :as reader]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- deep-merge
  "Recursively merge maps.

  When values are both maps, merges them; otherwise the right-most value wins."
  [& ms]
  (letfn [(m [a b]
            (merge-with
              (fn [x y]
                (if (and (map? x) (map? y))
                  (m x y)
                  y))
              (or a {})
              (or b {})))]
    (reduce m {} ms)))

(defn- normalize-pref-keyed-map
  "Normalize a map keyed by identifiers to a canonical app keyword map.

  Keeps values as-is; drops entries whose keys cannot be normalized." 
  [m]
  (when (map? m)
    (into {}
      (keep (fn [[k v]]
              (when-let [k' (model-naming/ensure-app-keyword k)]
                [k' v])))
      m)))

(defn- normalize-pref-key-vec
  "Normalize a sequential of identifiers to a vector of canonical app keywords.

  Drops items which cannot be normalized." 
  [xs]
  (when (sequential? xs)
    (->> xs
      (keep model-naming/ensure-app-keyword)
      vec)))

(defn- normalize-entity-pref
  "Normalize known nested shapes inside a single entity's prefs map.

  We intentionally restrict normalization to nested maps/vectors which are
  known to contain field/column identifiers, to avoid rewriting arbitrary
  stored user data." 
  [x]
  (if (map? x)
    (cond-> x
      (map? (get-in x [:columns :visible]))
      (update-in [:columns :visible] normalize-pref-keyed-map)

      (sequential? (get-in x [:columns :visible-order]))
      (update-in [:columns :visible-order] normalize-pref-key-vec)

      (map? (get-in x [:filters :fields]))
      (update-in [:filters :fields] normalize-pref-keyed-map))
    x))

(defn- normalize-stored-prefs
  "Normalize top-level entity keys in the persisted prefs map.

  This migrates old prefs that were accidentally stored under snake_case
  (e.g. :price_observations) to canonical kebab-case (:price-observations)." 
  [prefs]
  (when (map? prefs)
    (reduce-kv
      (fn [acc k v]
        (if-let [k' (model-naming/ensure-app-keyword k)]
          (deep-merge acc {k' (normalize-entity-pref v)})
          acc))
      {}
      prefs)))

(def ^:private storage-key "ui-entity-prefs")

(defn- safe-read-edn
  "Safely read EDN string, returns nil on error."
  [s]
  (when (and s (string? s) (seq s))
    (try
      (reader/read-string s)
      (catch :default e
        (log/warn "Failed to parse stored entity-prefs:" (.-message e))
        nil))))

(defn- get-stored-prefs
  "Load entity preferences from localStorage."
  []
  (when (exists? js/localStorage)
    (-> js/localStorage
      (.getItem storage-key)
      safe-read-edn)))

(defn- save-prefs!
  "Save entity preferences to localStorage."
  [prefs]
  (when (and (exists? js/localStorage) prefs)
    (try
      (.setItem js/localStorage storage-key (pr-str prefs))
      (log/debug "Saved entity-prefs to localStorage")
      (catch :default e
        (log/warn "Failed to save entity-prefs:" (.-message e))))))

;; ============================================================================
;; Effect Handler for explicit persistence
;; ============================================================================

(rf/reg-fx
 :persist-entity-prefs
  (fn [prefs]
    (save-prefs! prefs)))

;; ============================================================================
;; Cofx for loading stored prefs
;; ============================================================================

(rf/reg-cofx
  :stored-entity-prefs
  (fn [cofx _]
    (assoc cofx :stored-entity-prefs (get-stored-prefs))))

;; ============================================================================
;; Interceptor for auto-persistence
;; ============================================================================

(def persist-entity-prefs
  "Interceptor that persists [:ui :entity-prefs] after the event handler runs.
   
   Add this to events that modify entity preferences to enable automatic
   localStorage persistence."
  (rf/->interceptor
    :id :persist-entity-prefs
    :after (fn [context]
             (let [db (rf/get-effect context :db)
                   prefs (when db (get-in db [:ui :entity-prefs]))]
               (when prefs
                 (save-prefs! prefs)))
             context)))

;; ============================================================================
;; Event for loading stored prefs on app init
;; ============================================================================

(rf/reg-event-fx
  ::load-stored-prefs
  [(rf/inject-cofx :stored-entity-prefs)]
  (fn [{:keys [db stored-entity-prefs]} _]
    (if stored-entity-prefs
      (let [normalized (or (normalize-stored-prefs stored-entity-prefs) stored-entity-prefs)]
        (log/info "Loaded entity-prefs from localStorage:" (keys normalized))
        {:db (update-in db [:ui :entity-prefs] #(deep-merge % normalized))})
      {:db db})))

;; ============================================================================
;; Legacy Migration Utilities
;; ============================================================================

(def ^:private legacy-column-visibility-prefix "column-visibility-")

(defn- get-legacy-column-visibility
  "Load column visibility from legacy localStorage key.
   Returns a vector of visible column keywords, or nil if not found."
  [entity-name]
  (when (exists? js/localStorage)
    (let [key (str legacy-column-visibility-prefix (name entity-name))
          stored (.getItem js/localStorage key)]
      (when stored
        (try
          (->> (js/JSON.parse stored)
            js->clj
            (keep (fn [col]
                    (cond
                      (keyword? col) col
                      (string? col) (keyword col)
                      :else nil)))
            vec)
          (catch :default e
            (log/warn "Failed to parse legacy column-visibility:" (.-message e))
            nil))))))

(defn- delete-legacy-column-visibility!
  "Remove legacy localStorage key for column visibility."
  [entity-name]
  (when (exists? js/localStorage)
    (let [key (str legacy-column-visibility-prefix (name entity-name))]
      (.removeItem js/localStorage key)
      (log/debug "Removed legacy key:" key))))

(defn- vector->visibility-map
  "Convert a vector of visible columns to a visibility map.
   All columns in the vector are marked true.
   
   Note: This loses information about which columns are hidden.
   For admin entities that need the vector (order-preserving) format,
   we also store :visible-order."
  [visible-columns-vec]
  (into {} (map (fn [col] [col true]) visible-columns-vec)))

(defn migrate-legacy-column-visibility!
  "Migrate column visibility from legacy format to unified prefs.
   
   - Reads from localStorage key `column-visibility-<entity>`
   - Writes to [:ui :entity-prefs <entity> :columns :visible-order] (vector)
   - Also creates [:ui :entity-prefs <entity> :columns :visible] (map) for compatibility
   - Optionally deletes the legacy key after successful migration
   
   Returns the migrated data or nil if no migration needed."
  [entity-name & {:keys [delete-legacy?] :or {delete-legacy? false}}]
  (when-let [legacy-columns (get-legacy-column-visibility entity-name)]
    (log/info "Migrating column visibility for" entity-name ":" legacy-columns)
    (let [prefs (get-stored-prefs)
          entity-key (model-naming/ensure-app-keyword entity-name)
          updated-prefs (-> prefs
                          (assoc-in [entity-key :columns :visible-order] legacy-columns)
                          (assoc-in [entity-key :columns :visible] (vector->visibility-map legacy-columns)))]
      (save-prefs! updated-prefs)
      (when delete-legacy?
        (delete-legacy-column-visibility! entity-name))
      {:visible-order legacy-columns
       :visible (vector->visibility-map legacy-columns)})))

(rf/reg-fx
 :migrate-legacy-column-visibility
  (fn [{:keys [entity-name delete-legacy?]}]
    (migrate-legacy-column-visibility! entity-name :delete-legacy? delete-legacy?)))

;; Migrate a single entity's column visibility from legacy localStorage.
;; Dispatched during app initialization or when an entity admin page loads.
(rf/reg-event-fx
  ::migrate-entity-column-visibility
  (fn [{:keys [db]} [_ entity-name]]
    (let [entity-key (model-naming/ensure-app-keyword entity-name)
          ;; Check if already migrated (has :visible-order in unified prefs)
          already-migrated? (get-in db [:ui :entity-prefs entity-key :columns :visible-order])]
      (if already-migrated?
        {:db db}
        {:migrate-legacy-column-visibility {:entity-name entity-name
                                            :delete-legacy? true}
         ;; Reload prefs to pick up the migration
         :fx [[:dispatch-later {:ms 100 :dispatch [::load-stored-prefs]}]]}))))

;; Migrate all known entity column visibility settings from legacy localStorage.
;; Call this during app initialization.
(rf/reg-event-fx
  ::migrate-all-legacy-column-visibility
  (fn [_cofx [_ entity-names]]
    (let [entities-to-migrate (or entity-names
                                  ;; Default admin entities
                                [:users :admins :audit-logs :login-events])]
      {:fx (mapv (fn [entity-name]
                   [:dispatch [::migrate-entity-column-visibility entity-name]])
             entities-to-migrate)})))
