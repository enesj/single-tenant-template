# Mistral OCR 3 Implementation Plan for POS Terminal Receipts

**Date**: 2025-12-25
**Model**: `mistral-ocr-2512` (OCR 3 v25.12)
**Purpose**: Extract structured data from POS terminal receipts for the expenses domain

---

## Executive Summary

This plan integrates Mistral OCR 3 into the **existing Expenses receipts workflow** in this repository. Any vendor benchmark/pricing numbers in the “Model Overview” section are informational only; we will validate extraction quality and true cost on a small internal receipt set before rollout.

### Key Deliverables
- Automated processor that moves receipts through `uploaded → parsing → parsed → extracting → extracted|review_required` and writes `raw_parse_json`, `parsed_markdown`, `raw_extract_json`, and best-effort `*_guess` fields.
- Admin review remains the source of truth: extracted data is only used to prefill review; approval creates the real expense via `approve-and-post!`.
- Operational guardrails: idempotency, retries/backoff, concurrency limits, and basic cost caps.

### Repo Reality Check (already implemented here)
- DB schema: `resources/db/domain/models.edn` defines `:receipts` (status machine + storage key + parse/extract payload fields).
- Admin API: `/admin/api/expenses/receipts...` in `src/app/domain/backend/expenses/routes/receipts.clj` + `src/app/domain/backend/expenses/services/receipts.clj`.
- UI: receipts list/detail pages already render `parsed_markdown`, `raw_parse_json`, `raw_extract_json` (see `src/app/domain/frontend/expenses/pages/admin/receipts.cljs`, `src/app/domain/frontend/expenses/pages/admin/receipt_detail.cljs`, `src/app/domain/frontend/expenses/components/receipt_viewer.cljs`).
- Gap today: a worker/job that reads `storage_key`, calls the OCR provider, and persists results + status transitions.

### Current Implementation Status (as of 2025-12-25)
- ✅ Upload UI + API: `http://localhost:8085/expenses/upload` posts a multipart upload to `POST /api/v1/expenses/upload` and creates a `receipts` row with status `uploaded`.
- ✅ Local storage: uploaded bytes are stored under `upload/stripes/` and `storage_key` is saved as the generated filename.
- ✅ Worker: `src/app/domain/backend/expenses/workers/receipt_ocr.clj` processes candidates via `receipts/list-pending-for-processing` and transitions statuses (`uploaded → parsing → parsed → extracting → extracted|review_required`).
- ✅ Provider client: `src/app/domain/backend/expenses/integrations/mistral_ocr.clj` calls Mistral OCR with retries and supports structured extraction.
- ✅ Status/enum correctness: status comparisons/updates use explicit casts to `:receipt_status` (avoids Postgres enum vs varchar operator errors).
- 🟡 E2E still pending: set `MISTRAL_API_KEY` (and optionally `MISTRAL_OCR_ENABLED=true`) and run the worker against real uploaded receipts; confirm fields populate and admin approve creates an expense.

---

## Table of Contents

