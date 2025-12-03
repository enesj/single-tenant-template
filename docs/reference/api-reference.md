<!-- ai: {:tags [:reference :backend :http :single-tenant] :kind :reference} -->

# HTTP API Reference (Single-Tenant)

Current HTTP surface for the single-tenant template. Admin endpoints live under `/admin/api` at `http://localhost:8085`. User-facing endpoints live under `/api/v1` at the same host.

## Base

- **Base URL (dev)**: `http://localhost:8085`
- **Admin API**: `/admin/api/*` – requires admin token
- **User API**: `/api/v1/*` – some public, some require user session
- **Auth**: Admin token via `x-admin-token` header or `admin-token` cookie. User auth via Ring session cookie.
- **Content-Type**: `application/json`
- **Success shape**: `{:success true :data {...}}` (some handlers return top-level keys such as `{:users [...]}`)
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

## Admin Authentication (`/admin/api`, public)

### POST /admin/api/login
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

### POST /admin/api/logout
Clears the admin session and writes a `logout` audit entry for the current admin when available.

---

## User Authentication (`/api/v1/auth`, public)

### POST /api/v1/auth/register
Creates a new user account with email/password. Triggers email verification.

Body:
```json
{ "email": "user@example.com", "full-name": "John Doe", "password": "securepassword123" }
```
Response:
```json
{
  "success": true,
  "verification-required": true,
  "message": "Registration successful. Please check your email for verification."
}
```
Sets a Ring session cookie for the new user.

### POST /api/v1/auth/login
Authenticates a user with email and password.

Body:
```json
{ "email": "user@example.com", "password": "securepassword123" }
```
Response:
```json
{ "success": true, "user": {"id":"<uuid>","email":"user@example.com","full-name":"John Doe","role":"member"} }
```
Sets a Ring session cookie. Records a login event.

### GET /auth/status
Returns current authentication status (works for both admin and user sessions).

Response (authenticated):
```json
{
  "authenticated": true,
  "session-valid": true,
  "user": {"id":"<uuid>","email":"user@example.com","full-name":"John Doe","role":"member"},
  "permissions": ["read", "write"]
}
```

Response (not authenticated):
```json
{ "authenticated": false }
```

### POST /auth/logout
Clears the user session and redirects to `/about`.

---

## Password Reset (`/api/v1/auth`, public)

### POST /api/v1/auth/forgot-password
Initiates password reset flow. Sends reset email if user exists.

Body:
```json
{ "email": "user@example.com" }
```
Response:
```json
{ "success": true, "message": "Password reset instructions sent" }
```

### GET /api/v1/auth/verify-reset-token
Verifies a password reset token is valid.

Query: `?token=<reset-token>`

Response:
```json
{ "valid": true }
```

### POST /api/v1/auth/reset-password
Resets password using a valid token.

Body:
```json
{ "token": "<reset-token>", "new-password": "newsecurepassword123" }
```
Response:
```json
{ "success": true, "message": "Password reset successful" }
```

### POST /api/v1/auth/change-password
Changes password for authenticated user. Requires user session.

Body:
```json
{ "current-password": "oldpassword", "new-password": "newpassword123" }
```
Response:
```json
{ "success": true, "message": "Password changed successfully" }
```

---

## Email Verification (`/api/v1/auth`, public)

### GET /api/v1/auth/verification-status
Returns email verification status for the current session.

### POST /api/v1/auth/resend-verification
Resends verification email for the current session user.

---

## OAuth (`/login`, public)

### GET /login/google
Initiates Google OAuth flow. Redirects to Google for authentication.

On success, creates or updates user record and sets Ring session cookie.

**Security**: OAuth cannot overwrite existing password-based accounts. Users who registered with email/password must use that method.

---

## Admin Dashboard (`/admin/api`, protected)

### GET /admin/api/dashboard
Returns the summary payload used by the admin shell (counts/overview for users and recent activity).

---

## Admin Users (`/admin/api`, protected)

### GET /admin/api/users
Query params: `search`, `status`, `email-verified`, `page`, `per-page`  
Response: `{:users [...]}` where each user includes normalized keys (e.g., `:id`, `:email`, `:full-name`, `:role`, `:status`, `:last-login-at`).

### GET /admin/api/users/:id
Response: `{:user {...}}`

### POST /admin/api/users
Creates a user. Body includes standard fields (`email`, `full_name`, `password`, `role`, `status`, etc.).  
Response: `{:user {...}}`

### PUT /admin/api/users/:id
Updates user fields.  
Response: updated user map (top-level keys, not wrapped).

---

## Admin User Operations (`/admin/api/user-management`, protected)

- `PUT /admin/api/user-management/role/:id` – body `{:role "owner|admin|member|viewer"}` → `{:success true}`
- `POST /admin/api/user-management/verify-email/:id` → `{:success true :message "Email verified"}`
- `POST /admin/api/user-management/reset-password/:id` → `{:success <bool> :message ...}`
- `POST /admin/api/user-management/impersonate/:id` → `{... :success true :token <user-session-token>}`
- `GET /admin/api/user-management/search` – filters: `search`, `status`, `email-verified`, `role`, `auth-provider`, `sort-by`, `sort-order`, pagination. Response: `{:users [...]}`.
- `GET /admin/api/user-management/activity/:id` – query `page`/`per-page`. Response:
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

## Admin Audit Logs (`/admin/api`, protected)

### GET /admin/api/audit
Filters: `admin-id`, `entity-type`, `entity-id`, `action`, `limit`, `offset`  
Response: `{:logs [...]}` where each log includes normalized keys such as `:id`, `:actor-type`, `:actor-id`, `:action`, `:target-type`, `:target-id`, `:entity-name` (when resolvable), `:changes`, `:ip-address`, `:created-at` (epoch millis).

### DELETE /admin/api/audit/:id
Deletes an audit row (admin-only; mainly for local cleanup). Returns `{:success true :message "Audit log deleted successfully"}` or 404.

---

## Admin Login Events (`/admin/api`, protected)

### GET /admin/api/login-events
Filters: `principal-type` (`admin|user`), `success` (`true|false`), `limit` (default 100), `offset`.  
Response: `{:events [...]}` with fields:
- `:id`, `:principal-id`, `:principal-type`
- `:principal-name`, `:principal-email` (when resolved)
- `:success`, `:reason`
- `:ip-address`, `:user-agent`
- `:created-at` (epoch millis)

---

## Admin Dev Helpers (`/admin/api`, local only)

- `GET /admin/api/dev-get-rate-limits`
- `POST /admin/api/dev-clear-rate-limits`
- `PUT /admin/api/test-put` – simple echo/test endpoint

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
