<!-- ai: {:namespaces [app.backend.middleware.security] :tags [:backend :security :single-tenant] :kind :reference} -->

# Security Middleware (Single-Tenant)

This app uses a small, production-ready Ring stack that wraps all admin routes. Multi-tenant/RLS-specific notes were removed; this doc reflects the current single-tenant setup.

## Stack
Applied in `app.backend.middleware.security` (see `wrap-security`):
1. **HTTPS enforcement** (`force-https-middleware`) – redirects HTTP→HTTPS outside local/dev. Set `DISABLE_HTTPS_REDIRECT=true` to bypass for testing.
2. **Rate limiting** (`app.backend.middleware.rate-limiting/wrap-rate-limiting`, optional) – can be enabled; dev helpers exist at `/admin/api/dev-get-rate-limits` and `/admin/api/dev-clear-rate-limits`.
3. **Security headers** (`security-headers-middleware`) – adds X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, and stricter Cache-Control/CSP for admin routes.

Admin auth (`app.backend.middleware.admin/wrap-admin-authentication`) is applied separately inside the admin route tree; keep it in place for any `/admin/api/**` additions.

## Usage
Wrap your Ring handler (already wired in `app.backend.routes/create-app`):
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
