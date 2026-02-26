# Allium Review Prompt — Admin Code

Review current-session **admin-scope** changes and produce a report using the same structure as `reviewer-allium-report.prompt.md`.

## Scope

Focus on:

- `src/app/admin/**`
- admin API/domain route composition and admin handlers under `src/app/domain/backend/**`
- admin-facing frontend pages/components used for settings, list controls, and domain admin UX

Exclude unrelated user-only/domain-only code unless required for boundary reasoning.

## Primary specs to consult

- `specs/allium/template/authorization.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/domain/expenses/implementation.allium`
- `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` (only if receipt/admin OCR surfaces are touched)
- `specs/allium/README.md`

## Output format (must match)

1. **Allium review verdict** (`pass` or `misaligned`)
2. **Evidence**
   - changed files reviewed
   - spec files consulted
3. **Precise mismatch list** (if misaligned)
4. **Recommended fix direction**
   - update `specs/allium/domain/expenses/implementation.allium` and/or relevant template specs to model observed admin route/surface behavior
   - if code should follow existing spec instead, state exact revert/correction direction
5. **Residual risks**
6. **Commit status** (`committed` with hash/message, or `not committed` with reason)

## Notes

- Keep findings evidence-based with file paths and concrete behavior impact.
- Do not implement product-code fixes during review; only report or update docs/specs when policy allows.
- If alignment passes and required checks pass, commit reviewed changes in one commit.