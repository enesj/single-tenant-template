# Allium Review Report — Admin Code — 2026-02-26

## 1) Allium review verdict

**misaligned**

Two issues violate or outpace the authorization spec: (M1) the new duplicates/merge admin routes omit an explicit role-tier guard for a destructive operation; (M2) a deleted re-frame event (`set-view-option-draft`) creates silent dispatch failures. Additional residual risks below.

---

## 2) Evidence

### Changed files reviewed

**Backend — deleted**
- `src/app/admin/backend/services/admin/monitoring/integrations.clj` — full service deleted (184 lines)
- `src/app/admin/backend/services/admin/monitoring/shared.clj` — full service deleted (192 lines)
- `src/app/admin/backend/services/admin/users/security.clj` — full service deleted (87 lines)
- `src/app/template/backend/routes/admin/integrations.clj` — admin integrations route tree deleted

**Backend — new/changed**
- `src/app/domain/backend/expenses/routes/duplicates.clj` — new admin routes: detect, preview, merge, ignore clusters
- `src/app/domain/backend/expenses/services/duplicates.clj` — new duplicate-detection service (prefix/trigram/levenshtein)
- `src/app/domain/backend/expenses/services/merge.clj` — new merge service (FK reassignment + audit)
- `src/app/domain/backend/expenses/services/dedup_ignored_clusters.clj` — new ignored-clusters service
- `src/app/admin/backend/services/admin/users/bulk.clj` — `bulk-update-field!` DRY scaffold extracted
- `src/app/admin/backend/setup.clj` — `dangerously-delete-all-data!` deleted

**Frontend — deleted**
- `src/app/admin/frontend/security/wrapper.cljs` — security wrapper component deleted (63 lines)
- `src/app/admin/frontend/subs/audit.cljs` — audit subscriptions deleted (72 lines)
- `src/app/admin/frontend/events/audit.cljs` — audit events deleted (9 lines)
- `src/app/admin/frontend/events/admins.cljs` — admins events deleted (23 lines)
- Implicit deletion: `set-view-option-draft` re-frame event removed from `events/settings/view_options.cljs`

**Frontend — new/changed**
- `src/app/admin/frontend/pages/domain/expenses/duplicates.cljs` — new Dedup & Merge admin page
- `src/app/admin/frontend/events/settings/view_options.cljs` — `set-view-option-draft` deleted; now delegates to `view-options-helpers`
- `src/app/admin/frontend/utils/view_options_helpers.cljs` — new extracted helper module

### Spec files consulted

- `specs/allium/template/authorization.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/domain/expenses/implementation.allium`
- `specs/allium/README.md`

---

## 3) Precise mismatch list

### M1 — Duplicates/merge routes omit admin role-tier guard for destructive operations

- **Spec**: `authorization.allium` defines `AdminRouteRequest.required_role: support | admin | owner | super_admin` and `rule AdminRoleGuardAllowsSufficientPrivilege` — the model assumes admin routes that perform sensitive operations declare and check a required role level.
- **Code**: `detect-handler`, `merge-preview-handler`, and the merge execution handler in `src/app/domain/backend/expenses/routes/duplicates.clj` all check only:
  ```clojure
  (nil? admin-id) → 401
  ```
  No `required-role` guard is applied. Any authenticated admin (regardless of role tier) can execute a bulk FK-reassignment merge.
- **Effect**: A `support`-tier admin can perform destructive merge operations that permanently reassign FK references across `expenses`, `stores`, `article_aliases`, and `price_observations`. This conflicts with the authorization model's intent — `AdminRoleGuardAllowsSufficientPrivilege` exists precisely for tiered access to high-impact capabilities.
- **Spec reference**: `authorization.allium` lines 70–86, `AdminRoleGuardAllowsSufficientPrivilege` / `AdminRoleGuardRejectsInsufficientPrivilege`; also the general `ExpensesAuthorizationBoundary` guidance that destructive `power` actions require elevated role.

---

### M2 — Deleted `set-view-option-draft` event creates silent caller failures

