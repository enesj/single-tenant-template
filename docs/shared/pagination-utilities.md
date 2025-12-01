<!-- ai: {:namespaces [app.shared.pagination] :tags [:shared :pagination :single-tenant] :kind :reference} -->

# Pagination Utilities

Shared helpers for calculating pagination in the single-tenant admin app.

- **Location**: `src/app/shared/pagination.cljc`
- **Use cases**: admin user list, audit log list, login events, and per-user activity views.

## Core helpers
- `page->offset` / `offset->page` – convert between UI page numbers and DB offsets.
- `paginate` – produce `{:page :per-page :offset :limit :total :total-pages}` maps for API responses/UI state.
- `within-range?` – guard against invalid page inputs.

## Conventions
- User lists typically use `page`/`per-page`.
- Audit/login-event routes use `limit`/`offset`; convert to page counts in the UI as needed.
- No tenant scoping is required—pagination is global to the single-tenant dataset.

## Tips
- Default page sizes come from `admin.utils/extract-pagination-params`; keep server defaults aligned with UI expectations.
- Always bound `per-page`/`limit` to reasonable maxima to avoid oversized queries.
