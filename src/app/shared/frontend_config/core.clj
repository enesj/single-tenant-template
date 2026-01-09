(ns app.shared.frontend-config.core
  "Shared helpers for frontend config validation and syncing.

  This namespace is intentionally Clojure-only (used by bb tasks and tests).

  This file re-exports all public functions from submodules for backward compatibility:
  - discovery: Config discovery and loading
  - schema: DB schema index and allowlist handling
  - validation: Semantic validation
  - sync: Sync planning"
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [app.shared.frontend-config.schema :as schema]
    [app.shared.frontend-config.validation :as validation]
    [app.shared.frontend-config.sync :as sync]))

;; =============================================================================
;; Re-exports from discovery.clj
;; =============================================================================

(def normalize-id discovery/normalize-id)
(def normalize-entity-id discovery/normalize-entity-id)
(def normalize-field-id discovery/normalize-field-id)
(def read-edn-file discovery/read-edn-file)
(def discover-domain-names discovery/discover-domain-names)
(def config-bundles discovery/config-bundles)
(def load-bundles discovery/load-bundles)

;; =============================================================================
;; Re-exports from schema.clj
;; =============================================================================

(def models-index schema/models-index)
(def normalize-allowlist schema/normalize-allowlist)

;; =============================================================================
;; Re-exports from validation.clj
;; =============================================================================

(def validate-bundles validation/validate-bundles)

;; =============================================================================
;; Re-exports from sync.clj
;; =============================================================================

(def plan-sync sync/plan-sync)
