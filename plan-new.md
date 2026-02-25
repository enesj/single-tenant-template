## Refactor Plan: Decompose OCR Core, Articles Service, and Supplier Resolution Tests

### Summary
Refactor the three long files into focused modules with balanced size (~150–250 lines), introduce a safer compatibility layer for rollout, and include limited low-risk fixes discovered during extraction.  
Scope covers:
- [receipt_ocr core](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/core.clj)
- [articles service](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles.clj)
- [supplier resolution tests](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_test.clj)

### Design Decisions Locked
- API direction: introduce redesigned module APIs, but keep compatibility adapters (safer rollout).
- Granularity: balanced modules (not minimal split, not ultra-fragmented).
- Behavior scope: allow only small fixes tightly coupled to refactor.
- Rollout style: safety-first with adapters retained after migration.

### Target File Decomposition

#### 1) OCR worker split
Current monolith:
- [core.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/core.clj)

Create:
- [refine.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/refine.clj)  
  Contains refine-context loading/persisting, refine eligibility checks, single-result refine, parallel refine, and refine metadata stripping.
- [provider.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/provider.clj)  
  Contains provider selection and parse/extract provider wrappers.
- [runner.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/runner.clj)  
  Contains receipt processing flow (`uploaded/parsing/parsed/extracting`), pending batch flow, by-ids flow, and shared opts construction.
- [ui_queue.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/ui_queue.clj)  
  Contains bounded executor config and UI queue submission logic.
- Keep [core.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/core.clj) as compatibility facade exporting old entrypoints.

#### 2) Articles service split
Current monolith:
- [articles.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles.clj)

Create:
- [normalization.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/normalization.clj)  
  Article and alias normalization functions/constants.
- [crud.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/crud.clj)  
  Create/get/list/update/delete/count/search and ordering allowlist.
- [related_records.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/related_records.clj)  
  All related-* query functions plus dispatcher.
- [aliases.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/aliases.clj)  
  Alias lookup/create/batch upsert/reassign logic.
- [service.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/service.clj)  
  Redesigned internal API surface aggregating module operations.
- Keep [articles.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles.clj) as compatibility facade exposing existing public names.

#### 3) Supplier resolution test split
Current monolith:
- [supplier_resolution_test.clj](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_test.clj)

Split into:
- [supplier_resolution_inference_test.clj](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_inference_test.clj)  
  Store-alias inference and merged address splitting tests.
- [supplier_resolution_brand_repair_test.clj](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_brand_repair_test.clj)  
  Brand promotion and alias repair scenarios.
- [supplier_resolution_descriptor_test.clj](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_descriptor_test.clj)  
  `resolve-supplier-and-alias` descriptor-tail and legal-suffix cases.
- Optional shared helper (only if needed to avoid duplication):  
  [supplier_resolution_test_support.clj](/Users/enes/Projects/single-tenant-template/test/app/domain/backend/expenses/workers/receipt_ocr_extraction/supplier_resolution_test_support.clj)

### Public API / Interface Changes

#### OCR API
New internal API (preferred for new code):
- `runner/run-pending!`
- `runner/run-by-ids!`
- `runner/run-receipt!`
- `ui-queue/enqueue!`

Compatibility exports preserved in:
- [core.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/core.clj)  
  Old names delegate to new functions:
  - `process-pending!` -> `runner/run-pending!`
  - `process-receipts-by-ids!` -> `runner/run-by-ids!`
  - `process-receipt!` -> `runner/run-receipt!`
  - `queue-ui-ocr!` -> `ui-queue/enqueue!`

#### Articles API
New internal API in:
- [service.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/service.clj)

Compatibility exports preserved in:
- [articles.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles.clj)  
  Existing names remain callable by routes/handlers/tests (`list-articles`, `batch-create-aliases!`, `list-related-records`, etc.), delegating to split modules.

### Small Fixes Included (and only these)
- Introduce one shared worker opts builder used by both pending and by-ids OCR flows to remove duplicated default-merging paths.
- Consolidate user setting lookups for refine/auto-post decisions into a shared helper to avoid divergence and keep fallback behavior consistent.
- No schema, migration, or endpoint contract changes.

### Implementation Sequence
1. Add OCR modules (`refine`, `provider`, `runner`, `ui_queue`) and move logic without changing behavior.
2. Replace old OCR monolith bodies with delegating compatibility functions in [core.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/workers/receipt_ocr/core.clj).
3. Add articles modules (`normalization`, `crud`, `related_records`, `aliases`, `service`) and move logic.
4. Replace old articles bodies with delegating compatibility functions in [articles.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles.clj).
5. Split supplier resolution test file into three focused namespaces and keep all original assertions.
6. Update only necessary `:require` forms for moved internal calls; keep external call sites functional through adapters.
7. Run focused validation and fix any namespace/require regressions.

### Test Cases and Scenarios

#### Required focused tests
- OCR core suite:
  - `clj -M:test -m kaocha.runner --focus app.domain.backend.expenses.workers.receipt-ocr-core-test`
- OCR extraction supplier suites (new split namespaces):
  - `clj -M:test -m kaocha.runner --focus app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-inference-test`
  - `clj -M:test -m kaocha.runner --focus app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-brand-repair-test`
  - `clj -M:test -m kaocha.runner --focus app.domain.backend.expenses.workers.receipt-ocr-extraction.supplier-resolution-descriptor-test`
- Articles integration suite:
  - `clj -M:test -m kaocha.runner --focus app.domain.expenses.services.articles-test`

#### Acceptance checks
- Old public functions in `core` and `articles` remain callable.
- No route/handler compile errors from namespace changes.
- Supplier resolution assertions remain semantically identical after test split.
- Each original long file is reduced to a thin facade (or removed/replaced by smaller modules) and no new module exceeds ~300 lines.

### Assumptions and Defaults
- Refactor stays within repository code; no external consumers are version-pinned to these exact internals.
- Compatibility facades remain in place after this refactor (no immediate hard removal).
- Behavior is preserved except the explicitly listed small fixes.
- No database migrations are required.
- Naming follows existing repo conventions (`service`, `related_records`, focused test namespace files).