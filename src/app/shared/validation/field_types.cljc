(ns app.shared.validation.field-types
  "Field type validators and conversion functions.
   This namespace handles type-specific validation logic including
   enum validators, JSON validators, and type conversions."
  (:require
    #?@(:clj [[cheshire.core :as json]])
    #?@(:cljs [[goog.date.DateTime]])
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]))

;; Re-export from consolidated namespace
(def parse-number type-conv/parse-number)
(def numeric-field-type? type-conv/numeric-field-type?)

;; Validator creators
(defn enum-validator
  "Creates an enum validator with allowed values"
  [allowed-values]
  [:fn {:error/message (str "Must be one of: " (str/join ", " allowed-values))}
   (fn [value]
     ((into #{} allowed-values) value))])

(defn json-validator
  "Creates a JSON validator"
  []
  [:and
   string?
   [:fn {:error/message "Must be valid JSON"}
    (fn [value]
      (try
        #?(:clj (json/parse-string value)
           :cljs (js/JSON.parse value))
        true
        (catch #?(:clj Exception
                  :cljs :default) _
          false)))]])

(defn uuid-decoder
  "Creates a UUID decoder function for platform-specific UUID parsing"
  []
  #?(:clj #(try (java.util.UUID/fromString %)
             (catch Exception _ %))
     :cljs #(try (uuid %)
              (catch js/Error _ %))))

(defn date-validator
  "Creates a date validator for platform-specific date types"
  []
  [:or :string
   #?(:clj [:fn (fn [v] (instance? java.time.LocalDate v))]
      :cljs [:fn (fn [v] (instance? js/Date v))])])

(defn get-base-schema
  "Returns the base Malli schema for a field type"
  [field-type opts]
  (cond
    ;; Handle vector field types (e.g., [:decimal 15 2], [:varchar 255])
    (vector? field-type)
    (case (first field-type)
      :decimal [:or number? [:string {:decode/string type-conv/parse-number}]]
      :varchar [:string]
      :array (let [array-type (second field-type)]
               (case array-type
                 :uuid [:vector [:or uuid? [:string {:decode/string (uuid-decoder)}]]]
                 [:vector :any]))
      [:string])

    ;; Handle simple keyword field types
    :else
    (case field-type
      :text [:string]
      :varchar [:string]
      :decimal [:or number? [:string {:decode/string type-conv/parse-number}]]
      :integer [:or int? [:string {:decode/string type-conv/parse-number}]]
      :timestamp string?
      :timestamptz string?
      :date (date-validator)
      :uuid [:or uuid? [:string {:decode/string (uuid-decoder)}]]
      :jsonb [:or map? [:and string? (json-validator)]]
      :inet [:string]
      :boolean :boolean
      :enum [:and string? (enum-validator (get-in opts [:enum :choices]))]
      [:string])))

(defn convert-field-value
  "Convert a field value to the correct type based on the field definition.
   Takes validators map, entity name, field key, and value."
  [validators entity-name field-key value]
  (let [field-type (first (get-in validators [entity-name field-key]))]
    (type-conv/convert-for-validation field-type value)))
