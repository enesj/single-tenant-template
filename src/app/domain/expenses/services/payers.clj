(ns app.domain.expenses.services.payers
  "Payer CRUD services using factory pattern.
   Payers represent payment methods: cash, cards, bank accounts, or people."
  (:require
    [app.domain.expenses.services.service-configs :as configs]
    [app.domain.expenses.services.services-factory :as factory]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :payer))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def list-payers (:list service))
(def get-payer (:get service))
(def create-payer! (:create! service))
(def update-payer! (:update! service))
(def delete-payer! (:delete! service))
(def count-payers (:count service))
(def search-payers (:search service))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn list-payers-by-type
  "List payers filtered by type.
   
   Args:
     db - Database connection
     payer-type - One of: 'cash', 'card', 'account', 'person'
   
   Returns: Vector of payer maps"
  [db payer-type]
  (jdbc/execute!
    db
    (sql/format {:select [:*]
                 :from [:payers]
                 :where [:= :type [:cast payer-type :payer_type]]
                 :order-by [[:type :asc] [:label :asc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-default-payer
  "Get the default payer if one is set."
  [db]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:payers]
                 :where [:= :is_default true]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn set-default-payer!
  "Set a payer as the default (unsets any previous default)."
  [db payer-id]
  (jdbc/with-transaction [tx db]
    ;; Clear existing default
    (jdbc/execute!
      tx
      (sql/format {:update :payers
                   :set {:is_default false}
                   :where [:= :is_default true]}))
    ;; Set new default
    (jdbc/execute-one!
      tx
      (sql/format {:update :payers
                   :set {:is_default true
                         :updated_at [:now]}
                   :where [:= :id payer-id]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))
