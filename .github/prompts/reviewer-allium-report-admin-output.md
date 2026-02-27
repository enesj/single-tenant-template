# Allium Review Report — Admin Code — 2026-02-27

## 1) Allium review verdict

**pass**

Current admin-scope code aligns with the consulted Allium specs. The previously reported issues (M1/M2/M3) are resolved in current `HEAD` (notably by commit `13dcd3b`).

---

## 2) Evidence

### Changed files reviewed

**Primary admin/domain files (current behavior)**
- `src/app/domain/backend/expenses/routes/duplicates.clj`
- `src/app/domain/backend/expenses/services/duplicates.clj`
- `src/app/domain/backend/expenses/routes/core.clj`
- `src/app/template/backend/routes/admin_api.clj`
- `src/app/template/backend/middleware/admin.clj`
- `src/app/admin/frontend/events/settings/view_options.cljs`

**Review context**
- Prior report checked: `.github/prompts/reviewer-allium-report-admin-output.md`
- Change range inspected: `0f802d5..HEAD`
- Fix commit confirmed: `13dcd3b feat: add admin role checks & fetch limits to duplicates`

### Spec files consulted

- `specs/allium/template/authorization.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/domain/expenses/implementation.allium`
- `specs/allium/README.md`

---

## 3) Precise mismatch list

No active mismatches were detected in the reviewed admin scope.

Previously reported findings are now resolved:

- **M1 (merge role gate)**: `merge-preview-handler` and `merge-handler` explicitly reject non-`admin|owner|super_admin` roles with `403`.
- **M2 (removed event caller risk)**: no remaining dispatch call sites were found for `:app.admin.frontend.events.settings/set-view-option-draft` in `src/` or `test/`.
- **M3 (unbounded prefix fetch)**: prefix detection now applies a bounded `fetch-limit` (default `5000`), and `fetch-all-rows` applies SQL `:limit` when present.

---

## 4) Recommended fix direction

No mandatory code corrections are required for current spec alignment.

Optional hardening (non-blocking):

- Add explicit dedup merge/preview role expectations to `specs/allium/domain/expenses/implementation.allium` (or a dedicated candidate spec) to reduce future ambiguity.
- Consider centralizing duplicates-route role checks via a shared helper/middleware to avoid role-hierarchy drift.
- If operations need tuning at scale, expose a bounded, validated `fetch-limit` request knob rather than relying only on default value.

---

## 5) Residual risks

- `detect` / `ignore` / `unignore` are authenticated-admin operations without an elevated role-tier guard. This is currently consistent with spec boundaries, but policy intent should remain explicit to avoid future reviewer disagreement.
- Prefix strategy is bounded and safer now, but a fixed default scan cap may under-detect duplicates on very large datasets; monitor and tune as needed.

---

## 6) Commit status

**committed**

- `13dcd3b` — `feat: add admin role checks & fetch limits to duplicates`
