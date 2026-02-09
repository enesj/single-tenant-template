---
description: "Map article aliases extracted from receipt OCR data to canonical products via web search and database entry"
metadata:
  tags: ["articles", "article-aliases", "web-search", "database", "products", "receipts", "ocr", "mapping"]
---

# create-articles

## Goal
Map raw article aliases extracted from receipt OCR data to canonical products by finding actual products via web search, then create database entries for articles **and all related taxonomy tables** (manufacturers, categories, subcategories), and finally map aliases to articles.

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
- PostgreSQL MCP server must be available
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

Use PostgreSQL to query receipts and extract unique article aliases from OCR data.

```sql
-- List unmapped aliases (preferred: extraction already populates article_aliases)
SELECT
  aa.id AS alias_id,
  aa.raw_label,
  aa.raw_label_normalized,
  aa.supplier_id,
  s.display_name AS supplier
FROM article_aliases aa
LEFT JOIN suppliers s ON s.id = aa.supplier_id
WHERE aa.article_id IS NULL
ORDER BY s.display_name NULLS LAST, aa.raw_label;
```

Alternative - query directly from receipts:

```sql
-- Extract aliases from receipts with OCR data
SELECT DISTINCT
  jsonb_array_elements_text(
    jsonb_path_query_array(
      raw_extract_json,
      '$.extraction.items[*].raw_label'
    )
  ) as raw_label,
  supplier_guess
FROM receipts
WHERE raw_extract_json IS NOT NULL
ORDER BY raw_label;
```

---

## Step 2: Web Search for Products

Web search uses **only** `bb serper-search` (`scripts/bb/web/serper_search.clj`). If `ENABLE_SERPER_SEARCH` is not `true`, skip this step entirely and create best-effort generic articles from the OCR labels (see Step 6).

### Check if Serper is enabled

Before searching, verify the env var is set:
```bash
# Check .env for ENABLE_SERPER_SEARCH
grep ENABLE_SERPER_SEARCH .env
```

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

### 3.1 Upsert Manufacturer

Use a stable `normalized_key` (lowercase, hyphenated, ASCII-only) for deduplication.

```sql
-- Upsert manufacturer and return its id
WITH ins AS (
  INSERT INTO manufacturers (id, display_name, normalized_key)
  VALUES (gen_random_uuid(), 'Lasta', 'lasta')
  ON CONFLICT (normalized_key) DO NOTHING
  RETURNING id
)
SELECT id FROM ins
UNION ALL
SELECT id FROM manufacturers WHERE normalized_key = 'lasta'
LIMIT 1;
```

### 3.2 Upsert Category

```sql
-- Upsert category by unique name
WITH ins AS (
  INSERT INTO categories (id, name, description)
  VALUES (gen_random_uuid(), 'Food', 'Food & groceries')
  ON CONFLICT (name) DO NOTHING
  RETURNING id
)
SELECT id FROM ins
UNION ALL
SELECT id FROM categories WHERE name = 'Food'
LIMIT 1;
```

### 3.3 Upsert Subcategory

Subcategories are unique by `(category_id, name)`.

```sql
-- Upsert subcategory under a known category
WITH category AS (
  SELECT id
  FROM categories
  WHERE name = 'Food'
  LIMIT 1
), ins AS (
  INSERT INTO subcategories (id, category_id, name, description)
  SELECT gen_random_uuid(), category.id, 'Snacks', 'Chips, biscuits, sweets'
  FROM category
  ON CONFLICT (category_id, name) DO NOTHING
  RETURNING id
)
SELECT id FROM ins
UNION ALL
SELECT s.id
FROM subcategories s
JOIN categories c ON c.id = s.category_id
WHERE c.name = 'Food' AND s.name = 'Snacks'
LIMIT 1;
```

If you *don’t* have reliable taxonomy, it’s fine to leave `manufacturer_id` and/or `subcategory_id` as NULL and just create the article.

---

## Step 4: Create Canonical Articles

If you could not find reliable product info via web search, create a best-effort *generic* canonical article from the OCR label:
- `canonical_name`: cleaned-up `raw_label` (remove receipt noise / extra punctuation; keep grams/ml when present)
- `link`: NULL
- `manufacturer_id`: NULL
- `subcategory_id`: NULL

Insert articles into the database with web search results (when available) or generic values (when not).

### Article Schema
```sql
-- articles table structure
id              UUID PRIMARY KEY
canonical_name  VARCHAR(255) NOT NULL
normalized_key  VARCHAR(255) NOT NULL UNIQUE
category         VARCHAR(100)          -- legacy free-text (optional)
subcategory_id   UUID                 -- FK -> subcategories.id (preferred)
link            TEXT
manufacturer_id  UUID                 -- FK -> manufacturers.id
created_at      TIMESTAMPTZ DEFAULT NOW()
updated_at      TIMESTAMPTZ DEFAULT NOW()
```

