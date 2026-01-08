<!-- ai: {:tags [:expenses :domain :overview] :kind :guide} -->

# Expenses Domain Guide

## Overview

The expenses domain provides comprehensive financial management functionality for tracking business expenses, receipts, suppliers, and price monitoring. It integrates with the admin panel to provide full CRUD operations and reporting capabilities.

## Domain Architecture

### Core Entities

The expenses domain consists of several interconnected entities:

1. **Expenses** (`expenses`) - Main expense records (header rows) with line items in `expense_items`
2. **Expense Items** (`expense_items`) - Individual line items within an expense (2025-12-25: Now with standalone admin CRUD support)
3. **Receipts** (`receipts`) - Digital receipt storage and OCR processing
4. **Suppliers** (`suppliers`) - Vendor/supplier management
5. **Payers** (`payers`) - Payment method management
6. **Articles** (`articles`) - Product/item catalog with pricing
7. **Article Aliases** (`article_aliases`) - Alternative names for articles
8. **Price Observations** (`price_observations`) - Historical price tracking

### Entity Relationships

```
Suppliers ────┐
             ├──► Expenses ──► Receipts
Payers ───────┘                │
                               │
Articles ───► Article Aliases   │
    │                         │
    └──► Price Observations ──┘
    │
    └──► Expense Items (expense_items)
```

## Frontend Implementation

### Routes and Pages

The expenses domain contributes the following routes under `/admin` (see `src/app/domain/frontend/expenses/routes.cljs`):

- `/expenses` - List and manage expenses
- `/expenses/:id` - Expense detail / edit view
- `/expense-items` - List and manage individual expense line items (new 2025-12-25; create/edit via modal)
- `/receipts` - Receipt inbox
- `/receipts/:id` - Receipt detail / processing view
- `/suppliers` - Supplier list
- `/suppliers/:id` - Supplier detail / edit view
- `/payers` - Payer list
- `/payers/:id` - Payer detail / edit view
- `/articles` - Article list
- `/articles/:id` - Article detail / edit view
- `/article-aliases` - Article alias list
- `/article-aliases/:id` - Article alias detail / edit view
- `/price-observations` - Price observations list
- `/price-observations/:id` - Price observation detail / edit view

### Components and Features

#### Generic Admin Entity Pattern

All expense entities use the generic admin entity page pattern:

```clojure
;; Example: Suppliers page
(defui suppliers-page []
  ($ generic-admin-entity-page :suppliers))
```

This provides:
- Standard list view with filtering and sorting
- Create/edit forms with validation
- Delete operations with confirmation
- Batch operations where supported

#### List View Controls

Each entity supports configurable list view controls:
- Column visibility toggles
- Filter and sort options
- Batch selection and operations
- Search functionality

#### Form Configuration

Admin forms (the `/admin/...` pages) are configured per entity via the **Admin scope** settings, merged from:

- **System/admin-owned** config: `src/app/admin/frontend/config/*.edn`
- **Domain-owned** admin config: `src/app/domain/**/admin/config/*.edn` (for Expenses: `src/app/domain/frontend/expenses/admin/config/*`)

These are edited via `/admin/admin-settings`.

The form fields configuration lives in `form-fields.edn` and defines:
- Create field lists
- Edit field lists
- Required field validation
- Field-specific configuration (type, validation rules)

User-facing forms (the `/expenses/...` pages) use the domain-owned UI config under `src/app/domain/frontend/expenses/config/` (editable via `/admin/user-settings`).

#### Master/Detail Edit Forms (Expense + Line Items)

Expense edit views/forms need a **detail fetch** to populate line items (`:items`). To avoid duplicated orchestration code (requested flags, detail fetch, memoization to prevent Fork resets), the template provides a reusable wrapper:

- `app.template.frontend.components.form.master-detail/master-detail-form`
- Doc: `docs/frontend/master-detail-form.md`

Current integrations:

- Admin: `src/app/domain/frontend/expenses/components/expense_form.cljs`
- User: `src/app/domain/frontend/expenses/components/user_expense_form.cljs`

**Admin detail response key note:** the admin expenses detail endpoint returns `{ :expense ... }` (singular). The generic events factory supports this via `:detail-response-key` in `src/app/domain/frontend/expenses/events/entity_configs.cljs` so `::load-detail` stores the correct entity under `[:admin :expenses :entries :by-id <id>]`.

## User Expenses Interface

In addition to the admin panel, the expenses domain provides a user-facing interface for personal expense management.

### Routes

- `/expenses` - Personal expense dashboard
- `/expenses/list` - Expense list/history
- `/expenses/new` - Quick expense entry
- `/expenses/upload` - Receipt upload wizard (creates a `receipts` row)
- `/receipts` - Receipts inbox (review + approve)
- `/receipts/:receipt-id` - Receipt detail / approve flow
- `/suppliers` - Suppliers reference data
- `/payers` - Payers reference data
- `/expenses/:expense-id` - Expense detail
- `/expenses/reports` - Personal spending reports

### Key Features

