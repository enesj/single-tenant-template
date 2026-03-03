# Multi-Tenancy Manual Testing Plan (Phases 0–6)

> **Scope**: Comprehensive manual testing for the complete multi-tenancy implementation.
>
> **Prerequisites**:
> - `bb run-app` running on port 8085, dev DB on port 55432
> - At least one admin account in the admin panel (`/admin`)
> - REPL connected on port 7888
>
> **Test accounts needed**: 3 email/password users (or use OAuth). One admin.
>
> **Notation**: `[REPL]` = verify via nREPL. `[UI]` = verify via browser. `[API]` = verify via curl/API.

---

## 1. DB Foundation (Phase 0)

**Goal**: Confirm schema is correct and all tables/enums exist.

### 1.1 Verify tenant tables exist [REPL]

```clj
(require '[next.jdbc :as jdbc])
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT table_name FROM information_schema.tables
                      WHERE table_schema = 'public'
                      AND table_name IN ('tenants','tenant_memberships','tenant_invitations','impersonation_grants','user_sessions')
                      ORDER BY table_name"]))
```

**Expected**: 5 rows returned — all tables exist.

### 1.2 Verify tenant_id columns on domain tables [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT table_name, column_name
                      FROM information_schema.columns
                      WHERE column_name = 'tenant_id'
                      AND table_schema = 'public'
                      ORDER BY table_name"]))
```

**Expected**: `tenant_id` exists on: `expenses`, `expense_items`, `expense_categories`, `payers`, `payer_types`, `receipts`, `user_expense_settings`.

### 1.3 Verify enum types [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT typname FROM pg_type
                      WHERE typname IN ('tenant_status','membership_role','membership_status',
                                        'invitation_status','invitation_role',
                                        'impersonation_role','impersonation_status')
                      ORDER BY typname"]))
```

**Expected**: 7 enum types returned.

### 1.4 Verify price_observations table dropped [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT table_name FROM information_schema.tables
                      WHERE table_name = 'price_observations'"]))
```

**Expected**: Empty result — table does not exist.

---

## 2. Tenant Provisioning (Phase 1)

**Goal**: New user registration auto-creates a tenant with seed data.

### 2.1 Register new user (email/password) [UI]

1. Go to `/login` and click "Register"
2. Enter email: `test-user-a@example.com`, name: `Test User A`, password
3. Submit

**Expected**:
- Registration succeeds, user is logged in
- Redirected to the main app (not `/tenant-select`)
- Sidebar footer shows tenant name (derived from email prefix, e.g. "test-user-a")

### 2.2 Verify tenant provisioning seed data [UI]

1. As the newly registered user, navigate to expense categories
2. Check payer types (visible in expense creation form)

**Expected**:
- 8 default expense categories exist: Groceries, Transport, Utilities, Dining, Healthcare, Entertainment, Shopping, Other
- 4 default payer types exist: Cash (default), Credit Card, Debit Card, Bank Transfer

### 2.3 Verify tenant slug [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT id, name, slug, status FROM tenants ORDER BY created_at DESC LIMIT 3"]))
```

**Expected**: New tenant exists with status `"active"`, slug derived from email prefix.

### 2.4 Verify owner membership [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT tm.role, tm.status, u.email
                      FROM tenant_memberships tm
                      JOIN users u ON u.id = tm.user_id
                      WHERE tm.tenant_id = (SELECT id FROM tenants ORDER BY created_at DESC LIMIT 1)"]))
