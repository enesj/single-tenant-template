(ns app.domain.backend.expenses.services.payers
  "Payer CRUD services using factory pattern.
   Payers represent payment methods: cash, cards, bank accounts, or people."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
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
;; NOTE: We wrap the factory create/update fns to enforce that there is at most
;; one default payer at any time.
(declare set-default-payer-in-tx!)

(def ^:private create-payer!* (:create! service))
(def ^:private update-payer!* (:update! service))

(def create-payer!
  (fn [db payer-data]
    (let [want-default? (true? (:is_default payer-data))]
      (if want-default?
        (jdbc/with-transaction [tx db]
          (let [payer (create-payer!* tx (assoc payer-data :is_default false))]
            (set-default-payer-in-tx! tx (:id payer))))
        (create-payer!* db payer-data)))))
(def update-payer!
  (fn [db payer-id updates]
    (let [want-default? (true? (:is_default updates))]
      (if want-default?
        (jdbc/with-transaction [tx db]
          (when-let [_payer (update-payer!* tx payer-id (assoc (dissoc updates :is_default) :is_default false))]
            (set-default-payer-in-tx! tx payer-id)))
        (update-payer!* db payer-id updates)))))
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

(defn- set-default-payer-in-tx!
  "Internal helper: clear any existing default payer, then set the given payer-id
  as default. Must be called inside a transaction."
  [tx payer-id]
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
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn set-default-payer!
  "Set a payer as the default (unsets any previous default)."
  [db payer-id]
  (jdbc/with-transaction [tx db]
    (set-default-payer-in-tx! tx payer-id)))
