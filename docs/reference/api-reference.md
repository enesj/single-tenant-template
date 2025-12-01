<!-- ai: {:tags [:reference :backend :http :single-tenant] :kind :reference} -->

# HTTP API Reference (Single-Tenant Admin)

Current admin HTTP surface for the single-tenant template. All endpoints live under `/admin/api` and are served at `http://localhost:8085` in development.

## Base

- **Base URL (dev)**: `http://localhost:8085/admin/api`
- **Auth**: Admin token via `x-admin-token` header or the `admin-token` session cookie set by `/auth/login`.
- **Content-Type**: `application/json`
- **Success shape**: `{:success true :data {...}}` (some handlers return top-level keys such as `{:users [...]}` or `{:logs [...]}`)
- **Error shape**: `{:success false :error {:message \"...\"}}`

### Common headers
```
Content-Type: application/json
x-admin-token: <token>
X-Request-ID: <optional trace id>
```

### Pagination helpers
- `page` / `per-page` – used by user listing and activity endpoints (defaults come from `admin.utils/extract-pagination-params`).
- `limit` / `offset` – used by audit and login-event listing (default limit 100 if not provided).

---

## Authentication (public)

### POST /auth/login
Body:
```json
{ "email": "admin@example.com", "password": "pass" }
```
Response:
```json
{
  "success": true,
  "data": {
    "admin": {"id":"<uuid>","email":"admin@example.com","full_name":"System Administrator","role":"owner"},
    "token": "<admin-token>"
  }
}
```
Also records a login event and an audit log entry for the admin.

### POST /auth/logout
Clears the admin session and writes an `logout` audit entry for the current admin when available.

---

## Dashboard (protected)

### GET /dashboard
Returns the summary payload used by the admin shell (counts/overview for users and recent activity).

---

## Users (protected)

### GET /users
Query params: `search`, `status`, `email-verified`, `page`, `per-page`  
Response: `{:users [...]}` where each user includes normalized keys (e.g., `:id`, `:email`, `:full-name`, `:role`, `:status`, `:last-login-at`).

### GET /users/:id
Response: `{:user {...}}`

### POST /users
Creates a user. Body includes standard fields (`email`, `full_name`, `password`, `role`, `status`, etc.).  
Response: `{:user {...}}`

### PUT /users/:id
Updates user fields.  
Response: updated user map (top-level keys, not wrapped).

---

## Advanced User Operations (protected, `/user-management`)

- `PUT /user-management/role/:id` – body `{:role "owner|admin|member|viewer"}` → `{:success true}`
- `POST /user-management/verify-email/:id` → `{:success true :message "Email verified"}`
- `POST /user-management/reset-password/:id` → `{:success <bool> :message ...}`
- `POST /user-management/impersonate/:id` → `{... :success true :token <user-session-token>}`
- `GET /user-management/search` – filters: `search`, `status`, `email-verified`, `role`, `auth-provider`, `sort-by`, `sort-order`, pagination. Response: `{:users [...]}`.
- `GET /user-management/activity/:id` – query `page`/`per-page`. Response:
  ```json
  {
    "success": true,
    "data": {
      "activity": {
        "audit-logs": [...],
        "login-history": [...],
        "summary": {
          "total-actions": 0,
          "recent-logins": 0,
          "last-activity": 1700000000000,
          "last-login": 1700000000000
        }
      }
    }
  }
  ```

---

## Audit Logs (protected)

### GET /audit
Filters: `admin-id`, `entity-type`, `entity-id`, `action`, `limit`, `offset`  
Response: `{:logs [...]}` where each log includes normalized keys such as `:id`, `:actor-type`, `:actor-id`, `:action`, `:target-type`, `:target-id`, `:entity-name` (when resolvable), `:changes`, `:ip-address`, `:created-at` (epoch millis).

### DELETE /audit/:id
Deletes an audit row (admin-only; mainly for local cleanup). Returns `{:success true :message "Audit log deleted successfully"}` or 404.

---

## Login Events (protected)

### GET /login-events
Filters: `principal-type` (`admin|user`), `success` (`true|false`), `limit` (default 100), `offset`.  
Response: `{:events [...]}` with fields:
- `:id`, `:principal-id`, `:principal-type`
- `:principal-name`, `:principal-email` (when resolved)
- `:success`, `:reason`
- `:ip-address`, `:user-agent`
- `:created-at` (epoch millis)

---

## Dev Helpers (local only)

- `GET /dev-get-rate-limits`
- `POST /dev-clear-rate-limits`
- `PUT /test-put` – simple echo/test endpoint

---

## Error examples

```json
{ "success": false, "error": { "message": "Invalid credentials" } }
```

```json
{ "success": false, "error": { "message": "Missing role", "details": { "allowed": ["owner","admin","member","viewer"] } } }
```

---

**Related**:  
- [Backend HTTP guide](../backend/http-api.md)  
- [Admin frontend HTTP standards](../frontend/http-standards.md)  
- [Security middleware](../backend/security-middleware.md)
