(ns app.shared.specs.form-fields
  "Malli specs for form-fields configuration files.

  Used by:
  - src/app/admin/frontend/config/form-fields.edn
  - src/app/domain/frontend/expenses/config/form-fields.edn" 
  (:require
    [malli.core :as m]
    [malli.error :as me]))

;; =============================================================================
;; Common
;; =============================================================================

(def FieldId
  "Field identifiers are sometimes strings (JSON-style) and sometimes keywords." 
  [:or :keyword :string])

(def FieldIdVec
  [:vector FieldId])

(def FieldType
  [:or :keyword :string])

(def NumberSchema
  "Malli doesn't include a built-in :number schema; use a predicate." 
  [:fn number?])

(def OptionValue
  "Permissive option values for selects (legacy + rich metadata modes)."
  [:or :keyword :string NumberSchema :boolean])

(def OptionItem
  "Select option can be a scalar (legacy) or metadata map (rich mode)."
  [:or
   [:or :keyword :string]
   [:map {:closed false}
    [:value OptionValue]
    [:label {:optional true} :string]
    [:color {:optional true} :string]
    [:disabled {:optional true} :boolean]]])

(def OptionsVec
  [:vector OptionItem])

(def FieldConfig
  "Per-field UI config.

  This is intentionally permissive because the project mixes keyword- and
  string-based formats and evolves over time." 
  [:map {:closed false}
   [:type {:optional true} FieldType]
   [:validation {:optional true} [:or :keyword :string]]
   [:placeholder {:optional true} :string]
   [:options {:optional true} OptionsVec]
   [:default {:optional true} :any]
    [:min {:optional true} NumberSchema]
    [:max {:optional true} NumberSchema]
   [:min-length {:optional true} [:int {:min 0}]]
   [:max-length {:optional true} [:int {:min 0}]]
    [:step {:optional true} NumberSchema]])

(def EntityFormFields
  "Schema for a single entity's form-fields config." 
  [:map {:closed false}
   [:create-fields {:optional true} FieldIdVec]
   [:edit-fields {:optional true} FieldIdVec]
   [:required-fields {:optional true} FieldIdVec]
   [:field-config {:optional true} [:map-of FieldId FieldConfig]]])

(def FormFieldsFile
  "Top-level map: entity keyword -> form-fields config." 
  [:map-of :keyword EntityFormFields])

(defn validate-form-fields
  "Validate an entire form-fields.edn data structure." 
  [data]
  (if (m/validate FormFieldsFile data)
    {:valid? true :data data}
    {:valid? false
     :errors (me/humanize (m/explain FormFieldsFile data))}))

;; =============================================================================
;; Strict checks
;; =============================================================================

(defn- normalize-id
  [x]
  (cond
    (keyword? x) (name x)
    (string? x) x
    :else nil))

(defn- check-no-duplicate-fields
  "Ensure create/edit/required vectors do not contain duplicates (after normalizing
  keyword/string identifiers)." 
  [data]
  (let [problems
        (->> data
             (mapcat
               (fn [[entity cfg]]
                 (let [checks [[:create-fields (:create-fields cfg)]
                               [:edit-fields (:edit-fields cfg)]
                               [:required-fields (:required-fields cfg)]]]
                   (for [[k xs] checks
                         :when (seq xs)
                         :let [normalized (map normalize-id xs)
                               dupes (->> normalized
                                          (remove nil?)
                                          frequencies
                                          (filter (fn [[_ n]] (> n 1)))
                                          (map first)
                                          sort
                                          vec)]
                         :when (seq dupes)]
                     {:entity entity
                      :problem (str "Duplicate entries in " k ": " dupes)
                      :fix "Remove duplicates from the vector."}))))
             vec)]
    (when (seq problems)
      problems)))

(defn validate-form-fields-strict
  "Validate form-fields with additional consistency checks." 
  [data]
  (let [schema-result (validate-form-fields data)
        dupes (check-no-duplicate-fields data)]
    (cond
      (not (:valid? schema-result))
      schema-result

      (seq dupes)
      {:valid? false
       :errors [(str "Form-fields consistency issues: " dupes)]
       :warnings dupes
       :data data}

      :else
      schema-result)))
