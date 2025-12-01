(ns app.migrations.function-defaults
  "Preprocessor to convert function default markers to PostgreSQL syntax."
  (:require
    [clojure.walk :as walk]))

(defn convert-function-default
  "Convert function default marker for automigrate compatibility.
   Handles both map format {:function 'name'} and string format 'func_name()'."
  [default-value field-type options]
  (cond
    ;; Handle map format: {:function "gen_random_uuid"}
    (and (map? default-value) (:function default-value))
    (let [func-name (:function default-value)]
      (cond
        ;; For UUID primary keys with gen_random_uuid, use vector format
        (and (= field-type :uuid)
          (:primary-key options)
          (= func-name "gen_random_uuid"))
        [:gen_random_uuid]  ; Vector format for automigrate

        ;; For other cases, try the vector format
        :else
        [(keyword func-name)]))

    ;; Handle string format: "gen_random_uuid()"
    (and (string? default-value)
      (re-matches #"gen_random_uuid\(\)" default-value))
    ;; Convert to vector format for automigrate
    [:gen_random_uuid]

    ;; Return unchanged for other cases
    :else
    default-value))

(defn preprocess-field-defaults
  "Walk through field definitions and convert function defaults to SQL format.
   Also filters out validation metadata that automigrate doesn't understand.
   Preserves vector structure."
  [field-def]
  (if (vector? field-def)
    ;; Handle field definition vector: [field-name type options]
    (let [[field-name field-type options] field-def]
      (if (map? options)
        (let [;; Filter out validation metadata
              filtered-options (dissoc options :validation :admin :form :security :computed-fields)
              ;; Process defaults if present
              processed-options (if (contains? filtered-options :default)
                                  (let [new-default (convert-function-default (:default filtered-options) field-type filtered-options)]
                                    (if (nil? new-default)
                                      (dissoc filtered-options :default)
                                      (assoc filtered-options :default new-default)))
                                  filtered-options)]
          [field-name field-type processed-options])
        field-def))
    ;; If not a vector, process recursively
    (walk/postwalk
      (fn [form]
        (if (map? form)
          (let [;; Filter out validation metadata
                filtered-form (dissoc form :validation :admin :form :security :computed-fields)
                ;; Process defaults if present
                processed-form (if (contains? filtered-form :default)
                                 (let [new-default (convert-function-default (:default filtered-form) nil filtered-form)]
                                   (if (nil? new-default)
                                     (dissoc filtered-form :default)
                                     (assoc filtered-form :default new-default)))
                                 filtered-form)]
            processed-form)
          form))
      field-def)))

(defn preprocess-models
  "Preprocess all models to convert function defaults while preserving structure."
  [models]
  (into {}
    (for [[model-name model-def] models]
      [model-name
       (if (map? model-def)
         (cond-> (dissoc model-def :computed-fields)  ; Filter out model-level metadata
           (:fields model-def)
           (update :fields (fn [fields-vec]
                             (mapv preprocess-field-defaults fields-vec))))
         model-def)])))

(defn load-and-preprocess-models
  "Load models from EDN file and preprocess function defaults."
  [models-path]
  (-> models-path
    slurp
    read-string
    preprocess-models))
