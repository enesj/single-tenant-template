---
description: "Map OCR article aliases to canonical products: web research, taxonomy upserts, article creation, and batch alias mapping"
metadata:
  tags: ["articles", "aliases", "taxonomy", "ocr", "serper", "babashka"]
---

# create-articles

Map raw `article_aliases` (where `article_id IS NULL`) to canonical `articles` by researching
the real product, ensuring taxonomy, creating articles, and batch-mapping aliases.

---

## Pre-phase: Resolve Review-Required Receipts

Run this **before** touching the article alias backlog. Newly re-processed receipts
may produce fresh aliases that belong in the current session's work.

> **Resuming across context boundaries?** Re-verify the starting state before acting:
> ```bash
> bb list-review-required-receipts dev --pretty   # should be []
> bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty  # confirms backlog size
> ```
> Do not assume prior session work is still valid — aliases may have been partially mapped.

### Step 1 — Check how many receipts need attention

```bash
bb list-review-required-receipts dev --pretty
```

If the output is **empty (`[]`)**, skip to Phase 0.

### Step 2 — Re-run OCR via REPL (uses `run-by-ids!`)

`run-by-ids!` resets each receipt to `uploaded` (clearing OCR fields and errors)
then re-runs the full pipeline with `:force-refine? true` — this bypasses per-user
Cerebras preferences and gives the best chance of auto-resolution.

```clojure
;; In the connected nREPL (port 7888, backend namespace)
(require '[app.domain.backend.expenses.workers.receipt-ocr.runner :as runner])

(let [db         (:database @system.state/state)
      app-config (:config   @system.state/state)
      ids        (mapv :id
                   (next.jdbc/execute!
                     db
                     ["SELECT id FROM receipts WHERE status = 'review_required'"]
                     {:builder-fn next.jdbc.result-set/as-unqualified-lower-maps}))]
  (if (seq ids)
    (runner/run-by-ids! db app-config ids)
    {:skipped true :reason "no review_required receipts"}))
```

The result map includes `:summary` (frequencies of `:result` values) and `:results`
(per-receipt detail). Key result values: `:ok`, `:skipped`, `:failed`.

### Step 3 — Check remaining review-required

```bash
# Default: shows mismatch flag + items_sum vs total_amount_guess
bb list-review-required-receipts dev --pretty

# With full items + markdown (verbose, for mismatch diagnosis)
bb list-review-required-receipts dev --full --pretty
```

Receipts still `review_required` after retry fall into three categories — diagnose
before giving up:

| Root cause | Signal | Fix |
|-----------|--------|-----|
| **Lines-total mismatch** | `result :ok` + `effective-status review_required`; items sum ≠ `total_amount_guess` | See Step 4 — fixable without re-OCR |
| **Undefined supplier** | `supplier_guess` nil or resolves to "unknown" supplier | Needs manual UI review |
| **Bad extraction** | nil `total_amount_guess`, 0 items, no supplier | Needs manual UI review or better image |

### Step 4 — Fix lines-total mismatch (no re-OCR needed)

When items sum ≠ total but the receipt otherwise looks good:

**4a. Check `parsed_markdown`** — LlamaParse often captures items that the JSON
extractor missed. If the missing amount is explained by items in the markdown, add
them with `save-review!`.

**4b. If markdown is incomplete**, add a catch-all item for the gap (e.g
`"Ostale laboratorijske usluge"` for a lab receipt).