```

**Expected**: One membership with `role = 'owner'`, `status = 'active'`.

### 2.5 OAuth registration + provisioning [UI]

1. Log out, go to `/login`
2. Click OAuth provider (Google/GitHub)
3. Complete OAuth flow with a new account

**Expected**: Same as 2.1 — tenant auto-provisioned, seed data present, user is owner.

---

## 3. Session & Auth Context (Phase 1)

**Goal**: Session correctly carries tenant context through login flows.

### 3.1 Auth status returns tenant info [API]

```bash
# After logging in as test-user-a, check auth status:
curl -s -b cookies.txt http://localhost:8085/api/v1/auth/status | python3 -m json.tool
```

**Expected**: Response contains:
- `"tenant"`: `{"id": "...", "name": "...", "slug": "..."}`
- `"membership-role"`: `"owner"`
- `"tenant-selection-required"`: `false` (single tenant)
- No `"password_hash"` field (security — stripped in handler)

### 3.2 Single-tenant user auto-sets context [UI]

1. Log out as test-user-a
2. Log back in

**Expected**: Immediately lands on main app, not `/tenant-select`. Tenant context is auto-set.

### 3.3 Multi-tenant user sees selection page [UI]

> Requires a user with 2+ memberships — complete Section 5 first, then return here.

1. Log in as a user who belongs to 2+ tenants

**Expected**: Redirected to `/tenant-select` page showing tenant cards with role badges.

### 3.4 Tenant selection page works [UI]

1. On `/tenant-select`, click a tenant card

**Expected**: Redirected to main app. Sidebar shows selected tenant name. Data is scoped to that tenant.

---

## 4. Tenant Data Scoping (Phase 2)

**Goal**: Tenant-scoped data is isolated. Global catalog is shared.

### 4.1 Create test data in Tenant 1 [UI]

1. As test-user-a (Tenant 1 owner):
   - Create a custom expense category: "Tenant1 Special"
   - Create a payer type: "Tenant1 Wallet"
   - Create an expense with a line item
   - Upload a receipt
   - Note the expense ID and receipt ID

### 4.2 Create Tenant 2 [UI]

1. Register a new user: `test-user-b@example.com`
2. This auto-provisions Tenant 2

### 4.3 Cross-tenant expense isolation [UI + API]

1. As test-user-b (Tenant 2), check expense list

**Expected**: Empty — Tenant 1's expenses are NOT visible.

2. Try direct API access:
```bash
curl -s -b cookies-b.txt http://localhost:8085/api/v1/expenses/{TENANT-1-EXPENSE-ID}
```

**Expected**: 404 Not Found (not 403 — the resource doesn't "exist" for this tenant).

### 4.4 Cross-tenant receipt isolation [UI]

1. As test-user-b, check receipt list

**Expected**: Empty — Tenant 1's receipts are NOT visible.

### 4.5 Cross-tenant lookup data isolation [UI]

1. As test-user-b, check expense categories

**Expected**: Only default categories (Groceries, Transport, etc.) — "Tenant1 Special" does NOT appear.

2. Check payer types

**Expected**: Only default types — "Tenant1 Wallet" does NOT appear.

### 4.6 Global catalog remains shared [UI]

1. As test-user-a (Tenant 1), create a supplier: "Shared Supplier ABC"
2. Switch to test-user-b (Tenant 2)
3. Check supplier list

**Expected**: "Shared Supplier ABC" IS visible — suppliers are global catalog, shared across tenants.

### 4.7 Cross-tenant user_expense_settings isolation [UI]

1. As test-user-a (Tenant 1), set a default payer in settings
2. Switch to test-user-b (Tenant 2), check settings

**Expected**: Default payer is NOT set in Tenant 2 — settings are per (tenant, user).

---

## 5. Invitation Flow (Phase 1)

**Goal**: Owner can invite users; invitations work correctly.

### 5.1 Send invitation [UI]

1. As test-user-a (Tenant 1 owner), go to Members page (`/tenant/members`)
2. Enter email: `test-user-c@example.com`, select role: "member"
3. Click "Invite"

**Expected**: Invitation appears in pending invitations list with role badge.

### 5.2 Duplicate invitation blocked [UI]

1. Try inviting `test-user-c@example.com` again to the same tenant

**Expected**: Error — pending invitation already exists for this email.

### 5.3 Accept invitation [UI]

1. Find the invitation token:
```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT token, email, role, status FROM tenant_invitations ORDER BY created_at DESC LIMIT 1"]))
```

2. Open incognito window, go to `/invitation/accept?token={TOKEN}`
3. Register as test-user-c (or log in if already registered)

**Expected**:
- Invitation accepted, status changes to "accepted"
- User is now a member of Tenant 1
- Redirected to Tenant 1 context

### 5.4 Verify membership created [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT tm.role, tm.status, u.email, t.name as tenant_name
                      FROM tenant_memberships tm
                      JOIN users u ON u.id = tm.user_id
                      JOIN tenants t ON t.id = tm.tenant_id
                      WHERE u.email = 'test-user-c@example.com'"]))
```

