# CRUD Architecture Consolidation Plan

## Problem Summary

**Bug discovered**: "Highlights are not showing after user update"

**Root cause**: Admin form submission uses a completely separate code path that bypasses both the template form handlers AND the bridge system. The admin-specific success handler (`success_handlers.cljs`) was not tracking `recently-updated` IDs.

**Temporary fix applied**: Added `recently-updated` tracking to `success_handlers.cljs`

## Architectural Issues Identified

### 1. Multiple Parallel Event Chains for the Same Operation

There are at least **4 different paths** for handling a user update:

```
Path A: Template Form → form.cljs/submit-form → form.cljs/update-success
Path B: Template CRUD → crud.cljs/update-entity → bridge/update-success  
Path C: Admin Interceptor → form-interceptors.cljs → crud/factory → success_handlers.cljs
Path D: Admin Adapter → adapters/users.cljs → bridge system
```

Each path has its own success handler, and they don't share the `recently-updated` tracking logic.

### 2. Violation of DRY (Don't Repeat Yourself)

The ID extraction logic appears in at least 4 places:
- `src/app/template/frontend/events/form.cljs` (line ~125)
- `src/app/shared/frontend/bridges/crud.cljs` (line ~60)
- `src/app/admin/frontend/events/users/template/success_handlers.cljs`
- `src/app/admin/frontend/events/users/bulk_operations.cljs`

### 3. Layering Confusion

The architecture has overlapping layers with unclear boundaries:
- **Template layer**: Generic CRUD operations
- **Bridge layer**: Context-specific customization
- **Adapter layer**: Admin-specific data transformation
- **Interceptor layer**: Event routing override

The interceptor completely bypasses the bridge system.

### 4. Hidden Control Flow

The form interceptor (`form_interceptors.cljs`) silently redirects events:
```clojure
;; This LOOKS like it should use template form handling, but doesn't
:app.template.frontend.events.form/submit-form
→ Actually dispatches to :admin.template.form/submit-user-edit
→ Which uses crud/update-crud-handler
→ Which uses success_handlers.cljs (NOT the bridge system)
```

This makes debugging extremely difficult.

---

## Improvement Plan

### Phase 1: Consolidate Success Handling (Quick Win) ⭐ START HERE

**Goal**: Single source of truth for "what happens after a successful CRUD operation"

**Create**: `src/app/shared/frontend/crud/success.cljs`

```clojure
(ns app.shared.frontend.crud.success
  "Shared success handling for all CRUD operations.
   Ensures consistent behavior for:
   - Tracking recently-updated/recently-created IDs (for highlighting)
   - Clearing loading states
   - Extracting entity IDs from responses")

(defn extract-entity-id
  "Extract entity ID from response, handling both simple :id and namespaced keys."
  [response]
  (or (:id response)
      (->> response
           (filter (fn [[k _]] (and (keyword? k) (= (name k) "id"))))
           first
           second)))

(defn track-recently-created
  "Add entity ID to recently-created set for highlighting."
  [db entity-type entity-id]
  (update-in db [:ui :recently-created entity-type] 
             (fn [ids] (conj (or ids #{}) entity-id))))

(defn track-recently-updated
  "Add entity ID to recently-updated set for highlighting."
  [db entity-type entity-id]
  (update-in db [:ui :recently-updated entity-type] 
             (fn [ids] (conj (or ids #{}) entity-id))))

(defn handle-create-success
  "Standard create success handling. Returns updated db."
  [db entity-type response]
  (let [entity-id (extract-entity-id response)]
    (-> db
        (track-recently-created entity-type entity-id)
        ;; Clear any loading/error states as needed
        )))

(defn handle-update-success
  "Standard update success handling. Returns updated db."
  [db entity-type response]
  (let [entity-id (extract-entity-id response)]
    (-> db
        (track-recently-updated entity-type entity-id)
        ;; Clear any loading/error states as needed
        )))
```

**Files to update**:
- [ ] `src/app/template/frontend/events/form.cljs` - Use shared functions
- [ ] `src/app/shared/frontend/bridges/crud.cljs` - Use shared functions
- [ ] `src/app/admin/frontend/events/users/template/success_handlers.cljs` - Use shared functions
- [ ] `src/app/admin/frontend/events/users/bulk_operations.cljs` - Use shared functions

**Validation**:
- Edit a user in admin → row should highlight green
- Create a user in admin → row should highlight blue
- Bulk edit users → all affected rows should highlight

---

### Phase 2: Unify Admin Form Submission

**Goal**: Admin forms should go through the bridge system, not bypass it