### Insert Article
```sql
-- Insert a new article (FK-driven manufacturer + subcategory)
INSERT INTO articles (id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id)
VALUES (
  gen_random_uuid(),
  'Lasta Rondini keks 200g',
  'lasta-rondini-keks-200g',
  (SELECT s.id
   FROM subcategories s
   JOIN categories c ON c.id = s.category_id
   WHERE c.name = 'Food' AND s.name = 'Snacks'
   LIMIT 1),
  'https://lasta.com/lasta-product/rondini-limun-i-mak/',
  (SELECT id FROM manufacturers WHERE normalized_key = 'lasta' LIMIT 1)
)
ON CONFLICT (normalized_key) DO NOTHING
RETURNING id, canonical_name, manufacturer_id, subcategory_id;
```

### Insert Article + Taxonomy in One Go (CTE)

This pattern is convenient when you’re adding a brand-new manufacturer/category/subcategory.

```sql
WITH manufacturer AS (
  SELECT id
  FROM (
    WITH ins AS (
      INSERT INTO manufacturers (id, display_name, normalized_key)
      VALUES (gen_random_uuid(), 'Lasta', 'lasta')
      ON CONFLICT (normalized_key) DO NOTHING
      RETURNING id
    )
    SELECT id FROM ins
    UNION ALL
    SELECT id FROM manufacturers WHERE normalized_key = 'lasta'
    LIMIT 1
  ) x
), category AS (
  SELECT id
  FROM (
    WITH ins AS (
      INSERT INTO categories (id, name)
      VALUES (gen_random_uuid(), 'Food')
      ON CONFLICT (name) DO NOTHING
      RETURNING id
    )
    SELECT id FROM ins
    UNION ALL
    SELECT id FROM categories WHERE name = 'Food'
    LIMIT 1
  ) x
), subcategory AS (
  SELECT id
  FROM (
    WITH ins AS (
      INSERT INTO subcategories (id, category_id, name)
      SELECT gen_random_uuid(), category.id, 'Snacks'
      FROM category
      ON CONFLICT (category_id, name) DO NOTHING
      RETURNING id
    )
    SELECT id FROM ins
    UNION ALL
    SELECT s.id
    FROM subcategories s
    WHERE s.category_id = (SELECT id FROM category) AND s.name = 'Snacks'
    LIMIT 1
  ) x
)
INSERT INTO articles (id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id)
VALUES (
  gen_random_uuid(),
  'Lasta Rondini keks 200g',
  'lasta-rondini-keks-200g',
  (SELECT id FROM subcategory),
  'https://lasta.com/lasta-product/rondini-limun-i-mak/',
  (SELECT id FROM manufacturer)
)
ON CONFLICT (normalized_key) DO NOTHING
RETURNING id, canonical_name;
```

### Normalization Rules
- Lowercase: `Lasta Rondini` → `lasta-rondini`
- Remove non-ASCII/special chars during normalization (e.g. `Š` is dropped). Prefer ASCII canonical names when possible.
- Replace spaces with hyphens
- Keep it URL-safe

---

## Step 5: Map Aliases to Articles

Update article_aliases to link raw OCR labels to canonical articles.

### Map Single Alias
```sql
-- Map alias to article
UPDATE article_aliases
SET article_id = (
  SELECT id FROM articles
  WHERE canonical_name = 'Lasta Rondini keks 200g'
  LIMIT 1
)
WHERE raw_label = 'KEKS RONDINI LASTA/KO (E)'
  AND supplier_id = (
    SELECT id FROM suppliers
    WHERE display_name = 'TROPIC MALOPRUDAJA d.o.o. Banja Luka'
  )
RETURNING id, raw_label, article_id;
```

### Batch Map Multiple Aliases
```sql
-- Map multiple aliases using CASE
UPDATE article_aliases
SET article_id = (
  SELECT id FROM articles
  WHERE canonical_name = CASE
    WHEN raw_label = 'KEKS RONDINI LASTA/KO (E)' THEN 'Lasta Rondini keks 200g'
    WHEN raw_label = 'CIRIO PASSATA BRIK 3X200ML' THEN 'Cirio Passata Brik 3x200ml'
    WHEN raw_label = 'SIR ZANETI PARD 150G' THEN 'Zanetti Parmigiano Reggiano 150g'
    -- ... more cases
  END
  LIMIT 1
)
WHERE raw_label IN (
  'KEKS RONDINI LASTA/KO (E)',
  'CIRIO PASSATA BRIK 3X200ML',
  'SIR ZANETI PARD 150G'
)
RETURNING raw_label, article_id;
```

---

## Step 6: Handle Generic Products

For items without specific brands, create generic articles with descriptive names.

