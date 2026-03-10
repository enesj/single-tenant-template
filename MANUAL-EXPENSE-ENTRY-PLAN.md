# Manual Expense Entry — Implementation Plan

**Spec:** `specs/allium/drafts/expenses/manual-expense-entry.candidate.allium` (v2)
**Goal:** Flexible manual expense entry — from minimal (total + one context) to full itemized.

---

## Current State

| Aspect | Now | Target |
|--------|-----|--------|
| `supplier_id` | **NOT NULL** in DB, required by service | **Nullable** — optional context |
| `payer_id` | NOT NULL, required | NOT NULL, required (default pre-filled) |
| `purchased_at` | NOT NULL, required | NOT NULL, defaults to today |
| Line items | Required (≥1) | Optional — itemized mode is a toggle |
| Article | Only on line items | Also available as single-article shortcut |
| Category | Optional | Optional (counts as context) |
| Store | Optional | Optional, gated by supplier selection |
| Inline creation | Supplier only | Supplier, store, category, article |

**Minimum valid expense (target):** `total_amount` + `payer_id` + at least one of: supplier, store, category, or article.

---

## Phase 1: DB & Backend Foundation

### 1.1 Migration: Make `supplier_id` nullable

```sql
ALTER TABLE expenses ALTER COLUMN supplier_id DROP NOT NULL;
```

**Files:**
- `resources/db/domain/models.edn` — change `supplier_id` from `[:not-null]` to nullable
- Generate migration, apply to dev + test

**Risk:** Existing queries that JOIN on `expenses.supplier_id = suppliers.id` will silently drop rows. All such JOINs must become `LEFT JOIN`.

### 1.2 Update expense service config

**File:** `src/app/domain/backend/expenses/services/service_configs/config_maps.clj`

- Remove `:supplier_id` from `:required-fields` (keep `[:payer_id :purchased_at :total_amount]`)
- Change `:joins` from `INNER JOIN suppliers` to `LEFT JOIN suppliers`
- Change `:joins` from `INNER JOIN payers` to keep as INNER (payer stays required)
- Ensure `:select-fields` handles null supplier gracefully (supplier_display_name will be null)

### 1.3 Update expense service validation

**File:** `src/app/domain/backend/expenses/services/expenses.clj`

Current `create-expense!` requires:
- `:supplier_id`, `:payer_id`, `:purchased_at`, `:total_amount`
- At least one line item

Change to:
- Required: `:payer_id`, `:purchased_at`, `:total_amount`
- Context validation: at least one of `:supplier_id`, `:store_id`, `:expense_category_id`, or `:article_id` (new field) must be non-nil
- Line items: optional (empty list is valid when no itemized mode)
- When items present: `total_amount` must equal sum of `line_total`s (within tolerance)

### 1.4 Handle implicit single-article line item

**File:** `src/app/domain/backend/expenses/services/expenses.clj`

When `article_id` is provided but no items:
1. Look up article's `canonical_name`
2. Create a single `ExpenseItem` with `raw_label = canonical_name`, `qty = 1`, `line_total = total_amount`, `article_id = article_id`

This happens in `create-expense!` before the DB insert, so the receipt approval path is unaffected.

### 1.5 Inline reference creation endpoint

**File:** New handler or extend existing.

The frontend needs to create references inline during form interaction. Current state:
- Supplier inline create: **already works** via `[:user-expenses/create-supplier-modal]`
- Store, category, article: **no inline create from expense form**

**Options:**
- (A) Add individual create endpoints per entity (most exist already via admin API or user API)
- (B) Add a single `/api/v1/expenses/inline-reference` endpoint that dispatches by `kind`

**Recommendation: (A)** — use existing endpoints where possible:
- **Supplier:** Already has `POST /api/v1/expenses/suppliers` (user-scoped create)
- **Store:** Add `POST /api/v1/expenses/stores` — needs `supplier_id` + `display_name`
- **Category:** `POST /api/v1/expenses/expense-categories` likely exists; if not, add it
- **Article:** Add `POST /api/v1/expenses/articles` — needs `canonical_name`, returns created article

Verify which user-API create endpoints exist and add any missing ones.

### 1.6 Tenant safety for existing references

**File:** `src/app/domain/backend/expenses/services/expenses.clj` (in `create-expense!`)

Add validation before insert:
- `expense_category_id` → verify it belongs to `tenant_id`
- `payer_id` → verify it belongs to `tenant_id` (already done by factory)
- `store_id` → verify the store's `supplier_id` matches `supplier_id` on the expense (if both set)
- `supplier_id`, `article_id` → verify existence (global catalog, no tenant check needed)

---

## Phase 2: Frontend — Adaptive Form

### 2.1 New form component: `manual_expense_form.cljs`

**Directory:** `src/app/domain/frontend/expenses/components/manual_expense_form/`

