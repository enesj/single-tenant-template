---
description: "Hybrid doc-vs-code alignment audits using Morph discovery + audit-bundle + Lattice evidence queries"
tags: ["docs", "alignment", "audit", "morph", "lattice", "routing", "architecture"]
---

# doc-alignment-audit

Use this when you need an evidence-based comparison between docs and code.

This skill is deliberately **evidence-first**:
- Find the doc claim.
- Find the code truth.
- Present the result as **Claim → Evidence (with `path:line`)**.

## Default workflow
1) Discover the corpus with Morph search.
2) Generate an evidence bundle file with `bb audit-bundle` (writes to `target/audit-bundles/`).
3) Load the bundle into Lattice and extract exact `path:line` evidence.

## What “good” looks like
- Report as: Claim → Evidence (with `path:line`).
- Expand only the minimum lines needed to support conclusions.
- Re-run the audit after fixes.

## Lattice usage rules (read this twice)

### The one common failure mode
If you mean “search/filter/count/extract”, you must call **`mcp_lattice_lattice_query`**.

`mcp_lattice_lattice_status` is **diagnostic only** (it does not run searches). If the agent ever “keeps using status”, that’s a bug in tool choice, not a workflow choice.

### Non-negotiable mapping (tool → purpose)
- **Load a document once:** `mcp_lattice_lattice_load`
- **Run Nucleus queries (grep/filter/count/match/lines):** `mcp_lattice_lattice_query`
- **Inspect actual results from a handle:** `mcp_lattice_lattice_expand`
- **See which handles exist:** `mcp_lattice_lattice_bindings`
- **Reset handles (keep doc loaded):** `mcp_lattice_lattice_reset`
- **Check session state (rare):** `mcp_lattice_lattice_status`

### Practical guardrails
- After `mcp_lattice_lattice_load`, your **next** Lattice call is almost always `mcp_lattice_lattice_query`.
- Only call `status` when you are explicitly debugging “is a document loaded / are handles present?”.
- If you accidentally call `status` when you meant to search: immediately follow with the intended `mcp_lattice_lattice_query`.

## Lattice query patterns you should copy/paste

### 1) Load the audit bundle
Load the file created by `bb audit-bundle` (it’s a plain text evidence bundle).

### 2) Grep for a claim or symbol
Run via `mcp_lattice_lattice_query`:
- `(grep "<string>")` for matching
- Prefer escaping dots when searching for namespaces: `app\\.admin\\.frontend\\.core/init`

### 3) Narrow results (docs vs code)
If your bundle includes file paths in each line (typical), filter by path prefix:
- Docs-only evidence: filter matches containing `docs/`
- Code-only evidence: filter matches containing `src/` (or other code roots)

Example Nucleus commands (each is a separate `mcp_lattice_lattice_query` call):
- `(grep "app\\.admin\\.frontend\\.core/init")`
- `(filter RESULTS (lambda x (match x "docs/" 0)))`
- `(filter RESULTS (lambda x (match x "src/" 0)))`

### 4) Expand only what you need
After a query returns a handle (e.g. `$res1`), use `mcp_lattice_lattice_expand` to view a small slice:
- Expand first 10: `limit=10`
- Prefer readable line output: `format="lines"`

### 5) Count instead of dumping
When you just need “how many”, use:
- `(count RESULTS)`

## Minimal example (the “happy path”)

1) Create the bundle (shell):
   - `bb audit-bundle --query '<your queries>' --glob 'docs/**' --glob 'src/**' --label <label>`

2) Load it (Lattice):
   - `mcp_lattice_lattice_load` with `filePath` pointing at the bundle under `target/audit-bundles/`.

3) Query it (Lattice):
   - `mcp_lattice_lattice_query` with `(grep "<key phrase>")`.

4) Expand evidence lines (Lattice):
   - `mcp_lattice_lattice_expand` on the returned handle with `limit=10` and `format="lines"`.

5) Report:
   - Claim → Evidence (include `path:line` entries; quote only what’s necessary).

## Troubleshooting

### “The agent keeps using `status` instead of querying”
- Treat this as a tool-selection bug.
- Correct action: do not repeat `status`; proceed with `mcp_lattice_lattice_query` using the intended Nucleus command (usually `(grep "...")`).

### “I can’t see my results / what handle do I have?”
- Call `mcp_lattice_lattice_bindings` to list active handles.
- If you have a handle, expand a small preview with `mcp_lattice_lattice_expand`.

### “I ran a query and now results are confusing”
- Use `mcp_lattice_lattice_reset` to clear handles (document stays loaded), then start again with a clean `(grep ...)`.
