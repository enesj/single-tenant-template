<!-- ai: {:namespaces [app.domain.backend.expenses.routes.*] :tags [:expenses :domain :http] :kind :reference} -->

# Expenses HTTP API

This document covers **expenses domain** endpoints for both admin and user contexts.

- Admin endpoints are mounted under `/admin/api/expenses`.
- User endpoints are mounted under `/api/v1/expenses`.

Shared HTTP shapes and auth expectations are described in [Template HTTP API](../../template/backend/http-api.md).

## Admin API (mounted at `/admin/api/expenses`)

### Suppliers
- `GET /admin/api/expenses/suppliers` – list (search, pagination, order-by). Query: `include_archived=true` to include archived suppliers.
- `POST /admin/api/expenses/suppliers` – create; requires `display_name`.
- `GET /admin/api/expenses/suppliers/count` – total (optional `search`, `include_archived`).
- `GET /admin/api/expenses/suppliers/search?q=...` – autocomplete (optional `include_archived`).
- `GET /admin/api/expenses/suppliers/:id` – fetch.
- `PUT /admin/api/expenses/suppliers/:id` – update.
- `DELETE /admin/api/expenses/suppliers/:id` – **archive** supplier (idempotent; soft delete via `archived_at`).
- `GET /admin/api/expenses/suppliers/:id/purge-preview` – preview purge impact (**admin/owner only**).
- `POST /admin/api/expenses/suppliers/:id/purge` – permanently delete supplier (**admin/owner only**; requires supplier archived and no active expenses).

### Payers
- `GET /admin/api/expenses/payers` – list (optional `type`).
- `POST /admin/api/expenses/payers` – create; requires `type`, `label`.
- `GET /admin/api/expenses/payers/count` – total (optional `type`).
- `GET /admin/api/expenses/payers/suggest` – suggest from `method`/`card_last4`.
- `GET /admin/api/expenses/payers/default/:type` – fetch default for type.
- `POST /admin/api/expenses/payers/:id/default` – set default for payer’s type.
- `GET /admin/api/expenses/payers/:id` – fetch; `PUT` update; `DELETE` remove.
  - Delete may return `409` when the record is referenced (foreign key violation).

### Receipts
- `GET /admin/api/expenses/receipts` – list; filters `status`, `limit/offset`, `order-dir`.
- `GET /admin/api/expenses/receipts/pending` – pending for processing.
- `POST /admin/api/expenses/upload` – multipart upload (`file`); stores under `upload/stripes/` and creates a receipt (status `uploaded`). Optional `payer_id` (UUID) overrides the user’s default payer for that upload.
- `POST /admin/api/expenses/receipts` – upload (programmatic); requires `storage_key` and `file_hash` or `bytes`.
- `GET /admin/api/expenses/receipts/:id` – fetch one.
- `GET /admin/api/expenses/receipts/:id/download` – download/inline view the original file (`?download=true` forces attachment).
- `DELETE /admin/api/expenses/receipts/:id` – delete receipt.
- `POST /admin/api/expenses/receipts/:id/status` – set status.
- `POST /admin/api/expenses/receipts/:id/retry` – reset to uploaded + bump retry.
- `POST /admin/api/expenses/receipts/:id/fail` – mark failed with message/details.
- `POST /admin/api/expenses/receipts/:id/extraction` – store extraction payloads/guesses.
- `POST /admin/api/expenses/receipts/:id/review` – save user-reviewed fields/items without approving (does not create an expense).
- `POST /admin/api/expenses/receipts/:id/approve` – approve + create expense + mark posted.
- `POST /admin/api/expenses/receipts/ocr` – trigger async OCR for a batch of receipt IDs (returns `202` when queued; requires `MISTRAL_API_KEY`).
- `POST /admin/api/expenses/receipts/:id/ocr` – trigger async OCR for a single receipt (requires `MISTRAL_API_KEY`).

