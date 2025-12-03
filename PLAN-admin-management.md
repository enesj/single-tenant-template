# Admin Management Page Implementation Plan

## Overview

Add an "Admins" page to the admin panel where super admins can manage other admins (similar to the existing Users page). This follows the existing patterns established for user management.

## Pre-Implementation Research

### Existing Patterns Consulted

1. **Admin Panel Architecture** (`docs/frontend/admin-panel-single-tenant.md`)
   - Routes defined in `app.admin.frontend.routes`
   - Pages use `generic-admin-entity-page` component
   - Data flows through adapters → template entity store → UI

2. **HTTP API** (`docs/backend/http-api.md`)
   - All admin routes under `/admin/api`
   - Protected routes use `wrap-admin-authentication`
   - Response format: `{:success true :data ...}` or `{:success false :error {:message ...}}`

3. **Database Schema** (`resources/db/models.edn`)
   - `admins` table already exists with fields: id, email, full_name, password_hash, role, status, last_login_at, created_at, updated_at
   - Admin roles: `["admin" "support" "owner"]`
   - Admin statuses: `["active" "suspended"]`

4. **Existing User Management Implementation**
   - Backend: `src/app/backend/routes/admin/users.clj`, `src/app/backend/services/admin/users.clj`
   - Frontend: `src/app/admin/frontend/pages/users.cljs`, `src/app/admin/frontend/events/users.cljs`, `src/app/admin/frontend/adapters/users.cljs`

5. **Entity Configuration System**
   - Config: `src/app/admin/frontend/config/entities.edn`
   - Table columns: `resources/public/admin/ui-config/table-columns.edn`
   - Entity registry: `src/app/admin/frontend/system/entity_registry.cljs`

### Security Considerations

- Only `owner` role should be able to manage other admins (based on `wrap-admin-role` middleware)
- Admins should not be able to modify their own role/status (prevent lockout)
- Admins should not be able to delete/suspend the last owner
- Password changes should require re-authentication or use password reset flow

---

## Implementation Phases

### Phase 1: Backend - Admin Management Service

**Files to create/modify:**

1. **Create `src/app/backend/services/admin/admins.clj`** (NEW)
   - `list-all-admins` - List admins with pagination/filtering
   - `get-admin-details` - Get single admin details
   - `update-admin!` - Update admin info (excluding password)
   - `create-admin!` - Already exists in auth.clj, may need wrapper
   - `delete-admin!` - Delete/suspend admin (with safety checks)
   - `update-admin-role!` - Update admin role
   - `update-admin-status!` - Update admin status

2. **Modify `src/app/backend/services/admin.clj`**
   - Add require for new admins namespace
   - Add delegation functions for new admin management functions

### Phase 2: Backend - Admin Management API Routes

**Files to create/modify:**

1. **Create `src/app/backend/routes/admin/admins.clj`** (NEW)
   - `GET /admin/api/admins` - List admins
   - `POST /admin/api/admins` - Create admin
   - `GET /admin/api/admins/:id` - Get admin details
   - `PUT /admin/api/admins/:id` - Update admin
   - `DELETE /admin/api/admins/:id` - Delete/suspend admin
   - `PUT /admin/api/admins/:id/role` - Update admin role
   - `PUT /admin/api/admins/:id/status` - Update admin status

2. **Modify `src/app/backend/routes/admin_api.clj`**
   - Add require for admin/admins namespace
   - Mount admin routes under `/admins` path
   - Apply role-based protection (owner only for sensitive operations)

### Phase 3: Frontend - Admin Entity Configuration

**Files to create/modify:**

1. **Modify `resources/public/admin/ui-config/table-columns.edn`**
   - Add `:admins` configuration with columns: id, email, full-name, role, status, last-login-at, created-at

2. **Modify `src/app/admin/frontend/config/entities.edn`**
   - Add `:admins` entity configuration (page-title, description, display-settings, features, components)

### Phase 4: Frontend - Admin Adapter and Events

**Files to create:**

1. **Create `src/app/admin/frontend/adapters/admins.cljs`** (NEW)
   - `admin->template-entity` - Normalize admin data
   - Register entity spec subscription
   - Register sync event
   - Register CRUD bridge
   - `init-admins-adapter!` - Initialize adapter

