---
description: "Streamlined article mapping: batch research via Perplexity, local heuristics, taxonomy upserts, and batch alias mapping"
metadata:
  tags: ["articles", "aliases", "taxonomy", "ocr", "perplexity", "babashka"]
---

# create-articles- (streamlined)

Map raw `article_aliases` (where `article_id IS NULL`) to canonical `articles` using
**batch local heuristics + Perplexity web research** instead of per-alias Serper searches.

Key improvement over `create-articles`: the `articles_research.clj` script proposes
articles + mappings automatically. The agent **reviews and corrects** instead of
authoring from scratch.

---

## Pre-phase: Resolve Review-Required Receipts

Run this **before** touching the article alias backlog.

> **Resuming across context boundaries?** Re-verify state before acting:
> ```bash
> bb list-review-required-receipts dev --pretty   # should be []
> bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
> ```

### Step 1 — Check how many receipts need attention

```bash
bb list-review-required-receipts dev --pretty
```

If the output is **empty (`[]`)**, skip to Phase 0.

### Step 2 — Re-run OCR via REPL (uses `run-by-ids!`)

`run-by-ids!` resets each receipt to `uploaded` (clearing OCR fields and errors)
then re-runs the full pipeline with `:force-refine? true`.

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

### Step 3 — Check remaining review-required

```bash
bb list-review-required-receipts dev --pretty
bb list-review-required-receipts dev --full --pretty  # verbose, for mismatch diagnosis
```

| Root cause | Signal | Fix |
|-----------|--------|-----|
| **Lines-total mismatch** | `result :ok` + items sum != total | See Step 4 |
| **Undefined supplier** | supplier_guess nil / "unknown" | Manual UI review |
| **Bad extraction** | nil total, 0 items | Manual UI review |

### Step 4 — Fix lines-total mismatch (no re-OCR needed)

```clojure
(require '[app.domain.backend.expenses.services.receipts.approval :as approval])
(try
  (let [result
        (approval/save-review!
          (:database @system.state/state)
          <receipt-uuid>
          {:supplier_id  <supplier-uuid>
           :purchased_at "2026-01-07T11:48:00Z"   ; ISO string, NOT #inst
           :total_amount 61.44
           :currency     "BAM"
           :items        [{:raw-label "CRP" :qty 1 :unit-price 11.44 :line-total 11.44}
                          {:raw-label "Ostale usluge" :qty 1 :unit-price 50.00 :line-total 50.00}]})]
    (select-keys result [:id :status]))
  (catch Exception e {:error (ex-message e) :data (ex-data e)}))
```

---

## Response size rules (non-negotiable)

- **Never reprint file contents** — after reading any `tmp/` file, output only a short prose summary.
- **Tool output: summarise, don't repeat** — describe results in 1-5 bullet points.
- **Per-phase summaries only** — concise status lines, not alias-by-alias listings.
- **Article/mapping tables** — compact Markdown table, max 30 rows visible.

---

## Hard rules (non-negotiable)

- **Do not create new categories** — pick from the existing `categories` table only.
- **Do not use `psql` directly** — scripts call it internally; run via `bb`.
- **Subcategory names must be in Bosnian.**
- **Each distinct size/volume/weight = a separate article.** Never conflate variants.
- **Batch-first**: no alias-by-alias loops. Use batch scripts and files.
- **Temporary files** go under `tmp/`; delete them after use (`bb clear-folder`).

---

## Key normalization reference

The `db/normalize-key` function (`scripts/bb/articles/db.clj:104-123`) determines how
canonical names become article keys in the database.

**Algorithm**: NFD decomposition → strip combining marks → lowercase → non-alnum to hyphens → trim/collapse

**Diacritic transliteration** (NFD combining-mark removal):

| Input | Output | Example |
|-------|--------|---------|
| Š/š | S/s | Šećer → secer |
| Č/č | C/c | Čaj → caj |
| Ž/ž | Z/z | Žito → zito |
| Đ/đ | D/d | Đevđelija → devdelija |
| Ć/ć | C/c | Ćevapi → cevapi |

> **CRITICAL: Never write your own `normalize-key`.** Always use the output of
> `create_articles.clj --dry-run` or query `SELECT normalized_key FROM articles`
> to get actual keys. The NFD decomposition has subtle edge cases that a naive
> regex replacement will get wrong.

