# Docs Structure Alignment Plan

## Goal
Align `docs/` with `src/app/` by separating documentation into:
- `docs/admin/`
- `docs/template/`
- `docs/domain/`
- `docs/shared/`
- `docs/general/` (cross-cutting guidance used by more than one area)

This plan tracks implementation progress. Update the checklist as each step completes.

## Target Structure (high level)
```
docs/
  admin/
  template/
  domain/
  shared/
  general/
```

## Phase Checklist
- [ ] Phase 1: Define final mapping (file-by-file) and section extractions
- [x] Phase 2: Create new folder structure
- [x] Phase 3: Move docs into new folders (no content changes yet)
- [x] Phase 4: Extract mixed sections into new files
- [x] Phase 5: Update links and indexes
- [x] Phase 6: Final audit (missing files, broken links, consistency)

## Detailed Work Log
- [x] Mapping table added
- [x] Folder scaffolding created
- [x] Backend docs split (admin/template/domain)
- [x] Frontend docs split (admin/template/shared)
- [x] Architecture/operations/migrations/testing moved to general
- [x] Shared utilities docs normalized
- [x] Domain docs normalized (Expenses)
- [x] Root entry docs updated

## Notes
- Keep `:reference-only` docs under `docs/general/reference/`.
- Do not hand-edit generated migrations docs.
- Maintain `ai:` metadata headers when moving files.

## Mapping Table (current → new)

### General (cross-cutting)
- `docs/index.md` → `docs/general/index.md` (replace root with a short entry stub)
- `docs/README.md` → `docs/general/README.md` (replace root with a short entry stub)
- `docs/ai-index.yaml` → `docs/general/ai/ai-index.yaml`
- `docs/ai-quick-access.md` → `docs/general/ai/ai-quick-access.md`
- `docs/architecture/**` → `docs/general/architecture/**`
- `docs/operations/**` → `docs/general/operations/**`
- `docs/migrations/**` → `docs/general/migrations/**`
- `docs/testing/**` → `docs/general/testing/**`
- `docs/tools/**` → `docs/general/tools/**`
- `docs/validation/**` → `docs/general/validation/**`
- `docs/reference/**` → `docs/general/reference/**`
- `docs/libs/**` → `docs/general/libs/**`

### Admin
- `docs/frontend/admin.md` → `docs/admin/frontend/admin.md`
- `docs/frontend/admin-panel-single-tenant.md` → `docs/admin/frontend/admin-panel-single-tenant.md`
- `docs/frontend/admin-settings.md` → `docs/admin/frontend/admin-settings.md`
- `docs/frontend/list-view-controls-configuration.md` → `docs/admin/frontend/list-view-controls-configuration.md`
- `docs/frontend/debugging.md` → `docs/admin/frontend/debugging.md`
- `docs/backend/http-api.md` (admin endpoints) → `docs/admin/backend/http-api.md`
- `docs/backend/services.md` (admin services) → `docs/admin/backend/services.md`

### Template
- `docs/backend/single-tenant-template.md` → `docs/template/backend/single-tenant-template.md`
- `docs/backend/single-tenant-starter.md` → `docs/template/backend/single-tenant-starter.md`
- `docs/backend/template-infrastructure.md` → `docs/template/backend/template-infrastructure.md`
- `docs/backend/security-middleware.md` → `docs/template/backend/security-middleware.md`
- `docs/backend/generic-entity-crud.md` → `docs/template/backend/generic-entity-crud.md`
- `docs/frontend/app-shell.md` (template sections) → `docs/template/frontend/app-shell.md`
- `docs/frontend/template-component-integration.md` → `docs/template/frontend/template-component-integration.md`
- `docs/frontend/template-infrastructure.md` → `docs/template/frontend/template-infrastructure.md`
- `docs/frontend/crud-event-flow.md` → `docs/template/frontend/crud-event-flow.md`
- `docs/frontend/home-page-authentication-flow.md` → `docs/template/frontend/home-page-authentication-flow.md`

### Domain
- `docs/expenses/index.md` → `docs/domain/expenses/index.md`
- `docs/backend/http-api.md` (expenses endpoints) → `docs/domain/expenses/http-api.md`
- `docs/backend/services.md` (expenses services) → `docs/domain/expenses/backend-services.md`

### Shared
- `docs/shared/README.md` → `docs/shared/README.md`
- `docs/shared/architecture.md` → `docs/shared/architecture.md`
- `docs/shared/auth-utilities.md` → `docs/shared/auth-utilities.md`
- `docs/shared/date-utilities.md` → `docs/shared/date-utilities.md`
- `docs/shared/field-metadata.md` → `docs/shared/field-metadata.md`
- `docs/shared/http-utilities.md` → `docs/shared/http-utilities.md`
- `docs/shared/pagination-utilities.md` → `docs/shared/pagination-utilities.md`
- `docs/shared/pattern-utilities.md` → `docs/shared/pattern-utilities.md`
- `docs/shared/platform-specific.md` → `docs/shared/platform-specific.md`
- `docs/shared/string-utilities.md` → `docs/shared/string-utilities.md`
- `docs/shared/type-conversion.md` → `docs/shared/type-conversion.md`
- `docs/shared/template-domain-integration.md` → `docs/template/architecture/template-domain-integration.md`
- `docs/shared/template-domain-separation.md` → `docs/template/architecture/template-domain-separation.md`
- `docs/frontend/component-library.md` → `docs/shared/frontend/component-library.md`
- `docs/frontend/master-detail-form.md` → `docs/shared/frontend/master-detail-form.md`
- `docs/frontend/http-standards.md` → `docs/shared/frontend/http-standards.md`
- `docs/backend/snake-case-quick-reference.md` → `docs/shared/data/snake-case-quick-reference.md`
- `docs/backend/snake-case-refactoring-guide.md` → `docs/shared/data/snake-case-refactoring-guide.md`

### Reference-only (Hosting)
- `docs/backend/admin-billing.md` → `docs/general/reference/hosting/admin-billing.md`
- `docs/backend/financial-domain.md` → `docs/general/reference/hosting/financial-domain.md`
- `docs/backend/hosting-domain.md` → `docs/general/reference/hosting/hosting-domain.md`
- `docs/backend/integration-domain.md` → `docs/general/reference/hosting/integration-domain.md`
- `docs/frontend/integration-domain.md` → `docs/general/reference/hosting/frontend-integration-domain.md`

## Mixed-Content Splits (extractions)
- `docs/backend/http-api.md` → split into admin/template/domain endpoints.
- `docs/backend/services.md` → split admin services vs domain services.
- `docs/frontend/app-shell.md` → extract admin-only routing/auth notes to admin docs if present.
- `docs/index.md` / `docs/README.md` → general entry docs + new per-area indexes.
