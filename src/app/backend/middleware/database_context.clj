(ns app.backend.middleware.database-context
  "Database context middleware for setting tenant isolation and admin bypass.

   This middleware is critical for Row Level Security (RLS) to work properly.
   It sets PostgreSQL session variables that control which data each request can access:

   - app.current_tenant_id: Used by RLS policies for tenant isolation
   - app.bypass_rls: Used by admin requests to bypass tenant restrictions

   Security model:
   - Regular user requests: Set tenant_id, no RLS bypass
   - Admin requests: Set RLS bypass, no tenant_id restriction
   - Unauthenticated requests: No context set (RLS will deny access)"
  (:require
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ================================================================================
;; Database Context Setting Functions
;; ================================================================================

(defn set-tenant-context!
  "Set the current tenant context in the database session.

   This sets the app.current_tenant_id session variable that RLS policies use
   to automatically filter data to the current tenant.

   Args:
   - db: Database connection/spec
   - tenant-id: UUID string of the tenant

   Returns: true if successful, false if failed"
  [db tenant-id]
  (when tenant-id
    (try
      (jdbc/execute! db [(str "SET LOCAL app.current_tenant_id = '" tenant-id "'")])
      (log/debug "🔒 Database tenant context set" {:tenant-id tenant-id})
      true
      (catch Exception e
        (log/error e "❌ Failed to set tenant context" {:tenant-id tenant-id})
        false))))

(defn set-admin-bypass-context!
  "Set admin RLS bypass context in the database session.

   This allows admin operations to bypass Row Level Security policies
   and access data across all tenants.

   Args:
   - db: Database connection/spec

   Returns: true if successful, false if failed"
  [db]
  (try
    (jdbc/execute! db ["SET LOCAL app.bypass_rls = true"])
    (log/debug "🔓 Database admin bypass context set")
    true
    (catch Exception e
      (log/error e "❌ Failed to set admin bypass context")
      false)))

(defn clear-database-context!
  "Clear all database context variables.

   This should be called at the end of a request to ensure clean state.

   Args:
   - db: Database connection/spec"
  [db]
  (try
    (jdbc/execute! db ["SET LOCAL app.current_tenant_id = NULL"])
    (jdbc/execute! db ["SET LOCAL app.bypass_rls = false"])
    (log/debug "🧹 Database context cleared")
    (catch Exception e
      (log/warn e "⚠️ Failed to clear database context (may not be critical)"))))

;; ================================================================================
;; Context Extraction Functions
;; ================================================================================

(defn extract-tenant-id
  "Extract tenant ID from request session.

   Checks multiple possible locations where tenant ID might be stored
   in the session depending on authentication method.

   Args:
   - request: Ring request map

   Returns: Tenant ID as string, or nil if not found"
  [request]
  (or (get-in request [:session :auth-session :tenant :id])
    (get-in request [:session :tenant :id])
    (get-in request [:session :tenant-id])))

(defn extract-admin-info
  "Extract admin information from request.

   Args:
   - request: Ring request map

   Returns: Admin map if present, or nil"
  [request]
  (:admin request))

(defn should-set-admin-context?
  "Determine if this request should use admin bypass context.

   Args:
   - request: Ring request map

   Returns: true if admin context should be set"
  [request]
  (some? (extract-admin-info request)))

;; ================================================================================
;; Middleware Functions
;; ================================================================================

(defn wrap-database-context
  "Single-tenant no-op context middleware. Keeps handler signature but skips tenant/admin GUCs."
  [handler _db]
  (fn [request]
    (handler request)))

(defn wrap-transaction-context
  "Single-tenant transaction wrapper; leaves DB context untouched."
  [handler db]
  (fn [request]
    (jdbc/with-transaction [_tx db]
      (handler request))))

;; ================================================================================
;; Validation and Testing Functions
;; ================================================================================

(defn validate-database-context
  "Validate that database context is working correctly.

   This should be called during application startup to ensure RLS and context
   setting are working properly.

   Args:
   - db: Database connection/spec

   Returns: true if validation passes, throws exception if failed"
  [db]
  (try
    ;; Test 1: Check if RLS functions exist
    (jdbc/execute! db ["SELECT 1 FROM pg_proc WHERE proname = 'current_tenant_id'"])
    (jdbc/execute! db ["SELECT 1 FROM pg_proc WHERE proname = 'bypass_rls_for_admin'"])

    ;; Test 2: Test setting tenant context
    (set-tenant-context! db "00000000-0000-0000-0000-000000000001")
    (let [result (jdbc/execute! db ["SELECT current_tenant_id()"])]
      (when-not (= "00000000-0000-0000-0000-000000000001"
                  (str (-> result first vals first)))
        (throw (ex-info "Tenant context setting failed" {}))))

    ;; Test 3: Test admin bypass context
    (set-admin-bypass-context! db)
    (let [result (jdbc/execute! db ["SELECT bypass_rls_for_admin()"])]
      (when-not (-> result first vals first)
        (throw (ex-info "Admin bypass context setting failed" {}))))

    ;; Clean up
    (clear-database-context! db)

    (log/info "✅ Database context validation passed")
    true

    (catch Exception e
      (log/error e "❌ Database context validation failed")
      (throw e))))

(comment)
  ;; Example usage:

  ;; Apply as middleware (choose one approach):
  ;;(-> handler
  ;;  (wrap-database-context db)
  ;;  other-middleware)

  ;; OR with transaction wrapping:
  ;;(-> handler
  ;;  (wrap-transaction-context db)
  ;;  other-middleware)

  ;; Manual context setting:
  ;;(set-tenant-context! db "tenant-uuid")
  ;;(set-admin-bypass-context! db))
  ;;(clear-database-context! db)

  ;; Validation:
  ;;(validate-database-context db))
