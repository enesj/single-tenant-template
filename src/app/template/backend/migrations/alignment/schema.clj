(ns app.template.backend.migrations.alignment.schema
  "Schema expectation building from models.edn."
  (:require
    [app.template.backend.migrations.alignment.utils :as utils]))

(defn sql-type->expected
  [field-type]
  (cond
    (keyword? field-type)
    (case field-type
      :uuid {:type-kind :scalar :data-type "uuid"}
      :text {:type-kind :scalar :data-type "text"}
      :boolean {:type-kind :scalar :data-type "boolean"}
      :integer {:type-kind :scalar :data-type "integer"}
      :bigint {:type-kind :scalar :data-type "bigint"}
      :jsonb {:type-kind :scalar :data-type "jsonb"}
      :json {:type-kind :scalar :data-type "json"}
      :timestamptz {:type-kind :scalar :data-type "timestamp with time zone"}
      :timestamp {:type-kind :scalar :data-type "timestamp without time zone"}
      :date {:type-kind :scalar :data-type "date"}
      {:type-kind :scalar :data-type (utils/normalize-ident field-type)})

    (and (vector? field-type) (= :varchar (first field-type)))
    {:type-kind :scalar
     :data-type "character varying"
     :char-max (second field-type)}

    (and (vector? field-type) (= :decimal (first field-type)))
    {:type-kind :scalar
     :data-type "numeric"
     :numeric-precision (second field-type)
     :numeric-scale (nth field-type 2 nil)}

    (and (vector? field-type) (= :enum (first field-type)))
    {:type-kind :enum
     :udt-name (utils/normalize-ident (second field-type))}

    :else
    {:type-kind :unknown
     :raw field-type}))

(defn expected-nullable?
  [field-opts]
  (let [opts (or field-opts {})]
    (cond
      (true? (:primary-key opts)) false
      (contains? opts :null) (not (false? (:null opts)))
      :else true)))

(defn models->expected
  [models]
  (let [models (or models {})
        expected-tables
        (->> (keys models)
          (map utils/normalize-ident)
          (set))
        expected-columns
        (reduce-kv
          (fn [acc model-k model]
            (let [t (utils/normalize-ident model-k)
                  fields (:fields model)
                  cols
                  (reduce
                    (fn [m [field-k field-type field-opts]]
                      (let [col (utils/normalize-ident field-k)
                            type-exp (sql-type->expected field-type)
                            nullable? (expected-nullable? field-opts)]
                        (assoc m col
                          (merge type-exp
                            {:is-nullable (if nullable? "YES" "NO")}))))
                    {}
                    (or fields []))]
              (assoc acc t cols)))
          {}
          models)
        expected-indexes
        (->> models
          (mapcat (fn [[_ model]]
                    (for [[idx-name _idx-type _idx-opts] (:indexes model)]
                      (utils/normalize-ident idx-name))))
          (set))
        expected-enums
        (->> models
          (mapcat (fn [[_ model]]
                    (for [[type-name type-kind type-opts] (:types model)
                          :when (= type-kind :enum)]
                      [(utils/normalize-ident type-name) (vec (:choices type-opts))])))
          (into {}))]
    {:tables expected-tables
     :columns expected-columns
     :indexes expected-indexes
     :enums expected-enums}))
