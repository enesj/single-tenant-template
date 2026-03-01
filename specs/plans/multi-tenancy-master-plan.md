# Multi-Tenancy Implementation — Master Plan

> **Spec**: `specs/allium/template/multi-tenancy.candidate.allium`
> **Approach**: shared-schema, tenant_id FK, middleware enforcement (no RLS)
> **Status**: Phase 2 — Complete (1.3 slug routing deferred)

---

## Phase Overview

| # | Phase | Depends On | Status |
|---|-------|-----------|--------|
| 0 | Foundation — DB tables & migrations | — | `[x]` |
| 1 | Tenant Lifecycle — provisioning, invitations, session context | 0 | `[x]` |
| 2 | Tenant Data Scoping — tenant_id threading, middleware filtering | 0 | `[x]` |
| 3 | Access Control — role-based tier rules | 1, 2 | `[ ]` |
| 4 | Platform Admin — blocked resources, superpower CRUD, impersonation | 3 | `[ ]` |
| 5 | Frontend — tenant switcher, slug routing, UI state | 1, 3 | `[ ]` |
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

### Tasks

- [ ] **3.1** Define resource classification config (maps to Allium config block)
  - expense_write_resources, expense_read_resources
  - tenant_lookup_resources
  - tenant_admin_write_resources
  - global_catalog_resources, global_catalog_creatable
  - global_geo_resources
  - platform_admin_blocked_resources
- [ ] **3.2** Tenant data authorization middleware
  - Input: user, tenant, resource, action (read/write)
  - Lookup membership → check role against resource tier
  - Return grant/deny with reason
- [ ] **3.3** Implement expense tier rules
  - Read: all roles (viewer+), includes payers
  - Write (expenses, expense_items, receipts): member+
  - Payers write: admin/owner
- [ ] **3.4** Implement lookup tier rules
  - payer_types, expense_categories: read all, write admin/owner
- [ ] **3.5** Implement admin/owner full-write rule
  - All tenant_admin_write_resources: write for admin/owner
- [ ] **3.6** Implement global catalog rules
  - Read: all tenant members
  - Create suppliers/stores: member+
  - Create articles/categories/etc: platform admin only
- [ ] **3.7** Implement geo rules
  - Read cities/countries: admin/owner only
- [ ] **3.8** Deny rules
  - No active membership → deny
  - Write to read-only resource for role → deny with reason
  - Geo read for member/viewer → deny
- [ ] **3.9** Integration tests for all tier combinations (role × resource × action matrix)

### Dependencies
- Phase 1 (memberships exist and session context works)
- Phase 2 (tenant_id scoping active)

---

## Phase 4: Platform Admin — Blocked Resources, Superpower CRUD, Impersonation

**Goal**: Platform admins get their adjusted access: blocked from tenant expense data unless impersonating, superpower on users/memberships.
**Allium refs**: `PlatformAdminDataRequest`, `AllowPlatformAdminAccessOutsideBlockedResources`, `DenyPlatformAdminAccessToBlockedResources`, `GrantImpersonation`, `RevokeImpersonation*`, `ImpersonatedAccessGranted`.

**Admin `owner` role** = super_admin. No new DB enum needed.

### Tasks

- [ ] **4.1** Update admin authorization middleware
  - Check resource against `platform_admin_blocked_resources`
  - If blocked → return 403 with "requires impersonation" message
  - If not blocked → allow (existing admin auth flow)
- [ ] **4.2** Superpower CRUD for users and tenant_memberships
  - Admin API endpoints for user management across tenants
  - Admin API endpoints for membership management across tenants
  - Bypasses tenant rule chains (direct DB access via admin auth)
  - Cannot: change owner roles, remove owners, force-add memberships (invitation only)
  - Admin `owner` role required for superpower operations
- [ ] **4.3** Impersonation grant management
  - Endpoint for tenant owner to create grant (tenant, admin, role)
  - Guard: only one active grant per admin per tenant
  - Endpoint for tenant owner to revoke grant
  - Endpoint for platform admin to end their own impersonation
- [ ] **4.4** Impersonation session flow
  - Admin activates impersonation → session gains synthetic tenant context
  - All tenant data rules evaluate against the granted role
  - Actions audit-logged with both admin and impersonated context
- [ ] **4.5** Audit logging for impersonation
  - Log: grant creation, grant revocation, every data access during impersonation
  - Include: admin identity, tenant, effective role, action, resource
- [ ] **4.6** Integration tests
  - Admin blocked from expenses without impersonation
  - Admin can access expenses with active impersonation grant
  - Impersonation respects granted role (not admin's own role)
  - Revocation immediately ends access

### Dependencies
- Phase 3 (role-based access rules exist to evaluate against)

---

## Phase 5: Frontend — Tenant Switcher, Slug Routing, UI State

**Goal**: SPA supports multi-tenant navigation with slug URLs, tenant switcher, and role-aware UI.
**Allium refs**: Surfaces (TenantManagementBoundary, TenantMembersView, TenantSwitcher, PlatformTenantAdminView), hybrid routing rules.

**Pre-wired**: App-db has `:session :tenant`. Auth status returns `:tenant`. Two Shadow-CLJS builds (`:app`, `:admin`).

### Tasks

- [ ] **5.1** Backend SPA fallback for `/t/*`
  - Add `/t/*` catch-all route → `render-page` in `routes.clj`
  - Must come before the generic catch-all but after API routes
- [ ] **5.2** Frontend router prefix handling
  - reitit-frontend router: parse `/t/{slug}` prefix, pass slug as route parameter
  - Strip prefix before matching inner routes (expenses, receipts, etc.)
  - Update domain route descriptors in `expenses_user.cljc` for prefix awareness
- [ ] **5.3** Tenant context in re-frame app-db
  - Populate `:active-tenant` (id, name, slug), `:active-membership` (id, role) from auth status
  - `:available-tenants` (list for switcher) — new field
  - Load on app init / after login
- [ ] **5.4** Tenant switcher component
  - Dropdown/modal showing tenant name + role for each membership
  - On select: POST to switch endpoint, update app-db, navigate to `/t/{new-slug}/...`
  - Show when user has multiple tenants
- [ ] **5.5** Member management UI (tenant admin/owner)
  - Invite user form (email + role picker, no owner option)
  - Members list with role badges
  - Change role / remove member / transfer ownership actions
- [ ] **5.6** Impersonation management UI (tenant owner)
  - List active impersonation grants
  - Create grant form (select platform admin + role)
  - Revoke grant action
- [ ] **5.7** Role-aware UI gating
  - Hide write actions from viewers
  - Hide admin-only sections from member/viewer
  - Hide geo sections from member/viewer
- [ ] **5.8** Platform admin tenant view
  - Tenant list with stats
  - User/membership management across tenants
  - Impersonation activation/deactivation
- [ ] **5.9** Frontend tests (ClojureScript)

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
