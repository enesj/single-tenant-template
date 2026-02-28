 (ns app.domain.backend.expenses.services.payer-types
   "Payer Types CRUD services using factory pattern."
   (:require
     [app.domain.backend.expenses.services.service-configs :as configs]
     [app.domain.backend.expenses.services.services-factory :as factory]
     [honey.sql :as sql]
     [next.jdbc :as jdbc]
     [next.jdbc.result-set :as rs]))

 ;; ============================================================================
 ;; Service Registration
 ;; ============================================================================

(def config (configs/get-entity-config :payer-type))

 ;; ============================================================================
 ;; Generated CRUD Operations
 ;; ============================================================================

(def service (factory/build-entity-service config))

 ;; Enforce single default
(declare set-default-payer-type-in-tx!)

(def ^:private create-payer-type!* (:create! service))
(def ^:private update-payer-type!* (:update! service))

(def create-payer-type!
  (fn
    ([db data] (create-payer-type! db data nil))
    ([db data opts]
     (let [want-default? (true? (:is_default data))
           tenant-id (or (:tenant-id opts) (:tenant_id data))]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (let [row (create-payer-type!* tx (assoc data :is_default false))]
             (set-default-payer-type-in-tx! tx (:id row) tenant-id)))
         (create-payer-type!* db data))))))

(def update-payer-type!
  (fn
    ([db id updates] (update-payer-type! db id updates nil))
    ([db id updates opts]
     (let [want-default? (true? (:is_default updates))
           tenant-id (:tenant-id opts)]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (when-let [_row (update-payer-type!* tx id (assoc (dissoc updates :is_default) :is_default false) opts)]
             (set-default-payer-type-in-tx! tx id tenant-id)))
         (update-payer-type!* db id updates opts))))))

(defn get-default-payer-type
  "Get the default payer type. Optional `tenant-id` scopes to a specific tenant."
  ([db] (get-default-payer-type db nil))
  ([db tenant-id]
   (let [where (if tenant-id
                 [:and [:= :is_default true] [:= :tenant_id tenant-id]]
                 [:= :is_default true])]
     (jdbc/execute-one!
       db
       (sql/format {:select [:*]
                    :from [:payer_types]
                    :where where
                    :limit 1})
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- set-default-payer-type-in-tx!
  "Clear existing default payer type within the same tenant, then set new default."
  [tx id tenant-id]
  (let [clear-where (if tenant-id
                      [:and [:= :is_default true] [:= :tenant_id tenant-id]]
                      [:= :is_default true])]
    (jdbc/execute!
      tx
      (sql/format {:update :payer_types
                   :set {:is_default false}
                   :where clear-where})))
  (jdbc/execute-one!
    tx
    (sql/format {:update :payer_types
                 :set {:is_default true}
                 :where [:= :id id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