1. **Dashboard**: Overview of recent spending and monthly trends (`expenses_dashboard.cljs`)
2. **History**: Expense list with filtering/pagination (`expenses_list.cljs`)
3. **Quick Entry**: Simplified form for rapid expense recording (`expense_new.cljs`)
4. **Receipt Upload**: Multipart upload that creates a receipt for OCR processing (`expense_upload.cljs`)
5. **Receipts Inbox**: Review receipts and approve into expenses (`receipts_list.cljs`, `receipt_detail.cljs`)
6. **Reference Data**: Browse/manage suppliers and payers (`suppliers.cljs`, `payers.cljs`)
7. **Reports**: Visual breakdown of expenses by category and supplier (`expense_reports.cljs`)

## Backend Implementation

### API Endpoints

Admin expense domain APIs are mounted under `/admin/api/expenses`.
User-facing APIs are mounted under `/api/v1/expenses` (upload, receipts inbox, suppliers/payers reference data, expenses CRUD).

#### Core Entity Operations

**Suppliers**
- Full CRUD: create, read, update, delete
- Search and autocomplete functionality
- Reference checking before deletion

**Payers**
- Type-based categorization
- Default payer per type
- Suggestion engine based on payment method

**Receipts**
- File upload and storage
- OCR processing workflow
- Status management (uploaded → parsing → parsed → extracting → extracted|review_required → approved → posted|failed)
- Retry mechanism for failed processing

**Expenses**
- Complex expense creation with multiple line items
- Item-to-article mapping
- Posting status management
- Date-range filtering

**Expense Items** (new 2025-12-25)
- Standalone admin CRUD for `expense_items` line items (in addition to the items embedded in expense detail).
- Search spans raw label, article name, and the parent expense’s supplier/payer.

**Articles**
- Product catalog management
- Alias creation for supplier-specific names
- Price history tracking
- Unmapped item identification

#### New Entity APIs

**Article Aliases**
- Manage alternative product names
- Supplier-specific naming
- Bulk operations support

**Price Observations**
- Historical price tracking
- Supplier-specific pricing
- Comparison and reporting features

### Service Layer

Key services in `src/app/domain/backend/expenses/services/`:

- **articles.clj** - Article CRUD and alias management
- **article_aliases.clj** - Supplier-specific aliases mapped to articles
- **expenses.clj** - Expense CRUD and detail fetch (includes line items)
- **expense_items.clj** - Expense item CRUD (standalone admin page + API)
- **price_history.clj** - Price observation queries/helpers
- **price_observations.clj** - Price observations CRUD (and linking to expense items)
- **receipts.clj** - Receipt upload/status workflow and approval → expense creation
- **suppliers.clj** - Supplier management
- **payers.clj** - Payment method management
- **reports.clj** - Summary and breakdown reports

## Data Models

### Core Tables

```clojure
;; Receipts (receipts)
{:id :uuid
 :user_id :uuid?
 :storage_key :string
 :file_hash :string
 :original_filename :string?
 :content_type :string?
 :file_size :int?
 :status :enum
 :raw_parse_json :json?
 :raw_extract_json :json?
 :parsed_markdown :string?
 :supplier_guess :string?
 :total_amount_guess :decimal?
 :currency_guess :enum?
 :purchased_at_guess :timestamp?
 :error_message :string?
 :error_details :json?
 :retry_count :int
 :expense_id :uuid?
 :created_at :timestamp
 :updated_at :timestamp}

;; Expenses (expenses)
{:id :uuid
 :user_id :uuid?
 :receipt_id :uuid?
 :supplier_id :uuid
 :payer_id :uuid
 :purchased_at :timestamp
 :total_amount :decimal
 :currency :enum
 :notes :string?
 :is_posted :boolean
 :deleted_at :timestamp?
 :created_at :timestamp
 :updated_at :timestamp}

;; Expense Items (expense_items)
{:id :uuid
 :expense_id :uuid
 :article_id :uuid?
 :raw_label :string
 :qty :decimal
 :unit_price :decimal
 :line_total :decimal
 :created_at :timestamp}

;; Articles
{:id :uuid
 :canonical_name :string
 :normalized_key :string
 :barcode :string?
 :category :string?
 :created_at :timestamp
 :updated_at :timestamp}

;; Article Aliases
{:id :uuid
 :supplier_id :uuid
 :article_id :uuid
 :raw_label_normalized :string
 :confidence :int
 :created_at :timestamp}

;; Price Observations
{:id :uuid
 :article_id :uuid
 :supplier_id :uuid
 :expense_item_id :uuid?
 :observed_at :timestamp
 :unit_price :decimal?
 :line_total :decimal
 :qty :decimal?
 :currency :enum
 :created_at :timestamp}
```

## Workflow Examples

### Receipt Processing Flow

1. **Upload**: User uploads receipt image/file (creates a `receipts` row with status `uploaded`)
2. **OCR (async)**: Receipt OCR worker processes pending receipts and stores markdown/extraction + guess fields (status transitions toward `extracted` / `review_required`)
3. **Review**: User/admin reviews receipt detail and adjusts extracted data as needed
4. **Mapping**: Map items to articles (create if needed)
5. **Approval**: Approve receipt and create expense
6. **Posting**: Receipt becomes `posted` after expense creation

