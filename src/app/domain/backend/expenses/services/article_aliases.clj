(ns app.domain.backend.expenses.services.article-aliases
  "Article alias management (admin)."
  (:require
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :article-alias))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def ^:private list-article-aliases-base (:list service))

(defn- try-uuid
  [v]
  (when v
    (try
      (cond
        (instance? UUID v) v
        :else (UUID/fromString (str v)))
      (catch Exception _ nil))))

(defn list-article-aliases
  "List article aliases.

  Supports optional filters (used by supplier detail views):
  - :supplier-id / :supplier_id
  - :article-id / :article_id

  Keeps the same base signature as the generated service (db, opts)."
  [db {:keys [limit offset order-by order-dir search supplier-id supplier_id article-id article_id]
       :or {limit 50 offset 0 order-dir :asc}
       :as opts}]
  (let [supplier-uuid (try-uuid (or supplier-id supplier_id))
        article-uuid (try-uuid (or article-id article_id))
        base-filters (cond-> (vec (or (:base-filters config) []))
                       supplier-uuid (conj [:= :aa/supplier_id supplier-uuid])
                       article-uuid (conj [:= :aa/article_id article-uuid]))
        config* (assoc config :base-filters base-filters)
        base-query (factory/build-query-with-filters
                    config*
                    {:limit limit
                     :offset offset
                     :order-by order-by
                     :order-dir order-dir})
        final-query (factory/apply-search-filter base-query (:search-fields config*) search)]
    ;; NOTE: When no filters are provided, fall back to the base behavior.
    ;; This preserves compatibility for callers that rely on the generated list.
    (if (or supplier-uuid article-uuid)
      (jdbc/execute! db (sql/format final-query) {:builder-fn rs/as-unqualified-lower-maps})
      (list-article-aliases-base db opts))))

(def get-article-alias (:get service))
(def update-article-alias! (:update! service))
(def delete-article-alias! (:delete! service))
(def count-article-aliases (:count service))
(def search-article-aliases (:search service))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn create-alias!
  "Create or upsert an article alias."
  [db {:keys [supplier_id raw_label article_id confidence]}]
  (articles/create-alias! db supplier_id raw_label article_id {:confidence confidence}))

(def count-aliases
  "Count total aliases, optionally with search filter."
  (:count service))

(def search-aliases
  "Search aliases for autocomplete."
  (:search service))