# Mobile Web Implementation Plan

> Spec: `specs/allium/template/mobile-web.candidate.allium`
> Status: Draft — based on elicitation session 2026-03-22

## Overview

A simplified mobile web experience for the expense management app. **Primary goal:** capture expenses on the go. **Secondary goal:** review expenses, dashboard, and reports. Phones are redirected to `/m/*` routes automatically; tablets and desktops get the existing experience.

---

## Architecture Decision: Separate Build vs Runtime Switch

**Recommendation: Separate shadow-cljs build (`:mobile`).**

Reasons:
- Smaller bundle size for mobile users (no admin components, no desktop layout, no reference data management UI)
- Independent deploy/iterate cycle — mobile can ship features without touching desktop
- Clean code separation — mobile components live in their own namespace tree
- The desktop build stays unchanged; no risk of breaking existing functionality
- Shared code (API calls, i18n dictionaries, entity specs, re-frame subscriptions) lives in `.cljc`/shared namespaces

Trade-off: Some code duplication for shared patterns (auth forms, expense list rendering). Mitigated by extracting shared logic into `app.shared.frontend.*` namespaces.

---

## Phase 0: Foundation (Backend + Build Setup)

### 0.1 Device Detection Middleware

**File:** `src/app/template/backend/middleware/device_detection.clj`

```
Request → parse user-agent → phone? → 302 redirect to /m/* path
```

- Add Ring middleware `wrap-mobile-redirect` that:
  1. Parses `User-Agent` header
  2. Classifies as phone using a simple heuristic (check for `Mobile`, `Android`, `iPhone`, etc. — exclude `iPad`, `Tablet`)
  3. For phone requests to non-API, non-admin, non-`/m/` paths: issue HTTP 302 to `/m/{mapped-path}`
  4. Skip redirect for: `/api/*`, `/admin/*`, `/m/*`, static assets (`/js/*`, `/assets/*`, `/favicon*`)
- Path mapping function: strip known prefixes, translate desktop paths to mobile equivalents
- Insert in middleware stack in `src/app/template/backend/routes.clj` — early, before SPA fallback but after static assets

**Tests:** `test/app/template/backend/middleware/device_detection_test.clj`
- Phone UA → redirects to `/m/dashboard` for `/dashboard`
- Tablet UA → no redirect
- Desktop UA → no redirect
- API paths → never redirect
- Already on `/m/*` → no redirect

### 0.2 Mobile SPA Route Tree (Backend)

**File:** `src/app/template/backend/routes/mobile.clj`

- Register `/m/*` catch-all route that serves `mobile-index.html` (or shared `index.html` with mobile JS entry point)
- Mobile SPA fallback paths:
  - `/m/login`, `/m/forgot-password`, `/m/tenant-select`
  - `/m/dashboard`, `/m/expenses`, `/m/expenses/:id`
  - `/m/upload`, `/m/upload/review`, `/m/upload/manual`
  - `/m/reports`, `/m/receipts`, `/m/search`
  - `/m/more`
- All paths serve the same mobile SPA HTML — client-side routing handles the rest
- Mount in `src/app/template/backend/routes.clj` alongside existing SPA routes

### 0.3 Mobile shadow-cljs Build

**File:** `shadow-cljs.edn` — add `:mobile` build

```clojure
:mobile {:target :browser
         :output-dir "resources/public/js/mobile"
         :asset-path "/js/mobile"
         :modules {:app {:init-fn app.mobile.frontend.core/init!}}
         :devtools {:http-port 8086}}
```

**File:** `resources/public/mobile-index.html`
- Same structure as `index.html` but loads `/js/mobile/app.js`
- Same viewport meta, CSRF token injection, Tailwind/DaisyUI CSS

**Backend change:** `render-mobile-page` in `routes/mobile.clj` serves `mobile-index.html`

### 0.4 Mobile Frontend Core

