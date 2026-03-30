---
description: "Map OCR article aliases on Railway prod DB: web research, taxonomy upserts, article creation, and batch alias mapping"
metadata:
  tags: ["articles", "aliases", "taxonomy", "ocr", "serper", "babashka", "railway", "prod"]
---

# create-articles-remote-db

Map raw `article_aliases` (where `article_id IS NULL`) to canonical `articles` on the
**Railway production database**. Same workflow as `create-articles`, but scoped to
create + map operations (no REPL receipt-retry, no receipt re-OCR).

---

## REMOTE / PROD DB -- READ THIS FIRST

You are operating against the **production database**. Every write is real.

### Prerequisites

- Railway CLI installed and linked: `railway login && railway link`
- `bb` and `psql` installed locally (scripts shell out to `psql`)
- The `railway-articles-run` bb task is available (defined in `bb.edn`)

### Step 1: Switch postgres-mcp to Railway

The postgres MCP server must point to Railway so that `mcp__postgres__execute_sql`
and other postgres-mcp tools query the production database.

```bash
bb postgres-mcp-railway
```

This writes Railway connection settings to `tmp/.postgres.env`. After running it,
**restart the postgres MCP server** for the new settings to take effect.

When you are done with the remote session, switch back:

```bash
bb postgres-mcp-dev
```

### Step 2: Use `railway-articles-run` for bb scripts

All bb script invocations go through the `railway-articles-run` bb task:

```bash
bb railway-articles-run scripts/bb/articles/<script>.clj [args...]
bb railway-articles-run scripts/bb/expenses/group_aliases_by_brand.clj [args...]
```

This task automatically:
1. Fetches `DATABASE_URL` from Railway via `railway run printenv`
2. Rewrites the internal hostname to the public TCP proxy
3. Injects it as `DATABASE_PUBLIC_URL` and passes `prod` as the profile

### Two tools, one database

| Tool | Use for | Example |
|------|---------|---------|
| `mcp__postgres__execute_sql` | Ad-hoc reads, inspection, quick lookups | Check an alias, verify a mapping, count rows |
| `bb railway-articles-run` | Writes and batch operations via bb scripts | `create_articles.clj`, `map_aliases.clj`, `report_progress.clj` |

Both point to the same Railway prod database after setup.

**Do NOT** use `railway run bb ...` directly -- the internal hostname is unreachable
from your local machine.

**Do NOT** print, paste, or log `DATABASE_URL` values in this conversation.

---

## Response size rules (non-negotiable)

These apply to every phase. Violating them causes token-limit errors.

- **Never reprint file contents** -- after reading any `tmp/` file, output only a short prose summary. Do not echo raw EDN/JSON/Markdown back into the conversation.
- **Tool output: summarise, don't repeat** -- when a `bb` command returns many rows, describe the result in 1-5 bullet points. Never paste raw output verbatim unless <= 10 lines.
- **Per-phase summaries only** -- at the end of each phase write a concise status line (e.g. "Phase 0 complete: 200 aliases, 30 suppliers, 7 variant-risk groups"). Skip listing every alias by ID.
- **Article/mapping tables** -- when reporting created articles or mapped aliases, use a compact Markdown table (<= 30 rows visible; if more, state the count and note detail is in `tmp/`).

---

## Hard rules (non-negotiable)

- **Do not create new categories** -- pick from the existing `categories` table only.
- **Do not use `psql` directly** -- scripts call it internally; you run them via `bb`.
- **Subcategory names must be in Bosnian.**
- **Each distinct size/volume/weight = a separate article.** Never conflate variants.
- **Batch-first**: no alias-by-alias loops. Use `--alias-id` repeats or `--mappings-file`.
- **Temporary files** go under `tmp/`; delete them after use (`bb clear-folder`).
- **No new categories**: if a better category is missing, use `Other` / `Opste` and suggest it in the output.
- **Always `--dry-run` first** for any write operation (`create_articles.clj`, `map_aliases.clj`, `ensure_taxonomy.clj`). Only run without `--dry-run` after reviewing dry-run output.

---

## Phase 0: Triage the backlog

> **Resuming across context boundaries?** Re-verify the starting state before acting:
> ```bash
> bb railway-articles-run scripts/bb/articles/unmapped_aliases_counts.clj --pretty  # confirms backlog size
> ```
> Do not assume prior session work is still valid — aliases may have been partially mapped.

