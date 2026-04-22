# Plan: fully remove `expenses.is_posted`

## Context

`is_posted` on `expenses` is two things at once: a boolean column (default `true`) and a legacy state-machine flag left over from a "draft → posted" workflow. The UI lets you create unposted expenses (the "Mark as Posted" button), backend filters reports to only `is_posted = true`, and the admin dashboard counts unposted expenses as "pending". Because the column defaults to `true` and the UI flow has been superseded by receipt-driven expense creation, effectively every row is `true` — the functionality is dead.

Removal is a vertical slice across DB schema → service layer (filters, default injection, allowlists) → HTTP contract (query params, request bodies) → frontend (form fields, table columns, dashboard card, detail page action) → tests and docs. Safe order is bottom-up: DB + backend first, then HTTP surface, then frontend consumers, then the dashboard "pending" metric.

**Unrelated:** the `receipts` table has a `status` enum with a value `"posted"` — that's a different concept (receipt lifecycle) and is **not** touched by this plan. The reports adapter constant `expense-status-select-options` at `src/app/domain/frontend/expenses/admin/adapters/specs.cljs:14-16` and the `:post-transform` that maps `:is-posted → :status "draft"/"posted"` at `normalize.cljs:81-83` are expense-side and WILL go.

---

## Phase 1 — Backend services (remove column reads/writes and filter semantics)

1. **`src/app/domain/backend/expenses/services/reports.clj:10-11`** — drop `[:= :is_posted true]` from `base-where`; the AND collapses to only the date bounds.
2. **`src/app/domain/backend/expenses/services/user_expense_reports/shared.clj:84-86`** — drop `[:= :e.is_posted true]` from the cond→ in `expense-base-where`.
3. **`src/app/domain/backend/expenses/services/user_expenses.clj`** — remove all `is-posted?` / `:e.is_posted` / `:is_posted` mentions:
   - Remove `:is-posted :e.is_posted` from `allowed-user-expenses-order-by` (line 287).
   - Remove `is-posted?` destructured arg and its `conj` branches in `list-user-expenses` (lines 352, 389), `count-user-expenses` (lines 427, 462), and `highlight-user-expense-days` (lines 501, 536).
   - Drop `[:= :is_posted true]` clauses from dashboard aggregation `where`s (lines 580, 608, 635, 661).
   - Remove `:is_posted` from the column list at line 155 and the docstring column list at line 178.
   - Remove `:is-posted :e.is_posted` from `expense-alias-keys` at line 287.
4. **`src/app/domain/backend/expenses/services/expenses.clj`**:
   - Remove `:is-posted?` arg and its conj branches in `list-expenses` (lines 415-427) and `count-expenses` (lines 451-462).
   - Drop `:is_posted` from `update-if-present` in `normalize-expense-data` (line 239).
   - Drop `:is_posted` from the INSERT column list (line 612) and from `base-keys` in `update-expense!` (line 703).
5. **`src/app/domain/backend/expenses/services/service_configs/config_maps.clj:526-530`** — remove the `(update :is_posted #(if (nil? %) true (boolean %)))` line from the expense `:before-insert` fn (the surrounding `->` stays).
6. **`src/app/domain/backend/expenses/services/dashboard.clj`** — delete `get-pending-count` (lines 315-325) AND its invocation at line 378 (`power-user? (assoc :pending-count ...)`). The `unmapped-alias-count` assoc becomes the only branch; simplify to a regular `assoc` or keep the `cond->` with just that key.

## Phase 2 — Backend HTTP layer (remove from request/response contracts)

7. **`src/app/domain/backend/expenses/handlers/user_expenses/crud.clj`**:
   - Remove `:is-posted?` from the `opts` map in the list handler (line 57).
   - Strip `:is_posted` from `select-keys` in `create-expense-handler` (line 134) and `update-expense-handler` (line 170).
