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

;; NOTE: Avoid legacy alias vars like `list-payers`/`get-payer`/etc.
;; Route handlers resolve operations via the `service` map, except where we
;; intentionally provide wrappers (create/update) below.
;; NOTE: We wrap the factory create/update fns to enforce that there is at most
;; one default payer at any time.
(declare set-default-payer-in-tx!)

(def ^:private create-payer!* (:create! service))
(def ^:private update-payer!* (:update! service))

(def create-payer!
  (fn
    ([db payer-data] (create-payer! db payer-data nil))
    ([db payer-data opts]
     (let [want-default? (true? (:is_default payer-data))
           tenant-id (or (:tenant-id opts) (:tenant_id payer-data))]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (let [payer (create-payer!* tx (assoc payer-data :is_default false))]
             (set-default-payer-in-tx! tx (:id payer) tenant-id)))
         (create-payer!* db payer-data))))))

(def update-payer!
  (fn
    ([db payer-id updates] (update-payer! db payer-id updates nil))
    ([db payer-id updates opts]
     (let [want-default? (true? (:is_default updates))
           tenant-id (:tenant-id opts)]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (when-let [_payer (update-payer!* tx payer-id (assoc (dissoc updates :is_default) :is_default false) opts)]
             (set-default-payer-in-tx! tx payer-id tenant-id)))
         (update-payer!* db payer-id updates opts))))))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn get-default-payer
  "Get the default payer if one is set. Optional `tenant-id` scopes to a specific tenant."
  ([db] (get-default-payer db nil))
  ([db tenant-id]
   (let [where (if tenant-id
                 [:and [:= :is_default true] [:= :tenant_id tenant-id]]
                 [:= :is_default true])]
     (jdbc/execute-one!
       db
       (sql/format {:select [:*]
                    :from [:payers]
                    :where where
                    :limit 1})
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- set-default-payer-in-tx!
  "Internal helper: clear any existing default payer within the same tenant,
  then set the given payer-id as default. Must be called inside a transaction."
  [tx payer-id tenant-id]
  ;; Clear existing default (scoped by tenant when present)
  (let [clear-where (if tenant-id
                      [:and [:= :is_default true] [:= :tenant_id tenant-id]]
                      [:= :is_default true])]
    (jdbc/execute!
      tx
      (sql/format {:update :payers
                   :set {:is_default false}
                   :where clear-where})))
  ;; Set new default
  (jdbc/execute-one!
    tx
    (sql/format {:update :payers
                 :set {:is_default true}
                 :where [:= :id payer-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn set-default-payer!
  "Set a payer as the default (unsets any previous default).
   Optional `tenant-id` scopes the default to a specific tenant."
  ([db payer-id] (set-default-payer! db payer-id nil))
  ([db payer-id tenant-id]
   (jdbc/with-transaction [tx db]
     (set-default-payer-in-tx! tx payer-id tenant-id))))
