(ns automigrate.sql.constraints
  "Constraint generation utilities for SQL operations."
  (:require
   [automigrate.constraints :as constraints]
   [automigrate.sql.fields :as sql-fields]))

;; Constraint generation functions
(defn ->primary-key-constraint
  "Generate primary key constraint SQL."
  [{:keys [model-name primary-key]}]
  (when (seq primary-key)
    (concat
      [[:constraint (constraints/primary-key-constraint-name model-name)]]
      primary-key)))

(defn ->unique-constraint
  "Generate unique constraint SQL."
  [{:keys [model-name field-name unique]}]
  (when (some? unique)
    [[:constraint (constraints/unique-constraint-name model-name field-name)]
     unique]))

(defn ->foreign-key-constraint
  "Generate foreign key constraint SQL."
  [{:keys [model-name field-name references]}]
  (when (some? references)
    (concat [[:constraint (constraints/foreign-key-constraint-name model-name field-name)]]
      references)))

(defn ->check-constraint
  "Generate check constraint SQL."
  [{:keys [model-name field-name check]}]
  (when (some? check)
    [[:constraint (constraints/check-constraint-name model-name field-name)]
     check]))

;; Column generation
(defn fields->columns
  "Convert field definitions to SQL column definitions."
  [{:keys [fields model-name]}]
  (reduce
    (fn [acc [field-name {:keys [unique primary-key foreign-key check] :as options}]]
      (let [rest-options (remove #(= :EMPTY %) [(:on-delete options :EMPTY)
                                                (:on-update options :EMPTY)
                                                (:null options :EMPTY)
                                                (:default options :EMPTY)])]
        (conj acc (concat
                    [field-name]
                    (sql-fields/field-type->sql options)
                    (->primary-key-constraint {:model-name model-name
                                               :primary-key primary-key})
                    (->unique-constraint {:model-name model-name
                                          :field-name field-name
                                          :unique unique})
                    (->foreign-key-constraint {:model-name model-name
                                               :field-name field-name
                                               :references foreign-key})
                    (->check-constraint {:model-name model-name
                                         :field-name field-name
                                         :check check})
                    rest-options))))
    []
    fields))
