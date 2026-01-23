# Supplier Resolution with Google Places API (v1) — Implementation Plan

## Goals (non-goals)

### Goals
- Use Google Places API **v1** (`places:searchText`) as a **brand-name canonicalizer** for OCR merchant strings.
- Prevent duplicate suppliers via **dual lookup**:
  1) DB lookup by normalized OCR key  
  2) Places lookup (only on miss)  
  3) DB lookup by normalized canonical key  
  4) Create supplier (idempotent)
- Never block receipt OCR processing: **Places failures must always fall back** to the OCR guess.

### Non-goals
- No address/phone/place_id storage.
- No background enrichment jobs.
- No enrichment of existing suppliers (fast path returns immediately).
- No DB schema migrations.

---

## Location bias: determining the user’s region

This app is single-tenant and the `users` table currently has no country/region fields, so “user region” should be derived **without migrations** using a strict precedence order.

### Proposed precedence order
1. **Explicit override** passed to the resolver: `opts[:user-region]` (ISO 3166-1 alpha-2, e.g. `"BA"`, `"HR"`).  
   - Use when region is known from upstream context.
2. **Instance default** from config: `config[:places :region-code]` (single-tenant default).  
   - This is the recommended default path for now.
3. **Heuristic fallback** (optional): derive from the user’s `:default_currency` in `user_expense_settings` via a config map, e.g. `{"BAM" "BA" "EUR" "HR" "USD" "US"}`.
4. If none are available: omit region bias (Places still works, but more ambiguity).

### What to send to Places (v1)
Use `regionCode` as the primary bias. Optionally add `locationBias` **only for short/ambiguous queries**.

Recommended rule:
- If `(count normalized-query) <= 4`, include `locationBias` (circle) when configured.
- Else omit `locationBias` and use only `regionCode` / `languageCode`.

Config shape (suggested; secrets must live in `config/.secrets.edn`):
```edn
:places {:api-key     #ref [:secrets :places :api-key]
         :region-code "BA"
         :language-code "bs"
         :timeout-ms  3000
         :max-results 5
         ;; optional bias for ambiguous short strings:
         :location-bias {:circle {:lat 43.8563 :lng 18.4131 :radius-m 150000.0}}}
```

Where to obtain `:user-region` in receipt processing:
- In `src/app/domain/backend/expenses/workers/receipt_ocr/extraction.clj`, the receipt has `:user_id`; the worker can fetch effective user expense settings (already done for currency) and optionally derive region using the mapping above, then pass it into supplier resolution as `opts`.

---

## Similarity algorithm (Places candidate scoring)

### Overview
We need similarity scoring that works for common OCR patterns like:
- spaced letters: `"B I N G O"` vs `"Bingo"`
- punctuation/diacritics: `"Šamon"` vs `"Samon"`
- typos: `"Wllmart"` vs `"Walmart"`

### Normalization for similarity (NOT the DB key)
Implement a dedicated normalizer for similarity scoring (do **not** reuse DB `normalized_key` rules verbatim).

Recommended `normalize-for-similarity` steps:
1. `trim`
2. fold diacritics to ASCII:
   - NFD normalize + strip combining marks (`\\p{M}+`)
   - special-case `Đ/đ` → `D/d` (doesn’t always decompose)
3. uppercase
4. replace non-letters/digits with space
5. collapse whitespace
6. **join spaced single-letter tokens**: if tokens are mostly `1-char`, join them (e.g. `"B I N G O"` → `"BINGO"`)

### Similarity metric
Use normalized Levenshtein ratio:
`score = 1 - (distance / max(len(a), len(b)))`, producing `0..1`.

### Length-adjusted acceptance thresholds
Compute length using `normalize-for-similarity` output.

| Normalized input length | Acceptance rule |
|---:|---|
| `<= 2` | **Prefix match**: candidate starts with input (case-insensitive). Also apply `regionCode` and optional `locationBias`. |
| `3–4` | Levenshtein ratio `>= 0.80` |
| `5–7` | Levenshtein ratio `>= 0.70` |
| `8+` | Levenshtein ratio `>= 0.60` |