**Expected**: Two memberships — one "owner" (own tenant), one "member" (invited to Tenant 1).

### 5.5 Revoke invitation [UI]

1. As test-user-a, invite `test-user-d@example.com`
2. Before it's accepted, click "Revoke" on the pending invitation

**Expected**: Invitation status changes to "revoked". Token no longer works for acceptance.

---

## 6. Member Management (Phase 1)

**Goal**: Owner can manage tenant members.

### 6.1 Change member role [UI]

1. As test-user-a (owner), go to Members page
2. Find test-user-c (member)
3. Change role to "admin"

**Expected**: Role badge updates to "admin". Member now has admin-level access.

### 6.2 Remove member [UI]

1. Create a throwaway member (invite + accept)
2. As owner, click "Remove" on that member

**Expected**: Member removed from tenant. They can no longer access tenant data.

### 6.3 Transfer ownership [UI]

1. As owner, find an admin member
2. Click "Transfer Ownership"

**Expected**:
- Target user becomes owner
- Original owner becomes admin
- Only one owner per tenant at any time

### 6.4 Guard: cannot remove owner [UI]

1. Try to remove the current owner from the members list

**Expected**: Remove button not visible or action returns error. Owners cannot be removed.

### 6.5 Guard: cannot invite as owner [UI]

1. Try to invite someone with role "owner"

**Expected**: "owner" is not available in the role picker. Only viewer/member/admin can be invited.

---

## 7. Tenant Switching (Phase 1 + Phase 5)

**Goal**: Multi-tenant users can switch between tenants.

### 7.1 Tenant switcher visibility [UI]

1. Log in as test-user-c (has 2 tenants — own + Tenant 1 membership)

**Expected**: Sidebar footer shows a tenant switcher dropdown with swap icon.

2. Log in as test-user-a (has only 1 tenant)

**Expected**: Sidebar footer shows tenant name but NO swap icon (single tenant).

### 7.2 Switch tenants [UI]

1. As test-user-c, click the tenant switcher
2. Select the other tenant

**Expected**:
- Page reloads with new tenant context
- URL bar updates with new tenant slug: `/t/{new-slug}/...`
- Expense list shows only the selected tenant's data
- Categories/payers are different per tenant

### 7.3 Slug display in URL [UI]

1. After switching tenants, check the URL bar

**Expected**: URL shows `/t/{slug}/expenses` or similar. Slug is cosmetic — session is source of truth.

### 7.4 Direct slug URL navigation [UI]

1. Copy the URL with slug prefix
2. Paste into new tab (same session)

**Expected**: Page loads correctly with the correct tenant context.

---

## 8. Access Control — Role Enforcement (Phase 3)

**Goal**: 4-tier role model (owner/admin/member/viewer) is enforced correctly.

### 8.1 Owner access [UI]

1. As tenant owner, verify access to:
   - Expenses: list, create, update, delete
   - Receipts: list, upload, approve
   - Expense categories: list, create, update, delete
   - Members page: visible, can invite/change roles
   - Impersonation grants: visible, can create/revoke
   - Settings: read + write

**Expected**: All operations succeed.

### 8.2 Admin access [UI]

1. Change test-user-c to "admin" role
2. As admin, verify:
   - Expenses: list, create, update, delete (should work)
   - Receipts: list, upload, approve (should work — `tenant-elevated?` includes admin)
   - Expense categories: list (yes), create/update/delete (yes — admin/owner write)
   - Members page: visible, can invite/change roles
   - Settings: read + write

**Expected**: Same as owner for most operations.

### 8.3 Member access [UI]

1. Change test-user-c to "member" role
2. As member, verify:
   - Expenses: list (yes), create (yes), update own (yes)
   - Receipts: list own (yes), upload (yes), approve (yes)
   - Receipt visibility: only own receipts, NOT all tenant receipts
   - Expense categories: list (yes), create/update/delete (403)
   - Supplier aliases write: 403
   - Members page: NOT visible in sidebar
   - Settings: read only (banner says "read-only")

