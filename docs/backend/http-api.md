<!-- ai: {:namespaces [app.backend.routes.admin-api] :tags [:backend :http :single-tenant] :kind :reference} -->

# HTTP API (Single-Tenant Admin)

This is the current admin API surface for the single-tenant app. All routes live under `/admin/api` on **http://localhost:8085** and are secured by admin auth middleware.

## Base Shape
- **Auth**: `app.backend.middleware.admin/wrap-admin-authentication` expects an admin token (dev mode may relax). Pass `x-admin-token: <token>` or the token cookie set by the admin login flow.
- **Content**: JSON request/response. Success responses use `{:success true :data ...}`; errors use `{:success false :error {:message ...}}`.
- **Middleware**: JSON body parsing + security headers + admin auth (for protected routes). Rate-limiting hooks are available but may be disabled in dev.

## Route Map (Key Endpoints)
All paths are relative to `/admin/api`.

### Auth (public)
- `POST /admin/api/login` – exchange credentials for admin token (namespace `app.backend.routes.admin.auth`).
- `POST /admin/api/logout` – invalidate token.

### Dashboard (protected)
- `GET /admin/api/dashboard` – summary payload for the admin shell (namespace `app.backend.routes.admin.dashboard`).

### Settings (protected, `app.backend.routes.admin.settings`)
- `GET /admin/api/settings` – return `{:view-options {...}}` from `resources/public/admin/ui-config/view-options.edn`.
- `PUT /admin/api/settings` – replace the entire `view-options` map.
- `PATCH /admin/api/settings/entity` – upsert a single entity setting (`entity-name`, `setting-key`, `setting-value`).
- `DELETE /admin/api/settings/entity` – remove a hardcoded setting so it becomes user-configurable again.

### Users (protected)
- `GET /admin/api/users` – list users (supports pagination/filtering via query params in `admin-utils/extract-pagination-params`).
- `POST /admin/api/users` – create user.
- `GET /admin/api/users/:id` – fetch user.
- `PUT /admin/api/users/:id` – update user.
- `DELETE /admin/api/users/:id` – delete/deactivate user.

### Advanced User Operations (protected, `app.backend.routes.admin.user-operations`)
- `PUT /admin/api/user-management/role/:id` – update role (`owner|admin|member|viewer`).
- `POST /admin/api/user-management/verify-email/:id` – force email verification.
- `POST /admin/api/user-management/reset-password/:id` – reset password.
- `GET /admin/api/user-management/activity/:id` – aggregated audit + login activity for the user (supports pagination via `page`/`per-page`).
- `POST /admin/api/user-management/impersonate/:id` – create impersonation session.
- `GET /admin/api/user-management/search` – advanced search (filters: `search`, `status`, `email-verified`, `role`, `auth-provider`, `sort-by`, `sort-order`, pagination).

### Audit Logs (protected, `app.backend.routes.admin.audit`)
- `GET /admin/api/audit` – list audit events with filters: `principal-id`, `principal-type` (`admin|user`), `action`, `from`/`to` (ISO timestamps), `limit`, `offset`. Returns normalized keys ready for admin UI tables/export.
- `DELETE /admin/api/audit/:id` – delete a single audit log (admin action; RLS bypassed within txn).
- `DELETE /admin/api/audit/bulk` – delete multiple audit logs; body `{:ids [<uuid> ...]}`.

### Login Events (protected, `app.backend.routes.admin.login-events`)
- `GET /admin/api/login-events` – list login events for admins and users. Filters: `principal-type` (`admin|user`), `success?` (`true|false`), `limit` (default 100), `offset` (default 0). Response fields include `principal-id`, `principal-name/email` when available, `ip-address`, `user-agent`, `created-at`, and `reason`.
- `DELETE /admin/api/login-events/:id` – delete a login event (admin action; RLS bypassed within txn).
- `DELETE /admin/api/login-events/bulk` – delete multiple login events; body `{:ids [<uuid> ...]}`.

### Home Expenses (protected, mounted at `/admin/api/expenses`, `app.domain.expenses.routes.*`)
**Suppliers**
- `GET /admin/api/expenses/suppliers` – list (search, pagination, order-by).
- `POST /admin/api/expenses/suppliers` – create; requires `display_name`.
- `GET /admin/api/expenses/suppliers/count` – total (optional `search`).
- `GET /admin/api/expenses/suppliers/search?q=...` – autocomplete.
- `GET /admin/api/expenses/suppliers/:id` – fetch.
- `PUT /admin/api/expenses/suppliers/:id` – update.
- `DELETE /admin/api/expenses/suppliers/:id` – delete (fails if referenced).

