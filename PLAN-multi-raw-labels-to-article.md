# Multi Raw-Label Linking to a Single Article (Bulk Aliases UX)

## Goal
Make it **easy for non-technical users** to connect **more than one raw label** (supplier-scoped) to the **same canonical Article**, so future expenses auto-link reliably even with OCR/POS label variance.

In this repo, “linking a raw label to an article” means creating an `article_aliases` row:
- `(supplier_id, raw_label_normalized) -> article_id`

## Why (User Problem)
Users see many variants for the same item:
- “GALA APPLES”
- “APPLES G”
- “GALA APPLES 1KG”
- OCR typos (e.g. “GALA APPLFS”)

If users can only map **one** label at a time, the system learns slowly and “Unmapped Items” stays noisy.

## Current State (Reality Check)
- ✅ Data model already supports **many aliases** per article (create multiple alias rows pointing to the same `article_id`).
- ✅ Admin UI already has CRUD at `/admin/article-aliases` to create multiple aliases (but it’s **not optimized** for non-technical users).
- ✅ Auto-linking on expense creation/update uses supplier + normalized label to set `expense_items.article_id`.
- ⚠️ The convenient admin API endpoints documented for:
  - `/admin/api/expenses/articles/unmapped-items`
  - `/admin/api/expenses/articles/items/:item-id/map`
  - `/admin/api/expenses/articles/:id/aliases`
  appear **not implemented** today (only the service functions exist); the current articles routes are standard CRUD only.

## Product Principles
- **Predictable beats clever**: never “guess” a mapping; only explicit alias rows produce matches.
- **No surprises**:
  - Never override a user-provided `article_id` on expense create/update.
  - Never silently reassign an existing alias to a different article (require explicit confirmation).
- **Low-friction learning loop**: mapping should feel like “teach once, benefit forever”.

## Scope
### In scope (v1)
1) **Bulk alias creation UX**
   - From **Article Detail**: add many raw labels to the same article (optionally scoped to one supplier).
   - From **Unmapped Items** (recommended): select multiple raw labels → map to an article (existing or new) → create aliases for all selected labels.
2) **Safe conflict handling**
   - If a label is already aliased to a different article for that supplier, show a clear conflict + require confirmation to reassign.
3) **Basic normalization & validation**
   - Trim/normalize input labels.
   - Skip blank/too-short/punctuation-only labels.
   - Deduplicate labels that normalize to the same key.

### Not in scope (v1)
- Fuzzy matching, “best guess”, confidence scoring UI, cross-supplier alias copy, bulk backfill of historical items beyond “same raw label + supplier” convenience.

## UX Proposal

### A) Article Detail → “Add Aliases” Modal (fast path)
Location: `/admin/articles/:id`

Add a new card or section next to the “Article Aliases” related table:
- Primary button: **“Add aliases”**
- Modal contents:
  1) Supplier selector (optional but recommended; default empty = user must choose)
  2) Textarea: “Raw labels (one per line)”
  3) Checkbox: “Reassign conflicts” (default OFF)
  4) Save
- After save:
  - Show summary: `Created N`, `Skipped M`, `Conflicts K`
  - Refresh aliases list for this article

Why this helps:
- Non-technical users can paste labels from receipts/POS exports quickly.

### B) New Admin Page: “Unmapped Items” (recommended)
Route: `/admin/unmapped-items` (or `/admin/articles/unmapped-items`)

UI:
- Table grouped by supplier (or filterable by supplier)
- Rows should show: raw label, times seen, last seen, example amounts (optional)
- Actions:
  - Select multiple rows (same supplier recommended)
  - Click **“Map to article…”**
  - In modal: choose existing article OR “Create new article” (prefill canonical name)
  - “Create aliases for selected labels” is ON by default
  - Optional: “Also apply mapping to all existing items with these labels” (requires backend support; see below)

This is the best “teach the system” workflow because it starts from what the user actually sees (raw labels).

### C) Keep `/admin/article-aliases` as Advanced Management
Don’t remove it; keep it for:
- auditing, manual edits, bulk cleanup
- support/admin workflows

## Backend Implementation Plan

### 1) Add admin endpoints needed by the new UX
Because the existing routes are generated via `routes-factory`, add explicit custom routes (simplest) or extend the factory to support “additional routes” correctly.

Recommended explicit route approach:
- Update `src/app/domain/backend/expenses/routes/articles.clj` to include:
  - `GET /admin/api/expenses/articles/unmapped-items` → wraps `articles/list-unmapped-items`
  - `POST /admin/api/expenses/articles/items/:item-id/map` → wraps `articles/map-item-to-article!`
  - `POST /admin/api/expenses/articles/:id/aliases` → batch-create aliases for the article

