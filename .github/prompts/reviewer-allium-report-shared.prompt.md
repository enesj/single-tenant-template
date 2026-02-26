# Allium Review Prompt — Shared Code

Review current-session **shared-scope** changes and produce a report using the same structure as `reviewer-allium-report.prompt.md`.

## Scope

Focus on:

- `src/app/shared/**`
- cross-domain shared contracts (types, normalization, validation, field metadata, pagination helpers)
- shared frontend/backend adapters when they define cross-domain behavior

## Primary specs to consult

- `specs/allium/template/dry-principle.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- relevant domain specs that consume shared contracts (for example `specs/allium/domain/expenses/implementation.allium`)
- `specs/allium/README.md`

> Note: `specs/allium/shared/` is currently empty. If you find stable shared behavior not covered by existing specs, flag this as a spec-coverage gap and recommend adding a candidate spec.

## Output format (must match)

1. **Allium review verdict** (`pass` or `misaligned`)
2. **Evidence**
   - changed files reviewed
   - spec files consulted
3. **Precise mismatch list** (if misaligned)
4. **Recommended fix direction**
   - update relevant existing template/domain specs to model shared behavior
   - and, when necessary, add `specs/allium/shared/<topic>.candidate.allium` to close shared-contract gaps
   - if code should follow existing spec instead, state exact revert/correction direction
5. **Residual risks**
6. **Commit status** (`committed` with hash/message, or `not committed` with reason)

## Notes

- Keep findings contract-focused and cross-domain.
- Explicitly separate true behavior mismatches from missing-spec coverage.
- Do not implement product-code fixes during review; only report or update docs/specs when policy allows.
- If alignment passes and required checks pass, commit reviewed changes in one commit.