1. [Model Overview](#1-model-overview)
2. [API Specification](#2-api-specification)
3. [Receipt Data Schema Design](#3-receipt-data-schema-design)
4. [Implementation Phases](#4-implementation-phases)
5. [Clojure Backend Integration](#5-clojure-backend-integration)
6. [Frontend Integration](#6-frontend-integration)
7. [Testing & Validation](#7-testing--validation)
8. [Cost Analysis](#8-cost-analysis)
9. [Security & Privacy](#9-security--privacy)
10. [Monitoring & Observability](#10-monitoring--observability)

---

## 1. Model Overview

### Model Information

| Property | Value |
|----------|-------|
| **Model Name** | `mistral-ocr-2512` |
| **API Version** | v25.12 (December 2025) |
| **Release Date** | December 15, 2025 |
| **Type** | Document OCR / Vision Language Model |
| **Modalities** | Text extraction, Image understanding, Table reconstruction |
| **Pricing** | $2 / 1,000 pages (standard), $1 / 1,000 pages (batch API) |

### Performance Benchmarks

| Document Type | Accuracy |
|---------------|----------|
| Scanned Documents | 98.96% |
| Table Recognition | 96.12% |
| Forms & Receipts | ~95% (estimated from overall) |
| Multilingual | 89.55% |
| Mathematics | 94.29% |

### What Makes OCR 3 Different

1. **Unified Understanding**: Processes documents hierarchically, not as flat images
2. **Table Preservation**: Outputs HTML with `colspan`/`rowspan` for complex layouts
3. **Markdown + Images**: Returns interleaved text and embedded images with bounding boxes
4. **Backward Compatible**: Drop-in replacement for Mistral OCR 2

---

## 2. API Specification

### Endpoint

```
POST https://api.mistral.ai/v1/ocr
```

### Authentication

```bash
Authorization: Bearer YOUR_MISTRAL_API_KEY
```

### Request Format

```bash
curl -X POST "https://api.mistral.ai/v1/ocr" \
  -H "Authorization: Bearer $MISTRAL_API_KEY" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/receipt.jpg" \
  -F "document_type=receipt"
```

### Response Format

The OCR API returns a JSON response with the following structure:

```json
{
  "pages": [
    {
      "index": 1,
      "markdown": "# STORE NAME\n\n123 Main Street\nTel: 555-1234\n\n## Items\n| Item | Qty | Price |\n|------|-----|-------|\n| Coffee | 2 | $6.00 |\n| Bagel | 1 | $3.50 |\n\n## Totals\n| Subtotal | $9.50 |\n| Tax | $0.76 |\n| **Total** | **$10.26** |\n\nThank you for your purchase!",
      "images": [
        {
          "id": "img-0.jpeg",
          "top_left_x": 292,
          "top_left_y": 217,
          "bottom_right_x": 1405,
          "bottom_right_y": 649,
          "image_base64": "..."
        }
      ],
      "dimensions": {
        "dpi": 200,
        "height": 2200,
        "width": 1700
      }
    }
  ],
  "model": "mistral-ocr-2512",
  "usage_info": {
    "pages_processed": 1,
    "doc_size_bytes": 450123
  }
}
```

### Structured Output with JSON Schema

For guaranteed structured data, use the `response_format` parameter:

```bash
curl -X POST "https://api.mistral.ai/v1/ocr" \
  -H "Authorization: Bearer $MISTRAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "file": "base64_encoded_receipt",
    "response_format": {
      "type": "json_schema",
      "json_schema": {
        "name": "receipt_extraction",
        "schema": RECEIPT_JSON_SCHEMA
      }
    }
  }'
```

### Internal Integration Notes (this repo)

- The processor should persist results via `receipts/store-extraction-results!` and status transitions via `receipts/update-status!` / `receipts/mark-failed!`.
- The admin endpoints needed for debugging/ops already exist (see `docs/domain/expenses/http-api.md`):
  - `GET /admin/api/expenses/receipts`, `GET /admin/api/expenses/receipts/pending`
  - `POST /admin/api/expenses/receipts/:id/extraction`, `POST /admin/api/expenses/receipts/:id/retry`, `POST /admin/api/expenses/receipts/:id/approve`

---

## 3. Receipt Data Schema Design

### Existing Receipt Storage (this repo)

This repo already has a `receipts` table designed for OCR + approval workflow (see `resources/db/domain/models.edn` `:receipts`). We will **not** create new receipt tables.

Key columns we will populate/update:
- `storage_key`, `file_hash`, `original_filename`, `content_type`, `file_size`
- `status` (`uploaded|parsing|parsed|extracting|extracted|review_required|approved|posted|failed`)
- Parse: `raw_parse_json`, `parsed_markdown`
- Extract: `raw_extract_json`, plus convenience guesses: `supplier_guess`, `total_amount_guess`, `currency_guess`, `purchased_at_guess`
- Errors: `error_message`, `error_details`, `retry_count`

Extracted line items should live inside `raw_extract_json` until an admin approves; on approval, canonical `expense_items` are created via `approve-and-post!`.

### JSON Schema for `raw_extract_json` (structured output)

We store the full structured output payload in `receipts.raw_extract_json` (JSONB), and copy a few normalized fields into the `*_guess` columns for list views.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ReceiptExtractionV1",
  "type": "object",
  "properties": {
    "merchant": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "address": {"type": ["string", "null"]},
        "tax_id": {"type": ["string", "null"]}
      },
      "required": ["name"]
    },
    "purchased_at": {"type": ["string", "null"], "description": "ISO-8601 timestamp if available"},
    "currency": {"type": ["string", "null"], "description": "ISO 4217, e.g. USD/EUR/BAM"},
    "totals": {
      "type": "object",
      "properties": {
        "subtotal": {"type": ["number", "null"]},
        "tax": {"type": ["number", "null"]},
        "total": {"type": "number"}
      },
      "required": ["total"]
    },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "raw_label": {"type": "string"},
          "qty": {"type": ["number", "null"]},
          "unit_price": {"type": ["number", "null"]},
          "line_total": {"type": "number"}
        },
        "required": ["raw_label", "line_total"]
      }
    }
  },
  "required": ["merchant", "totals", "items"]
}
```

Notes:
- Keep this schema intentionally permissive; thermal POS receipts are noisy.
- Validate/coerce in code with Malli (this repo already uses Malli for validation/coercion).

---

## 4. Implementation Phases

### Phase 0: Confirm provider contract + acceptance criteria (🟡 pending)
- Verify the exact Mistral OCR request/response contract we will use (multipart vs base64 JSON, `response_format` support, page limits).
- Build an internal evaluation set (10–30 receipts) and define success criteria:
  - Required fields available: merchant name, total, currency (or default), at least one item line.
  - Total consistency check: sum(items) ≈ total (tolerance 0.01–0.05 depending on currency rounding).
  - “Review required” threshold for missing/low-confidence fields.

### Phase 1: Provider client (Clojure) (✅ implemented)
- Implemented in `src/app/domain/backend/expenses/integrations/mistral_ocr.clj`.
- Supports:
  - OCR parse → markdown aggregation.
  - Structured extraction with JSON schema.
  - Timeouts + retry/backoff and consistent error mapping.
- Configuration:
  - Env overrides: `MISTRAL_API_KEY`, `MISTRAL_OCR_BASE_URL`, `MISTRAL_OCR_MODEL`, `MISTRAL_OCR_ENABLED`.

### Phase 2: Receipt processing worker (parse + extract) (✅ implemented)
- Implemented in `src/app/domain/backend/expenses/workers/receipt_ocr.clj`.
- Pulls candidates via `receipts/list-pending-for-processing` (DB query; it does **not** scan folders).
- Claims work with atomic transitions (`uploaded → parsing`, `parsed → extracting`) to avoid double-processing.
- Loads bytes by joining `storage_key` with `--storage-base-dir` (local FS).
- Writes:
  - Parse: `raw_parse_json` + `parsed_markdown` and sets status `parsed`.
  - Extract: `raw_extract_json` + `*_guess` fields and sets status `extracted` or `review_required`.
- Runner:
  - `bb receipt-ocr-worker dev --max-receipts 25`
  - Default `--storage-base-dir` is `upload/stripes`.

### Phase 3: Admin + user workflow polish (🟡 partial)
- ✅ User upload page exists and now actually submits multipart uploads.
- ✅ Admin receipts list/detail pages already render parse/extract payloads.
- Optional polish still open:
  - **Next step: inline Supplier creation from the Approve & Post expense form**
    - Problem: POS receipts frequently introduce new merchants not yet in the shared `suppliers` catalog.
    - UX: in the expense approval/create form, add a **“New Supplier”** action next to the Supplier select.
    - Behavior: create supplier via `POST /admin/api/expenses/suppliers`, refresh supplier options, auto-select the newly created supplier, then return to completing the expense.
    - Implementation constraint: avoid nesting the template `form` component inside the expense form (it hardcodes `btn-save`/`btn-cancel` IDs). Use a small dedicated modal/drawer with uniquely derived IDs (prefix with the expense form-id).
  - Add explicit “Retry extraction” / “Process now” buttons with stable `:id` attributes.
  - Improve user-facing “Recent Uploads” and status display/links.

### Phase 4: Rollout + guardrails (🟡 pending)
- Feature-flag processing (env var) so we can ship UI/DB wiring first and enable processing later.
- Add basic rate limiting/cost caps (max pages per receipt, max file size, max receipts/hour).

---

## 5. Clojure Backend Integration

### Existing integration points
- Persistence + status machine: `src/app/domain/backend/expenses/services/receipts.clj`
- Admin API routes: `src/app/domain/backend/expenses/routes/receipts.clj`
- Public API surface reference: `docs/domain/expenses/http-api.md` (Receipts section)

### New backend code (proposed)
- `src/app/domain/backend/expenses/integrations/mistral_ocr.clj`
  - `ocr-parse!` → calls Mistral OCR, returns raw response + extracted markdown.
  - `ocr-extract!` → calls Mistral OCR with `response_format` JSON schema (or reuse parse call if supported).
- `src/app/domain/backend/expenses/workers/receipt_ocr.clj`
  - `process-receipt!` (one receipt) + `process-pending!` (batch loop) with safe status claiming.
- `scripts/bb/expenses/receipt_ocr_worker.clj` (invoked via `bb receipt-ocr-worker ...`)
  - Runs `process-pending!` as a one-shot batch (default) or in a loop (`--loop`).

### Status + data writes
- Use existing service functions:
  - `receipts/update-status!` for transitions.
  - `receipts/store-extraction-results!` to persist `raw_parse_json`/`raw_extract_json`/`parsed_markdown` + guess fields.
  - `receipts/mark-failed!` on hard failures.

---

## 6. Frontend Integration

### Existing UI surface
- Admin receipts:
  - List page: `src/app/domain/frontend/expenses/pages/admin/receipts.cljs`
  - Detail page: `src/app/domain/frontend/expenses/pages/admin/receipt_detail.cljs`
  - Viewer: `src/app/domain/frontend/expenses/components/receipt_viewer.cljs`
- Admin events/subs already exist for receipts (`src/app/domain/frontend/expenses/events/receipts.cljs`, `src/app/domain/frontend/expenses/subs/receipts.cljs`).
- User upload wizard exists at `src/app/domain/frontend/expenses/pages/user/expense_upload.cljs`.

### Next UX step: inline Supplier creation (admin expense form)

When approving extracted receipts, the expense form requires selecting a `supplier_id`. POS receipts often represent new merchants, so the admin flow should support creating a supplier without leaving the form:

- Add a custom Supplier field component in the expense form that renders:
  - the Supplier select
  - a **“New Supplier”** button
- “New Supplier” opens a small modal/drawer with `display_name` (required) + optional address/tax_id.
- On success: refresh suppliers (`...suppliers/load-list`) and set the expense form’s `supplier_id` to the new supplier.
- All interactive elements must have unique `:id`s (derive from the expense form’s `form-id`).

### Changes needed / current wiring
- Upload UI route: `http://localhost:8085/expenses/upload`.
- Upload API:
  - User: `POST /api/v1/expenses/upload` (multipart `file`)
  - Admin: `POST /admin/api/expenses/upload` (multipart `file`)
- Requirement: request must be `multipart/form-data` and include the field named `file`.

---

## 7. Testing & Validation

### Backend
- Add focused tests around:
  - Status claiming/idempotency (two workers can’t process the same receipt concurrently).
  - Provider error handling (timeout, 4xx/5xx mapping → `failed` with `error_details`).
  - Coercion/validation of `raw_extract_json` (Malli) and mapping to `*_guess` fields.
- Tests must not call Mistral; stub `clj-http` via `with-redefs` and use small fixture responses.

### Frontend
- Ensure receipts pages render all new fields without crashes; add/extend CLJS tests if there is a natural place.

### Manual E2E checklist (expanded)

1) Upload receipt (creates a `receipts` row)
- UI: `http://localhost:8085/expenses/upload`
- Backend endpoint: `POST /api/v1/expenses/upload`
- Expected DB changes:
  - new row in `receipts` with `status = uploaded`
  - `storage_key` set to the generated filename
  - file stored at `upload/stripes/<storage_key>`

2) Worker finds candidates (DB, not filesystem)
- Candidate query: `receipts/list-pending-for-processing` selects rows where `status ∈ {uploaded, parsing, parsed, extracting}`.
- Run one-shot batch:
  - `bb receipt-ocr-worker dev --max-receipts 25`

3) Status transitions + extracted fields
- Expected transitions:
  - `uploaded → parsing → parsed → extracting → extracted|review_required`
- Expected fields to be populated by the worker:
  - `raw_parse_json`, `parsed_markdown`
  - `raw_extract_json`
  - guess fields: `supplier_guess`, `total_amount_guess`, `currency_guess`, `purchased_at_guess`

4) Admin review
- Admin receipts list/detail should show:
  - OCR markdown + raw payloads
  - guess fields (used to prefill review)

