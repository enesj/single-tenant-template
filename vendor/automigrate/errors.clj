(ns automigrate.errors
  "Refactored error handling system with modular domain-specific error handling.

   This namespace serves as the main orchestration layer that:
   1. Requires all domain-specific error namespaces for side effects (multimethod registration)
   2. Re-exports the public API functions from automigrate.errors.core

   Domain-specific error handling is distributed across:
   - automigrate.errors.models     - Model definition and validation errors
   - automigrate.errors.fields     - Field type and option validation errors
   - automigrate.errors.migrations - Migration action validation errors
   - automigrate.errors.commands   - CLI command argument validation errors

   Core infrastructure:
   - automigrate.errors.core       - Multimethod definitions and public API
   - automigrate.errors.extraction - Data extraction utilities"
  (:require
   [automigrate.errors.commands]
   [automigrate.errors.core :as core]
   [automigrate.errors.extraction] ;; Domain-specific namespaces (required for multimethod implementations)
   [automigrate.errors.fields]
   [automigrate.errors.migrations]
   [automigrate.errors.models]))

;; Re-export public API functions from core
(def explain-data->error-report core/explain-data->error-report)
(def custom-error->error-report core/custom-error->error-report)
