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
- Use the helper script in `scripts/create_stores.clj` for repeatable, safe runs.

## Prerequisites

- Database connection configured in `config/base.edn` (dev/test profiles).
- Prefer using the Postgres MCP tools to inspect data and verify results.

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

### Step 3: Create (or reuse) a store row

Stores are unique per supplier by `(supplier_id, normalized_key)`. A minimal insert:

```sql
INSERT INTO stores (id, supplier_id, display_name, normalized_key, address, place_id)
VALUES (gen_random_uuid(), :supplier_id, :display_name, :normalized_key, NULL, NULL)
ON CONFLICT (supplier_id, normalized_key) DO NOTHING;
```

Then fetch the store id:

```sql
SELECT id
FROM stores
WHERE supplier_id = :supplier_id
  AND normalized_key = :normalized_key
LIMIT 1;
```

### Step 4: Map the alias to the store

```sql
UPDATE store_aliases
SET store_id = :store_id
WHERE id = :store_alias_id
  AND store_id IS NULL;
```

## Helper script (recommended)

Run the safe, repeatable script:

```bash
# Dry-run (no DB writes)
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev

# Apply changes (prompts for confirmation phrase)
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --apply

# Apply without prompt (still requires --apply)
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --apply --yes

# Reset ALL stores first (delete stores + clear references), then recreate + remap
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --reset-stores
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --reset-stores --apply --yes

# Dedupe existing stores (merge same-store duplicates where safe)
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing
clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing --apply --yes
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
