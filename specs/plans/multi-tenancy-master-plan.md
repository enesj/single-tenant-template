# Multi-Tenancy Implementation — Master Plan

> **Spec**: `specs/allium/template/multi-tenancy.candidate.allium`
> **Approach**: shared-schema, tenant_id FK, middleware enforcement (no RLS)
> **Status**: Phase 5 — Complete (5a essential flow + 5b impersonation UI, admin tenants, slug display, role gating)

---

## Phase Overview


| # | Phase | Depends On | Status |
|---|-------|-----------|--------|
| 0 | Foundation — DB tables & migrations | — | `[x]` |
| 1 | Tenant Lifecycle — provisioning, invitations, session context | 0 | `[x]` |
| 2 | Tenant Data Scoping — tenant_id threading, middleware filtering | 0 | `[x]` |
| 3 | Access Control — role-based tier rules | 1, 2 | `[x]` |
| 4 | Platform Admin — blocked resources, superpower CRUD, impersonation | 3 | `[x]` |
| 5 | Frontend — tenant switcher, member mgmt, UI state | 1, 3 | `[x]` |
| 6 | Cleanup & Hardening — per-tenant rate limiting, delete price_observations | 3 | `[ ]` |

```
Phase 0 ──┬──→ Phase 1 ──┬──→ Phase 3 ──┬──→ Phase 4
           │              │              ├──→ Phase 5
           └──→ Phase 2 ──┘              └──→ Phase 6
```

---

## Reality Check Decisions (post-exploration)

> These decisions were made after auditing the actual codebase against the Allium spec.

1. **Aliases stay globally unique** — `supplier_aliases`, `store_aliases`, `article_aliases` keep their existing UNIQUE constraints on `raw_label_normalized`. No `tenant_id` added to alias tables. Aliases are global shared intelligence (like suppliers/stores), not tenant-scoped. **Spec deviation**: the Allium spec says aliases are tenant-scoped; this simplification keeps OCR pipeline untouched.
2. **Server-side session store** — Switch from cookie-backed to server-side (DB or in-memory) to avoid cookie size limits when adding tenant context.
3. **Direct `tenant_id` on `expense_items`** — Even though it CASCADE-deletes from `expenses`, having direct `tenant_id` simplifies service factory queries, audit queries, and future RLS.
4. **Admin `owner` role = super_admin** — Use existing `:owner` admin role for superpower operations. No new DB enum value needed. The code-level `:super_admin` key maps to `:owner` in the DB.
5. **Session keys already exist** — Middleware already reads `[:session :tenant-id]` and `[:session :auth-session :tenant]`. Frontend app-db has `:session :tenant` slot. We populate these, not invent them.
6. **Rate limiting is in-memory** — Per-tenant rate limiting will use the existing `ConcurrentHashMap`. No Redis for now (known limitation for multi-instance).

---

## Phase 0: Foundation — DB Tables & Migrations

**Goal**: Create the new tables and columns that every subsequent phase depends on.
**Allium refs**: Entity declarations (Tenant, TenantMembership, TenantInvitation, ImpersonationGrant).

### Tasks

- [x] **0.1** Define new PostgreSQL ENUM types in `resources/db/template/models.edn`:
  - `:tenant-status` → `["active" "suspended" "archived"]`
  - `:membership-role` → `["owner" "admin" "member" "viewer"]`
  - `:membership-status` → `["active" "suspended"]`
  - `:invitation-status` → `["pending" "accepted" "expired" "revoked"]`
  - `:invitation-role` → `["admin" "member" "viewer"]`
  - `:impersonation-role` → `["owner" "admin" "member" "viewer"]`
  - `:impersonation-status` → `["active" "revoked"]`
- [x] **0.2** Create `tenants` table in `resources/db/template/models.edn`:
  - id (UUID PK), name (text), slug (varchar, unique), status (enum: tenant-status), created_at, updated_at
