# Home Expenses Tracker — Implementation Plan

> **Document created**: 2025-12-06
> **Based on**: `app-specs/specs.md` requirements
> **Target**: Single-tenant SaaS template (Clojure/ClojureScript, PostgreSQL)
> **Last Updated**: 2025-12-08

---

## Table of Contents

1. [Overview](#overview)
2. [Phase 1: Database Schema & Migrations](#phase-1-database-schema--migrations)
3. [Phase 2: Backend Services & Routes](#phase-2-backend-services--routes)
4. [Phase 3: Frontend Pages & Components](#phase-3-frontend-pages--components)
5. [Phase 4: ADE Integration](#phase-4-ade-integration)
6. [Phase 5: Reports & Analytics](#phase-5-reports--analytics)
7. [Phase 6: Testing & Polish](#phase-6-testing--polish)
8. [Progress Tracking](#progress-tracking)
9. [Risks & Assumptions](#risks--assumptions)

---

## Integration Points

### 1. Backend Routes Integration — ✅ Implemented
**File**: `src/app/backend/routes/admin_api.clj`
**Status**: Domain expenses routes are mounted under `/admin/api/expenses` via `app.domain.expenses.routes.core/routes`.

### 2. Frontend Routes Integration — ⚠️ Pending
**File**: `src/app/admin/frontend/routes.cljs`
**Change**: Add require and merge domain expense routes
```clojure
(require '[app.domain.expenses.frontend.routes :as expense-routes])

;; Merge expense-routes/expense-routes into admin-routes
```

### 3. Frontend Initialization (Optional) — ⚠️ Pending
**File**: `src/app/admin/frontend/core.cljs`
**Change**: Require domain events/subs namespaces for registration
```clojure
(require '[app.domain.expenses.frontend.core :as expenses-domain])
;; Call (expenses-domain/init!) if needed
```

### 4. Navigation Sidebar (Optional) — ⚠️ Pending
**File**: Depends on sidebar implementation
**Change**: Add links to expense domain pages

---

## Overview

### Domain Summary
A household expense tracking application with:
- **Receipt scanning** via LandingAI ADE (Parse → Extract)
- **Manual entry** with autocomplete for suppliers/payers/articles
- **Reports** (weekly/monthly breakdowns by payer/supplier/category/article)
- **Article price comparison** across suppliers

### Architecture Fit (Domain-First Approach)
> ⚠️ **Important**: All new code goes in `domain/` namespaces. Template code should NOT be modified without explicit approval.

- **Database**: `resources/db/domain/models.edn` (new domain models)
- **Backend Services**: `src/app/domain/expenses/services/` (domain services)
- **Backend Routes**: `src/app/domain/expenses/routes/` (domain routes, wired into admin-api)
- **Frontend Pages**: `src/app/domain/expenses/frontend/pages/` (domain pages)
- **Frontend Events/Subs**: `src/app/domain/expenses/frontend/events/` and `subs/`

### Directory Structure (New)
```
src/app/domain/
└── expenses/
    ├── services/
    │   ├── suppliers.clj
    │   ├── payers.clj
    │   ├── receipts.clj
    │   ├── expenses.clj
    │   ├── articles.clj
    │   ├── price_history.clj
    │   └── reports.clj
    ├── routes/
    │   ├── suppliers.clj
    │   ├── payers.clj
    │   ├── receipts.clj
    │   ├── expenses.clj
    │   ├── articles.clj
    │   └── reports.clj
    └── frontend/
        ├── core.cljs          ;; Domain init/bootstrap
        ├── routes.cljs        ;; Domain routes (to merge with admin)
        ├── pages/
        │   ├── inbox.cljs
        │   ├── review_receipt.cljs
        │   ├── expense_form.cljs
        │   ├── expense_list.cljs
        │   ├── suppliers.cljs
        │   ├── payers.cljs
        │   ├── articles.cljs
        │   └── reports.cljs
        ├── events/
        │   ├── receipts.cljs
        │   ├── expenses.cljs
        │   ├── suppliers.cljs
        │   ├── payers.cljs
        │   ├── articles.cljs
        │   └── reports.cljs
        ├── subs/
        │   └── ... (matching events)
        └── components/
            ├── line_items_editor.cljs
            ├── receipt_card.cljs
            ├── supplier_autocomplete.cljs
            └── payer_selector.cljs

resources/db/
└── domain/
    └── models.edn             ;; All expense tracker tables
```

---

## Phase 1: Database Schema & Migrations

### 1.1 New Tables to Add

Create `resources/db/domain/models.edn` with the following tables:

#### Enums (add to `:users` or create separate `:expenses` section)
```clojure
;; Expense domain enums
:types [[:payer-type :enum {:choices ["cash" "card" "account" "person"]}]
        [:receipt-status :enum {:choices ["uploaded" "parsing" "parsed" 
                                          "extracting" "extracted" "review_required" 
                                          "approved" "posted" "failed"]}]
        [:currency :enum {:choices ["BAM" "EUR" "USD"]}]]
```

#### :suppliers
```clojure
:suppliers {:fields [[:id :uuid {:primary-key true}]
                     [:display_name [:varchar 255] {:null false}]
                     [:normalized_key [:varchar 255] {:null false}]
                     [:address :text]
                     [:tax_id [:varchar 50]]
                     [:created_at :timestamptz {:default "NOW()"}]
                     [:updated_at :timestamptz {:default "NOW()"}]]
            :indexes [[:idx_suppliers_normalized_key :btree {:fields [:normalized_key] :unique true}]]}
```

#### :payers
```clojure
:payers {:fields [[:id :uuid {:primary-key true}]
                  [:type [:enum :payer-type] {:null false}]
                  [:label [:varchar 255] {:null false}]
                  [:last4 [:varchar 4]]  ;; For cards
                  [:is_default :boolean {:default false}]
                  [:created_at :timestamptz {:default "NOW()"}]
                  [:updated_at :timestamptz {:default "NOW()"}]]
         :indexes [[:idx_payers_type :btree {:fields [:type]}]]}
```

#### :receipts
```clojure
:receipts {:fields [[:id :uuid {:primary-key true}]
                    [:storage_key :text {:null false}]  ;; S3/local path
                    [:file_hash [:varchar 64] {:null false}]  ;; SHA-256 for dedupe
                    [:original_filename [:varchar 255]]
                    [:content_type [:varchar 100]]
                    [:file_size :integer]
                    [:status [:enum :receipt-status] {:null false :default "uploaded"}]
                    [:raw_parse_json :jsonb]
                    [:raw_extract_json :jsonb]
                    [:parsed_markdown :text]
                    [:supplier_guess [:varchar 255]]
                    [:total_amount_guess [:decimal 12 2]]
                    [:currency_guess [:enum :currency]]
                    [:purchased_at_guess :timestamptz]
                    [:payment_hints :jsonb]  ;; {method: "card", card_last4: "1234"}
                    [:error_message :text]
                    [:error_details :jsonb]
                    [:retry_count :integer {:default 0}]
                    [:expense_id :uuid {:foreign-key :expenses/id :on-delete :set-null}]
                    [:created_at :timestamptz {:default "NOW()"}]
                    [:updated_at :timestamptz {:default "NOW()"}]]
           :indexes [[:idx_receipts_file_hash :btree {:fields [:file_hash] :unique true}]
                     [:idx_receipts_status :btree {:fields [:status]}]
                     [:idx_receipts_created_at :btree {:fields [:created_at]}]]}
```

#### :expenses
```clojure
:expenses {:fields [[:id :uuid {:primary-key true}]
                    [:receipt_id :uuid {:foreign-key :receipts/id :on-delete :set-null}]
                    [:supplier_id :uuid {:foreign-key :suppliers/id :null false}]
                    [:payer_id :uuid {:foreign-key :payers/id :null false}]
                    [:purchased_at :timestamptz {:null false}]
                    [:total_amount [:decimal 12 2] {:null false}]
                    [:currency [:enum :currency] {:null false :default "BAM"}]
                    [:notes :text]
                    [:is_posted :boolean {:default true :null false}]  ;; Draft vs Posted
                    [:deleted_at :timestamptz]  ;; Soft delete
                    [:created_at :timestamptz {:default "NOW()"}]
                    [:updated_at :timestamptz {:default "NOW()"}]]
           :indexes [[:idx_expenses_supplier :btree {:fields [:supplier_id]}]
                     [:idx_expenses_payer :btree {:fields [:payer_id]}]
                     [:idx_expenses_purchased_at :btree {:fields [:purchased_at]}]
                     [:idx_expenses_is_posted :btree {:fields [:is_posted]}]]}
```

#### :expense_items (line items)
```clojure
:expense_items {:fields [[:id :uuid {:primary-key true}]
                         [:expense_id :uuid {:foreign-key :expenses/id :null false :on-delete :cascade}]
                         [:raw_label :text {:null false}]
                         [:article_id :uuid {:foreign-key :articles/id :on-delete :set-null}]
                         [:qty [:decimal 10 3]]
                         [:unit_price [:decimal 12 2]]
                         [:line_total [:decimal 12 2] {:null false}]
                         [:created_at :timestamptz {:default "NOW()"}]]
                :indexes [[:idx_expense_items_expense :btree {:fields [:expense_id]}]
                          [:idx_expense_items_article :btree {:fields [:article_id]}]]}
```

#### :articles (canonical products)
```clojure
:articles {:fields [[:id :uuid {:primary-key true}]
                    [:canonical_name [:varchar 255] {:null false}]
                    [:normalized_key [:varchar 255] {:null false}]
                    [:barcode [:varchar 50]]
                    [:category [:varchar 100]]
                    [:created_at :timestamptz {:default "NOW()"}]
                    [:updated_at :timestamptz {:default "NOW()"}]]
           :indexes [[:idx_articles_normalized_key :btree {:fields [:normalized_key] :unique true}]
                     [:idx_articles_category :btree {:fields [:category]}]]}
```

#### :article_aliases (mapping raw labels → articles)
```clojure
:article_aliases {:fields [[:id :uuid {:primary-key true}]
                           [:supplier_id :uuid {:foreign-key :suppliers/id :on-delete :cascade}]
                           [:raw_label_normalized [:varchar 255] {:null false}]
                           [:article_id :uuid {:foreign-key :articles/id :null false :on-delete :cascade}]
                           [:confidence :integer {:default 100}]  ;; 0-100, 100 = user confirmed
                           [:created_at :timestamptz {:default "NOW()"}]]
                  :indexes [[:idx_article_aliases_supplier_label :btree 
                             {:fields [:supplier_id :raw_label_normalized] :unique true}]
                            [:idx_article_aliases_article :btree {:fields [:article_id]}]]}
```

#### :price_observations (track prices over time)
```clojure
:price_observations {:fields [[:id :uuid {:primary-key true}]
                              [:article_id :uuid {:foreign-key :articles/id :null false :on-delete :cascade}]
                              [:supplier_id :uuid {:foreign-key :suppliers/id :null false :on-delete :cascade}]
                              [:expense_item_id :uuid {:foreign-key :expense_items/id :on-delete :set-null}]
                              [:observed_at :timestamptz {:null false}]
                              [:unit_price [:decimal 12 2]]
                              [:line_total [:decimal 12 2] {:null false}]
                              [:qty [:decimal 10 3]]
                              [:currency [:enum :currency] {:default "BAM"}]
                              [:created_at :timestamptz {:default "NOW()"}]]
                     :indexes [[:idx_price_obs_article :btree {:fields [:article_id]}]
                               [:idx_price_obs_supplier :btree {:fields [:supplier_id]}]
                               [:idx_price_obs_observed_at :btree {:fields [:observed_at]}]]}
```

### 1.2 Migration Steps

```clojure
;; From REPL:
(require '[app.migrations.simple-repl :as mig])

;; 1. Generate migrations (merges template/shared/domain → models.edn)
(mig/make-all-migrations!)

;; 2. Review generated files in resources/db/migrations/

;; 3. Apply migrations
(mig/migrate!)

;; 4. Verify
(mig/status)
```

### 1.3 Checklist
- [ ] Add enums (payer-type, receipt-status, currency) to models.edn
- [ ] Add :suppliers table
- [ ] Add :payers table
- [ ] Add :receipts table
- [ ] Add :expenses table
- [ ] Add :expense_items table
- [ ] Add :articles table
- [ ] Add :article_aliases table
- [ ] Add :price_observations table
- [ ] Run `(mig/make-all-migrations!)`
- [ ] Review generated migration files
- [ ] Run `(mig/migrate!)` on dev database
- [ ] Verify tables created correctly

---

## Phase 2: Backend Services & Routes

### 2.1 Service Namespaces

Create under `src/app/domain/expenses/services/`:

```
src/app/domain/expenses/services/
├── suppliers.clj      ;; Supplier CRUD + normalization
├── payers.clj         ;; Payer CRUD
├── receipts.clj       ;; Upload, status transitions, dedupe
├── expenses.clj       ;; Expense CRUD + posting from receipts
├── articles.clj       ;; Article CRUD + alias management
├── price_history.clj  ;; Price observations + comparisons
└── reports.clj        ;; Report aggregations
```

### 2.2 Key Service Functions

#### `app.domain.expenses.services.suppliers`
```clojure
(defn normalize-supplier-key [name] ...)
(defn create-supplier! [db data] ...)
(defn get-supplier [db id] ...)
(defn list-suppliers [db opts] ...)
(defn find-or-create-supplier! [db name] ...)
(defn search-suppliers [db query] ...)  ;; For autocomplete
```

#### `app.domain.expenses.services.payers`
```clojure
(defn create-payer! [db data] ...)
(defn list-payers [db] ...)
(defn get-default-payer [db type] ...)
(defn suggest-payer [db payment-hints] ...)  ;; From ADE hints
```

#### `app.domain.expenses.services.receipts`
```clojure
(defn upload-receipt! [db file-data] ...)
(defn compute-file-hash [bytes] ...)
(defn check-duplicate [db file-hash] ...)
(defn update-status! [db id new-status] ...)
(defn store-extraction-results! [db id results] ...)
(defn get-receipt-for-review [db id] ...)
(defn list-receipts [db opts] ...)  ;; With status filter
(defn approve-and-post! [db id review-data] ...)
(defn retry-extraction! [db id] ...)
```

#### `app.domain.expenses.services.expenses`
```clojure
(defn create-expense! [db expense-data items] ...)
(defn create-from-receipt! [db receipt-id review-data] ...)
(defn update-expense! [db id updates] ...)
(defn soft-delete-expense! [db id] ...)
(defn list-expenses [db opts] ...)
(defn get-expense-with-items [db id] ...)
```

#### `app.domain.expenses.services.articles`
```clojure
(defn normalize-article-key [name] ...)
(defn create-article! [db data] ...)
(defn find-article-by-alias [db supplier-id raw-label] ...)
(defn create-alias! [db supplier-id raw-label article-id] ...)
(defn list-unmapped-items [db] ...)  ;; Items without article_id
(defn map-item-to-article! [db item-id article-id create-alias?] ...)
```

#### `app.domain.expenses.services.price-history`
```clojure
(defn record-observation! [db expense-item] ...)
(defn get-price-history [db article-id opts] ...)
(defn get-price-comparison [db article-id opts] ...)  ;; Across suppliers
(defn get-latest-prices [db article-id] ...)  ;; Per supplier
```

#### `app.domain.expenses.services.reports`
```clojure
(defn get-summary [db date-range] ...)
(defn get-breakdown-by-payer [db date-range] ...)
(defn get-breakdown-by-supplier [db date-range] ...)
(defn get-weekly-totals [db year] ...)
(defn get-monthly-totals [db year] ...)
(defn get-top-suppliers [db date-range limit] ...)
(defn get-spending-trend [db date-range granularity] ...)
```

### 2.3 Route Namespaces

Create under `src/app/domain/expenses/routes/`:

```
src/app/domain/expenses/routes/
├── core.clj           ;; Combines all domain routes
├── suppliers.clj
├── payers.clj
├── receipts.clj
├── expenses.clj
├── articles.clj
└── reports.clj
```

> **Integration Note**: The domain routes will be composed in `core.clj` and then require minimal wiring into the admin-api (approval needed for that change).

### 2.4 API Endpoints

All under `/admin/api/`:

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| **Suppliers** |
| GET | `/suppliers` | `list-suppliers` | List with search/pagination |
| POST | `/suppliers` | `create-supplier` | Create new supplier |
| GET | `/suppliers/:id` | `get-supplier` | Get single supplier |
| PUT | `/suppliers/:id` | `update-supplier` | Update supplier |
| GET | `/suppliers/search` | `search-suppliers` | Autocomplete endpoint |
| **Payers** |
| GET | `/payers` | `list-payers` | List all payers |
| POST | `/payers` | `create-payer` | Create new payer |
| PUT | `/payers/:id` | `update-payer` | Update payer |
| DELETE | `/payers/:id` | `delete-payer` | Delete payer |
| **Receipts** |
| POST | `/receipts` | `upload-receipt` | Multipart upload |
| GET | `/receipts` | `list-receipts` | List with status filter |
| GET | `/receipts/:id` | `get-receipt` | Get with extraction data |
| POST | `/receipts/:id/retry` | `retry-extraction` | Retry ADE extraction |
| POST | `/receipts/:id/approve` | `approve-receipt` | Create expense from receipt |
| DELETE | `/receipts/:id` | `delete-receipt` | Delete receipt |
| **Expenses** |
| GET | `/expenses` | `list-expenses` | List with filters |
| POST | `/expenses` | `create-expense` | Manual expense creation |
| GET | `/expenses/:id` | `get-expense` | Get with line items |
| PUT | `/expenses/:id` | `update-expense` | Update expense |
| DELETE | `/expenses/:id` | `delete-expense` | Soft delete |
| **Articles** |
| GET | `/articles` | `list-articles` | List with search |
| POST | `/articles` | `create-article` | Create article |
| PUT | `/articles/:id` | `update-article` | Update article |
| GET | `/articles/unmapped` | `list-unmapped` | Unmapped items queue |
| POST | `/articles/aliases` | `create-alias` | Map raw_label → article |
| GET | `/articles/:id/prices` | `get-price-history` | Price comparison |
| **Reports** |
| GET | `/reports/summary` | `get-summary` | Dashboard summary |
| GET | `/reports/by-payer` | `get-by-payer` | Payer breakdown |
| GET | `/reports/by-supplier` | `get-by-supplier` | Supplier breakdown |
| GET | `/reports/trends` | `get-trends` | Spending trends |

### 2.5 DI Wiring

> ⚠️ **Requires Approval**: This section requires modifying template code (`src/app/template/di/config.clj`).

Option A (minimal template change): Add a single line to include domain routes
```clojure
;; In admin-api.clj, add:
(require '[app.domain.expenses.routes.core :as domain-expenses])
;; And merge domain-expenses/routes into the admin routes
```

Option B: Create domain-specific DI in `src/app/domain/expenses/di.clj`:
```clojure
(ns app.domain.expenses.di
  "Domain DI components - consumed by template di/config.clj"
  (:require [com.stuartsierra.component :as component]))

(defn domain-components
  "Returns map of domain component definitions"
  [system-config]
  {:expense-supplier-service (->SupplierService)
   :expense-payer-service (->PayerService)
   :receipt-service (->ReceiptService)
   :expense-service (->ExpenseService)
   :article-service (->ArticleService)
   :report-service (->ReportService)})
```

### 2.6 Checklist
- [ ] Create `src/app/domain/expenses/` directory structure
- [ ] Implement suppliers.clj service
- [ ] Implement payers.clj service
- [ ] Implement receipts.clj service
- [ ] Implement expenses.clj service
- [ ] Implement articles.clj service
- [ ] Implement price_history.clj service
- [ ] Implement reports.clj service
- [ ] Create domain routes core.clj
- [ ] Wire all route handlers
- [ ] **[APPROVAL NEEDED]** Update admin-api.clj to include domain routes
- [ ] Test all endpoints via curl/Postman

---

## Phase 3: Frontend Pages & Components

### 3.1 Page Structure

Create under `src/app/domain/expenses/frontend/pages/`:

```
src/app/domain/expenses/frontend/pages/
├── inbox.cljs          ;; Receipt inbox (list + status)
├── review_receipt.cljs ;; Receipt review + approval
├── expense_form.cljs   ;; Manual expense creation/edit
├── expense_list.cljs   ;; Expense history list
├── suppliers.cljs      ;; Supplier management
├── payers.cljs         ;; Payer management
├── articles.cljs       ;; Article management + unmapped queue
└── reports.cljs        ;; Reports dashboard
```

### 3.2 Routes to Add

Create `src/app/domain/expenses/frontend/routes.cljs`:

> ⚠️ **Integration Note**: These routes need to be merged into the admin router. This requires a small change to `src/app/admin/frontend/routes.cljs` (approval needed).

```clojure
(ns app.domain.expenses.frontend.routes
  "Expense domain routes - to be merged into admin routes"
  (:require
    [app.domain.expenses.frontend.pages.inbox :as inbox]
    [app.domain.expenses.frontend.pages.review-receipt :as review-receipt]
    [app.domain.expenses.frontend.pages.expense-form :as expense-form]
    [app.domain.expenses.frontend.pages.expense-list :as expense-list]
    [app.domain.expenses.frontend.pages.suppliers :as suppliers]
    [app.domain.expenses.frontend.pages.payers :as payers]
    [app.domain.expenses.frontend.pages.articles :as articles]
    [app.domain.expenses.frontend.pages.reports :as reports]
    [re-frame.core :as rf]))

(def expense-routes
  "Domain routes (to merge under /admin)"
;; Expenses domain routes
["/expenses"
 {:name :admin-expense-list
  :view expense-list/admin-expense-list-page
  :controllers [(guarded-start [:expenses/load-list])]}]

["/expenses/new"
 {:name :admin-expense-new
  :view expense-form/admin-expense-form-page
  :controllers [(guarded-start [:expenses/init-new])]}]

["/expenses/:id"
 {:name :admin-expense-detail
  :view expense-form/admin-expense-form-page
  :controllers [(fn [params] ...)]}]

["/receipts"
 {:name :admin-receipt-inbox
  :view inbox/admin-receipt-inbox-page
  :controllers [(guarded-start [:receipts/load-inbox])]}]

["/receipts/:id/review"
 {:name :admin-receipt-review
  :view review-receipt/admin-review-receipt-page
  :controllers [(fn [params] ...)]}]

["/suppliers"
 {:name :admin-suppliers
  :view suppliers/admin-suppliers-page
  :controllers [(guarded-start [:suppliers/load-list])]}]

["/payers"
 {:name :admin-payers
  :view payers/admin-payers-page
  :controllers [(guarded-start [:payers/load-list])]}]

["/articles"
 {:name :admin-articles
  :view articles/admin-articles-page
  :controllers [(guarded-start [:articles/load-list])]}]

["/articles/unmapped"
 {:name :admin-unmapped-articles
  :view articles/admin-unmapped-items-page
  :controllers [(guarded-start [:articles/load-unmapped])]}]

["/reports"
 {:name :admin-reports
  :view reports/admin-reports-page
  :controllers [(guarded-start [:reports/load-summary])]}]
```

### 3.3 Events & Subs

Create under `src/app/domain/expenses/frontend/events/`:

```
src/app/domain/expenses/frontend/events/
├── receipts.cljs   ;; Upload, list, approve events
├── expenses.cljs   ;; CRUD events
├── suppliers.cljs  ;; Supplier CRUD + search
├── payers.cljs     ;; Payer CRUD
├── articles.cljs   ;; Article CRUD + alias mapping
└── reports.cljs    ;; Report loading events
```

Create subscriptions under `src/app/domain/expenses/frontend/subs/`:

```
src/app/domain/expenses/frontend/subs/
├── receipts.cljs
├── expenses.cljs
├── suppliers.cljs
├── payers.cljs
├── articles.cljs
└── reports.cljs
```

### 3.4 Key UI Components

Reuse from `src/app/template/frontend/components/`:
- `list.cljs` — Generic list with pagination/filtering
- `form.cljs` — Form templates
- `modal.cljs` — Modals for quick actions
- `table.cljs` — Data tables
- `cards.cljs` — Summary cards for dashboard
- `filter.cljs` — Date range/status filters

Create new components under `src/app/domain/expenses/frontend/components/`:

```clojure
;; line-items-editor.cljs — Editable grid for expense items
(defn line-items-editor [{:keys [items on-add on-update on-remove]}]
  ...)

;; receipt-card.cljs — Receipt preview with status chip
(defn receipt-card [{:keys [receipt on-click]}]
  ...)

;; supplier-autocomplete.cljs — Autocomplete with create option
(defn supplier-autocomplete [{:keys [value on-change on-create]}]
  ...)

;; payer-selector.cljs — Payer dropdown with type icons
(defn payer-selector [{:keys [value on-change suggestion]}]
  ...)

;; price-chart.cljs — Price history chart (optional)
(defn price-chart [{:keys [data]}]
  ...)
```

### 3.5 File Upload Handling

For receipt upload, use multipart form submission:

```clojure
;; In events/expenses/receipts.cljs
(rf/reg-event-fx
  :receipts/upload
  (fn [{:keys [db]} [_ file]]
    {:http-xhrio {:method :post
                  :uri "/admin/api/receipts"
                  :body (doto (js/FormData.)
                          (.append "file" file))
                  :format (ajax/text-request-format)  ;; Don't JSON encode
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:receipts/upload-success]
                  :on-failure [:receipts/upload-failure]}}))
```

### 3.6 Polling Strategy for Receipt Status

```clojure
;; Poll receipt status while processing
(rf/reg-event-fx
  :receipts/start-polling
  (fn [{:keys [db]} [_ receipt-id]]
    {:db (assoc-in db [:receipts :polling receipt-id] true)
     :dispatch-later [{:ms 2000 :dispatch [:receipts/poll-status receipt-id]}]}))

(rf/reg-event-fx
  :receipts/poll-status
  (fn [{:keys [db]} [_ receipt-id]]
    (when (get-in db [:receipts :polling receipt-id])
      {:http-xhrio {:method :get
                    :uri (str "/admin/api/receipts/" receipt-id)
                    :on-success [:receipts/poll-success receipt-id]
                    :on-failure [:receipts/poll-failure receipt-id]}})))

(rf/reg-event-fx
  :receipts/poll-success
  (fn [{:keys [db]} [_ receipt-id response]]
    (let [status (get-in response [:data :status])
          terminal? #{"extracted" "review_required" "approved" "posted" "failed"}]
      (if (terminal? status)
        {:db (-> db 
                 (assoc-in [:receipts :items receipt-id] (:data response))
                 (update-in [:receipts :polling] dissoc receipt-id))}
        {:db (assoc-in db [:receipts :items receipt-id] (:data response))
         :dispatch-later [{:ms 2000 :dispatch [:receipts/poll-status receipt-id]}]}))))
```

### 3.7 Checklist
- [ ] Create `src/app/domain/expenses/frontend/` directory structure
- [ ] Implement inbox.cljs page
- [ ] Implement review_receipt.cljs page
- [ ] Implement expense_form.cljs page
- [ ] Implement expense_list.cljs page
- [ ] Implement suppliers.cljs page
- [ ] Implement payers.cljs page
- [ ] Implement articles.cljs page
- [ ] Implement reports.cljs page
- [ ] Create domain frontend events namespaces
- [ ] Create domain frontend subs namespaces
- [ ] Create domain expense components
- [ ] Create domain routes.cljs
- [ ] **[APPROVAL NEEDED]** Update admin routes.cljs to merge domain routes
- [ ] **[APPROVAL NEEDED]** Add navigation links to sidebar
- [ ] Test all pages in browser

---

## Phase 4: ADE Integration

### 4.1 ADE Client Service

Create `src/app/backend/services/ade.clj`:

```clojure
(ns app.backend.services.ade
  "LandingAI ADE client for receipt parsing and extraction"
  (:require
    [clj-http.client :as http]
    [cheshire.core :as json]
    [app.shared.config :as config]
    [taoensso.timbre :as log]))

(def extraction-schema
  ;; Load from app-specs/ade-schema.md JSON
  ...)

(defn parse-document
  "Parse document into markdown (optional step)"
  [file-bytes content-type]
  ...)

(defn extract-receipt
  "Extract structured data from receipt image"
  [file-bytes content-type]
  ...)

(defn process-receipt!
  "Full pipeline: upload → parse (optional) → extract"
  [db receipt-id file-bytes content-type]
  ...)
```

### 4.2 Background Processing

Create `src/app/backend/jobs/receipt_processing.clj`:

```clojure
(ns app.backend.jobs.receipt-processing
  "Background job for ADE processing"
  (:require
    [app.backend.services.ade :as ade]
    [app.backend.services.expenses.receipts :as receipts]
    [taoensso.timbre :as log]))

(defn process-pending-receipts!
  "Process receipts with status 'uploaded' or retry failed"
  [db]
  (let [pending (receipts/list-pending-for-processing db)]
    (doseq [receipt pending]
      (try
        (receipts/update-status! db (:id receipt) "parsing")
        (let [result (ade/process-receipt! db (:id receipt) 
                                           (:file-bytes receipt)
                                           (:content-type receipt))]
          (receipts/store-extraction-results! db (:id receipt) result)
          (receipts/update-status! db (:id receipt) 
                                   (if (:needs-review? result)
                                     "review_required"
                                     "extracted")))
        (catch Exception e
          (log/error e "Failed to process receipt" (:id receipt))
          (receipts/mark-failed! db (:id receipt) (ex-message e)))))))
```

### 4.3 Retry Strategy

```clojure
(def max-retries 3)
(def retry-delays [5000 30000 120000])  ;; 5s, 30s, 2min

(defn should-retry? [receipt]
  (< (:retry-count receipt) max-retries))

(defn schedule-retry! [db receipt-id retry-count]
  (let [delay-ms (get retry-delays retry-count 120000)]
    ;; Schedule via core.async or job queue
    (async/go
      (async/<! (async/timeout delay-ms))
      (process-receipt-with-retry! db receipt-id))))
```

### 4.4 Config Requirements

Add to `resources/config.edn`:

```clojure
{:ade {:api-key #env ADE_API_KEY
       :base-url "https://api.landing.ai/v1/agent/extract"
       :timeout-ms 60000
       :max-retries 3}
 
 :storage {:type :local  ;; or :s3
           :local-path "uploads/receipts"
           :s3-bucket #env RECEIPTS_BUCKET
           :s3-region "eu-central-1"}}
```

### 4.5 Checklist
- [ ] Create ade.clj service
- [ ] Implement parse-document function
- [ ] Implement extract-receipt function
- [ ] Create receipt_processing.clj job
- [ ] Add retry logic with exponential backoff
- [ ] Add ADE config to config.edn
- [ ] Set up local file storage for dev
- [ ] Test extraction with sample receipts
- [ ] Handle various error cases (timeout, rate limit, malformed response)

---

## Phase 5: Reports & Analytics

### 5.1 SQL Queries for Reports

#### Dashboard Summary
```sql
SELECT 
  SUM(total_amount) FILTER (WHERE purchased_at >= date_trunc('week', NOW())) as this_week_total,
  SUM(total_amount) FILTER (WHERE purchased_at >= date_trunc('month', NOW())) as this_month_total,
  COUNT(*) FILTER (WHERE purchased_at >= date_trunc('month', NOW())) as this_month_count
FROM expenses
WHERE is_posted = true AND deleted_at IS NULL;
```

#### Top Suppliers (Month)
```sql
SELECT 
  s.id,
  s.display_name,
  SUM(e.total_amount) as total_spent,
  COUNT(*) as expense_count
FROM expenses e
JOIN suppliers s ON e.supplier_id = s.id
WHERE e.is_posted = true 
  AND e.deleted_at IS NULL
  AND e.purchased_at >= date_trunc('month', NOW())
GROUP BY s.id, s.display_name
ORDER BY total_spent DESC
LIMIT 5;
```

#### Payer Breakdown
```sql
SELECT 
  p.id,
  p.type,
  p.label,
  SUM(e.total_amount) as total_spent,
  COUNT(*) as expense_count
FROM expenses e
JOIN payers p ON e.payer_id = p.id
WHERE e.is_posted = true 
  AND e.deleted_at IS NULL
  AND e.purchased_at BETWEEN :from AND :to
GROUP BY p.id, p.type, p.label
ORDER BY total_spent DESC;
```

#### Weekly Totals (ISO week, Monday start)
```sql
SELECT 
  date_trunc('week', purchased_at) as week_start,
  EXTRACT(ISOYEAR FROM purchased_at) as year,
  EXTRACT(WEEK FROM purchased_at) as week_num,
  SUM(total_amount) as total
FROM expenses
WHERE is_posted = true 
  AND deleted_at IS NULL
  AND purchased_at >= :from
GROUP BY 1, 2, 3
ORDER BY 1;
```

#### Article Price Comparison
```sql
SELECT 
  s.id as supplier_id,
  s.display_name as supplier_name,
  po.observed_at,
  po.unit_price,
  po.line_total,
  po.qty
FROM price_observations po
JOIN suppliers s ON po.supplier_id = s.id
WHERE po.article_id = :article-id
  AND po.observed_at >= :from
ORDER BY po.observed_at DESC;
```

### 5.2 Report Page Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Reports Dashboard                               [Week ▼] [Month]│
├─────────────────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│ │ This Week│ │This Month│ │  Trend   │                          │
│ │  $450.00 │ │ $1,230.00│ │   📈 +5% │                          │
│ └──────────┘ └──────────┘ └──────────┘                          │
├─────────────────────────────────────────────────────────────────┤
│ Top Suppliers                    │ Spending by Payer            │
│ ┌─────────────────────────────┐  │ ┌─────────────────────────┐  │
│ │ 1. Bingo         $320.00    │  │ │ ████████░░ Card 65%     │  │
│ │ 2. Konzum        $210.00    │  │ │ ████░░░░░░ Cash 25%     │  │
│ │ 3. Apoteka       $85.00     │  │ │ ██░░░░░░░░ Account 10%  │  │
│ └─────────────────────────────┘  │ └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│ Weekly Trend                                                    │
│ [─────Chart showing last 8 weeks──────────────────────────────] │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 Checklist
- [ ] Implement summary query in reports.clj
- [ ] Implement payer breakdown query
- [ ] Implement supplier breakdown query
- [ ] Implement weekly/monthly totals
- [ ] Implement price comparison query
- [ ] Create reports page component
- [ ] Add summary cards
- [ ] Add breakdown tables
- [ ] Add date range picker
- [ ] Optional: Add trend charts (use D3 or Chart.js)

---

## Phase 6: Testing & Polish

### 6.1 Backend Tests

Create under `test/app/domain/expenses/services/`:

```clojure
;; suppliers_test.clj
(deftest normalize-supplier-key-test
  (testing "Normalizes supplier names correctly"
    (is (= "bingo" (sut/normalize-supplier-key "  BINGO  ")))
    (is (= "bingo-centar" (sut/normalize-supplier-key "Bingo Centar")))
    (is (= "dm-drogerie" (sut/normalize-supplier-key "DM Drogerie!")))))

;; receipts_test.clj
(deftest check-duplicate-test
  (testing "Detects duplicate receipts by file hash"
    ...))

(deftest approve-and-post-test
  (testing "Creates expense and items from approved receipt"
    ...))

;; reports_test.clj
(deftest weekly-totals-test
  (testing "Weeks start on Monday (ISO week)"
    ...))
```

### 6.2 Frontend Tests

```clojure
;; test/app/domain/expenses/frontend/events_test.cljs
(deftest expense-form-validation-test
  (testing "Validates required fields"
    ...))

(deftest line-items-total-test
  (testing "Calculates total from line items"
    ...))
```

### 6.3 Integration Tests

```clojure
;; test/app/domain/expenses/integration_test.clj
(deftest full-receipt-workflow-test
  (testing "Upload → Extract → Review → Post"
    (let [file (io/file "test/fixtures/receipt.jpg")
          upload-result (receipts/upload-receipt! db file)
          ;; Simulate ADE extraction
          _ (receipts/store-extraction-results! db (:id upload-result) mock-extraction)
          ;; Approve
          expense (receipts/approve-and-post! db (:id upload-result) 
                                              {:supplier-id supplier-id
                                               :payer-id payer-id
                                               :purchased-at (java.time.Instant/now)
                                               :items [...]})]
      (is (some? (:id expense)))
      (is (= "posted" (-> (receipts/get-receipt db (:id upload-result)) :status))))))
```

### 6.4 Test Fixtures

Create sample data in `test/fixtures/`:
- `receipt.jpg` — Sample receipt image
- `mock_extraction.edn` — Mock ADE response
- `seed_data.clj` — Functions to seed test data

### 6.5 Polish Items

- [ ] Add loading states to all async operations
- [ ] Add error handling with user-friendly messages
- [ ] Add form validation feedback
- [ ] Add keyboard shortcuts (e.g., Enter to submit)
- [ ] Add mobile-responsive layout for key pages
- [ ] Add bulk actions (delete multiple expenses)
- [ ] Add export to CSV for expenses
- [ ] Add print-friendly receipt view

### 6.6 Checklist
- [ ] Write supplier service tests
- [ ] Write receipt service tests
- [ ] Write expense service tests
- [ ] Write report service tests
- [ ] Write integration tests
- [ ] Create test fixtures
- [ ] Run `bb be-test` — all pass
- [ ] Run `bb fe-test` — all pass
- [ ] Manual testing of full workflow
- [ ] Fix edge cases found in testing

---

## Progress Tracking

### Phase 1: Database Schema
| Task | Status | Notes |
|------|--------|-------|
| Add expense domain enums | ✅ Done | Defined in `resources/db/domain/models.edn`, included in migration `0010_schema.edn` |
| Add suppliers table | ✅ Done | In domain models; generated into migration 0010 |
| Add payers table | ✅ Done | In domain models; generated into migration 0010 |
| Add receipts table | ✅ Done | In domain models; generated into migration 0010 |
| Add expenses table | ✅ Done | In domain models; generated into migration 0010 |
| Add expense_items table | ✅ Done | In domain models; generated into migration 0010 |
| Add articles table | ✅ Done | In domain models; generated into migration 0010 |
| Add article_aliases table | ✅ Done | In domain models; generated into migration 0010 |
| Add price_observations table | ✅ Done | In domain models; generated into migration 0010 |
| Generate migrations | ✅ Done | `resources/db/migrations/0010_schema.edn` created |
| Apply migrations | ✅ Done | Applied through migration 0010 |

### Phase 2: Backend Services & Routes
| Task | Status | Notes |
|------|--------|-------|
| Suppliers service | ✅ Done | CRUD + normalization (`src/app/domain/expenses/services/suppliers.clj`) |
| Payers service | ✅ Done | CRUD + defaults/suggestions (`src/app/domain/expenses/services/payers.clj`) |
| Receipts service | ✅ Done | Upload/dedupe, status, approve/post (`services/receipts.clj`) |
| Expenses service | ✅ Done | Expense + line items + price logging (`services/expenses.clj`) |
| Articles service | ✅ Done | Canonical articles, aliases, unmapped queue (`services/articles.clj`) |
| Price history service | ✅ Done | Record/query observations (`services/price_history.clj`) |
| Reports service | ✅ Done | Aggregate queries (`services/reports.clj`) |
| Admin routes wiring | ✅ Done | Domain routes mounted at `/admin/api/expenses` |
| DI configuration | ⬜ Not Started | Needs domain components wiring |

### Phase 3: Frontend
| Task | Status | Notes |
|------|--------|-------|
| Receipt inbox page | ⬜ Not Started | |
| Receipt review page | ⬜ Not Started | |
| Expense form page | ⬜ Not Started | |
| Expense list page | ⬜ Not Started | |
| Suppliers page | ⬜ Not Started | |
| Payers page | ⬜ Not Started | |
| Articles page | ⬜ Not Started | |
| Reports dashboard | ⬜ Not Started | |
| Events & Subs | ⬜ Not Started | |
| Navigation integration | ⬜ Not Started | |

### Phase 4: ADE Integration
| Task | Status | Notes |
|------|--------|-------|
| ADE client service | ⬜ Not Started | |
| Background processing | ⬜ Not Started | |
| Retry logic | ⬜ Not Started | |
| File storage | ⬜ Not Started | |

### Phase 5: Reports
| Task | Status | Notes |
|------|--------|-------|
| Summary queries | ✅ Implemented | See `services/reports.clj` |
| Breakdown queries | ✅ Implemented | Supplier/payer breakdowns in `services/reports.clj` |
| Price comparison | ✅ Implemented | Price history queries cover comparison (`services/price_history.clj`) |
| Reports UI | ⬜ Not Started | |

### Phase 6: Testing
| Task | Status | Notes |
|------|--------|-------|
| Backend unit/integration tests | 🚧 In Progress | Core service integration suite passing (`expenses-services-test`) |
| Frontend tests | ⬜ Not Started | |
| Integration tests (e2e) | ⬜ Not Started | |
| Manual testing | ⬜ Not Started | |

---

## Risks & Assumptions

### Assumptions
1. **Single user/household** — No multi-tenancy or user isolation needed
2. **Week starts Monday** — ISO week for all weekly reports
3. **Currency is BAM** — Default currency, with optional EUR/USD
4. **ADE API available** — LandingAI service is accessible
5. **Local storage for dev** — Will use S3-compatible for production

### Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| ADE extraction unreliable | High | Implement manual fallback, store raw JSON for reprocessing |
| Duplicate receipts | Medium | SHA-256 file hash check before processing |
| Article normalization complexity | Medium | Start with alias-based mapping, add fuzzy match later |
| Large receipt images | Low | Compress on upload, set max file size |
| Price comparison data gaps | Low | Handle missing qty/unit_price gracefully |

### Edge Cases to Handle
1. Receipts without line items — Allow posting with just total
2. Negative line items (returns) — Support negative amounts
3. Missing purchase date — Require user input before posting
4. Duplicate supplier names — Merge/link UI for admins
5. Unmapped articles accumulating — Regular cleanup queue

---

## Next Steps

1. **Phase 3** — Frontend: wire routes, build suppliers/payers/expenses/receipts/pages + nav links.
2. **Phase 3** — Hook re-frame events/subs, form validation, list/inbox UIs.
3. **Phase 6** — Add backend route tests and expand service tests (reports, edge cases).
4. **Phase 4** — ADE integration after frontend scaffolding is in place.
5. **Phase 5** — Reports UI and price comparison views.

---

*Document will be updated as implementation progresses.*
