(ns app.template.backend.metadata.service.metadata-service
  (:require
    [app.shared.field-specs :as field-specs]
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [clojure.string :as str]))

(defn entity-definition*
  [models entity-key]
  (model-naming/entity-definition models entity-key))

(defn app-entity-key
  [models entity-key]
  (some-> (entity-definition* models entity-key) :app/entity))

(defn db-entity-key
  [models entity-key]
  (let [entity (entity-definition* models entity-key)]
    (when entity
      (or (:db/entity entity)
        (model-naming/app-entity->db models (:app/entity entity))))))

(defn app-field-key
  [entity-def field-name]
  (let [field-kw (if (keyword? field-name) field-name (keyword field-name))]
    (if entity-def
      (model-naming/db-field->app entity-def field-kw)
      (model-naming/db-keyword->app field-kw))))

(defn db-field-key
  [entity-def field-name]
  (let [field-kw (if (keyword? field-name) field-name (keyword field-name))]
    (if entity-def
      (model-naming/app-field->db entity-def field-kw)
      (model-naming/app-keyword->db field-kw))))

(defn entity-foreign-keys
  [models entity-key]
  (when-let [entity (entity-definition* models entity-key)]
    (let [fields (:fields entity)]
      (->> fields
        (keep (fn [[fname _ constraints]]
                (when-let [fk (:foreign-key constraints)]
                  (let [fk-kw (if (keyword? fk) fk (keyword fk))
                        fk-app (model-naming/db-keyword->app fk-kw)
                        foreign-entity (keyword (namespace fk-kw))
                        foreign-field (keyword (name fk-app))
                        foreign-entity-def (entity-definition* models foreign-entity)]
                    {:field fname
                     :db/field (db-field-key entity fname)
                     :foreign-table (model-naming/db-keyword->app foreign-entity)
                     :db/foreign-table (db-entity-key models foreign-entity)
                     :foreign-field foreign-field
                     :db/foreign-field (db-field-key foreign-entity-def foreign-field)}))))
        vec))))

(defrecord TemplateMetadataService [models]
  crud-protocols/MetadataService

  (get-entity-metadata [_ entity-key]
    (when-let [entity (entity-definition* models entity-key)]
      {:entity (:app/entity entity)
       :db/entity (:db/entity entity)
       :fields (:fields entity)
       :types (:types entity)
       :constraints (:constraints entity)
       :indexes (:indexes entity)
       :aliases {:db (:db/field-aliases entity)
                 :app (:app/field-aliases entity)}}))

  (get-field-metadata [_ entity-key field-name]
    (when-let [entity (entity-definition* models entity-key)]
      (let [app-field (app-field-key entity field-name)]
        (some (fn [[fname ftype constraints]]
                (when (= fname app-field)
                  {:entity (:app/entity entity)
                   :db/entity (:db/entity entity)
                   :field-name fname
                   :db/field-name (db-field-key entity fname)
                   :field-type ftype
                   :constraints constraints}))
          (:fields entity)))))

  (get-foreign-keys [_ entity-key]
    (entity-foreign-keys models entity-key))

  (validate-entity-exists [_ entity-key]
    (boolean (entity-definition* models entity-key)))

  (get-entity-field-specs [_ entity-key opts]
    (let [exclude-form-fields? (:exclude-form-fields? opts false)
          specs-fn (if exclude-form-fields?
                     field-specs/entity-specs
                     field-specs/form-entity-specs)
          app-key (app-entity-key models entity-key)]
      (when app-key
        (get (specs-fn models) app-key)))))
