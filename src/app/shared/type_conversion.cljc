(ns app.shared.type-conversion
  "Single source of truth for type conversions.
   Consolidates generic type parsing and validation logic."
  (:require
    #?@(:clj [[cheshire.core :as json]])
    [app.shared.type-conversion-db :as db]
    [clojure.string :as str]))

;; =============================================================================
;; Re-exports for Backward Compatibility
;; =============================================================================

(def cast-for-database db/cast-for-database)
(def cast-field-value db/cast-field-value)
(def prepare-data-for-db db/prepare-data-for-db)

;; =============================================================================
;; Type Parsing Functions
;; =============================================================================

(defn parse-number
  "Parse a string into a number, handling both integer and decimal types.
   Returns nil for invalid input."
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (try
      (let [trimmed (str/trim s)]
        (cond
          (re-matches #"^-?\d+$" trimmed)
          #?(:clj (Long/parseLong trimmed)
             :cljs (js/parseInt trimmed 10))

          (re-matches #"^-?\d+(\.\d+)?([eE][+-]?\d+)?$" trimmed)
          #?(:clj (Double/parseDouble trimmed)
             :cljs (let [n (js/parseFloat trimmed)]
                     (if (js/isNaN n) nil n)))

          :else nil))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn numeric-field-type?
  "Check if a field type is numeric (integer or decimal)."
  [field-type]
  (or (= field-type :integer)
    (= field-type :decimal)
    (and (vector? field-type)
      (#{:integer :decimal} (first field-type)))))

(defn normalize-field-type
  "Normalize field type to a consistent format."
  [field-type]
  (if (vector? field-type)
    (first field-type)
    field-type))

(defn get-base-type
  "Get the base type from a potentially complex field type definition."
  [field-type]
  (normalize-field-type field-type))

;; =============================================================================
;; Converters
;; =============================================================================

(defn convert-for-validation
  "Convert a field value to the correct type for validation."
  [field-type value]
  (cond
    (and (string? value) (str/blank? value)) nil
    (and (string? value) (numeric-field-type? field-type)) (parse-number value)
    :else value))

(defn- parse-boolean-local
  "Parse diverse values into booleans, throwing on invalid strings."
  [value]
  (cond
    (nil? value) nil
    (boolean? value) value
    (number? value) (not (zero? value))
    (string? value) (let [trimmed (str/trim value)
                          lowered (str/lower-case trimmed)]
                      (cond
                        (str/blank? trimmed) nil
                        (#{"true" "t" "1" "yes" "on"} lowered) true
                        (#{"false" "f" "0" "no" "off"} lowered) false
                        :else (throw (ex-info (str "Cannot convert to boolean: " value)
                                       {:value value :target-type :boolean}))))
    :else (throw (ex-info (str "Cannot convert to boolean: " value)
                   {:value value :target-type :boolean}))))

(defn parse-date-string
  "Parse a date string into a platform-appropriate date object."
  [date-str]
  (when (and (string? date-str) (not (str/blank? date-str)))
    (try
      #?(:clj (java.time.LocalDateTime/parse date-str)
         :cljs (js/Date. date-str))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn parse-json-string
  "Parse a JSON string, returning nil for invalid JSON."
  [json-str]
  (when (and (string? json-str) (not (str/blank? json-str)))
    (try
      #?(:clj (json/parse-string json-str true)
         :cljs (js/JSON.parse json-str))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(comment
  (declare parse-json-string))

(defn convert-to-type
  "Convert value to the specified logical type, throwing on invalid input."
  [value target-type]
  (let [base-type (get-base-type target-type)]
    (case base-type
      :nil nil
      :string (when (some? value) (str value))
      :text (when (some? value) (str value))
      :varchar (when (some? value) (str value))
      :integer (cond
                 (nil? value) nil
                 (integer? value) value
                 (number? value) (long value)
                 (string? value) (let [parsed (parse-number value)]
                                   (if (integer? parsed)
                                     parsed
                                     (throw (ex-info (str "Cannot convert to integer: " value)
                                              {:value value :target-type :integer}))))
                 :else (throw (ex-info (str "Cannot convert to integer: " value)
                                {:value value :target-type :integer})))
      :decimal (cond
                 (nil? value) nil
                 (number? value) (double value)
                 (string? value) (let [parsed (parse-number value)]
                                   (if (number? parsed)
                                     (double parsed)
                                     (throw (ex-info (str "Cannot convert to decimal: " value)
                                              {:value value :target-type :decimal}))))
                 :else (throw (ex-info (str "Cannot convert to decimal: " value)
                                {:value value :target-type :decimal})))
      :number (cond
                (nil? value) nil
                (number? value) value
                (string? value) (let [parsed (parse-number value)]
                                  (if (some? parsed)
                                    parsed
                                    (throw (ex-info (str "Cannot convert to number: " value)
                                             {:value value :target-type :number}))))
                :else (throw (ex-info (str "Cannot convert to number: " value)
                               {:value value :target-type :number})))
      :boolean (parse-boolean-local value)
      :uuid (cond
              (nil? value) nil
              (uuid? value) value
              (string? value) (try
                                #?(:clj (java.util.UUID/fromString (str/trim value))
                                   :cljs (cljs.core/uuid (str/trim value)))
                                (catch #?(:clj Exception :cljs js/Error) _
                                  (throw (ex-info (str "Cannot convert to uuid: " value)
                                           {:value value :target-type :uuid}))))
              :else (throw (ex-info (str "Cannot convert to uuid: " value)
                             {:value value :target-type :uuid})))
      :json (cond
              (nil? value) nil
              (or (map? value) (vector? value) (seq? value)) value
              (string? value) (let [trimmed (str/trim value)]
                                (if (#{"null" "NULL"} trimmed)
                                  nil
                                  (let [parsed (parse-json-string trimmed)]
                                    (if (nil? parsed)
                                      (throw (ex-info (str "Cannot convert to JSON: " value)
                                               {:value value :target-type :json}))
                                      parsed))))
              :else (throw (ex-info (str "Cannot convert to JSON: " value)
                             {:value value :target-type :json})))
      :jsonb (convert-to-type value :json)
      :map (convert-to-type value :json)
      :date (cond
              (nil? value) nil
              #?@(:clj [(instance? java.time.LocalDate value) value])
              #?@(:clj [(instance? java.time.LocalDateTime value) value])
              #?@(:cljs [(instance? js/Date value) value])
              (string? value) (or (parse-date-string value)
                                (throw (ex-info (str "Cannot convert to date: " value)
                                         {:value value :target-type :date})))
              :else (throw (ex-info (str "Cannot convert to date: " value)
                             {:value value :target-type :date})))
      :timestamptz (cond
                     (nil? value) nil
                     #?@(:clj [(instance? java.time.LocalDateTime value) value])
                     #?@(:clj [(instance? java.util.Date value) value])
                     #?@(:cljs [(instance? js/Date value) value])
                     (string? value) (or (parse-date-string value)
                                       (throw (ex-info (str "Cannot convert to timestamptz: " value)
                                                {:value value :target-type :timestamptz})))
                     :else (throw (ex-info (str "Cannot convert to timestamptz: " value)
                                    {:value value :target-type :timestamptz})))
      :enum (cond
              (nil? value) nil
              (keyword? value) (name value)
              (string? value) value
              :else (str value))
      :array (let [element-type (when (and (vector? target-type)
                                        (> (count target-type) 1))
                                  (second target-type))
                   convert-element (fn [item]
                                     (if element-type
                                       (convert-to-type item element-type)
                                       item))]
               (cond
                 (nil? value) nil
                 (vector? value) (mapv convert-element value)
                 (sequential? value) (mapv convert-element value)
                 (string? value) (let [parsed (parse-json-string value)]
                                   (if (sequential? parsed)
                                     (mapv convert-element parsed)
                                     (throw (ex-info (str "Cannot convert to array: " value)
                                              {:value value :target-type :array}))))
                 :else (throw (ex-info (str "Cannot convert to array: " value)
                                {:value value :target-type :array}))))
      value)))

(defn try-convert-to-type
  "Best-effort conversion wrapper around `convert-to-type`.

  Returns nil when conversion fails instead of throwing."
  [value target-type]
  (try
    (convert-to-type value target-type)
    (catch #?(:clj Exception :cljs js/Error) _
      nil)))

(defn try-parse-uuid
  "Best-effort UUID parsing.

  - Returns a UUID when `value` is a valid UUID (or already a UUID)
  - Returns nil when `value` is nil, blank, or invalid"
  [value]
  (let [uuid-re #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        value (cond
                (nil? value) nil
                (string? value) (let [s (str/trim value)]
                                  (when-not (str/blank? s) s))
                :else value)]
    (cond
      (nil? value) nil
      (uuid? value) value

      ;; In CLJS, `cljs.core/uuid` may return a UUID value even for invalid strings.
      ;; Validate format first to preserve best-effort semantics (invalid → nil).
      (string? value)
      (when (re-matches uuid-re value)
        (try-convert-to-type value :uuid))

      :else
      (try-convert-to-type value :uuid))))

(defn detect-field-type
  "Best-effort detection of a value's logical type."
  [value]
  (cond
    (nil? value) :nil
    (boolean? value) :boolean
    (integer? value) :integer
    (number? value) :decimal
    #?@(:clj [(instance? java.util.Date value) :timestamptz])
    #?@(:cljs [(instance? js/Date value) :timestamptz])
    #?@(:clj [(instance? java.time.temporal.Temporal value) :timestamptz])
    (uuid? value) :uuid
    (map? value) :json
    (vector? value) :json
    (set? value) :json
    (string? value)
    (let [trimmed (str/trim value)
          lowered (str/lower-case trimmed)]
      (cond
        (str/blank? trimmed) :string
        (#{"true" "false" "t" "f" "yes" "no" "on" "off" "1" "0"} lowered) :boolean
        (re-matches #"^-?\d+$" trimmed) :integer
        (re-matches #"^-?\d+(\.\d+)?([eE][+-]?\d+)?$" trimmed) :decimal
        (re-matches #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" trimmed) :uuid
        (re-matches #"^\d{4}-\d{2}-\d{2}$" trimmed) :date
        (re-matches #"^\d{4}-\d{2}-\d{2}T.*" trimmed) :timestamptz
        (and (>= (count trimmed) 2)
          (let [first-char (first trimmed)
                last-char (last trimmed)]
            (and (#{\{ \[} first-char)
              (#{\} \]} last-char)))
          (try
            (boolean (parse-json-string trimmed))
            (catch #?(:clj Exception :cljs js/Error) _
              false))) :json
        :else :string))
    :else :unknown))