**New namespace tree:**
```
src/app/mobile/frontend/
├── core.cljs              # Entry point, init!, mount
├── routes.cljs            # Reitit router for /m/* paths
├── db/
│   └── defaults.cljs      # Initial app-db state for mobile
├── layout.cljs            # Mobile shell: bottom tabs + content area
├── components/
│   ├── bottom_tabs.cljs   # Tab bar component
│   ├── header.cljs        # Top header with back nav + search
│   └── sync_banner.cljs   # Offline/sync status indicator
└── pages/                 # One file per mobile page (see Phase 1-3)
```

**Shared code (reuse from existing):**
- `app.template.frontend.i18n` — translation dictionaries + `use-t` hook
- `app.template.frontend.db.entity_specs` — entity key normalization
- `app.template.frontend.events.api` — HTTP request effects
- `app.shared.*` — model naming, type conversion
- `app.domain.shared.routes.*` — route descriptors (extended for mobile)

---

## Phase 1: Auth + Navigation Shell

### 1.1 Mobile Auth Pages

**Files:**
- `src/app/mobile/frontend/pages/login.cljs`
- `src/app/mobile/frontend/pages/forgot_password.cljs`
- `src/app/mobile/frontend/pages/tenant_select.cljs`

- Simplified, full-screen forms optimized for touch
- Same API endpoints as desktop (`/api/v1/auth/login`, `/api/v1/auth/forgot-password`, `/api/v1/tenant/switch`)
- After login: check `tenant-selection-required` → redirect to `/m/tenant-select` or `/m/dashboard`
- Login page shows "Don't have an account? Sign up on desktop." message (no register link)

### 1.2 Bottom Tab Bar Navigation

**File:** `src/app/mobile/frontend/components/bottom_tabs.cljs`

5 tabs:
| Position | Tab | Icon | Route |
|----------|-----|------|-------|
| 1 | Dashboard | chart | `/m/dashboard` |
| 2 | Expenses | list | `/m/expenses` |
| 3 (center) | **Upload** | camera (prominent) | `/m/upload` |
| 4 | Reports | bar-chart | `/m/reports` |
| 5 | More | menu | `/m/more` |

- Upload button is visually distinct: larger, raised, accent color
- Badge on Upload tab shows pending review count (from subscription)
- Active tab highlighted
- Fixed position at bottom of viewport

### 1.3 Mobile Layout Shell

**File:** `src/app/mobile/frontend/layout.cljs`

Structure:
```
┌─────────────────────┐
│ Header (contextual) │  ← page title, back button, search (on expenses tab)
├─────────────────────┤
│                     │
│   Content Area      │  ← scrollable, page-specific
│                     │
├─────────────────────┤
│ ◯  ◯  📷  ◯  ◯    │  ← bottom tab bar (fixed)
└─────────────────────┘
```

### 1.4 More Menu Page

**File:** `src/app/mobile/frontend/pages/more.cljs`

Simple list:
- Receipt list → `/m/receipts`
- Language: shows current locale (BS/EN) — read-only indicator, change on desktop
- Logout → clears session, redirects to `/m/login`

---

## Phase 2: Capture Flow (Primary Goal)

### 2.1 Upload Page — Source Chooser

**File:** `src/app/mobile/frontend/pages/upload.cljs`

Three sections:
1. **Capture Receipt** — two large buttons: "Take Photo" (camera) / "Choose from Gallery"
2. **Manual Entry** — button → navigates to `/m/upload/manual`
3. **Pending Reviews** — list of OCR-processed receipts awaiting confirmation (if any)

If offline:
- "Take Photo" / "Choose from Gallery" queue locally
- Show offline indicator banner
- "Sync Now" button appears when back online

### 2.2 Camera / Gallery Integration

**Implementation:** Standard HTML `<input type="file" accept="image/*" capture="environment">` for camera, `<input type="file" accept="image/*">` for gallery.

- On file selection: if online → upload immediately via existing `/api/v1/expenses/upload` endpoint
- If offline → store in offline queue (see 2.4)
- Optional metadata: user can add a note before confirming upload
- Show upload progress indicator

### 2.3 Receipt Confirmation (Pending Review)

