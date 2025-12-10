# Home Expenses Tracker — Implementation Progress

**Last Updated**: 2025-12-08

---

## ✅ Phase 1: Database Schema & Migrations (COMPLETED)

### Summary
Successfully created and migrated all 8 expense tracker domain tables to the database.

### Completed Tasks

#### Database Schema Definition
- ✅ Created `resources/db/domain/models.edn` with complete schema
- ✅ Defined 3 custom enums:
  - `payer-type` (cash, card, account, person)
  - `receipt-status` (uploaded, parsing, parsed, extracting, extracted, review_required, approved, posted, failed)
  - `currency` (BAM, EUR, USD)

#### Tables Created
- ✅ **suppliers** — Stores, shops, vendors (6 fields, 1 unique index)
- ✅ **payers** — Payment methods (6 fields, 1 index)
- ✅ **receipts** — Uploaded receipt tracking (21 fields, 3 indexes)
- ✅ **expenses** — Posted transactions (11 fields, 4 indexes)
- ✅ **expense_items** — Line items on receipts (7 fields, 2 indexes)
- ✅ **articles** — Canonical products (6 fields, 2 indexes)
- ✅ **article_aliases** — Raw label → article mapping (6 fields, 2 indexes)
- ✅ **price_observations** — Price history tracking (10 fields, 3 indexes)

#### Migrations
- ✅ Enhanced `hierarchical_models.clj` to support direct domain/models.edn files
- ✅ Generated migration `0010_schema.edn` (7753 bytes)
- ✅ Applied all 10 migrations to database successfully
- ✅ Verified all tables exist in database

### Technical Notes

#### Circular Dependency Resolution
- Removed foreign key constraint from `receipts.expense_id` to break circular dependency with `expenses` table
- Will add FK constraint in a future migration after addressing workflow needs
- Both tables can now be created in the same migration

#### Schema Organization
- All domain models are in `resources/db/domain/models.edn`
- Tables follow template patterns with:
  - UUID primary keys
  - Timestamptz for dates (created_at, updated_at, etc.)
  - Proper indexes on foreign keys and query fields
  - Enum types defined per-table in `:types` section

---

## ✅ Phase 2: Backend Services & Routes (COMPLETED)

### What shipped
- Implemented domain services: suppliers (normalize/dedupe), payers (defaults per type), receipts (upload + status + approval), expenses (with items + soft delete), articles + aliases, price history, reports.
- Added HoneySQL queries with enum casts for `payer_type`, `receipt_status`, `currency`.
- Admin API routes mounted under `/admin/api/expenses` via `src/app/domain/expenses/routes/*` and `src/app/backend/routes/admin_api.clj`.
- Added migration-backed schema already applied.
- Integration tests for services cover supplier dedupe, payer defaults, receipt approval → expense, price observation recording, and soft-delete exclusion; all passing (5 tests, 13 assertions).

---

## ✅ Phase 3A: Admin Expense Tracking UI (COMPLETED)

### What shipped
- **Backend enrichment**: Enhanced `list-expenses` and `get-expense-with-items` services to return supplier/payer display names, types, and metadata for direct UI consumption.
- **Admin expense list**: Rebuilt `/admin/expenses` with formatted supplier/payer cells, currency styling, status badges, and "View" links for expense details.
- **Admin expense detail**: Added `/admin/expenses/:id` route with guarded controller that loads expense metadata, line items, and breadcrumbs when `:id` param is available.
- **Admin data management**: Full CRUD capabilities for suppliers, payers, expenses, and receipt management through admin interface.

---

## 🚧 Phase 3B: User Experience & Dashboard (IN PROGRESS)

### Current State: Role-Based Access Control
- **Unassigned users**: See onboarding/waiting page indicating they need role assignment from admin
- **Assigned users**: Will see personalized expense tracking dashboard once implemented

### User Experience Components to Implement

#### 1. User Onboarding & Role Assignment Flow
- **Waiting room page**: For users without assigned roles
  - Clear messaging about pending admin approval
  - Contact information or request feature for role assignment
  - Periodic check for role status updates
- **Role notification**: Alert system when user receives role assignment

#### 2. User Dashboard & Core Flows
- **Personal expense dashboard** at `/expenses` or `/dashboard`
  - Recent expenses summary with quick filters
  - Monthly spending overview and trends
  - Quick receipt upload action
  - Pending receipts awaiting approval
  - Personal analytics and spending insights

#### 3. User-Facing Expense Management
- **Simplified expense creation**:
  - Mobile-optimized receipt photo upload
  - Quick expense entry with common suppliers/payers
  - Smart categorization based on history
- **Expense tracking views**:
  - Personal expense history with filters
  - Receipt status tracking (uploaded → approved → posted)
  - Monthly budget tracking and alerts
