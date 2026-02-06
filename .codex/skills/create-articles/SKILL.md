---
name: create-articles
description: "Map article aliases from receipt OCR to canonical products via web search and database entry"
metadata:
  tags:
    - articles
    - article-aliases
    - web-search
    - database
    - products
    - receipts
    - ocr
    - mapping
---

# create-articles

## Goal
Map raw article aliases extracted from receipt OCR data to canonical products by finding actual products via web search, then create database entries for articles and map aliases to them.

## When to use
- You need to populate the articles table with actual products found from web search
- Article aliases from receipts need to be mapped to canonical articles
- You have OCR-extracted labels (raw_label) that need to be resolved to real products
- Building product catalog from receipt data

## Prerequisites
- PostgreSQL MCP server must be available
- Web search tools must be available
- Database connection configured in `config/base.edn`
- Receipts with `raw_extract_json` data containing article items

## Process Overview

1. **Extract unmapped article aliases** from receipts
2. **Web search** each alias to find actual products
3. **Create canonical articles** in database
4. **Map aliases to articles** via article_id
5. **Identify OCR noise** and optionally remove it

---

## Step 1: Extract Article Aliases from Receipts

Use PostgreSQL to query receipts and extract unique article aliases from OCR data.

```sql
-- Get all unique article aliases from receipt OCR data
SELECT DISTINCT
  rl.raw_label,
  r.supplier_guess,
  rl.supplier_id,
  rl.id as alias_id
FROM article_aliases rl
JOIN receipts r ON r.supplier_alias_id = (
  SELECT sa.id FROM supplier_aliases sa
  WHERE sa.raw_label_normalized = r.supplier_guess
  LIMIT 1
)
WHERE rl.article_id IS NULL
ORDER BY rl.raw_label;
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

For each unique `raw_label`, search the web to find the actual product.

### Search Strategy
1. **Start with specific queries** including supplier name + Bosnia region
2. **Use English and local languages** (Bosnian/Croatian/Serbian)
3. **Look for e-commerce sites** (Vocar, Bingo, Maxi, Hoše komerc, etc.)
4. **Find canonical product info**: name, manufacturer, GTIN/EAN, product page URL

### Search Query Format
```
"{raw_label}" {supplier_name} Bosnia
"{raw_label}" product Bosnia
{raw_label} kupovina online
```

### Example Web Search
```clojure
;; Use web-search-prime tool
;; Query: "KEKS RONDINI LASTA" Bosnia
;; Expected: Find Lasta Rondini product page with:
;;   - Canonical name: "Lasta Rondini keks 200g"
;;   - Manufacturer: "Lasta"
;;   - Category: "Slatkiši i grickalice"
;;   - Link: https://lasta.com/lasta-product/rondini-...
```

### Web Reader for Product Details
After finding product URLs, use web-reader to extract structured data:
```clojure
;; Use web-reader tool with product URL
;; Extract: product name, description, manufacturer, GTIN, price
```

---

## Step 3: Create Canonical Articles

Insert articles into the database with web search results.

### Article Schema
```sql
-- articles table structure
id              UUID PRIMARY KEY
canonical_name  VARCHAR(255) NOT NULL
normalized_key  VARCHAR(255) NOT NULL UNIQUE
category        VARCHAR(100)
link            TEXT
manufacturer    VARCHAR(255)
created_at      TIMESTAMPTZ DEFAULT NOW()
updated_at      TIMESTAMPTZ DEFAULT NOW()
```

### Insert Article
```sql
-- Insert a new article
INSERT INTO articles (id, canonical_name, normalized_key, category, link, manufacturer)
VALUES (
  gen_random_uuid(),
  'Lasta Rondini keks 200g',
  'lasta-rondini-keks-200g',
  'Slatkiši i grickalice',
  'https://lasta.com/lasta-product/rondini-limun-i-mak/',
  'Lasta'
)
ON CONFLICT (normalized_key) DO NOTHING
RETURNING id, canonical_name;
```

### Normalization Rules
- Lowercase: `Lasta Rondini` → `lasta-rondini`
- Remove special chars: `Šljiva` → `sljiva`
- Replace spaces with hyphens
- Remove diacritics (ć → c, š → s, ž → z, đ → d)
- Keep it URL-safe

---

## Step 4: Map Aliases to Articles

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

## Step 5: Handle Generic Products

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

## Step 6: Identify and Remove OCR Noise

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

## Step 7: Verify and Report Progress

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
-- Articles distribution by category
SELECT
  COALESCE(category, 'Uncategorized') as category,
  COUNT(*) as article_count
FROM articles
GROUP BY category
ORDER BY article_count DESC;
```

### Articles by Manufacturer
```sql
-- Articles by manufacturer
SELECT
  COALESCE(manufacturer, 'Generic') as manufacturer,
  COUNT(*) as article_count
FROM articles
GROUP BY manufacturer
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

-- 2. For each alias, web search: "{raw_label}" {supplier} Bosnia

-- 3. After web search, insert article
INSERT INTO articles (id, canonical_name, normalized_key, category, link, manufacturer)
VALUES (
  gen_random_uuid(),
  'Found Product Name from Web',
  'normalized-product-name',
  'Category from research',
  'https://product-page-url',
  'Manufacturer Name'
)
ON CONFLICT DO NOTHING;

-- 4. Map alias
UPDATE article_aliases
SET article_id = (SELECT id FROM articles WHERE canonical_name = 'Found Product Name')
WHERE raw_label = 'OCR_EXTRACTED_LABEL'
  AND supplier_id = (SELECT id FROM suppliers WHERE display_name = 'Supplier Name');

-- 5. Check progress
SELECT
  COUNT(*) as total_aliases,
  COUNT(article_id) as mapped,
  ROUND(100.0 * COUNT(article_id) / COUNT(*), 1) as percent
FROM article_aliases;
```

---

## Tips & Best Practices

### Web Search Tips
- Include supplier name in queries for better results
- Use local language (Bosnian/Croatian/Serbian) for local products
- Look for official manufacturer pages or major retailers
- Check multiple sources to verify product details

### Database Tips
- Always use `ON CONFLICT DO NOTHING` when inserting articles
- Use normalized_key for unique constraints
- Keep canonical names descriptive but consistent
- Include manufacturer when known, NULL for generic items

### Quality Assurance
- Verify web search results are actual products, not similar items
- Check manufacturer names are correctly spelled
- Ensure categories are consistent
- Test that OCR labels actually match the products found

### Common Issues
- **Supplier name mismatch**: Suppliers may have different display names in DB vs receipts
- **Multiple products per alias**: One OCR label may match multiple product variants - choose the most common
- **No web results**: Some products may be local/private label - create generic article
- **OCR noise**: Filter out receipt metadata, discount info, totals

---

## Related Files

- `resources/db/models.edn` - Database schema for articles and article_aliases
- `src/app/domain/backend/expenses/services/article_aliases.clj` - Alias service logic
- `scripts/bb/expenses/list_unmapped_article_aliases.clj` - Extraction script
- `scripts/bb/expenses/seed_manufacturer_aliases_from_articles.clj` - Manufacturer inference
