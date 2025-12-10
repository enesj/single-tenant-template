(ns app.shared.validation.metadata
  "Validation metadata utilities for processing validation rules from model definitions.
   This namespace provides functions to extract and process validation metadata
   embedded in database model EDN files."
  (:require
    [clojure.set :as set]
    [clojure.string :as str]))

;; ============================================================================
;; Validation Metadata Schema Definition
;; ============================================================================

(def validation-types
  "Supported validation types for metadata"
  #{:text :email :phone :url :number :date :datetime :enum :boolean :json})

(def constraint-types
  "Supported constraint types in validation metadata"
  #{:pattern :min-length :max-length :min-value :max-value
    :required :unique :values :step})

;; ============================================================================
;; Validation Metadata Extraction
;; ============================================================================

(defn extract-validation-metadata
  "Extract validation metadata from field constraints.
   Returns the validation map or nil if no validation metadata exists."
  [constraints]
  (:validation constraints))

(defn get-validation-type
  "Get the validation type from metadata, with fallback based on field type"
  [validation-meta field-type field-name]
  (or (:type validation-meta)
      ;; Fallback to intelligent type detection
    (cond
      (str/ends-with? (name field-name) "_email") :email
      (str/ends-with? (name field-name) "_phone") :phone
      (str/ends-with? (name field-name) "_url") :url
      (= field-type :date) :date
      (= field-type :timestamptz) :datetime
      (vector? field-type)
      (cond
        (= (first field-type) :enum) :enum
        (= (first field-type) :varchar) :text
        :else :text)
      (#{:integer :bigint :decimal :numeric} field-type) :number
      (= field-type :boolean) :boolean
      (= field-type :jsonb) :json
      :else :text)))

(defn get-validation-constraints
  "Extract validation constraints from metadata, merging with database constraints"
  [validation-meta db-constraints field-type]
  (let [validation-constraints (:constraints validation-meta {})
        db-required (false? (:null db-constraints))
        unique (:unique db-constraints)
        varchar-max-length (when (vector? field-type)
                             (when (= (first field-type) :varchar)
                               (second field-type)))]
    (cond-> validation-constraints
      db-required (assoc :required true)
      unique (assoc :unique true)
      varchar-max-length (assoc :max-length varchar-max-length))))

(defn get-validation-messages
  "Get validation messages with defaults for common validation types"
  [validation-meta validation-type field-label]
  (let [custom-messages (:messages validation-meta {})
        default-messages (case validation-type
                           :email {:invalid (str field-label " must be a valid email address")
                                   :required (str field-label " is required")}
                           :phone {:invalid (str field-label " must be a valid phone number")
                                   :required (str field-label " is required")}
                           :url {:invalid (str field-label " must be a valid URL")
                                 :required (str field-label " is required")}
                           :number {:invalid (str field-label " must be a valid number")
                                    :required (str field-label " is required")}
                           :date {:invalid (str field-label " must be a valid date")
                                  :required (str field-label " is required")}
                           :datetime {:invalid (str field-label " must be a valid date and time")
                                      :required (str field-label " is required")}
                           :enum {:invalid (str "Please select a valid " field-label)
                                  :required (str field-label " is required")}
                           :boolean {:required (str field-label " selection is required")}
                           :json {:invalid (str field-label " must be valid JSON")
                                  :required (str field-label " is required")}
                           {:invalid (str "Invalid " field-label)
                            :required (str field-label " is required")})]
    (merge default-messages custom-messages)))

(defn get-ui-metadata
  "Extract UI-specific metadata for field rendering"
  [validation-meta validation-type]
  (let [custom-ui (:ui validation-meta {})
        default-ui (case validation-type
                     :email {:input-type "email"
                             :placeholder "Enter email address"}
                     :phone {:input-type "tel"
                             :placeholder "Enter phone number"}
                     :url {:input-type "url"
                           :placeholder "Enter URL"}
                     :number {:input-type "number"}
                     :date {:input-type "date"}
                     :datetime {:input-type "datetime-local"}
                     :boolean {:input-type "checkbox"}
                     {})]
    (merge default-ui custom-ui)))

;; ============================================================================
;; Validation Rule Generation
;; ============================================================================

(defn generate-field-validation-spec
  "Generate a complete field validation specification from metadata and constraints"
  [field-name field-type constraints field-label]
  (let [validation-meta (extract-validation-metadata constraints)
        validation-type (get-validation-type validation-meta field-type field-name)
        validation-constraints (get-validation-constraints validation-meta constraints field-type)
        validation-messages (get-validation-messages validation-meta validation-type field-label)
        ui-metadata (get-ui-metadata validation-meta validation-type)]

    {:validation-type validation-type
     :constraints validation-constraints
     :messages validation-messages
     :ui ui-metadata
     :has-metadata (some? validation-meta)}))

(defn generate-malli-schema
  "Generate Malli schema from validation specification"
  [validation-spec]
  (let [{:keys [validation-type constraints messages]} validation-spec
        ;; Ensure validation-type is a keyword (handle both keywords and symbols)
        validation-type-kw (if (keyword? validation-type)
                             validation-type
                             (keyword validation-type))
        base-schema (case validation-type-kw
                      :email :string
                      :phone :string
                      :url :string
                      :text :string
                      :number :double
                      :date :string  ;; ISO date strings
                      :datetime :string  ;; ISO datetime strings
                      :boolean :boolean
                      :json :any  ;; JSON can be any type
                      :enum :string  ;; Enum validation handled elsewhere
                      :any)

        ;; Build constraint properties with custom error messages
        props (cond-> {}
                ;; String length constraints
                (and (#{:email :phone :url :text} validation-type-kw)
                  (:min-length constraints))
                (assoc :min (:min-length constraints))

                (and (#{:email :phone :url :text} validation-type-kw)
                  (:max-length constraints))
                (assoc :max (:max-length constraints))

                ;; Numeric constraints
                (and (= validation-type-kw :number)
                  (:min-value constraints))
                (assoc :min (:min-value constraints))

                (and (= validation-type-kw :number)
                  (:max-value constraints))
                (assoc :max (:max-value constraints))

                ;; Add custom error function for better messages
                messages
                (assoc :error/fn
                  (fn [{:keys [value]}]
                    (let [pattern (when (:pattern constraints)
                                    (if (string? (:pattern constraints))
                                      (re-pattern (:pattern constraints))
                                      (:pattern constraints)))]
                      (cond
                        (and (or (nil? value)
                               (and (string? value) (empty? value)))
                          (:required messages))
                        (:required messages)

                        (and (:min-length constraints)
                          (string? value)
                          (< (count value) (:min-length constraints))
                          (:min-length messages))
                        (:min-length messages)

                        (and (:max-length constraints)
                          (string? value)
                          (> (count value) (:max-length constraints))
                          (:max-length messages))
                        (:max-length messages)

                        (and pattern
                          (string? value)
                          (not (re-matches pattern value))
                          (:invalid messages))
                        (:invalid messages)

                        :else
                        (or (:invalid messages) "Invalid value"))))))]

    (cond
      ;; Handle enum constraints specially
      (and (= validation-type-kw :enum) (:values constraints))
      [:enum (:values constraints)]

      ;; Handle all validations with a function validator that provides proper error messages
      (or (:pattern constraints) (seq props))
      [:fn {:error/fn (fn [{:keys [value]}]
                        (let [pattern (when (:pattern constraints)
                                        (if (string? (:pattern constraints))
                                          (re-pattern (:pattern constraints))
                                          (:pattern constraints)))]
                          (cond
                            ;; Check if value is nil or empty (required validation)
                            (or (nil? value)
                              (and (string? value) (empty? value)))
                            (or (:required messages) "This field is required")

                            ;; Check type for string-based fields
                            (and (#{:string :text :email :phone :url} validation-type-kw)
                              (not (string? value)))
                            "Must be a string"

                            ;; Check min length
                            (and (:min-length constraints)
                              (string? value)
                              (< (count value) (:min-length constraints)))
                            (or (:min-length messages)
                              (str "Must be at least " (:min-length constraints) " characters"))

                            ;; Check max length
                            (and (:max-length constraints)
                              (string? value)
                              (> (count value) (:max-length constraints)))
                            (or (:max-length messages)
                              (str "Cannot exceed " (:max-length constraints) " characters"))

                            ;; Check pattern
                            (and pattern
                              (string? value)
                              (not (re-matches pattern value)))
                            (or (:invalid messages) "Invalid format")

                            :else nil)))}
       ;; The actual validation function
       (fn [value]
         (let [pattern (when (:pattern constraints)
                         (if (string? (:pattern constraints))
                           (re-pattern (:pattern constraints))
                           (:pattern constraints)))]
           (and
             ;; Check string type for string-based validators
             (or (not (#{:string :text :email :phone :url} validation-type-kw))
               (string? value))
             ;; Min length check
             (or (not (:min-length constraints))
               (not (string? value))
               (>= (count value) (:min-length constraints)))
             ;; Max length check
             (or (not (:max-length constraints))
               (not (string? value))
               (<= (count value) (:max-length constraints)))
             ;; Pattern check
             (or (not pattern)
               (not (string? value))
               (re-matches pattern value)))))]

      ;; Base schema only
      :else
      base-schema)))

;; ============================================================================
;; Utility Functions
;; ============================================================================

(defn has-validation-metadata?
  "Check if field definition has validation metadata"
  [field-def]
  (let [[_ _ constraints] field-def]
    (contains? constraints :validation)))

(defn merge-field-validation
  "Merge validation metadata into existing field spec"
  [field-spec validation-spec]
  (-> field-spec
    (assoc :validation-type (:validation-type validation-spec))
    (assoc :validation-constraints (:constraints validation-spec))
    (assoc :validation-messages (:messages validation-spec))
    (merge (:ui validation-spec))
    (assoc :has-validation-metadata (:has-metadata validation-spec))))

(defn validate-metadata-schema
  "Validate that validation metadata follows the expected schema"
  [validation-meta]
  (when validation-meta
    (let [errors []]
      (cond-> errors
        (and (:type validation-meta)
          (not (validation-types (:type validation-meta))))
        (conj (str "Invalid validation type: " (:type validation-meta)
                ". Must be one of: " validation-types))

        (and (:constraints validation-meta)
          (not (set/subset? (set (keys (:constraints validation-meta))) constraint-types)))
        (conj (str "Invalid constraint keys: "
                (set/difference (set (keys (:constraints validation-meta))) constraint-types)
                ". Must be subset of: " constraint-types))))))

(defn field-name->label
  "Convert field name to human readable label"
  [field-name]
  (-> (name field-name)
    (clojure.string/split #"_id" 2)
    first
    (clojure.string/replace #"_" " ")
    clojure.string/capitalize))

(defn process-model-validation
  "Process all validation metadata for a model, returning enhanced field definitions"
  [model-def]
  (let [fields (:fields model-def)]
    (map (fn [[field-name _field-type constraints :as field-def]]
           (let [validation-meta (extract-validation-metadata constraints)
                 validation-errors (validate-metadata-schema validation-meta)]
             (if (seq validation-errors)
               (do
                 ;; Log validation errors in development
                 #?(:clj (println "Validation metadata errors for field" field-name ":" validation-errors)
                    :cljs (js/console.warn "Validation metadata errors for field" field-name ":" validation-errors))
                 field-def)  ;; Return original if invalid
               field-def))) ;; Return enhanced or original
      fields)))

(defn process-models-for-frontend
  "Process models data to include validation specs for frontend consumption.
   Accepts either the raw models map (as loaded from resources/db/models.edn)
   or a wrapped structure like {:data {...}}. Returns a map of
   model-name -> model-def with an added :validation-specs entry containing
   per-field validation metadata."
  [models-data]
  (let [;; Support both {:data {...}} and plain maps/vectors
        data-section (cond
                       (and (map? models-data) (:data models-data)) (:data models-data)
                       (map? models-data) models-data
                       (vector? models-data) (into {} models-data)
                       :else {})]
    (reduce-kv
      (fn [acc model-name model-def]
        (let [processed-fields
              (reduce (fn [field-acc [field-name field-type constraints :as field-def]]
                        (let [field-label (field-name->label field-name)
                              validation-spec (generate-field-validation-spec
                                                field-name field-type constraints field-label)]
                          (assoc field-acc field-name
                            {:field-def field-def
                             :validation-spec validation-spec})))
                {}
                (:fields model-def))]
          (assoc acc model-name
            (assoc model-def :validation-specs processed-fields))))
      {}
      data-section)))
