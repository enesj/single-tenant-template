(ns app.template.frontend.components.list.overrides
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]))

(defn entity-spec-fields
  [entity-spec]
  (cond
    (and (map? entity-spec) (sequential? (:fields entity-spec)))
    (:fields entity-spec)

    (sequential? entity-spec)
    entity-spec

    :else
    []))

(defn resolve-row-field
  [item entity-name fld]
  (let [direct (get item fld)]
    (if (some? direct)
      direct
      (let [by-db (when (keyword? fld)
                    (get item (model-naming/app-keyword->db fld)))]
        (if (some? by-db)
          by-db
          (let [ns-key (when (and entity-name fld)
                         (keyword (name entity-name) (name fld)))
                by-ns (when ns-key (get item ns-key))]
            (if (some? by-ns)
              by-ns
              (some (fn [[k v]]
                      (when (and (keyword? k)
                              (= (name k) (name fld)))
                        v))
                item))))))))

(defn infer-filter-type
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

    :else
    :text))

(defn apply-override-filters
  [rows active-filters]
  (if (empty? active-filters)
    rows
    (vec
      (filter (fn [item]
                (every? (fn [[field-id filter-value]]
                          (filter-helpers/matches-filter? {:item item
                                                           :field-id field-id
                                                           :filter-value filter-value
                                                           :filter-type (infer-filter-type filter-value)}))
                  active-filters))
        rows))))

(defn- field-spec-by-id
  [entity-spec]
  (into {}
    (keep (fn [spec]
            (when-let [field-id (some-> (:id spec) model-naming/ensure-app-keyword)]
              [field-id spec])))
    (entity-spec-fields entity-spec)))

(defn- normalize-sort-value
  [field-spec value]
  (let [date-field? (contains? #{"datetime-local" "date" "time"}
                      (some-> field-spec :input-type name))]
    (cond
      (nil? value) nil
      (string? value)
      (if date-field?
        (let [d (try (js/Date. value) (catch :default _ nil))]
          (if (and d (not (js/isNaN (.getTime d))))
            (.getTime d)
            (str/lower-case value)))
        (str/lower-case value))
      (boolean? value) (if value 1 0)
      (instance? js/Date value) (.getTime value)
      :else value)))

(defn- compare-sort-values
  [left right direction]
  (cond
    (and (nil? left) (nil? right)) 0
    (nil? left) 1
    (nil? right) -1
    :else (let [cmp (compare left right)]
            (if (= direction :desc)
              (- cmp)
              cmp))))

(defn- compare-row-ids
  [left right]
  (compare (str (or (id-utils/extract-entity-id left) ""))
    (str (or (id-utils/extract-entity-id right) ""))))

(defn sort-rows-by-sorts
  [rows {:keys [sorts entity-name entity-spec value-resolver]}]
  (let [sorts* (->> (or sorts [])
                 (keep (fn [{:keys [field direction]}]
                         (let [field* (some-> field model-naming/ensure-app-keyword)
                               direction* (cond
                                            (or (= direction :asc)
                                              (= direction "asc"))
                                            :asc

                                            (or (= direction :desc)
                                              (= direction "desc"))
                                            :desc

                                            :else nil)]
                           (when (and field* direction*)
                             {:field field*
                              :direction direction*}))))
                 vec)
        resolve-value (or value-resolver
                        (fn [item field]
                          (resolve-row-field item entity-name field)))
        specs-by-field (field-spec-by-id entity-spec)]
    (if (seq sorts*)
      (vec
        (sort (fn [left right]
                (or
                  (some (fn [{:keys [field direction]}]
                          (let [field-spec (get specs-by-field field)
                                left-value (normalize-sort-value field-spec (resolve-value left field))
                                right-value (normalize-sort-value field-spec (resolve-value right field))
                                cmp (compare-sort-values left-value right-value direction)]
                            (when (not= 0 cmp)
                              cmp)))
                    sorts*)
                  (compare-row-ids left right)))
          rows))
      (vec rows))))

(defn sort-override-rows
  [rows {:keys [sorts entity-name entity-spec]}]
  (sort-rows-by-sorts rows {:sorts sorts
                            :entity-name entity-name
                            :entity-spec entity-spec}))

(defn apply-rows-override-transforms
  [{:keys [rows active-filters sorts entity-name entity-spec]}]
  (-> rows
    (apply-override-filters active-filters)
    (sort-override-rows {:sorts sorts
                         :entity-name entity-name
                         :entity-spec entity-spec})
    vec))

(defn selected-item?
  [selected-ids item]
  (let [selected-set (set (or selected-ids #{}))
        item-id (id-utils/extract-entity-id item)
        item-id-int (if (string? item-id) (js/parseInt item-id) item-id)
        item-id-str (str item-id)]
    (or (contains? selected-set item-id)
      (contains? selected-set item-id-int)
      (contains? selected-set item-id-str))))

(defn apply-selection-visibility
  [rows selected-ids {:keys [show-selected-rows? show-unselected-rows?]}]
  (let [show-selected? (if (nil? show-selected-rows?) true show-selected-rows?)
        show-unselected? (if (nil? show-unselected-rows?) true show-unselected-rows?)]
    (cond
      (and show-selected? show-unselected?) rows
      (not (or show-selected? show-unselected?)) []
      show-selected? (vec (filter #(selected-item? selected-ids %) rows))
      :else (vec (remove #(selected-item? selected-ids %) rows)))))