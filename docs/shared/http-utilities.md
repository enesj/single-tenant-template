<!-- ai: {:namespaces [app.shared.http] :tags [:shared :http :single-tenant] :kind :reference} -->

# HTTP Utilities

Cross-platform HTTP constants and helpers used by the single-tenant admin backend and UI.

- **Location**: `src/app/shared/http.cljc`
- **What it covers**: status codes, header names/constructors, JSON helpers, and response helpers used in tests and UI API clients.

## Core pieces
- **Status codes**: `status-ok`, `status-created`, `status-bad-request`, etc.
- **Headers**: `header-content-type`, `header-authorization`, `header-user-agent`; content types like `content-type-json`.
- **Helpers**:
  - `success?`, `client-error?`, `server-error?`
  - `create-success-response`, `create-error-response`
  - `create-json-headers`, `create-auth-headers`, `merge-headers`
  - `is-json?`, `extract-error-message`

## Usage notes
- Shared between backend handlers/tests and frontend API wrappers to keep error handling consistent.
- Responses are JSON-only in this app; use `create-json-headers` and `content-type-json`.
- No tenant headers are required—admin auth uses `x-admin-token` or the session cookie.
