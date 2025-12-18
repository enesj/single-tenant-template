(ns app.template.backend.metadata.service
  "Metadata service implementation for handling entity models.
   Thin wrapper around specialized metadata sub-services.
   
   Note: Record types have been moved to:
   - app.template.backend.metadata.service.metadata-service/TemplateMetadataService
   - app.template.backend.metadata.service.type-casting/TemplateTypeCastingService
   - app.template.backend.metadata.service.validation/TemplateValidationService
   - app.template.backend.metadata.service.query-builder/TemplateQueryBuilder"
  (:require
    [app.template.backend.metadata.service.metadata-service :as ms]
    [app.template.backend.metadata.service.query-builder :as qb]
    [app.template.backend.metadata.service.type-casting :as tc]
    [app.template.backend.metadata.service.validation :as val]))

;; Factory Functions
(defn create-metadata-service
  "Create a new metadata service instance."
  [models]
  (ms/->TemplateMetadataService models))

(defn create-type-casting-service
  "Create a new type casting service instance."
  [models]
  (tc/->TemplateTypeCastingService models))

(defn create-validation-service
  "Create a new validation service instance."
  [models db-service]
  (val/->TemplateValidationService models db-service))

(defn create-query-builder
  "Create a new query builder service instance."
  [models]
  (qb/->TemplateQueryBuilder models))
