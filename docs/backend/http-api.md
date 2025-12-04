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