**Expected**: Members can read most things and write expenses/receipts, but cannot administer tenant settings.

### 8.4 Viewer access [UI]

1. Invite a user as "viewer" and accept
2. As viewer, verify:
   - Expenses: list (yes), create (403), update (403), delete (403)
   - Receipts: list own (yes), upload (403)
   - Expense categories: list (yes), write (403)
   - All write operations: 403

**Expected**: Read-only access everywhere.

### 8.5 Receipt visibility scoping [UI]

1. As owner, upload a receipt
2. As member, check receipt list

**Expected**: Member sees only their own receipts. Owner/admin see all tenant receipts.

### 8.6 Export requires read role [API]

```bash
# As a user with no membership (or after removal):
curl -s -b cookies.txt http://localhost:8085/api/v1/expenses/export
```

**Expected**: 403 if the user lacks expense read roles.

---

## 9. Platform Admin (Phase 4)

**Goal**: Admin panel is blocked from tenant data unless impersonating.

### 9.1 Admin blocked from expense data [UI]

1. Log in to admin panel at `/admin`
2. Navigate to Expenses section (suppliers, expenses, receipts, etc.)

**Expected**: 403 Forbidden on any expense-related admin API call. Non-blocked resources (users, tenants, admins) work normally.

### 9.2 Admin tenant management [UI]

1. In admin panel, go to Tenants page
2. Verify tenant list (search, status filter)
3. Click a tenant to see detail + members

**Expected**: List shows all tenants. Detail view shows tenant info + member list.

### 9.3 Admin user detail shows memberships [UI]

1. In admin panel, go to Users page
2. Click a user who has memberships

**Expected**: User detail includes a `memberships` array showing tenant name + role.

### 9.4 Create impersonation grant [UI]

1. As test-user-a (tenant owner), go to Impersonation Grants page
2. Select the platform admin
3. Create a grant

**Expected**: Grant appears in list with "active" status.

### 9.5 Activate impersonation [UI]

1. In admin panel, go to Impersonation section
2. Click "Activate" on the grant

**Expected**: Impersonation context set. Admin can now access that tenant's expense data.

### 9.6 Verify impersonated access [UI]

1. While impersonating, navigate to admin expense pages
2. Verify data is visible and scoped to the impersonated tenant

**Expected**: Expense data loads successfully. Only the impersonated tenant's data is visible.

### 9.7 Deactivate impersonation [UI]

1. Click "Deactivate" in admin panel

**Expected**: Impersonation context cleared. Expense data returns 403 again.

### 9.8 Revoke grant (owner-side) [UI]

1. As test-user-a, go to Impersonation Grants page
2. Revoke the active grant

**Expected**: Grant status changes to "revoked". Admin can no longer activate it. If admin had active session, impersonation is auto-deactivated.

### 9.9 Audit trail [REPL]

```clj
(let [db (:db @app.template.backend.core/system)]
  (jdbc/execute! db ["SELECT action, entity_type, details, created_at
                      FROM audit_logs
                      WHERE entity_type IN ('impersonation_grant', 'admin_session')
                      ORDER BY created_at DESC LIMIT 10"]))
```

**Expected**: Audit entries for: `impersonation_granted`, `impersonation_activated`, `impersonation_deactivated`, `impersonation_revoked_by_owner` (or `_by_admin`).

---

## 10. Frontend — UI State & Components (Phase 5)

**Goal**: Frontend correctly reflects multi-tenant state.

### 10.1 Role badge consistency [UI]

1. Go to Members page
2. Check role badges for each member

**Expected**: owner=primary color, admin=secondary, member=accent, viewer=ghost.

### 10.2 Sidebar role gating [UI]

1. As owner: sidebar shows all sections including "Members" under "Workspace"
2. As member: sidebar hides "Members" and admin-only sections
3. As viewer: minimal sidebar (read-only sections only)

**Expected**: Sidebar sections filter based on membership role, not global user role.

