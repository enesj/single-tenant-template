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
   (fn [db data]
     (let [want-default? (true? (:is_default data))]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (let [row (create-payer-type!* tx (assoc data :is_default false))]
             (set-default-payer-type-in-tx! tx (:id row))))
         (create-payer-type!* db data)))))

 (def update-payer-type!
   (fn [db id updates]
     (let [want-default? (true? (:is_default updates))]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (when-let [_row (update-payer-type!* tx id (assoc (dissoc updates :is_default) :is_default false))]
             (set-default-payer-type-in-tx! tx id)))
         (update-payer-type!* db id updates)))))

 (defn get-default-payer-type
   [db]
   (jdbc/execute-one!
     db
     (sql/format {:select [:*]
                  :from [:payer_types]
                  :where [:= :is_default true]
                  :limit 1})
     {:builder-fn rs/as-unqualified-lower-maps}))

 (defn- set-default-payer-type-in-tx!
   [tx id]
   (jdbc/execute!
     tx
     (sql/format {:update :payer_types
                  :set {:is_default false}
                  :where [:= :is_default true]}))
   (jdbc/execute-one!
     tx
     (sql/format {:update :payer_types
                  :set {:is_default true :updated_at [:now]}
                  :where [:= :id id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps}))

