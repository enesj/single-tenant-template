(ns app.domain.backend.expenses.routes.duplicates
  "Admin API routes for duplicate detection and merging."
  (:require
    [app.admin.backend.services.admin.audit :as admin-audit]
    [app.domain.backend.expenses.handlers.search.entity-queries :as entity-queries]
    [app.domain.backend.expenses.routes.routes-factory :as routes-factory]
    [app.domain.backend.expenses.services.dedup-ignored-clusters :as ignored-clusters]
    [app.domain.backend.expenses.services.duplicates :as duplicates]
    [app.domain.backend.expenses.services.merge :as merge]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as admin-utils]
    [clojure.string :as str]))

(def ^:private to-app shared-db/to-app)

(defn- parse-entity-type
  "Parse entity-type into a keyword. Returns nil for invalid values."
  [x]
  (when (some? x)
    (let [k (cond
              (keyword? x) x
              :else (keyword (str x)))]
      (when (#{:suppliers :articles :stores :manufacturers :subcategories} k)
        k))))

(defn- parse-strategy
  "Parse strategy into a keyword. Returns nil for invalid values."
  [x]
  (when (some? x)
    (let [k (cond
              (keyword? x) x
              :else (keyword (str x)))]
      (when (#{:exact :prefix :trigram :levenshtein} k)
        k))))

(def ^:private default-detect-fetch-limit
  5000)

(def ^:private max-detect-fetch-limit
  20000)

(defn- clamp-detect-fetch-limit
  [fetch-limit]
  (-> fetch-limit
    (max 1)
    (min max-detect-fetch-limit)))

(def ^:private default-manual-search-limit
  20)

(def ^:private max-manual-search-limit
  50)

(defn- clamp-manual-search-limit
  [limit]
  (-> limit
    (max 1)
    (min max-manual-search-limit)))

(defn- manual-search
  [db entity-type query limit]
  (case entity-type
    :suppliers (entity-queries/search-suppliers db query limit nil)
    :articles (entity-queries/search-articles db query limit nil)
    :stores (entity-queries/search-stores db query limit nil)
    :manufacturers (entity-queries/search-manufacturers db query limit nil)
    :subcategories (entity-queries/search-subcategories db query limit nil)
    []))

(defn- require-elevated-admin-role
  [handler]
  (admin-mw/wrap-admin-role handler :admin))

(defn- detect-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            admin-id (admin-utils/get-admin-id request)
            entity-type (parse-entity-type (get qp "entity-type"))
            strategy (parse-strategy (get qp "strategy"))
            fetch-limit (-> (admin-utils/parse-int-param qp "fetch-limit" default-detect-fetch-limit)
                          clamp-detect-fetch-limit)]
        (cond
          (nil? admin-id)
          (admin-utils/error-response "Admin authentication required" :status 401)

          (not (and entity-type strategy))
          (admin-utils/error-response
            "Missing or invalid entity-type or strategy"
            :status 400)

          :else
          (let [opts (cond-> {:fetch-limit fetch-limit}
                       (get qp "prefix-words")
                       (assoc :prefix-words (admin-utils/parse-int-param qp "prefix-words" 2))

                       (get qp "threshold")
                       (assoc :threshold (try (Double/parseDouble (str (get qp "threshold")))
                                           (catch Exception _ 0.4)))

                       (get qp "max-distance")
                       (assoc :max-distance (admin-utils/parse-int-param qp "max-distance" 2))

                       (get qp "limit")
                       (assoc :limit (admin-utils/parse-int-param qp "limit" 50)))
                clusters (->> (duplicates/detect-duplicates db entity-type strategy opts)
                           (duplicates/attach-cluster-ids entity-type))
                ignored-ids (ignored-clusters/list-ignored-cluster-ids db admin-id entity-type)
                clusters* (duplicates/filter-ignored-clusters ignored-ids clusters)
                enriched (duplicates/enrich-with-usage-counts db entity-type clusters*)]
            (admin-utils/success-response {:clusters (to-app enriched)})))))
    "Failed to detect duplicates"))

(defn- manual-search-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            admin-id (admin-utils/get-admin-id request)
            entity-type (parse-entity-type (get qp "entity-type"))
            query (some-> (get qp "q") str str/trim)
            limit (-> (admin-utils/parse-int-param qp "limit" default-manual-search-limit)
                    clamp-manual-search-limit)]
        (cond
          (nil? admin-id)
          (admin-utils/error-response "Admin authentication required" :status 401)

          (nil? entity-type)
          (admin-utils/error-response "Missing or invalid entity-type" :status 400)

          (or (str/blank? query) (< (count query) 2))
          (admin-utils/success-response {:results []})

          :else
          (admin-utils/success-response
            {:results (->> (manual-search db entity-type query limit)
                        (duplicates/enrich-members-with-context db entity-type)
                        to-app)}))))
    "Failed to search manual merge candidates"))

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

