<!-- ai: {:tags [:backend :http :security :template] :kind :guide} -->

# Generic Entity CRUD API (`/api/v1/entities/*`)

The template exposes a *generic*, metadata-driven CRUD surface under:

- `POST /api/v1/entities/{entity}`
- `PUT /api/v1/entities/{entity}/{id}`
- `DELETE /api/v1/entities/{entity}/{id}`

This API exists to support template components that can work against “entities” in a uniform way (lists, forms, bulk actions, etc.) **without** every feature needing its own bespoke HTTP wiring.

## What it is (and what it is not)

### Intended use

Use `/api/v1/entities/*` when all of the following are true:

- The entity is safe to operate on with **generic semantics** (create/update/delete are straightforward).
- The backend can safely enforce access rules with the generic middleware.
- You want to plug an entity into template lists/forms with minimal extra code.

### Not intended for domain/business entities by default

Many domain entities need business rules such as:

- ownership checks ("only delete my own rows")
- soft-delete semantics
- side effects (cascades, audit, invariants)
- role gating per operation

For those cases, prefer **domain endpoints** like `/api/v1/expenses/*` and route template CRUD actions to them via a **frontend CRUD bridge override**.

## Security model (deny-by-default)

The generic entity endpoints are guarded by user auth and an **entity access layer** that is intentionally *deny-by-default*:

- Unknown entities are blocked for safety.
- Only explicitly allowlisted entities are permitted.
- This prevents accidentally exposing sensitive tables/operations via a generic router.

If you hit a 403 with a message like:

- “Unknown entity - blocking for security”

…it usually means a template CRUD operation tried to call `/api/v1/entities/{entity}` for an entity that is **not allowlisted**.

## Practical guidance

### If a template UI action 403s

1. Check the Network request URL. If it’s `/api/v1/entities/<something>`, you’re on the generic path.
2. Decide whether `<something>` is safe for generic CRUD.
   - If **yes**, add it to the allowlist (and ensure handlers enforce the right per-user/role checks).
   - If **no** (common for domain entities), create/use a domain API and add a **CRUD bridge** so the UI routes to the domain API instead.

### Example: expenses delete

Expenses deletes should go through the user-scoped expenses API:

- `DELETE /api/v1/expenses/:id`

rather than the generic endpoint:

- `DELETE /api/v1/entities/expenses/:id`

because expenses deletion needs ownership and (typically) soft-delete semantics.

## Where it lives in code

- Generic CRUD routes: `src/app/template/backend/routes/crud.clj`
- API composition (mounting `/api/v1/entities`): `src/app/template/backend/routes/api.clj`
- Entity access control (allowlist / blocking): `src/app/template/backend/security/entity_access.clj`
- Frontend routing decision (admin vs user context): `src/app/template/frontend/api/http.cljs`
- Frontend overrides: `app.shared.frontend.bridges.crud` (CRUD bridge system)
