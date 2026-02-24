---
description: Add "Show related records" dropdown action to entity admin pages
---

You are working in `/Users/enes/Projects/single-tenant-template` (Clojure backend + shadow-cljs + Tailwind).

## Goal

Add a **"Show related records"** dropdown action to admin entity pages that need it, following the existing pattern from `/admin/articles`.

## Context

The Articles page already has this feature:
- **Frontend**: `src/app/admin/frontend/pages/domain/expenses/articles.cljs` has `show-related-records-actions` that adds a dropdown action
- **Wizard component**: `src/app/domain/frontend/expenses/components/article_related_records_wizard.cljs` implements a 3-step modal (type → record → details)
- **Backend API**: `GET /admin/api/expenses/articles/:id/related-records` in `src/app/domain/backend/expenses/routes/articles.clj`
- **Service**: `list-related-records` in `src/app/domain/backend/expenses/services/articles.clj`

## Entities that need this feature

Based on domain relationships, these entities should have "Show related records":

| Entity | Related Types | Priority |
|--------|---------------|----------|
| Suppliers | expenses, receipts, articles, stores | High |
| Stores | expenses, receipts, articles | High |
| Manufacturers | articles | Medium |
| Subcategories | articles | Medium |
| Article Aliases | expenses, receipts | Medium |

## Implementation checklist

For each entity, implement:

### 1. Backend API route
- Add `GET /admin/api/expenses/<entity>/:id/related-records` route
- Accept `type` query param (allowlisted types for that entity)
- Accept optional `limit` (clamp to 1..500)
- Return `{related-records [...]}` with normalized app keys

### 2. Backend service function
- Add `list-related-records` function in the entity's service namespace
- Implement type-specific queries with proper joins
- Handle alias linkages where applicable
- Return rows with useful display fields (names, dates, amounts, etc.)

### 3. Frontend events and subs
- Add events for opening/closing modal, selecting type, loading records
- Add subs for modal state, current step, records, loading, error
- Follow pattern from `src/app/domain/frontend/expenses/events/articles.cljs`

### 4. Frontend wizard component (or generalize existing)
- Either create entity-specific wizard (copy article pattern)
- Or generalize `article_related_records_wizard.cljs` to accept entity config

### 5. Frontend page integration
- Add `show-related-records-actions` function returning action groups
- Add to `render-<entity>-row-actions` via `custom-actions` prop
- Use `dropdown/action-dropdown` with `:position :portal`

### 6. Tests
- Add backend tests for `list-related-records` (happy path + edge cases)
- Test alias resolution where applicable
- Test invalid type handling

## Code patterns

### Dropdown action (frontend)
```clojure
(defn- show-related-records-actions
  [entity]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "🔗"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [::events/open-related-records-modal entity]))}]}])
```

### Row actions integration
```clojure
($ dropdown/action-dropdown
  {:entity-id item-id
   :actions (show-related-records-actions entity)
   :position :portal})
```

### Backend route
```clojure
["/:id/related-records"
 {:get (fn [request]
         ((utils/with-error-handling
            (fn [req]
              (let [entity-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                    qp (:query-params req)
                    related-type (or (get qp "type") (:type qp))
                    limit (utils/parse-int-param qp :limit 100)]
                (let [rows (service/list-related-records
                             db entity-id {:type related-type :limit limit})]
                  (utils/success-response {:related-records (factory/to-app rows)}))))
            "Failed to list related records")
          request))}]
```

## Validation

After implementation:
1. Run focused backend tests: `bb be-test --grep <entity>-related`
2. Verify in browser: navigate to entity page, click dropdown, select type, verify records load
3. Check modal navigation (Back/Next/Close) works correctly
4. Verify error handling for invalid types

## Files to reference

- `src/app/admin/frontend/pages/domain/expenses/articles.cljs` (frontend page pattern)
- `src/app/domain/frontend/expenses/components/article_related_records_wizard.cljs` (wizard component)
- `src/app/domain/frontend/expenses/events/articles.cljs` (events pattern)
- `src/app/domain/backend/expenses/routes/articles.clj` (route pattern)
- `src/app/domain/backend/expenses/services/articles.clj` (service pattern, `list-related-records`)
- `test/app/domain/expenses/services/articles_test.clj` (test patterns)