**File:** `src/app/mobile/frontend/pages/receipt_review.cljs`

Shown after OCR processing completes (or from pending review list):

```
┌─────────────────────┐
│ ← Review Receipt    │
├─────────────────────┤
│ [Receipt Image]     │  ← thumbnail, tappable to view full
│                     │
│ Supplier: [____]    │  ← editable, with autocomplete
│ Date:     [____]    │  ← date picker
│ Total:    [____]    │  ← numeric input
│ Currency: [____]    │  ← dropdown (pre-filled)
│                     │
│ ─── Line Items ───  │  ← read-only section
│ Article A    €2.50  │
│ Article B    €1.30  │
│ Article C    €4.20  │
│                     │
│ [  Confirm  ]       │  ← primary action button
└─────────────────────┘
```

- Pre-filled with OCR guesses
- Supplier field: search/autocomplete from existing suppliers
- On confirm: POST corrections to API, mark review as complete
- Badge count on Upload tab decrements

### 2.4 Offline Queue

**Implementation:** Use browser `IndexedDB` (via a lightweight wrapper) for persistence across app restarts.

**Re-frame state:**
```clojure
{:mobile/offline-queue [{:image-data "base64..."
                          :metadata {:note "Lunch"}
                          :queued-at "2026-03-22T12:00:00Z"}]
 :mobile/sync-status :idle}  ;; :idle | :syncing | :completed | :failed
```

**Sync flow:**
1. User opens app while online
2. If queue has entries → show "N receipts pending sync" banner + "Sync Now" button
3. User taps "Sync Now"
4. For each entry: upload image → create receipt → remove from queue
5. On completion: update sync status, show success message
6. OCR processes in background → creates pending reviews

**Edge cases:**
- Partial sync failure: keep failed entries in queue, report which succeeded
- App closed during sync: on next open, check queue state and offer retry
- Queue full (5): show "Queue full — sync your receipts first" message

### 2.5 Manual Expense Entry

**File:** `src/app/mobile/frontend/pages/manual_entry.cljs`

- Full field parity with desktop expense creation form
- Same fields: supplier, store, date, total, currency, category, payer, payment method, line items, notes
- Layout: vertical stacked form optimized for mobile (full-width inputs, large touch targets)
- Supplier/store/payer fields: search/autocomplete dropdowns
- Date: native mobile date picker
- Submit → POST to existing `/api/v1/expenses` endpoint
- On success: redirect to `/m/expenses` list with success toast

---

## Phase 3: Review Surfaces (Secondary Goal)

### 3.1 Dashboard

**File:** `src/app/mobile/frontend/pages/dashboard.cljs`

- Summary cards (30-day total, expense count) — full-width stacked cards
- Monthly trend chart — horizontal scrollable or simplified bar chart
- Top suppliers — list view (not chart)
- Category breakdown — simple pie/donut chart or list with percentages
- Same API endpoints: `/api/v1/expenses/summary`, `/by-month`, `/by-supplier`, `/by-category`

### 3.2 Expense List + Detail

**Files:**
- `src/app/mobile/frontend/pages/expense_list.cljs`
- `src/app/mobile/frontend/pages/expense_detail.cljs`

**List:**
- Search bar at top (maps to existing search API)
- Scrollable list with infinite scroll / load more
- Each item shows: supplier, date, total, category badge
- Tap → navigate to `/m/expenses/:id`

**Detail:**
- Full expense view with all fields (read-only on mobile)
- Line items list
- Back button → return to list

### 3.3 Receipt List

**File:** `src/app/mobile/frontend/pages/receipt_list.cljs`

- Scrollable list of receipts
- Each item: thumbnail, supplier guess, date, total, status badge
- No tap-through to detail (receipt detail is desktop-only)
- Same API: `/api/v1/expenses/receipts`

### 3.4 Reports

**File:** `src/app/mobile/frontend/pages/reports.cljs`

- By-category breakdown (list or chart)
- By-supplier breakdown
- Monthly trends
- Same API endpoints as desktop
- Charts adapted for mobile viewport (full-width, vertically stacked)

