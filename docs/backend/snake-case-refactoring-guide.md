<!-- ai: {:tags [:backend] :kind :guide} -->

# Snake_case to Kebab-case Refactoring Guide

## Overview

This guide provides a systematic approach for converting snake_case naming violations to kebab-case throughout the codebase, based on our successful refactoring of the integrations and transactions monitoring systems.

## 🎯 Goals

- **Establish naming consistency** across frontend and backend
- **Maintain data integrity** through the entire pipeline
- **Zero breaking changes** to external APIs
- **Comprehensive automated normalization**

## 📋 Pre-Refactoring Checklist

### 1. Identify Violations
```bash
# Use the naming conventions checker to find violations
rg --type cljs ":[a-z_]+_[a-z_]+" src/
rg --type clj ":[a-z_]+_[a-z_]+" src/

# Common patterns to search for:
# Frontend: :snake_case keys in component data access
# Backend: snake_case in return maps (not database columns)
```

### 2. Understand Data Flow
```
Database (snake_case) → Backend Service → Frontend Component
     ↓                      ↓                    ↓
PostgreSQL columns     Return maps         Re-frame data
(keep as-is)          (normalize these)    (use kebab-case)
```

### 3. Backup Current State
```bash
git checkout -b fix/snake-case-violations-NAMESPACE
git add . && git commit -m "backup: before snake_case refactoring"
```

## 🔧 Step-by-Step Refactoring Process

### Phase 1: Backend Service Analysis

#### 1.1 Identify Backend Services
- Look for services in `src/app/backend/services/`
- Focus on services that return data to frontend
- Common patterns: monitoring, admin, reporting services

#### 1.2 Examine Current Return Values
```clojure
;; ❌ BEFORE - Snake_case keys in return maps
(defn get-overview [db]
  (try
    {:integration_events events
     :api_usage usage
     :error_patterns errors}
    (catch Exception e
      {:integration_events []
       :api_usage {}
       :error_patterns []})))
```

#### 1.3 Check for Existing Normalization
```bash
# Search for existing normalization usage
rg "normalize-audit-map" src/app/backend/
rg "audit-service/normalize" src/app/backend/
```

### Phase 2: Backend Service Refactoring

#### 2.1 Add Shared Monitoring Import
```clojure
;; Add to existing require block
[app.backend.services.admin.monitoring.shared :as monitoring-shared]
```

#### 2.2 Replace Error Handling Pattern
```clojure
;; ✅ AFTER - Using shared error handling with normalization
(defn get-overview [db]
  (monitoring-shared/with-monitoring-error-handling
    "get overview"
    (fn [[events usage errors]]
      {:integration-events events    ; Use kebab-case keys
       :api-usage usage
       :error-patterns errors})
    {:integration-events []           ; Error defaults also kebab-case
     :api-usage {}
     :error-patterns []}
    (fn []
      ;; Implementation that returns [events usage errors]
      )))
```

#### 2.3 Alternative: Manual Normalization
If not using shared utilities:
```clojure
(defn get-overview [db]
  (try
    (->> {:integration-events events    ; Use kebab-case in map
          :api-usage usage
          :error-patterns errors}
         (audit-service/normalize-audit-map-recursive))
    (catch Exception e
      (->> {:integration-events []
            :api-usage {}
            :error-patterns []}
           (audit-service/normalize-audit-map-recursive)))))
```

### Phase 3: Frontend Component Analysis

#### 3.1 Identify Frontend Components
```bash
# Find components that access snake_case data
rg --type cljs ":.*_.*" src/app/*/frontend/pages/
rg --type cljs "get-in.*_" src/app/*/frontend/
```

#### 3.2 Map Data Access Patterns
```clojure
;; Common patterns to look for:
(get-in data [:snake_case_key])
(:snake_case_key data)
(:namespace/snake_case_key item)
```

### Phase 4: Frontend Component Refactoring

#### 4.1 Update Top-Level Key Access
```clojure
;; ❌ BEFORE
(:integration_events overview)
(get-in overview [:api_usage :total_calls])

;; ✅ AFTER
(:integration-events overview)
(get-in overview [:api-usage :total-calls])
```

