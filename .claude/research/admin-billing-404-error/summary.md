# Admin Billing 404 Error - Research Summary

## Issue Overview
**Problem**: "View billing" action in admin tenant management returns 404 Not Found error when accessing `GET /admin/api/billing/subscription/{tenant-id}`

**User Action**: Clicking "View billing" in tenant actions dropdown
**Expected Result**: Subscription details for the specified tenant
**Actual Result**: 404 Not Found

## Research Findings

### 🔍 Frontend Investigation

**Component Location**: `/Users/enes/Projects/hosting/src/app/admin/frontend/components/tenant_actions.cljs`

**Action Definition** (Lines 126-130):
```clojure
[{:id "view-tenant-billing"
  :icon "💰"
  :label "View Billing"
  :loading-key :loading-tenant-billing?
  :on-click (:view-billing action-handlers)}]
```

**Event Handler** (Lines 47-50):
```clojure
:view-billing (fn [e]
                (stop-propagation! e)
                (log/info "Viewing tenant billing" tenant-id tenant-name)
                (rf/dispatch [:admin/view-tenant-billing tenant-id]))
```

**Event Dispatch Chain**:
1. `:admin/view-tenant-billing` → `:admin/load-subscription-details`
2. Navigation to `:admin-billing` route

### 🔌 API Call Investigation

**Event Handler Location**: `/Users/enes/Projects/hosting/src/app/admin/frontend/events/billing.cljs`

**API Call Implementation** (Lines 80-86):
```clojure
(rf/reg-event-fx
  :admin/load-subscription-details
  (fn [{:keys [db]} [_ tenant-id]]
    {:db (assoc db :admin/subscription-loading? true)
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/billing/subscription/" tenant-id)
                    :on-success [:admin/subscription-details-loaded]
                    :on-failure [:admin/billing-load-failed]})}))
```

**Result**: API call correctly targets the reported endpoint.

### 🛣️ Backend Route Investigation

**Route Definition**: `/Users/enes/Projects/hosting/src/app/backend/routes/admin/billing.clj`

**Route Configuration** (Lines 104-107):
```clojure
["/subscription/:id"
 {:get (get-subscription-details-handler db)
  :put (update-subscription-handler db)}]
```

**Route Handler** (Lines 31-41):
```clojure
(defn get-subscription-details-handler
  "Get detailed subscription information for a tenant"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [tenant-id _request]
          (if-let [details (admin-service/get-subscription-details db tenant-id)]
            (utils/json-response (db-adapter/convert-db-keys->app-keys details))
            (utils/error-response "Subscription details not found" :status 404)))))
    "Failed to retrieve subscription details"))
```

**Route Registration**: `/Users/enes/Projects/hosting/src/app/backend/routes/admin_api.clj`
```clojure
["/billing" (admin-billing/routes db)]  ; Line 104
```

**Result**: Routes are properly defined and registered.

### 🏗️ Service Implementation Investigation

**Billing Service**: `/Users/enes/Projects/hosting/src/app/backend/services/admin/billing.clj`

**Core Function** (Lines 160-198):
```clojure
(defn get-subscription-details
  "Get detailed subscription information for a tenant"
  [db tenant-id]
  (try
    (db-adapter/with-admin-transaction db
      (fn [tx]
        (let [tenant (jdbc/execute-one! tx
                       (hsql/format {:select [:*]
                                     :from [:tenants]
                                     :where [:= :id tenant-id]}))
              subscription-status (when tenant
                                    (sub-service/get-subscription-status db tenant-id))
              ;; ... billing history and usage stats
              ))
      {:action "get-subscription-details" :entity-type "tenant" :entity-id tenant-id})
    (catch Exception e
      (log/error e "Failed to get subscription details" {:tenant_id tenant-id})
      nil)))  ; ← Returns nil on any error
```

**Subscription Service**: `/Users/enes/Projects/hosting/src/app/template/backend/subscription/service.clj`

