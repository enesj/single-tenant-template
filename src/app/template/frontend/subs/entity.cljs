(ns app.template.frontend.subs.entity
  (:require
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

(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-type]]
    (let [entity-key (if (string? entity-type) (keyword entity-type) entity-type)
          ;; Get UI configuration for this entity
          ui-config (get-in db [:ui :entity-configs entity-key])
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

;; Filter and sort entities
(rf/reg-sub
  ::filtered-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::entities entity-type])
     (rf/subscribe [::list-subs/active-filters entity-type])
     (rf/subscribe [::entity-config entity-type])])
  (fn [[entities active-filters entity-config] [_ _entity-type]]
    (if (empty? active-filters)
      entities
      (let [;; Create a map of field-id to field-spec for quick lookup
            fields-by-id (into {} (map (fn [field] [(keyword (:id field)) field])
                                    (:fields entity-config)))
            filtered (filter (fn [item]
                               (every? (fn [[field-id filter-value]]
                                         (let [field-key (if (keyword? field-id) field-id (keyword field-id))
                                               _field-spec (get fields-by-id field-key)
                                               ;; Determine filter type based on the filter value structure
                                               filter-type (cond
                                                             ;; Vector means select/multi-select filter
                                                             (vector? filter-value) :select
                                                             ;; Map with :min/:max means number range
                                                             (and (map? filter-value)
                                                               (or (contains? filter-value :min)
                                                                 (contains? filter-value :max))) :number-range
                                                             ;; Map with :from/:to means date range
                                                             (and (map? filter-value)
                                                               (or (contains? filter-value :from)
                                                                 (contains? filter-value :to))) :date-range
                                                             ;; String or other means text filter
                                                             :else :text)]
                                           ;; Use the helper function to check if item matches
                                           (filter-helpers/matches-filter? {:item item
                                                                            :field-id field-id
                                                                            :filter-value filter-value
                                                                            :filter-type filter-type})))
                                 active-filters))
                       entities)]
        filtered))))

;; Sort filtered entities
(rf/reg-sub
  ::sorted-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::filtered-entities entity-type])
     (rf/subscribe [::list-subs/sort-config entity-type])
     (rf/subscribe [:entity-specs/by-name (keyword entity-type)])])
  (fn [[entities sort-config entity-specs] [_ entity-type]]
    (let [{:keys [field direction]} sort-config
          ;; Resolve a field value from an item considering possible namespacing
          resolve-field (fn [item fld]
                          (let [direct (get item fld)]
                            (if (some? direct)
                              direct
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
                                    item))))))
          ;; Get field specification from entity specs
          get-field-spec (fn [field-name]
                           (some (fn [spec]
                                   (when (= (:id spec) (name field-name))
                                     spec))
                             entity-specs))
          ;; Determine if field should be treated as a date based on its input type
          date-field? (fn [field-name]
                        (let [field-spec (get-field-spec field-name)]
                          (when field-spec
                            (contains? #{"datetime-local" "date" "time"} (:input-type field-spec)))))
          ;; Normalize value for consistent comparison across types
          normalize (fn [v field-name]
                      (cond
                        (nil? v) nil
                        (string? v)
                        (if (date-field? field-name)
                          ;; Only attempt date parsing for fields with date/time input types
                          (let [d (try (js/Date. v) (catch :default _ nil))]
                            (if (and d (not (js/isNaN (.getTime d))))
                              (.getTime d)
                              (str/lower-case v)))
                          ;; For non-date fields, always treat as strings
                          (str/lower-case v))
                        (boolean? v) (if v 1 0)
                        (instance? js/Date v) (.getTime v)
                        :else v))]
      (if (and field direction)
        (let [sorted (sort-by (fn [item]
                                (let [v (normalize (resolve-field item field) field)
                                       ;; nil first for ascending; will be reversed for descending
                                      nil-key (if (some? v) 1 0)]
                                  [nil-key v]))
                       entities)]
          (if (= direction :desc)
            (reverse sorted)
            sorted))
        entities))))

;; Get entities with pagination applied
(rf/reg-sub
  ::paginated-entities
  (fn [[_ entity-type]]
    [(rf/subscribe [::sorted-entities entity-type])
     (rf/subscribe [::list-subs/entity-ui-state entity-type])])
  (fn [[sorted-entities ui-state] [_ _entity-type]]
    (let [per-page (or (:per-page ui-state)
                     (get-in ui-state [:pagination :per-page])
                     10)
          current-page (or (get-in ui-state [:pagination :current-page]) (:current-page ui-state) 1)
          start-idx (* (dec current-page) per-page)
          _end-idx (+ start-idx per-page)
          result (vec (take per-page (drop start-idx sorted-entities)))]
      result)))

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