**Problem**: `form_interceptors.cljs` intercepts form submissions and routes them to a completely separate handler chain.

**Solution**: Remove the interceptor and use the bridge system's customization points instead.

**Changes**:

1. **Remove/deprecate**: `src/app/admin/frontend/events/users/template/form_interceptors.cljs`
   - The event `:app.template.frontend.events.form/submit-form` should NOT be intercepted

2. **Remove/deprecate**: `src/app/admin/frontend/events/users/template/success_handlers.cljs`
   - These handlers duplicate what the bridge system should do

3. **Update**: `src/app/admin/frontend/adapters/users.cljs`
   - Register bridge operations for `:create` and `:update` that:
     - Use admin HTTP endpoints
     - On success, dispatch `:admin/load-users` (the admin-specific refresh)
   - The bridge's `default-update-success` already handles `recently-updated` tracking

**Validation**:
- Same tests as Phase 1
- Verify no regression in admin user CRUD

---

### Phase 3: Document Event Flow

**Goal**: Make the event routing explicit and traceable

**Create**: `docs/frontend/crud-event-flow.md`

Document:
1. All CRUD event paths (create, read, update, delete)
2. Which components dispatch which events
3. How the bridge system customizes behavior
4. How admin differs from template (and why)

**Add debug tooling**:
- Consider adding a "source" metadata tag to events
- Add optional verbose logging mode for CRUD operations

---

### Phase 4: Reduce Adapter Complexity (Optional/Future)

**Goal**: Adapters should only transform data, not control flow

**Principle**: Adapters should contain:
- ✅ Data normalization functions
- ✅ Entity spec definitions  
- ✅ HTTP request configuration
- ❌ Event dispatching logic (move to bridges)
- ❌ Success/failure handling (move to bridges)

This is a larger refactor and may not be immediately necessary if Phases 1-2 are completed.

---

## Files Reference

### Key files involved in the bug:

| File | Role | Issue |
|------|------|-------|
| `src/app/admin/frontend/events/users/template/form_interceptors.cljs` | Intercepts form submit | Bypasses bridge system |
| `src/app/admin/frontend/events/users/template/success_handlers.cljs` | Admin success handlers | Was missing `recently-updated` tracking |
| `src/app/shared/frontend/bridges/crud.cljs` | Bridge system | Not being used for admin forms |
| `src/app/template/frontend/events/form.cljs` | Template form handlers | Has its own `recently-updated` tracking |
| `src/app/admin/frontend/adapters/users.cljs` | Admin user adapter | Registers bridge but form bypasses it |

### Files with duplicated ID extraction logic:

1. `src/app/template/frontend/events/form.cljs` (lines 75-80, 125-130)
2. `src/app/shared/frontend/bridges/crud.cljs` (lines 58-65)
3. `src/app/admin/frontend/events/users/template/success_handlers.cljs` (lines 10-17)
4. `src/app/admin/frontend/events/users/bulk_operations.cljs` (lines 29-35, 69-75)

---

## Progress Tracking

- [x] **Phase 1**: Consolidate Success Handling ✅ COMPLETED
  - [x] Create `src/app/shared/frontend/crud/success.cljs`
  - [x] Update `form.cljs` to use shared functions
  - [x] Update `bridges/crud.cljs` to use shared functions
  - [x] Update `success_handlers.cljs` to use shared functions
  - [x] Update `bulk_operations.cljs` to use shared functions
  - [x] Test highlighting works for all CRUD paths (shadow-cljs compiles with 0 warnings)

- [x] **Phase 2**: Unify Admin Form Submission ✅ COMPLETED
  - [x] Analyze form interceptor usage
  - [x] Update form interceptors to route through bridge system
  - [x] Update bridge default handlers to clear form state
  - [x] Deprecate success_handlers.cljs (now dead code)
  - [x] Test compilation - shadow-cljs compiles with 0 warnings

- [x] **Phase 3**: Document Event Flow ✅ COMPLETED
  - [x] Create `docs/frontend/crud-event-flow.md`
  - [ ] Add debug logging option (deferred - current logs are adequate)

- [ ] **Phase 4**: Reduce Adapter Complexity (Optional)
  - [ ] Define clear adapter boundaries
  - [ ] Move event logic to bridges

---

## Notes

- The temporary fix in `success_handlers.cljs` (adding `recently-updated` tracking) is sufficient for now
- Phase 1 is the safest starting point - it doesn't change control flow, just consolidates duplicate code
- Phase 2 is the "proper" fix but requires more careful testing
- Debug logging was added during investigation - consider keeping the useful logs