- [x] **0.3** Create `tenant_memberships` table in `resources/db/template/models.edn`:
  - id (UUID PK), tenant_id (FK tenants, cascade), user_id (FK users, cascade), role (enum: membership-role), status (enum: membership-status), invited_by (FK users, nullable, set-null), created_at, updated_at
  - Unique index: (tenant_id, user_id)
- [x] **0.4** Create `tenant_invitations` table in `resources/db/template/models.edn`:
  - id (UUID PK), tenant_id (FK tenants, cascade), email (text), role (enum: invitation-role), invited_by (FK users, cascade), status (enum: invitation-status), token (varchar, unique), expires_at (timestamptz), created_at, updated_at
- [x] **0.5** Create `impersonation_grants` table in `resources/db/template/models.edn`:
  - id (UUID PK), tenant_id (FK tenants, cascade), admin_id (FK admins, cascade), granted_by (FK users, cascade), role (enum: impersonation-role), status (enum: impersonation-status), revoked_by_admin (FK admins, nullable, set-null), revoked_by_user (FK users, nullable, set-null), created_at, updated_at
- [x] **0.6** Add `tenant_id` (FK tenants, NOT NULL) to tenant-scoped tables in `resources/db/domain/models.edn`:
  - Transactional: `expenses`, `expense_items`, `receipts`, `payers`, `user_expense_settings`
  - Lookup: `payer_types`, `expense_categories`
  - Add tenant_id index on each table
  - Updated unique indexes to be tenant-scoped: `payer_types(label)`, `expense_categories(name)`, `receipts(file_hash)`
  - Restructured `user_expense_settings`: new id PK, (tenant_id, user_id) unique constraint
- [x] **0.7** Switch to server-side session store
  - Created `user_sessions` table: id (UUID PK), session_key (varchar, unique), data (jsonb), expires_at (timestamptz), created_at, updated_at
- [x] **0.8** Delete `price_observations` table (drop migration)
- [x] **0.9** Run `(mig/make-all-migrations!)` and `(mig/migrate!)` to generate and apply
  - Migration `0099_schema.edn` generated with 49 actions
  - Applied to both dev (port 55432) and test (port 55433) databases
- [x] **0.10** Tenant provisioning defaults defined in `config/base.edn` under `:tenant-defaults`
  - 4 payer types: Cash (default), Credit Card, Debit Card, Bank Transfer
  - 8 expense categories: Groceries, Transport, Utilities, Dining, Healthcare, Entertainment, Shopping, Other
  - Provisioning service (Phase 1) reads this config and INSERTs per-tenant rows

### Notes
- Aliases (`supplier_aliases`, `store_aliases`, `article_aliases`) are NOT modified — stay global.
- Since there's no production data, `tenant_id NOT NULL` can be added directly without backfill.
- ENUM types for `membership-role` and `impersonation-role` share the same values but are separate types (different semantic meaning, may diverge later).

---

## Phase 1: Tenant Lifecycle — Provisioning, Invitations, Session Context

**Goal**: Users can create tenants (auto on first login), invite others, manage members, and switch between tenants.
**Allium refs**: `CreateTenantOnFirstLogin`, `SeedTenantLookupData`, `AutoSetSessionAfterProvisioning`, `JoinTenantFromInvitation`, `InviteUserToTenant`, `RevokeInvitation`, `ChangeTenantMemberRole`, `TransferTenantOwnership`, `RemoveUserFromTenant`, `AutoSetTenantAfterLogin`, `SetActiveTenantFromSwitcher`, `ResolveTenantFromSlug`, `RejectInvalidTenantSlug`.

**Pre-wired**: Middleware already reads `[:session :tenant-id]`. Auth status endpoint returns `:tenant`. Frontend has `:session :tenant` slot.

### Tasks

