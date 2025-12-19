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

(defn extract-foreign-keys "Extract all foreign key relationships from models"
  [models]
  (reduce
    (fn [acc [entity-name entity-def]]
      (let [fields (:fields entity-def)
            fks (keep (fn [field-def]
                        (let [constraints (get-field-constraints field-def)
                              fk (:foreign-key constraints)]
                          (when fk
                            {:entity entity-name
                             :field (get-field-name field-def)
                             :references fk})))
                  fields)]
        (concat acc fks)))
    []
    models))

(defn build-dependency-graph "Build dependency graph showing which entities depend on others"
  [models]
  (let [fks (extract-foreign-keys models)]
    (reduce
      (fn [graph fk]
        (let [dependent (:entity fk)
              referenced (-> fk :references namespace keyword)]
          (update graph dependent (fnil conj #{}) referenced)))
      {}
      fks)))

(defn topological-sort "Sort entities in dependency order (referenced entities first)"
  [dependency-graph all-entities]
  (loop [sorted []
         remaining (set all-entities)
         deps dependency-graph]
    (if (empty? remaining)
      sorted
      (let [no-deps (set/difference remaining (set (keys deps)))
            next-items (if (seq no-deps)
                         no-deps
                         (take 1 remaining))                ; Fallback to avoid infinite loop
            new-sorted (concat sorted next-items)
            new-remaining (set/difference remaining next-items)
            new-deps (reduce dissoc deps next-items)]
        (recur new-sorted new-remaining new-deps)))))
