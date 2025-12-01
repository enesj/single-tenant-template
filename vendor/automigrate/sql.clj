(ns automigrate.sql
  "Module for transforming actions from migration to SQL queries.

   This is the main orchestration layer that coordinates all SQL generation
   operations. The actual implementations are distributed across operation-specific
   namespaces for better maintainability and modularity."
  (:require ;; Load operation-specific namespaces for multimethod implementations (side effects)
 ;; These requires are necessary to register the multimethod implementations
   [automigrate.sql.columns]
   [automigrate.sql.core :as core]
   [automigrate.sql.indexes]
   [automigrate.sql.tables]
   [automigrate.sql.types]
   [clojure.spec.alpha :as s] ;; Load operation-specific namespaces for multimethod implementations (side effects)
))

;; Re-export public API
(def ->sql core/->sql)

;; For backwards compatibility, also expose the multimethod
(def action->sql core/action->sql)

;; Backwards compatibility for specs expected at :automigrate.sql/->sql
;; Older code references this spec key directly. Alias it to the core spec.
(s/def ::->sql (s/get-spec :automigrate.sql.core/->sql))
