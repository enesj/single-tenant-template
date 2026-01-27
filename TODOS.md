# TODOS

## Fallback cleanup tracker

Goal: remove scattered "fallback" lookups (snake/camel/kebab OR-chains, string-vs-keyword map access, response envelope guessing) by enforcing a strict canonical contract.

Canonical contract (app/runtime):

- Keywords: kebab-case (e.g. `:canonical-name`, `:supplier-id`)
- IDs: one consistent representation per layer (backend UUIDs; frontend typically UUID strings). Avoid dual lookups.
- API responses: entity-keyed maps (e.g. `{:articles [...]}`), not ambiguous `{:data ...}`

### Completed

- `src/app/domain/frontend/expenses/pages/user/articles.cljs`
- `src/app/domain/frontend/expenses/pages/user/price_observations.cljs`
- `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`
- `src/app/domain/frontend/expenses/events/suppliers.cljs`
- `src/app/domain/frontend/expenses/events/user_expenses/lookups.cljs` (FormData uses canonical key names)
- `src/app/domain/frontend/expenses/components/form_fields/selects.cljs`
- `src/app/domain/frontend/expenses/components/unmapped_items.cljs`
- `src/app/domain/frontend/expenses/components/user_power_forms.cljs`
- `src/app/domain/backend/expenses/routes/routes_factory.clj` (normalize at HTTP boundary)
- `src/app/domain/backend/expenses/routes/route_configs.clj`
- `src/app/domain/backend/expenses/handlers/receipt_upload.clj` (normalize multipart inputs at boundary)

Follow-up bugfix triggered by the above:

- `src/app/domain/backend/expenses/services/expenses.clj` (derive `:unit_price` when omitted so price observations get recorded)

### Next up (prioritized)

1. `src/app/template/frontend/shared/utils/entity.cljs`
   - Evaluate whether entity alias-key support can be removed once boundaries are strict.
2. `src/app/admin/frontend/specs/generic.cljs`
   - Replace config key fallbacks with one-time normalization at load time.

