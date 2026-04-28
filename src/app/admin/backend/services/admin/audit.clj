(ns app.admin.backend.services.admin.audit
  "Audit log service; append/query audit events here."
  (:require
    [app.shared.adapters.database :as shared-db]
    [app.shared.adapters.normalization :as norm]
    [app.shared.query-builders :as shared-qb]
    [app.shared.type-conversion :as tc]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.utils.query-builders :as qb]
    [cheshire.core :as json]
    [clojure.string :as str]
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
                 shared-db/convert-pg-objects
                 (norm/normalize-admin-result audit-config))
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
                        #(norm/normalize-admin-result % audit-config))
                      base-with-aliases)]
      converted)))

(defn- maybe-uuid
  "Coerce an ID value to a UUID.

   Accepts:
   - UUID values (passthrough)
   - UUID strings

   Returns nil when the value is nil/blank or not a valid UUID string.

   This keeps audit logging from blowing up when upstream code provides
   string IDs (common at HTTP/session boundaries)."
  [v]
  (cond
    (nil? v) nil
    (instance? UUID v) v
    (string? v) (when-not (str/blank? v)
                  (try
                    (UUID/fromString v)
                    (catch Exception _ nil)))
    :else (maybe-uuid (str v))))

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
          actor-id-uuid (maybe-uuid actor-id)
          target-id-uuid (maybe-uuid entity-id)
          actor-type (cond
                       admin_id "admin"
                       user_id "user"
                       :else nil)
          _ (when (nil? actor-id)
              (log/warn "log-audit! called without actor id" {:action action :entity-type entity-type}))
          _ (when (and actor-id (nil? actor-id-uuid))
              (log/warn "log-audit! received non-UUID actor id; skipping audit entry"
                {:action action
                 :entity-type entity-type
                 :actor-id actor-id}))
          ;; For admin-initiated actions, embed initiator info in changes
          changes-with-initiator (cond-> changes
                                   admin_id (assoc :initiator {:type "admin" :admin-id (str admin_id)}))
          ;; Convert any PG objects to JSON-friendly values before encoding
          safe-changes (when changes-with-initiator (shared-db/convert-pg-objects changes-with-initiator))
          metadata-value (when safe-changes [:cast (json/generate-string safe-changes) :jsonb])
          ;; Ensure entity-type has a default value if nil
          safe-target-type (some-> (or entity-type "admin_action") str)
          ;; Cast actor_type to the enum type used by :audit_logs.actor_type
          actor-type-db (when actor-type
                          (tc/cast-for-database :audit-actor-type actor-type))]
      (when actor-id-uuid
        (jdbc/execute-one! db
          (hsql/format
            {:insert-into :audit_logs
             :values [{:id (UUID/randomUUID)
                       :actor_type actor-type-db
                       :actor_id actor-id-uuid
                       :action action
                       :target_type safe-target-type
                       :target_id target-id-uuid
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
;; External API Failure Logging
;; ============================================================================

(def api-failure-action "external_api_failure")
(def api-failure-target-type "external_api")

(defn log-api-failure!
  "Log an external API call failure to the audit log.

   Records failures from outgoing HTTP calls (Mistral, LlamaParse, Cerebras,
   Google Places, Gmail, Postmark, OAuth, CBBH, Serper) as system-actor
   audit entries. Only call this on final failures (after retries exhausted).

   Options:
   - :api-name       keyword  — which API failed (e.g. :mistral-ocr, :google-places)
   - :operation      string   — what triggered the call (e.g. \"receipt-ocr\", \"exchange-rate-fetch\")
   - :http-status    int?     — HTTP status code if available
   - :error-message  string   — error description
   - :error-type     string?  — exception type or error category
   - :request-url    string?  — API endpoint (keys must be redacted)
   - :severity       keyword  — :warning, :error, or :critical
   - :duration-ms    int?     — how long the call took
   - :user-id        uuid?    — triggering user if known
   - :user-name      string?  — accepted for backwards compatibility, but not stored"
  [db {:keys [api-name operation http-status error-message error-type
              request-url severity duration-ms user-id user-name]}]
  (try
    (let [actor-id-uuid (maybe-uuid user-id)
          user-ref (when actor-id-uuid
                     (email-privacy/user-ref actor-id-uuid))
          metadata (cond-> {:api-name (some-> api-name name)
                            :operation operation
                            :http-status http-status
                            :error-message error-message
                            :error-type error-type
                            :request-url request-url
                            :retry-attempted true
                            :retry-succeeded false
                            :triggering-user-id (some-> user-id str)
                            :triggering-user-ref user-ref
                            :severity (some-> (or severity :error) name)
                            :duration-ms duration-ms}
                     (some? user-name)
                     (assoc :triggering-user-name-redacted? true))
          safe-metadata (shared-db/convert-pg-objects metadata)
          metadata-value [:cast (json/generate-string safe-metadata) :jsonb]
          actor-type-db (tc/cast-for-database :audit-actor-type "system")]
      (jdbc/execute-one! db
        (hsql/format
          {:insert-into :audit_logs
           :values [{:id (UUID/randomUUID)
                     :actor_type actor-type-db
                     :actor_id actor-id-uuid
                     :action api-failure-action
                     :target_type api-failure-target-type
                     :target_id nil
                     :metadata metadata-value
                     :ip nil
                     :user_agent nil
                     :created_at (time/instant)}]}))
      (log/info "Logged API failure:" (name api-name) operation))
    (catch Exception e
      (log/error e "Failed to log API failure audit entry"
        {:api-name api-name
         :operation operation
         :error (.getMessage e)}))))

;; ============================================================================
;; API Failure Notification (badge count + acknowledge)
;; ============================================================================

(defn count-unacknowledged-api-failures
  "Count API failure audit entries created after the given timestamp.
   If `since` is nil, counts all API failure entries."
  [db since]
  (let [base-where [:= :action api-failure-action]
        where-clause (if since
                       [:and base-where [:> :created_at since]]
                       base-where)
        sql (hsql/format
              {:select [[[:count :*] :total]]
               :from [:audit_logs]
               :where where-clause})
        row (jdbc/execute-one! db sql)]
    (or (:total row) (some-> row vals first) 0)))

(defn get-admin-last-acknowledged
  "Get the last time an admin acknowledged API failure notifications.
   Returns a timestamp or nil."
  [db admin-id]
  (let [admin-uuid (maybe-uuid admin-id)]
    (when admin-uuid
      (let [sql (hsql/format
                  {:select [:created_at]
                   :from [:audit_logs]
                   :where [:and
                           [:= :actor_type (tc/cast-for-database :audit-actor-type "admin")]
                           [:= :actor_id admin-uuid]
                           [:= :action "acknowledge_api_failures"]]
                   :order-by [[:created_at :desc]]
                   :limit 1})
            row (jdbc/execute-one! db sql)]
        (or (:audit_logs/created_at row)
          (:created_at row))))))

(defn acknowledge-api-failures!
  "Record that an admin has acknowledged/dismissed the API failure badge.
   Creates an audit entry so we can track when they last dismissed."
  [db admin-id]
  (log-audit! db {:admin_id admin-id
                  :action "acknowledge_api_failures"
                  :entity-type "admin_action"
                  :changes {:acknowledged-at (str (time/instant))}}))

(defn get-unread-api-failure-count
  "Get the number of API failures an admin hasn't acknowledged yet."
  [db admin-id]
  (let [last-ack (get-admin-last-acknowledged db admin-id)]
    (count-unacknowledged-api-failures db last-ack)))

;; ============================================================================
;; Audit Log Retrieval
;; ============================================================================

(defn- resolve-tenant-name
  "Get tenant name by ID"
  [db tenant-id]
  (when tenant-id
    (try
      (let [sql-query (hsql/format
                        {:select [[:t.name :tenant_name]]
                         :from [[:tenants :t]]
                         :where [:= :t.id [:cast tenant-id :uuid]]})
            result (jdbc/execute-one! db sql-query)
            normalized (shared-db/to-app result)]
        (:tenant-name normalized))
      (catch Exception e
        (log/error "❌ AUDIT BACKEND: Error resolving tenant name for" tenant-id ":" (.getMessage e))
        nil))))

(defn- resolve-user-name
  "Return a pseudonymous user ref for routine audit displays."
  [_db user-id]
  (email-privacy/user-ref user-id))

(defn- resolve-admin-name
  "Return a pseudonymous admin ref for routine audit displays."
  [_db admin-id]
  (email-privacy/admin-ref admin-id))

(defn- resolve-entity-name
  "Get a routine-safe display label by entity type and ID."
  [db entity-type entity-id]
  (when (and entity-type entity-id)
    (case entity-type
      "tenant" (resolve-tenant-name db entity-id)
      "user" (resolve-user-name db entity-id)
      "users" (resolve-user-name db entity-id)
      "admin" (resolve-admin-name db entity-id)
      (do
        (log/warn "🔍 AUDIT BACKEND: Unknown entity type:" entity-type)
        nil))))

(def ^:private allowed-audit-order-by
  {:created-at :al/created_at
   :action :al/action
   :actor-type :al/actor_type
   :target-type :al/target_type
   :admin-ref :al/actor_id
   :admin-name :al/actor_id})

(defn- build-audit-filters-map
  [{:keys [admin-id entity-type entity-id action from-date to-date]}]
  (cond-> {}
    admin-id (assoc :actor-id {:type :equal :value admin-id :table-alias :al}
               :actor-type {:type :equal :value "admin" :table-alias :al})
    entity-type (assoc :target-type {:type :equal :value entity-type :table-alias :al})
    entity-id (assoc :target-id {:type :equal :value entity-id :table-alias :al})
    action (assoc :action {:type :equal :value action :table-alias :al})
    (or from-date to-date) (assoc :created-at {:type :date-range
                                               :value {:from from-date :to to-date}
                                               :table-alias :al})))

(defn- build-audit-list-query
  [{:keys [limit offset sorts order-by order-dir] :as opts}]
  (let [base-query {:select [:al.*]
                    :from [[:audit_logs :al]]}
        base-options {:filters (build-audit-filters-map opts)
                      :pagination {:limit limit :offset offset}}
        order-clauses (shared-qb/resolve-order-by-clauses
                        {:sorts sorts
                         :order-by order-by
                         :order-dir order-dir
                         :allowed-order-by allowed-audit-order-by
                         :default-order-by :al/created_at
                         :default-order-dir :desc
                         :tie-breaker [:al/id :asc]})]
    (-> (qb/compose-admin-query base-query base-options)
      (shared-qb/apply-order-bys order-clauses))))

(defn- build-audit-count-query
  [opts]
  (qb/compose-admin-query
    {:select [[[:count :*] :total]]
     :from [[:audit_logs :al]]}
    {:filters (build-audit-filters-map opts)}))

(defn count-audit-logs
  "Count audit logs using the same filters as `get-audit-logs`."
  [db opts]
  (let [sql (hsql/format (build-audit-count-query opts))
        row (jdbc/execute-one! db sql)
        total (or (:total row) (some-> row vals first) 0)]
    (long total)))

(defn- scrub-routine-audit-changes
  "Remove plaintext identity display names from routine audit metadata."
  [changes]
  (if (map? changes)
    (let [triggering-user-id (or (:triggering-user-id changes)
                               (:triggering_user_id changes))
          triggering-user-ref (or (:triggering-user-ref changes)
                                (:triggering_user_ref changes)
                                (email-privacy/user-ref triggering-user-id))]
      (cond-> (dissoc changes
                :triggering-user-name
                :triggering_user_name)
        triggering-user-ref (assoc :triggering-user-ref triggering-user-ref)))
    changes))

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
  [db opts]
  (let [sql-map (build-audit-list-query opts)]
    (try
      (let [raw-logs (->> sql-map
                       hsql/format
                       (jdbc/execute! db))]
        (log/info "📊 AUDIT SERVICE: Query executed successfully, found" (count raw-logs) "logs")
        (mapv (fn [log]
                (try
                  (let [normalized (-> log
                                     db-audit-log->app
                                     (update :changes scrub-routine-audit-changes)
                                     (dissoc :metadata))
                        actor-type-str (some-> (:actor-type normalized) str)
                        actor-id* (:actor-id normalized)
                        is-system? (= actor-type-str "system")
                        is-admin? (= actor-type-str "admin")
                        entity-type-str (some-> (:target-type normalized) str)
                        entity-id* (:target-id normalized)
                        entity-name (when (and (not is-system?)
                                            entity-type-str entity-id*)
                                      (resolve-entity-name db entity-type-str entity-id*))
                        admin-ref (when is-admin?
                                    (email-privacy/admin-ref actor-id*))]
                    (cond-> (dissoc normalized
                              :admin-email :admin-name :metadata
                              :actor-id :target-id)
                      entity-name (assoc :entity-name entity-name)
                      (and is-admin? admin-ref) (assoc :admin-ref admin-ref)
                      is-admin? (assoc :admin-name admin-ref)
                      is-system? (assoc :actor-display-name "System"
                                   :is-api-failure true)))
                  (catch Exception e
                    (log/error "❌ AUDIT BACKEND: Error processing log:" (.getMessage e))
                    (-> log
                      db-audit-log->app
                      (update :changes scrub-routine-audit-changes)
                      (dissoc :metadata :actor-id :target-id)))))
          raw-logs))
      (catch Exception e
        (log/error "❌ AUDIT SERVICE: Error executing query:" (.getMessage e))
        (throw e)))))

(defn get-audit-logs-page
  "Get audit logs with server-backed pagination metadata."
  [db {:keys [limit offset] :as opts}]
  (let [logs (get-audit-logs db opts)
        total (count-audit-logs db opts)]
    {:logs logs
     :total total
     :limit limit
     :offset offset}))
