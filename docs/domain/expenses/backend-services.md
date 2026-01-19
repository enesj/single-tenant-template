<!-- ai: {:namespaces [app.domain.backend.expenses.*] :tags [:expenses :domain :backend] :kind :guide} -->

# Expenses Backend Services

This document covers the service layer for the **Home Expenses** domain.

## Service Map
- **Suppliers** (`app.domain.backend.expenses.services.suppliers`) — CRUD, normalization/dedupe by `normalized_key`, search/count helpers.
- **Payers** (`app.domain.backend.expenses.services.payers`) — CRUD, default-per-type management.
- **Receipts** (`app.domain.backend.expenses.services.receipts`) — upload with file-hash dedupe, status transitions, approve → post expense, extraction storage.
- **Receipt OCR (Mistral)** (`app.domain.backend.expenses.integrations.mistral-ocr`, `app.domain.backend.expenses.workers.receipt-ocr.core`) — out-of-band worker that processes uploaded receipts and populates markdown + extraction results/guesses.
- **Expenses** (`app.domain.backend.expenses.services.expenses`) — create/update with line items, soft delete, listing filters; records price observations.
- **Expense Items** (`app.domain.backend.expenses.services.expense-items`) — standalone CRUD for `expense_items` line items (list/count/search; joins to expense/supplier/payer/article for admin tables).
- **Articles/Aliases/Price history** (`app.domain.backend.expenses.services.articles`, `price-history`) — canonical articles, alias mapping, price observation queries.
- **Reports** (`app.domain.backend.expenses.services.reports`) — summary, payer/supplier breakdowns, weekly/monthly totals, top suppliers.

## Routing Notes
- Routes are mounted under `/admin/api/expenses` via the backend domain registry (`app.domain.backend.registry`).
- The concrete implementation lives in `app.domain.backend.expenses.routes.*`.
- For endpoint details, see [Expenses HTTP API](./http-api.md).
