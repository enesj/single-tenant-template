<!-- ai: {:namespaces [app.template.backend.middleware.security] :tags [:backend :security :single-tenant] :kind :reference} -->

# Security Middleware (Single-Tenant)

This app uses a small, production-ready Ring stack that wraps all admin routes. Multi-tenant/RLS-specific notes were removed; this doc reflects the current single-tenant setup.

## Stack
Applied in `app.template.backend.middleware.security` (see `wrap-security`):
1. **HTTPS enforcement** (`force-https-middleware`) – redirects HTTP→HTTPS outside local/dev. Set `DISABLE_HTTPS_REDIRECT=true` to bypass for testing.
2. **Rate limiting** (`app.template.backend.middleware.rate-limiting/wrap-rate-limiting`, optional) – can be enabled; dev helpers exist at `/admin/api/dev-get-rate-limits` and `/admin/api/dev-clear-rate-limits`.
3. **Security headers** (`security-headers-middleware`) – adds X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, and stricter Cache-Control/CSP for admin routes.

Admin auth (`app.template.backend.middleware.admin/wrap-admin-authentication`) is applied separately inside the admin route tree; keep it in place for any `/admin/api/**` additions.

## Admin Session Management
- **Token generation**: UUID-based session tokens created on successful login.
- **Session storage**: In-memory atom (`session-store`) – sessions do not persist across server restarts.
- **Session expiry**: 8 hours from creation.
- **Token sources** (checked in order):
  1. `x-admin-token` HTTP header
  2. `:admin-token` in Ring session
  3. `admin-token` cookie
- **Activity tracking**: `update-session-activity!` refreshes `last-activity` timestamp on each authenticated request.
- **Invalidation**: `invalidate-session!` removes a single session; `invalidate-all-admin-sessions!` clears all sessions for an admin.

## Usage
Wrap your Ring handler (already wired in `app.template.backend.routes/app-routes`):
```clojure
(-> routes
    security/wrap-security) ; HTTPS → rate limit → headers
```

## Configuration
- `DISABLE_HTTPS_REDIRECT=true` – skip HTTPS redirect (dev only).
- `DISABLE_RATE_LIMITING=true` – bypass rate limits (dev only).
- CSP is relaxed for localhost (allows `unsafe-inline`/`unsafe-eval` for shadow-cljs); tighten in production as needed.

## Testing
- Hit `PUT /admin/api/test-put` to confirm middleware pipeline accepts PUT.
- Check logs for `🔐 ADMIN AUTH CHECK` and `🌐 GLOBAL REQUEST DEBUG` to confirm middleware ordering.

Keep any new middleware admin-safe (no token leakage, no JSON body rewrites that break auth). Update this doc if the order or behavior changes.
