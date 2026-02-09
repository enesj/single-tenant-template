---
name: create-stores
description: "Create canonical stores from unmapped store_aliases by inferring supplier_id from receipts/supplier_aliases, inserting stores when unambiguous, and mapping store_aliases.store_id. Use to backfill store mappings from receipt OCR data."
---

# create-stores

## Goal

Turn raw `store_aliases` (OCR / heuristic store labels) into canonical `stores`, and map `store_aliases.store_id` for all aliases where it can be inferred safely.

## Safety defaults

- Prefer **dry-run** first.
- Only create/link a store when the supplier can be inferred **unambiguously**.
- Use the helper script in `#.claude/skills/create-stores/scripts/create_stores.clj` for repeatable, safe runs.
- When you’re unsure about a store’s identity, prefer to leave it unmapped rather than guessing.

## Prerequisites

- Database connection configured in `config/base.edn` (dev/test profiles).
- Prefer using the Postgres MCP tools to inspect data and verify results.

Web search (optional but recommended when OCR is noisy / ambiguous):

- Web search priority:
  1. Preferred: use the agent's built-in web search/browsing tools to find official store pages and/or Maps listings.
  2. Optional fallback: use `bb serper-search` **only** when `ENABLE_SERPER_SEARCH=true` (in `.env`).
- If Serper fallback is enabled: Serper.dev Search API must be configured via `SERPER_API_KEY` (typically sourced from `.env`).
- If you find a URL you want to cite (Maps / retailer site), use a web reader / `fetch_webpage` to capture evidence and extract fields.

## Process

### Step 1: List unmapped store aliases

```sql
SELECT id, raw_label, raw_label_normalized, created_at, updated_at
FROM store_aliases
WHERE store_id IS NULL
ORDER BY updated_at DESC, created_at DESC;
```

### Step 2: Infer supplier_id per alias (via receipts)

The safest “automatic” inference is:

- Find receipts referencing a given `store_alias_id`
- From those receipts, resolve `supplier_id` via `supplier_aliases`
- Only accept cases where there is a **single** best supplier candidate (no ties)

```sql
-- Per store_alias, count supplier candidates seen on receipts
SELECT
  sa.id AS store_alias_id,
  ssa.supplier_id,
  count(*) AS receipts_cnt
FROM store_aliases sa
JOIN receipts r ON r.store_alias_id = sa.id
JOIN supplier_aliases ssa ON ssa.id = r.supplier_alias_id
WHERE sa.store_id IS NULL
  AND ssa.supplier_id IS NOT NULL
GROUP BY sa.id, ssa.supplier_id
ORDER BY receipts_cnt DESC;
```

If a store alias has ambiguous supplier candidates (ties / close counts), **do not create or link a store yet**.
Instead, move to web research (next step) and only proceed once you can justify a single supplier.

### Step 3: Web search to confirm store identity (recommended when ambiguous)

Use web search the same way as in `create-articles`: search, open 1–3 high-quality sources, then extract structured facts.

#### What to extract (minimum viable evidence)

- Store chain / brand (helps decide supplier)
- Canonical branch/store display name (often includes neighborhood/city)
- Full address (street + city)
- A Maps listing link, if available
- `place_id` when available (best identifier for dedupe + future matching)
- Source URL(s)

#### Suggested search queries

- `"<raw store label>" <city>`
- `<supplier brand> PJ <number> <city>` (if receipt header has a PJ/branch number)
- `site:google.com/maps "<store label>" <city>`
- `<store label> address <city>`

#### Places / Maps notes

- Prefer a Google Maps listing when possible; it makes it easier to keep a stable `place_id`.
- If you can’t reliably obtain a `place_id`, it’s still useful to capture the canonical address.

### Step 4: Create (or reuse) a store row (place_id-first)

Stores are unique per supplier by `(supplier_id, normalized_key)`.

**Preferred identity order** (most reliable → least):

1) Existing store with the same `(supplier_id, place_id)`
2) Existing store with the same `(supplier_id, normalized_key)`
3) Create a new store row

When you have a `place_id`, prefer a normalized key derived from it (this matches backend behavior):

- `normalized_key := "place-" + place_id`

That gives you a stable identity even when OCR-mangles the store name.

A minimal insert:

