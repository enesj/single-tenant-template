<!-- ai: {:tags [:shared :architecture :single-tenant] :kind :guide} -->

# Shared Utilities Architecture

The shared layer provides cross-platform helpers used by the single-tenant admin backend and frontend. It keeps data handling, validation, and HTTP behavior consistent without any tenant/RLS concerns.

## Layout (key namespaces)
```
app.shared/
├── adapters/
│   └── database.clj         ; PG object → Clojure data, key normalization
├── auth.cljc                ; roles/permissions helpers (owner/admin/member/viewer)
├── date.cljc                ; time formatting/parsing helpers
├── http.cljc                ; status codes, headers, response helpers
├── pagination.cljc          ; page/limit math and shapes
├── patterns.cljc            ; regex/pattern helpers for validation
├── string.cljc              ; casing/slug/email helpers
├── type_conversion.cljc     ; HoneySQL-friendly casts/coercions
└── validation/…             ; shared validation helpers
```

## Principles
- **Cross-platform**: Same APIs in Clojure and ClojureScript where possible; JVM-only pieces stay in `.clj`.
- **Normalized data**: Use `adapters.database/convert-pg-objects` and `convert-db-keys->app-keys` before returning data to the admin UI.
- **Small units**: Keep namespaces focused (status codes, pagination math, etc.) to encourage reuse.
- **No tenant context**: All helpers are single-tenant; there is no `tenant_id` handling or RLS integration.

## Common flows
- **Backend → UI data**: Query → `adapters.database` (convert PG objects/keys) → add epoch millis for timestamps (audit/login activity) → JSON response.
- **Pagination**: `pagination/->pagination` helpers calculate page/offset; admin routes use this for user lists, audit, and login events.
- **Auth helpers**: Use `auth/role-includes?` and related helpers to keep permission checks consistent between backend and admin UI.
- **Type conversion**: `type-conversion/cast-field-value` and related helpers wrap HoneySQL casts when inserting/updating.

## Contributor tips
- Reuse shared helpers before adding ad hoc functions in feature namespaces.
- Keep new helpers platform-neutral unless they need JVM/browser APIs; separate `.clj`/`.cljs` if required.
- When adding fields that carry timestamps to the UI, normalize to epoch millis for consistency with existing activity views.
