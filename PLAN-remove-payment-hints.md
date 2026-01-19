# Plan: Remove `payment_hints` from Receipts

## Overview
The `payment_hints` column in the receipts table is no longer needed. This plan outlines all the code locations that need to be modified to completely remove this feature.

**Date Created**: 2025-01-06
**Status**: Planning Phase

---

## 🎯 Phase 1: Backend Changes

### 1.1 Database Models (2 files)

**File: `resources/db/models.edn:222`**
- Remove the `[:payment_hints :jsonb]` column definition from the receipts table

**File: `resources/db/domain/models.edn:69`**
- Remove the `[:payment_hints :jsonb]` column definition from the domain model

### 1.2 Receipt Services (`src/app/domain/backend/expenses/services/receipts.clj`)

**Locations: Lines 272, 291, 304, 314**

**Function: `reset-for-ocr!` (lines 263-296)**
- Remove `payment_hints` from the docstring list (line 272)
- Remove `:payment_hints nil` from the UPDATE set map (line 291)

**Function: `store-extraction-results!` (lines 298-321)**
- Remove `payment_hints` from the function's destructuring keys (line 304)
- Remove the conditional assoc for `:payment_hints` (line 314):
  ```clojure
  (contains? data :payment_hints) (assoc :payment_hints (jsonb-value payment_hints))
  ```

### 1.3 Receipt OCR Worker (`src/app/domain/backend/expenses/workers/receipt_ocr.clj`)

**Locations: Lines 36-39, 144, 150-151, 157, 212**

**Function: `extraction-result-schema` (lines 27-44)**
- Remove the entire `payment_hints` schema definition (lines 36-39):
  ```clojure
  [:payment_hints {:optional true}
   [:maybe [:map {:closed false}
            [:method {:optional true} [:maybe string?]]
            [:card_last4 {:optional true} [:maybe string?]]]]]
  ```

**Function: `extraction->guesses` (lines 143-158)**
- Remove `payment_hints` from the destructuring keys (line 144)
- Remove the payment extraction logic (lines 150-151):
  ```clojure
  payment (when (map? payment_hints)
            (select-keys payment_hints [:method :card_last4]))
  ```
- Remove `:payment_hints payment` from the return map (line 157)

**Function: `process-receipt!` (line 212)**
- Remove any reference to `:payment_hints` in the results map

### 1.4 Mistral OCR Integration (`src/app/domain/backend/expenses/integrations/mistral_ocr.clj`)

**Location: Lines 62-67**

**Function: `extraction-schema`**
- Remove the `payment_hints` property definition from the JSON schema:
  ```clojure
  "payment_hints" {"type" ["object" "null"]
                   "description" "Optional hints about the payment method."
                   "properties" {"method" {"type" ["string" "null"]
                                           "description" "cash|card|account|person|unknown"}
                                 "card_last4" {"type" ["string" "null"]
                                               "description" "Last 4 digits if present."}}}
  ```

---

## 🎨 Phase 2: Frontend Changes

### 2.1 Receipt Viewer Component (`src/app/domain/frontend/expenses/components/receipt_viewer.cljs`)

**Locations: Lines 48, 132-136**

**Component: `receipt-viewer`**
- Remove `payment-hints` from the destructuring keys (line 48)
- Remove the entire payment hints display section (lines 132-136):
  ```clojure
  (when (seq payment-hints)
    ($ json-display-card
      {:title "Payment Hints"
       :json-value payment-hints
       :max-height "max-h-80"}))
  ```

---

## 📚 Phase 3: Documentation Updates (Optional)

The following documentation files reference `payment_hints`. Consider updating them to reflect the removal:

1. `app-specs/ade-schema.md` - Lines 25-26, 35, 76, 124
2. `app-specs/home-expenses-tracker-plan.md` - Lines 180, 338
3. `app-specs/specs.md` - Line 351
4. `docs/domain/expenses/index.md` - Line 233
5. `PLAN-mistral-ocr-pos-receipts.md` - Lines 179, 214, 380
6. `resources/db/migrations/0010_schema.edn` - Line 26 (migration file - historical)

---

## ⚠️ Phase 4: Database Migration (After Code Changes)

**IMPORTANT**: This requires a database migration to drop the column:

1. Create a new migration file (e.g., `0011_remove_payment_hints.edn`)
2. Add a down migration to re-add the column if rollback is needed
3. Follow the project's migration workflow (edit canonical EDN → run REPL helpers)

---

## ✅ Verification Steps

After completing all phases:

1. **Backend**: Run `bb be-test` to ensure no tests reference `payment_hints`
2. **Frontend**: Run `npm run test:cljs` to ensure no frontend tests break
3. **Manual Testing**:
   - Upload a new receipt and verify OCR processes without errors
   - Check the receipt viewer displays correctly without payment hints section
4. **System Logs**: Use the `system-logs` skill to check for any runtime errors

---

## Summary of Changes

| Phase | Files | Lines Affected |
|-------|-------|---------------|
| Backend - Models | 2 | 2 lines |
| Backend - Services | 1 | 4 locations |
| Backend - OCR Worker | 1 | 5 locations |
| Backend - Mistral | 1 | 1 location |
| Frontend | 1 | 2 locations |
| **Total** | **6** | **14 locations** |
