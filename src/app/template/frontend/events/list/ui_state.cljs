(ns app.template.frontend.events.list.ui-state
  "UI state management for list views - pagination, sorting, and toggles"
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.interceptors.persistence :as persistence]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- ->entity-key
  "Normalize incoming entity identifiers to keywords."
  [entity-type]
  (cond
    (keyword? entity-type) entity-type
    (string? entity-type) (keyword entity-type)
    (map? entity-type) (recur (:value entity-type))
    (nil? entity-type) nil
    :else (keyword (str entity-type))))

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

;;; -------------------------
;;; Pagination
;;; -------------------------

(rf/reg-event-db
  ::set-current-page
  common-interceptors
  (fn [db [entity-type page]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [safe-page (max 1 (or page 1))
            per-page (current-per-page db entity-key)]
        (-> db
          (sync-current-page entity-key safe-page)
          (sync-per-page entity-key per-page)))
      db)))

(rf/reg-event-db
  ::set-per-page
  common-interceptors
  (fn [db [entity-type per-page]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [parsed (cond
                     (number? per-page) per-page
                     (string? per-page) (js/parseInt per-page 10)
                     :else per-page)
            clamped (if (and parsed (pos? parsed)) parsed 10)]
        (-> db
          (sync-per-page entity-key clamped)
          (sync-current-page entity-key 1)
          ((fn [db*] (log/info "LIST SET-PER-PAGE →" (name entity-key) "to" clamped) db*))))
      db)))

;;; -------------------------
;;; Sorting
;;; -------------------------

(rf/reg-event-db
  ::set-sort-field
  common-interceptors
  (fn [db [entity-type field]]
    (if-let [entity-key (->entity-key entity-type)]
      (let [sort-config (get-in db (paths/list-sort-config entity-key))
            current-direction (:direction sort-config)
            current-field (:field sort-config)
            new-direction (if (and (= field current-field)
                                (= current-direction :asc))
                            :desc
                            :asc)]
        (-> db
          (assoc-in (conj (paths/list-sort-config entity-key) :field) field)
          (assoc-in (conj (paths/list-sort-config entity-key) :direction) new-direction)
          (sync-current-page entity-key 1)))
      db)))

;;; -------------------------
;;; Toggle States
;;; -------------------------

(defn- toggle-entity-flag
  "Toggle an entity-specific display flag.
   Reads from new path first, falls back to legacy, writes to new path only."
  [db entity-key path default-value]
  (let [;; New path: [:ui :entity-prefs <entity> :display <setting>]
        new-path (into [:ui :entity-prefs entity-key :display] path)
        ;; Legacy path: [:ui :entity-configs <entity> <setting>]
        legacy-path (into [:ui :entity-configs entity-key] path)
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
                                          :new-value (get-in result [:ui :entity-configs entity-key :show-select?])})
        result)
      (update-in db [:ui :show-select?] not))))

(rf/reg-event-db
  ::toggle-timestamps
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (if-let [entity-key (->entity-key entity-type)]
      (toggle-entity-flag db entity-key [:show-timestamps?] false)
      (update-in db [:ui :show-timestamps?] not))))

(rf/reg-event-db
  ::toggle-edit
  [common-interceptors persistence/persist-entity-prefs]
  (fn [db [entity-type]]
    (log/info "toggle-edit event fired" {:entity-type entity-type})
    (if-let [entity-key (->entity-key entity-type)]
      (let [result (toggle-entity-flag db entity-key [:show-edit?] true)]
        (log/info "toggle-edit result" {:entity-key entity-key
                                        :new-value (get-in result [:ui :entity-configs entity-key :show-edit?])})
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