Get an overview of what's unmapped on prod.

```bash
# Quick count + supplier grouping
bb railway-articles-run scripts/bb/articles/unmapped_aliases_counts.clj --pretty

# Full backlog list
bb railway-articles-run scripts/bb/articles/list_unmapped_aliases.clj --pretty

# Save backlog for reference
# IMPORTANT: use --limit 9999 — the default (200) silently truncates large backlogs
mkdir -p tmp
bb railway-articles-run scripts/bb/articles/list_unmapped_aliases.clj --limit 9999 --pretty | tee tmp/phase1-backlog.edn

# Variant groups (required input for phase1_triage.clj)
bb railway-articles-run scripts/bb/expenses/group_aliases_by_brand.clj --json | tee tmp/phase1-variant-groups.json

# Phase-1 triage: reads tmp/phase1-backlog.edn + tmp/phase1-variant-groups.json
# NOTE: pure local, no DB -- inputs must be pre-generated above
bb scripts/bb/articles/phase1_triage.clj

# Human-readable triage report (also pure local)
bb scripts/bb/articles/phase1_triage_report.clj
```

Scan for:
- OCR noise (blank labels, digits-only, punctuation-only, < 3 alnum chars)
- High-frequency aliases that should become articles quickly
- Supplier context clues (pharmacy -> Health, clothing -> Clothing & Accessories, etc.)

---

## Phase 1: Preflight -- variant risk

Before researching or creating, detect size/variant clusters that must NOT be merged.

```bash
bb railway-articles-run scripts/bb/expenses/group_aliases_by_brand.clj --min-group 2
```

Look for `VARIANT RISK` clusters -- aliases that share a brand but differ in size (e.g. `500ml` vs `1L`).
Create **separate articles** for each distinct size.

---

## Phase 2: Check existing categories

Always pick a category from the actual table. Never assume a name.

```bash
# Flat list
bb railway-articles-run scripts/bb/articles/list_categories.clj --pretty

# With subcategories
bb railway-articles-run scripts/bb/articles/list_categories.clj --with-subcategories --pretty
```

Then pick subcategory defaults per supplier/keyword (Bosnian names):

| Context | Category | Subcategory |
|---------|----------|-------------|
| Pharmacy supplier | Health & Pharmacy | Opste |
| Clothing/shoes retailer | Clothing & Accessories | Opste |
| Drugstore (dm, Bipa) | Personal Care | Opste |
| Dairy keywords (mlijeko, jogurt, sir) | Dairy & Eggs | (descriptive) |
| Bread/pastry keywords (hljeb, pecivo) | Bakery & Desserts | (descriptive) |
| Drinks (voda, sok, pivo) | Beverages | Opste |
| Unknown / no fit | Other | Opste |

---

## Phase 3: Web research (Serper)

For each alias cluster, search before creating -- prefer canonical names from the web.

```bash
bb serper-search "ALIAS TEXT supplier context" --type web --num 5 --format pretty

# Examples:
bb serper-search "Balea Dusch-Pflege Mandelmilch dm" --format pretty
bb serper-search "Meggle Mlijeko 1L" --format pretty
```

**Manufacturer resolution rules:**
- Label text -> decode brand abbreviations.
- Supplier context -> supplier private labels are real manufacturers (Balea -> dm, K-Classic -> Kaufland).
- `manufacturer_id = NULL` (Generic) only for truly unbranded items (loose produce, services, bags).
- After batch creation, if Generic > ~30% of **branded-product** articles, do targeted searches to resolve more.
  Exclude from the count: lab/medical tests, café/restaurant services, parking fees, utility charges, bulk produce — these are structurally Generic and do not benefit from re-search.

---

## Phase 4: Create canonical articles

**Always dry-run first on prod.**

### Single article

```bash
bb railway-articles-run scripts/bb/articles/create_articles.clj \
  --canonical-name "Meggle Mlijeko 1L" \
  --manufacturer-name "Meggle" \
  --category-name "Dairy & Eggs" \
  --subcategory-name "Mlijeko" \
  --dry-run --pretty

# Review output, then run without --dry-run:
bb railway-articles-run scripts/bb/articles/create_articles.clj \
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
  :subcategory-name "Gel za tusiranje"}]
```

Then run — always dry-run first to verify `normalized_key` values before any prod writes:

