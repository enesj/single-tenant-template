(ns app.shared.vector-config
  "Utilities for converting between vector-based and boolean map field configurations"
  (:require
    [clojure.string :as str]))

(defn vector-config->boolean-map
  "Convert vector config to boolean map format for existing UI compatibility"
  [vector-config available-columns]
  (let [visible-set (set vector-config)]
    (into {} (map (fn [col]
                    [col (contains? visible-set col)])
               available-columns))))

(defn boolean-map->vector-config
  "Convert boolean map back to ordered vector, preserving original order"
  [boolean-map original-order]
  (filterv #(get boolean-map % false) original-order))

(defn merge-config-with-overrides
  "Merge default vector config with user boolean overrides"
  [default-vector user-overrides available-columns]
  (let [boolean-defaults (vector-config->boolean-map default-vector available-columns)
        merged-boolean (merge boolean-defaults user-overrides)]
    (boolean-map->vector-config merged-boolean available-columns)))

(defn normalize-column-key
  "Convert between snake_case and kebab-case column keys"
  [key-name format]
  (case format
    :kebab-case (-> (name key-name)
                  (clojure.string/replace "_" "-")
                  keyword)
    :snake-case (-> (name key-name)
                  (clojure.string/replace "-" "_")
                  keyword)
    key-name))

(defn normalize-vector-config
  "Normalize a vector config to ensure consistent key format"
  [vector-config target-format]
  (mapv #(normalize-column-key % target-format) vector-config))

(defn validate-vector-config
  "Validate that vector config contains only available columns"
  [vector-config available-columns]
  (let [available-set (set available-columns)
        invalid-columns (remove available-set vector-config)]
    (when (seq invalid-columns)
      (throw (ex-info "Invalid columns in vector config"
               {:invalid-columns invalid-columns
                :available-columns available-columns})))
    true))

(defn apply-always-visible
  "Ensure always-visible columns are present and at the beginning"
  [vector-config always-visible]
  (let [always-set (set always-visible)
        without-always (remove always-set vector-config)
        result (vec (concat always-visible without-always))]
    result))

(defn calculate-display-order
  "Generate display-order values from vector config for legacy compatibility"
  [vector-config]
  (into {} (map-indexed (fn [idx col] [col (inc idx)]) vector-config)))
