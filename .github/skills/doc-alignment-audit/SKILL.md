---
description: "Hybrid doc-vs-code alignment audits using Morph discovery + audit-bundle + Lattice evidence queries"
tags: ["docs", "alignment", "audit", "morph", "lattice", "routing", "architecture"]
---

# doc-alignment-audit

Use this when you need an evidence-based comparison between docs and code.

## Default workflow
1) Discover the corpus with Morph search.
2) Generate an evidence bundle file with `bb audit-bundle` (writes to `target/audit-bundles/`).
3) Load the bundle into Lattice and extract exact `path:line` evidence.

## What “good” looks like
- Report as: Claim → Evidence (with `path:line`).
- Expand only the minimum lines needed to support conclusions.
- Re-run the audit after fixes.