```bash
# Step 1: Dry-run to preview normalized_key — REQUIRED before writing mappings.edn
bb railway-articles-run scripts/bb/articles/create_articles.clj \
  --articles-file tmp/articles.edn --dry-run --pretty | tee tmp/articles-planned.edn
# Inspect planned[*].normalized_key carefully (see Đ warning below), then:

# Step 2: Create for real
bb railway-articles-run scripts/bb/articles/create_articles.clj \
  --articles-file tmp/articles.edn --pretty | tee tmp/created-articles.edn
```

> **⚠ Đ normalization warning**: The character Đ/đ (U+0110/U+0111) is NOT NFD-decomposable —
> it is **dropped entirely** from `normalized_key`, leaving a gap.
> Examples: `"Deterđent za Suđe"` → `deter-ent-za-su-e`; `"Šećer Smeđi"` → `secer-sme-i`.
> Dž (two chars: D + ž) does decompose → `dz` (e.g. `"Džezva"` → `dzezva`).
> Always read `normalized_key` from dry-run output — never derive it mentally for names with Đ/đ.

Key facts:
- `normalized_key` is auto-derived from `canonical-name` if omitted (NFD normalization → lowercase → keep `[a-z0-9]` runs).
- Include size/volume in the name when known.
- `unit` is preserved end-to-end when present on aliases/mapping entries; the default remains `kom` only when no unit is supplied.
- Writes are idempotent per article identity (`ON CONFLICT (normalized_key, unit) DO NOTHING`) — safe to re-run.
- If you discover missing articles after the batch has run, create them in a second pass — idempotency makes it safe.

---

## Phase 5: Map aliases -> articles (batch-first)

**Always dry-run first on prod.**

### Many aliases -> same article

```bash
# Dry-run
bb railway-articles-run scripts/bb/articles/map_aliases.clj \
  --alias-id <uuid1> --alias-id <uuid2> \
  --article-key meggle-mlijeko-1l \
  --dry-run --pretty

# Then apply
bb railway-articles-run scripts/bb/articles/map_aliases.clj \
  --alias-id <uuid1> --alias-id <uuid2> \
  --article-key meggle-mlijeko-1l \
  --pretty
```

### Mixed targets -> mappings file (preferred for many articles)

**Before writing `mappings.edn`**: get the exact `normalized_key` for every article from the
dry-run output (`tmp/articles-planned.edn`) or by querying the DB for articles created earlier:

```bash
# Look up keys for pre-existing articles (not in this session's creation batch):
bb railway-articles-run scripts/bb/articles/report_progress.clj --coverage-only --pretty
# For targeted lookup, use postgres-mcp (already pointed at prod):
# SELECT normalized_key, canonical_name FROM articles WHERE canonical_name ILIKE '%<query>%' ORDER BY canonical_name LIMIT 20;
```

Write `tmp/mappings.edn`:

```clojure
[{:alias-id "uuid-1"  :article-key "meggle-mlijeko-1l"}
 {:alias-id "uuid-2"  :article-key "meggle-mlijeko-1l"}
 {:alias-id "uuid-3"  :article-key "balea-shower-gel-250ml"}
 {:raw-label "MEGGLE MLIJ" :supplier "BINGO" :article-key "meggle-mlijeko-1l"}]
```

```bash
# Dry-run
bb railway-articles-run scripts/bb/articles/map_aliases.clj \
  --mappings-file tmp/mappings.edn --dry-run --pretty

# Then apply
bb railway-articles-run scripts/bb/articles/map_aliases.clj \
  --mappings-file tmp/mappings.edn --pretty | tee tmp/mapped.edn

rm tmp/mappings.edn
```

Use `--allow-reassign` only for deliberate remaps.

> **Large backlog (> 150 aliases)**: Writing a single 200+ entry `mappings.edn` can exhaust the
> context window. Split into batches of ~80 entries by supplier group, running `map_aliases.clj`
> once per batch. Each run is safe to re-run (skips already-mapped aliases).

---

## Phase 6: Handle remaining unmapped aliases

After mapping, check what's left:

```bash
bb railway-articles-run scripts/bb/articles/unmapped_aliases_counts.clj --pretty
```

Classify each remaining alias:
- **Mappable but not yet done** -> go back to Phase 3.
- **OCR noise** (blank, digits-only, punctuation-only, < 3 alnum) -> candidate for deletion.
- **Ambiguous** (too generic, cannot determine product) -> document and leave unmapped.

