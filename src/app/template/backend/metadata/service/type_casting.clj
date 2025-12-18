(ns app.template.backend.metadata.service.type-casting
  (:require
    [app.shared.field-metadata :as field-meta]
    [app.shared.type-conversion :as type-conv]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.metadata.service.metadata-service :as ms])
  (:import
    (java.time LocalDateTime)))

(defrecord TemplateTypeCastingService [models]
  crud-protocols/TypeCastingService

  (cast-for-insert [_ entity-key data]
    (let [entity (ms/entity-definition* models entity-key)]
      (reduce-kv
        (fn [acc field-name value]
          (let [app-field (or (ms/app-field-key entity field-name) (keyword field-name))
                field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)
                casted-value (if field-type
                               (type-conv/cast-field-value field-type value)
                               value)]
            (assoc acc app-field casted-value)))
        {}
        data)))

  (cast-for-update [_ entity-key data]
    (let [entity (ms/entity-definition* models entity-key)
          cast-data (reduce-kv
                      (fn [acc field-name value]
                        (let [app-field (or (ms/app-field-key entity field-name) (keyword field-name))
                              field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)
                              casted-value (if field-type
                                             (type-conv/cast-field-value field-type value)
                                             value)]
                          (assoc acc app-field casted-value)))
                      {}
                      data)]
      (-> cast-data
        (dissoc :tenant-id :owner-id :tenant_id :owner_id)
        (assoc :updated-at [:cast (java.time.LocalDateTime/now) :timestamptz]))))

  (cast-field-value [_ entity-key field-name value]
    (let [entity (ms/entity-definition* models entity-key)
          app-field (or (ms/app-field-key entity field-name) (keyword field-name))
          field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)]
      (if field-type
        (type-conv/cast-field-value field-type value)
        value))))