Build a new adaptive form component rather than modifying the existing `user_expense_form` — the existing form is tightly coupled to receipt approval and has different validation assumptions.

**Sub-files:**
- `core.cljs` — main form component, state management
- `specs.cljs` — field specifications for the adaptive form
- `reference_pickers.cljs` — inline pick-or-create components
- `line_items_section.cljs` — optional itemized section (reuse existing `line_items.cljs` component)

### 2.2 Form layout: minimal → itemized

**Always visible:**
```
┌─────────────────────────────────────────┐
│  Total Amount*     Currency    Date      │
│  [___________]     [BAM ▾]    [today]   │
│                                         │
│  Payer*                                 │
│  [Default payer ▾]                      │
│                                         │
│  ── Context (pick at least one) ──────  │
│  Supplier   [Select or create... ▾]     │
│  Store      [Select... ▾]  (if supplier)│
│  Category   [Select or create... ▾]     │
│  Article    [Select or create... ▾]     │
│                                         │
│  Notes                                  │
│  [___________________________________] │
│                                         │
│  [+ Add line items]  ← toggle           │
└─────────────────────────────────────────┘
```

**When "Add line items" toggled ON:**
```
┌─────────────────────────────────────────┐
│  Total (computed)  Currency    Date      │
│  ✦ 45.50 (auto)   [BAM ▾]    [today]   │
│                                         │
│  Payer*   Supplier   Category           │
│  ...      ...        ...                │
│                                         │
│  ── Line Items ─────────────────────── │
│  Label          Qty    Price    Total   │
│  [Milk 1L    ] [2   ] [2.50 ] [5.00 ]  │
│  [Bread      ] [1   ] [1.50 ] [1.50 ]  │
│  [+ Add item]                           │
│                                         │
│  Article picker hidden (use items)      │
│  Notes  [____________________________]  │
└─────────────────────────────────────────┘
```

### 2.3 Reference picker component: `PickOrCreate`

A reusable component pattern for all four reference types:

```
Props:
  :entity-type    — :supplier | :store | :category | :article
  :options-sub    — re-frame subscription key for option list
  :create-event   — re-frame event key for inline creation
  :value          — current selected ID (or nil)
  :on-change      — callback with {:existing_id uuid} or {:new_name "..."}
  :filter-by      — optional parent filter (e.g., supplier_id for stores)
  :disabled?      — boolean
```

**Behavior:**
1. Dropdown with search/filter of existing options
2. "Create new" option at bottom of dropdown (or button)
3. Clicking "Create new" shows inline text input + save/cancel
4. On save: dispatches create event → on success, selects new entity
5. On cancel: returns to dropdown

**Existing to reuse:** `inline_supplier_select.cljs` already implements this pattern for suppliers. Generalize it.

### 2.4 Form state and validation

**Local form state** (reagent atom or re-frame path):

```clj
{:total_amount nil        ;; user-entered, nil when itemized
 :currency "BAM"
 :purchased_at "2026-03-10T..."
 :payer_id "uuid..."       ;; pre-filled from default payer
 :supplier nil             ;; {:existing_id uuid} or {:new_name "..."} or nil
 :store nil
 :expense_category nil
 :article nil
 :notes nil
 :itemized_mode false
 :items []}
```

**Validation rules (client-side):**
1. `effective_total > 0` (computed from items if itemized, else from total_amount)
2. `payer_id` is set
3. At least one of `supplier`, `store`, `expense_category`, `article` is set
4. If itemized: each item has `raw_label` and `line_total > 0`
5. `article` and `itemized_mode` are mutually exclusive

**On submit:**
1. Resolve all inline references (create new ones first via API calls)
2. Build expense payload with resolved IDs
3. If article was selected (no items): backend creates implicit line item
4. Dispatch `[:user-expenses/create-expense payload]`

### 2.5 Events and subscriptions

**New events** (in `src/app/domain/frontend/expenses/events/user_expenses/`):

```clj
;; Inline creation
:manual-expense/create-supplier-inline    ;; {:display_name "..."}
:manual-expense/create-store-inline       ;; {:supplier_id uuid, :display_name "..."}
:manual-expense/create-category-inline    ;; {:name "..."}
:manual-expense/create-article-inline     ;; {:canonical_name "..."}

;; Form submission
:manual-expense/submit                    ;; resolves refs, then calls create-expense
```

**Reuse existing:**
- `[:user-expenses/fetch-suppliers]`, `[:user-expenses/fetch-payers]`, etc. for loading option lists
- `[:user-expenses/create-expense-modal form-data callback]` for the final expense creation

### 2.6 Integration: wire into navigation

**File:** `src/app/domain/frontend/expenses/pages/user/expense_new.cljs`