```clojure
;; save-review! gotchas (learned in session):
;; - :purchased_at MUST be ISO-8601 string or java.time.Instant — NOT #inst (java.util.Date)
;; - items MUST use kebab-case keys: :line-total, :raw-label, :unit-price
;; - wrap in try/catch — multi-expression REPL evals swallow exceptions silently
;; - returns the receipt row; :status "extracted" when items sum matches total within 0.01

(require '[app.domain.backend.expenses.services.receipts.approval :as approval])
(try
  (let [result
        (approval/save-review!
          (:database @system.state/state)
          <receipt-uuid>
          {:supplier_id  <supplier-uuid>    ; get from supplier_aliases WHERE id = receipt.supplier_alias_id
           :purchased_at "2026-01-07T11:48:00Z"   ; ISO string, NOT #inst
           :total_amount 61.44
           :currency     "BAM"
           :items        [{:raw-label "CRP" :qty 1 :unit-price 11.44 :line-total 11.44}
                          {:raw-label "Ostale usluge" :qty 1 :unit-price 50.00 :line-total 50.00}]})]
    (select-keys result [:id :status]))
  (catch Exception e {:error (ex-message e) :data (ex-data e)}))
```

To get supplier UUIDs for the call:
```sql
SELECT id, supplier_id FROM supplier_aliases WHERE id = '<receipt.supplier_alias_id>';
```

Receipts that are **truly unresolvable** (nil supplier, bad image, missing Cerebras key)
need manual UI review and are out of scope for this skill.

---

## Response size rules (non-negotiable)

These apply to every phase. Violating them causes token-limit errors.

- **Never reprint file contents** — after reading any `tmp/` file (triage report, backlog, progress report, etc.), output only a short prose summary of the key findings. Do not echo the raw EDN/JSON/Markdown back into the conversation.
- **Tool output: summarise, don't repeat** — when a `bb` or SQL command returns many rows, describe the result in 1–5 bullet points. Never paste the raw output verbatim unless it is ≤ 10 lines.
- **Per-phase summaries only** — at the end of each phase write a concise status line (e.g. "Phase 0 complete: 200 aliases, 30 suppliers, 7 variant-risk groups, 0 noise"). Skip listing every alias by ID in prose.
- **Article/mapping tables** — when reporting created articles or mapped aliases, use a compact Markdown table (≤ 30 rows visible; if more, state the count and note that detail is in `tmp/`).

---

## Hard rules (non-negotiable)

- **Do not create new categories** — pick from the existing `categories` table only.
- **Do not use `psql` directly** — scripts call it internally; you run them via `bb`.
- **Subcategory names must be in Bosnian.**
- **Each distinct size/volume/weight = a separate article.** Never conflate variants.
- **Batch-first**: no alias-by-alias loops. Use `--alias-id` repeats or `--mappings-file`.
- **Temporary files** go under `tmp/`; delete them after use (`bb clear-folder`).
- **No new categories**: if a better category is missing, use `Other` / `Opšte` and suggest it in the output.

---

## Phase 0: Triage the backlog

Get an overview before acting.

```bash
# What's unmapped? (quick count + supplier grouping)
bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty

# Full backlog list (first 200 by default)
bb scripts/bb/articles/list_unmapped_aliases.clj dev --pretty

# Save backlog for reference during the session
# IMPORTANT: use --limit 9999 — the default (200) silently truncates large backlogs
mkdir -p tmp
bb scripts/bb/articles/list_unmapped_aliases.clj dev --limit 9999 --pretty | tee tmp/phase1-backlog.edn

# Variant groups (required input for phase1_triage.clj)
bb group-aliases-by-brand dev --json | tee tmp/phase1-variant-groups.json

# Phase-1 triage: reads tmp/phase1-backlog.edn + tmp/phase1-variant-groups.json
# NOTE: takes NO command-line args — inputs must be pre-generated above
bb scripts/bb/articles/phase1_triage.clj

# Human-readable triage report
bb scripts/bb/articles/phase1_triage_report.clj
```

Scan for:
- OCR noise (blank labels, digits-only, punctuation-only, < 3 alnum chars)
- High-frequency aliases that should become articles quickly
- Supplier context clues (pharmacy → Health, clothing → Clothing & Accessories, etc.)

---

## Phase 1: Preflight — variant risk

Before researching or creating, detect size/variant clusters that must NOT be merged.

```bash
bb group-aliases-by-brand dev --min-group 2
```

