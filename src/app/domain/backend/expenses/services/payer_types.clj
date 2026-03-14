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

(defn- assert-not-system-type!
  "Guard: throw if the payer type with given id has is_system = true."
  [db id]
  (let [row (jdbc/execute-one! db
              (sql/format {:select [:is_system]
                           :from   [:payer_types]
                           :where  [:= :id id]
                           :limit  1})
              {:builder-fn rs/as-unqualified-lower-maps})]
    (when (:is_system row)
      (throw (ex-info "Cannot modify a system payer type"
               {:type :validation-error :status 403
                :errors {:payer-type ["System payer types cannot be modified"]}})))))

(def create-payer-type!
  (fn
    ([db data] (create-payer-type! db data nil))
    ([db data opts]
     ;; Guard: cannot create with is_system = true via API
     (when (true? (:is_system data))
       (throw (ex-info "Cannot create a system payer type"
                {:type :validation-error :status 403
                 :errors {:payer-type ["System payer types are auto-provisioned"]}})))
     (let [want-default? (true? (:is_default data))
           tenant-id (or (:tenant-id opts) (:tenant_id data))]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (let [row (create-payer-type!* tx (assoc data :is_default false :is_system false))]
             (set-default-payer-type-in-tx! tx (:id row) tenant-id)))
         (create-payer-type!* db (assoc data :is_system false)))))))

(def update-payer-type!
  (fn
    ([db id updates] (update-payer-type! db id updates nil))
    ([db id updates opts]
     ;; Guard: cannot update a system payer type
     (assert-not-system-type! db id)
     (let [want-default? (true? (:is_default updates))
           tenant-id (:tenant-id opts)
           ;; Strip is_system from updates — it's immutable
           updates (dissoc updates :is_system)]
       (if want-default?
         (jdbc/with-transaction [tx db]
           (when-let [_row (update-payer-type!* tx id (assoc (dissoc updates :is_default) :is_default false) opts)]
             (set-default-payer-type-in-tx! tx id tenant-id)))
         (update-payer-type!* db id updates opts))))))

(def delete-payer-type!*  (:delete! service))

(defn delete-payer-type!
  "Delete a payer type. Guards against deleting system types."
  [db id & [opts]]
  (assert-not-system-type! db id)
  (delete-payer-type!* db id opts))

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

