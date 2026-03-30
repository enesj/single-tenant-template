(ns app.template.frontend.subs.list
  (:require
    [app.shared.model-naming :as model-naming]
    [app.shared.pagination :as pagination]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

;; Entity-related subscriptions

(rf/reg-sub
  ::entity-list
  (fn [db [_ entity-type]]
    (if (and entity-type (not= entity-type "null"))
      (let [ids (get-in db (paths/entity-ids entity-type) [])
            data (get-in db (paths/entity-data entity-type) {})
            metadata (get-in db (paths/entity-metadata entity-type) {:loading? false :error nil})]
        {:items (map #(get data % {}) ids)
         :loading? (:loading? metadata)
         :error (:error metadata)})
      {:items [] :loading? false :error nil})))

;; UI state subscriptions
(rf/reg-sub
  ::entity-ui-state
  (fn [db [_ entity-type]]
    (get-in db (paths/list-ui-state entity-type))))

(defn- pagination-mode
  [ui-state]
  (let [mode (or (:pagination-mode ui-state)
               (get-in ui-state [:pagination :mode]))]
    (if (or (= mode :server)
          (= mode "server"))
      :server
      :client)))

(defn server-pagination?
  "Returns true when the ui-state map indicates server-side pagination mode."
  [ui-state]
  (= :server (pagination-mode ui-state)))

;; infer-filter-type is provided by filter-helpers/infer-filter-type

(rf/reg-sub
  ::sort-config
  (fn [[_ entity-type]]
    [(rf/subscribe [::entity-ui-state entity-type])])
  (fn [[ui-state] [_ _]]
    (get ui-state :sort)))

(rf/reg-sub
  ::items
  (fn [db [_ entity-type]]
    ;; Directly access the DB and get the entity list data (1-layer function)
    (let [entity-list (if (nil? entity-type)
                        {:items []}
                        (let [ids (get-in db (paths/entity-ids entity-type))
                              data (get-in db (paths/entity-data entity-type))]

                          (if (and ids data)
                            {:items (map #(get data %) ids)}
                            {:items []})))]
      (or (:items entity-list) []))))

(rf/reg-sub
  ::visible-items
  (fn [[_ entity-type]]
    [(rf/subscribe [::filtered-items entity-type])
     (rf/subscribe [::items entity-type])
     (rf/subscribe [::entity-ui-state entity-type])])
  (fn [[filtered-items items ui-state] [_ _]]
    (if (server-pagination? ui-state)
      items
      (let [sort-config (:sort ui-state)
            sort-field (when sort-config (keyword (:field sort-config)))
            sort-dir (:direction sort-config :asc)
            per-page (or (:per-page ui-state)
                       (get-in ui-state [:pagination :per-page])
                       pagination/default-page-size)
            current-page (or (get-in ui-state [:pagination :current-page])
                           (:current-page ui-state)
                           pagination/default-page-number)
            pagination-state (pagination/create-pagination-state
                               {:page-number current-page
                                :page-size per-page
                                :total-items (count filtered-items)})]
        (pagination/paginate-with-sort filtered-items sort-field sort-dir pagination-state)))))

(rf/reg-sub
  ::total-pages
  (fn [[_ entity-type]]
    [(rf/subscribe [::filtered-items entity-type])
     (rf/subscribe [::entity-ui-state entity-type])])
  (fn [[items ui-state] _]
    (let [per-page (or (:per-page ui-state)
                     (get-in ui-state [:pagination :per-page])
                     pagination/default-page-size)
          total-items (if (server-pagination? ui-state)
                        (or (:total-items ui-state)
                          (:total ui-state)
                          (count items))
                        (count items))]
      (pagination/calculate-total-pages total-items per-page))))

;; Theme subscription
(rf/reg-sub
  ::theme
  (fn [db _]
    (get-in db [:ui :theme])))

;; Selected items subscription
(rf/reg-sub
  ::selected-ids
  (fn [db [_ entity-type]]
    (if (nil? entity-type)
      #{}
      (get-in db (paths/entity-selected-ids entity-type) #{}))))

(defn- normalize-filter-key
  [field-id]
  (some-> field-id model-naming/ensure-app-keyword))

(rf/reg-sub
  ::active-filters
  (fn [db [_ entity-type]]
    ;; Normalize legacy/string/snake_case filter keys into canonical app keywords.
    (let [raw-filters (get-in db (paths/list-filters entity-type) {})]
      (reduce-kv (fn [acc field-id filter-value]
                   (if-let [field-key (normalize-filter-key field-id)]
                     (assoc acc field-key filter-value)
                     acc))
        {}
        raw-filters))))

(rf/reg-sub
  ::filtered-items
  (fn [[_ entity-type] _]
    [(rf/subscribe [::items entity-type])
     (rf/subscribe [::entity-ui-state entity-type])
     (rf/subscribe [::active-filters entity-type])])
  (fn [[items ui-state filters] [_ _]]
    (if (or (server-pagination? ui-state)
          (empty? filters))
      items
      (let [filtered (filter (fn [item]
                               (every? (fn [[field-id filter-value]]
                                         (let [field-key (if (keyword? field-id) field-id (keyword field-id))]
                                           (filter-helpers/matches-filter? {:item item
                                                                            :field-id field-key
                                                                            :filter-value filter-value
                                                                            :filter-type (filter-helpers/infer-filter-type filter-value)})))
                                 filters))
                       items)]
        filtered))))

;; Batch Edit Inline Subscription
(rf/reg-sub
  ::batch-edit-inline
  (fn [db [_ entity-type]]
    (get-in db [:ui :batch-edit-inline entity-type] {:open? false})))

;; Current entity type subscription
(rf/reg-sub
  ::current-entity-type
  (fn [db _]
    (get-in db [:ui :current-entity-type])))
