# Soft Delete Removal Plan (Hard Cutover)

## Goal
Replace all soft-delete behavior with hard deletes for Expenses, Expense Items, and Suppliers.
No feature flags and no backward-compatibility shims — a single cutover release.

## Final Decisions
- Keep `deleted_at` / `archived_at` columns for now, but stop writing/reading them; plan a follow-up to drop them.
- Foreign key strategy:
  - Expense Items → Expenses: `ON DELETE CASCADE`.
  - Expenses → Suppliers: `ON DELETE RESTRICT` (delete blocked when dependent expenses exist).
- API semantics: successful delete returns 204; delete of a Supplier with dependent Expenses returns 409 with a clear error.

## Cutover Timeline (Single Window)
1) Enable maintenance mode and freeze writes.
2) Take a full database snapshot/backup.
3) Run mandatory data cleanup (see SQL below) so legacy soft-deleted rows cannot reappear.
4) Apply FK migration (see “Database & Migrations”).
5) Deploy code that removes all soft-delete logic and archive/purge UI.
6) Smoke-test critical flows (create → edit → delete; summaries).
7) Exit maintenance; monitor for 15–30 minutes.

## Database & Migrations
- Enforce FK rules explicitly via a generated schema migration (models-first):
  - `resources/db/migrations/0028_schema.edn`
    - `expense_items.expense_id` → `expenses.id` with `ON DELETE CASCADE`.
    - `expenses.supplier_id` → `suppliers.id` with `ON DELETE RESTRICT`.
- Keep `deleted_at` / `archived_at` columns for this release; drop in a follow-up migration along with any partial indexes relying on them.
- Execute migrations using the standard REPL helper:
  - `(require '[app.template.backend.migrations.simple-repl :as mig])`
  - `(mig/migrate!)`

### Mandatory Data Cleanup (Pre-Deploy)
Run against PROD-like data before deploying the cutover build. Adjust to your schema as needed.
```sql
-- Clear any rows previously marked as soft-deleted
DELETE FROM expense_items WHERE deleted_at IS NOT NULL;
DELETE FROM expenses      WHERE deleted_at IS NOT NULL;
DELETE FROM suppliers     WHERE archived_at IS NOT NULL;

-- If receipts/files reference expenses, detach those that were soft-deleted
-- (uncomment and adapt if applicable)
-- UPDATE receipts SET expense_id = NULL
--  WHERE expense_id IN (SELECT id FROM expenses WHERE deleted_at IS NOT NULL);
```

### Indexes/Constraints Audit (Pre-Deploy)
- Identify and plan to remove partial unique indexes that relied on `WHERE deleted_at IS NULL` / `WHERE archived_at IS NULL`.
- Check for duplicate keys that those partial indexes previously masked; resolve duplicates before the cutover.

## Backend Changes

### 1) Expenses Service (`src/app/domain/backend/expenses/services/expenses.clj`)
- Rename `soft-delete-expense!` → `delete-expense!`; implement `DELETE FROM expenses` (no `deleted_at` writes).
- Wrap related operations in one transaction: revert linked receipt (if any) → delete expense (CASCADE removes items).
- `update-expense!`: remove soft-delete logic for missing items; use `DELETE FROM expense_items` where appropriate.
- Remove all `[:is :e.deleted_at nil]` / `[:is :deleted_at nil]` filters from reads (`list-expenses`, `get-expense-with-items`, `count-expenses`).

### 2) User Expenses Service (`src/app/domain/backend/expenses/services/user_expenses.clj`)
- Rename `soft-delete-user-expense!` → `delete-user-expense!`; same for batch variant.
- Remove `deleted_at` filters from all reads and summaries.

### 3) Expense Items Service (`src/app/domain/backend/expenses/services/expense_items.clj`)
- `delete-expense-item!`: use `DELETE FROM expense_items`.
- Remove `deleted_at` filters from fetch/update/list logic.

### 4) Suppliers Service (`src/app/domain/backend/expenses/services/suppliers.clj`)
- Replace archive/purge model with a single hard delete: `delete-supplier!`.
- Enforce RESTRICT via DB: if expenses exist, delete fails; return 409 with guidance.
- Remove `purge-supplier!`, `unarchive-supplier!`, and all `include_archived` options.
- Drop `archived_at` handling from `list`, `search`, and `count`.
- Update any "active expenses" helpers to count all expenses (no soft-deleted state).

### 5) User Expense Handlers (`src/app/domain/backend/expenses/handlers/user_expenses/crud.clj`)
- Update `delete-expense-handler` to call `delete-user-expense!` and return 204 on success.

## Frontend Changes

### 1) Expenses List (`src/app/domain/frontend/expenses/pages/user/expenses_list.cljs`)
- Simplify actions: remove `deleted?` checks and disabled states tied to soft-delete.
- Keep a single destructive action: “Delete”.

### 2) Supplier Flows (`src/app/domain/frontend/expenses/events/suppliers.cljs`)
- Remove all archive/purge flows and purge confirmation UI.
- Remove `include_archived` toggles; simplify load params.

### 3) Lookups/Reference CRUD (`src/app/domain/frontend/expenses/events/user_expenses/reference_crud.cljs`)
- Remove `include_archived` parameters; simplify supplier delete flow to a single endpoint.

### 4) Tables Configuration (`src/app/domain/frontend/expenses/config/table-columns.edn`)
- Remove `deleted_at` / `archived_at` from `available-columns` and `sortable-columns`.

## Tests
- Remove tests asserting soft-delete visibility and “include archived”.
- Add tests asserting:
  - Deleted entities are not retrievable.
  - Supplier delete returns 409 when expenses exist (RESTRICT).
  - Aggregations (monthly/by-supplier) reflect deletions immediately.

## CI Guard (Repo Hygiene)
- Add a task that fails CI if soft-delete code paths reappear. Suggested ripgrep pattern: 
  `deleted_at|archived_at|include_archived|soft-?delete|purge`.
- Provide a `bb audit-soft-delete` task to run locally and in CI.

## Observability & Safety
- Log minimal delete events (entity type/id, user id, timestamp) for support/debugging.
- Take/retain a pre-cutover backup to enable full rollback (restore + previous deploy).

## Post-Cutover Follow-Up
- Drop `deleted_at` / `archived_at` columns and any partial indexes that referenced them.
- Remove any dead branches discovered by the CI guard.

## Pre-Flight Checklist
- [ ] Backup taken and verified.
- [ ] Cleanup SQL executed on prod-like data.
- [ ] FK migration present and reviewed (CASCADE items, RESTRICT suppliers).
- [ ] All soft-delete/archival code paths removed in BE/FE.
- [ ] Tests updated/added; CI guard enabled.

## Verification After Cutover
- [ ] Create, edit, delete expense with items; verify items are removed.
- [ ] Attempt to delete supplier with expenses → 409 with clear message.
- [ ] Summaries and counts reflect deletions.
- [ ] No “archived” UI or filters present.
