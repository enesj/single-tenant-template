# Plan: Unused/Legacy Code Audit in `src/app/domain`

**Date:** 2026-01-14  
**Status:** Planning Phase  
**Prerequisite:** See `PLAN-legacy-code-removal.md` for completed cleanup work

---

## Overview

This document identifies remaining unused, legacy, and stub code in `src/app/domain` that could be considered for cleanup or completion. The previous legacy cleanup plan (`PLAN-legacy-code-removal.md`) addressed most major issues. This audit captures remaining items.

---

## Category 1: Incomplete Features (PDF Export Stub)

### 1.1 PDF Export Handler - `settings.clj`

**File:** `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj`  
**Lines:** 191-195

```clojure
;; For PDF, return stub message
(h/json-response {:message "PDF export not yet implemented"
                  :format format
                  :count (count expenses)} 200)
```

- **Status:** Stub - returns placeholder message when `?format=pdf` is requested
- **Risk:** LOW (feature not exposed in UI, only reachable via direct API call)
- **Recommendation:** 
  - Option A: Implement PDF export using a library (e.g., clj-pdf)
  - Option B: Remove PDF option entirely and document CSV-only export
  - Option C: Leave as-is (low priority)

---

## Category 2: Legacy Payload Compatibility (Frontend)

### 2.1 Receipt Normalization - Underscore/Kebab Dual Support

**File:** `src/app/domain/frontend/expenses/admin/adapters/normalize.cljs`  
**Lines:** 110-125

```clojure
;; Note: keep underscore sources for legacy payloads.
:total_amount_guess [:total-amount-guess]
:lines_total_amount_guess [:lines-total-amount-guess]
:currency_guess [:currency-guess]}
:post-transform (fn [m]
                  (let [total (or (:total-amount-guess m) (:total_amount_guess m))
                        lines-total (or (:lines-total-amount-guess m) (:lines_total_amount_guess m))
                        currency (or (:currency-guess m) (:currency_guess m))
```

- **Status:** Backward compatibility for legacy API payloads
- **Risk:** LOW (code is defensive, not harmful)
- **Recommendation:**
  - If backend API is confirmed to always return kebab-case: remove underscore fallbacks
  - Otherwise: keep as-is for robustness

---

## Category 3: Route Aliases / Redundancy

### 3.1 Multiple Dashboard Routes

**File:** `src/app/domain/frontend/expenses/routes/user.cljs`

| Route Path | Route Name | View |
|------------|------------|------|
| `/expenses` | `:expenses-dashboard` | `:expenses-dashboard` |
| `/dashboard` | `:user-dashboard` | `:expenses-dashboard` |
| `/expenses/dashboard` | `:expenses-dashboard-alias` | `:expenses-dashboard` |

- **Status:** Three routes serve the same view (intentional for SEO/UX reasons)
- **Risk:** LOW (functional, just redundant)
- **Recommendation:** Keep - aliases provide flexibility for navigation and bookmarks

### 3.2 SPA Routes in Backend Registry

**File:** `src/app/domain/backend/registry.clj`  
**Lines:** 38-55

The `:spa-routes` vector includes paths that may be legacy or unused:

```clojure
:spa-routes
["/waiting-room"
 "/dashboard"          ;; Alias for /expenses
 "/unmapped-items"
 "/expenses"
 "/expenses/list"
 ...
```

- **Status:** These match the frontend routes, so they're valid
- **Risk:** N/A (correctly synchronized)
- **Recommendation:** No action needed

---

## Category 4: Legacy Comment Markers (Documentation Debt)

### 4.1 "NOTE: Avoid legacy alias vars" Comments

The following files contain notes about removed legacy patterns that may confuse future readers:

| File | Line | Comment |
|------|------|---------|
| `services/expense_items.clj` | 25 | `;; NOTE: Avoid legacy alias vars...` |
| `services/price_observations.clj` | 24 | `;; NOTE: Avoid legacy alias vars...` |
| `services/article_aliases.clj` | 24 | `;; NOTE: Avoid legacy alias vars...` |
| `services/payers.clj` | 23 | `;; NOTE: Avoid legacy alias vars...` |
| `services/suppliers.clj` | 22 | `;; NOTE: We intentionally avoid legacy alias vars...` |

- **Status:** Historical notes from the legacy cleanup
- **Risk:** LOW (documentation only)
- **Recommendation:** 
  - Option A: Remove these comments since the legacy vars are gone
  - Option B: Keep as architectural documentation

---

## Category 5: Test Stubbing Infrastructure

### 5.1 Mistral OCR HTTP Stubs

**Files:**
- `src/app/domain/backend/expenses/integrations/mistral_ocr.clj` (lines 8, 27)
- `src/app/domain/backend/expenses/integrations/mistral_ocr/http.clj` (lines 43, 50)

```clojure
;; Re-export HTTP utilities (for test stubbing)
"Low-level HTTP POST. Separated for stubbing in tests."
"Kept as a var so tests can stub network calls."
```

- **Status:** Intentional design for testability
- **Risk:** N/A (this is proper design, not legacy code)
- **Recommendation:** Keep - this is the correct pattern for external service mocking

---

## Summary

| Category | Items | Priority | Recommendation |
|----------|-------|----------|----------------|
| Incomplete Features | 1 (PDF export) | LOW | Implement or remove |
| Legacy Payload Compat | 1 (normalize.cljs) | LOW | Verify API & clean if safe |
| Route Aliases | 3 dashboard routes | N/A | Keep (intentional) |
| Comment Markers | 5 files | LOW | Consider removing |
| Test Infrastructure | 2 files | N/A | Keep (correct design) |

---

## Comparison with Previous Plan

The previous `PLAN-legacy-code-removal.md` addressed:
- ✅ Empty placeholder functions (`all-admin-routes`, `all-pages`, `init!`)
- ✅ Legacy function name aliases (5 service files)
- ✅ Legacy arity support (2-arity route fns)
- ✅ Stub implementations (settings handlers → now persisted)

This new audit found:
- 🟡 One remaining stub (PDF export)
- 🟡 Minor documentation debt (legacy comments)
- ⚪ Intentional patterns (route aliases, test stubs)

---

## Execution Plan

### Phase 1: Documentation Cleanup (Optional, Low Risk)

1. Remove "NOTE: Avoid legacy alias vars" comments from:
   - `services/expense_items.clj`
   - `services/price_observations.clj`
   - `services/article_aliases.clj`
   - `services/payers.clj`
   - `services/suppliers.clj`

### Phase 2: PDF Export Decision

1. Decide: Implement PDF export or remove the option?
2. If removing:
   - Update `export-expenses-handler` to return 400 for non-CSV formats
   - Update frontend to not offer PDF option (if applicable)
3. If implementing:
   - Add `clj-pdf` or similar dependency
   - Implement PDF generation in `export-expenses-handler`

### Phase 3: Frontend Normalization Cleanup (Requires Verification)

1. Verify backend always returns kebab-case for receipt fields
2. If confirmed:
   - Remove underscore fallbacks in `receipt->template-entity`
   - Remove "legacy payloads" comment

---

## Notes

- This codebase has been actively maintained and most legacy patterns have been cleaned up
- The remaining items are low-priority and low-risk
- Some "legacy" patterns are intentional (test stubbing, route aliases)
