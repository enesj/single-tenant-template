<!-- ai: {:tags [:validation :troubleshooting :single-tenant] :kind :runbook} -->

# Validation Troubleshooting

Common issues when using metadata-driven validation in the single-tenant app.

## Forms show generic inputs/messages
- **Check models**: Confirm the field in `resources/db/models.edn` has a `:validation` block with `:ui` hints.
- **Rebuild/refresh**: Ensure the admin UI is loading the latest models/specs.
- **Shared patterns**: Use `app.shared.patterns` to avoid typos in regex/constraints.

## Backend returns generic errors
- Verify the validator is built from the updated specs (Malli builder uses the same metadata).
- Ensure custom `:messages` exist for the field; otherwise defaults are used.

## Constraints not applied
- Confirm the constraint keys match the supported ones for the validation type (`:min-length`, `:max-length`, `:pattern`, `:min-value`, `:max-value`, `:values`, etc.).
- For enum fields, list `:values` explicitly.

## Timestamps/JSON fields
- Use `:type :json` for JSONB with a helpful message; UI placeholder can be `{}`.
- For dates/datetimes, include appropriate input type and max/min rules where needed.

## General tips
- Start with high-traffic fields; add metadata incrementally.
- Keep messages concise and user-friendly.
- Fields without validation metadata still work—they just won’t get enhanced UX/errors.