**Common Bosnian manufacturer patterns** (for manual review of heuristic output):

| Pattern in raw label | Manufacturer | Notes |
|---------------------|--------------|-------|
| Meggle | Meggle | Dairy |
| Dukat | Dukat | Dairy |
| Milkos | Milkos | Dairy (local) |
| Zlatna Džezva | Vispak | Coffee |
| Zvijezda | Podravka | Condiments/oil |
| Argeta | Atlantic Grupa | Pâté |
| Dunhill | BAT | Tobacco |
| Nescafé, Nescafe | Nestlé | Coffee |
| Dolcela | Podravka | Baking |
| Balea, Alverde | dm | Drugstore brands |
| Nivea | Beiersdorf | Personal care |
| Dove, Axe, Domestos | Unilever | Personal care/cleaning |

---

## Phase 0: Quick overview

**Ground-truth progress metric** (use this between phases, not the grouped script count):
```sql
SELECT COUNT(*) FILTER (WHERE article_id IS NULL) AS unmapped,
       COUNT(*) AS total
FROM article_aliases;
```

**Backlog composition** (for understanding what needs work, not for progress tracking):
```bash
bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
```

> **Why two metrics?** The script groups by `(raw_label, supplier)` — resolving one
> cross-supplier article can map dozens of aliases but barely change the grouped count.
> The SQL count tracks individual alias rows, which is the true measure of progress.

---

## Phase 1: Batch research (replaces old Phases 0-3)

This is the core improvement. One script does: cross-supplier dedup, OCR noise detection,
local heuristic resolution (brand patterns, supplier context, product keywords),
and optional Perplexity batched web research.

### Step 1a — Local heuristics only (recommended first pass)

```bash
bb articles-research dev --skip-research --pretty
```

This produces:
- `tmp/articles-suggested.edn` — articles from locally-resolved aliases
- `tmp/mappings-suggested.edn` — alias→article mappings
- `tmp/noise-candidates.edn` — OCR noise for review
- `tmp/needs-research.edn` — aliases that need web research
- `tmp/research-summary.edn` — stats

Read the summary and `tmp/needs-research.edn` to understand what local heuristics missed.

### Step 1b — Full research (with Perplexity)

```bash
bb articles-research dev --pretty
```

This runs everything from 1a plus sends unresolved aliases to Perplexity in batches of 15.
Requires `PERPLEXITY_API_KEY` in `.env`.

**Supplier filter** — process one supplier at a time for large backlogs:
```bash
bb articles-research dev --supplier "AMKO" --output-prefix amko --pretty
bb articles-research dev --supplier "APOTEKE" --output-prefix apoteke --pretty
```

`--output-prefix` prevents file overwrites between batches: output goes to
`tmp/amko-articles-suggested.edn`, `tmp/amko-mappings-suggested.edn`, etc.

> **File overwrite warning:** Each research run overwrites `tmp/articles-suggested.edn`
> and `tmp/mappings-suggested.edn`. When processing multiple suppliers sequentially,
> **create articles and map aliases immediately after each supplier batch** before
> running the next. The workflow per supplier is:
> 1. `bb articles-research dev --supplier "X" --pretty`
> 2. Review + correct `tmp/articles-suggested.edn`
> 3. Create articles (Phase 4a-4b)
> 4. Map aliases (Phase 4c)
> 5. Then proceed to next supplier

### Step 1c — Review the suggestions

Read and summarise `tmp/articles-suggested.edn`:
- Verify canonical names are sensible (especially for Đ/đ names — check normalized keys)
- Verify manufacturer assignments (branded vs Generic)
- Verify category/subcategory assignments match the DB taxonomy
- Look for variant conflicts (different sizes that should be separate articles)

**Quality audit checklist** (run after every research pass):

1. **Manufacturer coverage**: Count articles with `Generic` vs branded manufacturers.
   Target: >40% branded for consumer product backlogs. Services, parking, lab tests
   are exempt (legitimately Generic).

2. **Category/subcategory validation**: Cross-check suggested categories against:
   ```bash
   bb list-categories dev --with-subcategories --pretty
   ```
   Common mistakes: coffee capsules → "Zdravlje i apoteka", soap → "Mliječni proizvodi".

