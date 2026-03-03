 (ns app.template.backend.middleware.user
   "Enhanced middleware for authenticating regular (non-admin) API requests and
   authorizing access to entities with comprehensive security controls."
   (:require
     [app.shared.http :as shared-http]
     [app.template.backend.security.entity-access :as entity-access]
     [app.template.backend.services.tenant :as tenant-svc]
     [honey.sql :as sql]
     [next.jdbc :as jdbc]
     [next.jdbc.result-set :as rs]
     [taoensso.timbre :as log]))

(defn- unauthorized
  ([] (unauthorized "Authentication required"))
  ([message]
   ;; Return a JSON *string* body because this middleware short-circuits the
   ;; downstream response encoding middleware.
   (shared-http/json-string-response 401 {:error message})))

(defn- forbidden
  ([] (forbidden "Access denied"))
  ([message]
   ;; Return a JSON *string* body because this middleware short-circuits the
   ;; downstream response encoding middleware.
   (shared-http/json-string-response 403 {:error message})))

(defn- clear-user-auth-session
  "Remove user auth keys from session while preserving other session state."
  [session]
  (let [existing-session (or session {})
        new-session (-> existing-session
                      (dissoc :auth-session :user :tenant :tenant-id :user-id))]
    (when (seq new-session) new-session)))

(defn- normalize-status
  [value]
  (when (some? value)
    (cond
      (keyword? value) (name value)
      :else (str value))))

(defn- ->uuid
  [value]
  (cond
    (instance? java.util.UUID value) value
    (string? value) (try
                      (java.util.UUID/fromString value)
                      (catch Exception _ nil))
    :else nil))

(defn- resolve-db
  "Best-effort DB connection for request-time session validation."
  [request]
  (or (get-in request [:service-container :db])
    (some-> (get-in request [:service-container :db-adapter]) :connection)))

(defn- fetch-user-status
  [db user-id]
  (when (and db user-id)
    (when-let [u-id (->uuid user-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [:status]
                                :from   [:users]
                                :where  [:= :id u-id]})
                   opts)]
        (normalize-status (:status row))))))

(defn- fetch-tenant-status
  [db tenant-id]
  (when (and db tenant-id)
    (when-let [t-id (->uuid tenant-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [:status]
                                :from   [:tenants]
                                :where  [:= :id t-id]})
                   opts)]
        (normalize-status (:status row))))))

(defn- fetch-owner-user-status
  [db tenant-id]
  (when (and db tenant-id)
    (when-let [t-id (->uuid tenant-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [[:u.status :owner_user_status]]
                                :from   [[:tenant_memberships :tm]]
                                :join   [[:users :u] [:= :tm.user_id :u.id]]
                                :where  [:and
                                         [:= :tm.tenant_id t-id]
                                         [:= :tm.status [:cast "active" :membership_status]]
                                         [:= :tm.role [:cast "owner" :membership_role]]]
                                :limit  1})
                   opts)]
        (normalize-status (:owner_user_status row))))))

(defn- validate-auth-session
  "Return nil when the auth session is valid, otherwise an error message."
  [db {:keys [user tenant]}]
  (let [user-id (->uuid (or (:id user) (:users/id user)))
        tenant-id (->uuid (or (:id tenant) (:tenants/id tenant)))
        user-status (fetch-user-status db user-id)]
    (cond
      (not= "active" user-status)
      "Account is not active"

      tenant-id
      (let [tenant-status (fetch-tenant-status db tenant-id)]
        (cond
          (not= "active" tenant-status)
          "Tenant is not active"

          (nil? (tenant-svc/get-membership db tenant-id user-id))
          "You are not an active member of this tenant"

          (not= "active" (fetch-owner-user-status db tenant-id))
          "Tenant owner account is not active"

          :else nil))

      :else
      nil)))

(defn wrap-user-authentication
  "Require a logged-in user session for protected API routes.

   Looks for a user map at [:session :auth-session :user]. If absent, returns 401.

   When a DB connection is available on the request, this middleware also validates:
   - users.status is active
   - if a tenant is present in session:
     - tenants.status is active
     - tenant_memberships.status is active for (tenant_id, user_id)
     - the tenant owner's users.status is active

   Notes:
   - Admin routes use a separate admin middleware; this does not apply there.
   - Keep this middleware focused on generic /api routes only."
  [handler]
  (fn [request]
    (let [user (or (get-in request [:session :auth-session :user])
                 (get-in request [:session :user]))]
      (if-not user
        (do
          (log/warn "❌ USER AUTH FAILED: No user in session" {:uri (:uri request)})
          (unauthorized))
        (let [db (resolve-db request)]
          (if-not db
            (handler request)
            (let [auth-session (or (get-in request [:session :auth-session])
                                 {:user   (get-in request [:session :user])
                                  :tenant (get-in request [:session :tenant])})
                  invalid-message (validate-auth-session db auth-session)]
              (if invalid-message
                (do
                  (log/warn "🚫 USER AUTH BLOCKED: Invalid session"
                    {:uri (:uri request)
                     :reason invalid-message
                     :user-id (or (:id user) (:users/id user))})
                  (-> (forbidden invalid-message)
                    (assoc :session (clear-user-auth-session (:session request)))))
                (handler request)))))))))