#### 4.2 Handle Database Column Keys
**IMPORTANT**: Database columns and namespaced keys should remain as-is:
```clojure
;; ✅ CORRECT - Database columns remain snake_case
(:users/full_name user)      ; Keep as-is
(:transactions_v2/id tx)     ; Keep as-is
(:total_amount item)         ; Keep as-is (from DB aggregation)

;; ✅ CORRECT - Only structure keys become kebab-case
(:high-value-transactions data)  ; This is a structure key
(:integration-events data)       ; This is a structure key
```

#### 4.3 Update Component Props and Subscriptions
```clojure
;; Update subscription keys if needed
(use-subscribe [:admin/integration-overview])  ; Usually already kebab-case

;; Update component data access
(defui component []
  (let [overview (use-subscribe [:admin/overview])]
    ($ :div
      ;; Update data access to use kebab-case structure keys
      (:integration-events overview)    ; Changed from :integration_events
      (:api-usage overview))))          ; Changed from :api_usage
```

### Phase 5: Testing and Validation

#### 5.1 Test Backend Normalization
```clojure
;; Test in REPL
(require '[app.backend.services.admin.SERVICE :as service] :reload)

;; Test sample data
(let [sample-data {:snake_case_key "value"
                   :nested_data [{:item_key "nested"}]}]
  (audit-service/normalize-audit-map-recursive sample-data))
;; Should return: {:snake-case-key "value" :nested-data [{:item-key "nested"}]}
```

#### 5.2 Test Frontend Data Flow
```bash
# Check that frontend can access normalized data
# Start dev server and check browser console for errors
npm run dev
```

#### 5.3 Run Quality Checks
```bash
bb lint && bb cljfmt-check
# Should return: 0 errors, 0 warnings
```

## 🚨 Common Pitfalls and Solutions

### Pitfall 1: Converting Database Column Names
```clojure
;; ❌ WRONG - Don't change database column names
(:transactions-v2/id tx)     ; This will fail - DB uses snake_case

;; ✅ CORRECT - Keep database columns as-is
(:transactions_v2/id tx)     ; Matches actual DB column names
```

### Pitfall 2: Inconsistent Key Conversion
```clojure
;; ❌ WRONG - Mixing snake_case and kebab-case
{:integration-events events
 :api_usage usage}           ; Inconsistent!

;; ✅ CORRECT - Consistent kebab-case for structure keys
{:integration-events events
 :api-usage usage}
```

### Pitfall 3: Missing Normalization
```clojure
;; ❌ WRONG - Returning raw data without normalization
(defn get-data [db]
  {:snake_case_data (query-db)})

;; ✅ CORRECT - Always normalize return data
(defn get-data [db]
  (->> {:snake-case-data (query-db)}
       (audit-service/normalize-audit-map-recursive)))
```

### Pitfall 4: Breaking External APIs
```clojure
;; ❌ WRONG - Changing public API keys
;; If external services expect snake_case, don't change them

;; ✅ CORRECT - Only change internal data flow
;; Use separate formatters for external vs internal data
```

## 📝 Refactoring Checklist Template

For each namespace:

### Backend Service Checklist
- [ ] Identify all functions that return data to frontend
- [ ] Add monitoring-shared import (if applicable)
- [ ] Replace try-catch with `with-monitoring-error-handling`
- [ ] Convert return map keys to kebab-case
- [ ] Apply `normalize-audit-map-recursive` to results
- [ ] Keep database column names unchanged
- [ ] Test backend functions in REPL

### Frontend Component Checklist
- [ ] Identify all snake_case key access patterns
- [ ] Update structure key access (`:snake_case` → `:kebab-case`)
- [ ] Keep database column keys unchanged (`:table/column_name`)
- [ ] Update get-in paths for nested structure keys
- [ ] Verify component rendering still works
- [ ] Check browser console for errors

### Quality Assurance Checklist
- [ ] All lint checks pass: `bb lint && bb cljfmt-check`
- [ ] No console errors in browser
- [ ] Data displays correctly in UI
- [ ] No breaking changes to external APIs
- [ ] Backend normalization tests pass
- [ ] Frontend data access works correctly

