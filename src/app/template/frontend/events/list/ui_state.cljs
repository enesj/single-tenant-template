(ns app.template.frontend.events.list.ui-state
  "UI state management for list views - pagination, sorting, and toggles"
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.interceptors.persistence :as persistence]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- ->entity-key
  "Normalize incoming entity identifiers to keywords."
  [entity-type]
  (cond
    (map? entity-type) (recur (:value entity-type))
    :else (model-naming/ensure-app-keyword entity-type)))

(defn- current-per-page
  [db entity-key]
  (or (get-in db (paths/list-per-page entity-key))
    (get-in db (conj (paths/list-ui-state entity-key) :per-page))
    (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
    10))

(defn- sync-per-page
  [db entity-key per-page]
  (-> db
    (assoc-in (paths/list-per-page entity-key) per-page)
    (assoc-in (conj (paths/list-ui-state entity-key) :per-page) per-page)
    (assoc-in (conj (paths/list-ui-state entity-key) :pagination :per-page) per-page)))

(defn- sync-current-page
  [db entity-key page]
  (-> db
    (assoc-in (paths/list-current-page entity-key) page)
    (assoc-in (conj (paths/list-ui-state entity-key) :current-page) page)
    (assoc-in (conj (paths/list-ui-state entity-key) :pagination :current-page) page)))

(defn- normalize-pagination-mode
  [mode]
  (if (or (= mode :server)
        (= mode "server"))
    :server
    :client))

(defn list-refresh-dispatch
  "Returns the configured refresh event vector for an entity, if any.

  Accepts either:
  - a vector event form (returned as-is)
  - a keyword event id (wrapped to a single-element vector)

  Returns nil when no valid refresh event is configured."
  [db entity-type]
  (when-let [entity-key (->entity-key entity-type)]
    (let [configured (get-in db (paths/list-refresh-event entity-key))]
      (cond
        (vector? configured) configured
        (keyword? configured) [configured]
        :else nil))))

(defn- current-pagination-mode
  [db entity-key]
  (normalize-pagination-mode
    (or (get-in db (paths/list-pagination-mode entity-key))
      (get-in db (conj (paths/list-ui-state entity-key) :pagination :mode)))))

(defn- refresh-dispatch-for-server-mode
  [db entity-key]
  (when (= :server (current-pagination-mode db entity-key))
    (list-refresh-dispatch db entity-key)))

;;; -------------------------
;;; Pagination
;;; -------------------------

(rf/reg-event-fx
  ::set-current-page
  common-interceptors
  (fn [{:keys [db]} [entity-type page]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [safe-page (max 1 (or page 1))
            per-page (current-per-page db entity-key)
            db* (-> db
                  (sync-current-page entity-key safe-page)
                  (sync-per-page entity-key per-page))
            refresh-dispatch (refresh-dispatch-for-server-mode db* entity-key)]
        (cond-> {:db db*}
          refresh-dispatch (assoc :dispatch refresh-dispatch)))
      {:db db})))

(rf/reg-event-fx
  ::set-per-page
  [common-interceptors persistence/persist-entity-prefs]
  (fn [{:keys [db]} [entity-type per-page]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [parsed (cond
                     (number? per-page) per-page
                     (string? per-page) (js/parseInt per-page 10)
                     :else per-page)
            clamped (if (and parsed (pos? parsed)) parsed 10)
            db* (-> db
                  (sync-per-page entity-key clamped)
                  (sync-current-page entity-key 1)
                  ;; Persist as a display preference so it survives refresh.
                  ;; This also makes it available to the unified resolver via [:ui :entity-prefs].
                  (assoc-in (conj (paths/entity-prefs-display entity-key) :per-page) clamped)
                  ((fn [db**] (log/info "LIST SET-PER-PAGE →" (name entity-key) "to" clamped) db**)))
            refresh-dispatch (refresh-dispatch-for-server-mode db* entity-key)]
        (cond-> {:db db*}
          refresh-dispatch (assoc :dispatch refresh-dispatch)))
      {:db db})))

(rf/reg-event-db
  ::set-pagination-mode
  common-interceptors
  (fn [db [entity-type mode]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [normalized-mode (normalize-pagination-mode mode)]
        (-> db
          (assoc-in (paths/list-pagination-mode entity-key) normalized-mode)
          (assoc-in (conj (paths/list-ui-state entity-key) :pagination :mode) normalized-mode)))
      db)))

(rf/reg-event-db
  ::set-refresh-event
  common-interceptors
  (fn [db [entity-type refresh-event]]
    (if-let [entity-key (->entity-key entity-type)]
      (cond
        (or (nil? refresh-event)
          (vector? refresh-event)
          (keyword? refresh-event))
        (assoc-in db (paths/list-refresh-event entity-key) refresh-event)

        :else db)
      db)))

;;; -------------------------
;;; Sorting
;;; -------------------------

