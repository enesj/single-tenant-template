# PLAN-expenses-admin

Goal: Surface additional expenses-domain tables (articles, article_aliases, price_observations) in the admin panel with working list views/CRUD, matching the pattern used for suppliers/payers/receipts/expenses.

## Phases
1) Recon & API fit
- [x] Confirm existing admin API endpoints for articles, article_aliases, and price_observations (list/create/update/delete). Note gaps (e.g., aliases or price observations may lack list endpoints).
- [x] Decide per-entity whether to reuse existing endpoints or add new ones. Implemented full admin CRUD endpoints for articles, article_aliases, and price_observations to match FE needs.

2) Navigation & routing
- [x] Add sidebar links and admin route entries for articles, article aliases, and price observations (similar to suppliers/payers/receipts).
- [x] Add minimal pages that render `generic-admin-entity-page` for each entity key.

3) Data adapters & events
- [x] Add entity registry entries and adapter init fns for new entities.
- [x] Implement list/load events + CRUD bridges (if supported) pointing at the correct admin API endpoints.

4) UI config updates
- [x] Extend `resources/public/admin/ui-config` (entities.edn, table-columns.edn, form-fields.edn, view-options.edn if needed) with sensible defaults for the three entities and fill missing view-options for existing expenses entities (suppliers, payers, etc.).

5) Verification
- [ ] Manual check in admin UI: navigation highlights, lists load without 405, create/edit/delete where applicable (articles, article-aliases, price-observations, plus regression for suppliers/payers).
- [ ] Optional: run `npm run test:cljs` (frontend) if time permits.

Notes: Mirror the supplier/payer pattern for CRUD bridges to avoid template generic endpoints (which returned 405 previously). If backend lacks list endpoints for aliases/price observations, add them or adjust scope accordingly.

## Findings / Context (this session)
- Sidebar links live in `src/app/admin/frontend/components/layout.cljs`; domain section already has Expenses, New Expense, Receipts, Suppliers, Payers. New tables should be added here.
- Admin routes are composed in `src/app/admin/frontend/routes.cljs`; domain routes come from `app.domain.expenses.frontend.routes`.
- Domain FE routes file: `src/app/domain/expenses/frontend/routes.cljs` currently exposes /expenses, /expenses/new, /expenses/:id, /receipts, /suppliers, /payers. No routes yet for articles/aliases/price observations.
- Generic entity page component is `app.admin.frontend.components.generic-admin-entity-page`; per-entity pages for expenses domain live in `src/app/domain/expenses/frontend/pages/*.cljs` (suppliers, payers, receipts, etc.).
- Entity registry (for adapter init + actions) is `src/app/admin/frontend/system/entity_registry.cljs`; currently registers :receipts, :suppliers, :payers (plus core admin entities).
- Expenses admin adapters + CRUD bridges are in `src/app/admin/frontend/adapters/expenses.cljs`. Bridges already exist for :suppliers, :payers, :expenses, :receipts pointing at `/admin/api/expenses/{suppliers|payers|entries|receipts}` and refresh their lists on success. Follow this pattern for new entities.
- Form submit interceptor that reroutes admin CRUD through bridges is `src/app/admin/frontend/events/users/template/form_interceptors.cljs`; currently handles :suppliers, :payers, :expenses, :receipts (and :users). New entity keys must be added here to avoid 405s.
- Admin UI configs are static EDN files under `resources/public/admin/ui-config/`:
  - `entities.edn` describes page titles, adapter init fns, display flags for :expenses/:receipts/:suppliers/:payers.
  - `table-columns.edn` has column layouts for :expenses, :receipts, :suppliers, :payers (no articles/aliases/price_observations yet).
  - `form-fields.edn` defines create/edit fields for :suppliers and :payers; none for articles/aliases/price_observations.
  - `view-options.edn` currently only has admin core entities; no expense-domain entries besides those above.
- Backend routing: admin API mounts expenses domain under `/admin/api/expenses` via `src/app/domain/expenses/routes/core.clj`. Added (this session) imports for `article-aliases` and `price-observations` namespaces; need corresponding route namespaces implemented/exposed.
- Existing backend article/alias/price-observation services:
  - Article CRUD + alias creation + price history endpoints defined in `src/app/domain/expenses/routes/articles.clj` (under `/admin/api/expenses/articles` when mounted).
  - Price history service functions in `src/app/domain/expenses/services/price_history.clj`; tables `article_aliases` and `price_observations` present in `resources/db/domain/models.edn`.
- Navigation/API mismatch root-cause for 405 earlier: template default CRUD endpoints differ; solved for suppliers/payers/expenses/receipts via admin CRUD bridges. Repeat for new entities.
- New this session: implemented dedicated services + admin API route modules for `article-aliases` and `price-observations`, and added update/delete handlers for articles so full CRUD is available at `/admin/api/expenses/articles`, `/article-aliases`, and `/price-observations`.
- Added logging in admin config loader to list available entity keys when configs are missing; view-options now populated for suppliers/payers/etc. to suppress prior warnings.
