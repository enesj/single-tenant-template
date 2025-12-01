(ns automigrate.errors.models
  (:require
   [automigrate.errors.core :refer [->error-message ->error-title
                                    add-error-value]]
   [automigrate.errors.extraction :as extract]
   [automigrate.util.validation :as validation-util]
   [clojure.set :as set]
   [clojure.string :as str]))

;; Error title implementations for models domain
(defmethod ->error-title :automigrate.models/->internal-models
  [_]
  "MODEL ERROR")

(defmethod ->error-title :automigrate.models/internal-models
  [_]
  "MIGRATION ERROR")

;; Error message implementations for models domain
(defmethod ->error-message :automigrate.models/->internal-models
  [data]
  (condp = (:pred data)
    `keyword? (add-error-value "Model name should be a keyword." (:val data))
    `map? (add-error-value "Models should be defined as a map." (:val data))
    "Models' definition error."))

(defmethod ->error-message :automigrate.models/public-model
  [data]
  (let [model-name (extract/get-model-name data)]
    (condp = (extract/problem-reason data)
      '(clojure.core/fn [%] (clojure.core/contains? % :fields))
      (format "Model %s should contain the key :fields." model-name)

      "no method" (add-error-value
                    (format "Model %s should be a map or a vector." model-name)
                    (:val data))

      (format "Invalid definition of the model %s." model-name))))

(defmethod ->error-message :automigrate.models/public-model-as-map
  [data]
  (let [model-name (extract/get-model-name data)]
    (when-not (vector? (:val data))
      (condp = (:pred data)
        '(clojure.core/fn [%] (clojure.core/contains? % :fields))
        (add-error-value
          (format "Model %s should contain :fields key." model-name)
          (:val data))

        (format "Model %s should be a map." model-name)))))

(defmethod ->error-message :automigrate.models/public-model-as-vec
  [data]
  (let [model-name (extract/get-model-name data)]
    (format "Model %s should be a vector." model-name)))

(defmethod ->error-message :automigrate.models.fields-vec/fields
  [data]
  (let [model-name (extract/get-model-name data)]
    (when-not (map? (:val data))
      (condp = (:pred data)
        '(clojure.core/<= 1 (clojure.core/count %) Integer/MAX_VALUE)
        (add-error-value
          (format "Model %s should contain at least one field." model-name)
          (:val data))

        `vector? (add-error-value
                   (format "Model %s should be a vector." model-name)
                   (:val data))

        'distinct? (add-error-value
                     (format "Model %s has duplicated fields." model-name)
                     (:val data))

        (format "Fields definition error in model %s." model-name)))))

(defmethod ->error-message :automigrate.models.indexes-vec/indexes
  [data]
  (let [model-name (extract/get-model-name data)]
    (condp = (:pred data)
      '(clojure.core/<= 1 (clojure.core/count %) Integer/MAX_VALUE)
      (add-error-value
        (format "Model %s should contain at least one index if :indexes key exists." model-name)
        (:val data))

      `vector? (add-error-value
                 (format "Indexes definition of model %s should be a vector." model-name)
                 (:val data))

      'distinct? (add-error-value
                   (format "Indexes definition of model %s has duplicated indexes." model-name)
                   (:val data))

      (format "Indexes definition error in model %s." model-name))))

(defmethod ->error-message :automigrate.models/validate-fields-duplication
  [data]
  (let [model-name (extract/get-model-name data)]
    (add-error-value
      (format "Model %s has duplicated fields." model-name)
      (:val data))))

(defmethod ->error-message :automigrate.models/validate-indexes-duplication
  [data]
  (let [model-name (extract/get-model-name data)]
    (add-error-value
      (format "Model %s has duplicated indexes." model-name)
      (:val data))))

(defmethod ->error-message :automigrate.models/public-model-as-map-strict-keys
  [data]
  (let [model-name (extract/get-model-name data)]
    (format "Model %s definition has extra key." model-name)))

(defmethod ->error-message :automigrate.models/validate-indexes-duplication-across-models
  [data]
  (let [duplicated-indexes (->> (:origin-value data)
                             (vals)
                             (map (fn [model]
                                    (when (map? model)
                                      (map first (:indexes model)))))
                             (remove nil?)
                             (flatten)
                             (extract/duplicates))]
    (format "Models have duplicated indexes: [%s]." (str/join ", " duplicated-indexes))))

(defmethod ->error-message :automigrate.models/validate-types-duplication-across-models
  [data]
  (let [duplicated-types (->> (:origin-value data)
                           (vals)
                           (map (fn [model]
                                  (when (map? model)
                                    (map first (:types model)))))
                           (remove nil?)
                           (flatten)
                           (extract/duplicates))]
    (format "Models have duplicated types: [%s]." (str/join ", " duplicated-types))))

(defmethod ->error-message :automigrate.models/validate-enum-field-misses-type
  [data]
  (let [models-internal (->> (:val data))
        all-types (set (validation-util/get-all-types models-internal))
        all-fields-no-type (validation-util/get-all-enum-fields-without-type
                             models-internal all-types)]
    (format "There are enum fields with missing enum types: [%s]."
      (str/join ", " all-fields-no-type))))

(defmethod ->error-message :automigrate.models/validate-indexed-fields
  [data]
  (let [model-name (extract/get-model-name data)
        model (:val data)
        model-fields (set (map :name (:fields model)))
        index-fields (->> (:indexes model)
                       (map #(get-in % [:options :fields]))
                       (flatten)
                       (set))
        missing-fields (vec (set/difference index-fields model-fields))]
    (format "Missing indexed fields %s in model %s." missing-fields model-name)))
