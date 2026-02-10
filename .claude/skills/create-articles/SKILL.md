---
name: create-articles
description: "Map article aliases extracted from receipt OCR data to canonical products via web search and database entry"
metadata:
  tags: ["articles", "article-aliases", "web-search", "database", "products", "receipts", "ocr", "mapping"]
allowed-tools:
  - postgres:*

---

# create-articles

## Goal
Map raw article aliases extracted from receipt OCR data to canonical products by finding actual products via web search, then create database entries for articles and related taxonomy (manufacturers + subcategories under existing categories), and finally map aliases to articles.

## When to use
- You need to populate the articles table with actual products found from web search
- Article aliases from receipts need to be mapped to canonical articles
- You have OCR-extracted labels (raw_label) that need to be resolved to real products
- Building product catalog from receipt data

### How this fits the “resolve during extraction” approach

Receipt extraction already creates `article_aliases` for each line item (when the label is valid). With the “resolve during extraction” approach, you typically want receipt pages to show canonical articles **as early as possible**.

This skill is what makes that possible:

- First, extraction gives you the backlog: `article_aliases` rows with `article_id IS NULL`.
- Then you run this skill to create canonical `articles` (+ taxonomy) and set `article_aliases.article_id`.
- After that, anything that renders a receipt/expense and joins through `article_aliases` can show canonical article info.

**Recommendation (to avoid staleness):** persist the stable `article_aliases.id` on receipt-derived items (or keep the raw label + supplier_id) and derive the current `articles` mapping by joining `article_aliases.article_id` at read time. Only store an `article_id` “snapshot” on receipts if you also plan a backfill/update strategy when mappings change.

This skill is also the right place to backfill/maintain the *taxonomy* tables used by articles:
- `manufacturers` (canonical brands)
- `categories` and `subcategories` (FK-driven categorization)

## Prerequisites
- Babashka (`bb`) must be available
- `psql` must be available on your PATH (these scripts shell out to `psql` for DB access)
- Postgres must be reachable using the DB settings in `config/base.edn` (profile `:dev` / `:test`)
- Web search: use `bb serper-search` (`scripts/bb/web/serper_search.clj`) **only** when `ENABLE_SERPER_SEARCH=true` (in `.env`). When disabled, skip web search entirely and create best-effort generic articles from the OCR labels.
- If Serper is enabled: Serper.dev Search API must be configured via `SERPER_API_KEY` (typically sourced from `.env`)
- Database connection configured in `config/base.edn`
- Receipts with `raw_extract_json` data containing article items

## Process Overview

1. **Extract unmapped article aliases** from receipts
2. **Web search** each alias to find actual products
3. **Upsert taxonomy** (manufacturers, categories, subcategories)
4. **Create canonical articles** (linking to taxonomy via FK columns)
5. **Map aliases to articles** via article_id
6. **Identify OCR noise** and optionally remove it

---

## Step 1: Extract Article Aliases from Receipts

Preferred (fast + deterministic): query the `article_aliases` backlog created by extraction.

```bash
# List unmapped aliases (default limit 200)
bb scripts/bb/articles/list_unmapped_aliases.clj dev --pretty

# Increase/adjust limit
bb scripts/bb/articles/list_unmapped_aliases.clj dev --limit 1000 --pretty

# Include already-mapped aliases too
bb scripts/bb/articles/list_unmapped_aliases.clj dev --all --limit 200 --pretty
```

Alternative - query directly from receipts:

```bash
# Fallback: extract distinct raw_label values directly from receipts.raw_extract_json
bb scripts/bb/articles/list_aliases_from_receipts.clj dev --limit 500 --pretty
```

---

## Step 2: Web Search for Products

Web search uses **only** `bb serper-search` (`scripts/bb/web/serper_search.clj`). .

If not enabled, skip to Step 3 and create generic articles without web-sourced metadata.

### Serper search usage

When enabled, use `bb serper-search` to find product info:

```bash
# Basic search
bb serper-search "KEKS RONDINI LASTA" --gl BA --hl bs --num 5 --format pretty

# Focus on a specific retailer site
bb serper-search "RONDINI keks" --site vocar.ba --gl BA --num 5

# Get machine-readable output
bb serper-search "KEKS RONDINI LASTA" --gl BA --hl bs --num 5 --format edn
```

From the search results, extract:
- canonical name (with weight/size)
- manufacturer (brand)
- category + subcategory (when reasonably confident)
- product page link

