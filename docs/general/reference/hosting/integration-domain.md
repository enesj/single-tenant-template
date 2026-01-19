<!-- ai: {:tags [:backend :reference-only :single-tenant] :kind :note} -->

# Integration Domain (Reference Only)

The old cross-domain “Integration” layer (orchestration, dashboards, onboarding) is **not part of** this single-tenant app. This note exists only to signal that the previous multi-tenant content was removed.

## Current State
- No integration-domain namespaces are in use.
- Dashboards are simple admin pages driven by the admin backend and monitoring services.
- Onboarding/tenant flows are not present.

## If You Need Cross-Cutting Flows
- Add new orchestration services under your own namespace and document their endpoints in `http-api.md`.
- Keep auth + security middleware intact for any admin routes you add.
- Prefer reusing template/shared utilities instead of reintroducing the multi-tenant integration stack.