### Expenses
- `GET /admin/api/expenses/entries` – list; filters `from/to`, `supplier-id`, `payer-id`, `is-posted?`, pagination.
- `POST /admin/api/expenses/entries` – create expense with `items`.
- `GET /admin/api/expenses/entries/:id` – fetch with items.
- `PUT /admin/api/expenses/entries/:id` – update expense fields.
- `DELETE /admin/api/expenses/entries/:id` – soft delete.

### Expense Items (new 2025-12-25)
- `GET /admin/api/expenses/expense-items` – list expense items with pagination and filters.
- `POST /admin/api/expenses/expense-items` – create standalone expense item; requires `expense_id`, `line_total` and **either** `raw_label` (text) **or** `alias_id` (UUID). (Optional: `qty`, `unit_price`.)
- `GET /admin/api/expenses/expense-items/count` – total count with optional search.
- `GET /admin/api/expenses/expense-items/:id` – fetch single expense item.
- `PUT /admin/api/expenses/expense-items/:id` – update expense item (e.g. `raw_label` or `alias_id`, `qty`, `unit_price`, `line_total`).
- `DELETE /admin/api/expenses/expense-items/:id` – delete expense item.

### Articles / Price History
- `GET /admin/api/expenses/articles` – list/search.
- `POST /admin/api/expenses/articles` – create; requires `canonical_name`.
- `GET /admin/api/expenses/articles/:id` – fetch article.
- `PUT /admin/api/expenses/articles/:id` – update article.
- `DELETE /admin/api/expenses/articles/:id` – delete article.
- `GET /admin/api/expenses/articles/unmapped-aliases` – article aliases missing article mapping.
- `POST /admin/api/expenses/articles/aliases/:alias-id/map` – attach article to alias.
- `POST /admin/api/expenses/articles/:id/aliases` – add/replace alias for supplier/raw label.
- `GET /admin/api/expenses/articles/:id/price-history` – price observations (optional `supplier_id`, `limit`).
- `GET /admin/api/expenses/articles/:id/latest-prices` – latest price per supplier.
- `GET /admin/api/expenses/articles/:id/compare` – price observations for comparisons (optional `from`, `limit`).

### Article Aliases
- `GET /admin/api/expenses/article-aliases` – list aliases with optional filters.
- `POST /admin/api/expenses/article-aliases` – create new alias.
- `GET /admin/api/expenses/article-aliases/:id` – fetch alias.
- `PUT /admin/api/expenses/article-aliases/:id` – update alias.
- `DELETE /admin/api/expenses/article-aliases/:id` – delete alias.

### Price Observations
- `GET /admin/api/expenses/price-observations` – list price observations with filters.
- `POST /admin/api/expenses/price-observations` – create new price observation.
- `GET /admin/api/expenses/price-observations/:id` – fetch price observation.
- `PUT /admin/api/expenses/price-observations/:id` – update price observation.
- `DELETE /admin/api/expenses/price-observations/:id` – delete price observation.

### Reports
- `GET /admin/api/expenses/reports/summary` – totals for range.
- `GET /admin/api/expenses/reports/payers` – breakdown by payer.
- `GET /admin/api/expenses/reports/suppliers` – breakdown by supplier.
- `GET /admin/api/expenses/reports/weekly` – weekly totals.
- `GET /admin/api/expenses/reports/monthly` – monthly totals.
- `GET /admin/api/expenses/reports/top-suppliers` – top suppliers (optional `limit`).

## User API (mounted at `/api/v1/expenses`)

### Summary + Settings
- `GET /api/v1/expenses/summary` – Expense summary metrics.
- `GET /api/v1/expenses/by-month` – Monthly spending breakdown.
- `GET /api/v1/expenses/by-supplier` – Supplier spending breakdown.

**User expense settings (per-user, persisted)**
- `GET /api/v1/expenses/settings` – fetch effective user settings (defaults + any persisted values).
- `PUT /api/v1/expenses/settings` – update settings (**partial updates supported**).
  - Supported keys: `default_currency` (required when present), `default_payer_id` (UUID or blank to clear), `notifications_enabled` (boolean).

