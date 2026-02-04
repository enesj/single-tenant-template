# Store fingerprinting via `stores.normalized_key` (implementation plan)

## Opinion / decision

Using `(supplier_id, stores.normalized_key)` (and optionally `(suppliers.normalized_key, stores.normalized_key)`) as a **store “fingerprint”** is a good idea **as supporting context**, because:

- It’s already enforced unique via the DB constraint on `(supplier_id, normalized_key)`.
- It’s deterministic and human-readable (especially for `pj-<num>` stores).
- It works well for grouping receipts, caching, and “format hints” without relying on UUIDs (handy in dev where DB is reset often).

Important caveat:
- `stores.normalized_key` is only as stable as the *first* successful canonicalization. For stores without a stable branch identifier (e.g. no `PJ/Podružnica/Prodavnica` number), the key may be an address/label slug and can be more brittle across DB resets.

This plan intentionally avoids DB schema changes unless we later decide we need structured fingerprint fields.

## Goals

- Reuse store “fingerprints” as **context** during receipt refine and related heuristics.
- Keep all joins and logic still based on `store_id` (UUID); the fingerprint is an extra signal only.
- Prefer stable keys (`pj-*`) when available.
- Preserve current ingestion correctness and parallel-upload safety.

## Pipeline placement (OCR → resolve → refine)

- The fingerprint is derived **after** OCR returns markdown (from header cues like PJ/Podružnica/Prodavnica), and used in the post-OCR pipeline.
- It is **not** used to influence the OCR provider call itself (we treat OCR as “bytes → markdown”).
- Main uses before refine:
  - resolve `store_id` and `(supplier_key, store_key)` early
  - apply store-specific markdown cleanup/parsing rules when available
  - decide whether refine is needed
  - include store context when calling refine

## Non-goals

- No “reset stores” or destructive maintenance built into the app.
- No automatic merging of existing duplicate stores (dev workflow can still wipe/reupload).
- No schema changes in this phase.

## Definition: store fingerprint

Use this consistently:

- `supplier_key`: `suppliers.normalized_key`
- `store_key`: `stores.normalized_key`
- `store_fingerprint`: `(supplier_key, store_key)` (string form: `"<supplier_key>/<store_key>"`)

Notes:
- For database uniqueness we still rely on `(supplier_id, store_key)`.
- For “cross-environment stability” we use `(supplier_key, store_key)` because UUIDs differ after resets.

## Phase 1 — plumbing + observability (no behavior change)

1. Add a helper to load/store context for a receipt:
   - Input: `receipt-id`
   - Output: `{supplier_id supplier_key supplier_name store_id store_key store_display_name store_address}`
2. Log the fingerprint whenever we refine:
   - `{:supplier-key .. :store-key .. :fingerprint ..}` in refine logs.
3. Persist this context in `receipts.raw_extract_json` (under a namespaced key like `:receipt_refine/context`) so it’s inspectable later.

Acceptance criteria:
- No change to refine outputs.
- Logs show consistent fingerprints for receipts from the same store.

## Phase 2 — include fingerprint context in LLM refine

Today the Cerebras refine call is driven by `receipt-refine/build-chat-messages markdown` and is store-agnostic.

Implement a “context-aware refine” without contaminating receipt markdown:

1. Extend the refine API surface:
   - `cerebras/refine-receipt-markdown!` to accept `{:context ..}` or a second arity.
   - `receipt-refine/build-chat-messages` to accept `markdown` + optional `context`.
2. Add an extra **system message** (or an early assistant message) that includes:
   - `supplier_key`, `supplier_name`
   - `store_key` (highlight if `pj-*`)
   - `store_display_name`, `store_address` (optional; receipt markdown already includes these often)
3. Guard with a feature flag (env var) so this can be enabled gradually in dev:
   - Example: `RECEIPT_REFINE_INCLUDE_STORE_CONTEXT=true`

Acceptance criteria:
- Refine still returns valid JSON schema output.
- No regression in extraction rate; ideally fewer `review_required` cases for stores with stable keys.

## Phase 3 — per-supplier / per-store “format hints” keyed by fingerprint

Add an optional hints map keyed by the fingerprint to help refine for known tricky stores.

1. Create a hints config (EDN) that maps:
   - `supplier_key` → hints
   - `supplier_key + store_key` → more-specific hints
2. In the refine prompt builder, append hints when available.
3. Keep hints minimal and explicit (avoid including real receipt values).

Acceptance criteria:
- We can add a hint for one store and see targeted improvement without affecting other stores.

## Validation checklist (dev workflow)

- Upload a batch of receipts in parallel.
- Confirm no new `place-*` store keys are created by ingestion.
- Confirm multiple OCR variants map to the same store:
  - many `store_aliases.raw_label_normalized` → one `stores.normalized_key` (often `pj-*`).
- Compare refine outcomes before/after enabling store-context flag:
  - `review_required` count
  - parsing correctness for known problematic suppliers/stores

## Open questions (parked)

- Should we introduce structured columns later (`stores.branch_number`, `stores.branch_name_norm`)? That requires a migration and should only happen if:
  - we need more stability than `stores.normalized_key`, or
  - we want indexed matching by branch number without regex parsing.
- Supplier “fingerprints” only make sense if we can extract stable identifiers (e.g. JIB/PDV/VAT). That should be a separate scoped change (and likely a migration).

