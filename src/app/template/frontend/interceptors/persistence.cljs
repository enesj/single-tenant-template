(ns app.template.frontend.interceptors.persistence
  "Persistence interceptor for UI preferences.
   
   Provides localStorage persistence for user preferences stored at
   [:ui :entity-prefs]. Automatically saves preferences after any
   event that modifies them."
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
