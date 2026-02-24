 (ns app.template.backend.middleware.user
   "Enhanced middleware for authenticating regular (non-admin) API requests and
    authorizing access to entities with comprehensive security controls."
   (:require
     [app.shared.http :as shared-http]
     [app.template.backend.security.entity-access :as entity-access]
     [taoensso.timbre :as log]))

(defn- unauthorized
  ([] (unauthorized "Authentication required"))
  ([message]
   ;; Return a JSON *string* body because this middleware short-circuits the
   ;; downstream response encoding middleware.
   (shared-http/json-string-response 401 {:error message})))

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
   - Public entities: currently none
   - Unknown entities: blocked by default (deny-by-default)"
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
