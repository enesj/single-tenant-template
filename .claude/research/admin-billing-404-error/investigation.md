# Technical Investigation Details

## API Endpoint Analysis

### Complete Request Flow
```
1. User clicks "View Billing" in tenant actions dropdown
2. Frontend dispatches :admin/view-tenant-billing event
3. Event handler dispatches :admin/load-subscription-details
4. HTTP GET request to /admin/api/billing/subscription/{tenant-id}
5. Backend route handler processes request
6. Service layer queries database
7. Response returned or 404 if data not found
```

### Route Pattern Matching
**Expected Route**: `/admin/api/billing/subscription/4422a866-7670-48eb-81b4-3181059d7259`

**Route Construction**:
```
/admin/api           (from admin-api.clj line 40)
/billing             (from admin-api.clj line 104)
/subscription/:id    (from billing.clj line 104)
```

**Final Pattern**: `/admin/api/billing/subscription/:id` ✅ **MATCHES**

## Error Handling Chain Analysis

### Frontend Error Handling
```clojure
;; billing.cljs line 84-86
:http-xhrio (admin-http/admin-get
              {:uri (str "/admin/api/billing/subscription/" tenant-id)
               :on-success [:admin/subscription-details-loaded]
               :on-failure [:admin/billing-load-failed]})
```

### Backend Error Handling
```clojure
;; billing.clj line 31-41
(utils/with-error-handling
  (fn [request]
    (utils/handle-uuid-request request :id
      (fn [tenant-id _request]
        (if-let [details (admin-service/get-subscription-details db tenant-id)]
          (utils/json-response (db-adapter/convert-db-keys->app-keys details))
          (utils/error-response "Subscription details not found" :status 404)))))
  "Failed to retrieve subscription details")
```

### Service Layer Error Handling
```clojure
;; billing.clj line 160-198
(try
  (db-adapter/with-admin-transaction db
    (fn [tx] ...))  ; Returns data or nil
  (catch Exception e
    (log/error e "Failed to get subscription details" {:tenant_id tenant-id})
    nil))  ; ← Silent failure point
```

## Authentication Middleware Stack

Based on admin-api.clj structure:
```
/admin/api routes → admin authentication middleware → billing routes
```

**Middleware Chain**:
1. `admin-utils/wrap-json-body-parsing` (line 42)
2. `admin-middleware/wrap-admin-authentication` (line 83)
3. Route-specific handlers

## Database Transaction Analysis

### Transaction Flow
```clojure
(db-adapter/with-admin-transaction db
  (fn [tx]
    (let [tenant (jdbc/execute-one! tx
                   (hsql/format {:select [:*]
                                 :from [:tenants]
                                 :where [:= :id tenant-id]}))
          subscription-status (when tenant
                                (sub-service/get-subscription-status db tenant-id))
          billing-history (jdbc/execute! tx ...)
          usage-stats (jdbc/execute! tx ...)]
      {:tenant tenant
       :subscription subscription-status
       :billing_history billing-history
       :usage_stats usage-stats})))
```

### Failure Points
1. **Tenant Lookup Failure**: `tenant` is `nil` → entire result is `nil`
2. **Subscription Service Error**: Exception caught → returns `nil`
3. **Database Query Error**: Exception caught → returns `nil`
4. **Transaction Error**: Exception caught → returns `nil`

## Data Format Transformation

### Key Conversion Process
```clojure
;; In route handler
(db-adapter/convert-db-keys->app-keys details)

;; In service
(db-adapter/normalize-admin-result data billing-config)
```

### Potential Key Mapping Issues
- Database returns `:table/column` keys
- Frontend expects `:column` keys
- Conversion handles this but could be a failure point

## MCP Tool Scripts for Investigation

### 1. Database Query Test
```clojure
;; clojure-mcp script to test tenant lookup
(clojure-eval "
  (require '[app.backend.services.admin.billing :as billing])
  (require '[next.jdbc :as jdbc])

  (let [db (get @app.backend.core/system :db)
        tenant-id #uuid \"4422a866-7670-48eb-81b4-3181059d7259\"]
    (jdbc/execute-one! db
      [\"SELECT * FROM tenants WHERE id = ?\" tenant-id]))
")
```

### 2. Service Function Test
```clojure
;; clojure-mcp script to test billing service
(clojure-eval "
  (require '[app.backend.services.admin :as admin-service])

  (let [db (get @app.backend.core/system :db)
        tenant-id #uuid \"4422a866-7670-48eb-81b4-3181059d7259\"]
    (admin-service/get-subscription-details db tenant-id))
")
```

### 3. Route Registration Verification
```clojure
;; clojure-mcp script to check route registration
(clojure-eval "
  (require '[app.backend.routes.admin-api :as admin-routes])
  (require '[reitit.core :as r])

  (let [db (get @app.backend.core/system :db)
        router (r/router (admin-routes/admin-api-routes db {}))]
    (r/match-by-path router \"/admin/api/billing/subscription/4422a866-7670-48eb-81b4-3181059d7259\"))
")
```

### 4. Chrome Browser Test
```clojure
;; chrome-mcp script to test endpoint directly
(chrome_network_request
  {:url "http://localhost:8080/admin/api/billing/subscription/4422a866-7670-48eb-81b4-3181059d7259"
   :method "GET"
   :headers {"Authorization" "Bearer <admin-token>"}})
```

## Debugging Strategy

### 1. Enable Debug Logging
```clojure
;; Add to billing.clj get-subscription-details function
(log/debug "Looking up tenant" {:tenant-id tenant-id})
(log/debug "Tenant found" {:tenant tenant})
(log/debug "Subscription status" {:subscription subscription-status})
```

### 2. Test Individual Components
1. Test raw database query for tenant
2. Test subscription service independently
3. Test route handler with mock data
4. Test complete flow with known valid tenant

### 3. Verify Admin Authentication
```clojure
;; Check if admin authentication is working
(clojure-eval "
  (require '[app.backend.middleware.admin :as admin-mw])
  ;; Test admin authentication middleware
")
```

## Common Causes and Solutions

| Cause | Symptoms | Solution |
|-------|----------|----------|
| Tenant not found | 404 error, silent failure | Verify tenant exists in database |
| Admin auth failure | 403/401 error (different from 404) | Check admin login status |
| Database connection error | Logs show connection failures | Verify database is running |
| Route not registered | 404 for all billing endpoints | Restart application to reload routes |
| Key conversion error | 500 error, not 404 | Check data format conversion |

## Most Likely Root Cause

Based on the investigation, the **most probable cause** is:

**Tenant ID '4422a866-7670-48eb-81b4-3181059d7259' does not exist in the tenants table**

This would cause:
1. `tenant` lookup to return `nil`
2. Entire `get-subscription-details` function to return `nil`
3. Route handler to return 404 "Subscription details not found"

**Evidence**:
- Clean error handling that returns `nil` on any failure
- 404 error indicates route exists but data not found
- No authentication errors reported
- Infrastructure appears correctly implemented
