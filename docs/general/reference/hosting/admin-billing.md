<!-- ai: {:tags [:backend :single-tenant :reference-only] :kind :note} -->

# Admin Billing (Not Used)

This single-tenant app **does not ship billing or subscriptions**. The previous multi-tenant billing guide was removed because it no longer reflects the codebase.

## Current State
- No billing tables, services, or routes are present.
- The admin UI has no billing pages.
- Payment provider integrations are intentionally absent.

## If You Reintroduce Billing
- Model schema in `resources/db/models.edn`, regenerate migrations, and add dedicated services/routes under `/admin/api/billing/*` with admin auth + security middleware.
- Document any new endpoints and UI flows here once implemented.
- Keep billing concerns isolated from monitoring/audit so the current login/audit flows stay unaffected.

Until billing returns, treat this file as a placeholder to avoid confusion with the former multi-tenant system.
