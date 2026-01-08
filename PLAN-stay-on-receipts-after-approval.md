# Plan: Stay on Receipts Page After Expense Creation

**Date Created**: 2026-01-08
**Status**: Planning Phase (no code changes yet)

## Goal

After creating an expense from the "View details - Approve & Post" flow on the receipts page, the user should **stay on the receipts page** instead of navigating to the newly created expense detail page.

---

## Phase 0 — Discovery (Current Behavior Analysis)

### Admin Panel (✓ Already Correct)

**Location**: `src/app/domain/frontend/expenses/events/receipts.cljs`

**Current Behavior**:
- After approving a receipt from `/admin/receipts/:id`, the `::approve-receipt-success` event:
  1. Refreshes the receipts and expenses entities
  2. Reloads the receipt detail
  3. Calls the `on-success` callback (switches to Details tab)
  4. **NO navigation occurs**

**Outcome**: The admin panel already stays on the receipts page after approval. **No changes needed** for the admin flow.

---

### User-Facing Expenses (⚠️ Change Needed)

**Location**: `src/app/domain/frontend/expenses/events/user_expenses/receipts.cljs`

**Current Behavior** (lines 182-207):
```clojure
(rf/reg-event-fx
  :user-expenses/approve-receipt-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [expense-id (or (:id expense) (get-in expense [:id]))
          fx (cond-> []
               on-success (conj [:dispatch-later {:ms 100}
                                 :dispatch [:user-expenses/call-modal-callback on-success]]))]
      (cond-> {:db ...}
               :dispatch-n (cond-> [[:user-expenses/fetch-recent ...]
                                    [:user-expenses/fetch-receipts ...]
                                    [:user-expenses/fetch-receipt receipt-id]]
                             expense-id (conj [:navigate-to (str "/expenses/" expense-id)]))  ;; ← NAVIGATES AWAY
               :fx fx
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))
```

**Problem**: Line 204 includes `[:navigate-to (str "/expenses/" expense-id)]`, which navigates the user away from the receipts list/detail page to the newly created expense detail page.

---

## Phase 1 — Remove Navigation from User Approval Flow

### Change Required

**File**: `src/app/domain/frontend/expenses/events/user_expenses/receipts.cljs`

**Action**: Remove the navigation to expense detail from `:user-expenses/approve-receipt-success`

**Current code** (lines 201-204):
```clojure
:dispatch-n (cond-> [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                     [:user-expenses/fetch-receipts {:limit 50 :offset 0}]
                     [:user-expenses/fetch-receipt receipt-id]]
              expense-id (conj [:navigate-to (str "/expenses/" expense-id)]))
```

**New code**:
```clojure
:dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
             [:user-expenses/fetch-receipts {:limit 50 :offset 0}]
             [:user-expenses/fetch-receipt receipt-id]]
```

**Explanation**: The `cond->` expression with `expense-id (conj ...)` is removed. The user will stay on the receipts page, and the receipt list will be refreshed to show the updated status.

---

## Phase 2 — Verification

### UI Checks (Manual)

1. **Pre-change baseline**:
   - Navigate to receipts list page
   - Click on a receipt to view details
   - Go through "Approve & Post" flow
   - Observe: User is navigated to `/expenses/{expense-id}`

2. **Post-change verification**:
   - Navigate to receipts list page
   - Click on a receipt to view details
   - Go through "Approve & Post" flow
   - Observe: User stays on receipts page, receipt is updated with "posted" status
   - Verify the expense was created (check expenses list)

### Database Verification

After approval:
```sql
-- Verify receipt status is posted
SELECT id, status, expense_id FROM receipts WHERE id = ?;

-- Verify expense was created
SELECT id, supplier_id, total_amount FROM expenses WHERE id = ?;
```

### ClojureScript REPL Verification

```clojure
;; After approval, verify state
(require '[app.domain.frontend.expenses.events.user-expenses.receipts :as receipts])
(in-ns 'app.domain.frontend.expenses.events.user_expenses.receipts)

;; Check that receipt has expense_id and posted status
;; Check that expense exists in the expenses list
```

---

## Summary of Changes

| File | Lines | Change |
|------|-------|--------|
| `src/app/domain/frontend/expenses/events/user_expenses/receipts.cljs` | 201-204 | Remove `expense-id (conj [:navigate-to ...])` from cond-> |

---

## Notes

- **Admin panel**: No changes needed - already stays on receipts page
- **User expenses**: Single-line change to remove navigation
- **UX improvement**: Users can process multiple receipts in sequence without navigating back and forth
- **Accessibility**: Receipt remains in view, allowing immediate verification or further actions

---

## Non-Goals

- This plan does NOT change the admin behavior (already correct)
- This plan does NOT modify the backend approval logic
- This plan does NOT change the approval UI or form fields