- [x] **1.1** Tenant provisioning service: auto-create tenant + owner membership on first login
  - Hook into post-authentication flow (password login, registration, OAuth callback)
  - Generate URL slug from email prefix (handle collisions with suffix)
  - Seed payer_types + expense_categories from template into new tenant
  - Set session `:tenant-id` and `:auth-session :tenant` after creation
- [x] **1.2** Populate session tenant context in existing auth flow
  - On login (password + OAuth): lookup memberships, auto-set if single tenant
  - On login with multiple memberships: signal tenant selection required
  - Populate `[:session :tenant-id]` and `[:session :auth-session :tenant {:id :name :slug}]`
  - Update `/auth/status` endpoint to return tenant + membership info
- [ ] **1.3** Tenant slug resolution middleware: `/t/{slug}/*` → set/switch session tenant *(deferred — using session-based switching)*
  - Verify membership exists and is active
  - Switch session if navigating to a different tenant
  - Return 404 for unknown slug, 403 for no membership
- [x] **1.4** Invitation flow (backend)
  - Create invitation endpoint (owner/admin only, can't invite as owner)
  - Guard: no pending invitation for same email+tenant, no existing active membership
  - Accept invitation endpoint (token-based, creates membership)
  - Revoke invitation endpoint (owner/admin)
  - Expiration: 7 days
- [x] **1.5** Member management (backend)
  - Change role endpoint (owner guards for admin/owner targets)
  - Transfer ownership endpoint (owner → admin promotion, self-demotion)
  - Remove member endpoint (can't remove owners)
- [x] **1.6** Tenant switcher endpoint: `POST /api/v1/tenant/switch` (sets session context)
- [x] **1.7** Integration tests for provisioning, invitation, and member management flows

### Dependencies
- Phase 0 complete (tables exist)

### Risk
- Slug generation must handle Unicode names gracefully (email prefix is safest)
- Server-side session must be wired before tenant context can be reliably stored

---

## Phase 2: Tenant Data Scoping — Tenant_id Threading & Middleware Filtering

**Goal**: All tenant-scoped queries filter by `tenant_id`. Global catalog queries pass through unfiltered.
**Allium refs**: Data Scoping Classification, tenant isolation guarantee comment.

**Pattern**: Follow existing `user-id` threading pattern (explicit parameter through service wrappers), not dynamic middleware injection.

### Tasks

- [x] **2.1** Tenant-aware request context extraction
  - Added `get-tenant-id` to `handlers/user_expenses/helpers.clj`
  - `routes/utils.clj` `extract-common-data` already extracts `tenant-id` (done in Phase 1)
  - All user handlers extract tenant-id and pass to service calls
- [x] **2.2** Update generic CRUD service to thread tenant_id
  - `services_factory.clj`: all build-* functions accept optional `:tenant-id` in opts
  - `config_maps.clj`: added `:tenant-scoped? true` to payer, payer-type, expense-category, expense, expense-item, receipt configs
  - Factory auto-injects `[:= :tenant_id tenant-id]` into WHERE and validates tenant_id on create
- [x] **2.3** Update expense service to accept and thread tenant_id
  - `expenses.clj`: `create-expense!` includes tenant_id in expense row + expense_items
  - `user_expenses.clj`: all functions take `tenant-id` param after `db`, thread to admin service
  - Handlers: `crud.clj`, `summary.clj`, `batch.clj` extract and pass tenant-id
- [x] **2.4** Update receipt service to thread tenant_id
  - `receipts/queries.clj`: all queries accept optional tenant-id
  - `receipts/approval.clj`: reads tenant_id from receipt row, passes to expense creation
  - `receipt_upload.clj`: includes tenant_id in INSERT
  - `user_receipts.clj`: all handlers extract and pass tenant-id
- [x] **2.5** Update payer/payer_types/expense_categories services for tenant_id
  - `payers.clj`: `get-default-payer`, `set-default-payer-in-tx!` scoped by tenant
  - `payer_types.clj`: same pattern for default payer types
  - `reference_data.clj`: all payer/payer-type handlers pass tenant-id
  - Expense categories: handled via factory `:tenant-scoped? true`
- [x] **2.6** Update user_expense_settings for tenant_id
  - `get-user-expense-settings`: scopes by (tenant_id, user_id) when tenant-id present
  - `upsert-user-expense-settings!`: ON CONFLICT (tenant_id, user_id), includes id in INSERT
  - `settings.clj` handler: extracts and passes tenant-id
- [x] **2.7** Verify global services pass through WITHOUT tenant filtering
  - suppliers, stores: no tenant_id (global catalog, confirmed no change)
  - articles, categories, subcategories, manufacturers: confirmed no change
  - supplier_aliases, store_aliases, article_aliases: confirmed no change (stay global)
  - cities: confirmed no change
  - `expense_items.clj`: raw SQL queries scope via expenses.tenant_id JOIN
- [x] **2.8** Integration tests: verify tenant isolation
  - `tenant_isolation_test.clj`: 6 tests (payers, default-payer, payer-types, settings, expenses, global-catalog)
  - Updated `test_helpers.clj`: `ensure-test-tenant!`, `ensure-test-user!`, tenant-aware `create-payer!`
  - Updated `payers_test.clj`, `receipts_test.clj`, `user_expenses_test.clj` for tenant_id
  - Updated `user_pagination_envelopes_test.clj` for tenant-id in count opts

### Dependencies
- Phase 0 complete (tenant_id columns exist)

### Risk
- Missing a single query path = data leak. Audit every service call site.
- OCR pipeline: suppliers/stores/aliases are all global now, so OCR needs NO changes. But receipt creation during OCR must include tenant_id.

---

## Phase 3: Access Control — Role-Based Tier Rules

**Goal**: Enforce the 4-tier role model (owner/admin/member/viewer) for all resource access.
**Allium refs**: All `Allow*` and `Deny*` rules in the Tenant-Scoped Data Access Rules section, config resource sets.

**Approach**: Handler-level `ensure-role` checks using role-set constants — no centralized middleware or config registry needed. The existing pattern is explicit, auditable, and covers all cases. Resource classification is implicit in the role-set each handler checks against.

### Tasks

- [x] **3.1** Fix critical bug: `get-user-role` reads wrong role
  - **Was**: reading `[:session :auth-session :user :role]` (global user role from `users` table)
  - **Now**: reads `[:session :auth-session :membership :role]` first (tenant membership role), falls back to user role for backward compatibility
  - Single fix cascades to all 60+ `ensure-role` call sites
  - Added `tenant-elevated?` helper for admin/owner branching in receipt handlers
  - **File**: `handlers/user_expenses/helpers.clj`
- [x] **3.2** Add missing role check to export handler
  - `export-expenses-handler` had no `ensure-role` — any authenticated user bypassed role enforcement
  - Added `h/ensure-role request h/expenses-read-roles` gate
  - **File**: `handlers/user_expenses/settings.clj`
- [x] **3.3** Relax list permissions for tenant lookup resources (read: all roles)
  - `expense_items`: `power-user-roles` → `h/expenses-read-roles`
  - `expense_categories`: `ensure-admin-or-owner` → `h/reference-data-read-roles`
  - Write operations stay admin/owner — correct per `DenyMemberTenantAdmin`
  - **Files**: `handlers/user_expenses/expense_items.clj`, `handlers/user_expense_categories.clj`
- [x] **3.4** Relax list permissions for alias resources (read: all roles)
  - `supplier_aliases`, `store_aliases`: `power-user-roles`/`ensure-admin-or-owner` → `h/reference-data-read-roles`
  - Write operations stay admin/owner — correct per `AllowTenantAliasWrite`
  - **Files**: `handlers/user_expenses/supplier_aliases.clj`, `handlers/user_store_aliases.clj`
- [x] **3.5** Relax list permissions for global catalog resources (read: all roles)
  - `stores`, `categories`, `subcategories`, `articles` (list + unmapped), `manufacturers`: all `ensure-admin-or-owner` → `h/reference-data-read-roles`
  - Write operations stay admin/owner
  - **NOT changed**: `user_cities.clj` — cities are `global_geo_resources`, spec restricts read to admin/owner
  - **Files**: `handlers/user_stores.clj`, `handlers/user_categories.clj`, `handlers/user_subcategories.clj`, `handlers/user_articles.clj`, `handlers/user_manufacturers.clj`
- [x] **3.6** Fix receipt handler admin-role branching
  - 10 occurrences of `(= "admin" role)` replaced with `(h/tenant-elevated? request)`
  - owner/admin see all receipts within their tenant (tenant-scoped, not global)
  - member/viewer see only their own receipts
  - Removed unused `role` local bindings from all receipt handlers
  - **File**: `handlers/user_receipts.clj`
- [x] **3.7** Unit tests for role enforcement
  - 15 tests, 29 assertions — all pure functions, no DB required
  - Tests: membership role priority, fallback to user role, `tenant-elevated?`, all Allow/Deny rule combinations, 403 status code verification
  - **File**: `test/app/domain/backend/expenses/handlers/user_expenses/role_access_test.clj`

### Deny Rules Verification

| Rule | Status | Enforcement |
|------|--------|-------------|
| **DenyViewerWrites** | Enforced | No write role set includes "viewer" — after `get-user-role` reads membership role, viewers get 403 on all write endpoints |
| **DenyMemberTenantAdmin** | Enforced | All admin-only handlers (expense_categories write, aliases write) use `#{"admin" "owner"}` — members excluded |
| **DenyPlatformAdminUserData** | Architectural | Admin auth (`/admin/api`) and user auth (`/api/v1`) are separate middleware chains. Full enforcement deferred to Phase 4 |

### What Was NOT Done (from original plan)
- **No centralized resource classification config** — handler-level role-set constants serve the same purpose with less indirection
- **No centralized authorization middleware** — `ensure-role` pattern is lightweight and explicit at each handler
- **No full role × resource × action matrix integration tests** — unit tests cover all role-set combinations; integration tests deferred to Phase 4 when impersonation adds complexity

### Dependencies
- Phase 1 (memberships exist and session context works)
- Phase 2 (tenant_id scoping active)

---

## Phase 4: Platform Admin — Blocked Resources, Superpower CRUD, Impersonation

**Goal**: Platform admins get their adjusted access: blocked from tenant expense data unless impersonating, superpower on users/memberships.
**Allium refs**: `PlatformAdminDataRequest`, `AllowPlatformAdminAccessOutsideBlockedResources`, `DenyPlatformAdminAccessToBlockedResources`, `GrantImpersonation`, `RevokeImpersonation*`, `ImpersonatedAccessGranted`.

**Admin `owner` role** = super_admin. No new DB enum needed.

### Tasks

- [x] **4.1** Update admin authorization middleware
  - Added `impersonation_context` jsonb column to `admin_sessions` (migration 0100)
  - `get-admin-by-session-with-context` in `admin/auth.clj` loads impersonation context in single JOIN
  - `wrap-admin-authentication` in `middleware/admin.clj` attaches `:impersonation` to request
  - `wrap-require-impersonation` in `domain/expenses/routes/middleware.clj` returns 403 for blocked resources
  - Applied to 7 entity configs in `route_configs.clj` + receipts custom routes
  - **Files**: `admin/auth.clj`, `middleware/admin.clj`, `expenses/routes/middleware.clj`, `route_configs.clj`, `receipts.clj`, `models.edn`
- [x] **4.2** Superpower CRUD for users and tenant_memberships
  - Admin tenant routes: `routes/admin/tenants.clj` — list/detail/members at `/admin/api/tenants`
  - Membership management: change role (`PUT /:id/members/:member-id/role`), remove (`DELETE /:id/members/:member-id`)
  - Write ops guarded by `wrap-admin-role :owner` — only platform admin owners
  - Guards: cannot change owner role, cannot remove owner
  - User detail enrichment: `GET /admin/api/users/:id` now includes `:memberships` array
  - No force-add memberships (spec: "Cannot force-add users to tenants")
  - **Files**: `routes/admin/tenants.clj` (new), `routes/admin/users.clj` (modified), `routes/admin_api.clj` (modified)
  - **Tests**: `test/routes/admin/tenants_test.clj` — 11 tests (listing, detail, members, role change, removal, search, enrichment)
- [x] **4.3** Impersonation grant management
  - Service: `services/impersonation.clj` — create, revoke (by owner/admin), find, list grants
  - User-side routes: `routes/impersonation.clj` — GET/POST/DELETE at `/api/v1/tenant/impersonation-grants` (owner only)
  - Admin-side routes: `routes/admin/impersonation.clj` — status, grants list, activate, deactivate, self-revoke at `/admin/api/impersonation`
  - Guard: one active grant per admin+tenant; rejects duplicate
  - **Files**: `services/impersonation.clj`, `routes/impersonation.clj`, `routes/admin/impersonation.clj`, `routes/tenant.clj`, `routes/admin_api.clj`
- [x] **4.4** Impersonation session flow
  - `activate-impersonation!` validates grant active + admin match → writes context to `admin_sessions.impersonation_context` jsonb
  - `deactivate-impersonation!` clears context from all admin sessions
  - Revoke (by owner or admin) auto-calls `deactivate-impersonation!`
  - Context stored as `{"tenant-id": "...", "role": "...", "grant-id": "..."}`
  - **File**: `services/impersonation.clj`
- [x] **4.5** Audit logging for impersonation
  - All grant operations audit-logged: `impersonation_granted`, `impersonation_revoked_by_owner`, `impersonation_revoked_by_admin`, `impersonation_activated`, `impersonation_deactivated`
  - Uses existing `audit/log-audit!` with entity-type `"impersonation_grant"` or `"admin_session"`
  - **File**: `services/impersonation.clj`
- [x] **4.6** Integration tests
  - `impersonation_test.clj`: 8 service tests (create, duplicate rejection, unknown admin, revoke by owner/admin, activate/deactivate context, list grants) — 8 tests, 13 assertions
  - `blocked_resources_test.clj`: 3 middleware unit tests (blocks without context, allows with context, blocks nil context) — 3 tests, 5 assertions
  - End-to-end REPL verification: 403 → activate → 200 → deactivate → 403
  - **Files**: `test/services/impersonation_test.clj`, `test/routes/blocked_resources_test.clj`

### Dependencies
- Phase 3 (role-based access rules exist to evaluate against)

---

## Phase 5: Frontend — Tenant Switcher, Member Management, UI State

**Goal**: SPA supports multi-tenant navigation, tenant switcher, member management, and role-aware UI.
**Allium refs**: Surfaces (TenantManagementBoundary, TenantMembersView, TenantSwitcher, PlatformTenantAdminView), hybrid routing rules.

**Pre-wired**: App-db has `:session :tenant`. Auth status returns `:tenant`. Two Shadow-CLJS builds (`:app`, `:admin`).

### Phase 5a — Essential Multi-Tenant Flow (COMPLETE ✅)

- [x] **5.3** Tenant context in re-frame app-db
  - Auth status stores `membership-role`, `tenant-selection-required`, `available-tenants`
  - `:user-role` subscription reads membership role (not global user role)
  - `:available-tenants`, `:tenant-selection-required?` subscriptions added
  - Load on app init via `fetch-auth-status`
- [x] **5.4** Tenant switcher component
  - Sidebar footer dropdown showing tenant name + role for each membership
  - On select: POST to `/api/v1/tenant/switch`, update app-db + session, reload page
  - Shows swap icon when user has multiple tenants; hidden for single-tenant users
- [x] **5.5** Member management UI (tenant admin/owner)
  - Members page at `/tenant/members`: table with name, email, role, joined date
  - Invite user form (email + role picker: viewer/member/admin)
  - Revoke invitation with confirmation dialog
  - Change role / remove member / transfer ownership actions
  - Role badges (owner=primary, admin=secondary, member=accent, viewer=ghost)
- [x] **5.10** Tenant selection page
  - Full-page tenant selection at `/tenant-select` for users with multiple tenants
  - Centered card layout with tenant cards + role badges + click-to-select
  - Shown after OAuth/login when `tenant-selection-required: true`
- [x] **5.11** Invitation accept page
  - At `/invitation/accept?token=...` — accepts invitation, auto-switches to joined tenant
- [x] **5.7** Role-aware UI gating (partial)
  - Sidebar sections visible based on membership role (owner sees all, viewer sees minimal)
  - Members page gated to admin/owner

#### Bug fixes applied during 5a:
- **Fix 8**: OAuth callback EDN serialization — wrapped `build-auth-session` in `sanitize-for-serialization`
- **Fix 9**: Muuntaja conflict — replaced `json-response` with `response/response` in tenant routes
- **Fix 10**: Namespaced keys — added `(or (:key x) (:ns/key x))` fallbacks in frontend
- **Fix 11**: SQL JOIN alias keys — added `user_full_name`/`user_email` to member helpers
- **Fix 12**: Auth component role badge — switched to `:user-role` subscription
- **Fix 13**: `session-expired?` nil handling — return `false` when no `:expires-at`
- **Fix 14**: Password hash leak — `dissoc` password hash keys in auth status
- **Fix 15**: `java.time.Instant` in `tenant.clj` sanitize — caused 500 on tenant switch
- **Fix 16**: Namespaced keys in session tenant map — `build-auth-session` now normalizes tenant via `normalize-tenant`; `get-tenant-id` fallbacks added to 3 call sites

### Phase 5b — Impersonation UI, Admin Tenants, Slug Display, Role Gating (COMPLETE ✅)

- [x] **5.1** Backend SPA fallback for `/t/*` (slug routing)
  - Added `/t/:slug/*path` catch-all → `render-page` in `routes.clj`
  - Added `/tenant/impersonation` SPA fallback
- [x] **5.2** Frontend router prefix handling (slug routing)
  - Lightweight slug display: `/t/{slug}/...` in URL bar for context, session stays source of truth
  - `strip-slug-prefix` in `routes.cljs` strips prefix before reitit matches
  - `tenant-href` helper in layout prepends slug to sidebar links
  - `replaceState` updates URL bar after `push-state` for cosmetic slug display
  - Slug set on auth status load + tenant switch success
- [x] **5.6** Impersonation management UI (tenant owner)
  - Events: `events/impersonation.cljs` — fetch/create/revoke grants + subs
  - Page: `pages/impersonation_grants.cljs` — owner guard, grants table, create form, inline revoke confirm
  - Wired: route `/tenant/impersonation`, page init event, sidebar link (owner only)
- [x] **5.7** Role-aware UI gating (complete)
  - Settings page: read-only banner + disabled inputs for non-writers
  - Receipt approval: already gated by `:expenses/receipts.approve` capability
  - Upload page: already gated by `:expenses/upload` capability
  - Dashboard quick actions: already uses `can?` checks
- [x] **5.8** Platform admin tenant view
  - Events: `admin/frontend/events/tenants.cljs` — list/search/detail/members/role-change/remove
  - Page: `admin/frontend/pages/tenants.cljs` — list view (search + status filter + table) + detail view (tenant info + members)
  - Wired: admin route `/tenants`, admin sidebar link
  - Write ops gated by admin `:owner` role in UI
- [x] **5.9** Frontend tests (ClojureScript)
  - `test/app/template/frontend/events/tenant_test.cljs` — 11 tests (switch, memberships, members, invite, role change, remove, slug, subscriptions)
  - `test/app/template/frontend/events/impersonation_test.cljs` — 7 tests (fetch/create/revoke grants, clear messages)
  - `test/app/admin/frontend/events/tenants_test.cljs` — 10 tests (fetch/search/detail/members, role change, remove)

### Dependencies
- Phase 1 (session context + switching works)
- Phase 3 (access control determines what to show/hide)

---

## Phase 6: Cleanup & Hardening

**Goal**: Per-tenant rate limiting, remove deprecated code, final polish.
**Allium refs**: Rate limiting config, resolved decisions.

### Tasks

- [ ] **6.1** Per-tenant rate limiting for user API
  - Rate limit key: `tenant_id:ip` instead of just `ip`
  - Config: limits per tenant (requests/minute, etc.)
  - Admin API retains global rate limiting
  - Storage: in-memory ConcurrentHashMap (no Redis for now)
- [ ] **6.2** Tenant provisioning rate limiting
  - Prevent signup abuse (limit tenant creation per IP/email)
- [ ] **6.3** Remove price_observations references from code
  - Remove service/handler/frontend code
  - Update entity specs, admin routes, config maps
- [ ] **6.4** Clean up single-tenant assumptions
  - Remove hardcoded single-tenant defaults in middleware
  - Remove any `tenant_id = nil` fallback paths
  - Ensure all queries either scope by tenant or are explicitly global
- [ ] **6.5** End-to-end smoke test
  - Register → tenant created → invite → accept → switch → data isolation verified
- [ ] **6.6** Security audit checklist
  - [ ] No query path missing tenant_id filter for scoped resources
  - [ ] No cross-tenant data leak in any API endpoint
  - [ ] Impersonation audit trail complete
  - [ ] Platform admin blocked from expense data without impersonation
  - [ ] Rate limiting active per-tenant

### Dependencies
- Phase 3 (access control in place)

---

## Working Notes

_Use `scratch-pad` tool during implementation sessions for live phase progress. Flush completed items back to this document's checkboxes when a phase is done._

### Key Files
- **Model definitions**: `resources/db/template/models.edn` (platform tables), `resources/db/domain/models.edn` (domain tables)
- **Migration REPL**: `src/app/template/backend/migrations/simple_repl.clj` — `(mig/make-all-migrations!)`, `(mig/migrate!)`
- **Middleware**: `src/app/template/backend/middleware/user.clj` (already reads tenant-id from session)
- **Entity access**: `src/app/template/backend/security/entity_access.clj` (deny-by-default allowlist)
- **CRUD dispatch**: `src/app/template/backend/routes/utils.clj` (extracts tenant-id, line 96)
- **Service factory**: `src/app/domain/backend/expenses/services/services_factory.clj`
- **Service configs**: `src/app/domain/backend/expenses/services/service_configs/config_maps.clj` (19 entities)
- **User expense wrapper**: `src/app/domain/backend/expenses/services/user_expenses.clj` (pattern for tenant threading)
- **Auth routes**: `src/app/template/backend/routes/auth.clj` (login/register handlers, session population)
- **Auth status**: returns `:tenant` already — frontend pre-wired
- **Frontend defaults**: `src/app/template/frontend/db/defaults.cljs` (`:session :tenant` slot exists)
- **Frontend routes**: `src/app/domain/shared/routes/expenses_user.cljc` (canonical SPA route descriptors)
- **Domain registry**: `src/app/domain/backend/registry.clj` (manifests with `:spa-routes`)
- **Rate limiting**: `src/app/template/backend/middleware/rate_limiting.clj` (in-memory, IP-based)