### Tips for effective searches
- Include supplier name in queries for better results
- Use local language (Bosnian/Croatian/Serbian) for local products
- Use `--site` to target known retailers (e.g. `vocar.ba`, `konzum.ba`)
- Use `--gl BA --hl bs` for Bosnia-specific results
- Use `--format edn` for structured output when processing programmatically

---

## Step 3: Upsert Taxonomy (Manufacturers, Categories, Subcategories)

Articles should reference taxonomy via foreign keys:
- `articles.manufacturer_id` → `manufacturers.id`
- `articles.subcategory_id` → `subcategories.id` (and subcategory points to a `category_id`)

If you have taxonomy info from web search, **insert it first** and then reference the IDs from the article.

### Category Policy (Hard Rule)
- Do **not** create new categories while mapping articles.
- Categories are pre-seeded and must be treated as fixed.
- Creating new subcategories under existing categories is allowed.
- Subcategory names must be in English.
- If no adequate existing category is available, put the article in category `Other`.
- Use only category names that already exist in the `categories` table; never add categories from this workflow.
- `Food` is the only food-related top-level category. For food and beverage items, always use top-level category `Food` with an English subcategory (for example `Snacks`, `Carbonated Drinks`, `Dairy`).
- If research suggests a category that is not present in the `categories` table, map the article to `Other` and create an English subcategory (for example `General`) under `Other`.

By default, taxonomy upsert scripts preserve existing canonical values on conflicts. Use explicit update flags only when you intentionally want to overwrite existing values:
- `--update-manufacturer-name`
- `--update-category-description`
- `--update-subcategory-description`

### 3.1 Upsert Manufacturer

Use a stable `normalized_key` (lowercase, hyphenated, ASCII-only) for deduplication.

```bash
# Ensure just a manufacturer (returns its id)
bb scripts/bb/articles/ensure_taxonomy.clj dev --manufacturer-name "Lasta" --manufacturer-key "lasta" --pretty
```

### 3.2 Select Existing Category (Do Not Create)

```bash
# Inspect available categories (pick one; do not create new ones)
bb scripts/bb/articles/report_progress.clj dev --pretty
```

```sql
-- Direct DB check when needed (run via mcp__postgres__execute_sql)
SELECT name FROM categories ORDER BY name;
```

### 3.3 Upsert Subcategory

Subcategories are unique by `(category_id, name)`.
Use English names only (for example: `Tea`, `Carbonated Drinks`, `General`).

```bash
# Ensure category + subcategory together (also returns their ids)
bb scripts/bb/articles/ensure_taxonomy.clj dev \
  --category-name "Food" \
  --subcategory-name "Snacks" \
  --subcategory-description "Chips, biscuits, sweets" \
  --pretty
```

If you *don’t* have reliable taxonomy, keep `manufacturer_id` optional, but use category `Other` (with an `Other` subcategory such as `General`) instead of creating a new category.

---

## Step 4: Create Canonical Articles

If you could not find reliable product info via web search, create a best-effort *generic* canonical article from the OCR label:
- `canonical_name`: cleaned-up `raw_label` (remove receipt noise / extra punctuation; keep grams/ml when present)
- `link`: NULL
- `manufacturer_id`: NULL
- `subcategory_id`: point to a subcategory under `Other` (create one if needed, e.g. `General`)

Insert articles into the database with web search results (when available) or generic values (when not).

### Article Schema

Key columns (simplified):

- `id` (UUID, PK)
- `canonical_name` (string, required)
- `normalized_key` (string, required, unique)
- `category` (legacy free-text, optional)
- `subcategory_id` (UUID, FK → `subcategories.id`, preferred)
- `manufacturer_id` (UUID, FK → `manufacturers.id`, optional)
- `link` (text, optional)

### Insert Article (Single)
```bash
# Create an article and ensure taxonomy in the same call.
# Output includes :created? and the resolved IDs.
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Lasta Rondini keks 200g" \
  --normalized-key "lasta-rondini-keks-200g" \
  --manufacturer-name "Lasta" \
  --manufacturer-key "lasta" \
  --category-name "Food" \
  --subcategory-name "Snacks" \
  --link "https://lasta.com/lasta-product/rondini-limun-i-mak/" \
  --pretty
```

### Insert Articles in Batch (Faster)
```bash
mkdir -p tmp
cat >tmp/articles-batch.edn <<'EDN'
[{:canonical-name "Lasta Rondini keks 200g"
  :manufacturer-name "Lasta"
  :category-name "Food"
  :subcategory-name "Snacks"
  :link "https://example.com/rondini"}
 {:canonical-name "Vreće za smeće 60L crne"
  :normalized-key "vrece-za-smece-60l-crne"
  :category-name "Other"
  :subcategory-name "General"}]
EDN

bb scripts/bb/articles/create_articles.clj dev \
  --articles-file tmp/articles-batch.edn \
  --pretty
```

