(ns app.template.frontend.interceptors.persistence
  "Persistence interceptor for UI preferences.
   
   Provides localStorage persistence for user preferences stored at
   [:ui :entity-prefs]. Automatically saves preferences after any
   event that modifies them.
   
   Also provides migration utilities for legacy localStorage keys."
  (:require
    [cljs.reader :as reader]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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
      (do
        (log/info "Loaded entity-prefs from localStorage:" (keys stored-entity-prefs))
        {:db (update-in db [:ui :entity-prefs] merge stored-entity-prefs)})
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
          entity-key (keyword entity-name)
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
    (let [entity-key (keyword entity-name)
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
  (fn [{:keys [db]} [_ entity-names]]
    (let [entities-to-migrate (or entity-names
                                  ;; Default admin entities
                                [:users :admins :audit-logs :login-events])]
      {:fx (mapv (fn [entity-name]
                   [:dispatch [::migrate-entity-column-visibility entity-name]])
             entities-to-migrate)})))
