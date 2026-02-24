(ns app.domain.backend.expenses.services.subcategories
  "Subcategory CRUD services using the factory pattern.

  Subcategories belong to a category via `:category_id` and are referenced by
  articles via `:subcategory_id`."
  (:require
    [app.domain.backend.expenses.services.related-records :as rr]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :subcategory))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))

;; ============================================================================
;; Related Records
;; ============================================================================

(defn- list-related-articles
  [db subcategory-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:a.id :id]
                          [:a.canonical_name :canonical_name]
                          [:a.normalized_key :normalized_key]
                          [:a.link :link]
                          [:a.created_at :created_at]
                          [:a.updated_at :updated_at]
                          [:m.display_name :manufacturer_display_name]]
                 :from [[:articles :a]]
                 :left-join [[:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                 :where [:= :a.subcategory_id subcategory-id]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  "List records related to a subcategory by type.

  Supported types: articles."
  [db subcategory-id {:keys [type limit]}]
  (when-not subcategory-id
    (throw (ex-info "subcategory-id is required" {:status 400})))
  (let [related-type (rr/normalize-related-type type)
        related-limit (rr/clamp-related-limit limit)]
    (case related-type
      :articles (list-related-articles db subcategory-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: articles."
               {:status 400 :type type})))))