### 10.3 Settings read-only mode [UI]

1. As member or viewer, go to Settings page

**Expected**: Read-only banner displayed. Inputs are disabled. No save button.

### 10.4 Slug in URL after navigation [UI]

1. Navigate between pages (expenses, receipts, settings)
2. Check URL bar

**Expected**: Each page URL includes `/t/{slug}/...` prefix. Slug persists across navigation.

### 10.5 Re-frame app-db state [REPL/DevTools]

Open browser DevTools console:
```js
// Check tenant state in app-db:
app.template.frontend.db.defaults.app_db
```

Or via ClojureScript REPL:
```clj
(shadow.cljs.devtools.api/nrepl-select :app)
@re-frame.db/app-db
;; Check [:session :auth-session :tenant] and [:session :auth-session :membership]
```

**Expected**: `:tenant` has `:id`, `:name`, `:slug`. `:membership` has `:role`.

---

## 11. Dead Code Removal (Phase 6)

**Goal**: No runtime breakage from removing price_observations code.

### 11.1 Create expense with article-linked alias [UI]

1. Select a supplier that has article aliases
2. Add a line item with a raw label that maps to an existing alias
3. Save

**Expected**: Expense created successfully. No 500 error.

### 11.2 Update expense with new items [UI]

1. Edit an existing expense, add a new item with alias-mapped label
2. Save

**Expected**: Updated successfully.

### 11.3 Grep verification [Shell]

```bash
grep -r "price.observations\|price.history\|record-observation" src/ --include="*.clj" --include="*.cljs" --include="*.cljc"
```

**Expected**: Only 2 hits in docstring comments. Zero in executable code.

---

## 12. Per-Tenant Rate Limiting (Phase 6)

**Goal**: Rate limit keys include tenant-id for user API; admin stays global.

### 12.1 Verify tenant-scoped key format [REPL]

```clj
(require 'app.template.backend.middleware.rate-limiting :reload)

;; Make some API requests as authenticated user, then:
(let [storage @#'app.template.backend.middleware.rate-limiting/rate-limit-storage]
  (vec (keys storage)))
```

**Expected**: Regular API keys: `"regular-api:{ip}:t:{tenant-id}:u:{user-id}"`. Admin keys: `"admin-api:{ip}"` (no `:t:` segment).

### 12.2 Two tenants, same IP, independent buckets [REPL]

1. Make API requests as User A (Tenant 1)
2. Make API requests as User B (Tenant 2)
3. Check storage keys

**Expected**: Two separate `regular-api:...` entries with different `:t:{id}` segments.

### 12.3 Provisioning rate limit [REPL]

```clj
(app.template.backend.middleware.rate-limiting/clear-rate-limits!)

;; First 5 should pass
(dotimes [_ 5]
  (println (app.template.backend.middleware.rate-limiting/check-provisioning-rate-limit! "test-ip")))

;; 6th should block
(println "6th:" (app.template.backend.middleware.rate-limiting/check-provisioning-rate-limit! "test-ip"))

(app.template.backend.middleware.rate-limiting/clear-rate-limits!)
```

**Expected**: First 5 return `nil`. 6th returns `{:status 429 ...}`.

---

## 13. Automated Test Verification

### 13.1 Backend tests

```bash
mkdir -p tmp && bb be-test 2>&1 | tee tmp/be-test-final.txt
tail -5 tmp/be-test-final.txt
```

**Expected baseline**:
- ~527 tests, ~2015 assertions
- 39 errors (pre-existing: DI/route config, legacy tenant_id fixtures)
- 5 failures (pre-existing: manufacturer authz, pagination, user-articles)
- **0 new failures**

### 13.2 Focused multi-tenancy test suites

