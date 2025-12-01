(ns app.shared.type-conversion
  "Single source of truth for type conversions.

   This namespace consolidates type conversion logic from both validation
   and field_casting namespaces, providing clear, testable conversion
   functions per type.

   Functions in this namespace handle:
   - Database type casting (HoneySQL format)
   - Field value conversion (validation format)
   - Type parsing and normalization"
  (:require
    #?@(:clj [[taoensso.timbre :as log]
              [cheshire.core :as json]])
    #?@(:cljs [[taoensso.timbre :as log]
               [goog.date.DateTime]])
    [app.shared.field-metadata :as field-meta]
    [clojure.string :as str]))

(declare parse-json-string)

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
        (if (re-matches #"^-?\d+$" trimmed)
          #?(:clj (Long/parseLong trimmed)
             :cljs (js/parseInt trimmed 10))
          #?(:clj (Double/parseDouble trimmed)
             :cljs (js/parseFloat trimmed))))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn numeric-field-type?
  "Check if a field type is numeric (integer or decimal)."
  [field-type]
  (or (= field-type :integer)
    (= field-type :decimal)
    (and (vector? field-type)
      (#{:integer :decimal} (first field-type)))))

(defn- ->keyword
  "Best-effort conversion to keyword while preserving underscores."
  [v]
  (cond
    (keyword? v) v
    (string? v) (keyword v)
    (symbol? v) (keyword (name v))
    :else (keyword (str v))))

(defn- candidate-field-keys
  "Generate lookup variants for a field key to handle kebab/snake casing."
  [field-name]
  (let [kw (->keyword field-name)
        raw (name kw)
        snake (keyword (str/replace raw "-" "_"))
        kebab (keyword (str/replace raw "_" "-"))]
    (->> [kw snake kebab]
      (distinct))))

(defn- resolve-field-type
  "Resolve a field type using models metadata with casing fallbacks."
  [models entity field-name]
  (when (and models entity)
    (let [entity-key (field-meta/normalize-entity-key entity)]
      (some (fn [candidate]
              (field-meta/get-field-type models entity-key candidate))
        (candidate-field-keys field-name)))))

  ;; =============================================================================
  ;; Database Type Casting (HoneySQL format)
  ;; =============================================================================

(defn cast-for-database
  "Cast a value to the appropriate PostgreSQL type for HoneySQL.

     This function handles all PostgreSQL field types consistently and prevents
     HoneySQL from flattening nested maps in JSONB fields by properly casting them.

     Args:
       field-type: The field type from models.edn (e.g., :jsonb, :timestamp, [:enum :user-role])
       value: The value to cast

     Returns:
       Either the raw value (for simple types) or a HoneySQL cast expression [:cast value type]"
  [field-type value]

  (let [result (cond
                   ;; Nil values pass through unchanged
                 (nil? value)
                 (do
                   (log/info "Value is nil, passing through")
                   nil)

                   ;; JSONB fields - use [:lift ...] to prevent HoneySQL from flattening nested maps
                 (or (= field-type :jsonb) (= field-type :map))
                 [:cast [:lift value] :jsonb]

                   ;; Timestamp fields (both variants)
                 (or (= field-type :timestamp) (= field-type :timestamptz))
                 (let [converted-value (cond
                                         #?@(:clj [(instance? java.util.Date value) (str value)])
                                         #?@(:cljs [(instance? js/Date value) (.toISOString value)])
                                         :else value)]
                   [:cast converted-value field-type])

                   ;; Date fields
                 (= field-type :date)
                 (let [converted-value (cond
                                         #?@(:clj [(instance? java.util.Date value) (str value)])
                                         #?@(:cljs [(instance? js/Date value) (.toISOString value)])
                                         :else value)]
                   [:cast converted-value :date])

                   ;; Numeric types
                 (= field-type :decimal)
                 [:cast value :decimal]

                 (= field-type :integer)
                 [:cast value :integer]

                   ;; Network and other specialized types
                 (= field-type :inet)
                 [:cast value :inet]

                 (= field-type :boolean)
                 [:cast value :boolean]

                 (= field-type :uuid)
                 [:cast value :uuid]

                   ;; Text types don't need casting
                 (= field-type :text)
                 value

                   ;; Vector field types (enums, arrays, etc.)
                 (vector? field-type)
                 (case (first field-type)
                   :enum (let [enum-type-name (second field-type)
                               enum-type-str (-> (name enum-type-name)
                                               (str/replace "-" "_"))]
                           ;; Fixed: Use raw SQL cast to avoid parameter placeholder issues
                           [:raw (str "CAST('" value "' AS " enum-type-str ")")])

                   :array (let [array-element-type (second field-type)
                                pg-array-type-str (str (name array-element-type) "[]")]
                            (cond
                              (nil? value)
                              [:raw (str "ARRAY[]::" pg-array-type-str)]

                              (and (vector? value) (empty? value))
                              [:raw (str "ARRAY[]::" pg-array-type-str)]

                              (vector? value)
                              (if (= array-element-type :uuid)
                                (let [uuid-strs (map str value)
                                      array-literal (str "ARRAY[" (str/join "," (map #(str "'" % "'") uuid-strs)) "]")]
                                  (log/info "UUID array literal:" array-literal)
                                  [:raw (str array-literal "::" pg-array-type-str)])
                                  ;; Handle other array types
                                (let [array-literal (str "ARRAY[" (str/join "," (map #(str "'" % "'") value)) "]")]
                                  (log/info "Array literal:" array-literal)
                                  [:raw (str array-literal "::" pg-array-type-str)]))

                              :else
                              [:cast value [:raw pg-array-type-str]]))

                   :decimal
                   [:cast value :decimal]

                   :varchar
                   value

                     ;; Default for unknown vector types
                   value)

                   ;; Standalone enum type
                 (keyword? field-type)
                 (let [enum-type-str (-> (name field-type)
                                       (str/replace "-" "_"))]
                   ;; Fixed: Use raw SQL cast to avoid parameter placeholder issues
                   [:raw (str "CAST('" value "' AS " enum-type-str ")")])

                   ;; Default - pass through unchanged
                 :else
                 value)]

      ;;(log/info "Conversion result:" result)
    result))

(defn cast-field-value
  "Public wrapper matching documentation semantics for DB casting."
  [field-type value]
  (cast-for-database field-type value))

  ;; =============================================================================
  ;; Field Value Conversion (validation format)
  ;; =============================================================================

(defn convert-for-validation
  "Convert a field value to the correct type for validation.

     This handles the conversion of user input (often strings) to the appropriate
     types for validation and processing.

     Args:
       field-type: The field type from models.edn
       value: The value to convert

     Returns:
       The converted value or nil for empty strings"
  [field-type value]
  (cond
      ;; If we have an empty string, return nil
    (and (string? value) (str/blank? value))
    nil

      ;; Convert string to number if needed
    (and (string? value) (numeric-field-type? field-type))
    (parse-number value)

      ;; Otherwise return the original value
    :else
    value))

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

  ;; =============================================================================
  ;; Type Normalization
  ;; =============================================================================

(defn normalize-field-type
  "Normalize field type to a consistent format.

     Handles vector types like [:enum :user-role] or [:decimal 15 2] by
     extracting the base type."
  [field-type]
  (if (vector? field-type)
    (first field-type)
    field-type))

(defn get-base-type
  "Get the base type from a potentially complex field type definition.

     Examples:
     - :text -> :text
     - [:enum :user-role] -> :enum
     - [:decimal 15 2] -> :decimal"
  [field-type]
  (normalize-field-type field-type))

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

  ;; =============================================================================
  ;; Date and Time Utilities
  ;; =============================================================================

(defn format-date-for-db
  "Format a date value for database storage."
  [date-value]
  (cond
    #?@(:clj [(instance? java.util.Date date-value) (str date-value)])
    #?@(:cljs [(instance? js/Date date-value) (.toISOString date-value)])
    :else date-value))

(defn parse-date-string
  "Parse a date string into a platform-appropriate date object."
  [date-str]
  (when (and (string? date-str) (not (str/blank? date-str)))
    (try
      #?(:clj (java.time.LocalDateTime/parse date-str)
         :cljs (js/Date. date-str))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

  ;; =============================================================================
  ;; JSON Utilities
  ;; =============================================================================

(defn parse-json-string
  "Parse a JSON string, returning nil for invalid JSON."
  [json-str]
  (when (and (string? json-str) (not (str/blank? json-str)))
    (try
      #?(:clj (json/parse-string json-str true)
         :cljs (js/JSON.parse json-str))
      (catch #?(:clj Exception :cljs js/Error) _
        nil))))

(defn format-json-for-db
  "Format a value for JSONB storage in the database."
  [value]
  (cond
    (string? value) value
    :else
    #?(:clj (json/generate-string value)
       :cljs (js/JSON.stringify value))))

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

(defn prepare-data-for-db
  "Cast each field in `data` using model metadata and return DB-ready map.

   Options:
   - :include-nils? (default false) retains fields with nil values.
   - :field-type-resolver custom function (fn [field] field-type).
   - :preserve-unknown? (default true) keeps fields without metadata."
  ([models entity data]
   (prepare-data-for-db models entity data {}))
  ([models entity data {:keys [include-nils? field-type-resolver preserve-unknown?]
                        :or {include-nils? false preserve-unknown? true}}]
   (let [data-map (cond-> (or data {})
                    (not (map? data)) (into {}))
         result (empty data-map)
         resolver (or field-type-resolver
                    (when models
                      (fn [field-name]
                        (resolve-field-type models entity field-name))))]
     (reduce-kv
       (fn [acc field-name value]
         (let [field-type (when resolver (resolver field-name))
               casted (if field-type
                        (cast-field-value field-type value)
                        value)
               has-value? (some? casted)
               keep? (cond
                       has-value? true
                       field-type include-nils?
                       preserve-unknown? include-nils?
                       :else false)]
           (cond-> acc
             keep? (assoc field-name casted))))
       result
       data-map))))

(comment
    ;; Example usage of cast-for-database
  (cast-for-database :jsonb {:a 1 :b {:c 2}})
    ;; => [:cast [:lift {:a 1, :b {:c 2}}] :jsonb]

  (cast-for-database :timestamptz "2023-01-01T12:00:00Z")
    ;; => [:cast "2023-01-01T12:00:00Z" :timestamptz]

  (cast-for-database [:enum :user-role] "admin")
    ;; => [:cast "admin" [:raw "user_role"]]

  (cast-for-database [:array :uuid] [#?(:clj (java.util.UUID/randomUUID) :cljs (random-uuid))])
    ;; => [:raw "ARRAY['...']::uuid[]"]

    ;; Example usage of convert-for-validation
  (convert-for-validation :integer "123")
    ;; => 123

  (convert-for-validation :decimal "123.45")
    ;; => 123.45

  (convert-for-validation :text "  hello  ")
    ;; => "  hello  "

  (convert-for-validation :integer "   ")
    ;; => nil
  )
