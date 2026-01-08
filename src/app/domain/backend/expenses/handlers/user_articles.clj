(ns app.domain.backend.expenses.handlers.user-articles
  "User-facing (non-admin) articles endpoints.

  These endpoints are mounted under /api/v1/expenses/articles and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Response shapes intentionally mirror the admin helpers used by the unmapped-items UX."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [cheshire.core :as json]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(defn- try-parse-uuid
  [s]
  (when s
    (try
      (UUID/fromString (str s))
      (catch Exception _ nil))))

(defn- json-response
  ([body] (json-response body 200))
  ([body status]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string body)}))

(defn- to-app
  "Convert DB rows to API-friendly maps (kebab-case keys, JSON-safe values)."
  [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn- unauthorized-response
  ([] (unauthorized-response "Authentication required"))
  ([message]
   (json-response {:error message} 401)))

(defn- forbidden-response
  ([] (forbidden-response "Forbidden"))
  ([message]
   (json-response {:error message} 403)))

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defn- get-user
  [request]
  (or (get-in request [:session :auth-session :user])
    (get-in request [:session :user])))

(defn- get-user-role
  [request]
  (normalize-role
    (or (get-in request [:session :auth-session :user :role])
      (get-in request [:session :user :role]))))

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (let [role (get-user-role request)]
    (when-not (contains? allowed-roles role)
      (forbidden-response "Only admins and owners can access this page."))))

(defn- read-json-body
  [request]
  (or (:body-params request)
    (when-let [body (:body request)]
      (json/parse-string (slurp body) true))
    {}))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-articles-handler
  [db]
  (fn [request]
    (if-not (get-user request)
      (unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (or (some-> (or (get qp "limit") (:limit qp)) parse-long) 200)
                offset (or (some-> (or (get qp "offset") (:offset qp)) parse-long) 0)
                search (or (get qp "search") (:search qp))
                rows (to-app (articles/list-articles db {:limit limit
                                                         :offset offset
                                                         :search search}))]
            (json-response {:success true
                            :articles rows}))
          (catch Exception e
            (log/error e "Failed to list articles" {:message (.getMessage e)})
            (json-response {:error "Failed to list articles"} 500)))))))

(defn create-article-handler
  [db]
  (fn [request]
    (if-not (get-user request)
      (unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (read-json-body request)
                canonical-name (or (:canonical_name body) (:canonical-name body) (:canonicalName body))
                article (to-app (articles/create-article! db {:canonical_name canonical-name}))]
            (json-response {:success true
                            :article article} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating article" {:error (ex-message e) :data (ex-data e)})
            (json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create article" {:message (.getMessage e)})
            (json-response {:error "Failed to create article"} 500)))))))

(defn list-unmapped-items-handler
  [db]
  (fn [request]
    (if-not (get-user request)
      (unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                supplier-id (try-parse-uuid (or (get qp "supplier-id")
                                              (get qp "supplier_id")
                                              (:supplier-id qp)
                                              (:supplier_id qp)))
                limit (or (some-> (or (get qp "limit") (:limit qp)) parse-long) 50)
                offset (or (some-> (or (get qp "offset") (:offset qp)) parse-long) 0)
                rows (to-app (articles/list-unmapped-items db (cond-> {:limit limit :offset offset}
                                                                supplier-id (assoc :supplier-id supplier-id))))]
            (json-response {:success true
                            :unmapped-items rows}))
          (catch Exception e
            (log/error e "Failed to list unmapped items" {:message (.getMessage e)})
            (json-response {:error "Failed to list unmapped items"} 500)))))))

(defn map-item-to-article-handler
  [db]
  (fn [request]
    (if-not (get-user request)
      (unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [item-id (try-parse-uuid (or (get-in request [:path-params :item-id])
                                          (get-in request [:parameters :path :item-id])))
                body (read-json-body request)
                article-id (try-parse-uuid (or (:article-id body) (:article_id body)))
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
              (json-response {:success true
                              :expense-item (to-app (:expense-item result))
                              :alias-result (to-app (:alias-result result))})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to map item" {:message (.getMessage e)})
            (json-response {:error "Failed to map item"} 500)))))))

(defn batch-create-aliases-handler
  [db]
  (fn [request]
    (if-not (get-user request)
      (unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [article-id (try-parse-uuid (or (get-in request [:path-params :id])
                                             (get-in request [:parameters :path :id])))
                body (read-json-body request)
                supplier-id (try-parse-uuid (or (:supplier-id body) (:supplier_id body)))
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
              (json-response {:success true
                              :result (to-app result)})))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (json-response {:error (ex-message e)} (or status 400))))
          (catch Exception e
            (log/error e "Failed to batch-create aliases" {:message (.getMessage e)})
            (json-response {:error "Failed to batch-create aliases"} 500)))))))