Replace or augment the existing "new expense" page to use the adaptive form. Keep the old receipt-approval form unchanged (it's a different UX flow).

**Routes:** The existing `/expenses/new` route can point to the new adaptive form. No new routes needed.

---

## Phase 3: Polish & Edge Cases

### 3.1 Store ↔ Supplier coupling

- Store picker is disabled until supplier is selected
- Store options filtered by `supplier_id`
- When supplier changes: clear selected store
- Inline store creation auto-inherits the selected supplier_id

### 3.2 Itemized mode toggle behavior

- **Toggle ON:** article field clears and hides, total becomes read-only (computed)
- **Toggle OFF:** items list clears, total becomes editable, article field reappears
- Confirmation prompt if toggling OFF when items exist ("This will remove your line items")

### 3.3 Default payer pre-fill

On form mount:
1. Load payers list
2. Find payer with `is_default = true`
3. Pre-fill `payer_id`
4. If no default payer exists, leave empty (user must select)

### 3.4 Total auto-calculation in itemized mode

Reuse the existing `total-amount-input` / `totals-display` pattern from receipt approval:
- Itemized: show computed total as read-only display
- Non-itemized: show editable number input

### 3.5 Backend: update list/search for nullable supplier

**Files to audit:**
- `expenses.clj` — `list-expenses`, `get-expense-with-items` must handle null supplier
- `service_configs/config_maps.clj` — search on supplier_display_name must handle null
- `summary.clj` (if exists) — by-supplier grouping must handle "No supplier" bucket
- Frontend expense list columns: show "—" or "Uncategorized" when supplier is null

---

## Phase 4 (Deferred): Smart Suggestions

**Spec:** `deferred SmartSuggestions` in allium file

### Concept

When user opens the manual entry form, rank reference options by relevance:
- **Hybrid ranking:** recency-weighted frequency
- **Combo pre-fill:** "Last time you bought from Bingo → Cash payer, Groceries category"
- Data source: both manual entries and OCR-extracted receipts

### Sketch

1. Backend endpoint: `GET /api/v1/expenses/suggestions?context=manual-entry`
2. Query: aggregate user's expense history (last N months)
3. Return: ranked suppliers, categories, payers, supplier→payer→category combos
4. Frontend: sort dropdown options by suggestion score, show "Recent" section at top

**Not in scope for Phase 1-3.**

---

## Implementation Order

```
Phase 1 (Backend)               Phase 2 (Frontend)
┌─────────────────────┐         ┌─────────────────────────────┐
│ 1.1 Migration        │         │ 2.1 New form component       │
│ 1.2 Service config   │         │ 2.2 Adaptive layout          │
│ 1.3 Validation rules │    ──►  │ 2.3 PickOrCreate component   │
│ 1.4 Implicit item    │         │ 2.4 State & validation       │
│ 1.5 Create endpoints │         │ 2.5 Events & subs            │
│ 1.6 Tenant safety    │         │ 2.6 Route wiring             │
└─────────────────────┘         └─────────────────────────────┘
                                           │
                                           ▼
                                 ┌─────────────────────────────┐
                                 │ Phase 3: Polish & edge cases │
                                 └─────────────────────────────┘
                                           │
                                           ▼
                                 ┌─────────────────────────────┐
                                 │ Phase 4: Smart suggestions   │
                                 │ (deferred)                   │
                                 └─────────────────────────────┘
```

**Recommended start:** Phase 1.1 (migration) → 1.2 + 1.3 (service changes) → 1.5 (verify create endpoints) → then Phase 2 in parallel with 1.4 + 1.6.

---

## Files Changed (Summary)

### Modified
| File | Change |
|------|--------|
| `resources/db/domain/models.edn` | `supplier_id` nullable |
| `services/service_configs/config_maps.clj` | required-fields, LEFT JOIN |
| `services/expenses.clj` | validation, implicit item, tenant checks |
| `pages/user/expense_new.cljs` | wire new form |

### New
| File | Purpose |
|------|---------|
| `resources/db/migrations/NNNN-*.sql` | nullable supplier migration |
| `components/manual_expense_form/core.cljs` | adaptive form component |
| `components/manual_expense_form/specs.cljs` | field specs |
| `components/manual_expense_form/reference_pickers.cljs` | pick-or-create |
| `components/manual_expense_form/line_items_section.cljs` | optional items |
| `events/user_expenses/manual_entry.cljs` | inline create + submit events |

### Unchanged
| File | Why |
|------|-----|
| `user_expense_form/forms.cljs` | Receipt approval form stays as-is |
| `receipts/approval.clj` | OCR→expense flow unchanged |
| `receipt_ocr/extraction.clj` | OCR pipeline unchanged |

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Existing queries break with null supplier_id | Audit all JOINs → LEFT JOIN; test list/search/summary |
| Receipt approval regression | Don't touch existing form; new form is separate component |
| Inline creation race conditions | Create ref → wait for response → then submit expense (sequential) |
| Garbage data from too-easy inline create | Normalize names (trim, dedupe), validate min length |
| Orphan stores without suppliers | Enforce supplier requirement in both spec and DB constraint |