**Support Function** (Lines 241-259):
```clojure
(defn get-subscription-status
  "Get current subscription status for a tenant"
  [db tenant-id]
  (when-let [tenant (db-protocols/find-by-id db :tenants tenant-id)]
    ;; Returns subscription details or nil if tenant not found
    ))
```

### 🗄️ Database Schema Verification

**Tenants Table**: `/Users/enes/Projects/hosting/resources/db/models.edn`

**Key Fields**:
```clojure
:tenants
 {:fields
  [[:id :uuid {:primary-key true}]
   [:name [:varchar 255] {:null false}]
   [:subscription_tier [:enum :subscription-tier] {:default "free"}]
   [:subscription_status [:enum :subscription-status] {:default "trialing"}]
   [:stripe_customer_id [:varchar 255] {:unique true}]
   [:stripe_subscription_id [:varchar 255]]
   ;; ... other fields
   ]})
```

**Result**: Database schema is correct and contains all required fields.

## Root Cause Analysis

### 🎯 Most Likely Causes

1. **Tenant Not Found** (Most Probable)
   - The `get-subscription-details` function returns `nil` if tenant lookup fails
   - Route handler returns 404 when `details` is `nil`
   - Tenant ID `4422a866-7670-48eb-81b4-3181059d7259` may not exist in database

2. **Database Transaction Error**
   - Any exception in the transaction block causes `nil` return
   - Errors are logged but not propagated to the client

3. **Admin Authentication Issue**
   - Route requires admin authentication middleware
   - Authentication failure could cause 404

### 🔧 Supporting Evidence

**Error Handling Pattern**:
```clojure
;; In get-subscription-details
(catch Exception e
  (log/error e "Failed to get subscription details" {:tenant_id tenant-id})
  nil))  ; Silent failure - returns nil

;; In route handler
(if-let [details (admin-service/get-subscription-details db tenant-id)]
  (utils/json-response ...)
  (utils/error-response "Subscription details not found" :status 404))
```

## Potential Investigation Points

### 1. Verify Tenant Existence
```sql
SELECT id, name, subscription_tier, subscription_status
FROM tenants
WHERE id = '4422a866-7670-48eb-81b4-3181059d7259';
```

### 2. Check Admin Authentication
- Verify admin user is authenticated
- Check if admin has required permissions

### 3. Review Application Logs
Look for error messages like:
- `"Failed to get subscription details"`
- `"Tenant not found"`
- Database connection errors

### 4. Test Endpoint Directly
```bash
curl -X GET "http://localhost:8080/admin/api/billing/subscription/4422a866-7670-48eb-81b4-3181059d7259" \
  -H "Authorization: Bearer <admin-token>"
```

## Code Flow Summary

```
Frontend Action → :admin/view-tenant-billing
               → :admin/load-subscription-details
               → HTTP GET /admin/api/billing/subscription/{tenant-id}
               → get-subscription-details-handler
               → admin-service/get-subscription-details
               → sub-service/get-subscription-status
               → Database tenant lookup
               → Return details OR nil → 404 if nil
```

## Key Files and Locations

| Component | File Path | Key Lines |
|-----------|-----------|-----------|
| Frontend Action | `src/app/admin/frontend/components/tenant_actions.cljs` | 126-130, 47-50 |
| Frontend Event | `src/app/admin/frontend/events/billing.cljs` | 80-86 |
| Backend Route | `src/app/backend/routes/admin/billing.clj` | 104-107, 31-41 |
| Route Registration | `src/app/backend/routes/admin_api.clj` | 104 |
| Billing Service | `src/app/backend/services/admin/billing.clj` | 160-198 |
| Subscription Service | `src/app/template/backend/subscription/service.clj` | 241-259 |
| Database Schema | `resources/db/models.edn` | Tenants table definition |

## Recommended Next Steps for Debugging

1. **Verify tenant existence** in database with the provided UUID
2. **Check admin authentication** status and permissions
3. **Review application logs** for specific error messages
4. **Test endpoint directly** with curl or similar tool
5. **Add debug logging** to pinpoint exact failure point

The infrastructure appears to be correctly implemented, suggesting the issue is likely data-related (tenant not found) or authentication-related rather than a code bug.
