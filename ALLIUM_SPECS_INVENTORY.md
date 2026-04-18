# Allium Specs Inventory

Generated from the current repository state on 2026-04-18.

## Summary

- Total `.allium` specs: **34**
- Sections:
  - `template`: **14**
  - `drafts`: **11**
  - `shared`: **5**
  - `domain`: **4**
- Status breakdown:
  - `active`: **12**
  - `candidate`: **22**
- Allium version markers:
  - v1: **6**
  - v2: **12**
  - v3: **16**

## Status meaning

- **active** — no `.candidate` suffix in the filename; treated as the current spec in this repo.
- **candidate** — exploratory or in-progress spec, marked by `.candidate.allium`.

## Domain specs

| Spec | Version | Status | Summary |
| --- | ---: | --- | --- |
| `domain/expenses/implementation.allium` | 3 | active | Expenses domain route composition, manifest contracts, and shared descriptor wiring between frontend and backend. |
| `domain/expenses/price-history-integrity.candidate.allium` | 2 | candidate | Rules governing which expense items contribute to article price history. |
| `domain/expenses/settings-hierarchy.candidate.allium` | 2 | candidate | Settings architecture with global, tenant, and per-user layers for expenses. |
| `domain/expenses/workspace-dashboard.candidate.allium` | 2 | candidate | Tenant workspace dashboard for authenticated users, including summaries, rankings, and overview metrics. |

## Draft specs

| Spec | Version | Status | Summary |
| --- | ---: | --- | --- |
| `drafts/expenses/expense-entry.candidate.allium` | 2 | candidate | Manual expense entry Smart Input flow for web and mobile. |
| `drafts/expenses/expense-list-item-expand.candidate.allium` | 2 | candidate | Inline readonly expansion of expense line items on the expense list page. |
| `drafts/expenses/list-filter-contracts.candidate.allium` | 1 | candidate | Server-backed filter contracts for the expenses list and expense-items list. |
| `drafts/expenses/payer-and-defaults.candidate.allium` | 2 | candidate | Payer types, provisioned payers, per-user defaults, and tenant default expense-category behavior. |
| `drafts/expenses/receipt-ocr-supplier-alias-resolution.candidate.allium` | 1 | candidate | OCR supplier-guess resolution into canonical suppliers and supplier aliases. |
| `drafts/expenses/receipt-ocr-unit-extraction.candidate.allium` | 1 | candidate | Unit extraction and label normalization for OCR-captured article labels. |
| `drafts/expenses/receipt-ocr.candidate.allium` | 3 | candidate | Receipt OCR lifecycle covering upload, extraction, review, and posting for BAM-based receipts. |
| `drafts/list-view-date-range-picker.candidate.allium` | 2 | candidate | Custom date-range picker behavior inside the shared list-view filter component. |
| `drafts/list-view-filter-form.candidate.allium` | 2 | candidate | Inline list-filter form behavior, option sources, and auto-apply semantics. |
| `drafts/list-view-filtering.candidate.allium` | 1 | candidate | Canonical list filtering semantics across client-side and server-side list modes. |
| `drafts/list-view-sort.candidate.allium` | 3 | candidate | Canonical list sorting behavior across client-side and server-side list modes. |

## Shared specs

| Spec | Version | Status | Summary |
| --- | ---: | --- | --- |
| `shared/form-field-options.allium` | 3 | active | Shared form-field option schema for scalar and rich option definitions. |
| `shared/infrastructure.allium` | 3 | active | Shared infrastructure contracts used across template and domain specs. |
| `shared/list-filter-pagination-reset.allium` | 3 | active | Pagination reset semantics when list filters change. |
| `shared/list-filter-query-param-forwarding.allium` | 1 | active | Shared contract for how canonical list filters become server query params. |
| `shared/view-options.allium` | 3 | active | Shared view-options configuration schema. |

## Template specs

| Spec | Version | Status | Summary |
| --- | ---: | --- | --- |
| `template/admin-invitation.candidate.allium` | 3 | candidate | Email-based onboarding flow for platform administrators. |
| `template/authentication.allium` | 3 | active | User/admin authentication, sessions, password reset, and email verification flows. |
| `template/authorization.allium` | 3 | active | Authorization gates for user routes, admin routes, generic CRUD, and expenses actions. |
| `template/backlog-remove-priority.candidate.allium` | 2 | candidate | Removal of backlog priority plus related enum and ordering cleanup. |
| `template/deploy-railway.allium` | 3 | active | Railway production deployment workflow. |
| `template/domain-architecture.allium` | 3 | active | Domain registry, shared route descriptors, and template/domain composition boundaries. |
| `template/dry-principle.allium` | 3 | active | DRY reuse boundaries for shared list views, CRUD bridges, generic CRUD, DI, and domain composition. |
| `template/email-privacy-at-rest.candidate.allium` | 1 | candidate | Protecting email identifiers at rest while preserving exact-match behavior and pseudonymous admin views. |
| `template/external-api-audit.candidate.allium` | 2 | candidate | Logging failures from outgoing HTTP calls to external APIs into audit surfaces. |
| `template/frontend-config-runtime-resolution.candidate.allium` | 3 | candidate | Operator-managed runtime frontend configuration for admin and user list settings. |
| `template/mobile-web.candidate.allium` | 2 | candidate | Mobile web experience for phone-based expense capture, review, dashboard, and reports. |
| `template/multi-tenancy.candidate.allium` | 3 | candidate | Shared-schema multi-tenancy with tenant-scoped access, provisioning, invitations, and impersonation. |
| `template/platform-boundaries.allium` | 3 | active | High-level template platform boundaries across admin API, user API, and SPA route catalogs. |
| `template/workspace-onboarding.candidate.allium` | 2 | candidate | First-run onboarding experience for users entering a workspace. |

## Notes

- All inventoried specs live under `specs/allium/`.
- Nested areas currently represented are:
  - `domain/expenses/`
  - `drafts/expenses/`
  - top-level `drafts/` list-view specs
  - top-level `shared/`
  - top-level `template/`
