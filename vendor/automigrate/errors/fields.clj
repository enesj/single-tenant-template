(ns automigrate.errors.fields
  (:require
   [automigrate.errors.core :refer [->error-message add-error-value]]
   [automigrate.errors.extraction :as extract]))

;; Error message implementations for fields domain
(defmethod ->error-message :automigrate.fields/fields
  [data]
  (condp = (extract/problem-reason data)
    '(clojure.core/<= 1
       (clojure.core/count %)
       Integer/MAX_VALUE) (add-error-value
                            "Action should contain at least one field."
                            (:val data))

    (add-error-value "Invalid fields definition." (:val data))))

(defmethod ->error-message :automigrate.fields/field
  [data]
  (condp = (extract/problem-reason data)
    '(clojure.core/fn [%]
       (clojure.core/contains? % :type))
    (add-error-value "Field should contain type." (:val data))

    (add-error-value "Invalid field definition." (:val data))))

(defmethod ->error-message :automigrate.fields/field-vec
  [data]
  (let [model-name (extract/get-model-name data)]
    (if (:reason data)
      (let [fq-field-name (extract/get-fq-field-name data)]
        (case (:reason data)
          "Extra input" (add-error-value
                          (format "Field %s has extra value in definition." fq-field-name)
                          (:val data))

          (add-error-value
            (format "Invalid field definition in model %s." model-name)
            (:val data))))
      (add-error-value
        (format "Invalid field definition in model %s." model-name)
        (:val data)))))

(defmethod ->error-message :automigrate.fields/type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (if (= "Insufficient input" (:reason data))
      (format "Missing type of field %s." fq-field-name)
      (add-error-value
        (format "Invalid type of field %s." fq-field-name)
        value))))

(defmethod ->error-message :automigrate.fields/float-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (condp = (extract/problem-reason data)
      `automigrate.fields/float-precision?
      (add-error-value
        (format "Parameter for float type of field %s should be integer between 1 and 53." fq-field-name)
        value)

      '(clojure.core/= (clojure.core/count %) 2)
      (add-error-value
        (format "Vector form of float type of field %s should have parameter." fq-field-name)
        value)

      (add-error-value
        (format "Invalid float type of field %s." fq-field-name)
        value))))

(defmethod ->error-message :automigrate.fields/keyword-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (add-error-value
      (format "Unknown type of field %s." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/char-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (condp = (extract/problem-reason data)
      `pos-int? (add-error-value
                  (format "Parameter for char type of field %s should be positive integer." fq-field-name)
                  value)

      '(clojure.core/= (clojure.core/count %) 2)
      (add-error-value
        (format "Vector form of char type of field %s should have parameter." fq-field-name)
        value)

      (add-error-value
        (format "Invalid float type of field %s." fq-field-name)
        value))))

(defmethod ->error-message :automigrate.fields/decimal-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (add-error-value
      (format "Invalid definition decimal/numeric type of field %s." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/bit-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (add-error-value
      (format "Invalid definition bit type of field %s." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/time-types
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (:val data)]
    (condp = (extract/problem-reason data)
      `pos-int? (add-error-value
                  (format "Parameter for time type of field %s should be positive integer." fq-field-name)
                  value)

      '(clojure.core/= (clojure.core/count %) 2)
      (add-error-value
        (format "Vector form of time type of field %s should have parameter." fq-field-name)
        value)

      (add-error-value
        (format "Invalid time type of field %s." fq-field-name)
        value))))

(defmethod ->error-message :automigrate.fields/field-name
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)]
    (add-error-value
      (format "Invalid field name %s." fq-field-name)
      (:val data))))

(defmethod ->error-message :automigrate.fields/options
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Invalid options of field %s." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/null
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :null of field %s should be boolean." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/primary-key
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :primary-key of field %s should be `true`." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/check
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :check of field %s should be a not empty vector." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/unique
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :unique of field %s should be `true`." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/options-strict-keys
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)]
    (add-error-value
      (format "Field %s has extra options." fq-field-name)
      (:val data))))

(defmethod ->error-message :automigrate.fields/default
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :default of field %s should be a constant." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/foreign-key
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :foreign-key of field %s should be a reference." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/on-delete
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :on-delete of field %s should be a valid action." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/on-update
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :on-update of field %s should be a valid action." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/array
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :array of field %s should be `true`." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/comment
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Option :comment of field %s should be string." fq-field-name)
      value)))

;; Field validation methods
(defmethod ->error-message :automigrate.fields/validate-fk-options-on-delete
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Field %s has :on-delete option without :foreign-key." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/validate-fk-options-on-update
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        value (extract/get-options data)]
    (add-error-value
      (format "Field %s has :on-update option without :foreign-key." fq-field-name)
      value)))

(defmethod ->error-message :automigrate.fields/validate-default-and-type
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)
        field-type (get-in data [:val :type])
        value (-> data :val (select-keys [:default :type]))]
    (add-error-value
      (format "Option %s of field %s does not match the field type: `%s`."
        :default
        fq-field-name
        field-type)
      value)))

(defmethod ->error-message :automigrate.fields/validate-default-and-null
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)]
    (add-error-value
      (format "Option :default of field %s couldn't be `nil` because of: `:null false`."
        fq-field-name)
      (:val data))))

(defmethod ->error-message :automigrate.fields/validate-fk-options-and-null-on-delete
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)]
    (add-error-value
      (format "Option :on-delete of field %s couldn't be :set-null because of: `:null false`."
        fq-field-name)
      (:val data))))

(defmethod ->error-message :automigrate.fields/validate-fk-options-and-null-on-update
  [data]
  (let [fq-field-name (extract/get-fq-field-name data)]
    (add-error-value
      (format "Option :on-update of field %s couldn't be :set-null because of: `:null false`."
        fq-field-name)
      (:val data))))
