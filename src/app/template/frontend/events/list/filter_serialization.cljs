(ns app.template.frontend.events.list.filter-serialization
  "Helpers for normalizing shared list UI filter state into backend query params."
  (:require
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]))

(defn date->iso-str
  "Convert a js/Date or scalar value to a query-string-safe representation."
  [value]
  (cond
    (instance? js/Date value) (.toISOString value)
    (string? value) value
    :else (str value)))

(defn normalize-filter-value
  "Normalize a UI filter value.

  - Preserve date/number range maps.
  - Unwrap select-shaped maps via `:value`.
  - Trim strings and drop blank values.
  - Collapse one-item selections to a scalar value."
  [value]
  (cond
    (and (map? value)
      (or (contains? value :from)
        (contains? value :to)
        (contains? value "from")
        (contains? value "to")
        (contains? value :min)
        (contains? value :max)
        (contains? value "min")
        (contains? value "max")))
    value

    (map? value)
    (or (some-> (get value :value) normalize-filter-value)
      (some-> (get value "value") normalize-filter-value))

    (keyword? value) (name value)
    (string? value) (some-> value str/trim not-empty)

    (vector? value)
    (let [items (->> value
                  (map normalize-filter-value)
                  (remove nil?)
                  vec)]
      (when (seq items)
        (if (= 1 (count items))
          (first items)
          items)))

    (sequential? value)
    (let [items (->> value
                  (map normalize-filter-value)
                  (remove nil?)
                  vec)]
      (when (seq items)
        (if (= 1 (count items))
          (first items)
          items)))

    :else value))

(defn flatten-ui-filters
  "Flatten normalized UI filters into flat backend query params.

  Date ranges become `<field>-from` / `<field>-to`.
  Number ranges become `<field>-min` / `<field>-max`.
  Select values are normalized to their scalar `:value` form."
  [filters]
  (reduce-kv
    (fn [acc k v]
      (let [normalized (normalize-filter-value v)
            field-key (keyword (name k))
            field-name (name field-key)
            range-from (when (map? normalized)
                         (or (get normalized :from)
                           (get normalized "from")))
            range-to (when (map? normalized)
                       (or (get normalized :to)
                         (get normalized "to")))
            range-min (when (map? normalized)
                        (or (get normalized :min)
                          (get normalized "min")))
            range-max (when (map? normalized)
                        (or (get normalized :max)
                          (get normalized "max")))]
        (cond
          (nil? normalized)
          acc

          (or (some? range-from) (some? range-to))
          (cond-> acc
            (some? range-from) (assoc (keyword (str field-name "-from")) (date->iso-str range-from))
            (some? range-to) (assoc (keyword (str field-name "-to")) (date->iso-str range-to)))

          (or (some? range-min) (some? range-max))
          (cond-> acc
            (some? range-min) (assoc (keyword (str field-name "-min")) range-min)
            (some? range-max) (assoc (keyword (str field-name "-max")) range-max))

          :else
          (assoc acc field-key normalized))))
    {}
    (or filters {})))

(defn serialize-server-filters
  "Serialize UI filters for server-backed list requests.

  Range filters are emitted using the UI field name.
  Scalar/select filters are emitted only when `filter-key-map` contains a target
  backend param for the field. Multi-select values are joined with commas."
  [filters filter-key-map]
  (reduce-kv
    (fn [acc field-id filter-value]
      (let [app-key (model-naming/ensure-app-keyword field-id)
            field-name (name app-key)
            backend-param (when (seq filter-key-map)
                            (or (get filter-key-map field-id)
                              (get filter-key-map app-key)))
            normalized (normalize-filter-value filter-value)
            range-from (when (map? normalized)
                         (or (get normalized :from)
                           (get normalized "from")))
            range-to (when (map? normalized)
                       (or (get normalized :to)
                         (get normalized "to")))
            range-min (when (map? normalized)
                        (or (get normalized :min)
                          (get normalized "min")))
            range-max (when (map? normalized)
                        (or (get normalized :max)
                          (get normalized "max")))]
        (cond
          (nil? normalized)
          acc

          (or (some? range-from) (some? range-to))
          (cond-> acc
            (some? range-from) (assoc (keyword (str field-name "-from")) (date->iso-str range-from))
            (some? range-to) (assoc (keyword (str field-name "-to")) (date->iso-str range-to)))

          (or (some? range-min) (some? range-max))
          (cond-> acc
            (some? range-min) (assoc (keyword (str field-name "-min")) range-min)
            (some? range-max) (assoc (keyword (str field-name "-max")) range-max))

          (nil? backend-param)
          acc

          (sequential? normalized)
          (let [values (->> normalized
                         (map (fn [value]
                                (if (keyword? value)
                                  (name value)
                                  value)))
                         (remove nil?)
                         (map str)
                         vec)]
            (if (seq values)
              (assoc acc backend-param (str/join "," values))
              acc))

          :else
          (assoc acc backend-param normalized))))
    {}
    (or filters {})))