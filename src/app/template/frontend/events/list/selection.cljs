(ns app.template.frontend.events.list.selection
  "Selection management for list views - item selection, fetching, and bulk operations"
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

;;; -------------------------
;;; Selection Management
;;; -------------------------

(rf/reg-event-db
  ::select-item
  common-interceptors
  (fn [db [entity-type id selected?]]
    (let [path (paths/entity-selected-ids entity-type)
          current-selections (get-in db path #{})
          new-db (if selected?
                   ;; Add to selection
                   (assoc-in db path (conj current-selections id))
                   ;; Remove from selection
                   (assoc-in db path (disj current-selections id)))]
      new-db)))

(rf/reg-event-db
  ::select-all
  common-interceptors
  (fn [db [entity-type items selected?]]
    (let [path (paths/entity-selected-ids entity-type)
          ;; Extract ID from items, handling both :id and namespaced keys like :users/id
          extract-id (fn [item]
                       (or (:id item)
                           ;; Generic fallback for namespaced entities
                         (get item (keyword (str (name entity-type) "/id")))))]
      (if selected?
        ;; Select all items
        (assoc-in db path (into #{} (map extract-id items)))
        ;; Clear all selections
        (assoc-in db path #{})))))

(rf/reg-event-fx
  ::delete-selected
  common-interceptors
  (fn [{:keys [db]} [entity-type selected-ids]]
    {:db (assoc-in db (paths/entity-loading? entity-type) true)
     :fx (vec
           (for [id selected-ids]
             [:dispatch [:app.template.frontend.events.list.crud/delete-entity entity-type id]]))}))

;;; -------------------------
;;; Item Fetching
;;; -------------------------

;; Fetch a specific item by ID
(rf/reg-event-fx
  ::fetch-item-by-id
  common-interceptors
  (fn [{:keys [db]} [entity-type item-id]]
    (when (and entity-type item-id)
      ;; IDs should already be normalized upstream, but we'll convert to string for consistent tracking
      (let [fetch-key (str item-id)
            already-fetching? (get-in db [:entity-fetches entity-type fetch-key])]

        (println "FETCH REQUEST - Entity:" entity-type "ID:" item-id "(type:" (type item-id) ")" "Already fetching?" already-fetching?)

        (if already-fetching?
          ;; If already fetching, do nothing
          (do
            (println "Already fetching, skipping duplicate request")
            {})
          ;; Otherwise, start a new fetch
          {:db (-> db
                 (assoc-in (paths/entity-loading? entity-type) true)
                 ;; Mark as fetching to prevent duplicates
                 (assoc-in [:entity-fetches entity-type fetch-key] true))
           :http-xhrio (http/get-entity
                         {:entity-name (name entity-type)
                          :id item-id
                          :on-success [::fetch-item-success entity-type item-id]
                          :on-failure [:app.frontend.events.list.crud/fetch-failure entity-type]})})))))

(rf/reg-event-fx
  ::fetch-item-success
  common-interceptors
  (fn [{:keys [db]} [entity-type item-id response]]
    (println "Fetched item successfully:" entity-type item-id "(type:" (type item-id) ")")
    ;; IDs should already be normalized upstream, but we'll convert to string for consistent tracking
    (let [fetch-key (str item-id)]

      (println "SUCCESS: Clearing fetch flag for:" entity-type fetch-key)

      {:db (-> db
             ;; Clear the fetch-in-progress flag
             (update-in [:entity-fetches entity-type] dissoc fetch-key)
             ;; Add the item to the entity data
             (assoc-in (conj (paths/entity-data entity-type) item-id) response)
             ;; Ensure the item ID is in the list of IDs
             (update-in (paths/entity-ids entity-type) #(if (some #{item-id} %)
                                                          %
                                                          (conj (or % []) item-id)))
             ;; Update metadata
             (assoc-in (paths/entity-metadata entity-type)
               {:loading? false
                :error nil
                :last-updated (js/Date.now)})
             ;; Set the editing ID in UI state
             (assoc-in [:ui :editing] item-id))
       ;; Initialize form data with the item
       :dispatch [::initialize-form-with-item entity-type response]})))

;; Initialize form data with item
(rf/reg-event-db
  ::initialize-form-with-item
  common-interceptors
  (fn [db [entity-type item]]
    (assoc-in db [:forms (keyword entity-type) :values] item)))