Optional guards (low cost, improves precision):
- If the OCR guess contains digits, require the candidate to contain the same digit sequence.
- If prefix-match mode (`<=2`), require prefix match on the **joined** normalization (no whitespace).

### Candidate selection
- Score all Places candidates, pick the max score.
- Accept only if it passes the threshold for the OCR input length.
- If no candidate passes, behave as “API miss” and fall back to OCR create.

---

## Code changes (by file)

### 1) New Places client
**File**: `src/app/domain/backend/expenses/services/places_api.clj` (new)
- Implements a failure-safe wrapper around Google Places API v1 `places:searchText`.
- Uses `clj-http` with:
  - `:throw-exceptions false`
  - `:socket-timeout` / `:conn-timeout` from config
  - `X-Goog-Api-Key` and required `X-Goog-FieldMask`
- Minimal field mask for canonicalization:
  - `places.displayName` (optionally include `places.id` for debugging/log correlation)
- Returns a normalized internal shape:
  - `{:places [{:name \"Bingo\" :raw <place>} ...] :error nil}` on success
  - `{:places [] :error {:type ... :status ...}}` on any failure

### 2) Supplier resolver (OCR-only)
**File**: `src/app/domain/backend/expenses/services/suppliers.clj`
- Add `resolve-or-create-supplier-with-places! [db ocr-guess & [opts]]`
- Implements:
  - Phase 1 DB lookup using `normalize-supplier-key` → `find-by-normalized-key`
  - Phase 2 Places call (on miss) → best candidate selection via similarity scoring
  - Phase 3 DB lookup using normalized candidate name
  - Phase 4 Create:
    - attempt `(:create! service)` with chosen `display_name`
    - on SQLState `23505` (unique violation), re-`find-by-normalized-key` and return existing
- Returns `{:supplier <row> :source :db|:places-api|:ocr-fallback}`

### 3) Fix supplier normalization (diacritics)
**File**: `src/app/domain/backend/expenses/services/service_configs.clj`
- Update `normalize-supplier-key` to fold diacritics **before** applying the current `[a-z0-9\\s-]` filter.
- This prevents `"Šamon"` becoming `"amon"`.

Optional: also collapse spaced single-letter OCR patterns so `"B I N G O"` normalizes to `"bingo"`; this reduces API calls, but should be added only if it doesn’t create new false positives in real data.

### 4) Wire OCR worker to new resolver
**File**: `src/app/domain/backend/expenses/workers/receipt_ocr/extraction.clj`
- Replace `(suppliers/find-or-create-supplier! db supplier-guess ...)` with the new resolver.
- Pass Places opts (api key, region code, timeouts) from the system config into the worker pipeline.

---

## Configuration changes (no secrets in git)

1. `config/base.edn`
- Add `:places` map with:
  - `:api-key` pulled from secrets
  - `:region-code`, `:language-code`
  - `:timeout-ms`, `:max-results`
  - optional `:location-bias` circle

2. `config/.secrets.edn` (local only, not committed)
- Add `{:places {:api-key \"...\"}}`

---

## Testing plan

**File**: `test/app/domain/expenses/services/suppliers_test.clj` (extend)
- DB hit: ensures Places isn’t called (use `with-redefs` for Places function, assert not invoked).
- DB miss + Places returns canonical that exists: returns existing supplier (dedupe win).
- Places timeout/error: returns created supplier from OCR guess (fallback).
- SQLState `23505` path: simulate by `with-redefs` create fn throwing a `java.sql.SQLException` with state `23505`; assert resolver reselects and returns the existing supplier.
- Diacritics: `"Šamon"` normalizes to `"samon"` (not `"amon"`).

---

## Implementation sequence (safe, incremental)
1. Implement diacritic folding in `normalize-supplier-key` + tests.
2. Add `places_api.clj` with failure-safe HTTP and parsing.
3. Implement similarity normalization + scoring helpers (unit-tested).
4. Implement `resolve-or-create-supplier-with-places!` + tests (mock Places).
5. Wire OCR worker to call new resolver and pass config.
6. Manual smoke test in dev with real receipts.