(rf/reg-event-fx
  ::set-sort-field
  common-interceptors
  (fn [{:keys [db]} [entity-type field]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [sort-config (get-in db (paths/list-sort-config entity-key))
            current-direction (:direction sort-config)
            current-field (:field sort-config)
            new-direction (if (and (= field current-field)
                                (= current-direction :asc))
                            :desc
                            :asc)
            db* (-> db
                  (assoc-in (conj (paths/list-sort-config entity-key) :field) field)
                  (assoc-in (conj (paths/list-sort-config entity-key) :direction) new-direction)
                  (sync-current-page entity-key 1))
            refresh-dispatch (refresh-dispatch-for-server-mode db* entity-key)]
        (cond-> {:db db*}
          refresh-dispatch (assoc :dispatch refresh-dispatch)))
      {:db db})))

;;; -------------------------
;;; Toggle States
;;; -------------------------

(defn- toggle-entity-flag
  "Toggle an entity-specific display flag.
   Reads from new path first, falls back to legacy, writes to new path only."
  [db entity-key path default-value]
  (let [;; New path: [:ui :entity-prefs <entity> :display <setting>]
        new-path (into (paths/entity-prefs-display entity-key) path)
        ;; Legacy path: [:ui :entity-configs <entity> <setting>]
        legacy-path (into (paths/entity-display-settings entity-key) path)
        ;; Read from new path first
        new-value (get-in db new-path)
        legacy-value (get-in db legacy-path)
        ;; Effective current value (new > legacy > defaults > global > default)
        effective (cond
                    (some? new-value) new-value
                    (some? legacy-value) legacy-value
                    :else (or (get-in db (into [:ui :defaults] path))
                            (get-in db (into [:ui] path))
                            default-value))]
    ;; Write toggled value to new path only
    (assoc-in db new-path (not effective))))

(rf/reg-event-db
  ::toggle-highlights
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-highlights?] true)
      (update-in db [:ui :show-highlights?] not))))

(rf/reg-event-db
  ::toggle-select
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (log/info "toggle-select event fired" {:entity-type entity-type})
    (if-let [entity-key (->entity-key entity-type)]
      (let [result (toggle-entity-flag db entity-key [:show-select?] false)]
        (log/info "toggle-select result" {:entity-key entity-key
                                          :new-value (get-in result (conj (paths/entity-display-settings entity-key) :show-select?))})
        result)
      (update-in db [:ui :show-select?] not))))

(rf/reg-event-db
  ::toggle-edit
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (log/info "toggle-edit event fired" {:entity-type entity-type})
    (if-let [entity-key (->entity-key entity-type)]
      (let [result (toggle-entity-flag db entity-key [:show-edit?] true)]
        (log/info "toggle-edit result" {:entity-key entity-key
                                        :new-value (get-in result (conj (paths/entity-display-settings entity-key) :show-edit?))})
        result)
      (update-in db [:ui :show-edit?] not))))

(rf/reg-event-db
  ::toggle-delete
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-delete?] true)
      (update-in db [:ui :show-delete?] not))))

(rf/reg-event-db
  ::toggle-pagination
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-pagination?] true)
      (update-in db [:ui :show-pagination?] not))))

(rf/reg-event-db
  ::toggle-filtering
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-filtering?] true)
      (update-in db [:ui :show-filtering?] not))))

(rf/reg-event-db
  ::toggle-add-button
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-add-button?] true)
      (update-in db [:ui :show-add-button?] not))))

(rf/reg-event-db
  ::toggle-batch-edit
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-batch-edit?] false)
      (update-in db [:ui :show-batch-edit?] not))))

(rf/reg-event-db
  ::toggle-batch-delete
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-batch-delete?] false)
      (update-in db [:ui :show-batch-delete?] not))))

(rf/reg-event-db
  ::toggle-selected-rows
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-selected-rows?] true)
      (update-in db [:ui :show-selected-rows?] not))))

(rf/reg-event-db
  ::toggle-unselected-rows
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-unselected-rows?] true)
      (update-in db [:ui :show-unselected-rows?] not))))

(def ^:private display-setting-keys
  [:show-timestamps? :show-edit? :show-delete? :show-highlights?
   :show-select? :show-filtering? :show-pagination? :show-add-button?
   :show-batch-edit? :show-batch-delete? :show-selected-rows?
   :show-unselected-rows? :per-page])

;; Remove the current browser's entity-level display preferences.
;;
;; This is useful when testing changes to view-options defaults/locks: local
;; overrides stored in localStorage can otherwise make defaults appear ignored.
;;
;; Clears:
;; - New prefs path: [:ui :entity-prefs <entity> :display]
;; - Legacy path keys: [:ui :entity-configs <entity> :show-*?]
(rf/reg-event-db
  ::clear-display-prefs
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [legacy-path (paths/entity-display-settings entity-key)
            legacy (get-in db legacy-path)
            db* (update-in db [:ui :entity-prefs]
                  (fn [prefs]
                    (let [prefs (or prefs {})
                          current (get prefs entity-key)]
                      (if (map? current)
                        (let [updated (dissoc current :display)]
                          (if (seq updated)
                            (assoc prefs entity-key updated)
                            (dissoc prefs entity-key)))
                        prefs))))]
        (if (map? legacy)
          (let [cleaned (apply dissoc legacy display-setting-keys)]
            (if (empty? cleaned)
              (update-in db* [:ui :entity-configs] dissoc entity-key)
              (assoc-in db* legacy-path cleaned)))
          db*))
      db)))