2. **Create `src/app/admin/frontend/events/admins.cljs`** (NEW)
   - `:admin/load-admins` - Load admins list
   - Success/failure handlers
   - Import modular events if needed

3. **Create `src/app/admin/frontend/subs/admins.cljs`** (NEW)
   - Subscriptions for admin data if needed beyond template system

### Phase 5: Frontend - Admin Page and Components

**Files to create/modify:**

1. **Create `src/app/admin/frontend/pages/admins.cljs`** (NEW)
   - Simple page using `generic-admin-entity-page` component

2. **Create `src/app/admin/frontend/components/admin-actions.cljs`** (NEW - optional)
   - Custom action buttons for admin-specific operations
   - Role change dropdown
   - Status toggle

3. **Modify `src/app/admin/frontend/system/entity_registry.cljs`**
   - Add `:admins` entry with init-fn, actions, modals

### Phase 6: Frontend - Routes and Navigation

**Files to modify:**

1. **Modify `src/app/admin/frontend/routes.cljs`**
   - Add `/admin/admins` route with guarded controller

2. **Modify `src/app/admin/frontend/components/layout.cljs`**
   - Add "Admins" link to sidebar navigation
   - Use shield/user-cog icon to distinguish from Users

### Phase 7: Security and Authorization

**Implementation details:**

1. **Backend authorization:**
   - Add `wrap-owner-only` middleware or use `(wrap-admin-role :owner)`
   - Apply to sensitive admin management routes

2. **Frontend authorization:**
   - Subscribe to current admin role
   - Conditionally show "Admins" link only for owner role
   - Disable certain actions (role change, delete) for non-owners

3. **Self-modification prevention:**
   - Backend: Check if target admin ID matches current admin ID
   - Frontend: Disable/hide certain actions on own admin row

4. **Last owner protection:**
   - Backend: Count owners before allowing role change/deletion
   - Return appropriate error message

---

## File Summary

### New Files (9)
- `src/app/backend/services/admin/admins.clj`
- `src/app/backend/routes/admin/admins.clj`
- `src/app/admin/frontend/adapters/admins.cljs`
- `src/app/admin/frontend/events/admins.cljs`
- `src/app/admin/frontend/subs/admins.cljs`
- `src/app/admin/frontend/pages/admins.cljs`
- `src/app/admin/frontend/components/admin-actions.cljs` (optional)

### Modified Files (7)
- `src/app/backend/services/admin.clj`
- `src/app/backend/routes/admin_api.clj`
- `resources/public/admin/ui-config/table-columns.edn`
- `src/app/admin/frontend/config/entities.edn`
- `src/app/admin/frontend/system/entity_registry.cljs`
- `src/app/admin/frontend/routes.cljs`
- `src/app/admin/frontend/components/layout.cljs`

---

## Testing Checklist

### Backend Tests
- [x] List admins returns correct data
- [x] Create admin works with valid data
- [x] Update admin works (non-owner fields)
- [x] Role change works for owner only
- [x] Cannot change own role
- [x] Cannot delete/suspend last owner
- [ ] Non-owner gets 403 on protected endpoints (needs Phase 7)

### Frontend Tests
- [ ] Admins page loads and displays list
- [ ] Create admin form works
- [ ] Edit admin inline/modal works
- [ ] Role badge displays correctly
- [ ] Status badge displays correctly
- [x] Only owner sees Admins link in sidebar
- [ ] Self-actions are disabled appropriately

### Integration Tests
- [ ] Full CRUD flow for admin management
- [ ] Authorization flow for different roles

---

## Notes

1. The existing `create-admin!` function in `auth.clj` can be reused but may need to be wrapped to add audit logging.

2. The admin role hierarchy from middleware (`support: 1, admin: 2, super_admin: 3`) suggests `owner` should be the highest role. Need to verify if `owner` maps to `super_admin` or if we should add `owner` to the hierarchy.

3. Consider whether to implement admin password reset via the existing password reset flow or add a dedicated admin password management feature.

4. The sidebar currently has no role-based visibility. We'll need to add this for the Admins link.

---

## Implementation Order

1. Phase 1 (Backend Service) → Phase 2 (Backend Routes) → Test API with curl
2. Phase 3 (Frontend Config) → Phase 4 (Adapter/Events) → Phase 5 (Page/Components)
3. Phase 6 (Routes/Navigation) → Phase 7 (Security polish)
4. Full integration testing