Dry-run noise deletion — `--raw-label` is **required**; use labels identified by triage:

```bash
# Dry-run first (default; shows would_delete count — no writes)
bb railway-articles-run scripts/bb/articles/delete_unmapped_aliases.clj \
  --raw-label "NOISE LABEL" \
  --supplier "SUPPLIER NAME" \
  --pretty

# Repeat --raw-label for multiple noise labels in one call:
bb railway-articles-run scripts/bb/articles/delete_unmapped_aliases.clj \
  --raw-label "NOISE LABEL" \
  --raw-label "----" \
  --pretty

# Apply only after confirming would_delete count looks right
bb railway-articles-run scripts/bb/articles/delete_unmapped_aliases.clj \
  --raw-label "NOISE LABEL" \
  --supplier "SUPPLIER NAME" \
  --apply --yes --pretty
```

---

## Phase 7: Verify and report progress

```bash
bb railway-articles-run scripts/bb/articles/report_progress.clj --pretty | tee tmp/progress-report.edn
```

Check the report:
- **Coverage**: what % of aliases are now mapped.
- **By-category**: ensure `Other` is a small slice.
- **By-manufacturer**: ensure Generic is < ~30%.

---

## Completion gate (must pass before finishing)

- [ ] Requested backlog slice: articles created + aliases mapped.
- [ ] Variant risks addressed: no different sizes mapped to the same article.
- [ ] Taxonomy linked for created articles (manufacturer + subcategory where known).
- [ ] No subcategory named `"General"` -- all subcategories are descriptive.
- [ ] `Generic` manufacturer ≤ ~30% of **branded-product** articles (lab tests, services, parking, bulk produce are exempt).
- [ ] `Other` category is used sparingly.
- [ ] Progress verified via `report_progress.clj`.
- [ ] Remaining unmapped aliases documented (noise vs ambiguity).
- [ ] Suggested new categories listed in Bosnian with justification.

---

## Output contract

Report after each run:

1. **Summary counts**
   - New articles created vs existing articles reused
   - New manufacturers created vs existing reused
   - New subcategories created vs existing reused
2. What was created (articles + taxonomy) and what was mapped (aliases -> articles).
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

| Script | Command | Purpose |
|--------|---------|---------|
| `list_categories.clj` | `bb railway-articles-run scripts/bb/articles/list_categories.clj --pretty` | List categories (optionally with subcategories) |
| `list_unmapped_aliases.clj` | `bb railway-articles-run scripts/bb/articles/list_unmapped_aliases.clj --pretty` | Backlog list |
| `unmapped_aliases_counts.clj` | `bb railway-articles-run scripts/bb/articles/unmapped_aliases_counts.clj --pretty` | Grouped counts |
| `list_aliases_from_receipts.clj` | `bb railway-articles-run scripts/bb/articles/list_aliases_from_receipts.clj --pretty` | Extract raw labels from receipts |
| `list_review_required_receipts.clj` | `bb railway-articles-run scripts/bb/articles/list_review_required_receipts.clj --pretty` | Receipts in review_required status |
| `report_progress.clj` | `bb railway-articles-run scripts/bb/articles/report_progress.clj --pretty` | Coverage + taxonomy report |
| `create_articles.clj` | `bb railway-articles-run scripts/bb/articles/create_articles.clj --dry-run --pretty` | Create/upsert articles + taxonomy |
| `ensure_taxonomy.clj` | `bb railway-articles-run scripts/bb/articles/ensure_taxonomy.clj --dry-run --pretty` | Ensure manufacturers/categories/subcategories |
| `map_aliases.clj` | `bb railway-articles-run scripts/bb/articles/map_aliases.clj --dry-run --pretty` | Batch alias mapping |
| `delete_unmapped_aliases.clj` | `bb railway-articles-run scripts/bb/articles/delete_unmapped_aliases.clj --pretty` | Noise deletion (dry-run default) |
| `group_aliases_by_brand.clj` | `bb railway-articles-run scripts/bb/expenses/group_aliases_by_brand.clj --json` | Variant risk detection |
| `phase1_triage.clj` | `bb scripts/bb/articles/phase1_triage.clj` (local, no DB) | OCR noise triage |
| `phase1_triage_report.clj` | `bb scripts/bb/articles/phase1_triage_report.clj` (local, no DB) | Triage markdown report |
| `serper_search.clj` | `bb serper-search "query"` (local, no DB) | Web product research |