### 3.5 Search

**Implementation:** Search bar on the Expenses tab header.

- Triggers search on the existing `/api/v1/expenses/search` endpoint
- Results shown in the expense list area
- Clear search → return to full list

---

## Phase 4: Polish & Integration

### 4.1 i18n

- Reuse existing `app.template.frontend.i18n` dictionaries
- Add mobile-specific keys (e.g., `:mobile/sync-now`, `:mobile/queue-full`, `:mobile/take-photo`, `:mobile/choose-gallery`)
- Language preference synced from user profile (server-side), no mobile language toggle
- Language indicator in More menu shows current locale

### 4.2 Responsive Touch Optimization

- All interactive elements: minimum 44x44px touch targets (Apple HIG)
- Form inputs: full-width, 48px height minimum
- Buttons: large, prominent, with adequate spacing
- Swipe gestures: none initially (keep it simple)
- Pull-to-refresh: on list views (expenses, receipts)

### 4.3 Loading & Error States

- Skeleton screens for loading states (not spinners)
- Network error: inline banner with retry action
- API errors: toast messages
- Empty states: helpful messages with CTA (e.g., "No expenses yet — upload your first receipt!")

### 4.4 Session Management

- Session check on app open (call `/api/v1/auth/status`)
- If session expired: redirect to `/m/login`
- If tenant-selection-required: redirect to `/m/tenant-select`
- Session cookie shared with desktop (same domain, same cookie)

---

## File Summary

### New Files

| File | Purpose |
|------|---------|
| `src/app/template/backend/middleware/device_detection.clj` | UA-based phone detection + redirect |
| `src/app/template/backend/routes/mobile.clj` | Mobile SPA route tree + handler |
| `resources/public/mobile-index.html` | Mobile SPA HTML entry point |
| `src/app/mobile/frontend/core.cljs` | Mobile app entry point |
| `src/app/mobile/frontend/routes.cljs` | Client-side routing for `/m/*` |
| `src/app/mobile/frontend/db/defaults.cljs` | Initial mobile app-db |
| `src/app/mobile/frontend/layout.cljs` | Shell: header + content + tabs |
| `src/app/mobile/frontend/components/bottom_tabs.cljs` | Tab bar |
| `src/app/mobile/frontend/components/header.cljs` | Contextual header |
| `src/app/mobile/frontend/components/sync_banner.cljs` | Offline/sync indicator |
| `src/app/mobile/frontend/pages/login.cljs` | Mobile login |
| `src/app/mobile/frontend/pages/forgot_password.cljs` | Mobile forgot password |
| `src/app/mobile/frontend/pages/tenant_select.cljs` | Mobile tenant picker |
| `src/app/mobile/frontend/pages/dashboard.cljs` | Mobile dashboard |
| `src/app/mobile/frontend/pages/expense_list.cljs` | Expense list + search |
| `src/app/mobile/frontend/pages/expense_detail.cljs` | Expense detail (read-only) |
| `src/app/mobile/frontend/pages/upload.cljs` | Upload hub: capture + pending reviews |
| `src/app/mobile/frontend/pages/receipt_review.cljs` | OCR confirmation |
| `src/app/mobile/frontend/pages/manual_entry.cljs` | Manual expense form |
| `src/app/mobile/frontend/pages/receipt_list.cljs` | Receipt list |
| `src/app/mobile/frontend/pages/reports.cljs` | Mobile reports |
| `src/app/mobile/frontend/pages/more.cljs` | More menu |
| `test/app/template/backend/middleware/device_detection_test.clj` | Detection tests |

### Modified Files

| File | Change |
|------|--------|
| `shadow-cljs.edn` | Add `:mobile` build target |
| `src/app/template/backend/routes.clj` | Mount mobile SPA routes + add device detection middleware |
| `src/app/template/frontend/i18n.cljs` | Add mobile-specific i18n keys |
| `resources/public/index.html` | No change (mobile gets its own HTML) |