(defn- handle-users-entity-access
  "Handle special authorization rules for users entity."
  [request handler tenant-id method]
  (cond
    ;; Ensure we have a tenant in session
    (nil? tenant-id)
    (do (log/warn "❌ USER ACCESS DENIED: No tenant in session"
          {:uri (:uri request) :method method})
      (unauthorized))

    ;; All user operations must be scoped to current tenant
    :else
    (do
      (log/debug "🔒 Users entity access with tenant isolation"
        {:tenant-id tenant-id :method method})
      (handler request))))

(defn wrap-entities-authorization
  "Authorization guard for generic CRUD entities under /api/v1/entities.

   Security model:
   - Admin-only entities (:admins, :admin-sessions, :audit-logs): NEVER accessible via generic CRUD
   - Protected entities (:users): Allowed only with tenant context in session
   - Public entities: tenant-scoped reads are allowed; writes are role-gated
   - Unknown entities: blocked by default (deny-by-default)"
  [handler]
  (let [read-only-methods #{:get :head :options}
        write-method? (fn [m] (not (contains? read-only-methods m)))
        normalize-role (fn [role]
                         (when (some? role)
                           (cond
                             (keyword? role) (name role)
                             :else (str role))))
        membership-role (fn [request]
                          (normalize-role
                            (or (get-in request [:session :auth-session :membership :role])
                              (get-in request [:session :auth-session :membership-role])
                              (get-in request [:session :membership :role]))))
        allow-entity-write? (fn [entity-key role]
                              ;; Keep this intentionally conservative.
                              ;; - viewers are read-only
                              ;; - members can write only to their own operational entities
                              ;; - reference data and global catalogs are owner/admin only
                              (let [role* (or role "viewer")]
                                (cond
                                  (#{"owner" "admin"} role*) true
                                  (= "member" role*) (contains? #{:receipts} entity-key)
                                  (= "viewer" role*) false
                                  :else false)))]
    (fn [request]
      (let [entity-name (or (get-in request [:path-params :entity])
                          (get-in request [:parameters :path :entity]))
            entity-key (when entity-name (keyword entity-name))
            method (:request-method request)
            uri (:uri request)
            is-admin? (some? (:admin request))
            tenant-id (or (get-in request [:session :auth-session :tenant :id])
                        (get-in request [:session :auth-session :tenant :tenants/id])
                        (get-in request [:session :tenant :id])
                        (get-in request [:session :tenant-id]))
            user-id (or (get-in request [:session :auth-session :user :id])
                      (get-in request [:session :user :id]))
            role (membership-role request)]

        ;; Log the access attempt for security monitoring
        (entity-access/log-entity-access-attempt entity-key
          (entity-access/entity-allowed-for-generic-crud? entity-key is-admin?)
          {:method method :uri uri :tenant-id tenant-id :user-id user-id :role role})

        (cond
          ;; No entity specified - allow (shouldn't happen in normal routing)
          (nil? entity-key)
          (handler request)

          ;; Check if entity is allowed for generic CRUD access
          (not (entity-access/entity-allowed-for-generic-crud? entity-key is-admin?))
          (let [reason (cond
                         (entity-access/admin-only-entity? entity-key) :admin-only
                         :else :security)]
            (log/warn "🚫 ENTITY ACCESS BLOCKED"
              {:entity entity-key :reason reason :method method :uri uri})
            (entity-access/get-blocked-entity-response entity-key reason))

          ;; Special handling for users entity (protected)
          (= entity-key :users)
          (handle-users-entity-access request handler tenant-id method)

          ;; Public entities - require tenant context and enforce role-based write restrictions
          (entity-access/public-entity? entity-key)
          (cond
            (nil? tenant-id)
            (do
              (log/warn "❌ TENANT CONTEXT MISSING for public entity"
                {:entity entity-key :method method :uri uri})
              (unauthorized "Tenant context required"))

            (and (write-method? method)
              (not (allow-entity-write? entity-key role)))
            (do
              (log/warn "🚫 ENTITY WRITE BLOCKED"
                {:entity entity-key :method method :uri uri :tenant-id tenant-id :user-id user-id :role role})
              (forbidden "Insufficient permissions for write operation"))

            :else
            (handler request))

          ;; Unknown case - should not happen due to allowlist check above
          :else
          (do
            (log/error "🚨 UNEXPECTED: Entity passed allowlist but no handler"
              {:entity entity-key :method method :uri uri})
            (entity-access/get-blocked-entity-response entity-key :security)))))))