- **Receipt management**:
  - Direct mobile camera integration
  - Batch receipt upload
  - Receipt status notifications

#### 4. User Analytics & Reporting
- **Personal spending insights**:
  - Category-wise spending breakdowns
  - Monthly/yearly spending trends
  - Top merchants and spending patterns
  - Budget vs actual comparisons
- **Export capabilities**:
  - PDF/CSV export for personal records
  - Tax-ready expense summaries
  - Custom date range reports

#### 5. Mobile-First Design
- **Responsive layouts** optimized for mobile devices
- **Touch-friendly interfaces** for receipt upload
- **Progressive Web App (PWA)** features for native-like experience
- **Offline support** for expense entry and receipt capture

#### 6. User Settings & Preferences
- **Personal profile management**
- **Notification preferences** (receipt status, budget alerts)
- **Default payment methods and suppliers**
- **Currency and display preferences**
- **Data privacy and export settings**

### Technical Implementation Plan
1. **User routes and controllers** at `/expenses/*` and `/dashboard/*`
2. **User-specific API endpoints** with proper authentication/authorization
3. **Mobile-optimized UI components** using DaisyUI responsive classes
4. **Service layer extensions** for user-specific data filtering and aggregation
5. **Background processing** for receipt uploads and notifications

---

## ⏸️ Phase 3C: Advanced User Features (PLANNED)

### Future Enhancements
- **Receipt AI suggestions**: Automatic categorization and tagging
- **Recurring expenses**: Automatic bill tracking and reminders
- **Collaborative expenses**: Shared expense tracking for families/teams
- **Bank integration**: Automatic expense import from banking APIs
- **Advanced analytics**: Predictive spending insights and recommendations

---

## ⏸️ Phase 4: ADE Integration (NOT STARTED)

Will integrate LandingAI ADE for receipt extraction after basic CRUD is working.

---

## 🚧 Phase 5: Reports & Analytics (IN PROGRESS)

### Admin Analytics (Completed)
- Basic admin reporting infrastructure exists in the backend services
- Admin can view expense summaries and basic analytics through admin interface

### User Analytics (Pending - Part of Phase 3B)
- Personal spending insights and reports will be implemented as part of user dashboard
- Tax-ready export capabilities and custom date range reports
- Budget tracking and spending trend analysis for individual users

---

## 🚧 Phase 6: Testing & Polish (IN PROGRESS)

- Backend service integration tests remain in place and passing for core flows.
- Frontend tests now run via `npm run test:cljs` (212 tests, 1,251 assertions) over the refreshed list/detail UI with zero failures.
- Pending: targeted route-level tests and end-to-end coverage for receipts upload + ADE integration when those flows land.

---

## Key Decisions & Changes

### 1. Domain Model Organization
- **Decision**: Support both `domain/models.edn` (flat) and `domain/*/models.edn` (modular)
- **Rationale**: Simpler single-file approach for this domain while maintaining future flexibility
- **Implementation**: Modified `hierarchical_models.clj` to check for direct files first

### 2. Circular Dependency Handling
- **Decision**: Remove `receipts.expense_id` foreign key temporarily
- **Rationale**: Allows both tables to be created in one migration
- **Impact**: Application code must handle referential integrity; FK can be added later

### 3. Currency Default
- **Decision**: Default currency is "BAM" (Bosnia and Herzegovina Convertible Mark)
- **Rationale**: Based on project context and requirements
- **Flexibility**: EUR and USD also supported

---

## Integration Points

- Backend route integration ✅ (admin API mounts expense routes and the services now return supplier/payer metadata consumed by the frontend).
- Frontend route wiring ✅ (expense list + detail pages are reachable via `/admin/expenses` and `/admin/expenses/:id`; the guarded controller loads detail data once `:id` exists and navigation links updated accordingly).

---

## Database Statistics

- **Total Tables**: 15 (6 template + 1 shared + 8 domain)
- **Total Indexes**: 35+
- **Total Enums**: 9 (3 new domain enums)
- **Migration Files**: 11 (0001-0010 + hstore extension)
- **Migration Status**: All applied ✅

---

## Next Session Goals

1. Implement the receipts upload and extraction workflow, including the ADE integration once the basic flow is stable.
2. Finish CRUD editors for suppliers, payers, and expenses (forms, validation, review states) so admins can maintain the catalog.
3. Add targeted route-level tests that cover the expense list → detail navigation plus edge cases around missing `:id` params, and expand end-to-end coverage once receipts and forms exist.
4. Begin Phase 5 by scoping reporting/analytics requirements and prototyping the first dashboards or KPI views.

---

*This document tracks implementation progress for the Home Expenses Tracker domain feature.*
