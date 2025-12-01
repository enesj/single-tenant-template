(ns app.template.frontend.state.normalize
  (:require
    [app.frontend.utils.id :as id-utils]
    [clojure.string]))

(defn extract-entity-id
  "Extracts the ID from an entity, handling both namespaced and plain :id fields.
   Delegates to the centralized utility function."
  [entity]
  (id-utils/extract-entity-id entity))

(defn normalize-entity
  "Normalizes a single entity into {id -> entity} format.
   Handles both namespaced IDs (like :tenants/id) and plain :id fields.
   Ensures the entity always has a plain :id field for consistent access."
  [entity]
  (let [id (extract-entity-id entity)
        normalized-entity (assoc entity :id id)]
    {id normalized-entity}))

(defn normalize-entities
  "Takes a collection of entities and normalizes them into:
   {:data {id -> entity}
    :ids [id1 id2 ...]}
   Maintains order through :ids vector.
   Filters out entities with invalid or missing IDs to prevent normalization errors."
  [entities]
  (try
    (when-not (coll? entities)
      (throw (ex-info "Entities must be a collection" {:entities entities})))

    (let [;; Safely filter entities, handling null/undefined values
          safe-entities (remove nil? entities)

          ;; Filter out entities without valid IDs
          valid-entities (filter (fn [entity]
                                   (try
                                     (and entity  ; Not nil
                                       (map? entity)  ; Is a map
                                       ;; Has a valid (non-nil, non-empty) ID
                                       (let [id (or (:id entity)
                                                    ;; Find any namespaced id field
                                                  (->> entity
                                                    (filter (fn [[k v]]
                                                              (and (keyword? k)
                                                                (= (name k) "id")
                                                                (some? v))))  ; Value must not be nil
                                                    first
                                                    second))]
                                         (and id (not= id "") (not (nil? id)))))
                                     (catch js/Error e
                                       (.warn js/console "Error processing entity during validation:" entity e)
                                       false)))
                           safe-entities)

          filtered-count (- (count entities) (count valid-entities))

          ;; Log warning if entities were filtered
          _ (when (> filtered-count 0)
              (.warn js/console (str "🚨 Filtered out " filtered-count " entities with invalid IDs")))

          ;; Safely normalize entities with error handling
          normalized-map (reduce (fn [acc entity]
                                   (try
                                     (merge acc (normalize-entity entity))
                                     (catch js/Error e
                                       (.error js/console "Error normalizing entity:" entity e)
                                       acc)))  ; Skip problematic entity but continue
                           {}
                           valid-entities)

          ;; Safely extract IDs with error handling
          ids (reduce (fn [acc entity]
                        (try
                          (let [id (extract-entity-id entity)]
                            (if (some? id)
                              (conj acc id)
                              acc))
                          (catch js/Error e
                            (.error js/console "Error extracting ID from entity:" entity e)
                            acc)))  ; Skip problematic entity but continue
                []
                valid-entities)]
      {:data normalized-map
       :ids ids})
    (catch js/Error e
      (.error js/console "Critical error in normalize-entities:" e)
      {:data {} :ids []})))  ; Return empty structure instead of failing

(defn denormalize-entities
  "Takes normalized entity data and returns a vector of entities in order"
  [{:keys [data ids]}]
  (mapv #(get data %) ids))

(defn add-entity
  "Adds an entity to normalized structure"
  [{:keys [data ids] :as normalized} entity]
  (let [id (extract-entity-id entity)]
    (if (get data id)
      normalized
      {:data (assoc data id entity)
       :ids (conj ids id)})))

(defn update-entity
  "Updates an entity in normalized structure"
  [{:keys [data ids] :as normalized} entity]
  (let [id (extract-entity-id entity)]
    (if (get data id)
      {:data (assoc data id entity)
       :ids ids}
      normalized)))

(defn remove-entity
  "Removes an entity from normalized structure"
  [{:keys [data ids]} id]
  {:data (dissoc data id)
   :ids (vec (remove #{id} ids))})

(defn sort-normalized
  "Sorts normalized data by given comparator function"
  [{:keys [data ids] :as entities} [sort-field sort-direction]]
  (if (and sort-field sort-direction data ids)
    (let [sorted-ids (vec (sort-by #(get-in data [% sort-field])
                            (if (= :desc sort-direction)
                              #(compare %2 %1)
                              compare)
                            ids))]
      {:data data
       :ids sorted-ids})
    entities))