4b) (If needed) Create Supplier inline during approval
- In the Approve & Post expense form:
  - Click “New Supplier”, create the supplier, confirm it becomes selected in the Supplier dropdown.
  - Continue completing the expense (payer selection, etc.).

5) Approve → expense created and receipt status becomes posted
- Approve action calls `receipts/approve-and-post!`:
  - creates the real `expense` + items
  - updates receipt `status` to `posted`

---

## 8. Cost Analysis

Pricing numbers below should be verified against current Mistral docs/contract before relying on them; implement a daily cost cap once we confirm the billing proxy field(s) (e.g., `usage_info.pages_processed`).

### Pricing Structure

| Usage | Cost |
|-------|------|
| Standard API | $2 / 1,000 pages |
| Batch API (50% discount) | $1 / 1,000 pages |

### Estimated Monthly Costs

Based on typical SaaS usage patterns:

| Plan | Users/Month | Avg Receipts/User | Total Pages | Monthly Cost |
|------|------------|-------------------|-------------|--------------|
| Startup | 50 | 10 | 500 | $1.00 |
| Growth | 500 | 15 | 7,500 | $15.00 |
| Scale | 5,000 | 20 | 100,000 | $200.00 |
| Enterprise | 50,000 | 25 | 1,250,000 | $2,500.00 |