Look for `VARIANT RISK` clusters — aliases that share a brand but differ in size (e.g. `500ml` vs `1L`).
Create **separate articles** for each distinct size. Do not map them to the same `normalized_key`.

---

## Phase 2: Check existing categories

Always pick a category from the actual table. Never assume a name.

```bash
# Flat list
bb list-categories dev --pretty

# With subcategories nested under each category
bb list-categories dev --with-subcategories --pretty
```

Then pick subcategory defaults per supplier/keyword (Bosnian names):

| Context | Category | Subcategory |
|---------|----------|-------------|
| Pharmacy supplier | Health & Pharmacy | Opšte |
| Clothing/shoes retailer | Clothing & Accessories | Opšte |
| Drugstore (dm, Bipa) | Personal Care | Opšte |
| Dairy keywords (mlijeko, jogurt, sir) | Dairy & Eggs | (descriptive) |
| Bread/pastry keywords (hljeb, pecivo) | Bakery & Desserts | (descriptive) |
| Drinks (voda, sok, pivo) | Beverages | Opšte |
| Unknown / no fit | Other | Opšte |

---

## Phase 3: Web research (Serper)

For each alias cluster, search before creating — prefer canonical names from the web.

```bash
bb serper-search "ALIAS TEXT supplier context" --type web --num 5 --format pretty

# Examples:
bb serper-search "Balea Dusch-Pflege Mandelmilch dm" --format pretty
bb serper-search "Meggle Mlijeko 1L" --format pretty
```

**Manufacturer resolution rules:**
- Label text → decode brand abbreviations.
- Supplier context → supplier private labels are real manufacturers (Balea → dm, K-Classic → Kaufland).
- `manufacturer_id = NULL` (Generic) only for truly unbranded items (loose produce, services, bags).
- After batch creation, if Generic > ~30% of **branded-product** articles, do targeted searches to resolve more.
  Exclude from the count: lab/medical tests, café/restaurant services, parking fees, utility charges, bulk produce — these are structurally Generic and do not benefit from re-search.

---

## Phase 4: Create canonical articles

### Single article

```bash
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Meggle Mlijeko 1L" \
  --manufacturer-name "Meggle" \
  --category-name "Dairy & Eggs" \
  --subcategory-name "Mlijeko" \
  --pretty
```

### Batch via EDN file (preferred for > 2 articles)

Write `tmp/articles.edn` as an EDN vector of maps:

```clojure
[{:canonical-name "Meggle Mlijeko 1L"
  :manufacturer-name "Meggle"
  :manufacturer-key "meggle"
  :category-name "Dairy & Eggs"
  :subcategory-name "Mlijeko"}

 {:canonical-name "Balea Shower Gel 250ml"
  :manufacturer-name "Balea"
  :manufacturer-key "balea"
  :category-name "Personal Care"
  :subcategory-name "Gel za tuširanje"}

 {:canonical-name "Hljeb Bijeli 500g"
  :category-name "Bakery & Desserts"
  :subcategory-name "Hljeb"}]
```

Then run — **always dry-run first** to verify the `normalized_key` values before committing:

```bash
# Step 1: Dry-run to preview normalized_key — REQUIRED before writing mappings.edn
bb scripts/bb/articles/create_articles.clj dev \
  --articles-file tmp/articles.edn \
  --dry-run --pretty | tee tmp/articles-planned.edn
# Inspect planned[*].normalized_key carefully (see Đ warning below), then:

# Step 2: Create for real
bb scripts/bb/articles/create_articles.clj dev \
  --articles-file tmp/articles.edn \
  --pretty | tee tmp/created-articles.edn
```

> **⚠ Đ normalization warning**: The character Đ/đ (U+0110/U+0111) is NOT NFD-decomposable —
> it is **dropped entirely** from `normalized_key`, leaving a gap.
> Examples: `"Deterđent za Suđe"` → `deter-ent-za-su-e`; `"Šećer Smeđi"` → `secer-sme-i`.
> Dž (two chars: D + ž) does decompose → `dz` (e.g. `"Džezva"` → `dzezva`).
> Always read `normalized_key` from dry-run output — never derive it mentally for names with Đ/đ.

