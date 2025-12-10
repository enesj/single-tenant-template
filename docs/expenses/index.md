<!-- ai: {:tags [:expenses :domain :overview] :kind :guide} -->

# Expenses Domain Guide

## Overview

The expenses domain provides comprehensive financial management functionality for tracking business expenses, receipts, suppliers, and price monitoring. It integrates with the admin panel to provide full CRUD operations and reporting capabilities.

## Domain Architecture

### Core Entities

The expenses domain consists of several interconnected entities:

1. **Expenses** (`expense_entries`) - Main expense records with line items
2. **Receipts** (`receipts`) - Digital receipt storage and OCR processing
3. **Suppliers** (`suppliers`) - Vendor/supplier management
4. **Payers** (`payers`) - Payment method management
5. **Articles** (`articles`) - Product/item catalog with pricing
6. **Article Aliases** (`article_aliases`) - Alternative names for articles
7. **Price Observations** (`price_observations`) - Historical price tracking

### Entity Relationships

```
Suppliers ────┐
             ├──► Expenses ──► Receipts
Payers ───────┘                │
                               │
Articles ───► Article Aliases   │
    │                         │
    └───► Price Observations ──┘
```

## Frontend Implementation

### Routes and Pages

All expense domain pages are accessible under `/admin` with the following routes:

- `/expenses` - List and manage expenses
- `/expenses/new` - Create new expense
- `/expenses/:id` - Edit existing expense
- `/receipts` - View and process receipts
- `/suppliers` - Manage suppliers
- `/payers` - Manage payment methods
- `/articles` - Manage product catalog (new)
- `/article-aliases` - Manage article aliases (new)
- `/price-observations` - View price history (new)

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

Forms are configured per entity in `src/app/admin/frontend/config/form-fields.edn`:
- Create field lists
- Edit field lists
- Required field validation
- Field-specific configuration (type, validation rules)

## Backend Implementation

### API Endpoints

All expense domain APIs are mounted under `/admin/api/expenses`:

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
- Status management (uploaded → processing → approved → posted)
- Retry mechanism for failed processing

**Expenses**
- Complex expense creation with multiple line items
- Item-to-article mapping
- Posting status management
- Date-range filtering

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

Key services in `src/app/domain/expenses/services/`:

- **articles.clj** - Article CRUD and alias management
- **price_history.clj** - Price observation tracking
- **receipt_processing.clj** - OCR and receipt workflow
- **suppliers.clj** - Supplier management
- **payers.clj** - Payment method management

## Data Models

### Core Tables

```clojure
;; Expenses (expense_entries)
{:id :uuid
 :supplier_id :uuid
 :payer_id :uuid
 :total_amount :decimal
 :currency :string
 :date :date
 :is_posted? :boolean
 :created_at :timestamp
 :updated_at :timestamp}

;; Expense Items (expense_items)
{:id :uuid
 :expense_id :uuid
 :article_id :uuid?
 :description :string
 :quantity :decimal
 :unit_price :decimal
 :total_price :decimal}

;; Articles
{:id :uuid
 :canonical_name :string
 :description :string?
 :category :string?
 :active? :boolean
 :created_at :timestamp}

;; Article Aliases
{:id :uuid
 :article_id :uuid
 :supplier_id :uuid
 :alias_name :string
 :created_at :timestamp}

;; Price Observations
{:id :uuid
 :article_id :uuid
 :supplier_id :uuid
 :price :decimal
 :currency :string
 :observed_at :date
 :created_at :timestamp}
```

## Workflow Examples

### Receipt Processing Flow

1. **Upload**: User uploads receipt image/file
2. **Processing**: OCR service extracts expense data
3. **Review**: Admin reviews extracted data
4. **Mapping**: Map items to articles (create if needed)
5. **Approval**: Approve receipt and create expense
6. **Posting**: Mark expense as posted for accounting

### Price Tracking Flow

1. **Expense Creation**: Items linked to articles record prices
2. **Automatic Capture**: Price observations created for each item
3. **Historical Analysis**: View price trends over time
4. **Supplier Comparison**: Compare prices across suppliers
5. **Reporting**: Generate price change reports

## Configuration

### UI Configuration

Expense entities are configured in `src/app/admin/frontend/config/`:

- **entities.edn** - Entity definitions and adapters
- **table-columns.edn** - Column configurations for each entity
- **form-fields.edn** - Form field definitions and validation
- **view-options.edn** - Default view settings and controls

### Example Configuration

```clojure
;; entities.edn
{:articles {:display-name "Articles"
            :plural-name "Articles"
            :adapter-init-fn app.admin.frontend.adapters.expenses/init-articles
            :icon-style "fas fa-box"
            :color-class "text-blue-600"}}

;; table-columns.edn
{:articles {:available-columns [:canonical_name :description :category :created-at]
            :default-hidden-columns [:description]
            :always-visible [:canonical_name]}}

;; form-fields.edn
{:articles {:create-fields [:canonical_name :description :category]
            :edit-fields [:canonical_name :description :category :active?]
            :required-fields [:canonical_name]}}
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