### Cost Optimization Strategies

1. **Batch Processing**: Use batch API for bulk uploads (50% savings)
2. **Caching**: Cache OCR results for duplicate receipts (hash-based)
3. **Preprocessing**: Compress/optimize images before API calls
4. **Tiered Processing**: Use older OCR model for non-critical receipts

---

## 9. Security & Privacy

### Data Protection (practical checklist)
- Store API keys only in `config/.secrets.edn` or environment variables (never commit).
- Do not log receipt bytes, full OCR markdown, or full raw provider payloads at INFO level; log receipt-id, status, duration, and page count.
- Ensure deletion flows cover `receipts` rows and any stored objects referenced by `storage_key`.
- Confirm provider data usage policy (training/retention) before enabling in production.

### Privacy Considerations
- Define retention for `raw_parse_json`, `raw_extract_json`, and `parsed_markdown` if not needed long-term (e.g., 30–90 days).
- Ensure “right to deletion” covers DB rows and stored objects (`storage_key`).
- If data residency matters, confirm provider region support before enabling production processing.

---

## 10. Monitoring & Observability

### Metrics to Track

```clojure
;; High-level metric names (implementation may use logs, DB counters, or a metrics lib)
(def metrics
  {:ocr/latency-ms         "OCR processing duration"
   :ocr/success-rate       "Percentage of successful OCR runs"
   :ocr/review-required    "Receipts landing in review_required"
   :ocr/failures           "Receipts marked failed"
   :ocr/pages-processed    "Pages processed (billing proxy)"})
```