3. **OCR artifact detection**: Search for trailing hex/numbers that aren't weight/volume:
   - Trailing hex patterns: `[0-9a-f]{3,4}` at end of name (e.g., "Meggle Mlijeko 6f93")
   - Trailing bare numbers: not preceded by weight units (g, kg, ml, l, kom)
   - Short manufacturer names (≤ 2 chars): likely OCR fragments ("DJ", "MA", "RK")

4. **Near-duplicate detection**: Look for same product from different suppliers:
   - "Kefa Potrosacka Pvc" vs "Kesa Potrosacka Pvc /ko" → should be one article
   - Multiple Meggle Mlijeko 3.2% variants → merge into one

5. **Subcategory language check**: All subcategories must be in Bosnian.
   If Perplexity returned English (e.g., "Jams & Marmalades"), translate before proceeding.

**Edit `tmp/articles-suggested.edn` directly** to correct any issues before proceeding.
The file is standard EDN — same format as `create_articles.clj --articles-file` expects.

### Step 1d — Post-Perplexity quality gate (after Step 1b only)

When Perplexity was used, run these additional checks before proceeding:

- **English category names**: Perplexity sometimes returns English. Scan for non-Bosnian
  category/subcategory values and translate them.
- **Subcategory proliferation**: Check for near-duplicate subcategories
  (e.g., "Maramice" vs "Maramice i papirni proizvodi", "Čokolada i bomboni" vs
  "Čokolada i slatkiši"). Pick the existing one from the DB taxonomy.
- **Hallucinated manufacturers**: Verify manufacturer names against known brands.
  Short/nonsensical manufacturer values (≤ 2 chars, all-caps fragments) are likely
  OCR artifacts that Perplexity treated as brand names — set to "Generic".

---

## Phase 2: Check existing categories

Always verify categories before creating articles.

```bash
bb list-categories dev --with-subcategories --pretty
```

Default mappings by context:

| Context | Category | Subcategory |
|---------|----------|-------------|
| Pharmacy | Zdravlje i apoteka | (descriptive) |
| Clothing/shoes | Odjeća i modni dodaci | Opste |
| Drugstore (dm, Bipa) | Lična njega | (descriptive) |
| Dairy (mlijeko, jogurt, sir) | Mliječni proizvodi i jaja | (descriptive) |
| Bread/pastry | Pekara i deserti | (descriptive) |
| Drinks (voda, sok, pivo) | Pakovana hrana i pića | Bezalkoholna pića |
| Unknown / no fit | Ostalo | Opste |

---

## Phase 3: Variant risk check

Before creating articles, detect size/variant clusters.

```bash
bb group-aliases-by-brand dev --min-group 2
```

Look for `VARIANT RISK` clusters. Create **separate articles** per distinct size.

---

## Phase 4: Create articles + map aliases

### Step 4a — Dry-run articles (REQUIRED)

```bash
bb scripts/bb/articles/create_articles.clj dev \
  --articles-file tmp/articles-suggested.edn \
  --dry-run --pretty | tee tmp/articles-planned.edn
```

> **Inspect `normalized_key` values carefully.** Diacritics are transliterated via NFD
> decomposition (Š→S, Đ→D, č→c, ž→z, ć→c), not dropped. See "Key normalization reference"
> section above for the full algorithm. Always verify from dry-run output — never derive
> keys mentally or write your own normalize-key function.

### Step 4b — Create articles for real

```bash
bb scripts/bb/articles/create_articles.clj dev \
  --articles-file tmp/articles-suggested.edn \
  --pretty | tee tmp/created-articles.edn
```

### Step 4c — Map aliases

If the research script generated `tmp/mappings-suggested.edn`, use it directly:

```bash
bb scripts/bb/articles/map_aliases.clj dev \
  --mappings-file tmp/mappings-suggested.edn \
  --pretty | tee tmp/mapped.edn
```

> **Large backlog (> 150 mappings)**: Split into batches of ~80. Each run is safe to re-run.

> **Already-mapped aliases:** By default `map_aliases.clj` throws on the first alias
> where `article_id IS NOT NULL`. Use `--skip-mapped` to silently skip them:
> ```bash
> bb scripts/bb/articles/map_aliases.clj dev \
>   --mappings-file tmp/mappings-suggested.edn \
>   --skip-mapped --pretty | tee tmp/mapped.edn
> ```
> The output will include a `:skipped_count` field showing how many were skipped.

### Step 4d — Manual mappings (for corrections)

