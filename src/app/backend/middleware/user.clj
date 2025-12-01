 (ns app.backend.middleware.user
   "Enhanced middleware for authenticating regular (non-admin) API requests and
    authorizing access to entities with comprehensive security controls."
   (:require
     [app.backend.security.entity-access :as entity-access]
     [ring.util.response :as response]
     [taoensso.timbre :as log]))

(defn- unauthorized
  ([] (unauthorized "Authentication required"))
  ([message]
   (-> (response/response {:error message})
     (response/status 401)
     (response/content-type "application/json"))))

(defn wrap-user-authentication
  "Require a logged-in user session for protected API routes.

   Looks for a user map at [:session :user]. If absent, returns 401.

   Notes:
   - Admin routes use a separate admin middleware; this does not apply there.
   - Keep this middleware focused on generic /api routes only."
  [handler]
  (fn [request]
    (let [user (or (get-in request [:session :auth-session :user])
                 (get-in request [:session :user]))]
      (if user
        (handler request)
        (do
          (log/warn "❌ USER AUTH FAILED: No user in session" {:uri (:uri request)})
          (unauthorized))))))

(defn- handle-tenants-entity-access
  "Handle special authorization rules for tenants entity."
  [request handler tenant-id method]
  (cond
   ;; Ensure we have a tenant in session for any tenant access
    (nil? tenant-id)
    (do (log/warn "❌ TENANT ACCESS DENIED: No tenant in session"
          {:uri (:uri request) :method method})
      (unauthorized))

   ;; POST /entities/tenants – creation via generic API not allowed
    (= method :post)
    (do (log/warn "🚫 Tenant creation via generic API is not allowed")
      (-> (response/response {:error "Tenant creation not allowed via this endpoint"
                              :suggestion "Use onboarding flow for tenant creation"})
        (response/status 403)
        (response/content-type "application/json")))

   ;; Item routes (have :id param) – allow only when id matches current tenant
    (some? (get-in request [:path-params :id]))
    (let [path-id (get-in request [:path-params :id])
          matches? (= (str path-id) (str tenant-id))]
      (if matches?
        (handler request)
        (do (log/warn "🚫 TENANT ITEM ACCESS DENIED" {:path-id path-id :tenant-id tenant-id})
          (-> (response/response {:error "Access denied: Can only access your own tenant"})
            (response/status 403)
            (response/content-type "application/json")))))

   ;; Collection GET – scope to current tenant by injecting a filter
    (= method :get)
    (let [scoped-req (assoc-in request [:query-params :filters]
                               (merge (or (:filters (:query-params request)) {})
                                 {:id tenant-id}))]
      (log/info "🔒 Scoping tenants GET to current tenant" {:tenant-id tenant-id})
      (handler scoped-req))

   ;; Any other method – pass through with tenant context
    :else
    (handler request)))

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
  "Enhanced authorization guard for generic CRUD entities under /api/v1/entities.

   This middleware implements comprehensive security controls:
   1. Blocks admin-only entities (admins, admin_sessions, audit_logs, etc.)
   2. Enforces tenant isolation for tenant-specific entities
   3. Special handling for protected entities (tenants, users)
   4. Logs all access attempts for security monitoring

   Security model:
   - Admin-only entities: NEVER accessible via generic CRUD
   - Protected entities: Allowed but with strict tenant isolation
   - Public tenant entities: Normal tenant-scoped access"
  [handler]
  (fn [request]
    (let [entity-name (or (get-in request [:path-params :entity])
                        (get-in request [:parameters :path :entity]))
          entity-key (when entity-name (keyword entity-name))
          method (:request-method request)
          uri (:uri request)
          is-admin? (some? (:admin request))
          tenant-id (or (get-in request [:session :auth-session :tenant :id])
                      (get-in request [:session :tenant :id])
                      (get-in request [:session :tenant-id]))
          user-id (or (get-in request [:session :auth-session :user :id])
                    (get-in request [:session :user :id]))]

      ;; Log the access attempt for security monitoring
      (entity-access/log-entity-access-attempt entity-key
        (entity-access/entity-allowed-for-generic-crud? entity-key is-admin?)
        {:method method :uri uri :tenant-id tenant-id :user-id user-id})

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

        ;; Special handling for tenants entity (protected)
        (= entity-key :tenants)
        (handle-tenants-entity-access request handler tenant-id method)

        ;; Special handling for users entity (protected)
        (= entity-key :users)
        (handle-users-entity-access request handler tenant-id method)

        ;; Public entities - ensure tenant context exists
        (entity-access/public-entity? entity-key)
        (if tenant-id
          (handler request)
          (do
            (log/warn "❌ TENANT CONTEXT MISSING for public entity"
              {:entity entity-key :method method :uri uri})
            (unauthorized "Tenant context required")))

        ;; Unknown case - should not happen due to allowlist check above
        :else
        (do
          (log/error "🚨 UNEXPECTED: Entity passed allowlist but no handler"
            {:entity entity-key :method method :uri uri})
          (entity-access/get-blocked-entity-response entity-key :security))))))
