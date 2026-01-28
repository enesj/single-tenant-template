(ns app.domain.backend.expenses.handlers.user-articles
  "User-facing articles endpoints.

  These endpoints are mounted under /api/v1/expenses/articles and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
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
            (h/json-response {:data rows :limit limit :offset offset}))
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
            (h/json-response {:data article} 201))
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
                    manufacturer-id-provided? (or (contains? body :manufacturer_id)
                                                (contains? body :manufacturer-id)
                                                (contains? body :manufacturerId))
                    manufacturer-id-raw (when manufacturer-id-provided?
                                          (or (:manufacturer_id body)
                                            (:manufacturer-id body)
                                            (:manufacturerId body)))
                    manufacturer-id (when manufacturer-id-provided?
                                      (let [v (some-> manufacturer-id-raw str str/trim)]
                                        (when-not (str/blank? v)
                                          (h/try-parse-uuid v))))]

                (when (and manufacturer-id-provided?
                        (some? manufacturer-id-raw)
                        (not (str/blank? (some-> manufacturer-id-raw str str/trim)))
                        (nil? manufacturer-id))
                  (throw (ex-info "Invalid manufacturer id" {:status 400
                                                             :manufacturer-id manufacturer-id-raw
                                                             :article-id article-id})))

                (let [updates (cond-> {}
                                canonical-provided?
                                (assoc :canonical_name (:canonical_name body))

                                (contains? body :category)
                                (assoc :category (:category body))

                                (contains? body :link)
                                (assoc :link (:link body))

                                manufacturer-id-provided?
                                (assoc :manufacturer_id manufacturer-id)

                                (contains? body :manufacturer)
                                (assoc :manufacturer (:manufacturer body)))
                      updated (articles/update-article! db article-id updates)]
                  (if updated
                    (h/json-response {:data (to-app updated)})
                    (h/not-found-response "Article not found"))))
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
                  (h/json-response {:data {:deleted true}})
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

(defn list-unmapped-aliases-handler
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
                rows (to-app (aliases/list-unmapped-aliases db (cond-> {:limit limit :offset offset}
                                                                 supplier-id (assoc :supplier-id supplier-id))))]
            (h/json-response {:data rows :limit limit :offset offset}))
          (catch Exception e
            (log/error e "Failed to list unmapped aliases" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list unmapped aliases"} 500)))))))

(defn map-alias-to-article-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [alias-id (h/try-parse-uuid (or (get-in request [:path-params :alias-id])
                                             (get-in request [:parameters :path :alias-id])))
                body (h/read-body-params request)
                article-id (h/try-parse-uuid (or (:article-id body) (:article_id body)))]
            (when-not alias-id
              (throw (ex-info "Invalid alias-id" {:status 400})))
            (when-not article-id
              (throw (ex-info "Invalid article-id" {:status 400})))

            (let [updated (aliases/map-alias-to-article! db alias-id article-id)]
              (h/json-response {:data (to-app updated)})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (h/json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to map alias" {:message (.getMessage e)})
            (h/json-response {:error "Failed to map alias"} 500)))))))

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
                allow-reassign? (boolean (or (:allow-reassign? body) (:allow_reassign? body)))]
            (when-not article-id
              (throw (ex-info "Invalid article id" {:status 400})))
            (when-not supplier-id
              (throw (ex-info "Invalid supplier-id" {:status 400})))

            (let [result (articles/batch-create-aliases!
                           db
                           {:supplier-id supplier-id
                            :article-id article-id
                            :raw-labels raw-labels
                            :allow-reassign? allow-reassign?})]
              (h/json-response {:data (to-app result)})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (h/json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to batch-create aliases" {:message (.getMessage e)})
            (h/json-response {:error "Failed to batch-create aliases"} 500)))))))
