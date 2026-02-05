(ns app.domain.backend.expenses.handlers.user-categories
  "User-facing categories endpoints.

  These endpoints are mounted under /api/v1/expenses/categories and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.categories :as categories]
    [app.shared.adapters.database :as shared-db]
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

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-categories-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (or (some-> (h/get-param qp :limit) parse-long) 200)
                offset (or (some-> (h/get-param qp :offset) parse-long) 0)
                search (h/get-param qp :search)
                rows (to-app ((:list categories/service) db {:limit limit
                                                             :offset offset
                                                             :search search}))
                rows (cond-> rows (sequential? rows) vec)]
            (h/json-response {:data rows :limit limit :offset offset}))
          (catch Exception e
            (log/error e "Failed to list categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list categories"} 500)))))))

(defn create-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                name (:name body)
                description-provided? (or (contains? body :description)
                                        (contains? body :description_text)
                                        (contains? body :description-text)
                                        (contains? body :descriptionText))
                description (or (:description body)
                              (:description_text body)
                              (:description-text body)
                              (:descriptionText body))
                payload (cond-> {:name name}
                          description-provided? (assoc :description description))
                category (to-app ((:create! categories/service) db payload))]
            (h/json-response {:data category} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating category" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create category" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create category"} 500)))))))

(defn update-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [category-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                              (get-in request [:parameters :path :id])))]
          (if-not category-id
            (h/json-response {:error "Invalid category id"} 400)
            (try
              (let [body (h/read-body-params request)
                    name-provided? (or (contains? body :name)
                                     (contains? body :display_name)
                                     (contains? body :display-name)
                                     (contains? body :displayName))
                    name (or (:name body)
                           (:display_name body)
                           (:display-name body)
                           (:displayName body))
                    description-provided? (or (contains? body :description)
                                            (contains? body :description_text)
                                            (contains? body :description-text)
                                            (contains? body :descriptionText))
                    description (or (:description body)
                                  (:description_text body)
                                  (:description-text body)
                                  (:descriptionText body))
                    updates (cond-> {}
                              name-provided? (assoc :name name)
                              description-provided? (assoc :description description))
                    updated (some-> ((:update! categories/service) db category-id updates)
                              to-app)]
                (if updated
                  (h/json-response {:data updated})
                  (h/not-found-response "Category not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating category" {:error (ex-message e)
                                                                :data (ex-data e)
                                                                :category-id (str category-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update category" {:message (.getMessage e)
                                                          :category-id (str category-id)})
                (h/json-response {:error "Failed to update category"} 500)))))))))

(defn batch-delete-categories-handler
  "Batch delete categories (admin/owner only).

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
                          (:category_ids body)
                          (:category-ids body)
                          (:categoryIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No category ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more category ids are invalid"} 400)

              :else
              (let [delete! (:delete! categories/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [category-id ids]
                  (try
                    (if (boolean (delete! db category-id))
                      (swap! deleted-ids conj (str category-id))
                      (swap! errors conj {:id (str category-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str category-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str category-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete categories"} 500)))))))
