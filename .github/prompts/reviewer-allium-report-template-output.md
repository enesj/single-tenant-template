# Allium Review Report — Template Code — 2026-02-26

## 1) Allium review verdict

**pass**

Template-scope changes are well-aligned with specs. The new user auth guard implements `authentication.allium`'s `UserAuthenticationBoundary` correctly. The middleware tenants dead-branch removal resolves the `authorization.allium` open question. List component decomposition is consistent with `dry-principle.allium`. One spec maintenance item and two residual risks are noted below but do not constitute behavioral misalignment.

---

## 2) Evidence

### Changed files reviewed

**Backend — changed**
- `src/app/template/backend/middleware/user.clj` — `handle-tenants-entity-access` deleted; `wrap-entities-authorization` docstring and security model updated
- `src/app/template/backend/middleware/rate_limiting.clj` — reviewed current state (dev-relaxed limits; P0 fixes from fallback-audit already applied)
- `src/app/template/backend/middleware/admin.clj` — minor cleanup
- `src/app/template/backend/routes/api.clj` — minor composition updates
- `src/app/template/backend/routes/admin/entities.clj` — entities route changes
- `src/app/template/backend/routes/admin/entities/backlog.clj` — new backlog sub-file

**Frontend — new**
- `src/app/template/frontend/events/auth/guard.cljs` — new `user/check-auth-protected`, `user/auth-guard-check-success`, `user/auth-invalid` events
- `src/app/template/frontend/routes/controllers.cljs` — new `user-guarded-start`, `make-entity-controller`, `make-simple-controller`
- `src/app/template/frontend/components/list/handlers.cljs` — list handler logic extracted from `list.cljs`
- `src/app/template/frontend/components/list/modals.cljs` — modal logic extracted from `list.cljs`
- `src/app/template/frontend/components/list/overrides.cljs` — override logic extracted from `list.cljs`
- `src/app/template/frontend/components/filter/helpers.cljs` — filter helper logic extracted

**Frontend — deleted**
- `src/app/template/frontend/components/states.cljs` (132 lines)
- `src/app/template/frontend/utils/shared.cljs` (90 lines)
- `src/app/template/frontend/components/table_headers.cljs` (60 lines)
- `src/app/template/frontend/hooks/display_settings.cljs` (32 lines)
- `src/app/template/frontend/components/json_highlight.cljs` (53 lines)
- `src/app/template/frontend/components/icons.cljs` (35 lines)

### Spec files consulted

- `specs/allium/template/authentication.allium`
- `specs/allium/template/authorization.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/template/dry-principle.allium`
- `specs/allium/drafts/list-view-filtering.candidate.allium`
- `specs/allium/drafts/list-view-sort.candidate.allium`
- `specs/allium/README.md`

---

## 3) Precise alignment notes (pass findings)

### A1 — User auth guard correctly implements `authentication.allium` `UserAuthenticationBoundary`

- `user/check-auth-protected` in `events/auth/guard.cljs`:
  - If `[:session :authenticated?] = true` → dispatch on-success events (page init proceeds)
  - If `false` → redirect to `/login` (session explicitly expired)
  - If `nil` (bootstrap in-flight) → fetch `/auth/status` → on-success dispatches events or redirects
- `user-guarded-start` in `routes/controllers.cljs` wraps route `:start` with this check.
- This mirrors the admin's `guarded-start` pattern and correctly implements `UserAuthenticationBoundary` (`session.user` checked before page init), consistent with `rule RequireUserSessionForProtectedUserRoutes`.
- `trim-v` destructuring in all three events is correct: `[on-success-events]`, `[on-success-events response]`, `[_]`.

---

### A2 — Tenants middleware dead-branch removal resolves `authorization.allium` open question

- Deleted: `handle-tenants-entity-access` from `middleware/user.clj`.
- The `authorization.allium` open question reads:
  > `wrap-entities-authorization` contains tenant-specific handling for `:tenants`, but current entity allowlist blocks unknown entities before that branch. Should `:tenants` be formally allowlisted or the dead branch removed?
- Decision implemented: dead branch removed. `:tenants` is now blocked by `BlockUnknownEntitiesByDefault` (not in the `{"users", "admins", "admin-sessions", "audit-logs"}` allowlist), which is the correct deny-by-default behavior.
- **Spec maintenance required** (see section 4): the open question in `authorization.allium` should be closed by recording the decision.

---

### A3 — List component decomposition is DRY-principle aligned

- `list.cljs` had inline functions (`entity-spec-fields`, `resolve-row-field`, `infer-filter-type`) and mixed concerns (handlers, modals, overrides).
- These are now in `list/handlers.cljs`, `list/modals.cljs`, `list/overrides.cljs`, `filter/helpers.cljs`.
- The main `list.cljs` now imports from these sub-namespaces rather than re-defining behavior inline.
- This aligns with `dry-principle.allium` `FrontendListAndEntityManagerReuseBoundary` guidance: "DRY at this layer is configuration-driven: each page changes entity specs/props, not list mechanics."
- `list-view-filtering.candidate.allium` behavioral guarantees (admin client-mode, user server-mode; filter apply/clear semantics) are preserved — the decomposition is structural, not behavioral.

---

## 4) Recommended fix direction

### Spec maintenance: close `authorization.allium` open question

Update `specs/allium/template/authorization.allium` to replace the open question with a resolved note:
```
-- resolved: handle-tenants-entity-access (dead branch) was removed.
-- :tenants is now blocked by BlockUnknownEntitiesByDefault (not in allowlist).
-- Decision: tenants entity is not exposed via generic CRUD at user-API level.
```

This is a doc-only update; no code change needed.

---

## 5) Residual risks

- **Deleted template utilities**: `states.cljs`, `utils/shared.cljs`, `table_headers.cljs`, `hooks/display_settings.cljs`, `json_highlight.cljs`, `icons.cljs` are gone. If any admin or domain component still imports from these namespaces, a compile-time error will occur in ClojureScript (ShadowCLJS will report a missing namespace). No consumer search was performed in this pass; recommend running `bb fe-test-parallel` to confirm no compile failures.
- **Rate limiting permissive dev defaults**: `rate_limiting.clj` currently has very high limits (`admin-login: 200/min`, `admin-api: 1000/min`). These are appropriate for local development but must not reach production as-is. No spec covers rate limiting, but the middleware comments note "in production, consider Redis." A config-driven limit map (dev vs prod) would reduce this risk.
- **`list/overrides.cljs` and `list/modals.cljs` behavioral scope**: Not read in full in this pass. If either file introduces new behavior (not just extraction), it may diverge from `list-view-filtering` or `list-view-sort` spec guarantees. Recommend a targeted read before merge.

---

## 6) Commit status

**committed** (multiple commits across this branch for template scope)

No spec-blocking misalignment found. Spec maintenance item (close authorization.allium open question) recommended as a follow-up, not a blocker.
