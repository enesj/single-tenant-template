(ns app.template.frontend.list-test-utils
  "Helper functions and re-frame events for list operation tests"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.state.normalize :as normalize]
    [re-frame.core :as rf]))

(defn get-items-from-db
  "Helper function to get items directly from app-db without using subscriptions"
  [db entity-type]
  (let [entity-data (get-in db (paths/entity-data entity-type) {})
        entity-ids (get-in db (paths/entity-ids entity-type) [])]
    (map entity-data entity-ids)))

(defn get-visible-items-from-db
  "Helper function to simulate visible-items subscription logic"
  [db entity-type]
  (let [items (get-items-from-db db entity-type)
        ui-state (get-in db (paths/list-ui-state entity-type) {})
        sort-config (:sort ui-state)
        current-page (:current-page ui-state 1)
        per-page (:per-page ui-state 10)]
    (cond->> items
      sort-config (sort-by (:field sort-config))
      (= :desc (:direction sort-config)) reverse
      true (drop (* (dec current-page) per-page))
      true (take per-page))))

(defn get-total-pages-from-db
  "Helper function to calculate total pages directly from app-db"
  [db entity-type]
  (let [items (get-items-from-db db entity-type)
        ui-state (get-in db (paths/list-ui-state entity-type) {})
        per-page (:per-page ui-state 10)
        total-items (count items)]
    (max 1 (js/Math.ceil (/ total-items per-page)))))

(defn get-selected-ids-from-db
  "Helper function to get selected IDs directly from app-db"
  [db entity-type]
  (get-in db (paths/entity-selected-ids entity-type) #{}))

(defonce list-ops-test-events-registered
  (do
    ;; Initialize test database
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))

    ;; Mock HTTP success for fetch operations
    (rf/reg-event-db
      ::test-fetch-success
      (fn [db [_ entity-type response]]
        (let [normalized (normalize/normalize-entities response)]
          (-> db
            (assoc-in (paths/entity-data entity-type) (:data normalized))
            (assoc-in (paths/entity-ids entity-type) (:ids normalized))
            (assoc-in (paths/entity-metadata entity-type)
              {:loading? false
               :error nil
               :last-updated (js/Date.now)})
            (assoc-in (paths/list-total-items entity-type) (count response))))))

    ;; Mock HTTP failure for fetch operations
    (rf/reg-event-db
      ::test-fetch-failure
      (fn [db [_ entity-type error-msg]]
        (assoc-in db (paths/entity-metadata entity-type)
                  {:loading? false
                   :error error-msg
                   :last-updated nil})))

    ;; Set entity loading state
    (rf/reg-event-db
      ::test-set-loading
      (fn [db [_ entity-type loading?]]
        (assoc-in db (paths/entity-loading? entity-type) loading?)))

    ;; Set list UI state
    (rf/reg-event-db
      ::test-set-list-ui-state
      (fn [db [_ entity-type ui-state]]
        (update-in db (paths/list-ui-state entity-type) merge ui-state)))

    ;; Set selected IDs
    (rf/reg-event-db
      ::test-set-selected-ids
      (fn [db [_ entity-type selected-ids]]
        (assoc-in db (paths/entity-selected-ids entity-type) (set selected-ids))))

    ;; Add entity to data
    (rf/reg-event-db
      ::test-add-entity
      (fn [db [_ entity-type entity]]
        (let [id (:id entity)
              current-data (get-in db (paths/entity-data entity-type) {})
              current-ids (get-in db (paths/entity-ids entity-type) [])]
          (-> db
            (assoc-in (paths/entity-data entity-type) (assoc current-data id entity))
            (assoc-in (paths/entity-ids entity-type) (conj current-ids id))))))

    ;; Remove entity from data
    (rf/reg-event-db
      ::test-remove-entity
      (fn [db [_ entity-type entity-id]]
        (let [current-data (get-in db (paths/entity-data entity-type) {})
              current-ids (get-in db (paths/entity-ids entity-type) [])]
          (-> db
            (assoc-in (paths/entity-data entity-type) (dissoc current-data entity-id))
            (assoc-in (paths/entity-ids entity-type) (vec (remove #(= % entity-id) current-ids)))))))

    true))
