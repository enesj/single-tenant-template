<!-- ai: {:tags [:shared :overview :single-tenant] :kind :overview} -->

# Shared Utilities (Single-Tenant Template)

The `app.shared` layer holds cross-platform helpers used by both the backend and the admin UI. It is single-tenant only—there is no tenant context or RLS. Modules are small, reusable, and focused on consistent data handling across Clojure and ClojureScript.

## What lives here
- **Adapters**: `app.shared.adapters.database` – normalize DB results (convert PG objects, strip namespaced keys) and support admin-facing APIs like audit/login events.
- **Type conversion**: `app.shared.type-conversion` – HoneySQL-friendly casts and value coercion.
- **HTTP utilities**: `app.shared.http` – status constants, header helpers, and response helpers shared by admin UI and backend tests.
- **Validation/Patterns**: `app.shared.patterns`, `app.shared.validation` helpers used by form and API validation.
- **Dates/Time**: `app.shared.date` – timezone-safe formatting and parsing used in admin analytics and activity timelines.
- **Pagination**: `app.shared.pagination` – page/limit math plus UI helper shapes.
- **Strings**: `app.shared.string` – casing/slug/email helpers.
- **Auth helpers**: `app.shared.auth` – shared role set (`owner|admin|member|viewer`) and permission helpers for the admin shell.

## Usage notes
- Prefer the shared helpers before adding one-off utils in feature code.
- All functions are Clojure/ClojureScript compatible unless noted; JVM-only pieces (e.g., JDBC adapters) live under `.clj`.
- Keep responses/frontend data normalized (flat keys, epoch millis) using the adapter helpers.
- Update this index when adding new shared namespaces.

## Related docs
- [architecture.md](architecture.md) – structure of shared modules
- [auth-utilities.md](auth-utilities.md) – roles/permissions helpers
- [http-utilities.md](http-utilities.md) – HTTP helpers
