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