**Payers**
- `GET /admin/api/expenses/payers` – list (optional `type`).
- `POST /admin/api/expenses/payers` – create; requires `type`, `label`.
- `GET /admin/api/expenses/payers/count` – total (optional `type`).
- `GET /admin/api/expenses/payers/suggest` – suggest from `method`/`card_last4`.
- `GET /admin/api/expenses/payers/default/:type` – fetch default for type.
- `POST /admin/api/expenses/payers/:id/default` – set default for payer’s type.
- `GET /admin/api/expenses/payers/:id` – fetch; `PUT` update; `DELETE` remove.

**Receipts**
- `GET /admin/api/expenses/receipts` – list; filters `status`, `limit/offset`, `order-dir`.
- `GET /admin/api/expenses/receipts/pending` – pending for processing.
- `POST /admin/api/expenses/receipts` – upload; requires `storage_key` and `file_hash` or `bytes`.
- `GET /admin/api/expenses/receipts/:id` – fetch one.
- `POST /admin/api/expenses/receipts/:id/status` – set status.
- `POST /admin/api/expenses/receipts/:id/retry` – reset to uploaded + bump retry.
- `POST /admin/api/expenses/receipts/:id/fail` – mark failed with message/details.
- `POST /admin/api/expenses/receipts/:id/extraction` – store extraction payloads/guesses.
- `POST /admin/api/expenses/receipts/:id/approve` – approve + create expense + mark posted.

**Expenses**
- `GET /admin/api/expenses/entries` – list; filters `from/to`, `supplier-id`, `payer-id`, `is-posted?`, pagination.
- `POST /admin/api/expenses/entries` – create expense with `items`.
- `GET /admin/api/expenses/entries/:id` – fetch with items.
- `PUT /admin/api/expenses/entries/:id` – update expense fields.
- `DELETE /admin/api/expenses/entries/:id` – soft delete.

**Articles / Price history**
- `GET /admin/api/expenses/articles` – list/search.
- `POST /admin/api/expenses/articles` – create; requires `canonical_name`.
- `GET /admin/api/expenses/articles/unmapped-items` – expense items missing article mapping.
- `POST /admin/api/expenses/articles/items/:item-id/map` – attach article to item (optional alias create).
- `GET /admin/api/expenses/articles/:id` – fetch article.
- `POST /admin/api/expenses/articles/:id/aliases` – add/replace alias for supplier/raw label.
- `GET /admin/api/expenses/articles/:id/price-history` – price observations (optional `supplier_id`, `limit`).
- `GET /admin/api/expenses/articles/:id/latest-prices` – latest price per supplier.
- `GET /admin/api/expenses/articles/:id/compare` – price observations for comparisons (optional `from`, `limit`).

**Reports**
- `GET /admin/api/expenses/reports/summary` – totals for range.
- `GET /admin/api/expenses/reports/payers` – breakdown by payer.
- `GET /admin/api/expenses/reports/suppliers` – breakdown by supplier.
- `GET /admin/api/expenses/reports/weekly` – weekly totals.
- `GET /admin/api/expenses/reports/monthly` – monthly totals.
- `GET /admin/api/expenses/reports/top-suppliers` – top suppliers (optional `limit`).

### Dev Helpers (no auth; for local debugging only)
- `GET /admin/api/dev-get-rate-limits` – inspect rate-limit state.
- `POST /admin/api/dev-clear-rate-limits` – reset rate limits.
- `PUT /admin/api/test-put` – simple request sanity check.

## Example Requests

### Fetch user activity (modal)
```
GET /admin/api/user-management/activity/a55fef53-f21e-4860-8c73-70bf7dc2ce57?page=1&per-page=20
x-admin-token: <token>
```
Response (`data.activity`) contains audit entries, login history, and derived counters (recent logins, last login/activity).

### List login events
```
GET /admin/api/login-events?principal-type=admin&limit=50
x-admin-token: <token>
```
Returns `{:success true :data {:events [...], :total <count>}}` with normalized `principal-name/principal-email` fields when resolvable.

### Fetch audit logs
```
GET /admin/api/audit?principal-type=user&principal-id=a55fef53-f21e-4860-8c73-70bf7dc2ce57&limit=25
x-admin-token: <token>
```
Returns paginated audit rows suitable for the global audit page and per-user modal.

## Notes for Contributors
- Add new admin endpoints under `src/app/backend/routes/admin/*` and compose them in `app.backend.routes.admin-api/admin-api-routes`.
- Keep responses normalized (`success-response`/`error-response` helpers in `admin-utils`).
- Update this file when endpoints or parameters change.