### Logging

Prefer `taoensso.timbre` (already used by the backend). Log with safe, structured context:
- `receipt-id`, `status-from`, `status-to`, `duration-ms`, `pages`, `provider`/`model`, `error-type`.

### Alerting Rules

| Alert | Condition | Action |
|-------|-----------|--------|
| High failure rate | >5% OCR failures in 1h | Investigate provider/API health |
| Cost spike | >200% expected daily cost | Throttle/disable processing |
| Slow processing | p95 > 10s | Inspect timeouts/retries |

---

## References & Resources

### Official Documentation
- [Mistral OCR 3 Announcement](https://mistral.ai/news/mistral-ocr-3)
- [OCR API Endpoints](https://docs.mistral.ai/api/endpoint/ocr)
- [Structured Outputs Guide](https://docs.mistral.ai/capabilities/structured_output)
- [Structured OCR Cookbook](https://github.com/mistralai/cookbook/blob/main/mistral/ocr/structured_ocr.ipynb)

### Community Resources
- [Mistral OCR API Guide (Bind AI)](https://blog.getbind.co/2025/03/08/mistral-ocr-api-ai-powered-document-parsing/)
- [2025 Mistral OCR Review](https://www.cursor-ide.com/blog/mistral-ocr-review-guide-2025)

### API Client Libraries
- JavaScript/TypeScript: `npm install @mistralai/mistralai`
- Clojure: use `clj-http.client` (already in `deps.edn`) with multipart upload

---

`★ Insight ─────────────────────────────────────`
1. **Unified Document Understanding**: Mistral OCR 3's key advantage is processing documents hierarchically rather than as flat images, which preserves table structures and spatial relationships—critical for receipt line items.

2. **JSON Schema Enforcement**: Using `response_format` with `json_schema` type can provide structured output and reduce fragile parsing.

3. **Batch API Economics**: If/when batch processing is supported for our workflow, it may reduce cost for high volume.
`─────────────────────────────────────────────────`

---

*This plan is a living document. Update as implementation progresses and new learnings emerge.*