### 2) Add a safe batch alias service function
Create a new service function in `src/app/domain/backend/expenses/services/articles.clj` (or a dedicated namespace) that:
- Accepts: `supplier-id`, `article-id`, `raw-labels` (collection), and `opts` (e.g. `{:allow-reassign? false}`)
- Normalizes + validates labels
- Dedupes by normalized key
- For each normalized label:
  - If no alias exists → insert
  - If alias exists and `article_id` matches → skip as already present
  - If alias exists and points to different article:
    - If `allow-reassign?` false → record conflict
    - If `allow-reassign?` true → update to new `article_id` (log/return in response)
- Returns a structured result:
  - `{:created [...], :skipped [...], :conflicts [...], :reassigned [...]}` (IDs + normalized labels)

### 3) Optional: Backfill mapping for existing items
If we want “apply to existing items with those labels” from the UI:
- Add service function that updates `expense_items.article_id` where:
  - parent expense supplier matches
  - `article_id` is NULL
  - normalized(raw_label) in selected normalized keys
- Keep it opt-in and clearly explained in UI.

## Frontend Implementation Plan (Admin)

### 1) New page + state for Unmapped Items
- Add a new admin route in `src/app/domain/frontend/expenses/routes.cljs`
- Implement page under `src/app/domain/frontend/expenses/pages/admin/` (new file)
- Add re-frame events + subs under `src/app/domain/frontend/expenses/events/` + `src/app/domain/frontend/expenses/subs/`
- Add an admin-crud style request wrapper in `src/app/domain/frontend/expenses/admin/adapters/admin_crud.cljs` for the new endpoints

### 2) “Map to Article” modal supports multi-label → single article
Modal behavior:
- Shows selected labels (read-only list)
- Article selector + “Create new article” (prefill canonical name)
- Checkbox “Create aliases for these labels” (default ON)
- Checkbox “Reassign existing aliases” (default OFF; only shown when conflicts detected)
- On save:
  - Create article if requested
  - Call batch aliases endpoint
  - Optionally backfill existing items (if supported)
  - Refresh unmapped list + article aliases list

### 3) Article Detail “Add aliases” modal
Add a simple modal to `/admin/articles/:id` that:
- picks supplier
- takes textarea list
- calls batch aliases endpoint
- refreshes aliases list

## Component IDs (for chrome-mcp tests)
All new interactive components must have stable `:id`:
- Unmapped items page:
  - filters: `filter-supplier-unmapped-items`
  - actions: `btn-map-unmapped-items`
  - table rows: `row-unmapped-item-<item-id>`
- Map modal:
  - `modal-map-unmapped-items`
  - `select-map-article`
  - `btn-create-article-from-labels`
  - `toggle-create-aliases`
  - `toggle-reassign-conflicts`
  - `btn-submit-map-unmapped-items`
- Article detail modal:
  - `btn-open-add-aliases-article-<article-id>`
  - `select-supplier-add-aliases`
  - `textarea-aliases-add`
  - `btn-submit-add-aliases-article-<article-id>`

## Acceptance Criteria
1) A user can add **multiple raw labels** for the **same supplier** to one article in **one action**.
2) Each added label produces (or reuses) an alias row, and future expenses with those labels auto-link.
3) Blank/too-short/punctuation-only labels are skipped (user sees a “Skipped” count and why).
4) If a label is already aliased to a different article:
   - the system does **not** change it silently
   - user must explicitly confirm reassigning that label
5) All new UI controls have stable `:id`s for browser automation.

## Automated Tests (Plan)
Backend (Kaocha, focused namespaces):
- Batch alias creation:
  - creates multiple aliases
  - dedupes normalized collisions
  - rejects conflicts by default
  - reassigns when `allow-reassign?` true
- Unmapped items endpoint returns items with `article_id = nil`
- Map item endpoint sets `article_id` and optionally creates alias

Frontend (CLJS):
- If existing test infrastructure supports it: event-level tests for “map to article” flow (happy path + conflict path).

## Manual Test Script (Plan)
1) Create supplier “Walmart” and payer “Cash”.
2) Create article “Gala Apples”.
3) Add aliases in bulk for Walmart:
   - `APPLES G`
   - `GALA APPLES`
   - `GALA APPLFS` (typo)
4) Create an expense with items using those labels and verify items auto-link to “Gala Apples”.
5) Conflict test:
   - Create article “Green Apples”
   - Attempt to alias `APPLES G` to “Green Apples”
   - Verify the UI blocks or requires explicit “reassign” confirmation.

## Phases / Checklist
- [ ] Phase 1: Backend custom routes for unmapped/map/batch-alias
- [ ] Phase 2: Backend batch alias service + conflict handling
- [ ] Phase 3: Admin “Unmapped Items” page + multi-map modal
- [ ] Phase 4: Article detail “Add aliases” modal
- [ ] Phase 5: Focused BE/FE tests + docs update

## Progress Log
- 2026-01-08: Plan created (no code changes yet).

