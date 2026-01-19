<!-- ai: {:tags [:backend :reference-only :single-tenant] :kind :note} -->

# Hosting Domain (Reference Only)

The hosting/property domain from the multi-tenant app is **not present** in this single-tenant template. Keep this file only as an indicator that the old docs no longer apply.

## Current State
- No hosting/property services under `src/app/domain`.
- No hosting tables in `resources/db/models.edn`.
- Admin UI focuses on users + monitoring (audit/logins), not listings/bookings.

## If You Add Hosting Later
- Model tables in `resources/db/models.edn` and regenerate migrations.
- Add services/routes under a new namespace and document the endpoints in `http-api.md`.
- Reuse security middleware and monitoring hooks (audit/login events) for admin actions.
