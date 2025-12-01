(ns app.shared.validation.builder
  "Builder functions for creating validators from model definitions.
   This namespace constructs Malli validators based on field definitions
   from models.edn with enhanced metadata support."
  (:require
    [app.shared.validation.constraints :as constraints]
    [app.shared.validation.field-types :as field-types]
    [app.shared.validation.metadata :as validation-meta]
    [app.shared.validation.unique :as unique]))

(defn generate-field-validator
  "Generate a complete Malli validator for a field including its type and checks.
   Takes entity-type, field definition tuple, and an optional get-values-fn
   for unique validation."
  ([entity-type field-def]
   (generate-field-validator entity-type field-def nil))
  ([entity-type [field-name field-type opts] get-values-fn]
   (let [base-schema (field-types/get-base-schema field-type opts)
         checks (constraints/get-field-checks [field-name field-type opts])
         unique? (:unique opts)
         result (cond-> [:and]
                  base-schema (conj base-schema)
                  (seq checks) (conj (into [] (concat checks)))
                  (and unique? get-values-fn)
                  (conj (unique/create-unique-validator-with-context
                          entity-type field-name get-values-fn)))
         required? (not (:null opts))]
     (if (not required?)
       [result :maybe]
       result))))

(defn generate-field-validator-with-metadata
  "Generate a Malli validator from validation metadata if present, otherwise fallback to original method.
   Takes entity-type, field definition tuple, and optional get-values-fn for unique validation."
  ([entity-type field-def]
   (generate-field-validator-with-metadata entity-type field-def nil))
  ([entity-type [field-name field-type opts] get-values-fn]
   (let [validation-meta (validation-meta/extract-validation-metadata opts)]
     (if validation-meta
       ;; Use metadata-driven validation
       (let [field-label (validation-meta/field-name->label field-name)
             validation-spec (validation-meta/generate-field-validation-spec
                               field-name field-type opts field-label)
             malli-schema (validation-meta/generate-malli-schema validation-spec)
             unique? (:unique opts)
             required? (not (:null opts))
             result (cond-> malli-schema
                      (and unique? get-values-fn)
                      (conj (unique/create-unique-validator-with-context
                              entity-type field-name get-values-fn)))]
         (if (not required?)
           [:maybe result]
           result))
       ;; Fallback to original method
       (generate-field-validator entity-type [field-name field-type opts] get-values-fn)))))

(defn enhanced-generate-model-validators
  "Generate Malli validators for all fields using metadata when available.
   Optionally takes a get-values-fn for unique constraint validation."
  ([models model-name]
   (enhanced-generate-model-validators models model-name nil))
  ([models model-name get-values-fn]
   (let [all-fields (get-in models [model-name :fields])
         fields (remove #(#{:id :created_at :updated_at} (first %)) all-fields)
         types (get-in models [model-name :types])]
     (->> fields
       (map (fn [[field-name field-type opts]]
              (let [opts- (if (and (vector? field-type) (= :enum (first field-type)))
                            (apply assoc {} (->> types
                                              (filter #(= (first %) (second field-type)))
                                              first
                                              rest))
                            opts)
                    field-type- (if (and (vector? field-type) (= :enum (first field-type)))
                                  (first field-type)
                                  field-type)
                    has-validation-meta? (validation-meta/has-validation-metadata? [field-name field-type- opts-])]

                (if (or has-validation-meta?
                      (:unique opts-)
                      (:check opts-)
                      (:enum opts-)
                      (:foreign-key opts-)
                      (#{:uuid :decimal :integer :jsonb :boolean :date :timestamptz :text :inet} field-type-)
                      (and (vector? field-type-)
                        (#{:decimal :varchar :array} (first field-type-))))
                  [field-name (generate-field-validator-with-metadata model-name
                                [field-name field-type- opts-]
                                get-values-fn)]
                  [field-name [:fn (constantly true)]]))))
       (into {})))))

(defn create-enhanced-validators
  "Create validators map from models data using metadata-driven validation.
   Optionally takes a get-values-fn for unique constraint validation."
  ([models-data]
   (create-enhanced-validators models-data nil))
  ([models-data get-values-fn]
   (into {}
     (for [model-name (keys models-data)]
       [model-name (enhanced-generate-model-validators models-data model-name get-values-fn)]))))

(defn generate-model-validators
  "Generate Malli validators for all fields in a model that need validation.
   Optionally takes a get-values-fn for unique constraint validation."
  ([models model-name]
   (generate-model-validators models model-name nil))
  ([models model-name get-values-fn]
   (let [all-fields (get-in models [model-name :fields])
         fields (remove #(#{:id :created_at :updated_at} (first %)) all-fields)
         types (get-in models [model-name :types])]
     (->> fields
       (map (fn [[field-name field-type opts]]
              (let [opts- (if (and (vector? field-type) (= :enum (first field-type)))
                            (apply assoc {} (->> types
                                              (filter #(= (first %) (second field-type)))
                                              first
                                              rest))
                            opts)
                    field-type- (if (and (vector? field-type) (= :enum (first field-type)))
                                  (first field-type)
                                  field-type)]

                (if (or (:unique opts-)
                      (:check opts-)
                      (:enum opts-)
                      (:foreign-key opts-)
                      (#{:uuid :decimal :integer :jsonb :boolean :date :timestamptz :text :inet} field-type-)
                      (and (vector? field-type-)
                        (#{:decimal :varchar :array} (first field-type-))))
                  [field-name (generate-field-validator model-name
                                [field-name field-type- opts-]
                                get-values-fn)]
                  [field-name [:fn (constantly true)]]))))
       (into {})))))

(defn create-validators
  "Create validators map from models data.
   Optionally takes a get-values-fn for unique constraint validation."
  ([models-data]
   (create-validators models-data nil))
  ([models-data get-values-fn]
   (into {}
     (for [model-name (keys models-data)]
       [model-name (generate-model-validators models-data model-name get-values-fn)]))))

(defn create-validators-with-platform-getters
  "Creates validators with platform-specific value getters.
   For CLJ: takes db-conn, execute-fn, and format-sql-fn
   For CLJS: takes app-db-atom and denormalize-fn"
  #?(:clj
     ([models-data db-conn execute-fn format-sql-fn]
      (let [get-values-fn (unique/make-clj-value-getter db-conn execute-fn format-sql-fn)]
        (create-validators models-data get-values-fn)))
     :cljs
     ([models-data app-db-atom denormalize-fn]
      (let [get-values-fn (unique/make-cljs-value-getter app-db-atom denormalize-fn)]
        (create-validators models-data get-values-fn)))))
