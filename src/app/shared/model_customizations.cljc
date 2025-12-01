(ns app.shared.model-customizations
  "Utilities for extracting field and entity customizations from models.edn.

   This namespace provides functions to parse enhanced models.edn files that include
   UI customizations, computed fields, and security settings alongside traditional
   database schema definitions.")

;; Field customization extraction

(defn extract-field-admin-customizations
  "Extract admin interface customizations from a field definition.

   Follows the same pattern as validation metadata extraction.
   Input: [:email [:varchar 255] {:admin {:display-order 2 :width '200px'}}]
   Output: {:email {:display-order 2 :width '200px'}}"
  [field-def]
  (when (and (vector? field-def) (>= (count field-def) 3))
    (let [[field-name _field-type constraints] field-def
          field-name-kw (keyword field-name)
          admin-customizations (:admin constraints)]
      (when admin-customizations
        {field-name-kw admin-customizations}))))

(defn extract-field-form-customizations
  "Extract form customizations from a field definition.

   Follows the same pattern as validation metadata extraction."
  [field-def]
  (when (and (vector? field-def) (>= (count field-def) 3))
    (let [[field-name _field-type constraints] field-def
          field-name-kw (keyword field-name)
          form-customizations (:form constraints)]
      (when form-customizations
        {field-name-kw form-customizations}))))

(defn extract-field-security-settings
  "Extract security/privacy settings from a field definition.

   Follows the same pattern as validation metadata extraction."
  [field-def]
  (when (and (vector? field-def) (>= (count field-def) 3))
    (let [[field-name _field-type constraints] field-def
          field-name-kw (keyword field-name)
          security-settings (:security constraints)]
      (when security-settings
        {field-name-kw security-settings}))))

;; Entity customization extraction

(defn extract-computed-fields
  "Extract computed field definitions from an entity.

   Input: {:computed-fields {:tenant-name {:type :string :compute-fn :join-tenant-name}}}
   Output: {:tenant-name {:type :string :compute-fn :join-tenant-name}}"
  [entity-def]
  (:computed-fields entity-def))

(defn extract-entity-admin-customizations
  "Extract all admin customizations for an entity, including computed fields.

   Follows the same pattern as field-specs/process-field for consistency.
   Returns a map of field-name -> admin-customizations for all fields in the entity."
  [entity-def]
  (let [;; Extract customizations from regular fields
        field-customizations (->> (:fields entity-def)
                               (map extract-field-admin-customizations)
                               (apply merge))
        ;; Extract customizations from computed fields
        computed-fields (:computed-fields entity-def)
        computed-field-customizations (->> computed-fields
                                        (map (fn [[field-name field-config]]
                                               (when-let [admin-config (:admin field-config)]
                                                 {field-name admin-config})))
                                        (apply merge))]
    (merge field-customizations computed-field-customizations)))

(defn extract-entity-form-customizations
  "Extract all form customizations for an entity."
  [entity-def]
  (->> (:fields entity-def)
    (map extract-field-form-customizations)
    (apply merge)))

;; Main extraction functions

(defn extract-all-admin-customizations
  "Extract admin customizations from complete models data.

   Handles both map format (EDN) and vector format (JSON) like field-specs/entity-specs.
   Input: {:users {:fields [...]} :tenants {:fields [...]}}
   Output: {:users {:field1 {...} :field2 {...}} :tenants {...}}"
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [(keyword entity-name) (extract-entity-admin-customizations entity-def)]))
      (into {}))))

(defn extract-all-form-customizations
  "Extract form customizations from complete models data."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [(keyword entity-name) (extract-entity-form-customizations entity-def)]))
      (into {}))))

(defn extract-all-computed-fields
  "Extract computed field definitions from complete models data."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [(keyword entity-name) (extract-computed-fields entity-def)]))
      (filter (fn [[_entity-name computed-fields]] (seq computed-fields)))
      (into {}))))

;; Role hierarchy helper
(defn role-sufficient?
  "Check if admin-role is sufficient for min-role requirement."
  [admin-role min-role]
  (let [role-hierarchy [:support :admin :super-admin :platform-admin]
        admin-level (.indexOf role-hierarchy admin-role)
        required-level (.indexOf role-hierarchy min-role)]
    (and (>= admin-level 0)
      (>= required-level 0)
      (>= admin-level required-level))))

;; Utility functions for working with customizations

(defn merge-with-defaults
  "Merge field customizations with default values."
  [customizations defaults]
  (->> customizations
    (map (fn [[field-name field-customizations]]
           [field-name (merge defaults field-customizations)]))
    (into {})))

(defn filter-by-role
  "Filter field customizations based on admin role permissions."
  [customizations admin-role]
  (letfn [(has-permission? [field-customizations]
            (if-let [min-role (:min-role field-customizations)]
              (role-sufficient? admin-role min-role)
              true))]
    (->> customizations
      (filter (fn [[_field-name field-customizations]]
                (has-permission? field-customizations)))
      (into {}))))

(defn filter-by-conditions
  "Filter field customizations based on conditional visibility rules."
  [customizations record]
  (letfn [(evaluate-condition [condition record]
            (cond
              ;; Simple field value matching: {:status "active"}
              (and (map? condition) (not (vector? (first (vals condition)))))
              (every? (fn [[field expected]]
                        (= (get record field) expected))
                condition)

              ;; Function-based conditions
              (fn? condition)
              (condition record)

              ;; Complex conditions: {:field [:operator value]}
              ;; Example: {:account_age_days [:< 30]}
              (and (map? condition) (vector? (first (vals condition))))
              (every? (fn [[field [operator value]]]
                        (let [field-value (get record field)]
                          (case operator
                            :< (< field-value value)
                            :> (> field-value value)
                            :<= (<= field-value value)
                            :>= (>= field-value value)
                            := (= field-value value)
                            :!= (not= field-value value)
                            :in (contains? (set value) field-value)
                            :not-in (not (contains? (set value) field-value))
                            false)))
                condition)

              :else false))

          (should-show? [field-customizations record]
            (let [show-when (:show-when (:conditional-visibility field-customizations))
                  hide-when (:hide-when (:conditional-visibility field-customizations))]
              (cond
                ;; If show-when is specified, field is only visible if condition is true
                show-when (evaluate-condition show-when record)

                ;; If hide-when is specified, field is hidden if condition is true
                hide-when (not (evaluate-condition hide-when record))

                ;; If no conditions specified, always show
                :else true)))]

    (->> customizations
      (filter (fn [[_field-name field-customizations]]
                (should-show? field-customizations record)))
      (into {}))))
