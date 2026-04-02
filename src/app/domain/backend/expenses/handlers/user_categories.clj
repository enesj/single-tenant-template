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
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-categories-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (or (h/get-param qp :search)
                         (h/get-param qp :name))
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                description (h/get-param qp :description)
                created-at-from (h/parse-instant-param (h/get-param qp :created-at-from))
                created-at-to (h/parse-instant-param (h/get-param qp :created-at-to))
                extra-filters (cond-> []
                                created-at-from (conj [:>= :created_at created-at-from])
                                created-at-to (conj [:<= :created_at created-at-to]))
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir)
                       (some? description) (assoc :description description)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                rows (h/to-app ((:list categories/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                count-opts (cond-> (select-keys opts [:search :description])
                             (seq extra-filters) (assoc :extra-filters extra-filters))
                total (long (or ((:count categories/service) db count-opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list categories"} 500)))))))

(defn create-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
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
                category (h/to-app ((:create! categories/service) db payload))]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [category-id (h/parse-path-id request)]
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
                              h/to-app)]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                [raw-ids ids] (h/parse-batch-ids body [:ids :category_ids :category-ids :categoryIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No category ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more category ids are invalid"} 400)

              :else
              (let [delete! (:delete! categories/service)]
                (h/json-response {:data (h/batch-delete-entities #(delete! db %) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete categories"} 500)))))))
