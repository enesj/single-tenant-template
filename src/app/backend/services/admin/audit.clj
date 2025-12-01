(ns app.backend.services.admin.audit
  "Admin audit logging and compliance tracking.

   This namespace handles:
   - Audit log creation with proper change tracking
   - Audit log retrieval with filtering capabilities
   - Compliance and security audit trails"
  (:require
    [app.shared.adapters.database :as db-adapter]
    [app.shared.query-builders :as qb]
    [app.shared.type-conversion :as tc]
    [cheshire.core :as json]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Audit Logging
;; ============================================================================

(def ^:private audit-config
  "Configuration for normalizing audit database results to application format"
  {:prefixes ["audit-" "log-"]
   :namespaces #{"audit" "logs" "al"}
   ;; In the simplified audit_logs schema we track actor/target IDs
   :id-fields #{:id :actor-id :target-id}})

(defn db-audit-log->app
  "Normalize a raw database audit log row using shared utilities.

   The underlying table uses the simplified schema:
   - actor_type / actor_id
   - target_type / target_id
   - metadata (JSONB)
   We normalize these to kebab-case keys and expose a few
   convenience aliases (e.g. :changes, :ip-address)."
  [log]
  (when log
    (let [base (-> log
                 db-adapter/convert-pg-objects
                 (db-adapter/normalize-admin-result audit-config))
          ;; Promote metadata and IP to more descriptive keys for consumers
          base-with-aliases (cond-> base
                              (contains? base :metadata)
                              (assoc :changes (:metadata base))

                              (contains? base :ip)
                              (assoc :ip-address (:ip base))

                              (contains? base :id)
                              (assoc :audit-log-id (:id base)))
          ;; Normalize nested changes map if present
          converted (if (contains? base-with-aliases :changes)
                      (update base-with-aliases :changes
                        #(db-adapter/normalize-admin-result % audit-config))
                      base-with-aliases)]
      converted)))

(defn log-audit!
  "Log an admin or user action to the audit log.

   Accepts either :user_id (application user) or :admin_id (system admin).
   These are mapped to the simplified audit_logs schema fields
   (actor_type, actor_id, target_type, target_id, metadata).

   When :admin_id is provided, it is also embedded in :changes as
   :initiator for convenience."
  [db {:keys [user_id admin_id action entity-type entity-id changes ip-address user-agent]}]
  (try
    (let [actor-id (or admin_id user_id)
          actor-type (cond
                       admin_id "admin"
                       user_id "user"
                       :else nil)
          _ (when (nil? actor-id)
              (log/warn "log-audit! called without actor id" {:action action :entity-type entity-type}))
          ;; For admin-initiated actions, embed initiator info in changes
          changes-with-initiator (cond-> changes
                                   admin_id (assoc :initiator {:type "admin" :admin-id (str admin_id)}))
          ;; Convert any PG objects to JSON-friendly values before encoding
          safe-changes (when changes-with-initiator (db-adapter/convert-pg-objects changes-with-initiator))
          metadata-value (when safe-changes [:cast (json/generate-string safe-changes) :jsonb])
          ;; Ensure entity-type has a default value if nil
          safe-target-type (some-> (or entity-type "admin_action") str)
          ;; Cast actor_type to the enum type used by :audit_logs.actor_type
          actor-type-db (when actor-type
                          (tc/cast-for-database :audit-actor-type actor-type))]
      (when actor-id
        (jdbc/execute-one! db
          (hsql/format
            {:insert-into :audit_logs
             :values [{:id (UUID/randomUUID)
                       :actor_type actor-type-db
                       :actor_id actor-id
                       :action action
                       :target_type safe-target-type
                       :target_id entity-id
                       :metadata metadata-value
                       :ip ip-address
                       :user_agent user-agent
                       :created_at (time/instant)}]}))))
    (catch Exception e
      (log/error e "Failed to log audit entry"
        {:user_id user_id
         :admin_id admin_id
         :action action
         :entity-type entity-type
         :entity-id entity-id
         :error (.getMessage e)})
      (throw e))))

;; ============================================================================
;; Audit Log Retrieval
;; ============================================================================

(defn- resolve-tenant-name
  "Get tenant name by ID"
  [db tenant-id]
  (when tenant-id
    (try
      (let [sql-query (hsql/format
                        {:select [:t.name]
                         :from [[:tenants :t]]
                         :where [:= :t.id [:cast tenant-id :uuid]]})
            result (jdbc/execute-one! db sql-query)
            ;; Handle both namespaced keys (:tenants/name) and simple keys (:name)
            tenant-name (or (:tenants/name result) (:name result))]
        tenant-name)
      (catch Exception e
        (log/error "❌ AUDIT BACKEND: Error resolving tenant name for" tenant-id ":" (.getMessage e))
        nil))))

(defn- resolve-user-name
  "Get user full name by ID"
  [db user-id]
  (when user-id
    (try
      (let [sql-query (hsql/format
                        {:select [:u.full_name]
                         :from [[:users :u]]
                         :where [:= :u.id [:cast user-id :uuid]]})
            result (jdbc/execute-one! db sql-query)
            ;; Handle both namespaced keys (:users/full_name) and simple keys (:full_name)
            user-name (or (:users/full_name result) (:full_name result))]
        user-name)
      (catch Exception e
        (log/error "❌ AUDIT BACKEND: Error resolving user name for" user-id ":" (.getMessage e))
        nil))))

(defn- resolve-admin-name
  "Get admin name by ID"
  [db admin-id]
  (when admin-id
    (try
      (let [sql-query (hsql/format
                        {:select [:a.full_name]
                         :from [[:admins :a]]
                         :where [:= :a.id [:cast admin-id :uuid]]})
            result (jdbc/execute-one! db sql-query)
            ;; Handle both namespaced keys (:admins/full_name) and simple keys (:full_name)
            admin-name (or (:admins/full_name result) (:full_name result))]
        admin-name)
      (catch Exception e
        (log/error "❌ AUDIT BACKEND: Error resolving admin name for" admin-id ":" (.getMessage e))
        nil))))

(defn- resolve-entity-name
  "Get entity name by type and ID"
  [db entity-type entity-id]
  (when (and entity-type entity-id)
    (case entity-type
      "tenant" (resolve-tenant-name db entity-id)
      "user" (resolve-user-name db entity-id)
      "users" (resolve-user-name db entity-id)              ; Handle plural 'users' entity type
      "admin" (resolve-admin-name db entity-id)
      ;; Add more entity types as needed
      (do
        (log/warn "🔍 AUDIT BACKEND: Unknown entity type:" entity-type)
        nil))))

(defn get-audit-logs
  "Get audit logs with optional filters.

   Supported filters map keys:
   - :admin-id    UUID of admin actor (filters actor_type = admin)
   - :entity-type target entity type string (for example, user)
   - :entity-id   UUID of target entity
   - :action      exact action string
   - :from-date   start timestamp (inclusive)
   - :to-date     end timestamp (inclusive)
   - :limit       pagination limit
   - :offset      pagination offset"
  [db {:keys [admin-id entity-type entity-id action limit offset from-date to-date]}]
  (let [build-query (fn []
                      (let [join-clause [[:admins :a] [:= :al.actor_id :a.id]]
                            filters-map (cond-> {}
                                          admin-id (assoc :actor-id {:type :equal :value admin-id :table-alias :al}
                                                     :actor-type {:type :equal :value "admin" :table-alias :al})
                                          entity-type (assoc :target-type {:type :equal :value entity-type :table-alias :al})
                                          entity-id (assoc :target-id {:type :equal :value entity-id :table-alias :al})
                                          action (assoc :action {:type :equal :value action :table-alias :al})
                                          (or from-date to-date) (assoc :created-at {:type :date-range
                                                                                     :value {:from from-date :to to-date}
                                                                                     :table-alias :al}))
                            options {:filters filters-map
                                     :sort {:by :created-at :order :desc :table-alias :al}
                                     :pagination {:limit limit :offset offset}}
                            base-query {:select [:al.*
                                                 [:a.email :admin_email]
                                                 [:a.full_name :admin_name]]
                                        :from [[:audit_logs :al]]
                                        :left-join join-clause}]
                        (log/info "📋 AUDIT SERVICE: Query built successfully")
                        (qb/compose-admin-query base-query options)))
        execute-query (fn [sql-map]
                        (try
                          (let [raw-logs (->> sql-map
                                           hsql/format
                                           (jdbc/execute! db))]
                            (log/info "📊 AUDIT SERVICE: Query executed successfully, found" (count raw-logs) "logs")
                            ;; Resolve names for each audit log using normalized data
                            (mapv (fn [log]
                                    (try
                                      (let [normalized (db-audit-log->app log)
                                            entity-type-str (some-> (:target-type normalized) str)
                                            entity-id* (:target-id normalized)
                                            entity-name (when (and entity-type-str entity-id*)
                                                          (resolve-entity-name db entity-type-str entity-id*))]
                                        (cond-> normalized
                                          entity-name (assoc :entity-name entity-name)))
                                      (catch Exception e
                                        (log/error "❌ AUDIT BACKEND: Error processing log:" (.getMessage e))
                                        ;; Return the log without computed fields
                                        (db-audit-log->app log))))
                              raw-logs))
                          (catch Exception e
                            (log/error "❌ AUDIT SERVICE: Error executing query:" (.getMessage e))
                            (throw e))))]
    (execute-query (build-query))))
