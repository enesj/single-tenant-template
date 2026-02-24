(ns app.domain.backend.expenses.routes.duplicates
  "Admin API routes for duplicate detection and merging."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as routes-factory]
    [app.domain.backend.expenses.services.duplicates :as duplicates]
    [app.domain.backend.expenses.services.merge :as merge]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.routes.admin.utils :as admin-utils]))

(def ^:private to-app shared-db/to-app)

(defn- parse-entity-type
  "Parse entity-type string to keyword. Returns nil for invalid values."
  [s]
  (when s
    (let [k (keyword s)]
      (when (#{:suppliers :articles :stores :manufacturers} k)
        k))))

(defn- parse-strategy
  "Parse strategy string to keyword. Returns nil for invalid values."
  [s]
  (when s
    (let [k (keyword s)]
      (when (#{:prefix :trigram :levenshtein} k)
        k))))

(defn- detect-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            entity-type (parse-entity-type (get qp "entity-type"))
            strategy (parse-strategy (get qp "strategy"))]
        (if (and entity-type strategy)
          (let [opts (cond-> {}
                       (get qp "prefix-words")
                       (assoc :prefix-words (admin-utils/parse-int-param qp "prefix-words" 2))

                       (get qp "threshold")
                       (assoc :threshold (try (Double/parseDouble (str (get qp "threshold")))
                                           (catch Exception _ 0.4)))

                       (get qp "max-distance")
                       (assoc :max-distance (admin-utils/parse-int-param qp "max-distance" 2))

                       (get qp "limit")
                       (assoc :limit (admin-utils/parse-int-param qp "limit" 50)))
                clusters (duplicates/detect-duplicates db entity-type strategy opts)
                enriched (duplicates/enrich-with-usage-counts db entity-type clusters)]
            (admin-utils/success-response {:clusters (to-app enriched)}))
          (admin-utils/error-response
            "Missing or invalid entity-type or strategy"
            :status 400))))
    "Failed to detect duplicates"))

(defn- merge-preview-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [body (routes-factory/read-json-body request)
            entity-type (parse-entity-type (or (:entity-type body)
                                             (:entity_type body)
                                             (get body "entity-type")
                                             (get body "entity_type")))
            primary-id (admin-utils/parse-uuid-custom
                         (or (:primary-id body)
                           (:primary_id body)
                           (get body "primary-id")
                           (get body "primary_id")))
            secondary-ids (->> (or (:secondary-ids body)
                                 (:secondary_ids body)
                                 (get body "secondary-ids")
                                 (get body "secondary_ids")
                                 [])
                            (map admin-utils/parse-uuid-custom)
                            (filter some?)
                            vec)]
        (cond
          (nil? entity-type)
          (admin-utils/error-response "Missing or invalid entity-type" :status 400)

          (nil? primary-id)
          (admin-utils/error-response "Missing or invalid primary-id" :status 400)

          (empty? secondary-ids)
          (admin-utils/error-response "Missing or empty secondary-ids" :status 400)

          :else
          (let [preview (merge/merge-preview db entity-type primary-id secondary-ids)]
            (admin-utils/success-response {:preview (to-app preview)})))))
    "Failed to generate merge preview"))

(defn- merge-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [body (routes-factory/read-json-body request)
            entity-type (parse-entity-type (or (:entity-type body)
                                             (:entity_type body)
                                             (get body "entity-type")
                                             (get body "entity_type")))
            primary-id (admin-utils/parse-uuid-custom
                         (or (:primary-id body)
                           (:primary_id body)
                           (get body "primary-id")
                           (get body "primary_id")))
            secondary-ids (->> (or (:secondary-ids body)
                                 (:secondary_ids body)
                                 (get body "secondary-ids")
                                 (get body "secondary_ids")
                                 [])
                            (map admin-utils/parse-uuid-custom)
                            (filter some?)
                            vec)]
        (cond
          (nil? entity-type)
          (admin-utils/error-response "Missing or invalid entity-type" :status 400)

          (nil? primary-id)
          (admin-utils/error-response "Missing or invalid primary-id" :status 400)

          (empty? secondary-ids)
          (admin-utils/error-response "Missing or empty secondary-ids" :status 400)

          :else
          (let [result (merge/merge-entities! db entity-type primary-id secondary-ids)]
            (admin-utils/success-response {:result (to-app result)})))))
    "Failed to merge entities"))

(defn routes
  "Duplicate detection and merge routes, mounted under /duplicates."
  [db]
  ["/duplicates"
   ["/detect" {:get {:handler (detect-handler db)}}]
   ["/merge-preview" {:post {:handler (merge-preview-handler db)}}]
   ["/merge" {:post {:handler (merge-handler db)}}]])
