(ns app.domain.backend.expenses.handlers.user-subcategories
  "User-facing subcategories endpoints.

  These endpoints are mounted under /api/v1/expenses/subcategories and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.subcategories :as subcategories]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access this page."))

(def ^:private max-page-limit
  500)

(defn- parse-page-limit
  [params default-limit]
  (-> (or (some-> (h/get-param params :limit) parse-long)
        default-limit)
    long
    (max 1)
    (min max-page-limit)))

(defn- parse-page-offset
  [params]
  (max 0 (long (or (some-> (h/get-param params :offset) parse-long) 0))))

(defn- category-id-provided?
  [body]
  (or (contains? body :category_id)
    (contains? body :category-id)
    (contains? body :categoryId)))

(defn- extract-category-id
  [body]
  (or (:category_id body)
    (:category-id body)
    (:categoryId body)))

(defn- parse-category-id
  [body]
  (when (category-id-provided? body)
    (let [raw (extract-category-id body)
          s (some-> raw str str/trim)]
      (when (and (some? s) (not (str/blank? s)))
        (h/try-parse-uuid s)))))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-subcategories-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (parse-page-limit qp 200)
                offset (parse-page-offset qp)
                search (h/get-param qp :search)
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir))
                rows (to-app ((:list subcategories/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count subcategories/service) db {:search search}) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list subcategories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list subcategories"} 500)))))))

(defn create-subcategory-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                category-id (parse-category-id body)
                name (:name body)
                description-provided? (or (contains? body :description)
                                        (contains? body :description_text)
                                        (contains? body :description-text)
                                        (contains? body :descriptionText))
                description (or (:description body)
                              (:description_text body)
                              (:description-text body)
                              (:descriptionText body))]

            (when-not category-id
              (throw (ex-info "Invalid category id" {:status 400})))

            (let [payload (cond-> {:category_id category-id
                                   :name name}
                            description-provided? (assoc :description description))
                  subcategory (to-app ((:create! subcategories/service) db payload))]
              (h/json-response {:data subcategory} 201)))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating subcategory" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} (or (:status (ex-data e)) 400)))
          (catch Exception e
            (log/error e "Failed to create subcategory" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create subcategory"} 500)))))))

(defn update-subcategory-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [subcategory-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                                 (get-in request [:parameters :path :id])))]
          (if-not subcategory-id
            (h/json-response {:error "Invalid subcategory id"} 400)
            (try
              (let [body (h/read-body-params request)
                    category-id-provided?* (category-id-provided? body)
                    category-id (parse-category-id body)
                    name-provided? (contains? body :name)
                    name (:name body)
                    description-provided? (or (contains? body :description)
                                            (contains? body :description_text)
                                            (contains? body :description-text)
                                            (contains? body :descriptionText))
                    description (or (:description body)
                                  (:description_text body)
                                  (:description-text body)
                                  (:descriptionText body))]

                (when (and category-id-provided?*
                        (some? (extract-category-id body))
                        (not (str/blank? (some-> (extract-category-id body) str str/trim)))
                        (nil? category-id))
                  (throw (ex-info "Invalid category id" {:status 400
                                                         :subcategory-id (str subcategory-id)
                                                         :category-id (extract-category-id body)})))

                (let [updates (cond-> {}
                                category-id-provided?* (assoc :category_id category-id)
                                name-provided? (assoc :name name)
                                description-provided? (assoc :description description))
                      updated (some-> ((:update! subcategories/service) db subcategory-id updates)
                                to-app)]
                  (if updated
                    (h/json-response {:data updated})
                    (h/not-found-response "Subcategory not found"))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating subcategory" {:error (ex-message e)
                                                                   :data (ex-data e)
                                                                   :subcategory-id (str subcategory-id)})
                (h/json-response {:error (ex-message e)} (or (:status (ex-data e)) 400)))
              (catch Exception e
                (log/error e "Failed to update subcategory" {:message (.getMessage e)
                                                             :subcategory-id (str subcategory-id)})
                (h/json-response {:error "Failed to update subcategory"} 500)))))))))

(defn batch-delete-subcategories-handler
  "Batch delete subcategories (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:subcategory_ids body)
                          (:subcategory-ids body)
                          (:subcategoryIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No subcategory ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more subcategory ids are invalid"} 400)

              :else
              (let [delete! (:delete! subcategories/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [subcategory-id ids]
                  (try
                    (if (boolean (delete! db subcategory-id))
                      (swap! deleted-ids conj (str subcategory-id))
                      (swap! errors conj {:id (str subcategory-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str subcategory-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str subcategory-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete subcategories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete subcategories"} 500)))))))