### Shared Code (No Changes, Reused)

| Namespace | What mobile uses |
|-----------|-----------------|
| `app.template.frontend.i18n` | `use-t`, translation dictionaries |
| `app.template.frontend.events.api` | HTTP effect handlers |
| `app.template.frontend.db.entity_specs` | Entity key normalization |
| `app.shared.model-naming` | snake↔kebab conversion |
| `app.domain.shared.routes.*` | Route descriptors |

---

## Implementation Order

```
Phase 0 ── DONE ──────────────────────────────────────────
  ✅ 0.1 Device detection middleware + tests (32 assertions, 3 tests pass)
  ✅ 0.2 Mobile backend route tree (/m/* → mobile SPA)
  ✅ 0.3 shadow-cljs :mobile build + mobile-index.html
  ✅ 0.4 Mobile core + empty layout shell
  ✓ Milestone: phone access shows mobile app shell

Phase 1 ── DONE (merged into Phase 0.4) ─────────────────
  ✅ 1.1 Login + forgot password pages (with correct i18n keys)
  ✅ 1.2 Tenant selection page
  ✅ 1.3 Bottom tab bar + layout shell (5 tabs, prominent Upload center)
  ✅ 1.4 More menu (placeholder — receipt list link, logout, language)
  ✓ Milestone: can log in on phone and see tab navigation

Phase 2 ──────────────────────────────────────────────────
  ✅ 2.1 Upload page — source chooser (camera / gallery / manual)
  ✅ 2.2 Camera/gallery integration + upload flow (FormData POST)
  ✅ 2.5 Manual expense entry form (full field parity + autocomplete)
  ✅ 2.3 Receipt confirmation (pending review — fetch, review, approve)
  2.4 Offline queue (IndexedDB + sync)
  ✓ Milestone: full capture flow works (online + offline)

Phase 3 ── DONE ─────────────────────────────────────────
  ✅ 3.1 Dashboard (summary cards, monthly trend, top suppliers, categories, averages, biggest expense)
  ✅ 3.2 Expense list (search, pagination, load more) + detail (read-only, line items)
  ✅ 3.3 Receipt list (status badges, pagination)
  ✅ 3.4 Reports (by-category, monthly spending, day-of-week bar chart)
  ✅ 3.5 Search (reuses expenses page with search bar)
  ✅ 3.6 More menu (receipts link, search link, language indicator, logout)
  ✅ Placeholder pages removed — all pages have real implementations
  ✓ Milestone: full review experience

Phase 4 ──────────────────────────────────────────────────
  4.1 i18n keys + language sync
  4.2 Touch optimization pass
  4.3 Loading/error states
  4.4 Session management edge cases
  ✓ Milestone: production-ready mobile web app
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Offline queue data loss (browser clears IndexedDB) | Warn user about queue size; encourage frequent sync |
| Chart library not mobile-friendly | Use simple list views as fallback; evaluate mobile chart libs |
| Large bundle size despite separate build | Tree-shake aggressively; lazy-load report/chart components |
| Session cookie not shared (different path) | Ensure cookie path is `/` (already the case with Ring defaults) |
| UA detection false positives/negatives | Use well-tested UA parsing library; log edge cases for tuning |
| Offline detection reliability | Use `navigator.onLine` + `online`/`offline` events; test on real devices |

---

## Open Questions (from spec)

1. Should offline queue entries persist across app restarts (IndexedDB) or only in-memory? **Recommendation: IndexedDB** — users expect photos to survive closing the browser.
2. How should session expiry work on mobile — silent redirect or inline message? **Recommendation: redirect to /m/login** with a brief "Session expired" message.
3. Should pending review badge poll or update on navigation? **Recommendation: check on tab navigation** + after sync completes. No polling.
4. Dashboard charts — same library as desktop or mobile-optimized? **Recommendation: evaluate during Phase 3.1** — start with list views, add charts if they render well.
5. Attach photo to existing manual expense? **Recommendation: defer** — keep V1 simple. Manual entry and receipt upload are separate flows.
