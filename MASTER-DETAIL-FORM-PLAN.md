# Reusable Master/Detail Form Component (Template) — Plan

This document proposes a refactor to create a **reusable master/detail ("header + line items") form wrapper** under `src/app/template/frontend/components/`, then migrate **admin `/admin/expenses` first**, and finally migrate the **user `/expenses/list`** flow.

The intent is to keep the existing expenses-specific UI/logic (line totals, "Use total", etc.) but remove duplicated orchestration code (detail fetch + normalization + fork stability) and make future master/detail forms easier.

---

## Implementation Status

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 0 | Fix admin detail event response key | ✅ Complete |
| Phase 1 | Create master_detail.cljs wrapper | ✅ Complete |
| Phase 2 | Migrate admin expenses to wrapper | ✅ Complete |
| Phase 3 | Migrate user expenses to wrapper | ✅ Complete |
| Phase 4 | Cleanup & Documentation | ✅ Complete |

---

## Changes Made

### Phase 0: Detail Response Key Fix

**File**: `src/app/domain/frontend/expenses/events/events_factory.cljs`

Added `:detail-response-key` option to `generate-detail-events` to handle cases where the API returns a singular key (`:expense`) but the entity-key is plural (`:expenses`).

**File**: `src/app/domain/frontend/expenses/events/entity_configs.cljs`

Added `:detail-response-key :expense` to the expenses config.

### Phase 1: Master-Detail Form Wrapper

**File**: `src/app/template/frontend/components/form/master_detail.cljs`

Created a reusable wrapper component that provides:

1. **Detail fetch orchestration** - Automatic loading of detail data in edit mode
2. **Normalization** - Transform backend data into form initial values
3. **Fork stability** - Memoized specs and initial values to prevent dirty resets
4. **Validation** - Custom validation before submit
5. **Submit preparation** - Transform form values to API payload

### Phases 2 & 3: Admin & User Expense Forms

**Files**:
- `src/app/domain/frontend/expenses/components/expense_form.cljs`
- `src/app/domain/frontend/expenses/components/user_expense_form.cljs`

Both files now:
- Use the `master-detail-form` wrapper for edit modal orchestration
- Share common helper functions (normalization, validation, prepare-submit)
- Have cleaner, more maintainable code with less duplication

---

## Why do this?

We currently have two similar modal edit flows:

- User expenses: `src/app/domain/frontend/expenses/components/user_expense_form.cljs`
- Admin expenses: `src/app/domain/frontend/expenses/components/expense_form.cljs`

Both need the same core behaviors:

1. **Detail fetch** for edit (master + detail rows)
2. **Normalization** from backend/list entity shape → form initial values
3. **Stable fork initialization** (memoized `:entity-spec` and `:initial-values`) to avoid dirty resets
4. **Prepared submit payload** (preserve `items[].id`, coerce numbers, validate totals)

The plan is to extract (1) + (2) + (3) into a reusable **template component**, while keeping expense-domain validation + line-item calculation helpers in the domain (`form_fields.cljs`).

---

## Scope (what moves where)

### Stays in domain (expenses)

- The custom line-items input UI and calculations:
  - `src/app/domain/frontend/expenses/components/form_fields.cljs`
- Expense-specific normalization rules (key mapping) and submit preparation rules:
  - Defined in each form file (`expense_form.cljs`, `user_expense_form.cljs`)
- Expense-specific validation messages (e.g. totals mismatch tolerance)

### Moves to template (reusable wrapper)

A UIX component that wraps `app.template.frontend.components.form/form` and provides:

- "Edit modal needs detail" orchestration (requested?, loading/error display, fallback to list row)
- Memoization to prevent fork resets
- A consistent API for:
  - detail fetching (`:load-detail!`)
  - selecting detail (`:select-detail`)
  - normalization (`:normalize-initial-data`)
  - submit preparation (`:prepare-submit-values`)
  - validation (`:validate-values`)

---

## Component API

### File location

`src/app/template/frontend/components/form/master_detail.cljs`

### Component name

`master-detail-form` (UIX component)

### Props

- `:mode` — `:create` or `:edit`
- `:entity-name` — string passed to `form` (e.g. `"expense"`, `"user-expense"`)
- `:entity-spec` — vector spec for `form`
- `:entity-id` — string (required for `:edit`)
- `:load-detail!` — fn `(fn [entity-id] (rf/dispatch [...]))`
- `:select-detail` — the currently loaded entity value
- `:detail-loading?` — boolean
- `:detail-error` — string or nil
- `:normalize-initial-data` — fn `(fn [raw-entity] normalized-map)`
- `:validate-values` — fn `(fn [form-values] {:ok? true} | {:ok? false :error "..."})`
- `:prepare-submit-values` — fn `(fn [form-values] prepared-payload-map)`
- `:on-submit` — fn `(fn [prepared-payload] ...)`
- `:on-cancel` — fn
- `:button-text` — string (e.g. `"Update Expense"`)
- `:initial-row-data` — map (fallback while detail is loading)
- `:default-values` — map

### Wrapper responsibilities

Inside `master-detail-form`:

1. If `:mode :edit` and `:entity-id` exists, call `:load-detail!` in `use-effect` on `[entity-id]`
2. Compute `effective-entity`:
   - if detail is loaded for the same `:entity-id`, use it
   - else fall back to `:initial-row-data`
3. Memoize:
   - `memo-spec` via `use-memo` (deps: `entity-spec`)
   - `memo-initial-values` via `use-memo` (deps: `effective-entity`, `normalize-initial-data`, `default-values`)
4. Render:
   - error alert (detail error or validation error)
   - loading placeholder while detail is in-flight
   - the `form` with stable `:initial-values`
5. On fork submit:
   - run `:validate-values`
   - if ok → call `:on-submit (prepare-submit-values values)`

---

## Verification (chrome-mcp + CLJS eval)

### Admin (chrome-mcp)

1. Go to `http://localhost:8085/admin/expenses`.
2. Click `#btn-edit-expenses-<expense-id>`.
3. Confirm populated fields:
   - `#expense-total_amount`
   - `#items-<item-id>-qty`, `#items-<item-id>-unit_price`, `#items-<item-id>-line_total`
4. Change qty:
   - line_total updates immediately
   - total_amount auto-updates (unless manually overridden)
5. Confirm submit enablement:
   - `#btn-update` enabled on valid dirty changes, disabled on mismatch
6. Submit and reopen → items still present.

### User (chrome-mcp)

Repeat the equivalent checks on `http://localhost:8085/expenses/list`.

### CLJS eval

Use `mcp__clojure-mcp__clojurescript_eval` to:

- Confirm admin detail is stored under `[:admin :expenses :entries :by-id <id>]`.
- Confirm wrapper produces stable initial values (no reinit while typing).
