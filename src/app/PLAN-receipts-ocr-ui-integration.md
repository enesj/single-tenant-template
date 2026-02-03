---
title: Receipt OCR UI Integration (User App)
created_at: 2025-12-26
status: implemented (user app)
scope:
  - /receipts
---

# NOTE (2026-01-13)

The Expenses domain is no longer exposed in the admin panel. `/admin/receipts` and `src/app/domain/frontend/expenses/pages/admin/*` were removed. Treat any “Admin” sections in this document as historical context; focus on the user app (`/receipts`) flow.

# Goal

Expose “run receipt OCR (parse/extract)” from the UI:

- **Single receipt**: an action in the per-row **dropdown actions** menu.

This should reuse the existing worker logic used by `scripts/bb/expenses/receipt_ocr_worker.clj`, but **must not** shell out to `bb` from the web app.

# Current State (What Exists)

- OCR logic lives in `app.domain.backend.expenses.workers.receipt-ocr.core`:
  - `process-receipt!` (per receipt)
  - `process-pending!` (N pending receipts)
- Admin receipts page (`/admin/receipts`) is `generic-admin-entity-page :receipts` (config-driven list-view).
- User receipts page (`/receipts`) is `app.domain.frontend.expenses.pages.user.receipts-list/receipts-list-page` and already renders a per-row dropdown via `dropdown/action-dropdown`.
- Admin already has `::receipts-events/retry-extraction` which resets status to `"uploaded"` but does **not** actually execute OCR.

# UX Spec

## 1) Per-row dropdown action

Add an action item:

- Label: `Parse (OCR)`
- Tooltip/confirmation: warn that OCR may take time; status will update asynchronously.
- Behavior: **always force-reset** the receipt to `uploaded` first (even if `extracted` / `review_required`), then trigger OCR.

Visibility rules (initial proposal):

- Show when receipt status is one of: `uploaded`, `failed`, `review_required`, `extracted`, `parsing`, `parsed`, `extracting`
- Disable when receipt is `posted` (already turned into an expense) unless explicitly allowed.

IDs (required for browser testing):

- Per-row dropdown trigger already follows `actions-btn-<receipt-id>` via `dropdown/action-dropdown`.
- Add menu item id: `parse-ocr` so it becomes `parse-ocr-<receipt-id>`.

# Backend Plan

## A) Factor a “process by ids” entrypoint

Add a new function in `app.domain.backend.expenses.workers.receipt-ocr.core` (or a small adjacent namespace) that reuses the existing worker stages:

- `process-receipts!` (name TBD)
  - inputs: `db`, `app-config`, `{:receipt-ids [...], :lease-seconds ..., :storage-base-dir ..., :max-file-size-bytes ..., :default-currency ...}`
  - fetch receipts by id (preserve request order)
  - for each receipt:
    - **reset** to `uploaded` (see next item)
    - call `process-receipt!`
  - return summary similar to `process-pending!`

Notes:

- Do **not** remove `process-pending!`. The bb script continues to use it.
- Reuse `mistral-ocr/build-config` and honor `MISTRAL_OCR_ENABLED` / `MISTRAL_API_KEY`.
- Keep lease/claim semantics via `receipts/claim-for-extracting!` / `claim-for-parsing!` so concurrent runs are safe.

## B) Re-run/reset behavior (chosen)

Always **force re-run**: UI action sets status back to `uploaded` before processing, even when current status is `extracted` / `review_required`.

Add a dedicated service function (e.g. `receipts/reset-for-ocr!`) that:

- sets status to `uploaded`
- clears error fields and (optionally) OCR fields (`raw_parse_json`, `raw_extract_json`, `parsed_markdown`, guesses)
- increments `retry_count`

## C) HTTP endpoints

Expose one endpoint for **admin** and one for **user**:

Admin (auth: admin):

- `POST /admin/api/expenses/receipts/:id/ocr`

User (auth: user; must enforce visibility/ownership):

- `POST /api/v1/expenses/receipts/:id/ocr`

Response shape:

- Admin: `{:success true :data {:queued true :receipt_ids [...]}}`
- User: `{:data {:queued true :receipt_ids [...]}}`

Execution mode:

- Run OCR **async** (recommended): enqueue via a `future` and return `202`.
- Log start/end and errors via timbre, with `:receipt-ids` and user/admin context.

# Frontend Plan

## Admin: `/admin/receipts`

### 1) Add per-row dropdown action

Change `src/app/domain/frontend/expenses/pages/admin/receipts.cljs` to pass page overrides into `generic-admin-entity-page`:

- `:list-overrides {:render-actions (fn [receipt] ...)}` where the renderer uses `dropdown/action-dropdown`
  - group: “OCR”
  - item: “Parse (OCR)” dispatching a new event `::receipts-events/ocr-receipt` (name TBD)

### 3) Add re-frame events

Extend `src/app/domain/frontend/expenses/events/receipts.cljs`:

- `::ocr-receipt` → `POST /admin/api/expenses/receipts/:id/ocr`
- Maintain `:action-loading?` state (reuse existing `:action-loading?` key)
- On success: refresh list (dispatch `::load-list` with current params or `:admin/refresh-entity` if appropriate)

## User: `/receipts`

### 1) Add per-row dropdown action

Update `receipt-actions` in `src/app/domain/frontend/expenses/pages/user/receipts_list.cljs`:

- Add a second group `OCR` with `Parse (OCR)` item.
- Dispatch `:user-expenses/ocr-receipt` with `receipt-id`.

### 3) Add re-frame events

Add events to `src/app/domain/frontend/expenses/events/user_expenses/receipts.cljs`:

- `:user-expenses/ocr-receipt` → `POST /api/v1/expenses/receipts/:id/ocr` (with fallback to admin endpoint if user has admin role, matching existing endpoint patterns)
- On success: refresh list + refresh any open receipt detail modal

# Validation / Testing Plan

- Backend: add focused tests for the new endpoints:
  - admin auth required
  - user auth required + visibility enforced
  - disabled OCR returns 409/400 with clear message
  - success returns 202 and does not block
  - (if implemented) reset-for-ocr clears fields as expected
- Frontend (CLJS): add a focused test for:
  - admin receipts row actions include parse item
  - user receipts row actions include parse item
  - all new buttons/items have stable `:id`s

# Rollout Notes

- If OCR is not enabled (`MISTRAL_OCR_ENABLED=false` or missing key), UI should show the action disabled with a tooltip.
- Keep worker command `bb receipt-ocr-worker` as an ops option; UI-triggered OCR should be additive, not a replacement.