Key facts:
- `normalized_key` is auto-derived from `canonical-name` if omitted (NFD normalization → lowercase → keep `[a-z0-9]` runs).
- Include size/volume in the name when known (`"Mlijeko 1L"` not just `"Mlijeko"`).
- Writes are idempotent (`ON CONFLICT (normalized_key) DO NOTHING`) — safe to re-run.
- If you discover missing articles after the batch has run, create them in a second pass — idempotency makes it safe.

**Conflict flags** (use only for intentional overwrites):
- `--update-manufacturer-name`
- `--update-category-description`
- `--update-subcategory-description`

---

## Phase 5: Map aliases → articles (batch-first)

### Many aliases → same article

```bash
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id <uuid1> \
  --alias-id <uuid2> \
  --alias-id <uuid3> \
  --article-key meggle-mlijeko-1l \
  --pretty
```

### Mixed targets → mappings file (preferred for many articles)

**Before writing `mappings.edn`**: get the exact `normalized_key` for every article from the
dry-run output (`tmp/articles-planned.edn`) or by querying the DB for articles created earlier:

```bash
# Look up keys for pre-existing articles (not in this session's creation batch):
bb scripts/bb/articles/report_progress.clj dev --coverage-only --pretty
# For targeted lookup, use postgres-mcp:
# SELECT normalized_key, canonical_name FROM articles WHERE canonical_name ILIKE '%<query>%' ORDER BY canonical_name LIMIT 20;
```

Write `tmp/mappings.edn` as an EDN vector using the confirmed keys:

```clojure
[{:alias-id "uuid-1"  :article-key "meggle-mlijeko-1l"}
 {:alias-id "uuid-2"  :article-key "meggle-mlijeko-1l"}
 {:alias-id "uuid-3"  :article-key "balea-shower-gel-250ml"}
 {:alias-id "uuid-4"  :article-key "hljeb-bijeli-500g"}
 {:raw-label "MEGGLE MLIJ" :supplier "BINGO" :article-key "meggle-mlijeko-1l"}]
```

Then run one call:

```bash
bb scripts/bb/articles/map_aliases.clj dev \
  --mappings-file tmp/mappings.edn \
  --pretty | tee tmp/mapped.edn

rm tmp/mappings.edn
```

Use `--allow-reassign` only for deliberate remaps. The default is safe: only fills `article_id IS NULL` aliases.

> **Large backlog (> 150 aliases)**: Writing a single 200+ entry `mappings.edn` can exhaust the
> context window. Split into batches of ~80 entries by supplier group, running `map_aliases.clj`
> once per batch. Each run is safe to re-run (skips already-mapped aliases).

---

## Phase 6: Handle remaining unmapped aliases

After mapping, check what's left:

```bash
bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
```

Classify each remaining alias:
- **Mappable but not yet done** → go back to Phase 3.
- **OCR noise** (blank, digits-only, punctuation-only, < 3 alnum) → candidate for deletion.
- **Ambiguous** (too generic, cannot determine product) → document and leave unmapped.

Dry-run noise deletion — `--raw-label` is **required**; use labels identified by triage:

```bash
# Dry-run first (default; shows would_delete count — no writes)
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "0 ML 4f92" \
  --supplier "SUPPLIER NAME" \
  --pretty

# Repeat --raw-label for multiple noise labels in one call:
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "0 ML 4f92" \
  --raw-label "----" \
  --pretty

# Apply only after confirming would_delete count looks right
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "0 ML 4f92" \
  --supplier "SUPPLIER NAME" \
  --apply --yes --pretty
```

---

## Phase 7: Verify and report progress

```bash
bb scripts/bb/articles/report_progress.clj dev --pretty | tee tmp/progress-report.edn
```