## 🔄 Example: Complete Namespace Refactoring

### Before:
```clojure
;; Backend
(defn get-user-stats [db]
  (try
    {:active_users (query-active-users)
     :user_growth (query-user-growth)
     :top_users (query-top-users)}
    (catch Exception e
      {:active_users 0
       :user_growth []
       :top_users []})))

;; Frontend
(defui user-stats []
  (let [stats (use-subscribe [:admin/user-stats])]
    ($ :div
      (:active_users stats)
      (:user_growth stats))))
```

### After:
```clojure
;; Backend
(defn get-user-stats [db]
  (monitoring-shared/with-monitoring-error-handling
    "get user stats"
    (fn [[active-users user-growth top-users]]
      {:active-users active-users
       :user-growth user-growth
       :top-users top-users})
    {:active-users 0
     :user-growth []
     :top-users []}
    (fn []
      [(query-active-users)
       (query-user-growth)
       (query-top-users)])))

;; Frontend
(defui user-stats []
  (let [stats (use-subscribe [:admin/user-stats])]
    ($ :div
      (:active-users stats)      ; kebab-case structure keys
      (:user-growth stats))))    ; kebab-case structure keys
```

## 📊 Progress Tracking

Create an issue/task for each namespace with this checklist:

```markdown
## Snake_case Refactoring: [NAMESPACE]

### Files to Update:
- [ ] `src/app/backend/services/.../[service].clj`
- [ ] `src/app/[domain]/frontend/pages/[page].cljs`

### Backend Changes:
- [ ] Add shared utilities import
- [ ] Update [function1] return keys
- [ ] Update [function2] return keys
- [ ] Apply normalization to all return values

### Frontend Changes:
- [ ] Update [component1] data access
- [ ] Update [component2] data access
- [ ] Fix any get-in paths

### Testing:
- [ ] Backend REPL tests pass
- [ ] Frontend renders correctly
- [ ] Lint checks pass
- [ ] No console errors
```

## 🎯 Success Metrics

- **0 snake_case violations** in structure keys
- **0 lint warnings** related to naming
- **100% functional** frontend components
- **Consistent data flow** from backend → frontend
- **Maintained compatibility** with database schema

## 🔍 Finding Additional Violations

Use this command to scan for remaining snake_case violations:

```bash
# Search for snake_case in ClojureScript files (frontend)
rg --type cljs ":[a-z_]+_[a-z_]+" src/ | grep -v "_v2\|/full_name\|/subscription_tier"

# Search for snake_case in Clojure files (backend return maps)
rg --type clj ":[a-z_]+_[a-z_]+" src/app/backend/services/ | grep -v "sql\|jdbc\|db"

# Find get-in usage with snake_case
rg --type cljs "get-in.*:.*_" src/

# Find specific violation patterns
rg --type cljs ":.*_.*overview\|:.*_.*status\|:.*_.*data" src/
```

## 🛠 Shared Utilities Reference

### Backend Normalization
```clojure
;; Import the audit service
[app.backend.services.admin.audit :as audit-service]

;; Recursive normalization (recommended)
(audit-service/normalize-audit-map-recursive data)

;; Top-level only normalization
(audit-service/normalize-audit-map data)

;; Shared monitoring utilities
[app.backend.services.admin.monitoring.shared :as monitoring-shared]
(monitoring-shared/with-monitoring-error-handling "operation" success-fn error-data impl-fn)
```

### Frontend Component Patterns
```clojure
;; Import shared monitoring components
[app.admin.frontend.components.shared.monitoring-components :as monitoring]

;; Use shared metric helpers
(monitoring/make-metric "Label" value "Sub-label" :success)
(monitoring/count-metric "Errors" error-count "Last 7 days" {:error-threshold 5})

;; Use shared layout
($ monitoring/monitoring-page-layout
  {:title "Page Title"
   :description "Page description"
   :overview-component overview-card
   :main-components [component1 component2]})
```

This systematic approach ensures consistent, error-free refactoring while maintaining system functionality and establishing sustainable naming conventions across the entire codebase.
