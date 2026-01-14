(ns app.template.backend.utils.adapters.database
  "Shared database adapter utilities. 
   Acts as an aggregator for normalization and persistence logic.

   Backward-compatible wrapper that delegates JVM-only PG object conversion to
   `app.shared.adapters.database`."
  (:require
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.utils.adapters.normalization :as norm]
    [app.template.backend.utils.adapters.persistence :as persist]))

;; ============================================================================
;; Low-level PostgreSQL Object Conversion
;; ============================================================================

(def convert-pg-objects shared-db/convert-pg-objects)

;; Convenience: standard DB → app normalization pipeline
(def to-app shared-db/to-app)

;; ============================================================================
;; Re-exports for Backward Compatibility
;; ============================================================================

;; Normalization
(def convert-db-keys->app-keys norm/convert-db-keys->app-keys)
(def app-keyword->camel norm/app-keyword->camel)
(def convert-app-keys->camel-keys norm/convert-app-keys->camel-keys)
(def db-keyword->app-with-aliases norm/db-keyword->app-with-aliases)
(def normalize-admin-result norm/normalize-admin-result)

;; Persistence
(def with-admin-transaction persist/with-admin-transaction)

(defn execute-admin-query
  "Wrapped version of execute-admin-query that provides the internal normalization fn."
  [db query normalization-config & [options]]
  (persist/execute-admin-query db query 
    (fn [raw-result]
      (-> raw-result
        convert-pg-objects
        (norm/normalize-admin-result normalization-config)))
    options))
