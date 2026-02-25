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

(defn sort-override-rows
  [rows {:keys [sort-config entity-name entity-spec]}]
  (let [{:keys [field direction]} sort-config
        field (some-> field model-naming/ensure-app-keyword)
        field-spec (some (fn [spec]
                           (when (= (some-> (:id spec) model-naming/ensure-app-keyword name)
                                   (some-> field name))
                             spec))
                     (entity-spec-fields entity-spec))
        date-field? (contains? #{"datetime-local" "date" "time"}
                      (some-> field-spec :input-type name))
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
                              (let [v (normalize (resolve-row-field item entity-name field))
                                    nil-key (if (some? v) 1 0)]
                                [nil-key v]))
                     rows)]
        (if (= direction :desc)
          (reverse sorted)
          sorted))
      rows)))

(defn apply-rows-override-transforms
  [{:keys [rows active-filters sort-config entity-name entity-spec]}]
  (-> rows
    (apply-override-filters active-filters)
    (sort-override-rows {:sort-config sort-config
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