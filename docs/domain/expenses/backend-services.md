<!-- ai: {:namespaces [app.domain.backend.expenses.*] :tags [:expenses :domain :backend] :kind :guide} -->

# Expenses Backend Services

This document covers the service layer for the **Home Expenses** domain.

## Service Map
- **Suppliers** (`app.domain.backend.expenses.services.suppliers`) — CRUD, normalization/dedupe by `normalized_key`, search/count helpers. Receipt OCR uses Places-assisted resolution (`resolve-or-create-supplier-with-places!`) and includes legacy `normalized_key` compatibility for older rows.
- **Payers** (`app.domain.backend.expenses.services.payers`) — CRUD, default-per-type management.
- **Receipts** (`app.domain.backend.expenses.services.receipts`) — upload with file-hash dedupe, status transitions, approve → post expense, extraction storage. Uploads now capture the selected `payer_id` so approval/edit forms can prefill the payer on a per-receipt basis.
- **Receipt OCR (Mistral)** (`app.domain.backend.expenses.integrations.mistral-ocr`, `app.domain.backend.expenses.workers.receipt-ocr.core`) — out-of-band worker that processes uploaded receipts and populates markdown + extraction results/guesses.
- **Expenses** (`app.domain.backend.expenses.services.expenses`) — create/update with line items, soft delete, listing filters; records price observations.
- **Expense Items** (`app.domain.backend.expenses.services.expense-items`) — standalone CRUD for `expense_items` line items (list/count/search; joins to expense/supplier/payer/article for admin tables).
- **Raw Labels** (`app.domain.backend.expenses.services.raw-labels`) — dedupe + normalization for line-item labels; `expense_items` stores a FK to `raw_labels`.
- **Articles/Aliases/Price history** (`app.domain.backend.expenses.services.articles`, `price-history`) — canonical articles, alias mapping, price observation queries.
- **Reports** (`app.domain.backend.expenses.services.reports`) — summary, payer/supplier breakdowns, weekly/monthly totals, top suppliers.

## Routing Notes
- Routes are mounted under `/admin/api/expenses` via the backend domain registry (`app.domain.backend.registry`).
- The concrete implementation lives in `app.domain.backend.expenses.routes.*`.
- For endpoint details, see [Expenses HTTP API](./http-api.md).

## Supplier resolution & dedupe

Suppliers are deduped via `suppliers.normalized_key` (derived from `display_name`). During receipt OCR processing we additionally use Google Places API v1 as a **canonicalizer**:

- Fast path: lookup by `normalized_key`.
- Miss path: call Places (failure-safe; never blocks OCR), select the best candidate, then lookup again by candidate `normalized_key`.
- Compatibility: the resolver also checks a *legacy* normalization variant (historically, diacritics like `Š/š` could be dropped), so new OCR runs don’t create duplicates against old rows.
