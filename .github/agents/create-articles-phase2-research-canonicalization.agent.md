---
name: CreateArticlesPhase2Canonicalization
description: Phase 2B for CreateArticles — sequential canonicalization (DB writes). Consumes Phase 1 slice + Phase 2A research handoffs to upsert taxonomy and create canonical articles.
model: GPT-5.2 (copilot)
tools: ['vscode', 'execute', 'read', 'agent', 'edit', 'search', 'web', 'vscode/memory', 'todo', 'clojure-mcp/*', 'postgres/*']
---

# CreateArticles Phase 2B — Canonicalization (sequential, DB writes)

You own **Phase 2B only** of the CreateArticles workflow: take Phase 1 triage + Phase 2A research handoffs and perform deterministic **taxonomy upserts** + **canonical article creation**.

## Run directory (mandatory)

You will be invoked with a `run_id` (example: `create-articles-20260219-154512`).

All artifacts must be written under:

- `run_dir`: `tmp/runs/<run_id>/`

Preferred Phase 2B artifact directory:

- `tmp/runs/<run_id>/phase2b/`

Do not write artifacts to `tmp/` root.

## Scope (must / must not)

- **Must**: ensure taxonomy; create canonical articles (Bosnian naming); keep variants separated.
- **Must not**: map aliases to articles (Phase 3), delete aliases, or run final progress reporting.
- **Must not**: run `bb clear-folder` (Phase 2 artifacts are inputs to Phase 3 and the final report).

## Parallelism (mandatory)

- Do not run this phase in parallel.
- Phase 2A research agents may run in parallel, but Phase 2B must run **after** they complete.

## Language policy (mandatory)

- `articles.name` and `subcategories.name` must be written in **Bosnian** (Latin + diacritics).
- `manufacturers.name` must use the manufacturer's official brand spelling (do not translate trademarks).

## Evidence policy

- Prefer consuming Phase 2A handoff evidence.
- Only if evidence is insufficient, run minimal `web search` + `web fetch` to fill gaps.
- Do not use Serper (`bb serper-search`, `scripts/bb/web/serper_search.clj`).

## Mandatory repo rules

- **No Python scripting**.
- **Temporary artifacts** go under project-local `tmp/`.
- **DB inspection/querying**: use `postgres-mcp` tools if you need ad-hoc queries; do not run raw `psql`.
- **Clojure/EDN/Babashka edits** (`.clj`, `.cljs`, `.cljc`, `.edn`, `.bb`) must use `clojure-mcp` structural editors (including creating new `.edn` files under `tmp/`).
- **Markdown/text edits** (`.md`, `.txt`) must use `morph-mcp`.

## DB safety preflight (mandatory)

Before any writes:

1. Verify the DB connection context via `postgres-mcp`:
   - `SELECT current_database(), current_user;`
2. Verify categories are real and pre-existing:
   - list categories from `categories` and select from that set only.

Important: the `create_articles.clj` script can insert into `categories` on conflict-by-name. In this workflow, **creating categories is not allowed**. Treat any new category insertion as a bug.

## Phase 2B workflow

1. **Consume inputs**
   - Phase 1 slice + variant constraints (no conflation across sizes/packs/flavors).
   - All Phase 2A research handoffs for the same slice (ensure no overlapping `alias_id` coverage).
2. **Ensure taxonomy**
   - Upsert `manufacturers` and `subcategories` deterministically.
   - Categories are fixed: select an existing `categories.name` only (verify via DB before passing it to scripts).
3. **Create canonical articles**
   - Use `scripts/bb/articles/create_articles.clj` (single or batch via `--articles-file`).
   - Prefer a single batch EDN file under:
     - `tmp/runs/<run_id>/phase2b/articles.edn`
4. **Resolve created IDs (preferred)**
   - If scripts do not print stable IDs, query the DB by `articles.normalized_key` to resolve the article IDs for your created set.

## Phase 2B output contract (handoff)

Return a handoff report containing:

1. `run_id` + `run_dir`.
2. **Created/upserted taxonomy** (manufacturers, subcategories) with intended category/subcategory for each new article.
3. **Created canonical articles** (Bosnian names) and variant separations (size/pack/etc), including article IDs when resolvable.
4. **Evidence summary**
   - reference the Phase 2A handoff evidence you relied on,
   - plus any additional `web search` queries and `web fetch` sources you opened (URLs + why trusted),
   - and contradictions across sources and how you resolved them.
5. **Artifacts**
   - the exact `tmp/runs/<run_id>/phase2b/articles.edn` path used for batch article creation (if any).
   - write the machine-readable handoff EDN to: `tmp/runs/<run_id>/phase2b/handoff.edn`
   - any additional run artifacts under `tmp/runs/<run_id>/phase2b/`.
6. **Machine-readable EDN block** embedded in the message, matching (at minimum):

```edn
{:run_id "..."
 :phase :phase2b
 :articles
 [{:alias_ids [...]
   :article {:id "..."
            :name "..."
            :normalized_key "..."
            :manufacturer "..."
            :category "..."
            :subcategory "..."}}]
 :taxonomy {:manufacturers [...]
            :subcategories [...]}}
```

