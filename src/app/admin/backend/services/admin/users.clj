(ns app.admin.backend.services.admin.users
  "User management service entry; centralize user CRUD."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.users.management :as management]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.shared.adapters.database :as shared-db]
    [app.shared.adapters.normalization :as norm]
    [app.template.backend.utils.adapters.persistence :as persist]
    [taoensso.timbre :as log]))

(def ^:private user-config
  "Configuration for normalizing user database results to application format"
  {:prefixes ["users-" "user-"]
   :namespaces #{"users" "user" "u"}
   :id-fields #{:id}})

(defn- db-user->app
  "Normalize a user row from the database using shared utilities"
  [user]
  (norm/normalize-admin-result user user-config))

(defn- normalize-user-result
  "Normalization fn for admin user queries (handles single row or row collections)."
  [raw]
  (-> raw
    shared-db/convert-pg-objects
    (norm/normalize-admin-result user-config)))

;; ============================================================================
;; User Listing and Search (Core API)
;; ============================================================================

(defn list-all-users
  "List all users (single-tenant)"
  [db {:keys [search status email-verified limit offset]}]
  (let [base-query {:select [:u.*]
                    :from [[:users :u]]
                    :order-by [[:created_at :desc]]}
        query (cond-> base-query
                search (update :where (fn [w]
                                        (let [clause [:or
                                                      [:ilike :u/email (str "%" search "%")]
                                                      [:ilike :u/full_name (str "%" search "%")]]]
                                          (if w [:and w clause] clause))))
                status (update :where (fn [w]
                                        (let [clause [:= :u/status status]]
                                          (if w [:and w clause] clause))))
                (some? email-verified) (update :where (fn [w]
                                                        (let [clause [:= :u/email_verified email-verified]]
                                                          (if w [:and w clause] clause))))
                limit (assoc-in [:limit] limit)
                offset (assoc-in [:offset] offset))]
    (persist/execute-admin-query db query normalize-user-result
      {:audit-context {:action "list-users"}})))

(defn search-users-advanced
  "Advanced user search with multiple criteria (single-tenant)"
  [db {:keys [search status email-verified role auth-provider
              created-after created-before last-login-after last-login-before
              limit offset sort-by sort-order]}]
  (let [order-by [(or sort-by :created_at) (or sort-order :desc)]
        base-query {:select [:u.*]
                    :from [[:users :u]]
                    :order-by [order-by]}
        date-range (fn [col from to]
                     (cond
                       (and from to) [:between col from to]
                       from [:= col from]
                       to [:= col to]
                       :else nil))
        query (cond-> base-query
                search (update :where (fn [w]
                                        (let [clause [:or
                                                      [:ilike :u/email (str "%" search "%")]
                                                      [:ilike :u/full_name (str "%" search "%")]]]
                                          (if w [:and w clause] clause))))
                status (update :where (fn [w]
                                        (let [clause [:= :u/status status]]
                                          (if w [:and w clause] clause))))
                (some? email-verified) (update :where (fn [w]
                                                        (let [clause [:= :u/email_verified email-verified]]
                                                          (if w [:and w clause] clause))))
                role (update :where (fn [w]
                                      (let [clause [:= :u/role role]]
                                        (if w [:and w clause] clause))))
                auth-provider (update :where (fn [w]
                                               (let [clause [:= :u/auth_provider auth-provider]]
                                                 (if w [:and w clause] clause))))
                (or created-after created-before) (update :where (fn [w]
                                                                   (let [clause (date-range :u/created_at created-after created-before)]
                                                                     (if w [:and w clause] clause))))
                (or last-login-after last-login-before) (update :where (fn [w]
                                                                         (let [clause (date-range :u/last_login_at last-login-after last-login-before)]
                                                                           (if w [:and w clause] clause))))
                limit (assoc :limit limit)
                offset (assoc :offset offset))]
    (persist/execute-admin-query db query normalize-user-result
      {:audit-context {:action "search-users-advanced"}})))

;; ============================================================================
;; User Details and Activity (Core API)
;; ============================================================================

(defn get-user-details
  "Get detailed user information"
  [db user-id]
  (let [query {:select [:u.*]
               :from [[:users :u]]
               :where [:= :u.id user-id]}]
    (persist/execute-admin-query db query normalize-user-result
      {:single? true
       :audit-context {:action "get-user-details" :entity-type "user" :entity-id user-id}})))

(defn get-user-activity
  "Get user activity and analytics for admin monitoring.

   Returns a map with keys:
   - :audit-logs    vector of audit log entries where the user is the target
   - :login-history vector of login_events for the user
   - :summary       aggregate stats {:total-actions :recent-logins :last-activity :last-login}"
  [db user-id {:keys [limit offset from-date to-date]
               :or {limit 50 offset 0}}]
  (let [;; Helper to normalize Java time instances to epoch millis for frontend
        ->millis (fn [v]
                   (cond
                     (instance? java.time.Instant v) (.toEpochMilli ^java.time.Instant v)
                     (instance? java.time.OffsetDateTime v) (.toInstant ^java.time.OffsetDateTime v)
                     :else v))
        ;; Fetch audit logs for this user as target entity
        audit-logs (try
                     (audit/get-audit-logs db
                       {:entity-type "user"
                        :entity-id user-id
                        :limit limit
                        :offset offset
                        :from-date from-date
                        :to-date to-date})
                     (catch Exception e
                       (log/error e "Failed to load user audit logs" {:user-id user-id})
                       []))
        ;; Fetch login history from login_events and normalize keys for frontend
        login-history (try
                        (let [rows (login-monitoring/get-login-history db :user user-id
                                     {:limit limit :offset offset})]
                          (mapv (fn [row]
                                  (let [converted (shared-db/to-app row)
                                        created-at (:created-at converted)]
                                    (cond-> converted
                                      created-at (assoc :created-at (->millis created-at))
                                      (:ip converted) (assoc :ip-address (:ip converted)))))
                            rows))
                        (catch Exception e
                          (log/error e "Failed to load user login history" {:user-id user-id})
                          []))
        ;; Derive summary metrics
        all-actions-count (count audit-logs)
        recent-login-count (count login-history)
        last-activity (or (some-> audit-logs first :created-at ->millis)
                        (some-> login-history first :created-at))
        last-login (some-> login-history first :created-at)]
    {:audit-logs audit-logs
     :login-history login-history
     :summary {:total-actions all-actions-count
               :recent-logins recent-login-count
               :last-activity last-activity
               :last-login last-login}}))

;; ============================================================================
;; Delegated Operations (Redirect to specialized modules)
;; ============================================================================

(defn update-user!
  [db user-id updates admin-id ip-address user-agent]
  (some-> (management/update-user! db user-id updates admin-id ip-address user-agent)
    db-user->app))
(defn create-user!
  [db user-data admin-id ip-address user-agent]
  (some-> (management/create-user! db user-data admin-id ip-address user-agent)
    db-user->app))
