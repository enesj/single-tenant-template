(ns app.template.backend.migrations.alignment.schema
  "Schema expectation building from models.edn."
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
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

(defn- foreign-key->expected
  [field-opts]
  (let [opts (or field-opts {})
        fk (:foreign-key opts)]
    ;; fk is typically a keyword like :suppliers/id
    (when fk
      (cond-> {:ref-table (some-> (namespace fk) utils/normalize-ident)
               :ref-column (some-> (name fk) utils/normalize-ident)}
        (:on-delete opts) (assoc :on-delete (:on-delete opts))
        (:on-update opts) (assoc :on-update (:on-update opts))))))

(defn- models->expected-foreign-keys
  [models]
  (reduce-kv
    (fn [acc model-k model]
      (let [t (utils/normalize-ident model-k)
            fields (:fields model)
            fk-cols
            (reduce
              (fn [m [field-k _field-type field-opts]]
                (let [col (utils/normalize-ident field-k)
                      opts (or field-opts {})]
                  (if-let [exp (foreign-key->expected opts)]
                    (assoc m col exp)
                    m)))
              {}
              (or fields []))]
        (if (seq fk-cols)
          (assoc acc t fk-cols)
          acc)))
    {}
    (or models {})))

(defn- extract-where-predicate
  "Extract the predicate portion from a formatted SQL query string.

  We intentionally generate a trivial query (SELECT 1 FROM t WHERE ...) via
  HoneySQL, and then keep only the predicate."
  [formatted-sql]
  (when-let [[_ pred] (re-find #"(?is)where\s+(.*)$" (or formatted-sql ""))]
    (str/trim pred)))

(defn- where->predicate-sql
  "Return a SQL predicate string for a HoneySQL :where expression.

  The returned value is intended for comparison against pg_get_expr(indpred,...)
  output, so we only return the predicate (no leading WHERE keyword)."
  [where-expr]
  (when where-expr
    (let [formatted (sql/format {:select [1]
                                 :from [:t]
                                 :where where-expr}
                      {:inline true})
          formatted-sql (if (sequential? formatted)
                          (first formatted)
                          formatted)]
      (extract-where-predicate formatted-sql))))

(defn- models->expected-index-definitions
  "Extract expected index definitions from models.

  Returns:
    {:definitions {index-name {:table .. :method .. :unique? .. :keys [...] :predicate ..}}
     :duplicates  {index-name [{:table .. :index .. :definition ..} ...]}}

  Notes:
  - index-name is normalized (lowercase, underscores)
  - :keys are derived from :indexes entry opts :fields
  - :predicate is derived from :indexes entry opts :where (HoneySQL form)"
  [models]
  (reduce-kv
    (fn [acc model-k model]
      (let [t (utils/normalize-ident model-k)]
        (reduce
          (fn [acc2 [idx-name idx-type idx-opts]]
            (let [idx (utils/normalize-ident idx-name)
                  opts (or idx-opts {})
                  fields (:fields opts)
                  keys (when (sequential? fields)
                         (mapv utils/normalize-ident fields))
                  definition {:table t
                              :method (utils/normalize-ident idx-type)
                              :unique? (boolean (:unique opts))
                              :keys keys
                              :predicate (where->predicate-sql (:where opts))}]
              (if (get-in acc2 [:definitions idx])
                (update-in acc2 [:duplicates idx] (fnil conj [])
                  {:table t :index idx :definition definition})
                (assoc-in acc2 [:definitions idx] definition))))
          acc
          (or (:indexes model) []))))
    {:definitions {} :duplicates {}}
    (or models {})))

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
        index-defs (models->expected-index-definitions models)
        expected-enums
        (->> models
          (mapcat (fn [[_ model]]
                    (for [[type-name type-kind type-opts] (:types model)
                          :when (= type-kind :enum)]
                      [(utils/normalize-ident type-name) (vec (:choices type-opts))])))
          (into {}))
        expected-foreign-keys (models->expected-foreign-keys models)]
    {:tables expected-tables
     :columns expected-columns
     :indexes expected-indexes
     :index-definitions (:definitions index-defs)
     :index-definition-duplicates (:duplicates index-defs)
     :enums expected-enums
     :foreign-keys expected-foreign-keys}))
