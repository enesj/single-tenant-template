<!-- ai: {:tags [:backend :reference-only :single-tenant] :kind :note} -->

# Financial Domain (Reference Only)

The single-tenant app does **not** include the legacy financial domain (transactions, payouts, revenue analytics). This file remains only as a marker to avoid confusion.

## Current State
- No financial domain namespaces are active under `src/app/domain`.
- No financial tables are present in `resources/db/models.edn`.
- Admin UI has no financial pages.

## If You Add Finance Later
- Define schema in `resources/db/models.edn` and regenerate migrations.
- Keep services/routes under a new namespace (e.g. `app.domain.financial.backend`) and document new endpoints in `http-api.md`.
- Ensure auth + security middleware stays enabled for any admin-facing routes.
