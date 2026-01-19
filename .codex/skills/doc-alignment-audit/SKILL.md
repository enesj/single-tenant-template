---
name: doc-alignment-audit
description: "Hybrid doc-vs-code alignment audits using Morph discovery + audit-bundle + Lattice evidence queries"
allowed-tools:
  - morph-mcp:warpgrep_codebase_search
  - lattice:lattice_load
  - lattice:lattice_query
  - lattice:lattice_expand
---

# doc-alignment-audit

Use this skill for any doc-vs-code alignment work where you need precise, quoteable evidence.

## Workflow
1) **Discover**: Use Morph search to find the relevant docs + code entry points.
2) **Bundle**: Use `bb audit-bundle` to create a provenance-preserving evidence file in `target/audit-bundles/`.
3) **Query**: Load the bundle into Lattice and extract exact “Claim → Evidence” lines.

## Output discipline
- Evidence must include `path:line`.
- Keep expansions small and targeted.
- After making fixes, re-run the audit to confirm alignment.