8. **`src/app/domain/backend/expenses/routes/route_configs.clj:168-173`** — remove `:is-posted [:boolean :is-posted?]` from the expense `:filter-params` map.
9. **`src/app/domain/backend/expenses/routes/routes_factory.clj:274`** — update the docstring example (drop `:is-posted` from the `{:search :string, :supplier-id :uuid, ...}` sample so it doesn't mislead readers).
10. **`src/app/domain/backend/expenses/services/user_expenses.clj`** `batch-update-allowed-keys` set (line 155) — already listed in Phase 1, confirm removal here.

## Phase 3 — DB schema (canonical source + generated migration)

11. **`resources/db/domain/models.edn`** — remove:
    - Column `[:is_posted :boolean {:default true :null false}]` (line 228).
    - Index `[:idx_expenses_is_posted :btree {:fields [:is_posted]}]` (line 240).
    - The comment `;; Expenses (posted entries with totals) — tenant-scoped` at line 208: drop "posted entries" wording → `;; Expenses — tenant-scoped`.
12. **`resources/db/models.edn`** — same removals at lines 217 and 237 (this file is regenerated but some workflows hand-edit it; confirm by running the generator).
13. **Generate migration** via the normal migration workflow (`(require 'app.template.backend.migrations.simple-repl)` → `(generate!)`). Do NOT hand-edit `resources/db/migrations/*`. Expected generated ops:
    - `DROP INDEX idx_expenses_is_posted`
    - `ALTER TABLE expenses DROP COLUMN is_posted`
14. **Apply to dev + test**: `(mig/migrate!)` and `(mig/migrate! :test)`.

## Phase 4 — Frontend (UI configs, forms, detail page, dashboard card)

15. **Config EDN files** — remove every `"is_posted"` entry:
    - `src/app/domain/frontend/expenses/config/table-columns.edn:183`
    - `src/app/domain/frontend/expenses/config/form-fields.edn:52,61,78`
    - `src/app/domain/frontend/expenses/config/view-options.edn:59`
    - `src/app/admin/frontend/config/table-columns.edn:155,165,172,181`
    - `src/app/admin/frontend/config/form-fields.edn:75,82,93`
    - `src/app/admin/frontend/config/view-options.edn:52`
16. **`src/app/domain/frontend/expenses/admin/adapters/normalize.cljs`**:
    - Remove `:is_posted [:is-posted]` from `:alias-keys` (line 70).
    - Delete the `:post-transform` that derives `:status "posted"/"draft"` (lines 81-83) — nothing else writes `:status` onto an expense row.
17. **`src/app/domain/frontend/expenses/admin/adapters/specs.cljs`**:
    - Delete `expense-status-select-options` (lines 14-16).
    - Remove the `{:id :status ... :options expense-status-select-options}` field from `expenses-entity-spec` (lines 43-46).
18. **`src/app/domain/frontend/expenses/events/user_expenses/crud/expenses.cljs`** — delete the whole `:user-expenses/post-expense`, `…-success`, `…-failure` event trio (lines 158-188). Also scan the namespace facade (per the memory note on def-re-export facades) for re-exports pointing at these — remove them.
19. **`src/app/domain/frontend/expenses/pages/user/expense_detail.cljs`**:
    - Drop `is-posted` from the `:keys` destructure (line 150) and the `posted?` let binding (line 164).
    - Remove the `(when (not posted?) ...)` guards around the Delete and Mark-as-Posted buttons (lines 262-273) — Delete should stay (becomes unconditional for `can-write?`), Mark-as-Posted goes entirely.
20. **`src/app/template/frontend/i18n/expenses.cljs`** — remove i18n keys `:expense-detail/status-posted` (lines 293, 615) and `:expense-detail/btn-post` (lines 316, 638). If `:receipts/status-posted` is unrelated (it is — receipt status), keep it.
21. **`src/app/domain/frontend/expenses/subs/workspace_dashboard.cljs:94-98`** — delete `:workspace-dashboard/pending-count` sub.
22. **`src/app/domain/frontend/expenses/pages/user/expenses_dashboard.cljs`**:
    - Drop `pending-count` from the `use-subscribe` block (line 396) and from the `admin-alerts-card` props (line 496).
    - In `admin-alerts-card` (lines 332-346), remove the `pending-count` arg and the `(when (some? pending-count) ...)` row. If that leaves only the unmapped-alias row, consider renaming the card (out of scope for this plan unless the user wants it).
    - Remove the `:dashboard/pending-expenses` i18n key if unused elsewhere.

## Phase 5 — Tests

23. **`test/app/domain/expenses/services/expenses_test.clj`** — lines 159, 166, 400, 407, 415, 422: either delete the test cases that exercise `:is-posted?` filtering and `count-expenses {:is-posted? false}`, or rewrite them around a different field. Cleaner to delete the two unposted-count assertions entirely.
24. **`test/app/domain/backend/expenses/services/user_expenses_test.clj`** — line 107 assertion `(re-find #"(?i)is_posted" count-sql-str)` must go; the fixture rows at lines 128, 138 drop `:is_posted`.
25. **`test/app/domain/backend/expenses/services/receipt_janitor_test.clj:65`** — the hand-rolled INSERT SQL lists `is_posted` and supplies a value; drop the column + value. The test name stays; janitor candidacy uses receipt `status = "posted"` which is unrelated.
26. **`test/app/domain/backend/expenses/services/dashboard_test.clj:29-34`** — delete the `get-pending-count` mock stub and the `(is (= 3 (:pending-count data)))` assertion.
27. **`test/app/template/frontend/auto_test_models.cljs:45`** — remove the `[:is_posted ...]` line so auto-test model generation matches the real schema.

## Phase 6 — Docs

28. **`docs/domain/expenses/http-api.md:57`** — remove `is-posted?` from the filter list.
29. **`docs/domain/expenses/index.md:265`** — remove the `:is_posted :boolean` line from the schema sample.

## Phase 7 — Verification

30. From the connected REPL:
    - `(require 'app.domain.backend.expenses.services.user-expenses :reload)` and similar for the other edited namespaces.
    - `(mig/migrate!)` then run `bb be-test 2>&1 | tee tmp/be-test.txt`.
    - `bb fe-test-parallel`.
31. Smoke test manually: list expenses, create one, update one, open the detail page, load the admin dashboard — confirm no console errors and no 500s referencing `is_posted`.

---

## Order-of-operations notes

- **Do Phase 1 & 2 together** before the migration — once the column is dropped, any service still referencing it will throw at query time. The HoneySQL keyword `:is_posted` only fails when a SQL statement actually executes with it, so a partial deployment leaves dormant bombs.
- **Phase 3 can't run until Phases 1-2 are in place in every running process** (dev REPL + test DB). In prod this would need a two-deploy strategy (deploy code change, then run migration); locally this is a single REPL session.
- **Phase 4 is independent of 1-3** as long as `is_posted` doesn't leak into request bodies anymore (which Phase 2 guarantees) — but keep the frontend changes in the same PR to avoid the detail page showing a "Mark as Posted" button that 400s because the backend no longer accepts the field.
- The two backup SQL files under `backups/` reference `is_posted` — those are historical snapshots; don't touch them.