`create_articles.clj` supports the same explicit taxonomy overwrite flags as `ensure_taxonomy.clj`: `--update-manufacturer-name`, `--update-category-description`, and `--update-subcategory-description`.
Do not use it to introduce new categories; category names must come from the existing category set.

### Insert Article + Taxonomy in One Go (CTE)

You don’t need CTEs for this workflow anymore—`create_articles.clj` and `ensure_taxonomy.clj` handle the deterministic upsert/select pattern.

### Normalization Rules
- Lowercase: `Lasta Rondini` → `lasta-rondini`
- Strip diacritics first (e.g. `Š` → `S`), then replace non `[a-z0-9]` runs with `-`
- Replace spaces with hyphens
- Keep it URL-safe

---

## Step 5: Map Aliases to Articles

Update article_aliases to link raw OCR labels to canonical articles.

### Map Aliases (Single or Batch)
Prefer mapping by stable `alias_id` to avoid ambiguity. `map_aliases.clj` accepts one or more `--alias-id` values and exactly one article selector.

```bash
# 1) List unmapped aliases to get alias_id
bb scripts/bb/articles/list_unmapped_aliases.clj dev --limit 200 --pretty

# 2) Map a specific alias_id to an article (by normalized_key)
#    Default behavior updates only currently unmapped aliases (article_id IS NULL).
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id "<ALIAS_UUID>" \
  --article-key "lasta-rondini-keks-200g" \
  --pretty

# 3) Batch-map multiple alias_ids to the same article
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id "<ALIAS_UUID_1>" \
  --alias-id "<ALIAS_UUID_2>" \
  --alias-id "<ALIAS_UUID_3>" \
  --article-key "lasta-rondini-keks-200g" \
  --pretty

# 4) Intentionally remap already mapped aliases
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id "<ALIAS_UUID>" \
  --article-key "lasta-rondini-keks-200g" \
  --allow-reassign \
  --pretty
```

### Required Batch Mode (Important)
- Default operating mode is **one canonical article variant → many alias IDs in one `map_aliases.clj` call**.
- Do **not** run `map_aliases.clj` in a one-by-one alias loop when aliases resolve to the same article variant.
- Create/update the canonical article once, then map all matching alias IDs for that variant in a single command (or chunked commands).

### Practical Batch Recipe
```bash
# 1) Create canonical article once
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Lasta Rondini keks 200g" \
  --normalized-key "lasta-rondini-keks-200g" \
  --pretty

# 2) Collect alias IDs for that same product variant (one UUID per line)
mkdir -p tmp
cat >tmp/lasta-rondini.alias-ids.txt <<'EOF'
<ALIAS_UUID_1>
<ALIAS_UUID_2>
<ALIAS_UUID_3>
EOF

# 3) Map them in one call by repeating --alias-id
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id "<ALIAS_UUID_1>" \
  --alias-id "<ALIAS_UUID_2>" \
  --alias-id "<ALIAS_UUID_3>" \
  --article-key "lasta-rondini-keks-200g" \
  --pretty
```

### Batch Throughput Tips
Use repeated `--alias-id` in one command to reduce process/DB overhead. For very large batches, run in chunks (e.g. 50-200 alias IDs per call). Prefer alias IDs over raw labels to avoid “same text, different supplier” collisions.

---

## Step 6: Handle Generic Products

For items without specific brands, create generic articles with descriptive names.

### Generic Article Examples
```bash
# Generic aluminum containers
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Aluminijumska posuda za teletinu 500ml" \
  --normalized-key "aluminijumska-posuda-teletina-500ml" \
  --category-name "Other" \
  --subcategory-name "General" \
  --pretty

# Generic garbage bags
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Vreće za smeće 60L crne" \
  --normalized-key "vrace-smece-60l-crne" \
  --category-name "Other" \
  --subcategory-name "General" \
  --pretty
```

---

## Step 7: Identify and Remove OCR Noise

After mapping valid products, identify remaining unmapped aliases that are OCR noise.

### Common OCR Noise Patterns
- Receipt metadata: `"PARTICA:"` (savings), `"POPRAT"` (total), `"POU:"` (change)
- Discount info: `"POPUST -10,00%:"`, `"CSN. E:"`
- Entry formats: `"ENTRY 1350M: NAR"`, `"2,000x 4,80"`
- Fragments: `"KONICA:"`, `"KONTIČ:"`, `"ROVRAT:"`, `"UJIPNO"`, `"UJUPNO"`

