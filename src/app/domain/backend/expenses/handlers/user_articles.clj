(ns app.domain.backend.expenses.handlers.user-articles
  "User-facing (non-admin) articles endpoints.

  These endpoints are mounted under /api/v1/expenses/articles and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Response shapes intentionally mirror the admin helpers used by the unmapped-items UX."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(def ^:private to-app db-adapter/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access this page."))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-articles-handler
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
                rows (to-app (articles/list-articles db {:limit limit
                                                         :offset offset
                                                         :search search}))]
            (h/json-response {:success true
                              :articles rows}))
          (catch Exception e
            (log/error e "Failed to list articles" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list articles"} 500)))))))

(defn create-article-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
              (let [body (h/read-body-params request)
                canonical-name (:canonical_name body)
                article (to-app (articles/create-article! db {:canonical_name canonical-name}))]
            (h/json-response {:success true
                              :article article} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating article" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create article" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create article"} 500)))))))

(defn update-article-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [article-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                             (get-in request [:parameters :path :id])))]
          (if-not article-id
            (h/json-response {:error "Invalid article id"} 400)
            (try
                (let [body (h/read-body-params request)
                  canonical-provided? (contains? body :canonical_name)
                    updates (cond-> {}
                              canonical-provided?
                    (assoc :canonical_name (:canonical_name body))

                              (contains? body :barcode)
                              (assoc :barcode (:barcode body))

                              (contains? body :category)
                              (assoc :category (:category body)))
                    updated (articles/update-article! db article-id updates)]
                (if updated
                  (h/json-response {:success true
                                    :article (to-app updated)})
                  (h/not-found-response "Article not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating article" {:error (ex-message e)
                                                               :data (ex-data e)
                                                               :article-id (str article-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update article" {:message (.getMessage e)
                                                         :article-id (str article-id)})
                (h/json-response {:error "Failed to update article"} 500)))))))))

(defn delete-article-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [article-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                             (get-in request [:parameters :path :id])))]
          (if-not article-id
            (h/json-response {:error "Invalid article id"} 400)
            (try
              (let [deleted? (boolean (articles/delete-article! db article-id))]
                (if deleted?
                  (h/json-response {:success true})
                  (h/not-found-response "Article not found")))
              (catch org.postgresql.util.PSQLException e
                (let [sql-state (.getSQLState e)]
                  (if (= "23503" sql-state)
                    (do
                      (log/warn "Cannot delete article - has related records" {:article-id (str article-id)})
                      (h/json-response {:error "Cannot delete article: it has related expense items, aliases, or price observations. Remove related records first."} 409))
                    (do
                      (log/error e "Database error deleting article" {:article-id (str article-id)
                                                                      :sql-state sql-state})
                      (h/json-response {:error "Failed to delete article"} 500)))))
              (catch Exception e
                (log/error e "Failed to delete article" {:message (.getMessage e)
                                                         :article-id (str article-id)})
                (h/json-response {:error "Failed to delete article"} 500)))))))))

(defn list-unmapped-items-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                supplier-id (h/try-parse-uuid (h/get-param qp :supplier_id))
                limit (or (some-> (h/get-param qp :limit) parse-long) 50)
                offset (or (some-> (h/get-param qp :offset) parse-long) 0)
                rows (to-app (articles/list-unmapped-items db (cond-> {:limit limit :offset offset}
                                                                supplier-id (assoc :supplier-id supplier-id))))]
            (h/json-response {:success true
                              :unmapped-items rows}))
          (catch Exception e
            (log/error e "Failed to list unmapped items" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list unmapped items"} 500)))))))

(defn map-item-to-article-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [item-id (h/try-parse-uuid (or (get-in request [:path-params :item-id])
                                            (get-in request [:parameters :path :item-id])))
                body (h/read-body-params request)
                article-id (h/try-parse-uuid (or (:article-id body) (:article_id body)))
                create-alias? (boolean (or (:create-alias? body) (:create_alias? body)))
                allow-alias-reassign? (boolean (or (:allow-alias-reassign? body)
                                                 (:allow_alias_reassign? body)
                                                 (:allow-reassign? body)
                                                 (:allow_reassign? body)))]
            (when-not item-id
              (throw (ex-info "Invalid item-id" {:status 400})))
            (when-not article-id
              (throw (ex-info "Invalid article-id" {:status 400})))

            (let [result (articles/map-item-to-article!
                           db
                           item-id
                           article-id
                           {:create-alias? create-alias?
                            :allow-alias-reassign? allow-alias-reassign?})]
              (h/json-response {:success true
                                :expense-item (to-app (:expense-item result))
                                :alias-result (to-app (:alias-result result))})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (h/json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to map item" {:message (.getMessage e)})
            (h/json-response {:error "Failed to map item"} 500)))))))

(defn batch-create-aliases-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [article-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                               (get-in request [:parameters :path :id])))
                body (h/read-body-params request)
                supplier-id (h/try-parse-uuid (or (:supplier-id body) (:supplier_id body)))
                raw-labels (or (:raw-labels body)
                             (:raw_labels body)
                             (:raw-labels-text body)
                             (:raw_labels_text body))
                allow-reassign? (boolean (or (:allow-reassign? body) (:allow_reassign? body)))
                confidence (or (:confidence body) (:confidence_score body) 100)]
            (when-not article-id
              (throw (ex-info "Invalid article id" {:status 400})))
            (when-not supplier-id
              (throw (ex-info "Invalid supplier-id" {:status 400})))

            (let [result (articles/batch-create-aliases!
                           db
                           {:supplier-id supplier-id
                            :article-id article-id
                            :raw-labels raw-labels
                            :allow-reassign? allow-reassign?
                            :confidence confidence})]
              (h/json-response {:success true
                                :result (to-app result)})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (h/json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to batch-create aliases" {:message (.getMessage e)})
            (h/json-response {:error "Failed to batch-create aliases"} 500)))))))
