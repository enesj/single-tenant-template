(ns app.shared.validation.unique
  "Unique value validation with platform-specific implementations.
   This namespace provides validation for unique constraints without
   direct database access - instead accepting functions to retrieve values.")

(defn create-unique-validator
  "Creates a validator that checks if value is unique.
   Takes a function that retrieves existing values rather than
   accessing the database directly."
  [get-existing-values-fn]
  [:fn {:error/message "This value already exists"}
   (fn [value]
     (let [existing-values (get-existing-values-fn)]
       (not (contains? (set existing-values) value))))])

(defn create-unique-validator-with-context
  "Creates a unique validator with entity and field context.
   The get-values-fn should accept entity-type and field-name as parameters."
  [entity-type field-name get-values-fn]
  [:fn {:error/message "This value already exists"}
   (fn [value]
     (let [existing-values (get-values-fn entity-type field-name)]
       (not (contains? (set existing-values) value))))])

;; Platform-specific value retrieval functions
;; These should be injected by the calling code rather than defined here

(defn make-clj-value-getter
  "Creates a Clojure-specific value getter function.
   Takes a database connection and returns a function that retrieves values."
  [db-conn execute-fn format-sql-fn]
  (fn [entity field]
    (let [query (format-sql-fn {:select [(keyword field)]
                                :from [(keyword entity)]
                                :where [:not= (keyword field) nil]})]
      (->> (execute-fn db-conn query)
        (map #(get % (keyword field)))))))

(defn make-cljs-value-getter
  "Creates a ClojureScript-specific value getter function.
   Takes app-db atom and denormalize function."
  [app-db-atom denormalize-fn]
  (fn [entity field]
    (let [entities (denormalize-fn (get-in @app-db-atom [:entities entity]))
          field-vals (map (keyword field) entities)]
      field-vals)))
