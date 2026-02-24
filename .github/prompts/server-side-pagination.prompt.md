---
description: Migrate entity list pages from client-side fetch-1000 to proper server-side pagination with backend filtering
argument-hint: "Entities to migrate (default: articles, suppliers, manufacturers)"
agent: "agent"
---

You are working in `/Users/enes/Projects/single-tenant-template` (Clojure backend + shadow-cljs + re-frame + UIx).

## Goal

Switch admin entity list pages from the current **client-side pagination pattern** (fetching up to 1000 records at once and filtering in-memory) to **proper server-side pagination** — fetching one page at a time with filters forwarded as query params to the backend.

Default target entities: **articles**, **suppliers**, **manufacturers**.
If arguments were provided, use those entity names instead.

---

## Background

### Current (broken) pattern

Pages dispatch `load-list {:fetch-limit 1000 :fetch-offset 0}`. This:
- Fetches all records in one request (doesn't scale)
- Performs filtering client-side (only filters what was fetched)
- After CRUD mutations, CRUD bridge reloads the full 1000 records

### Target (server-side) pattern

Pages initialise the list in `:server` pagination mode. On every page change or filter change the `load-list` event fires with `limit`/`offset` derived from current page state, plus any active filters forwarded as query params. The backend returns filtered/paginated results; the frontend reads `:total` from the response to drive the pagination UI.

---

## Architecture reference

### Key DB paths (all in `src/app/template/frontend/db/paths.cljs`)

| Path | Purpose |
|------|---------|
| `[:ui :lists <entity> :pagination-mode]` | `:server` or `:client` (default) |
| `[:ui :lists <entity> :refresh-event]` | Event vector fired on filter/page change |
| `[:ui :lists <entity> :filters]` | Active filter map `{field-key value}` |
| `[:ui :lists <entity> :total-items]` | Total record count for pagination UI |
| `[:ui :lists <entity> :current-page]` | Current page number |
| `[:ui :lists <entity> :per-page]` | Page size |

### Key subscriptions

- `::entity-subs/paginated-entities` — in server mode, returns items as-is (backend already paginated)
- `::list-subs/total-pages` — in server mode, reads `[:ui :lists entity :total-items]`
- `::list-subs/filtered-items` — in server mode, skips client-side filter (backend filters)
- `server-pagination?` checks `[:ui :lists entity :pagination-mode]` = `:server`

### Key events

- `filter-events/apply-filter` — when server-mode and filter changed, calls `list-refresh-dispatch`
- `ui-state-events/list-refresh-dispatch` — reads `[:ui :lists entity :refresh-event]` and dispatches it
- `events-factory/generate-list-events` — generates `load-list`, `list-loaded`, `load-failed`

---

## Implementation

Work through these layers **in order**. Use the REPL to validate each layer before moving on.

### Layer 1 — Backend: enable count endpoint for each entity

**File:** `src/app/domain/backend/expenses/routes/route_configs.clj`

For each target entity, ensure:
- `has-count? true`
- `custom-count-params` mirrors the filters in `custom-query-params`

```clojure
;; Before (articles)
(def article-config
  {:entity-key :article
   :entity-plural :articles
   ...
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp] {:search (:search qp)})})

;; After
(def article-config
  {:entity-key :article
   :entity-plural :articles
   ...
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp] {:search (:search qp)})
   :custom-count-params (fn [qp] {:search (:search qp)})})
```

Suppliers and manufacturers already have `has-count? true`; verify their `custom-count-params` matches their `custom-query-params`.

---

### Layer 2 — Backend: add count service function for articles

**File:** `src/app/domain/backend/expenses/services/articles.clj`

The routes factory resolves the count fn as `count-articles` (via `resolve-service-op-fn`). Add it:

```clojure
(defn count-articles
  "Return total count of articles matching optional search filter."
  [db {:keys [search]}]
  (let [base {:select [[:%count.* :total]]
              :from [[:articles :a]]}
        query (cond-> base
                (seq search)
                (assoc :where [:or
                               [:ilike :a.canonical_name (str "%" search "%")]
                               [:ilike :a.normalized_key (str "%" search "%")]]))]
    (first (jdbc/execute! db (sql/format query)
             {:builder-fn rs/as-unqualified-kebab-maps}))))
```

Verify suppliers and manufacturers already have `count-suppliers` / `count-manufacturers` service fns. If missing, add them following the same pattern.

**Validate via REPL:**
```clojure
(require 'app.domain.backend.expenses.services.articles :reload)
(app.domain.backend.expenses.services.articles/count-articles db {:search "milk"})
;; => {:total 42}
(app.domain.backend.expenses.services.articles/count-articles db {})
;; => {:total <total-article-count>}
```

---

### Layer 3 — Backend: expose count total inline in list response

The routes factory currently returns only `{:articles [...]}` from the list handler. Modify `build-list-handler` in `src/app/domain/backend/expenses/routes/routes_factory.clj` to include `:total` when a count fn is available:

```clojure
;; In build-list-handler, after resolving results:
(let [count-fn (when has-count?
                 (resolve-service-op-fn service
                   (symbol (str "count-" (name entity-plural)))
                   :count))
      total (when count-fn
              (:total (count-fn db custom-params)))
      response (cond-> {response-key response-data}
                 (some? total) (assoc :total total))]
  (utils/success-response response))
```

This means `GET /admin/api/expenses/articles?limit=25&offset=0&search=foo` returns:
```json
{"articles": [...25 items...], "total": 312}
```

**Validate via HTTP (with server running):**
```
GET /admin/api/expenses/articles?limit=5&offset=0
# Expect: {"articles":[...5 items...], "total":<N>}
GET /admin/api/expenses/articles?limit=5&offset=0&search=milk
# Expect: {"articles":[...filtered items...], "total":<filtered-count>}
```

---

### Layer 4 — Frontend events factory: auto-merge DB filters and store total

**File:** `src/app/domain/frontend/expenses/events/events_factory.cljs`

Two changes in `generate-list-events`:

#### 4a. In `load-list` — auto-merge active DB filters when in server mode

When the refresh event fires with no params (e.g. `[::articles/load-list]`), filters must be read from the DB rather than from the event payload.

```clojure
;; In the load-list rf/reg-event-fx handler, near the top:
(fn [{:keys [db]} [_ params]]
  (let [params (or params {})
        fetch-limit (:fetch-limit params)
        fetch-mode? (some? fetch-limit)
        ;; NEW: in server mode, seed params with current DB filter state
        server-mode? (= :server (get-in db (paths/list-pagination-mode entity-key)))
        db-filters (when (and server-mode? (not fetch-mode?))
                     (get-in db (paths/list-filters entity-key) {}))
        ;; DB filters are base; explicit params override (allows filter reset via params)
        params (cond-> params
                 (seq db-filters) (as-> p (merge db-filters p)))
        params* (cond-> params
                  fetch-mode? (dissoc :fetch-limit :fetch-offset))
        ...
```

#### 4b. In `list-loaded` — store `:total` from response into UI state

```clojure
;; In the list-loaded rf/reg-event-fx handler:
(fn [{:keys [db]} [_ pagination response]]
  (let [total (:total response)
        db* (-> db
              (finish-load entity-key base-path nil)
              (assoc-in (conj base-path :items)
                (vec (or (get response (keyword (name entity-key))) []))))
        ;; NEW: store total for server-mode pagination UI
        db* (if (some? total)
              (assoc-in db* (paths/list-total-items entity-key) total)
              db*)
        db* (if pagination
              (update-pagination-state db* entity-key pagination)
              db*)]
    {:db db*
     :dispatch-n [[:admin/refresh-entity-list entity-key response]]}))
```

**Validate via REPL after shadow reload:**
```clojure
;; After loading articles page, check DB state
@re-frame.db/app-db
;; Look for [:ui :lists :articles :total-items] — should be non-nil
```

---

### Layer 5 — Frontend entity config: add count endpoint

**File:** `src/app/domain/frontend/expenses/events/entity_configs.cljs`

No structural change needed if Layer 3 returns `:total` inline. The config is already used as-is. Skip this layer if Layer 3 is implemented.

If you chose NOT to do Layer 3 (separate count request instead), add `:count-endpoint` to each config and handle it in the factory.

---

### Layer 6 — Frontend admin page: switch to server mode

**File:** `src/app/admin/frontend/pages/domain/expenses/articles.cljs`
(Repeat for suppliers and manufacturers pages.)

#### 6a. On mount — initialise server pagination state

Replace the current `fetch-limit` dispatch with a server-mode init:

```clojure
;; Before
(let [refresh-list (use-callback
                     (fn []
                       (rf/dispatch [::articles-events/load-list
                                     {:fetch-limit 1000 :fetch-offset 0}]))
                     [])]

;; After
(let [entity-key :articles
      refresh-list (use-callback
                     (fn []
                       ;; Set server mode + register refresh event, then load page 1
                       (rf/dispatch-sync
                         [:app.template.frontend.events.list.ui-state/set-pagination-mode
                          entity-key :server])
                       (rf/dispatch-sync
                         [:app.template.frontend.events.list.ui-state/set-refresh-event
                          entity-key [:app.domain.frontend.expenses.events.articles/load-list]])
                       (rf/dispatch [:app.domain.frontend.expenses.events.articles/load-list
                                     {:page 1 :per-page 25}]))
                     [])]
```

> **Note:** `set-pagination-mode` and `set-refresh-event` events must exist in `ui_state.cljs` — see Layer 7.

#### 6b. CRUD bridge reload — keep filters preserved

**File:** `src/app/domain/frontend/expenses/admin/adapters/admin_crud.cljs`

The CRUD bridge `on-success` handlers should reload without `fetch-limit` (already fixed by prior patch). In server mode this is correct — `load-list {}` will trigger a server-side reload of the current page with current filters (via the DB-filter auto-merge from Layer 4a).

No additional change needed here if Layer 4a is implemented.

---

### Layer 7 — Frontend ui-state events: add set-pagination-mode and set-refresh-event

**File:** `src/app/template/frontend/events/list/ui_state.cljs`

These events may already exist — check first. If missing, add them:

```clojure
(rf/reg-event-db
  ::set-pagination-mode
  common-interceptors
  (fn [db [entity-type mode]]
    (assoc-in db (paths/list-pagination-mode entity-type)
      (normalize-pagination-mode mode))))

(rf/reg-event-db
  ::set-refresh-event
  common-interceptors
  (fn [db [entity-type event-vec]]
    (assoc-in db (paths/list-refresh-event entity-type) event-vec)))
```

---

## Validation checklist

Run these checks for each migrated entity:

### Happy path
- [ ] Page loads — correct first page of results shown, pagination controls reflect total
- [ ] Typing in search filter — list updates to filtered results, page resets to 1
- [ ] Clicking page 2 — next page of (filtered) results loads
- [ ] Clearing filter — full list resumes, page resets to 1

### Edge cases
- [ ] Empty filter value — treated as no filter (returns all)
- [ ] Filter matching zero results — empty list shown, total = 0
- [ ] Page 1 with per-page = 25 and total = 0 — no crash
- [ ] Edit an entity and save — list refreshes keeping current filter and page
- [ ] Delete an entity — list refreshes; if current page becomes empty, stays on last valid page

### Backend validation
```bash
# Run focused backend tests (save once)
mkdir -p tmp && bb be-test --grep articles 2>&1 | tee tmp/be-test-articles.txt
```

### Frontend validation
```clojure
;; In browser REPL after navigating to /admin/articles:
(get-in @re-frame.db/app-db [:ui :lists :articles])
;; Expect: {:pagination-mode :server
;;          :refresh-event [...]
;;          :total-items <N>
;;          :current-page 1
;;          :per-page 25
;;          :filters {}}
```

---

## Files to change

| File | Change |
|------|--------|
| `src/app/domain/backend/expenses/routes/route_configs.clj` | `has-count? true`, `has-search? true`, add `custom-count-params` for articles |
| `src/app/domain/backend/expenses/services/articles.clj` | Add `count-articles` fn |
| `src/app/domain/backend/expenses/routes/routes_factory.clj` | Include `:total` in list response when count fn available |
| `src/app/domain/frontend/expenses/events/events_factory.cljs` | Auto-merge DB filters in server mode; store `:total` from response |
| `src/app/admin/frontend/pages/domain/expenses/articles.cljs` | Init server mode on mount; remove `fetch-limit` |
| `src/app/admin/frontend/pages/domain/expenses/suppliers.cljs` | Same as articles page |
| `src/app/admin/frontend/pages/domain/expenses/manufacturers.cljs` | Same as articles page |
| `src/app/template/frontend/events/list/ui_state.cljs` | Add `set-pagination-mode` and `set-refresh-event` events if missing |

## Files to reference

- `src/app/domain/backend/expenses/services/articles.clj` — `list-articles` pattern (model count fn on this)
- `src/app/domain/backend/expenses/routes/routes_factory.clj` — `build-list-handler`, `build-count-handler`, `build-extended-routes`
- `src/app/domain/backend/expenses/routes/route_configs.clj` — existing `has-count? true` examples (suppliers, manufacturers)
- `src/app/domain/frontend/expenses/events/events_factory.cljs` — full `generate-list-events` fn
- `src/app/template/frontend/subs/list.cljs` — `::total-pages`, `server-pagination?`, `::filtered-items`
- `src/app/template/frontend/subs/entity.cljs` — `::paginated-entities`, `::filtered-entities`, `server-pagination?`
- `src/app/template/frontend/events/list/filters.cljs` — `::apply-filter`, `server-pagination?`, `list-refresh-dispatch`
- `src/app/template/frontend/events/list/ui_state.cljs` — `list-refresh-dispatch`, `normalize-pagination-mode`
- `src/app/template/frontend/db/paths.cljs` — `list-pagination-mode`, `list-refresh-event`, `list-total-items`, `list-filters`
