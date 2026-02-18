(ns app.template.frontend.subs.list
  (:require
    [app.shared.pagination :as pagination]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

;; Entity-related subscriptions

(rf/reg-sub
  ::entity-ids
  (fn [db [_ entity-type]]
    (get-in db (paths/entity-ids entity-type))))

(rf/reg-sub
  ::entity-list
  (fn [db [_ entity-type]]

    (try
      (if (and entity-type (not= entity-type nil) (not= entity-type "null"))
        (let [ids (get-in db (paths/entity-ids entity-type) [])
              data (get-in db (paths/entity-data entity-type) {})
              metadata (get-in db (paths/entity-metadata entity-type) {:loading? false :error nil})
              result {:items (map #(get data % {}) ids)
                      :loading? (:loading? metadata)
                      :error (:error metadata)}]

          result)
        (let [result {:items [] :loading? false :error nil}]

          result))
      (catch :default e
        (js/console.error "Error in entity-list:" e)
        {:items [] :loading? false :error nil}))))

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

(defn- server-pagination?
  [ui-state]
  (= :server (pagination-mode ui-state)))

(defn- infer-filter-type
  [filter-value]
  (cond
    (vector? filter-value)
    :select

    (and (map? filter-value)
      (or (contains? filter-value :min)
        (contains? filter-value :max)))
    :number-range

    (and (map? filter-value)
      (or (contains? filter-value :from)
        (contains? filter-value :to)))
    :date-range

    (and (map? filter-value)
      (contains? filter-value :value))
    :select

    :else
    :text))

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
     (rf/subscribe [::entity-ui-state entity-type])])
  (fn [[filtered-items ui-state] [_ _]]
    (if (server-pagination? ui-state)
      filtered-items
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
    (try
      (if (nil? entity-type)
        #{}
        (get-in db (paths/entity-selected-ids entity-type) #{}))
      (catch :default e
        (js/console.error "Error in ::selected-ids subscription for entity-type:" entity-type "error:" e)
        #{}))))

(rf/reg-sub
  ::active-filters
  (fn [db [_ entity-type]]
    ;; New structure: {:filters {field-id filter-value, field-id2 filter-value2, ...}}
    (get-in db (paths/list-filters entity-type) {})))

(rf/reg-sub
  ::filtered-items
  (fn [[_ entity-type] _]
    [(rf/subscribe [::items entity-type])
     (rf/subscribe [::active-filters entity-type])
     (rf/subscribe [::entity-ui-state entity-type])])
  (fn [[items filters ui-state] [_ _]]
    (if (empty? filters)
      items
      (let [filtered (filter (fn [item]
                               (every? (fn [[field-id filter-value]]
                                         (let [field-key (if (keyword? field-id) field-id (keyword field-id))]
                                           (filter-helpers/matches-filter? {:item item
                                                                            :field-id field-key
                                                                            :filter-value filter-value
                                                                            :filter-type (infer-filter-type filter-value)})))
                                 filters))
                       items)]
        filtered))))

(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-type]]
    (get-in db [:entities :config entity-type])))

;; Batch Edit Inline Subscription
(rf/reg-sub
  ::batch-edit-inline
  (fn [db [_ entity-type]]
    (get-in db [:ui :batch-edit-inline entity-type] {:open? false})))

;; Current entity type subscription
(rf/reg-sub
  ::current-entity-type
  (fn [db _]
    ;; Get current entity type from UI state - could be in either location
    (or (get-in db [:ui :current-entity-type])
      (get-in db [:ui :entity-name]))))