### Query Unmapped Aliases
```bash
# Check remaining unmapped aliases (grouped + counted)
bb scripts/bb/articles/unmapped_aliases_counts.clj dev --limit 200 --pretty
```

### Remove OCR Noise (Optional)
```bash
# Dry-run first (default)
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "PARTICA:" \
  --raw-label "POPRAT" \
  --raw-label "POU:" \
  --raw-label "POPUST -10,00%:" \
  --raw-label "CSN. E:" \
  --pretty

# Apply (will prompt unless --yes)
bb scripts/bb/articles/delete_unmapped_aliases.clj dev \
  --raw-label "PARTICA:" \
  --raw-label "POPRAT" \
  --raw-label "POU:" \
  --raw-label "POPUST -10,00%:" \
  --raw-label "CSN. E:" \
  --apply
```

---

## Step 8: Verify and Report Progress

Track mapping progress and final statistics.

### Coverage Statistics
```bash
bb scripts/bb/articles/report_progress.clj dev --pretty

# Coverage only
bb scripts/bb/articles/report_progress.clj dev --coverage-only --pretty
```

### Save Output to File (Use `tee`)
```bash
mkdir -p tmp
# Save full progress output while still printing to terminal
bb scripts/bb/articles/report_progress.clj dev --pretty 2>&1 | tee tmp/articles-progress-$(date +%Y%m%d-%H%M%S).txt

# Save coverage-only output while still printing to terminal
bb scripts/bb/articles/report_progress.clj dev --coverage-only --pretty 2>&1 | tee tmp/articles-coverage-$(date +%Y%m%d-%H%M%S).txt
```

Use this pattern instead of plain `>` redirection when saving output.

### Articles by Category
Included in `report_progress.clj` output under `:by-category`.

### Articles by Manufacturer
Included in `report_progress.clj` output under `:by-manufacturer`.

---

## Complete Example Workflow

```bash
# 1) Get unmapped aliases (backlog)
bb scripts/bb/articles/list_unmapped_aliases.clj dev --limit 50 --pretty

# 2) For a given raw label, optionally web-search (only when ENABLE_SERPER_SEARCH=true)
bb serper-search "KEKS RONDINI LASTA" --gl BA --hl bs --num 5 --format pretty

# 3) Create the canonical article (+ ensure taxonomy)
bb scripts/bb/articles/create_articles.clj dev \
  --canonical-name "Lasta Rondini keks 200g" \
  --manufacturer-name "Lasta" \
  --category-name "Food" \
  --subcategory-name "Snacks" \
  --link "https://example.com/product" \
  --pretty

# 4) Map aliases (prefer alias_id; repeat --alias-id for batch)
bb scripts/bb/articles/map_aliases.clj dev \
  --alias-id "<ALIAS_UUID_1>" \
  --alias-id "<ALIAS_UUID_2>" \
  --alias-id "<ALIAS_UUID_3>" \
  --article-key "lasta-rondini-keks-200g" \
  --pretty

# 5) Check progress
bb scripts/bb/articles/report_progress.clj dev --pretty
```

---

## Tips & Best Practices

### Web Search Tips (Serper only)
- Only search when `ENABLE_SERPER_SEARCH=true`; otherwise create generic articles
- Include supplier name in queries for better results
- Use local language (Bosnian/Croatian/Serbian) for local products
- Use `--site` to focus on known retailers or manufacturer sites
- Use `--gl BA --hl bs` for Bosnia-specific results
- Check multiple sources to verify product details

### Database Tips
- Always use `ON CONFLICT DO NOTHING` when inserting articles
- Use normalized_key for unique constraints
- Keep canonical names descriptive but consistent
- Include manufacturer when known, NULL for generic items
- Never create new categories during mapping; pick an existing one, and use `Other` when no fit exists
- Use English names for all newly created subcategories
- Taxonomy upserts preserve existing canonical values by default; use explicit `--update-*` flags only for intentional overwrites
- `map_aliases.clj` updates only unmapped aliases by default; repeat `--alias-id` for batch updates and use `--allow-reassign` only for deliberate remaps
- Batch-first rule: group aliases by target canonical article and map each group in one command instead of alias-by-alias loops.

