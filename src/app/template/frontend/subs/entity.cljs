(ns app.template.frontend.subs.entity
  (:require
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
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
    (if (or (nil? entity-type) (= entity-type ""))
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
     (rf/subscribe [::list-subs/active-filters entity-type])])
  (fn [[entities active-filters] [_ _entity-type]]
    (if (empty? active-filters)
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
     (rf/subscribe [::list-subs/sort-config entity-type])
     (rf/subscribe [:entity-specs/by-name (keyword entity-type)])
     (rf/subscribe [::entities-state])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])])
  (fn [[entities sort-config entity-specs entities-state ui-state] [_ entity-type]]
    (if (list-subs/server-pagination? ui-state)
      entities
      (let [{:keys [field direction]} sort-config
            field (when field (model-naming/ensure-app-keyword field))
            ;; Resolve a field value from an item considering possible namespacing
            resolve-field (fn [item fld]
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
                                        ;; Fallback: find any key whose local name matches the field
                                        (some (fn [[k v]]
                                                (when (and (keyword? k)
                                                        (= (name k) (name fld)))
                                                  v))
                                          item))))))))
            ;; Get field specification from entity specs
            get-field-spec (fn [field-name]
                             (some (fn [spec]
                                     (when (= (:id spec) (name field-name))
                                       spec))
                               entity-specs))
            field-spec (when field (get-field-spec field))
            date-field? (when field-spec
                          (contains? #{"datetime-local" "date" "time"} (:input-type field-spec)))
            select-sort-info (when (and field-spec
                                     (= "select" (some-> (:type field-spec) kw/ensure-name str/lower-case))
                                     (vector? (:options field-spec))
                                     (= 2 (count (:options field-spec))))
                               (let [[ref-entity label-field] (:options field-spec)
                                     ref-entity (some-> ref-entity kw/ensure-keyword model-naming/ensure-app-keyword)
                                     label-field (some-> label-field kw/ensure-keyword model-naming/ensure-app-keyword)]
                                 (when (and ref-entity label-field)
                                   {:ref-entity ref-entity
                                    :label-field label-field})))
            resolve-select-label (fn [raw-id]
                                   (when (and select-sort-info (some? raw-id))
                                     (let [{:keys [ref-entity label-field]} select-sort-info
                                           data (get-in entities-state [ref-entity :data])
                                           ref-item (or (get data raw-id)
                                                      (when (and (not (string? raw-id))
                                                              (some? raw-id))
                                                        (get data (str raw-id))))]
                                       (when (map? ref-item)
                                         (or (get ref-item label-field)
                                           (get ref-item (model-naming/app-keyword->db label-field)))))))
            resolve-sort-value (fn [item]
                                 (let [raw (resolve-field item field)]
                                   (if select-sort-info
                                     (or (resolve-select-label raw) raw)
                                     raw)))
            normalize (fn [v]
                        (cond
                          (nil? v) nil
                          (string? v)
                          (if date-field?
                            (let [d (try (js/Date. v) (catch :default _ nil))]
                              (if (and d (not (js/isNaN (.getTime d))))
                                (.getTime d)
                                (str/lower-case v)))
                            (str/lower-case v))
                          (boolean? v) (if v 1 0)
                          (instance? js/Date v) (.getTime v)
                          :else v))]
        (if (and field direction)
          (let [sorted (sort-by (fn [item]
                                  (let [v (normalize (resolve-sort-value item))
                                        nil-key (if (some? v) 1 0)]
                                    [nil-key v]))
                         entities)]
            (if (= direction :desc)
              (reverse sorted)
              sorted))
          entities)))))

;; Get entities with pagination applied
(rf/reg-sub
  ::paginated-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::sorted-entities entity-type])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])])
  (fn [[sorted-entities ui-state] [_ _entity-type]]
    (if (list-subs/server-pagination? ui-state)
      (vec sorted-entities)
      (let [per-page      (or (:per-page ui-state)
                            (get-in ui-state [:pagination :per-page])
                            10)
            current-page  (or (get-in ui-state [:pagination :current-page])
                            (:current-page ui-state)
                            1)
            start-idx     (* (dec current-page) per-page)]
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
      1)))
