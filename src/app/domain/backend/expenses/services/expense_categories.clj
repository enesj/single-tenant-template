(ns app.domain.backend.expenses.services.expense-categories
  "Expense category CRUD services with single-default enforcement."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :expense-category))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(declare create-expense-category!
  update-expense-category!
  delete-expense-category!
  set-default-expense-category-in-tx!)

(def ^:private base-service
  (factory/build-entity-service config))

(def ^:private create-expense-category!*
  (:create! base-service))

(def ^:private update-expense-category!*
  (:update! base-service))

(def ^:private delete-expense-category!*
  (:delete! base-service))

(defn- expense-category-tenant-id
  [db expense-category-id]
  (:tenant_id
   (jdbc/execute-one!
     db
     (sql/format {:select [:tenant_id]
                  :from [:expense_categories]
                  :where [:= :id expense-category-id]
                  :limit 1})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn- expense-category-record
  [db expense-category-id tenant-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:id :tenant_id :name :is_default]
                 :from [:expense_categories]
                 :where (if tenant-id
                          [:and
                           [:= :id expense-category-id]
                           [:= :tenant_id tenant-id]]
                          [:= :id expense-category-id])
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-default-expense-category
  "Return the current default expense category for a tenant, if one exists."
  ([db]
   (get-default-expense-category db nil))
  ([db tenant-id]
   (let [where (if tenant-id
                 [:and [:= :tenant_id tenant-id]
                  [:= :is_default true]]
                 [:= :is_default true])]
     (jdbc/execute-one!
       db
       (sql/format {:select [:*]
                    :from [:expense_categories]
                    :where where
                    :limit 1})
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- set-default-expense-category-in-tx!
  [tx expense-category-id tenant-id]
  (when-not tenant-id
    (throw (ex-info "tenant-id is required to set a default expense category"
             {:expense-category-id expense-category-id})))
  (jdbc/execute!
    tx
    (sql/format {:update :expense_categories
                 :set {:is_default false}
                 :where [:and
                         [:= :tenant_id tenant-id]
                         [:= :is_default true]]}))
  (jdbc/execute-one!
    tx
    (sql/format {:update :expense_categories
                 :set {:is_default true}
                 :where [:and
                         [:= :id expense-category-id]
                         [:= :tenant_id tenant-id]]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- reject-default-unset!
  [db expense-category-id tenant-id]
  (when-let [expense-category (expense-category-record db expense-category-id tenant-id)]
    (when (:is_default expense-category)
      (throw (ex-info "Default expense category cannot be unset directly. Choose another default first."
               {:status 400
                :expense-category-id expense-category-id
                :tenant-id tenant-id})))))

(defn- reject-default-delete!
  [db expense-category-id tenant-id]
  (when-let [expense-category (expense-category-record db expense-category-id tenant-id)]
    (when (:is_default expense-category)
      (throw (ex-info "Default expense category cannot be deleted. Choose another default first."
               {:status 400
                :expense-category-id expense-category-id
                :tenant-id tenant-id})))))

(defn create-expense-category!
  ([db data]
   (create-expense-category! db data nil))
  ([db data opts]
   (let [want-default? (true? (:is_default data))
         tenant-id (or (:tenant-id opts) (:tenant_id data))]
     (if want-default?
       (jdbc/with-transaction [tx db]
         (let [expense-category (create-expense-category!* tx (assoc data :is_default false))]
           (set-default-expense-category-in-tx! tx (:id expense-category) tenant-id)))
       (create-expense-category!* db data)))))

(defn update-expense-category!
  ([db expense-category-id updates]
   (update-expense-category! db expense-category-id updates nil))
  ([db expense-category-id updates opts]
   (let [has-default? (contains? updates :is_default)
         want-default? (true? (:is_default updates))
         tenant-id (or (:tenant-id opts)
                     (:tenant_id updates)
                     (expense-category-tenant-id db expense-category-id))]
      (cond
        (and has-default? want-default?)
        (jdbc/with-transaction [tx db]
          (when-let [_expense-category (update-expense-category!* tx
                                         expense-category-id
                                         (assoc (dissoc updates :is_default) :is_default false)
                                         opts)]
            (set-default-expense-category-in-tx! tx expense-category-id tenant-id)))

        (and has-default? (false? (:is_default updates)))
        (do
          (reject-default-unset! db expense-category-id tenant-id)
          (update-expense-category!* db expense-category-id updates opts))

        :else
        (update-expense-category!* db expense-category-id updates opts)))))

(defn delete-expense-category!
  ([db expense-category-id]
   (delete-expense-category! db expense-category-id nil))
  ([db expense-category-id opts]
   (let [tenant-id (:tenant-id opts)]
     (reject-default-delete! db expense-category-id tenant-id)
     (delete-expense-category!* db expense-category-id opts))))

(def service
  (assoc base-service
    :create! create-expense-category!
    :update! update-expense-category!
    :delete! delete-expense-category!))
