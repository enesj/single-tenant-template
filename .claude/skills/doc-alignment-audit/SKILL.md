---
description: "Hybrid doc-vs-code alignment audits using Morph discovery + audit-bundle + Lattice evidence queries"
allowed-tools:
  - morph-mcp:warpgrep_codebase_search
  - lattice:lattice_load
  - lattice:lattice_query
  - lattice:lattice_expand
---

# doc-alignment-audit

Use this when you need an **evidence-backed** comparison between documentation and the codebase.

This skill standardizes the workflow:
1) **Discover** the corpus quickly (Morph search)
2) **Bundle** relevant evidence into a single provenance-preserving file (`bb audit-bundle`)
3) **Query** and extract exact lines (Lattice)

## Use when
- “Check the docs vs the app code”
- “Are these docs aligned with implementation?”
- “Find the mismatch between docs and code, then fix docs/config”
- Any cross-file audit where you need **quotes + exact file:line evidence**

## Why this approach
- **Morph** is best for *finding* the right files quickly.
- **Lattice** is best for *iterating* over a fixed corpus and extracting precise evidence.
- The **audit bundle** bridges them: one file that preserves `path:line` provenance, optimized for Lattice queries.

## Step 1 — Scope the corpus (Morph)
Run 1–2 focused searches to find the canonical docs and the canonical code entry points.

Examples of good discovery queries:
- “Find frontend routing docs and the actual router init/start code”
- “Find references to the admin shadow build init function and entrypoint”

Collect:
- A shortlist of files
- The “claim strings” you want to verify (namespaces, fns, config keys, URLs)

## Step 2 — Generate an audit bundle (Babashka)
Create a single file under `target/audit-bundles/` containing lines like:
- `[MATCH] path:line: ...`
- `[CTX]   path:line: ...`

Recommended usage patterns:
- Keep the query set small and specific.
- Increase context (`--context`) only when needed.

Example:
- Include docs + code + build config:
  - `--glob docs/**`
  - `--glob src/**`
  - `--glob shadow-cljs.edn`

## Step 3 — Evidence extraction (Lattice)
Load the bundle and run greps/filters to extract only the exact lines you need.

Typical queries:
- Find a disputed entrypoint:
  - `(grep "app\\.admin\\.frontend\\.core/init")`
- Find shadow init-fn lines:
  - `(grep "\\[MATCH\\].*:init-fn")`
- Split docs vs code:
  - `(filter RESULTS (lambda x (match x "^\\[MATCH\\] docs/" 0)))`

Then `lattice_expand` a small number of lines for the report.

## Reporting rules (non-negotiable)
- Always write “Claim → Evidence”.
- Evidence must include `path:line`.
- Prefer 5–15 evidence lines over dumping large files.
- If you fix mismatches, re-run the audit (or regenerate the bundle) to confirm alignment.

## Regenerate strategy
If evidence is missing:
- Add another `--query` (more specific string)
- Add another `--glob` (missed folder)
- Increase `--context` (if the match line alone is ambiguous)
