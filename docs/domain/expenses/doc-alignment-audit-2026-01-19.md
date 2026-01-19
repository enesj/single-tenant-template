# Expenses Domain Doc/Code Alignment Audit (2026-01-19)

## Scope

Audited documentation and implementation for the Expenses domain.

**Docs:**
- `docs/domain/expenses/index.md`
- `docs/domain/expenses/http-api.md`
- `docs/domain/expenses/backend-services.md`
- `docs/shared/frontend/master-detail-form.md`

**Code/config:**
- `src/app/domain/backend/**`
- `src/app/domain/frontend/expenses/**`
- `src/app/template/frontend/components/form/master_detail.cljs`
- `resources/db/domain/models.edn`

**Evidence bundles:**
- `target/audit-bundles/20260119-131302-domain-expenses-doc-alignment-2026-01-19-v2.txt`
- `target/audit-bundles/20260119-134024-domain-expenses-doc-alignment-2026-01-19-v3.txt`
- `target/audit-bundles/20260119-134306-domain-expenses-doc-alignment-2026-01-19-v4-xhrio.txt`
- `target/audit-bundles/20260119-134508-domain-expenses-doc-alignment-2026-01-19-v5-ui-config.txt`
- `target/audit-bundles/20260119-134549-domain-expenses-doc-alignment-2026-01-19-v6-master-detail.txt`
- `target/audit-bundles/20260119-135616-domain-expenses-doc-alignment-2026-01-19-v7-authz.txt`
- `target/audit-bundles/20260119-135624-domain-expenses-doc-alignment-2026-01-19-v8-user-purge-roles.txt`
- `target/audit-bundles/20260119-135632-domain-expenses-doc-alignment-2026-01-19-v9-admin-supplier-purge-roles.txt`
- `target/audit-bundles/20260119-135545-domain-expenses-doc-alignment-2026-01-19-v10-receipt-status-enum.txt`
- `target/audit-bundles/20260119-140411-domain-expenses-doc-alignment-2026-01-19-v11-user-purge-doc-fix.txt`

## Findings

| Claim | Docs evidence | Code evidence | Status |
|---|---|---|---|
| Admin expenses API is mounted under `/admin/api/expenses`. | `docs/domain/expenses/index.md:152` | `src/app/domain/backend/expenses/routes/core.clj:16` | Aligned |
| User-facing expenses API is mounted under `/api/v1/expenses`. | `docs/domain/expenses/index.md:153` | `src/app/domain/backend/expenses/routes/user_api.clj:4` | Aligned |
| User pages load from `/api/v1/expenses/*` and switch to `/admin/api/expenses/*` in admin context via xhrio. | `docs/domain/expenses/index.md:68` `docs/domain/expenses/index.md:70` | `src/app/domain/frontend/expenses/events/user_expenses/xhrio.cljs:2` `src/app/domain/frontend/expenses/events/user_expenses/xhrio.cljs:26-28` | Aligned |
| SPA routes include `/expenses/new`. | `docs/domain/expenses/index.md:53` `docs/domain/expenses/index.md:119` | `src/app/domain/backend/registry.clj:36` `src/app/domain/frontend/expenses/routes/user.cljs:65` | Aligned |
| SPA routes include `/receipts/:receipt-id`. | `docs/domain/expenses/index.md:52` `docs/domain/expenses/index.md:122` | `src/app/domain/backend/registry.clj:35` `src/app/domain/frontend/expenses/routes/user.cljs:58` | Aligned |
| User-facing forms use domain-owned UI config under `src/app/domain/frontend/expenses/config/`, editable via `/admin/user-settings`. | `docs/domain/expenses/index.md:82` `docs/domain/expenses/index.md:351` | `src/app/domain/backend/registry.clj:21-22` `src/app/domain/frontend/expenses/config/preload.cljs:19,23` | Aligned |
| Master/detail form wrapper is used for expense edit flows, and admin detail response uses `:detail-response-key`. | `docs/domain/expenses/index.md:94-95` `docs/domain/expenses/index.md:109` | `src/app/template/frontend/components/form/master_detail.cljs:68` `src/app/domain/frontend/expenses/components/expense_form.cljs:4` `src/app/domain/frontend/expenses/events/entity_configs.cljs:61` | Aligned |
| Role/capability gating: read = viewer+, write = member+, danger zone = admin/owner. | `docs/domain/expenses/index.md:144-146` | `src/app/domain/frontend/expenses/authz.cljs:5` `src/app/domain/frontend/expenses/authz.cljs:26` `src/app/domain/frontend/expenses/authz.cljs:33` | Aligned |
| Supplier deletion is archiving via `archived_at`. | `docs/domain/expenses/index.md:165` `docs/domain/expenses/http-api.md:21` | `src/app/domain/backend/expenses/services/suppliers.clj:155` | Aligned |
| Admin supplier purge endpoints are admin/owner-only. | `docs/domain/expenses/http-api.md:22-23` | `src/app/domain/backend/expenses/routes/suppliers.clj:8-16` | Aligned |
| User API supplier purge endpoints require admin/owner. | `docs/domain/expenses/http-api.md:125-127` | `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj:239,266` `src/app/domain/backend/expenses/routes/user_api.clj:49-53` | Aligned |
| Receipt OCR endpoints are available for both admin and user APIs. | `docs/domain/expenses/http-api.md:49` `docs/domain/expenses/http-api.md:146` | `src/app/domain/backend/expenses/routes/receipts.clj:350` `src/app/domain/backend/expenses/routes/user_api.clj:91,98` | Aligned |
| OCR requires `MISTRAL_API_KEY` and can be disabled with `MISTRAL_OCR_ENABLED=false`. | `docs/domain/expenses/index.md:326` | `src/app/domain/backend/expenses/integrations/mistral_ocr/config.clj:56` `src/app/domain/backend/expenses/integrations/mistral_ocr/http.clj:60` | Aligned |
| Receipt status flow includes uploaded → parsing → parsed → extracting → extracted/review_required → approved → posted/failed. | `docs/domain/expenses/index.md:176` | `resources/db/domain/models.edn:73-75` `src/app/domain/backend/expenses/services/receipts/queries.clj:194` | Aligned |

## Mismatches & recommended edits

None.

## Open questions / uncertainties

None.
