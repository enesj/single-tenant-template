(ns app.template.backend.metadata.service.validation
  (:require
    [app.shared.field-metadata :as field-meta]
    [app.shared.type-conversion :as type-conv]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.metadata.service.metadata-service :as ms]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defrecord TemplateValidationService [models db-service]
  crud-protocols/ValidationService

  (validate-field [_ entity-key field-name value]
    (try
      (let [entity (ms/entity-definition* models entity-key)
            app-field (when entity (ms/app-field-key entity field-name))
            field-metadata (when (and entity app-field)
                             (field-meta/get-field-spec models (:app/entity entity) app-field))]
        (cond
          (nil? entity)
          {:valid? false :message (str "Unknown entity: " entity-key)}

          (nil? field-metadata)
          {:valid? false :message (str "Unknown field: " field-name)}

          :else
          (let [[_ field-type constraints] field-metadata
                type-valid? (case (type-conv/get-base-type field-type)
                              :uuid (or (nil? value)
                                      (uuid? value)
                                      (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" (str value)))
                              :integer (or (nil? value)
                                         (integer? value)
                                         (and (string? value) (re-matches #"^\d+$" value)))
                              :decimal (or (nil? value)
                                         (number? value)
                                         (and (string? value) (re-matches #"^\d+(\.\d+)?$" value)))
                              :varchar (or (nil? value) (string? value))
                              :text (or (nil? value) (string? value))
                              :boolean (or (nil? value) (boolean? value))
                              :jsonb true
                              :enum (or (nil? value) (string? value))
                              true)
                required? (false? (:null constraints))
                required-valid? (or (not required?)
                                  (and (some? value)
                                    (if (#{:varchar :text} (type-conv/get-base-type field-type))
                                      (not (str/blank? value))
                                      true)))
                valid? (and type-valid? required-valid?)
                message (cond
                          (not type-valid?) (str "Invalid type for " app-field)
                          (not required-valid?) (str app-field " is required")
                          :else nil)]
            {:valid? valid? :message message})))
      (catch Exception e
        (log/error e "Error validating field" field-name "for entity" entity-key)
        {:valid? false :message "Validation error"})))

  (validate-entity [this entity-key data]
    (try
      (let [validations (for [[field-name value] data
                               :when (not= field-name :id)]
                           (assoc (crud-protocols/validate-field this entity-key field-name value)
                             :field field-name))
            errors (filter #(not (:valid? %)) validations)
            valid? (empty? errors)]

        {:valid? valid? :errors (vec errors)})
      (catch Exception e
        (log/error e "Error validating entity" entity-key)
        {:valid? false :errors [{:message "Entity validation error"}]})))

  (validate-required-fields [_ entity-key data]
    (when-let [entity (ms/entity-definition* models entity-key)]
      (let [required-fields (->> (:fields entity)
                              (filter (fn [[field-name _ constraints]]
                                        (and (false? (:null constraints))
                                          (not (:primary-key constraints))
                                          (not (#{:created-at :updated-at} field-name)))))
                              (map first)
                              set)
            provided-fields (->> (keys data)
                              (map (fn [k]
                                     (let [kw (if (keyword? k) k (keyword k))]
                                       (ms/app-field-key entity kw))))
                              (remove nil?)
                              set)
            missing (seq (remove provided-fields required-fields))]
        {:valid? (nil? missing)
         :missing-fields (vec missing)})))

  (validate-foreign-keys [_this _tenant-id _entity-key _data]
    {:valid? true :invalid-references []}))