### Export & Danger Zone
- `GET /api/v1/expenses/export` – export user expenses (currently `format=csv` supported).
- `DELETE /api/v1/expenses/all` – soft-delete all user expenses (**admin/owner only**; requires confirmation token `DELETE_ALL_EXPENSES`).

### Reference Data (shared catalog)
- `GET /api/v1/expenses/suppliers` – list suppliers.
- `POST /api/v1/expenses/suppliers` – create supplier (role-gated to `member|admin`).
- `GET /api/v1/expenses/suppliers/:id` – fetch supplier.
- `PUT /api/v1/expenses/suppliers/:id` – update supplier (role-gated to `member|admin`).
- `DELETE /api/v1/expenses/suppliers/:id` – archive supplier (role-gated to `member|admin`).

**Supplier purge (hard delete; admin/owner only)**
- `GET /api/v1/expenses/suppliers/:id/purge-preview` – preview purge impact (**admin/owner only**).
- `POST /api/v1/expenses/suppliers/:id/purge` – permanently delete supplier (**admin/owner only**; requires supplier archived and no active expenses).

**Supplier detail lists (used by supplier detail UI)**
- `GET /api/v1/expenses/article-aliases` – list article aliases (typically filtered by supplier).
- `GET /api/v1/expenses/price-observations` – list price observations (typically filtered by supplier).

- `GET /api/v1/expenses/payers` – list payers.
- `POST /api/v1/expenses/payers` – create payer (role-gated to `member|admin`).
- `PUT /api/v1/expenses/payers/:id` – update payer (role-gated to `member|admin`).
- `DELETE /api/v1/expenses/payers/:id` – delete payer (role-gated to `member|admin`; may return `409` on FK violations).

### Power-user Endpoints (admin/owner only)
- `GET /api/v1/expenses/expense-items` – list expense items.
- `PUT /api/v1/expenses/expense-items/:id` – update expense item.
- `DELETE /api/v1/expenses/expense-items/:id` – delete expense item.

### Receipts
- `POST /api/v1/expenses/upload` – multipart upload (`file`); creates a receipt (status `uploaded`). Optional `payer_id` (UUID) overrides the user’s default payer for that upload.
- `GET /api/v1/expenses/receipts` – list receipts (filters `status`, `limit/offset`, `order_dir`). Response now includes each receipt’s `payer_id` so caller sees the upload-selected payer without fetching the detail again.
- `POST /api/v1/expenses/receipts/ocr` – trigger async OCR for a batch of receipt IDs (requires `MISTRAL_API_KEY`).
- `GET /api/v1/expenses/receipts/:id/download` – download/inline view the original file (`?download=true` forces attachment).
- `GET /api/v1/expenses/receipts/:id` – fetch receipt.
- `DELETE /api/v1/expenses/receipts/:id` – delete receipt.
- `POST /api/v1/expenses/receipts/:id/review` – save reviewed fields/items without approving.
- `POST /api/v1/expenses/receipts/:id/approve` – approve receipt and create expense.
- `POST /api/v1/expenses/receipts/:id/ocr` – trigger async OCR for a single receipt (requires `MISTRAL_API_KEY`).

### Articles + Auto-matching (admin/owner only)
- `GET /api/v1/expenses/articles` – list/search articles.
- `POST /api/v1/expenses/articles` – create article.
- `GET /api/v1/expenses/articles/unmapped-aliases` – list unmapped article aliases.
- `POST /api/v1/expenses/articles/aliases/:alias-id/map` – map an alias to an article.
- `POST /api/v1/expenses/articles/:id/aliases` – batch create aliases (supplier/raw labels) for an article.

### CRUD Operations
- `GET /api/v1/expenses` – List user expenses.
- `POST /api/v1/expenses` – Create new expense.
- `GET /api/v1/expenses/:id` – Fetch specific expense.
- `PUT /api/v1/expenses/:id` – Update expense.
- `DELETE /api/v1/expenses/:id` – Delete expense.

### Batch Operations
- `PUT /api/v1/expenses/batch` – batch update expenses.
- `POST /api/v1/expenses/batch-delete` – batch delete expenses.
