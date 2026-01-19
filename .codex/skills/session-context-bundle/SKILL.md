---
name: session-context-bundle
description: "Session bootstrap: Morph discovery → bb audit-bundle → Lattice queries (Option A, agent-curated queries)"
allowed-tools:
  - morph-mcp:warpgrep_codebase_search
  - lattice:lattice_load
  - lattice:lattice_query
  - lattice:lattice_expand
---

# session-context-bundle

Use this at the start of a session to generate a prompt-scoped evidence bundle (with `path:line`) and then use Lattice to query it.

## Decision guide
Use this when **2+** are true:
- The prompt implies multi-file work (flows/refactors/debugging)
- You expect iterative searching
- You need `path:line` evidence

Skip it when:
- Single-file, small edit
- Purely conceptual discussion

Rule of thumb: if you expect >~5 repo searches, bootstrap a bundle.

## Workflow (Option A)
1) Morph discovery: identify files + keywords from the prompt.
2) Run `bb audit-bundle` with curated `--query` and `--glob`.
3) Load the bundle into Lattice and use it as the main corpus for the session.

## Defaults
- Start with globs: `src/**`, `docs/**`
- Add: `shadow-cljs.edn`, `resources/**`, `config/**`, `test/**` as needed

## Output discipline
- Keep bundles small and focused.
- Prefer Claim → Evidence with `path:line` in reports.