- **Removed**: `:app.admin.frontend.events.settings/set-view-option-draft` re-frame event in `events/settings/view_options.cljs`.
- **Risk**: Re-frame does not error on unknown event dispatch in production — it silently no-ops. If any component or test still dispatches `[:app.admin.frontend.events.settings/set-view-option-draft ...]`, the setting change will silently be dropped with no UI error.
- **Not confirmed**: A targeted search for callers was not performed in this pass.
- **Spec reference**: No spec directly governs re-frame event naming, but the `dry-principle.allium` `FrontendCrudBridgeAndSuccessBoundary` guidance requires consistent success/failure semantics — silent drops violate that spirit.

---

### M3 — Prefix duplicate-detection strategy fetches all entity rows into memory

- **Code**: `duplicates/fetch-all-rows` in `duplicates.clj` uses:
  ```clojure
  {:select [:id name-col key-col :created_at] :from [(keyword table)] :order-by [[:created_at :asc]]}
  ```
  No `LIMIT`. The prefix strategy subsequently groups these rows in-memory.
- **Effect**: For large tables (e.g. `articles` or `suppliers` with tens of thousands of rows), this loads the entire table into the JVM heap. The fallback-audit (P1) just eliminated `{:fetch-limit 1000}` client-side fetches; introducing a server-side unbounded fetch in admin code recreates the problem pattern on the backend.
- **Spec reference**: No spec explicitly governs this, but the fallback-audit prompt in `.github/prompts/fallback-audit.prompt.md` identified unbounded fetches as a systemic risk. The admin UI's explicit `:limit` query param (default 50 clusters) does not protect the underlying fetch.

---

## 4) Recommended fix direction

### M1 — Add role guard to destructive admin routes

- Wrap merge execution (and optionally detect + preview) with an admin role check:
  ```clojure
  (when (admin-utils/require-role request :admin)
    ...)
  ```
- Update `authorization.allium` to document the expected `required_role` for the new `ExpensesDuplicatesMergeOperation` surface.
- Alternatively: add a candidate spec `specs/allium/domain/expenses/dedup-merge.candidate.allium` that explicitly models the required role gate for merge operations.

### M2 — Verify no remaining callers of `set-view-option-draft`

- Search for `:app.admin.frontend.events.settings/set-view-option-draft` dispatches across admin + domain frontend files. If callers exist, add a deprecated shim that calls the new replacement path.

### M3 — Add limit to `fetch-all-rows` or switch to SQL-only grouping

- For the prefix strategy: either add a configurable `:limit` to `fetch-all-rows` (passed through from the handler's `:limit` param), or rewrite the grouping as a SQL subquery using `LEFT(normalized_key, ...)`.
- The trigram and levenshtein strategies already use SQL self-joins with `LIMIT`, which is the right pattern.

---

## 5) Residual risks

- **`dangerously-delete-all-data!` removed from `admin/backend/setup.clj`**: `test/app/backend/fixtures.clj` had 33 lines removed in the same diff. If the test fixture called this function and was not updated, backend tests that need a clean DB state could fail silently or error.
- **Monitoring service deletion**: `monitoring/integrations.clj` and `monitoring/shared.clj` are deleted. `template/backend/routes/admin/integrations.clj` is also deleted. If any admin UI page navigated to an integrations route, it would now 404. No frontend route guard appears to have been added.
- **`admin/frontend/security/wrapper.cljs` deleted**: The security wrapper may have been used by admin pages to control access. Its deletion is not confirmed as safe without checking all consumers.
- **Audit subs/events deleted**: Admin audit log subscriptions removed. If any admin UI component still subscribes to `::audit/*`, it will receive `nil` with no error.

---

## 6) Commit status

**not committed** — misalignment found:
- M1 (merge route lacks role guard) is a direct authorization-spec concern; requires fix or explicit spec update before merge.
- M2 (deleted event risk) requires a search-and-verify pass.
- M3 (unbounded fetch-all-rows) should be addressed given the recent fallback-audit work.
