---
mode: agent
description: "Audit DRY implementation in domain code and execute prioritized refactor backlog without behavior changes."
---

# Domain DRY Audit + Refactor Backlog (Saved as Prompt)

## Summary
- Objective: audit how DRY is implemented across `src/app/domain/**`, then execute a behavior-preserving refactor backlog.
- Added requirement: persist this plan as an agent prompt in `.github/prompts`.
- Scope (default): all domain backend/frontend/shared modules plus focused domain tests under `test/app/domain/**`.

## Prompt Artifact (New)
- Create file: `.github/prompts/domain-dry-audit-refactor-backlog.prompt.md`
- Use prompt frontmatter format already present in repo:
  - `mode: agent`
  - `description: "Audit DRY implementation in domain code and execute prioritized refactor backlog without behavior changes."`
- Prompt body should include this full plan (sections below), so it is directly executable by another agent.

## Public APIs / Interfaces / Types
- No external API contract changes planned.
- Keep stable:
  - HTTP routes and response shapes.
  - Re-frame event/subscription keywords.
  - Domain route descriptor contract in `src/app/domain/shared/routes/expenses_user.cljc`.
  - Service map op keys from `services_factory`.

## Implementation Plan
1. Baseline audit document.
- Produce `tmp/domain-dry-audit.md` with current DRY mechanisms, duplication evidence, and P1/P2/P3 priorities.

2. Backend DRY consolidation: related-record helpers.
- Refactor `src/app/domain/backend/expenses/services/articles.clj` to reuse shared helpers from `src/app/domain/backend/expenses/services/related_records.clj` instead of local duplicates.

3. Frontend DRY consolidation: article related-record modal.
- Replace article-specific related-record event implementation in `src/app/domain/frontend/expenses/events/articles.cljs` with `register-related-records-events!`.
- Replace article-specific related-record subs in `src/app/domain/frontend/expenses/subs/articles.cljs` with `register-related-records-subs!`.
- Preserve existing article keyword contracts.

4. Backend handler helper extraction.
- Centralize duplicated pagination parsing used in:
  - `.../handlers/user_expenses/reference_data.clj`
  - `.../handlers/user_expenses/supplier_aliases.clj`
  - `.../handlers/user_expenses/supplier_detail.clj`
- Centralize duplicated service-op resolver logic currently duplicated between:
  - `.../routes/routes_factory.clj`
  - `.../handlers/user_expenses/reference_data.clj`

5. Route config dedupe cleanup.
- Replace repeated `{:search (:search qp)}` lambdas in `src/app/domain/backend/expenses/routes/route_configs.clj` with shared helper(s).

6. Post-refactor audit update.
- Extend `tmp/domain-dry-audit.md` with before/after duplication map and deferred optional work.

7. Persist reusable prompt artifact.
- Save this finalized plan into `.github/prompts/domain-dry-audit-refactor-backlog.prompt.md`.

## Test Cases and Scenarios
- Run focused existing tests:
  - `test/app/domain/shared/routes/expenses_user_test.cljc`
  - `test/app/domain/backend/registry_test.clj`
  - `test/app/domain/frontend/registry_test.cljs`
- Add focused tests for new shared helpers:
  - related-record helper behavior (`nil`, empty, invalid type, limit bounds, dedupe ordering)
  - pagination helper parsing (`nil`, non-numeric, negative, >max)
  - service resolver precedence and missing-op errors
- Minimum validation matrix: happy path, `nil`, empty collections, invalid/boundary inputs.

## Assumptions and Defaults
- Filename default chosen: `.github/prompts/domain-dry-audit-refactor-backlog.prompt.md`.
- No migration/schema work.
- No behavior or public contract changes unless explicitly marked as bug fixes in audit notes.
- Refactors are internal DRY improvements only.
