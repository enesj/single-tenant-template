(ns app.domain.expenses.services.suppliers
  "Supplier CRUD services using factory pattern."
  (:require
   [app.domain.expenses.services.service-configs :as configs]
   [app.domain.expenses.services.services-factory :as factory]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :supplier))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def list-suppliers (:list service))
(def get-supplier (:get service))
(def create-supplier! (:create! service))
(def update-supplier! (:update! service))
(def delete-supplier! (:delete! service))
(def count-suppliers (:count service))
(def search-suppliers (:search service))

;; ============================================================================
;; Normalization (re-exported for external use)
;; ============================================================================

(def normalize-supplier-key configs/normalize-supplier-key)

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn find-by-normalized-key
  "Find supplier by normalized key for deduplication."
  [db normalized-key]
  (when normalized-key
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:suppliers]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-or-create-supplier!
  "Find supplier by normalized name or create new one.
   Returns {:existing? bool :supplier {...}}"
  [db display-name & [{:keys [address tax_id]}]]
  (let [normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key db normalized)]
      {:existing? true :supplier existing}
      {:existing? false
       :supplier (create-supplier! db {:display_name display-name
                                       :address address
                                       :tax_id tax_id})})))

(defn search-suppliers-autocomplete
  "Search suppliers for autocomplete with fuzzy matching."
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))
    (let [search-pattern (str "%" query "%")]
      (jdbc/execute!
        db
        (sql/format {:select [:*]
                     :from [:suppliers]
                     :where [:or
                             [:ilike :display_name search-pattern]
                             [:ilike :normalized_key search-pattern]]
                     :order-by [[:display_name :asc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))