### Receipt OCR Worker (Mistral)

- Run one-shot processing: `bb receipt-ocr-worker dev`
- Run continuously: `bb receipt-ocr-worker dev --loop` (polls every 30s by default)
- Requires `MISTRAL_API_KEY` (disable with `MISTRAL_OCR_ENABLED=false`); see `PLAN-mistral-ocr-pos-receipts.md` for details.

### Price Tracking Flow

1. **Expense Creation**: Items linked to articles record prices
2. **Automatic Capture**: Price observations created for each item
3. **Historical Analysis**: View price trends over time
4. **Supplier Comparison**: Compare prices across suppliers
5. **Reporting**: Generate price change reports

## Configuration

### UI Configuration

This domain has two related configuration locations:

**Admin UI (admin list pages)**
- Merged from:
  - system config in `src/app/admin/frontend/config/`
  - domain config in `src/app/domain/**/admin/config/` (for Expenses: `src/app/domain/frontend/expenses/admin/config/`)
- Edited via `/admin/admin-settings`

**User UI config (domain-owned, user-facing defaults/locks)**
- Stored in `src/app/domain/frontend/expenses/config/`
- Edited via `/admin/user-settings`

Both use the same EDN file types:
- **entities.edn** - Entity definitions and metadata
- **table-columns.edn** - Structural column configuration (incl. `:always-visible` enforcement)
- **form-fields.edn** - Create/edit field lists + required fields
- **view-options.edn** - Policy defaults/locks for display toggles and column visibility

### Example Configuration

```clojure
;; entities.edn (admin scope)
;; For domain entities, the EDN is *metadata only*; the effective :adapter-init-fn
;; is injected at preload time from the domain registry.
{:articles {:entity-key :articles
            :page-title "Articles"
            :page-description "Canonical expense items"
            ;; see `app.domain.frontend.expenses.admin.config.preload`
            :display-settings {:show-add-button? true
                               :per-page 50}}}

;; table-columns.edn
{:articles {:available-columns [:canonical-name :barcode :category :normalized-key :created-at :updated-at :id]
            :default-visible-columns [:canonical-name :barcode :category :created-at]
            :filterable-columns [:canonical-name :barcode :category :normalized-key :created-at :updated-at]
            :sortable-columns [:canonical-name :barcode :category :normalized-key :created-at :updated-at]
            :always-visible [:canonical-name]}}

;; form-fields.edn
{:articles {:create-fields [:canonical-name :barcode :category]
            :edit-fields [:canonical-name :barcode :category]
            :required-fields [:canonical-name]}}
```

## Integration Points

### Admin Panel Integration

The expenses domain integrates seamlessly with the admin panel:

- **Navigation**: Sidebar links under "Expenses" section
- **Entity Registry**: Automatic registration with admin system
- **Generic Components**: Reuse of admin table and form components
- **Settings**: Configurable through admin settings interface

### Template Infrastructure

Leverages shared template components:

- **List Views**: Generic list component with filtering
- **Forms**: Form validation and submission handling
- **HTTP Client**: Standardized API communication
- **Error Handling**: Consistent error display and reporting

## Best Practices

### Data Management

1. **Reference Integrity**: Check dependencies before deletion
2. **Soft Deletes**: Use soft deletes for financial records
3. **Audit Trail**: Track all changes to financial data
4. **Currency Handling**: Always store currency with amounts

### User Experience

1. **Progressive Disclosure**: Hide complex features behind toggles
2. **Auto-complete**: Help users find existing entities
3. **Bulk Operations**: Support batch updates where appropriate
4. **Search**: Provide robust search across all text fields

### Performance

1. **Pagination**: Always paginate large lists
2. **Indexing**: Proper database indexes for common queries
3. **Caching**: Cache reference data (suppliers, articles)
4. **Lazy Loading**: Load related data on demand

## Testing

### Frontend Tests

- Component unit tests for each entity page
- Integration tests for CRUD operations
- UI testing for form validation
- List view control testing

### Backend Tests

- API endpoint tests for all operations
- Service layer unit tests
- Database integration tests
- Workflow testing (receipt processing)

## Future Enhancements

### Planned Features

1. **Advanced Reporting**: More comprehensive financial reports
2. **Budget Tracking**: Add budget categories and tracking
3. **Receipt Mobile App**: Mobile receipt capture
4. **Invoice Integration**: Generate invoices from expenses
5. **Tax Reporting**: Tax categorization and reporting

### Technical Improvements

1. **Real-time Updates**: WebSocket updates for collaborative editing
2. **File Storage**: Cloud storage integration for receipts
3. **OCR Enhancement**: Machine learning for better OCR
4. **API Versioning**: Versioned API for external integrations

This expenses domain provides a solid foundation for financial management within the single-tenant template, with clear patterns for extending functionality and integrating new features.
