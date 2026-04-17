(ns app.domain.backend.expenses.services.payers
  "Payer CRUD services using factory pattern.
   Payers represent shared payment sources within a tenant."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.template.backend.security.email :as email-privacy]
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
;; intentionally provide wrappers (create/update/delete) below.
;; NOTE: We wrap the factory create/update/delete fns to enforce payer-specific
;; invariants such as default uniqueness and delete guards.
(declare set-default-payer-in-tx!
  payer-has-system-type?)

(def ^:private create-payer!* (:create! service))
(def ^:private update-payer!* (:update! service))
(def ^:private delete-payer!* (:delete! service))

(def ^:private list-payers* (:list service))
(def ^:private get-payer* (:get service))

(defn- resolve-payer-email [row]
  (let [email (email-privacy/resolve-email row)]
    (cond-> (dissoc row :user_email_ciphertext)
      email (assoc :user_email email))))

(defn list-payers
  "List payers with the linked user's email resolved from ciphertext."
  [db opts]
  (mapv resolve-payer-email (list-payers* db opts)))

(defn get-payer
  "Get a payer by id with the linked user's email resolved from ciphertext."
  ([db payer-id]
   (get-payer db payer-id nil))
  ([db payer-id opts]
   (some-> (get-payer* db payer-id opts) resolve-payer-email)))

(defn- system-payer-type?
  [value]
  (= "system" (some-> value str)))

(defn related-expense-count
  "Count expenses currently linked to the payer.

   Optional `tenant-id` scopes the count to the current tenant."
  ([db payer-id]
   (related-expense-count db payer-id nil))
  ([db payer-id tenant-id]
   (let [where-clause (if tenant-id
                        [:and
                         [:= :e/payer_id payer-id]
                         [:= :e/tenant_id tenant-id]]
                        [:= :e/payer_id payer-id])
         row (jdbc/execute-one! db
               (sql/format {:select [[[:count :*] :n]]
                            :from [[:expenses :e]]
                            :where where-clause})
               {:builder-fn rs/as-unqualified-lower-maps})]
     (long (or (:n row) 0)))))

(defn- payer-has-related-expenses?
  [db payer-id tenant-id]
  (pos? (related-expense-count db payer-id tenant-id)))

(def create-payer!
  (fn
    ([db payer-data] (create-payer! db payer-data nil))
    ([db payer-data opts]
     (when (system-payer-type? (:type payer-data))
       (throw (ex-info "Cannot create a system-provisioned payer"
                {:type :validation-error
                 :status 403
                 :errors {:type ["System payers can only be created by the application"]}})))
     ;; Force the type to "custom" for user-initiated creates; the enum cast
     ;; itself is applied by the payer-config :before-insert step in
     ;; service_configs/config_maps.clj, so passing a plain string here is safe.
     (let [payer-data (assoc payer-data :type "custom")
           want-default? (true? (:is_default payer-data))
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
           tenant-id (:tenant-id opts)
           active-change? (contains? updates :is_active)]
       (when (and active-change?
               (payer-has-system-type? db payer-id))
         (throw (ex-info "Cannot activate or deactivate a system-provisioned payer"
                  {:type :validation-error
                   :status 403
                   :errors {:payer ["System-provisioned payers cannot be activated or deactivated"]}})))
       (if want-default?
         (jdbc/with-transaction [tx db]
           (when-let [_payer (update-payer!* tx payer-id (assoc (dissoc updates :is_default) :is_default false) opts)]
             (set-default-payer-in-tx! tx payer-id tenant-id)))
         (update-payer!* db payer-id updates opts))))))

;; ============================================================================
;; System Type Guards
;; ============================================================================

(defn- payer-has-system-type?
  "Check if a payer is system-provisioned."
  [db payer-id]
  (let [row (jdbc/execute-one! db
              (sql/format {:select [:type]
                           :from [[:payers :p]]
                           :where [:= :p.id payer-id]
                           :limit 1})
              {:builder-fn rs/as-unqualified-lower-maps})]
    (system-payer-type? (:type row))))

(defn- assert-payer-not-system!
  "Guard: throw if the payer is system-provisioned."
  [db payer-id]
  (when (payer-has-system-type? db payer-id)
    (throw (ex-info "Cannot modify a system-provisioned payer"
             {:type :validation-error
              :status 403
              :errors {:payer ["System-provisioned payers cannot be deleted"]}}))))

(defn- assert-payer-without-related-expenses!
  [db payer-id tenant-id]
  (when (payer-has-related-expenses? db payer-id tenant-id)
    (throw (ex-info "Cannot delete a payer that is already used by expenses. Deactivate it instead."
             {:type :validation-error
              :status 409
              :errors {:payer ["Payers already used by expenses cannot be deleted"]
                       :action ["Deactivate the payer instead"]}}))))

(defn delete-payer!
  "Delete a payer.

   Guards against deleting:
   - system-type payers
   - payers already referenced by expenses"
  [db id & [opts]]
  (let [tenant-id (:tenant-id opts)]
    (assert-payer-not-system! db id)
    (assert-payer-without-related-expenses! db id tenant-id)
    (delete-payer!* db id opts)))

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

(defn get-user-payer-id
  "Look up the user's own payer ID from user_expense_settings for the given user + tenant.
   Returns the payer UUID or nil when no settings row exists."
  [db user-id tenant-id]
  (when (and user-id tenant-id)
    (:default_payer_id
     (jdbc/execute-one! db
       (sql/format {:select [:default_payer_id]
                    :from [:user_expense_settings]
                    :where [:and
                            [:= :user_id user-id]
                            [:= :tenant_id tenant-id]]
                    :limit 1})
       {:builder-fn rs/as-unqualified-lower-maps}))))