```sql
-- If you have place_id, prefer a place-based normalized_key.
INSERT INTO stores (id, supplier_id, display_name, normalized_key, address, place_id)
VALUES (
  gen_random_uuid(),
  :supplier_id,
  :display_name,
  COALESCE(:normalized_key, CONCAT('place-', :place_id)),
  :address,
  :place_id
)
ON CONFLICT (supplier_id, normalized_key) DO UPDATE
SET display_name = EXCLUDED.display_name,
    address = COALESCE(EXCLUDED.address, stores.address),
    place_id = COALESCE(EXCLUDED.place_id, stores.place_id),
    updated_at = NOW();
```

Then fetch the store id:

```sql
-- Prefer (supplier_id, place_id) lookup when place_id is known.
SELECT id
FROM stores
WHERE supplier_id = :supplier_id
  AND place_id = :place_id
LIMIT 1;

-- Fallback lookup by normalized_key.
SELECT id
FROM stores
WHERE supplier_id = :supplier_id
  AND normalized_key = :normalized_key
LIMIT 1;
```

### Step 5: Map the alias to the store

```sql
UPDATE store_aliases
SET store_id = :store_id
WHERE id = :store_alias_id
  AND store_id IS NULL;
```

### Step 6: Verify + report progress

Coverage + sanity checks (recommended to run after every batch):

```sql
-- Coverage stats
SELECT
  (SELECT count(*) FROM stores) AS stores,
  (SELECT count(*) FROM store_aliases) AS store_aliases,
  (SELECT count(*) FROM store_aliases WHERE store_id IS NULL) AS store_aliases_unmapped,
  (SELECT count(*) FROM store_aliases WHERE store_id IS NOT NULL) AS store_aliases_mapped,
  ROUND(100.0 * (SELECT count(*) FROM store_aliases WHERE store_id IS NOT NULL)
        / NULLIF((SELECT count(*) FROM store_aliases), 0), 1) AS mapped_percent;

-- Top unmapped aliases by receipt count (prioritize research)
SELECT
  sa.id,
  sa.raw_label,
  COUNT(r.id) AS receipts_cnt
FROM store_aliases sa
JOIN receipts r ON r.store_alias_id = sa.id
WHERE sa.store_id IS NULL
GROUP BY sa.id, sa.raw_label
ORDER BY receipts_cnt DESC, sa.raw_label
LIMIT 50;

-- Stores missing address/place_id (opportunity for web enrichment)
SELECT
  st.supplier_id,
  st.display_name,
  st.address,
  st.place_id,
  COUNT(sa.id) AS alias_cnt
FROM stores st
LEFT JOIN store_aliases sa ON sa.store_id = st.id
WHERE st.address IS NULL OR st.place_id IS NULL
GROUP BY st.supplier_id, st.display_name, st.address, st.place_id
ORDER BY alias_cnt DESC
LIMIT 50;

-- Duplicate place_id rows per supplier (should be empty after dedupe)
SELECT supplier_id, place_id, COUNT(*) AS cnt
FROM stores
WHERE place_id IS NOT NULL
GROUP BY supplier_id, place_id
HAVING COUNT(*) > 1
ORDER BY cnt DESC;
```

## Helper script (recommended)

Run the safe, repeatable script:

```bash
# Dry-run (no DB writes)
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev

# (Fallback / legacy location)
# clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev

# Apply changes (prompts for confirmation phrase)
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --apply

# Apply without prompt (still requires --apply)
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --apply --yes

# Reset ALL stores first (delete stores + clear references), then recreate + remap
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --reset-stores
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --reset-stores --apply --yes

# Dedupe existing stores (merge same-store duplicates where safe)
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing
clj -M .claude/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing --apply --yes
```

Script behavior:
- Infers `supplier_id` via `receipts.store_alias_id` + `receipts.supplier_alias_id -> supplier_aliases.supplier_id`
- Treats receipt `PJ` branch number as the primary store identifier; if exactly one existing store has the same `PJ`, it will be reused even if the `PJ` name is OCR-mangled
- Matches aliases to existing stores using receipt header fingerprints (e.g. `PJ` number + store name parsed from `receipts.parsed_markdown`) when available
- Falls back to conservative key matching (exact / loose / fuzzy) when receipt fingerprints are missing
- Skips aliases with ambiguous supplier candidates (ties) or insufficient evidence
- Creates a store when missing, then sets `store_aliases.store_id`

Script modes:
- `--reset-stores`: clears `store_id` on `store_aliases`/`expenses`, deletes all `stores`, then recreates and remaps
- `--dedupe-existing`: merges existing duplicate stores (conservative, receipt-driven) before mapping
