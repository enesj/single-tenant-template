# Home Expenses Tracker — Full App Spec (Draft)

## 1) Overview

A lightweight web app to track household expenses with two input paths:

1) **Receipt scanning (POS bills)** using **LandingAI ADE** (Parse → Extract) to capture supplier + totals + line items.
2) **Manual entry**, optimized with autocomplete and “remembered” suppliers/payers/articles.

The app stores:
- Expenses (with **payer** + **supplier/payee**)
- Articles / line items (for **price comparisons** across suppliers)
- Reports (weekly/monthly, breakdowns by payer/supplier/category/article)

---

## 2) Core Definitions (to avoid ambiguity)

- **Supplier / Payee**: the merchant you paid (e.g., “Bingo”, “Konzum”, “Pharmacy X”).
- **Payer**: who/what paid:
  - `cash` (a “Cash” wallet)
  - `card` (specific card like “Visa ••••1234”, or generic card)
  - `account` (bank account / e-banking pot)
  - `person` (e.g., Enes, spouse) — optional, if you want per-person reporting

- **Receipt**: the uploaded file + ADE outputs + review status (may or may not become an Expense).
- **Expense**: a finalized financial record used in reports.
- **Line item / Article**:
  - Line item is “as on receipt” (raw label + line_total)
  - Article is a normalized canonical product identity used for price history and comparisons.

---

## 3) Goals & Non-goals

### Goals (MVP)
- Fast capture of expenses from receipts + manual flow
- Accurate payer reports (weekly/monthly totals per payer)
- Supplier-based reports
- Article price comparisons across suppliers

### Non-goals (MVP)
- Accounting-grade tax, VAT, invoice compliance
- Bank sync / PSD2 connections
- Perfect article normalization without user confirmation

---

## 4) Target Users / Use Cases

### Primary User
A household member tracking spending.

### Key Use Cases
- Upload receipt photo → auto-extract → quick review → save as expense
- Add manual expense (when no receipt / quick entry)
- See weekly/monthly spending, with breakdown per payer/supplier
- Compare price of the same article across suppliers

---

## 5) Functional Requirements

### 5.1 Receipt Capture & Processing

**Upload formats**
- Image: JPG/PNG/HEIC (if supported by your upload stack)
- PDF (optional; but receipts usually single page)

**Receipt lifecycle**
- `uploaded` → `parsing` → `parsed` → `extracting` → `extracted`
- `review_required` (missing or inconsistent key fields)
- `approved` → `posted` (creates Expense + Line Items)
- `failed` (store error details)

**Deduplication**
- Compute `file_hash` (SHA-256) on upload. If already exists for household, show existing receipt and block or allow “duplicate anyway”.

**ADE Extraction**
- Use **Totals + Line Items** schema (from your previous doc).
- Store:
  - Parsed markdown (optional)
  - Raw parse JSON (optional)
  - Raw extract JSON (recommended for troubleshooting + reprocessing)
  - Extracted normalized fields placed into app models after review/approval

**Validation rules (at review time)**
- Must have:
  - supplier name (or user selects supplier)
  - total amount
  - purchased_at (optional, but recommended; if missing, user must set it)
  - payer (always required for final Expense)
- Line items:
  - Accept empty list if extraction fails, but allow manual adding

**User review**
- Show extracted fields with inline editing:
  - Supplier (autocomplete + create new)
  - Purchased date/time (date picker + time picker)
  - Total (numeric)
  - Payment hints → suggest payer
  - Line items table (editable)
- Buttons:
  - “Save Draft” (keeps receipt, not in reports)
  - “Approve & Post” (creates Expense)

---

### 5.2 Manual Data Entry

**Manual Expense form**
- Supplier (required): autocomplete existing suppliers + “create new”
- Purchased date/time (required): default to “now”
- Payer (required): select existing payer or create new
- Total:
  - Option A: auto-sum from items
  - Option B: allow manual total override (with a warning if mismatch)
- Line items:
  - Add item rows quickly: raw_label + line_total (minimum)
  - Optional qty + unit price
- Notes (optional)

**Manual Supplier creation**
- name required
- address/tax_id optional

---

### 5.3 Payers (Cash/Card/Account/Person)

**Payer types**
- Cash: single default “Cash” payer per household
- Card: multiple cards, optional last4
- Account: one or more accounts (e.g., “Revolut”, “UniCredit”)
- Person: optional if you want per-person spending (can overlap with payer types if you want, but MVP keeps payer as the payment source)

**Payer reports**
- Must support:
  - Total per payer by week and month
  - Breakdown per payer → suppliers (top merchants)
  - Breakdown per payer → categories (optional)

---

### 5.4 Suppliers (Payees)

**Supplier list**
- name, address, tax_id (optional)
- dedupe by normalized key (case-insensitive, trim spaces, remove punctuation)

**Supplier report page**
- total spend in period
- trend chart by month
- top articles purchased (by spend)

---

### 5.5 Articles & Price Comparison

This is the “money feature”: compare the same product across suppliers.

**Data approach**
- Keep every line item as:
  - `raw_label` (always)
  - optional `article_id` (normalized canonical product)
- Price history is computed from line items mapped to a canonical article.

**Article normalization workflow**
1) When a receipt is posted, attempt to map each `raw_label` to an `article_id`:
   - Exact normalized match
   - Alias match
   - Fuzzy match suggestion (optional)
2) If confidence is low:
   - Mark line item as “unmapped”
   - In UI: show “Match to existing article / create new article”
3) Once user confirms mapping:
   - Store an alias rule: `(supplier_id, raw_label_normalized) -> article_id`
   - This makes the next receipt from that supplier auto-map reliably

