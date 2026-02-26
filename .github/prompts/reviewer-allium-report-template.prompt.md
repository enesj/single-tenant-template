# Allium Review Prompt — Template Code

Review current-session **template-scope** changes and produce a report using the same structure as `reviewer-allium-report.prompt.md`.

## Scope

Focus on:

- `src/app/template/**`
- cross-cutting template middleware, routing composition, auth/authz boundaries
- reusable template frontend components/subscriptions/events used across domains

Include domain references only when needed to verify template boundary contracts.

## Primary specs to consult

- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/template/authentication.allium`
- `specs/allium/template/authorization.allium`
- `specs/allium/template/dry-principle.allium`
- `specs/allium/drafts/list-view-filtering.candidate.allium` (when list filtering touched)
- `specs/allium/drafts/list-view-sort.candidate.allium` (when list sorting touched)
- `specs/allium/README.md`

## Output format (must match)

1. **Allium review verdict** (`pass` or `misaligned`)
2. **Evidence**
   - changed files reviewed
   - spec files consulted
3. **Precise mismatch list** (if misaligned)
4. **Recommended fix direction**
   - update `specs/allium/template/*.allium` and relevant draft list-view specs to model observed template behavior
   - if code should follow existing spec instead, state exact revert/correction direction
5. **Residual risks**
6. **Commit status** (`committed` with hash/message, or `not committed` with reason)

## Notes

- Keep findings boundary-centric (middleware order, route composition, auth/authz, shared UI contracts).
- Do not implement product-code fixes during review; only report or update docs/specs when policy allows.
- If alignment passes and required checks pass, commit reviewed changes in one commit.