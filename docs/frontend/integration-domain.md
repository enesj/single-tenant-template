<!-- ai: {:tags [:frontend] :kind :guide} -->

# Integration Domain (Single-Tenant Status)

The single-tenant app does not use the legacy “integration domain” (cross-tenant onboarding, property/hosting/financial orchestration). Those flows and components have been removed.

## Current Scope

- Admin console only: users, audit logs, login events, dashboard cards.
- No tenant switching, property onboarding, or cross-domain workflows.
- Shared UI components still live under `app.template.frontend.components.*` and are reused by admin pages.

## If You Add Integrations Later

- Create new pages/events/subs under a dedicated namespace (e.g., `app.integration.frontend.*`).
- Reuse template components and admin HTTP helpers where possible.
- Keep routes separate from admin unless the feature is admin-only.

For now, treat this domain as unused in single-tenant and avoid resurrecting multi-tenant examples.
