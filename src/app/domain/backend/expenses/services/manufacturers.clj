(ns app.domain.backend.expenses.services.manufacturers
  "Manufacturer CRUD services using the factory pattern.

  This is a canonical brand catalog used by articles via :manufacturer_id."
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
  (configs/get-entity-config :manufacturer))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))

;; ============================================================================
;; Normalization (re-exported for external use)
;; ============================================================================

;; ============================================================================
;; Related Records
;; ============================================================================

(defn- list-related-articles
  [db manufacturer-id limit]
  (jdbc/execute!
    db
    (sql/format {:select [[:a.id :id]
                          [:a.canonical_name :canonical_name]
                          [:a.normalized_key :normalized_key]
                          [:a.link :link]
                          [:a.created_at :created_at]
                          [:a.updated_at :updated_at]
                          [:s.name :subcategory_name]
                          [:c.name :category_name]]
                 :from [[:articles :a]]
                 :left-join [[:subcategories :s] [:= :s.id :a.subcategory_id]
                             [:categories :c] [:= :c.id :s.category_id]]
                 :where [:= :a.manufacturer_id manufacturer-id]
                 :order-by [[:a.canonical_name :asc]]
                 :limit limit})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-related-records
  "List records related to a manufacturer by type.

  Supported types: articles."
  [db manufacturer-id {:keys [type limit]}]
  (when-not manufacturer-id
    (throw (ex-info "manufacturer-id is required" {:status 400})))
  (let [related-type (rr/normalize-related-type type)
        related-limit (rr/clamp-related-limit limit)]
    (case related-type
      :articles (list-related-articles db manufacturer-id related-limit)
      (throw (ex-info
               "Invalid related type. Expected one of: articles."
               {:status 400 :type type})))))
