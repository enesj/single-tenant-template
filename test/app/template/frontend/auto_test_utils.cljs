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

(defn- extract-foreign-keys
  "Extract all foreign key relationships from models."
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
        (into acc fks)))
    []
    models))

(defn build-dependency-graph
  "Build dependency graph showing which entities depend on others."
  [models]
  (reduce
    (fn [graph {:keys [entity references]}]
      (let [referenced-entity (-> references namespace keyword)]
        (update graph entity (fnil conj #{}) referenced-entity)))
    {}
    (extract-foreign-keys models)))

(defn topological-sort
  "Sort entities so referenced entities appear before dependents."
  [dependency-graph all-entities]
  (loop [sorted []
         remaining (into {}
                     (map (fn [entity]
                            [entity (set (get dependency-graph entity #{}))]))
                     all-entities)]
    (if (empty? remaining)
      sorted
      (let [ready (->> remaining
                    (keep (fn [[entity deps]]
                            (when (empty? deps)
                              entity)))
                    sort
                    vec)]
        (if (seq ready)
          (let [ready-set (set ready)
                next-remaining (reduce-kv
                                 (fn [acc entity deps]
                                   (if (contains? ready-set entity)
                                     acc
                                     (assoc acc entity (set/difference deps ready-set))))
                                 {}
                                 remaining)]
            (recur (into sorted ready) next-remaining))
          ;; Cycle fallback: keep progressing deterministically.
          (let [fallback (first (sort (keys remaining)))]
            (recur (conj sorted fallback) (dissoc remaining fallback))))))))