Check the report:
- **Coverage**: what % of aliases are now mapped.
- **By-category**: ensure `Other` is a small slice. If it's large, propose new categories.
- **By-manufacturer**: ensure Generic is < ~30%. If higher, do more targeted Serper searches.

---

## Completion gate (must pass before finishing)

- [ ] **Pre-phase done**: `review_required` receipts retried; lines-total mismatches fixed via `save-review!`; truly unresolvable receipts documented with root cause.
- [ ] Requested backlog slice: articles created + aliases mapped.
- [ ] Variant risks addressed: no different sizes mapped to the same article.
- [ ] Taxonomy linked for created articles (manufacturer + subcategory where known).
- [ ] No subcategory named `"General"` — all subcategories are descriptive.
- [ ] `Generic` manufacturer ≤ ~30% of **branded-product** articles (lab tests, services, parking, bulk produce are exempt).
- [ ] `Other` category is used sparingly — if > ~20% of articles, suggest better categories.
- [ ] Progress verified via `report_progress.clj`.
- [ ] Remaining unmapped aliases are documented (noise vs ambiguity).
- [ ] Suggested new categories listed in Bosnian with justification.

---

## Output contract

Report after each run:

0. **Receipt resolution** — how many `review_required` receipts were found, how many resolved via OCR retry (`:ok`), how many fixed via `save-review!` (lines-total mismatch), how many are still stuck and why (undefined supplier / bad image).
1. **Summary counts**
   - New articles created vs existing articles reused
   - New manufacturers created vs existing reused
   - New subcategories created vs existing reused
2. What was created (articles + taxonomy) and what was mapped (aliases → articles).
3. How variant/size conflation was prevented.
4. Where evidence lives in `tmp/` (progress report, created-articles EDN, etc.).
5. What remains unmapped and why (noise vs ambiguity).
6. **Suggested new categories** (in Bosnian, with justification) for items that fell into `Other`.

---

## Cleanup (always last)

```bash
bb clear-folder
```

Deletes all files under `tmp/`.

---

## Key scripts reference

| Script / Function | Command | Purpose |
|-------------------|---------|---------|
| `runner/run-by-ids!` | REPL: `(runner/run-by-ids! db app-config ids)` | Re-process review_required receipts |
| `approval/save-review!` | REPL: `(approval/save-review! db receipt-id review-data)` | Fix items/status without re-OCR; `:purchased_at` = ISO string; items = kebab-case keys |
| `list_review_required_receipts.clj` | `bb list-review-required-receipts dev [--full]` | List review_required receipts with mismatch diagnosis |
| `list_categories.clj` | `bb list-categories dev [--with-subcategories]` | List categories (and optionally subcategories) |
| `list_unmapped_aliases.clj` | `bb scripts/bb/articles/list_unmapped_aliases.clj dev` | Backlog list |
| `unmapped_aliases_counts.clj` | `bb scripts/bb/articles/unmapped_aliases_counts.clj dev` | Grouped counts |
| `phase1_triage.clj` | `bb scripts/bb/articles/phase1_triage.clj` (no args — reads from `tmp/`) | OCR noise triage |
| `phase1_triage_report.clj` | `bb scripts/bb/articles/phase1_triage_report.clj` | Triage markdown report |
| `group_aliases_by_brand.clj` | `bb group-aliases-by-brand dev` | Variant risk detection |
| `create_articles.clj` | `bb scripts/bb/articles/create_articles.clj dev` | Create/upsert articles + taxonomy |
| `map_aliases.clj` | `bb scripts/bb/articles/map_aliases.clj dev` | Batch alias mapping |
| `delete_unmapped_aliases.clj` | `bb scripts/bb/articles/delete_unmapped_aliases.clj dev --raw-label "X"` | Noise deletion (dry-run default) |
| `report_progress.clj` | `bb scripts/bb/articles/report_progress.clj dev` | Coverage + taxonomy report |
| `serper_search.clj` | `bb serper-search "query"` | Web product research |
