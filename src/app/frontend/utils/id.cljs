(ns app.frontend.utils.id
  "Utilities for consistent ID extraction across the application")

(defn extract-entity-id
  "Generic ID extraction that works with any entity type.
   Handles both :id and namespaced IDs like :users/id, :transaction-types/id etc."
  [entity]
  (or (:id entity)
      ;; Find any keyword with local name "id" (e.g., :transaction-types/id, :users/id)
    (->> entity
      (filter (fn [[k _]] (and (keyword? k) (= (name k) "id"))))
      first
      second)))

(defn extract-ids
  "Extract IDs from a collection of entities"
  [entities]
  (into #{} (map extract-entity-id) entities))
