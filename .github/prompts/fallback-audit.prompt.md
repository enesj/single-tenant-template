---
description: Inventory all fallback/defensive-default code and produce a prioritised elimination plan toward clean architecture
agent: "agent"
---

You are working in `/Users/enes/Projects/single-tenant-template` — a Clojure + shadow-cljs + re-frame + UIx single-tenant app.

## Goal

Produce a **complete, categorised inventory** of all fallback and defensive-default code in `src/app/`, then output a **prioritised elimination plan** that moves the codebase toward clean architecture (explicit contracts, no silent defaults, push IO/validation to boundaries).

Do not make any code changes. Output only the audit document and the plan.

---

## Clean-architecture target principles

1. **Boundary validation** — nil/invalid inputs are caught once at IO/API boundaries (HTTP handlers, DB read, external integrations), never defended against deep inside business logic.
2. **Explicit contracts** — functions receive well-typed non-nil arguments; callers are responsible for providing correct input.
3. **No silent swallowing** — `try/catch` with a fallback return value hides errors; errors should propagate or be surfaced explicitly.
4. **Single source of truth for defaults** — defaults live in one canonical place per concept (config, schema), not scattered as `(or x 25)` literals.
5. **Server-authoritative data** — the backend owns pagination, filtering and sorting; client-side fetch-1000 snapshots are an anti-pattern.

---

## Audit phases

### Phase 1 — Inventory

Search `src/app/` for each of the following categories and list every occurrence (file + line range + brief description of what it does):

#### Category A — `(or expr default)` nil-defensive hardcoded defaults
Search for stacked `or` chains: `(or ... default-literal)`. Pay special attention to:
- Pagination defaults (`per-page`, `current-page`, `fetch-limit`, `fetch-offset`)
- UI state defaults inside subscriptions or event handlers
- `def fallback-defaults` named constants

#### Category B — `(get-in m path default)` / `(get m k default)` with hardcoded defaults
Look for map lookups that mask missing data with `[]`, `{}`, `#{}`, `nil`, or numeric literals.

#### Category C — Nil guard branches (`if nil?`, `when-not nil?`, `if-let` fallback branch)
Identify cases where the guard compensates for a caller that should never pass nil.

#### Category D — `try/catch` that returns a fallback value
Silent error swallowing — the catch arm returns `{}`, `nil`, `false`, `[]`, or similar instead of propagating the error.

#### Category E — Client-side fetch-all pattern (`fetch-limit 1000`)
List every page/event that fetches up to 1000 records for client-side filtering instead of using server-side pagination. Note which entities are affected.

#### Category F — Unconstrained `:else` catchall fallbacks in `cond` / `condp` / `case`
Focus on cases where an `:else` branch silently coerces unknown values rather than throwing an assertion error.

#### Category G — `settings/resolver.cljs` `fallback-defaults` precedence chain
The resolver has a multi-level `(or in-code-default config-file-value admin-override)` chain. Map the full precedence tree.

---

### Phase 2 — Classify each occurrence

For each item found, assign:

| Column | Values |
|--------|--------|
| **Keep** | Legitimate boundary guard that belongs at this layer |
| **Extract** | Move default to a canonical config / schema location |
| **Delete** | Remove — the caller should be fixed to never pass nil/invalid |
| **Propagate** | Replace silent catch with explicit error propagation |
| **Server-side** | Replace client-side fallback with proper server pagination/filtering |

---

### Phase 3 — Elimination plan

Group the items from Phase 2 into a prioritised backlog:

**P0 — Safety issues**: `try/catch` that masks real errors (Category D) where the swallowing could hide data corruption or integration failures.

**P1 — Scalability**: All client-side fetch-1000 patterns (Category E). Use the existing `server-side-pagination.prompt.md` workflow for each entity.

**P2 — Contract clarity**: Stacked `(or ...)` chains and nil guards inside subscriptions and event handlers (Categories A, B, C) that should be eliminated by fixing callers.

**P3 — Config consolidation**: Scattered `(or x 25)` literals and `fallback-defaults` maps (Categories A, G) that should be collapsed into a single canonical defaults namespace or config EDN key.

**P4 — Catchall hygiene**: `:else` arms (Category F) that should assert-or-throw on unrecognised values instead of coercing silently.

For each priority level, list:
- The specific occurrences to tackle
- The recommended fix pattern (one sentence)
- The file(s) that need to change
- Whether a schema/DB migration is required

---

## Output format

Produce a Markdown document with the following top-level sections:

```
# Fallback Code Audit — <date>

## Executive summary
(5–10 bullet points: total count per category, highest-risk items, recommended first action)

## Inventory
### Category A …
### Category B …
… (all categories)

## Elimination plan
### P0 — Safety
### P1 — Scalability
### P2 — Contract clarity
### P3 — Config consolidation
### P4 — Catchall hygiene

## Items to keep (justified)
(Occurrences classified as "Keep" with one-sentence justification each)
```

Save the document to `tmp/fallback-audit-<YYYY-MM-DD>.md`.

---

## Key files to read first

Before starting the search, read these files for context:

- `src/app/template/frontend/subs/list.cljs` — stacked `or` chains + `try/catch` fallbacks
- `src/app/template/frontend/subs/entity.cljs` — four-level key-resolution fallback chain
- `src/app/template/frontend/settings/resolver.cljs` — `fallback-defaults` named constant + precedence chain
- `src/app/domain/frontend/expenses/events/events_factory.cljs` — `resolve-pagination` with `(or ... 25)` chain + client-side filter note
- `src/app/template/frontend/db/defaults.cljs` — intentional `(or % first-entity)` initialisation pattern
- `src/app/template/backend/middleware/rate_limiting.clj` — `try/catch` masking time-comparison error

These files contain the most representative examples across all seven categories.
