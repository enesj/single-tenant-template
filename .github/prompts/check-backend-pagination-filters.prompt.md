---
description: Check and fix filter regressions after backend pagination changes
argument-hint: "Failing pages (e.g. /admin/articles), working pages (optional), symptoms"
agent: "agent"
---

You are working in `/Users/enes/Projects/single-tenant-template`.

## Goal

Investigate and fix filter functionality regressions introduced after switching pages to backend pagination.

Primary example to validate: filters failing on `/admin/articles` while working on some other pages.

## Inputs

Use the user-provided arguments from this prompt invocation:
- failing pages/routes (required)
- working pages/routes for comparison (optional)
- observed symptoms (optional)

If inputs are missing, assume at least `/admin/articles` is failing and continue.

## Task

1. Reproduce the issue on each failing page and confirm expected filter behavior.
2. Compare failing vs working pages across the full filter pipeline:
   - frontend filter state, events, and query param construction
   - backend route/query-param parsing and allowlists
   - service/query filtering logic under pagination
3. Identify root cause(s), especially boundary mismatches:
   - snake_case vs kebab-case
   - filter key name drift between UI and backend
   - dropped params when pagination state changes
   - allowlist/normalization mismatches
4. Implement the smallest safe fix that keeps behavior consistent across pages.
5. Validate with focused checks:
   - happy path filtering
   - empty filter reset
   - invalid filter input handling
   - pagination + filtering interaction (page change, page size change)
6. Run focused tests only (backend/frontend as relevant), save output once under `tmp/` when using terminal runs.

## Output format

Return results in this structure:

- **Scope checked**: pages tested and which were broken
- **Root cause**: concise technical explanation
- **Files changed**: list with one-line purpose each
- **Verification**: tests/checks run and outcomes
- **Remaining risks**: any pages not yet verified

If you cannot reproduce, report exactly what was tried and the next highest-signal debugging step.