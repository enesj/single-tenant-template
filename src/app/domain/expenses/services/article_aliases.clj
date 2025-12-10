(ns app.domain.expenses.services.article-aliases
  "Article alias management (admin)."
  (:require
    [app.domain.expenses.services.articles :as articles]
    [app.domain.expenses.services.service-configs :as configs]
    [app.domain.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :article-alias))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def list-article-aliases (:list service))
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