For aliases the script missed or got wrong, write a manual `tmp/mappings-extra.edn`:

```clojure
[{:alias-id "uuid-1"  :article-key "meggle-mlijeko-1l"}
 {:alias-id "uuid-2"  :article-key "coca-cola-125l"}]
```

```bash
bb scripts/bb/articles/map_aliases.clj dev \
  --mappings-file tmp/mappings-extra.edn \
  --pretty
```

---

## Phase 5: Handle noise + remaining unmapped

### Noise deletion

Review `tmp/noise-candidates.edn`, then delete confirmed noise:

```bash
# Dry-run first (default)
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "0 ML 4f92" \
  --raw-label "----" \
  --pretty

# Apply after confirming
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "0 ML 4f92" \
  --raw-label "----" \
  --apply --yes --pretty
```

### Remaining unmapped

```bash
bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
```

Classify each remaining alias:
- **Mappable** → re-run `articles-research` with `--supplier` filter, or research with Perplexity manually
- **OCR noise** → delete
- **Ambiguous** → document and leave unmapped

For manual Perplexity research on specific items:
```bash
bb scripts/bb/perplexity_search.clj "Product identification: 'RAW_LABEL' sold at SUPPLIER. Give: canonical name with size, manufacturer, product category"
```

---

## Phase 6: Verify and report

```bash
bb scripts/bb/articles/report_progress.clj dev --pretty | tee tmp/progress-report.edn
```

Check:
- **Coverage**: what % of aliases are now mapped
- **By-category**: `Ostalo` should be a small slice
- **By-manufacturer**: Generic < ~30% of branded-product articles
  (lab tests, services, parking, bulk produce are exempt)

---

## Completion gate (must pass before finishing)

- [ ] **Pre-phase done**: review_required receipts retried; mismatches fixed; stuck receipts documented.
- [ ] Requested backlog slice: articles created + aliases mapped.
- [ ] Variant risks addressed: no different sizes mapped to the same article.
- [ ] Taxonomy linked (manufacturer + subcategory where known).
- [ ] No subcategory named `"General"` — all subcategories are descriptive.
- [ ] `Generic` manufacturer <= ~30% of **branded-product** articles.
- [ ] `Ostalo` category used sparingly (< ~20%).
- [ ] Progress verified via `report_progress.clj`.
- [ ] Remaining unmapped aliases documented (noise vs ambiguity).
- [ ] Suggested new categories listed in Bosnian with justification.

---

## Output contract

Report after each run:

0. **Receipt resolution** — review_required count, resolved vs stuck.
1. **Summary counts** — new articles/manufacturers/subcategories created vs existing reused.
2. What was created (articles + taxonomy) and mapped (aliases -> articles).
3. How variant/size conflation was prevented.
4. Where evidence lives in `tmp/`.
5. What remains unmapped and why.
6. **Suggested new categories** (in Bosnian, with justification).

---

## Cleanup (always last)

```bash
bb clear-folder
```

---

## Key scripts reference

| Script | Command | Purpose |
|--------|---------|---------|
| `articles_research.clj` | `bb articles-research dev [--skip-research] [--supplier X]` | **NEW** — batch heuristics + Perplexity research |
| `runner/run-by-ids!` | REPL | Re-process review_required receipts |
| `approval/save-review!` | REPL | Fix items/status without re-OCR |
| `list_review_required_receipts.clj` | `bb list-review-required-receipts dev [--full]` | List review_required receipts |
| `list_categories.clj` | `bb list-categories dev [--with-subcategories]` | List categories |
| `unmapped_aliases_counts.clj` | `bb scripts/bb/articles/unmapped_aliases_counts.clj dev` | Grouped counts |
| `group_aliases_by_brand.clj` | `bb group-aliases-by-brand dev` | Variant risk detection |
| `create_articles.clj` | `bb scripts/bb/articles/create_articles.clj dev` | Create/upsert articles |
| `map_aliases.clj` | `bb scripts/bb/articles/map_aliases.clj dev` | Batch alias mapping |
| `delete_unmapped_aliases.clj` | `bb scripts/bb/articles/delete_unmapped_aliases.clj dev` | Noise deletion |
| `report_progress.clj` | `bb scripts/bb/articles/report_progress.clj dev` | Coverage report |
| `perplexity_search.clj` | `bb scripts/bb/perplexity_search.clj "query"` | Manual web research (fallback) |
