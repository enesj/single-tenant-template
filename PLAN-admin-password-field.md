# Admin Password Field Fix

## Problem
The "Add Admins" form shows "Password hash" as a required field. Users should enter a plain password that gets hashed on the backend, not the hash directly.

## Current Behavior
- Form field: `password_hash` (required)
- User is expected to enter a hash directly
- This is incorrect UX and security issue

## Desired Behavior
- Form field: `password` (required for create, optional for update)
- User enters plain text password
- Backend hashes the password before storing
- Password field should NOT be displayed in the table
- Password should be hidden (type="password") in the form

## Solution

### Phase 1: Backend API Changes
**File:** `src/app/backend/routes/admin/admins.cljs`

1. Modify POST `/admin/api/admins` to:
   - Accept `password` field (not `password_hash`)
   - Hash the password using `buddy.hashers/derive`
   - Store as `password_hash` in database

2. Modify PUT `/admin/api/admins/:id` to:
   - Accept optional `password` field for password changes
   - Only update password_hash if password is provided
   - Don't require password for other updates

### Phase 2: Frontend Form Configuration
**File:** `resources/public/admin/ui-config/form-fields.edn`

1. Add `:admins` form configuration:
   - `email` - required, type email
   - `full_name` - optional, type text  
   - `password` - required for create, type password (hidden input)
   - `role` - required, type select with options
   - `status` - required, type select with options

### Phase 3: Table Column Configuration
**File:** `resources/public/admin/ui-config/table-columns.edn`

1. Ensure `password_hash` is NOT in visible columns
2. Verify current config excludes sensitive fields

### Phase 4: Frontend Adapter Update
**File:** `src/app/admin/frontend/adapters/admins.cljs`

1. Update create operation to send `password` instead of `password_hash`
2. Ensure adapter transforms form data correctly before API call

## Implementation Steps

### Step 1: Update Backend Create Handler
- Modify `create-admin-handler` to accept `password` and hash it
- Add validation for password (min length, etc.)

### Step 2: Update Backend Update Handler  
- Modify `update-admin-handler` to optionally accept `password`
- Only hash and update if password provided

### Step 3: Add Form Fields Config
- Create admins entry in `form-fields.edn`
- Configure password field as type "password"

### Step 4: Test End-to-End
- Verify create admin with password works
- Verify update admin without changing password works
- Verify update admin with new password works

## Security Considerations
- Never return `password_hash` in API responses (already handled)
- Use proper password hashing (bcrypt via buddy.hashers)
- Minimum password length validation
- Password field should use `type="password"` in form

## Files to Modify
1. `src/app/backend/routes/admin/admins.clj` - API handlers
2. `src/app/backend/services/admin/admins.clj` - Service layer (if needed)
3. `resources/public/admin/ui-config/form-fields.edn` - Form configuration
4. `resources/public/admin/ui-config/table-columns.edn` - Verify password not shown

## Testing
- [x] Create new admin with password
- [ ] Verify password is hashed in database
- [ ] Update admin without password change
- [ ] Update admin with password change
- [x] Verify password_hash never returned in API
- [x] Verify form shows password field with hidden input