**Price comparison view**
- Search/select Article → show:
  - last observed unit/line price per supplier
  - average over last 30/90 days
  - min price in range
  - time series (optional)
- Note: if `unit_price` is often missing, use `line_total / qty` when qty exists; else fall back to `line_total` and treat as “package price”.

---

### 5.6 Reporting

**Reporting periods**
- Weekly: ISO week, **week starts Monday** (important for Europe/Sarajevo consistency)
- Monthly: calendar month
- Use `purchased_at` as the primary timestamp (not upload time)

**Required reports (MVP)**
- Dashboard:
  - This week total, this month total
  - Top 5 suppliers this month
  - Spend by payer this month
- Reports by Payer:
  - Weekly/monthly totals per payer
  - For selected payer: top suppliers, trend
- Reports by Supplier:
  - Weekly/monthly totals per supplier
  - For selected supplier: top articles, trend
- Report by Article:
  - spend on an article in a date range
  - price comparison across suppliers

**Optional (nice-to-have)**
- Categories and category breakdown
- Budget alerts (monthly cap by payer/category)

---

## 6) Non-Functional Requirements

### Performance
- Upload → “review screen” should feel fast:
  - Show “receipt uploaded” immediately
  - Process ADE in background while user stays in inbox
- Reports should load quickly using DB aggregates / indexes.

### Reliability
- Robust retry for ADE calls (network/timeouts).
- Safe idempotency:
  - posting the same receipt twice should not create duplicate expenses.

### Security & Privacy
- Store ADE API key server-side only.
- Role model: single user or household; basic auth is fine for MVP.
- Retention policy:
  - keep original receipt files configurable (e.g., 90 days) or forever.

---

## 7) System Architecture (recommended)

### Components
1) **Web UI** (React/Next.js or similar)
2) **API server** (Node/TS)
3) **Job worker** (can be the same server initially)
4) **Database** (Postgres recommended)
5) **Object storage** for receipts (local disk for dev, S3-compatible for prod)

### Suggested flow
- UI uploads file → API stores file + creates Receipt row
- Worker runs:
  - ADE Parse (optional but recommended)
  - ADE Extract with your schema
- UI polls receipt status or uses websockets/SSE
- User reviews → approves → API posts Expense + items + price observations

---

## 8) Database Model (MVP)

### Tables (minimal)
- `suppliers`
- `payers`
- `receipts`
- `expenses`
- `expense_items`
- `articles`
- `article_aliases` (raw label mapping)
- `price_observations` (optional; can be derived, but better to store)

### Key columns (high level)
**suppliers**
- id, household_id, display_name, address?, tax_id?, normalized_key

**payers**
- id, household_id, type (cash/card/account/person), label, last4?

**receipts**
- id, household_id, storage_key, file_hash
- status, raw_extract_json, purchased_at?, total_amount?, supplier_guess?
- created_at

**expenses**
- id, household_id, receipt_id?
- supplier_id (required)
- payer_id (required)
- purchased_at (required)
- total_amount (required), currency
- notes?

**expense_items**
- id, expense_id
- raw_label (required)
- article_id? (nullable)
- qty?, unit_price?, line_total (required)

**articles**
- id, household_id
- canonical_name, normalized_key, barcode?

**article_aliases**
- id, household_id
- supplier_id? (nullable if global)
- raw_label_normalized
- article_id

---

## 9) API Endpoints (Draft)

### Receipts
- `POST /api/receipts` upload (multipart)
- `GET /api/receipts` list (filter by status/date)
- `GET /api/receipts/:id` details (extraction results)
- `POST /api/receipts/:id/retry` rerun extraction
- `POST /api/receipts/:id/approve` create expense from receipt

### Expenses
- `POST /api/expenses` manual create
- `GET /api/expenses` list (filters)
- `GET /api/expenses/:id` details
- `PUT /api/expenses/:id` edit
- `DELETE /api/expenses/:id` (soft delete recommended)

### Reference data
- `GET/POST /api/suppliers`
- `GET/POST /api/payers`
- `GET/POST /api/articles`
- `POST /api/aliases` (map raw label → article)

### Reports
- `GET /api/reports/summary?from=&to=`
- `GET /api/reports/by-payer?from=&to=`
- `GET /api/reports/by-supplier?from=&to=`
- `GET /api/reports/article/:articleId/prices?from=&to=`

---

## 10) UX Screens (MVP)

1) **Inbox**
- List receipts with status chips:
  - Needs review / Ready / Posted / Failed
- Quick actions: retry / open / delete

2) **Review Receipt**
- Supplier selector
- Purchased at picker
- Total amount
- Payer selector (with suggestion from payment_hints)
- Line items editable grid
- “Approve & Post” and “Save Draft”

3) **Add Expense**
- Supplier + payer + date/time
- Items grid
- Total auto-calculated with override

4) **Reports**
- Summary (week/month toggles)
- By payer (table + trend)
- By supplier (table + trend)
- Article price compare

5) **Articles**
- Search articles
- “Unmapped items” queue → map to articles fast

---

## 11) Cost & Operational Cost Reduction (practical)

- Prefer **single image per receipt**; avoid multi-page PDFs.
- Keep extraction schema minimal (your current schema is good).
- Deduplicate uploads via `file_hash` to avoid re-processing.
- Support **Totals-only mode** as a fallback if line items extraction frequently fails (even if your main schema is totals + items).
- Limit retries automatically; require manual retry clicks after N failures.
- Cache article aliases so repeated receipts become cheaper operationally (less user correction time).

---

## 12) Edge Cases & Rules

- **Negative lines / returns**: allow negative `line_total` and adjust totals; MVP can_
