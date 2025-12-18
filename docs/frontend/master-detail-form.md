<!-- ai: {:namespaces [app.template.frontend.components.form.master-detail app.domain.frontend.expenses.components.expense-form app.domain.frontend.expenses.components.user-expense-form],
         :tags [:frontend :forms :master-detail :template],
         :kind :guide} -->

# Master/Detail Form Wrapper (`master-detail-form`)

## What it is

`master-detail-form` is a reusable UIX wrapper around `app.template.frontend.components.form/form` for **edit modals/pages that require a detail fetch** (e.g. an expense header + line items).

It standardizes:

- Detail-load orchestration for `:edit` mode
- Fallback to list-row data while detail loads
- Stable Fork initialization (memoized spec + initial values) so typing doesn’t reset the form
- A consistent hook point for domain-specific normalization, validation, and submit-payload preparation

**Source:** `src/app/template/frontend/components/form/master_detail.cljs`

---

## When to use it

Use `master-detail-form` when:

- You open an **edit** form from a list row that may not include all detail fields (e.g. `:items`).
- You need to `dispatch` a fetch on open, then populate the form when the full entity arrives.
- You want to avoid copy/pasting `requested?` / `use-effect` / `use-memo` boilerplate across forms.

Keep **domain rules** (numeric coercions, tolerances, required fields, payload shape) in the domain namespace.

---

## Component API

```clojure
($ master-detail-form
   {:mode :edit               ;; :create | :edit
    :entity-name "expense"   ;; string used by template form state
    :entity-spec entity-spec  ;; vector form spec
    :entity-id expense-id     ;; required for :edit

    ;; Detail orchestration
    :load-detail! (fn [id] (rf/dispatch [::events/load-detail id]))
    :select-detail detail-entity          ;; value from subscription
    :detail-loading? detail-loading?
    :detail-error detail-error

    ;; Domain hooks
    :normalize-initial-data normalize
    :validate-values validate             ;; => {:ok? true} or {:ok? false :error "..."}
    :prepare-submit-values prepare

    ;; Callbacks
    :on-submit (fn [prepared] ...)
    :on-cancel on-cancel

    ;; Optional
    :initial-row-data row                 ;; fallback while loading
    :default-values defaults
    :button-text "Update ..."})
```

### Behavior

- On mount / `entity-id` change in `:edit` mode, calls `:load-detail!`.
- Uses `:select-detail` when it matches `:entity-id`; otherwise falls back to `:initial-row-data`.
- Computes `:initial-values` as:
  - `(merge :default-values (normalize-initial-data effective-entity))`
- Runs `:validate-values` before submit.
- Calls `:on-submit` with `prepare-submit-values` output.

---

## Expenses integration notes

### Detail response key mismatch (admin)

The admin expenses detail endpoint returns a singular key:

```json
{ "expense": { ... } }
```

…but the entity key is plural (`:expenses`). The generic event factory supports this via `:detail-response-key`.

**Config:** `src/app/domain/frontend/expenses/events/entity_configs.cljs`

```clojure
(def expenses-config
  {:entity-key :expenses
   :detail-response-key :expense
   ...})
```

This ensures `(::expenses-events/load-detail <id>)` stores the entity under:

- `[:admin :expenses :entries :by-id <id>]`

### Current usage

- Admin edit modal: `src/app/domain/frontend/expenses/components/expense_form.cljs`
- User edit modal: `src/app/domain/frontend/expenses/components/user_expense_form.cljs`

Both use `master-detail-form` and keep expense-specific rules (tolerance, numeric coercions, items[].id preservation) in those namespaces.

---

## Testing checklist

### Admin

1. Open `http://localhost:8085/admin/expenses`
2. Click `#btn-edit-expenses-<expense-id>`
3. Confirm line item inputs exist (IDs from `form_fields.cljs`):
   - `#items-<item-id>-qty`
   - `#items-<item-id>-unit_price`
   - `#items-<item-id>-line_total`
4. Change qty and confirm:
   - line total updates
   - total auto-updates unless overridden
5. Submit and reopen: items still present (no placeholder-only fallback)

### User

Repeat similar checks on `http://localhost:8085/expenses/list`.
