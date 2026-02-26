(ns app.template.frontend.auto-test-utils
  "Model parsing and dependency resolution utilities for test data generation."
  (:require
    [clojure.set :as set]))

;; =============================================================================
;; Model Parsing Utilities
;; =============================================================================

(defn get-field-constraints "Extract constraints from field definition: [name type constraints]"
  [field-def]
  (when (>= (count field-def) 3)
    (nth field-def 2)))

(defn get-field-type "Extract field type from definition: [name type constraints]"
  [field-def]
  (nth field-def 1))

(defn get-field-name "Extract field name from definition: [name type constraints]"
  [field-def]
  (first field-def))

;; =============================================================================
;; Dependency Resolution System
;; =============================================================================




