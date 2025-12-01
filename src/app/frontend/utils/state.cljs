(ns app.frontend.utils.state
  "Shared utilities for Re-frame state management patterns"
  (:require
    [app.frontend.utils.id :as id-utils]))

;; ============================================================================
;; Loading State Management
;; ============================================================================

(defn create-loading-state
  "Sets loading state and optionally clears error state for a given key."
  ([db loading-key]
   (create-loading-state db loading-key nil))
  ([db loading-key error-key]
   (cond-> (assoc db loading-key true)
     error-key (dissoc error-key))))

(defn clear-loading-state
  "Clears loading state for a given key."
  [db loading-key]
  (dissoc db loading-key))

(defn create-error-state
  "Sets error state and clears loading state for given keys."
  [db error-key loading-key error-message]
  (-> db
    (assoc error-key error-message)
    (dissoc loading-key)))

;; ============================================================================
;; Entity State Management
;; ============================================================================

(defn update-entity-loading
  "Updates entity loading state in the standard [:entities entity-type :metadata :loading?] path."
  [db entity-type loading?]
  (assoc-in db [:entities entity-type :metadata :loading?] loading?))

(defn update-entity-error
  "Updates entity error state and clears loading."
  [db entity-type error-message]
  (-> db
    (assoc-in [:entities entity-type :error] error-message)
    (update-entity-loading entity-type false)))

;; ============================================================================
;; Combined Patterns
;; ============================================================================

(defn start-api-request
  "Standard pattern for starting an API request - sets loading states and clears errors."
  [db {:keys [loading-key error-key entity-type]}]
  (cond-> db
    loading-key (create-loading-state loading-key error-key)
    entity-type (update-entity-loading entity-type true)))

(defn handle-api-success
  "Standard pattern for API success - clears loading states."
  [db {:keys [loading-key entity-type]}]
  (cond-> db
    loading-key (clear-loading-state loading-key)
    entity-type (update-entity-loading entity-type false)))

(defn handle-api-error
  "Standard pattern for API errors - sets error states and clears loading."
  [db {:keys [loading-key error-key entity-type error-message]}]
  (let [safe-error (str error-message)]
    (cond-> db
      loading-key (clear-loading-state loading-key)
      error-key (assoc error-key safe-error)
      entity-type (update-entity-error entity-type safe-error))))

;; ============================================================================
;; Entity Normalization
;; ============================================================================

(defn safe-js->clj
  "Safely converts JavaScript objects to Clojure data with keywordized keys."
  [obj]
  (if (object? obj)
    (js->clj obj :keywordize-keys true)
    obj))

(defn normalize-entity-response
  "Normalizes an API response containing entities into the standard format.
   Handles both direct entity arrays and wrapped responses like {:users [...]}."
  [response entity-type]
  (let [clj-response (safe-js->clj response)
        entity-keyword (keyword entity-type)
        ;; Handle singular/plural conversion
        singular-type (if (.endsWith (name entity-type) "s")
                        (subs (name entity-type) 0 (dec (count (name entity-type))))
                        (name entity-type))
        plural-type (if (.endsWith (name entity-type) "s")
                      (name entity-type)
                      (str (name entity-type) "s"))
        ;; Try different response formats
        entities (or (get clj-response entity-keyword)                    ; exact match
                   (get clj-response (keyword singular-type))           ; singular form
                   (get clj-response (keyword plural-type))             ; plural form
                   (when (coll? clj-response) clj-response))            ; Direct array
        ;; Convert entities if they're still JavaScript objects
        safe-entities (if (and (coll? entities) (seq entities) (object? (first entities)))
                        (map safe-js->clj entities)
                        entities)
        ;; Filter out invalid entities
        valid-entities (if (coll? safe-entities)
                         (filter #(and % (map? %)) safe-entities)
                         [])
        ;; Extract IDs using existing utility
        entity-ids (id-utils/extract-ids valid-entities)
        ;; Create entities-by-id map
        entities-by-id (into {} (map (fn [entity]
                                       (let [id (id-utils/extract-entity-id entity)]
                                         [id entity])) valid-entities))]
    {:entities valid-entities
     :data entities-by-id
     :ids entity-ids}))

(defn update-entity-store
  "Updates entity store with normalized data in the standard template format."
  [db entity-type normalized-data]
  (let [{:keys [data ids]} normalized-data]
    (-> db
      (assoc-in [:entities entity-type :data] data)
      (assoc-in [:entities entity-type :ids] ids))))

(defn handle-entity-api-success
  "Complete pattern for handling entity API success with normalization."
  [db entity-type response {:keys [loading-key admin-key sync-event]}]
  (let [normalized (normalize-entity-response response entity-type)
        entities (:entities normalized)
        updated-db (-> db
                     (handle-api-success {:loading-key loading-key :entity-type entity-type})
                     (update-entity-store entity-type normalized))
        final-db (if admin-key
                   (assoc updated-db admin-key entities)
                   updated-db)
        dispatch-event (cond
                         (keyword? sync-event)
                         [sync-event entities]

                         (and (sequential? sync-event) (keyword? (first sync-event)))
                         (let [base (vec sync-event)]
                           (if (= 1 (count base))
                             (conj base entities)
                             base))

                         :else nil)]
    (cond-> {:db final-db}
      dispatch-event (assoc :dispatch dispatch-event))))
