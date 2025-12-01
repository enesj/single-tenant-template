<!-- ai: {:tags [:frontend] :kind :guide} -->

# Admin Authentication Flow (Single-Tenant)

## Overview

Single-tenant app with an admin console served at `http://localhost:8085/admin`. Authentication gates all admin routes except `/admin/login`. This guide covers the frontend flow, redirects, and guard patterns now that tenant signup/onboarding and multi-tenant roles are removed.

## UX Flow

- Unauthenticated visitor hitting any admin route is redirected to `/admin/login`.
- Successful admin login stores the bearer token in browser storage (transient) and boots the console (dashboard, users, audit, login events).
- Logout clears the token and returns to `/admin/login`.

Optional public landing (`:app` build) can link to `/admin/login`; it no longer offers tenant signup/onboarding.

## Route Flow

```
/admin/login  → unauthenticated entry
/admin        → dashboard (guarded)
/admin/users  → user management + per-user activity modal (guarded)
/admin/audit  → global audit logs (guarded)
/admin/login-events → global login events (guarded)
```

`guarded-start` in `app.admin.frontend.routes` dispatches route controllers only after admin auth is confirmed. Login page stays open to all.

## Events and Guards

- Auth events: `app.admin.frontend.events.auth` handles login, token storage, logout, and session bootstrap.
- Guard: `:admin/check-auth-protected` runs before route controllers; on missing/invalid token it redirects to `/admin/login`.
- API calls: use `/admin/api/*` with `Authorization: Bearer <token>`; failures at 401 trigger logout/redirect.

## Implementation Notes

- Admin init (`app.admin.frontend.core/init-admin!`) registers events/subs, applies theme, and fetches UI config.
- Token is stored in local/session storage; avoid keeping sensitive data in app-db beyond current admin and table rows.
- Per-user activity modal fetches audit + login events for that user; global pages list all events with pagination.

## Manual Test Steps

1) Open `/admin` while logged out → expect redirect to `/admin/login`.
2) Login as admin → expect dashboard to load; token stored.
3) Navigate to `/admin/audit` and `/admin/login-events` → data loads; stays authenticated.
4) Clear storage or click logout → redirected to `/admin/login`; protected routes no longer load data.

## Console Checks

- No `re-frame: no :event handler registered` errors.
- Requests to `/admin/api/*` include `Authorization` header.
- On 401 responses, app dispatches logout and redirects to `/admin/login`.

## Future Enhancements

- Add idle-timeout/refresh handling for tokens.
- Surface clearer error toasts on auth failures.
- Optional public landing CTA that points directly to `/admin/login`.
