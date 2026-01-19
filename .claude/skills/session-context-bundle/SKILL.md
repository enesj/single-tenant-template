---
description: "Session bootstrap: Morph discovery → bb audit-bundle → Lattice queries (Option A, agent-curated queries)"
allowed-tools:
  - morph-mcp:warpgrep_codebase_search
  - lattice:lattice_load
  - lattice:lattice_query
  - lattice:lattice_expand
---

# session-context-bundle

Use this at the **beginning of a session** to create a single, provenance-preserving “context bundle” file tailored to the user’s prompt.

This is **Option A (manual/agent-curated)**:
- The agent uses Morph to discover *likely* files + keywords.
- The agent then chooses a small set of `--query/--glob` arguments.
- The agent runs `bb audit-bundle` to generate a bundle under `target/audit-bundles/`.
- The agent loads that bundle into Lattice and uses it as the primary searchable corpus for the rest of the session.

## Use when
- Most non-trivial tasks (bugfixes, refactors, feature work, doc audits)
- The prompt touches multiple files/systems
- You want stable `path:line` citations while iterating

## Do NOT use when
- The user names a single file + function and wants a small edit (bundle adds overhead)
- The task is purely conversational/strategic (no repo evidence needed)

## Decision guide (fast)
Use **`session-context-bundle`** when **2+** are true:
- Multi-file by nature (trace a flow, refactor, debug across layers)
- Cross-cutting scope (docs+code+config, FE+BE)
- You expect iterative searching (many “find where X happens” loops)
- You need `path:line` evidence for reporting

Skip it when:
- Single-file surgical change
- Purely conceptual request

**Rule of thumb:** if you expect more than ~5 repo searches in the session, bootstrap a bundle.

## Workflow (copy/paste mindset)

### 1) Morph discovery (2 searches max)
Goal: identify **canonical files** and **search strings**.

Good discovery queries:
- “Find the entrypoint(s) for <feature> and where it’s wired into routes/events”
- “Find docs describing <feature> and the actual implementation code path”

Collect:
- candidate globs (e.g. `docs/**`, `src/**`, `shadow-cljs.edn`, `resources/**`, `config/**`)
- candidate queries (namespaces, function names, route paths, config keys)

### 2) Generate the bundle
Run `bb audit-bundle` with *curated* `--query` and *narrow* `--glob`.

Rules:
- Prefer 3–8 queries.
- Prefer 2–5 globs.
- Keep context small (2–3) unless you need more.

### 3) Load + query with Lattice
- Load the bundle once.
- Run repeated `(grep ...)` / `(filter ...)` / `(count ...)` in Lattice.
- Expand only the lines needed for conclusions.

## Prompt → query/glob heuristics (Option A)

### Default globs
- Always start with: `src/**` and `docs/**`
- Add if relevant:
  - `shadow-cljs.edn` (frontend build wiring)
  - `resources/**` (HTML, models, assets)
  - `config/**` (settings, secrets placeholders)
  - `test/**` (when debugging tests)

### Good query types
- Exact namespace/function: `app.template.frontend.core/init`
- Config key: `:init-fn`, `:builds`, `:routes`
- Route path: `"/admin"`, `"/api/v1"`
- Known symbols from the prompt (entity names, event ids, API endpoints)

## Minimal example (session bootstrap)
1) Morph: find the likely entrypoints + docs.
2) Bundle:
   - `bb audit-bundle --query '<q1>' --query '<q2>' --glob 'src/**' --glob 'docs/**' --label <slug>`
3) Lattice:
   - load the bundle
   - grep the key claims
   - report/fix based on `path:line` evidence
