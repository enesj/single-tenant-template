(ns app.admin.backend.services.admin.users
  "User management service entry; centralize user CRUD."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.users.management :as management]
    [app.admin.backend.services.admin.users.validation :as validation]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.shared.adapters.database :as shared-db]
    [app.shared.adapters.normalization :as norm]
    [app.shared.query-builders :as shared-qb]
    [app.shared.type-conversion :as tc]
    [app.template.backend.utils.adapters.persistence :as persist]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

(def ^:private user-config validation/user-normalization-config)

(defn- db-user->app
  "Normalize a user row from the database using shared utilities"
  [user]
  (some-> (norm/normalize-admin-result user user-config)
    email-privacy/identity-user-view))

(defn- normalize-user-result
  "Normalization fn for admin user queries (handles single row or row collections)."
  [raw]
  (let [normalized (-> raw
                     shared-db/convert-pg-objects
                     (norm/normalize-admin-result user-config))]
    (if (sequential? normalized)
      (mapv email-privacy/identity-user-view normalized)
      (some-> normalized email-privacy/identity-user-view))))

(def ^:private latest-successful-user-login-query
  {:select [[:le.principal_id :principal_id]
            [[:max :le.created_at] :last_login_at]]
   :from [[:login_events :le]]
   :where [:and
           [:= :le.success true]
           [:= :le.principal_type (tc/cast-for-database :login-principal-type "user")]]
   :group-by [:le.principal_id]})

(def ^:private effective-last-login-at-expr
  [:raw "COALESCE(u.last_login_at, ul.last_login_at)"])

(def ^:private user-list-select-columns
  [[:u.id :id]
   [:u.email_ciphertext :email_ciphertext]
   [:u.email_lookup_hash :email_lookup_hash]
   [:u.email_key_version :email_key_version]
   [:u.full_name :full_name]
   [:u.password_hash :password_hash]
   [:u.status :status]
   [:u.email_verified :email_verified]
   [:u.avatar_url :avatar_url]
   [:u.auth_provider :auth_provider]
   [:u.created_at :created_at]
   [:u.updated_at :updated_at]
   [effective-last-login-at-expr :last_login_at]])

(defn- build-user-list-filter-clauses
  [{:keys [search status full-name email-verified
           created-at-from created-at-to
           updated-at-from updated-at-to
           last-login-at-from last-login-at-to]}]
  (cond-> []
    search (conj [:or
                  [:ilike :u/full_name (str "%" search "%")]
                  [:ilike [:cast :u/id :text] (str "%" search "%")]])
    status (conj [:= :u/status (tc/cast-for-database :user-status status)])
    full-name (conj [:ilike :u/full_name (str "%" full-name "%")])
    (some? email-verified) (conj [:= :u/email_verified email-verified])
    created-at-from (conj [:>= :u/created_at created-at-from])
    created-at-to (conj [:<= :u/created_at created-at-to])
    updated-at-from (conj [:>= :u/updated_at updated-at-from])
    updated-at-to (conj [:<= :u/updated_at updated-at-to])
    last-login-at-from (conj [:>= effective-last-login-at-expr last-login-at-from])
    last-login-at-to (conj [:<= effective-last-login-at-expr last-login-at-to])))

(defn- build-user-list-where-clause
  [filters]
  (let [clauses (build-user-list-filter-clauses filters)]
    (when (seq clauses)
      (into [:and] clauses))))

(def ^:private allowed-user-order-by
  {:created-at :u/created_at
   :updated-at :u/updated_at
   :user-ref :u/id
   :full-name :u/full_name
   :status :u/status
   :email-verified :u/email_verified
   :last-login-at effective-last-login-at-expr})

(defn- build-users-list-query
  [{:keys [limit offset sorts order-by order-dir] :as filters}]
  (let [where-clause (build-user-list-where-clause filters)
        order-clauses (shared-qb/resolve-order-by-clauses
                        {:sorts sorts
                         :order-by order-by
                         :order-dir order-dir
                         :allowed-order-by allowed-user-order-by
                         :default-order-by :u/created_at
                         :default-order-dir :desc
                         :tie-breaker [:u.id :asc]})]
    (cond-> {:select user-list-select-columns
             :from [[:users :u]]
             :left-join [[latest-successful-user-login-query :ul]
                         [:= :ul.principal_id :u.id]]
             :order-by order-clauses}
      where-clause (assoc :where where-clause)
      limit (assoc :limit limit)
      offset (assoc :offset offset))))

;; ============================================================================
;; User Listing and Search (Core API)
;; ============================================================================

(defn list-all-users
  "List all users (single-tenant)"
  [db opts]
  (persist/execute-admin-query db (build-users-list-query opts) normalize-user-result
    {:audit-context {:action "list-users"}}))

(defn count-all-users
  "Count all users using the same filters as `list-all-users`."
  [db opts]
  (let [where-clause (build-user-list-where-clause opts)
        query (cond-> {:select [[[:count :*] :total]]
                       :from [[:users :u]]
                       :left-join [[latest-successful-user-login-query :ul]
                                   [:= :ul.principal_id :u.id]]}
                where-clause (assoc :where where-clause))
        row (jdbc/execute-one! db (hsql/format query))
        total (or (:total row) (some-> row vals first) 0)]
    (long total)))

(defn list-all-users-page
  "List users and include pagination metadata for server-backed pagination."
  [db {:keys [limit offset] :as opts}]
  (let [users (list-all-users db opts)
        total (count-all-users db opts)]
    {:users users
     :total total
     :limit limit
     :offset offset}))

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
