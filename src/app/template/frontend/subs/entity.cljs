(ns app.template.frontend.subs.entity
  (:require
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.shared.pagination :as pagination]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.components.list.overrides :as overrides]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.subs.list :as list-subs]
    [clojure.string :as str]
    [re-frame.core :as rf]))

;; Basic entity data subscriptions
(rf/reg-sub
  ::entity-ids
  (fn [db [_ entity-type]]
    (get-in db (paths/entity-ids entity-type))))

(rf/reg-sub
  ::entity-data
  (fn [db [_ entity-type]]
    (get-in db (paths/entity-data entity-type))))

;; Snapshot of all loaded entities in app-db.
;; Used for sorting/select-label resolution where the current entity's field is a FK.
(rf/reg-sub
  ::entities-state
  (fn [db _]
    (:entities db)))

(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-type]]
    (let [entity-key (if (string? entity-type) (keyword entity-type) entity-type)
          ;; Get UI configuration for this entity
          ui-config (get-in db (paths/entity-display-settings entity-key))
          ;; Get field specifications for this entity
          entity-specs @(rf/subscribe [:entity-specs])
          ;; Get fields and add filterable flag to each
          fields (map #(assoc % :filterable true) (get entity-specs entity-key))]
      ;; Merge UI config with field specs
      (assoc ui-config :fields fields))))

(rf/reg-sub
  ::entities
  (fn [[_ entity-type]]
    (if (nil? entity-type)
      []
      [(rf/subscribe [::entity-ids entity-type])
       (rf/subscribe [::entity-data entity-type])]))
  (fn [[ids data] [_ _]]
    (if (and ids data)
      (let [result (mapv #(get data %) ids)]
        result)
      [])))

;; server-pagination? is provided by list-subs/server-pagination?

;; Filter and sort entities
(rf/reg-sub
  ::filtered-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::entities entity-type])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])
     (rf/subscribe [::list-subs/active-filters entity-type])])
  (fn [[entities ui-state active-filters] [_ _entity-type]]
    (if (or (list-subs/server-pagination? ui-state)
          (empty? active-filters))
      entities
      (let [filtered (filter (fn [item]
                               (every? (fn [[field-id filter-value]]
                                         (let [field-key (if (keyword? field-id) field-id (keyword field-id))]
                                           (filter-helpers/matches-filter? {:item item
                                                                            :field-id field-key
                                                                            :filter-value filter-value
                                                                            :filter-type (filter-helpers/infer-filter-type filter-value)})))
                                 active-filters))
                       entities)]
        filtered))))

;; Sort filtered entities
(rf/reg-sub
  ::sorted-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::filtered-entities entity-type])
     (rf/subscribe [::list-subs/sorts entity-type])
     (rf/subscribe [:entity-specs/by-name (keyword entity-type)])
     (rf/subscribe [::entities-state])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])])
  (fn [[entities sorts entity-specs entities-state ui-state] [_ entity-type]]
    (if (list-subs/server-pagination? ui-state)
      entities
      (let [resolve-field (fn [item fld]
                            (let [direct (get item fld)]
                              (if (some? direct)
                                direct
                                (let [by-db (when (keyword? fld)
                                              (get item (model-naming/app-keyword->db fld)))]
                                  (if (some? by-db)
                                    by-db
                                    (let [ns-key (when (and entity-type fld)
                                                   (keyword (name entity-type) (name fld)))
                                          by-ns (when ns-key (get item ns-key))]
                                      (if (some? by-ns)
                                        by-ns
                                        (some (fn [[k v]]
                                                (when (and (keyword? k)
                                                        (= (name k) (name fld)))
                                                  v))
                                          item))))))))
            specs-by-field (into {}
                             (keep (fn [spec]
                                     (when-let [field-id (some-> (:id spec) model-naming/ensure-app-keyword)]
                                       [field-id spec])))
                             entity-specs)
            select-sort-info-by-field (into {}
                                        (keep (fn [[field field-spec]]
                                                (when (and field-spec
                                                        (= "select" (some-> (:type field-spec) kw/ensure-name str/lower-case))
                                                        (vector? (:options field-spec))
                                                        (= 2 (count (:options field-spec))))
                                                  (let [[ref-entity label-field] (:options field-spec)
                                                        ref-entity (some-> ref-entity kw/ensure-keyword model-naming/ensure-app-keyword)
                                                        label-field (some-> label-field kw/ensure-keyword model-naming/ensure-app-keyword)]
                                                    (when (and ref-entity label-field)
                                                      [field {:ref-entity ref-entity
                                                              :label-field label-field}]))))
                                          specs-by-field))
            resolve-select-label (fn [{:keys [ref-entity label-field]} raw-id]
                                   (when (some? raw-id)
                                     (let [data (get-in entities-state [ref-entity :data])
                                           ref-item (or (get data raw-id)
                                                      (when (and (not (string? raw-id))
                                                              (some? raw-id))
                                                        (get data (str raw-id))))]
                                       (when (map? ref-item)
                                         (or (get ref-item label-field)
                                           (get ref-item (model-naming/app-keyword->db label-field)))))))
            value-resolver (fn [item field]
                             (let [field* (some-> field model-naming/ensure-app-keyword)
                                   raw (resolve-field item field*)]
                               (if-let [select-sort-info (get select-sort-info-by-field field*)]
                                 (or (resolve-select-label select-sort-info raw) raw)
                                 raw)))]
        (overrides/sort-rows-by-sorts entities {:sorts sorts
                                                :entity-name entity-type
                                                :entity-spec entity-specs
                                                :value-resolver value-resolver})))))

;; Get entities with pagination applied
(rf/reg-sub
  ::paginated-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::sorted-entities entity-type])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])])
  (fn [[sorted-entities ui-state] [_ _entity-type]]
    (if (list-subs/server-pagination? ui-state)
      (vec sorted-entities)
      (let [per-page     (or (:per-page ui-state)
                           (get-in ui-state [:pagination :per-page])
                           pagination/default-page-size)
            current-page (or (get-in ui-state [:pagination :current-page])
                           (:current-page ui-state)
                           pagination/default-page-number)
            start-idx    (* (dec current-page) per-page)]
        (vec (take per-page (drop start-idx sorted-entities)))))))

;; Get loading and error status
(rf/reg-sub
  ::loading?
  (fn [db [_ entity-type]]
    (get-in db (paths/entity-loading? entity-type))))

(rf/reg-sub
  ::error
  (fn [db [_ entity-type]]
    (get-in db (paths/entity-error entity-type))))

;; Current page subscription
(rf/reg-sub
  ::current-page
  (fn [db [_ entity-type]]
    (or (get-in db (paths/list-current-page entity-type))
      pagination/default-page-number)))