### Size & Variant Differentiation (critical)
- **Each distinct size/volume/weight is a separate article.** Never map aliases with different sizes to the same article (e.g. Coca-Cola 0.25L, 1.25L, 2L must be three articles).
- Extract size clues from the alias (`2L`, `0,25`, `1 25L`, `025`, `500G`, etc.) and from the supplier context.
- **Supplier type matters:** a restaurant/fast-food supplier (e.g. "ČEVABDŽINICA") selling "Coca cola" almost certainly means a single-serve portion — create a separate article like `coca-cola-restoran` or `coca-cola-033l` rather than mapping to a retail bottle.
- When an alias has no size info **and** the supplier is a retailer, prefer the most common retail size for that product. When in doubt, create a generic variant (no size in the key) rather than force-mapping to a specific size.
- Before mapping a batch of aliases that share a brand, list them all and compare sizes first.

#### Preflight helper: group aliases by brand (recommended)

Use the helper script `scripts/bb/expenses/group_aliases_by_brand.clj` to pre-group aliases by detected brand/product family and extract size tokens.

This is specifically designed to prevent size/variant conflation (e.g. mapping Coca-Cola 0.25L + 1.25L + 2L to a single canonical article).

Usage examples:

- Show *unmapped only* (default):
  - `bb group-aliases-by-brand dev`
- Include already-mapped aliases too:
  - `bb group-aliases-by-brand dev --mapped`
- Focus on clusters (e.g. only groups with 2+ aliases):
  - `bb group-aliases-by-brand dev --min-group 2`

How to interpret output:

- **VARIANT RISK** groups are the ones to handle first.
  - Multiple detected sizes (e.g. `0.25l`, `1.25l`, `2l`) => create separate articles per size.
  - Mixed supplier types (restaurant + retail) => likely separate “serving” vs “retail-pack” articles.
- **Single-variant** groups are usually safe to map directly (still sanity-check manufacturer and product type).

### Quality Assurance
- Verify web search results are actual products, not similar items
- Check manufacturer names are correctly spelled
- Ensure categories are consistent
- Test that OCR labels actually match the products found

### Common Issues
- **Supplier name mismatch**: Suppliers may have different display names in DB vs receipts
- **Multiple products per alias**: One OCR label may match multiple product variants — choose the most common **but create separate articles when sizes differ** (see "Size & Variant Differentiation" above)
- **Same brand, different sizes**: Group aliases by brand first; inspect size tokens (`0,25`, `1 25L`, `2L PET`, etc.) before inserting articles. Creating one article per size avoids costly remapping later.
- **Restaurant vs retail**: Aliases from restaurants/cafés/fast-food often refer to single-serve or on-tap portions, not retail packs. Create a dedicated `(restoran)` or serving-size article.
- **No web results**: Some products may be local/private label - create generic article
- **OCR noise**: Filter out receipt metadata, discount info, totals

---

## Related Files

- `resources/db/domain/models.edn` - Domain schema for articles + taxonomy tables (manufacturers/categories/subcategories)
- `src/app/domain/backend/expenses/services/articles.clj` - Article CRUD + alias normalization
- `src/app/domain/backend/expenses/services/manufacturers.clj` - Manufacturer CRUD
- `scripts/bb/web/serper_search.clj` - Serper.dev web search helper (the only web search tool for this skill)
- `scripts/bb/expenses/list_unmapped_article_aliases.clj` - Print candidate aliases from receipt extracts
- `scripts/bb/expenses/search_article_products.clj` - Helper to print aliases in a web-search-friendly format
- `scripts/bb/expenses/group_aliases_by_brand.clj` - Preflight grouping + size extraction to prevent size/variant mapping mistakes
- `scripts/bb/expenses/spellcheck_article_canonical_names.clj` - Spellcheck `articles.canonical_name` and generate suggestions
- `scripts/bb/articles/list_unmapped_aliases.clj` - Deterministic backlog query for unmapped aliases
- `scripts/bb/articles/list_aliases_from_receipts.clj` - Fallback raw label extraction from receipts JSON
- `scripts/bb/articles/ensure_taxonomy.clj` - Deterministic taxonomy upserts (manufacturer/category/subcategory)
- `scripts/bb/articles/create_articles.clj` - Create/fetch one or many canonical articles (optionally ensuring taxonomy)
- `scripts/bb/articles/map_aliases.clj` - Map alias(es) → article via alias_id(s) and article selector
- `scripts/bb/articles/unmapped_aliases_counts.clj` - Find likely OCR noise via grouped occurrence counts
- `scripts/bb/articles/delete_unmapped_aliases.clj` - Safe deletion of unmapped OCR noise aliases (dry-run by default)
- `scripts/bb/articles/report_progress.clj` - Coverage + breakdown reports