### Generic Article Examples
```sql
-- Generic aluminum containers
INSERT INTO articles (id, canonical_name, normalized_key, category)
VALUES (
  gen_random_uuid(),
  'Aluminijumska posuda za teletinu 500ml',
  'aluminijumska-posuda-teletina-500ml',
  'Ambalaža'
);

-- Generic garbage bags
INSERT INTO articles (id, canonical_name, normalized_key, category)
VALUES (
  gen_random_uuid(),
  'Vreće za smeće 60L crne',
  'vrace-smece-60l-crne',
  'Kućne potrepštine'
);
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
```sql
-- Check remaining unmapped aliases
SELECT
  aa.raw_label,
  s.display_name as supplier,
  COUNT(*) as occurrence_count
FROM article_aliases aa
LEFT JOIN suppliers s ON s.id = aa.supplier_id
WHERE aa.article_id IS NULL
GROUP BY aa.raw_label, s.display_name
ORDER BY occurrence_count DESC, aa.raw_label;
```

### Remove OCR Noise (Optional)
```sql
-- Remove identified noise aliases
DELETE FROM article_aliases
WHERE raw_label IN ('PARTICA:', 'POPRAT', 'POU:', 'POPUST -10,00%:', 'CSN. E:')
  AND article_id IS NULL;
```

---

## Step 8: Verify and Report Progress

Track mapping progress and final statistics.

### Coverage Statistics
```sql
-- Final mapping statistics
WITH stats AS (
  SELECT
    COUNT(DISTINCT aa.id) as total_aliases,
    COUNT(DISTINCT CASE WHEN aa.article_id IS NOT NULL THEN aa.id END) as mapped_aliases,
    COUNT(DISTINCT a.id) as total_articles
  FROM article_aliases aa
  LEFT JOIN articles a ON a.id = aa.article_id
)
SELECT
  total_aliases,
  mapped_aliases,
  total_articles,
  ROUND(100.0 * mapped_aliases / NULLIF(total_aliases, 0), 1) as coverage_percent
FROM stats;
```

### Articles by Category
```sql
-- Articles distribution by FK-driven category/subcategory (preferred)
SELECT
  COALESCE(c.name, 'Uncategorized') as category,
  COALESCE(s.name, 'Uncategorized') as subcategory,
  COUNT(*) as article_count
FROM articles a
LEFT JOIN subcategories s ON s.id = a.subcategory_id
LEFT JOIN categories c ON c.id = s.category_id
GROUP BY c.name, s.name
ORDER BY article_count DESC;
```

### Articles by Manufacturer
```sql
-- Articles by manufacturer
SELECT
  COALESCE(m.display_name, 'Generic') as manufacturer,
  COUNT(*) as article_count
FROM articles a
LEFT JOIN manufacturers m ON m.id = a.manufacturer_id
GROUP BY m.display_name
ORDER BY article_count DESC;
```

---

## Complete Example Workflow

```sql
-- 1. Get unmapped aliases
SELECT raw_label, supplier_guess
FROM receipts r,
  jsonb_array_elements(
    jsonb_path_query_array(r.raw_extract_json, '$.extraction.items[*].raw_label')
  ) as raw_label
WHERE raw_extract_json IS NOT NULL
GROUP BY raw_label, supplier_guess
LIMIT 10;

-- 2. For each alias, search with bb serper-search (only if ENABLE_SERPER_SEARCH=true):
--    bb serper-search "{raw_label}" --gl BA --hl bs --num 5 --format pretty
--    If Serper is not enabled, skip to step 4 and create generic articles.

-- 3. After web search, upsert taxonomy (manufacturer/category/subcategory)
--    then insert article referencing manufacturer_id + subcategory_id

-- (See Step 3 for upsert examples)

-- 4. Insert article
INSERT INTO articles (id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id)
VALUES (
  gen_random_uuid(),
  'Found Product Name from Web',
  'normalized-product-name',
  (SELECT s.id
   FROM subcategories s
   JOIN categories c ON c.id = s.category_id
   WHERE c.name = 'Food' AND s.name = 'Snacks'
   LIMIT 1),
  'https://product-page-url',
  (SELECT id FROM manufacturers WHERE normalized_key = 'manufacturer-key' LIMIT 1)
)
ON CONFLICT DO NOTHING;

-- 5. Map alias
UPDATE article_aliases
SET article_id = (SELECT id FROM articles WHERE canonical_name = 'Found Product Name')
WHERE raw_label = 'OCR_EXTRACTED_LABEL'
  AND supplier_id = (SELECT id FROM suppliers WHERE display_name = 'Supplier Name');

-- 6. Check progress
SELECT
  COUNT(*) as total_aliases,
  COUNT(article_id) as mapped,
  ROUND(100.0 * COUNT(article_id) / COUNT(*), 1) as percent
FROM article_aliases;
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