```bash
# Tenant auth (5 tests)
clojure -M:test -m kaocha.runner --focus app.template.backend.auth.tenant-test

# Tenant isolation (6 tests)
clojure -M:test -m kaocha.runner --focus app.domain.backend.expenses.services.tenant-isolation-test

# Impersonation (8 tests)
clojure -M:test -m kaocha.runner --focus app.domain.backend.expenses.services.impersonation-test

# Role access (15 tests)
clojure -M:test -m kaocha.runner --focus app.domain.backend.expenses.handlers.user-expenses.role-access-test

# Blocked resources (3 tests)
clojure -M:test -m kaocha.runner --focus app.domain.backend.expenses.routes.blocked-resources-test

# Admin tenants (11 tests)
clojure -M:test -m kaocha.runner --focus app.template.backend.routes.admin.tenants-test

# Merge (5 tests — verifies price_observations removal)
clojure -M:test -m kaocha.runner --focus app.domain.backend.expenses.services.merge-test
```

### 13.3 Frontend tests

```bash
npm run test:cljs
```

**Expected**: ~401 tests, 0 failures. Includes tenant events (11), impersonation events (7), admin tenants events (10).

---

## Sign-Off Checklist

### Phase 0 — Foundation
| # | Check | Status |
|---|-------|--------|
| 1 | All 5 tenant tables exist in DB | |
| 2 | tenant_id column on all 7 domain tables | |
| 3 | All 7 enum types exist | |
| 4 | price_observations table dropped | |

### Phase 1 — Tenant Lifecycle
| # | Check | Status |
|---|-------|--------|
| 5 | Registration auto-provisions tenant with seed data | |
| 6 | OAuth login auto-provisions tenant | |
| 7 | Auth status returns tenant + membership role | |
| 8 | Single-tenant user auto-sets context (no selection page) | |
| 9 | Multi-tenant user sees tenant selection page | |
| 10 | Invitation send + accept flow works | |
| 11 | Duplicate invitation blocked | |
| 12 | Invitation revoke works | |
| 13 | Change member role works | |
| 14 | Transfer ownership works | |
| 15 | Cannot remove owner / invite as owner | |
| 16 | Tenant switching works between 2+ tenants | |

### Phase 2 — Data Scoping
| # | Check | Status |
|---|-------|--------|
| 17 | Cross-tenant expense isolation (list + direct access) | |
| 18 | Cross-tenant receipt isolation | |
| 19 | Cross-tenant category/payer isolation | |
| 20 | Global catalog (suppliers, stores, articles) shared | |
| 21 | User settings scoped per (tenant, user) | |

### Phase 3 — Access Control
| # | Check | Status |
|---|-------|--------|
| 22 | Owner: full access to all features | |
| 23 | Admin: full access (same as owner for data ops) | |
| 24 | Member: read + write expenses/receipts, no admin ops | |
| 25 | Viewer: read-only everywhere, all writes return 403 | |
| 26 | Receipt visibility: owner/admin see all, member/viewer see own | |
| 27 | Export requires read role | |

### Phase 4 — Platform Admin
| # | Check | Status |
|---|-------|--------|
| 28 | Admin blocked from tenant expense data (403) | |
| 29 | Admin tenant management (list, detail, members) works | |
| 30 | Impersonation grant → activate → access → deactivate flow | |
| 31 | Impersonation revoke (owner-side) auto-deactivates | |
| 32 | Audit trail for all impersonation operations | |

### Phase 5 — Frontend
| # | Check | Status |
|---|-------|--------|
| 33 | Tenant switcher visible for multi-tenant users, hidden for single | |
| 34 | Slug displayed in URL bar | |
| 35 | Role badges consistent (owner/admin/member/viewer) | |
| 36 | Sidebar sections gated by role | |
| 37 | Settings page read-only for non-writers | |
| 38 | Members page gated to admin/owner | |
| 39 | Impersonation grants page gated to owner | |

### Phase 6 — Hardening
| # | Check | Status |
|---|-------|--------|
| 40 | Expense create/update works (price_observations removed cleanly) | |
| 41 | Rate limit keys contain `:t:{tenant-id}` for user API | |
| 42 | Admin rate limit keys do NOT contain tenant-id | |
| 43 | Provisioning rate limit blocks after 5 tenants/IP/hour | |

### Automated Tests
| # | Check | Status |
|---|-------|--------|
| 44 | Backend: no new failures (39 errors, 5 failures baseline) | |
| 45 | Tenant-specific test suites: all pass | |
| 46 | Frontend: ~401 tests, 0 failures | |