(defn- ignore-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent]} (admin-utils/extract-request-context request)
            admin-id (admin-utils/get-admin-id request)
            body (routes-factory/read-json-body request)
            entity-type (parse-entity-type (or (:entity-type body)
                                             (:entity_type body)
                                             (get body "entity-type")
                                             (get body "entity_type")))
            cluster-id (some-> (or (:cluster-id body)
                                 (:cluster_id body)
                                 (get body "cluster-id")
                                 (get body "cluster_id"))
                         str)
            member-ids-raw (or (:member-ids body)
                             (:member_ids body)
                             (get body "member-ids")
                             (get body "member_ids")
                             [])
            member-ids-vec (cond
                             (sequential? member-ids-raw) (vec member-ids-raw)
                             (nil? member-ids-raw) []
                             :else [member-ids-raw])
            parsed-member-ids (mapv admin-utils/parse-uuid-custom member-ids-vec)
            note (some-> (or (:note body) (get body "note")) str)
            expected-id (duplicates/cluster-id entity-type parsed-member-ids)
            member-ids* (->> parsed-member-ids (map str) (map str/lower-case) sort vec)]
        (cond
          (nil? admin-id)
          (admin-utils/error-response "Admin authentication required" :status 401)

          (nil? entity-type)
          (admin-utils/error-response "Missing or invalid entity-type" :status 400)

          (or (empty? member-ids-vec) (some nil? parsed-member-ids))
          (admin-utils/error-response "Missing or invalid member-ids" :status 400)

          (or (not (seq cluster-id)) (not= cluster-id expected-id))
          (admin-utils/error-response "Missing or invalid cluster-id" :status 400)

          :else
          (do
            (ignored-clusters/ignore-cluster!
              db
              {:admin-id admin-id
               :entity-type entity-type
               :cluster-id cluster-id
               :member-ids member-ids*
               :note note})

            (admin-audit/log-audit! db
              {:admin_id admin-id
               :action "dedup_ignore_cluster"
               :entity-type "dedup_ignored_clusters"
               :entity-id nil
               :changes {:entity-type (name entity-type)
                         :cluster-id cluster-id
                         :member-ids-count (count member-ids*)
                         :note note}
               :ip-address ip-address
               :user-agent user-agent})

            (admin-utils/success-response {:cluster-id cluster-id})))))
    "Failed to ignore duplicate cluster"))

(defn- unignore-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [{:keys [ip-address user-agent]} (admin-utils/extract-request-context request)
            admin-id (admin-utils/get-admin-id request)
            body (routes-factory/read-json-body request)
            entity-type (parse-entity-type (or (:entity-type body)
                                             (:entity_type body)
                                             (get body "entity-type")
                                             (get body "entity_type")))
            cluster-id (some-> (or (:cluster-id body)
                                 (:cluster_id body)
                                 (get body "cluster-id")
                                 (get body "cluster_id"))
                         str)]
        (cond
          (nil? admin-id)
          (admin-utils/error-response "Admin authentication required" :status 401)

          (nil? entity-type)
          (admin-utils/error-response "Missing or invalid entity-type" :status 400)

          (not (seq cluster-id))
          (admin-utils/error-response "Missing or invalid cluster-id" :status 400)

          :else
          (do
            (ignored-clusters/unignore-cluster! db admin-id entity-type cluster-id)

            (admin-audit/log-audit! db
              {:admin_id admin-id
               :action "dedup_unignore_cluster"
               :entity-type "dedup_ignored_clusters"
               :entity-id nil
               :changes {:entity-type (name entity-type)
                         :cluster-id cluster-id}
               :ip-address ip-address
               :user-agent user-agent})

            (admin-utils/success-response {:cluster-id cluster-id})))))
    "Failed to unignore duplicate cluster"))

(defn- ignored-handler
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            admin-id (admin-utils/get-admin-id request)
            entity-type (parse-entity-type (get qp "entity-type"))
            limit (admin-utils/parse-int-param qp "limit" 100)
            offset (admin-utils/parse-int-param qp "offset" 0)]
        (if (nil? admin-id)
          (admin-utils/error-response "Admin authentication required" :status 401)
          (let [rows (ignored-clusters/list-ignored-clusters
                       db
                       admin-id
                       {:entity-type entity-type
                        :limit limit
                        :offset offset})]
            (admin-utils/success-response {:ignored (to-app rows)})))))
    "Failed to list ignored clusters"))

(defn routes
  "Duplicate detection and merge routes, mounted under /duplicates."
  [db]
  ["/duplicates"
   ["/detect" {:get {:handler (detect-handler db)}}]
   ["/manual-search" {:get {:handler (manual-search-handler db)}}]
   ["/merge-preview"
    {:middleware [require-elevated-admin-role]
     :post {:handler (merge-preview-handler db)}}]
   ["/merge"
    {:middleware [require-elevated-admin-role]
     :post {:handler (merge-handler db)}}]
   ["/ignore" {:post {:handler (ignore-handler db)}}]
   ["/unignore" {:post {:handler (unignore-handler db)}}]
   ["/ignored" {:get {:handler (ignored-handler db)}}]])
