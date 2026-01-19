<!-- ai: {:tags [:validation :architecture :single-tenant] :kind :overview} -->

# Validation System Overview

Validation is metadata-driven and defined in `resources/db/models.edn`. The same metadata feeds backend validators and admin UI form hints.

## What it does
- **Single source**: Define `:validation` metadata next to fields; reuse across stack.
- **Cross-platform**: Shared code builds Malli validators (backend) and form specs (admin UI) from the same metadata.
- **UX hints**: Input types, placeholders, autocomplete, and friendly messages come from the metadata.
- **Schema safety**: Validation metadata does not affect schema generation.

## Core pieces
- `app.shared.validation.metadata` – extract/normalize validation metadata from model fields.
- `app.shared.field-specs` – merge validation info into field specs for consumers.
- `app.shared.validation.builder` – build Malli validators from specs for the backend.
- Admin UI form helpers consume the same specs for client-side validation and UI hints.

## Example metadata
```edn
[:email [:varchar 255]
 {:null false
  :validation {:type :email
               :constraints {:pattern "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                             :max-length 255}
               :messages {:invalid "Please enter a valid email address"
                          :required "Email is required"}
               :ui {:input-type "email"
                    :placeholder "Enter your email address"
                    :autocomplete "email"}}}]
```

## Supported types (common)
- Text: `:text`, `:email`, `:phone`, `:url`
- Numeric: `:number`
- Date/time: `:date`, `:datetime`
- Other: `:enum`, `:boolean`, `:json`

## Integration points
- **Models**: add `:validation` blocks in `resources/db/models.edn`.
- **Backend**: Malli validators built from specs; return consistent error messages.
- **Frontend**: Admin forms use the same specs for types/placeholders/messages and client-side checks.

## Adoption tips
- Start with high-impact fields (email/name/password).
- Keep patterns and messages concise; reuse shared patterns from `app.shared.patterns`.
- Validation metadata is optional—fields without it still work, but lack the enhanced UX/errors.
