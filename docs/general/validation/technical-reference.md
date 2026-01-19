<!-- ai: {:tags [:validation :reference :single-tenant] :kind :reference} -->

# Validation Technical Reference

Key namespaces and helpers used to process validation metadata for the single-tenant app.

## Namespaces
- `app.shared.validation.metadata` – extract/normalize `:validation` blocks from `models.edn`; build validation specs and UI hints.
- `app.shared.field-specs` – merge validation info into field specs for consumers.
- `app.shared.validation.builder` – build Malli validators from field specs for backend use.
- `app.shared.patterns` – common regex/pattern helpers to keep validation rules consistent.

## Core functions (representative)
- `extract-validation-metadata` – pull `:validation` from a field constraints map.
- `generate-field-validation-spec` – combine field type + constraints + messages + UI hints into a spec map.
- `generate-malli-schema` / builder helpers – produce Malli-compatible schemas/validators from specs.
- Field spec helpers – expose `:input-type`, `:placeholder`, `:validation-type`, `:validation-constraints`, `:messages` for UI rendering.

## Metadata shape (example)
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

## Supported validation types (common)
- `:text`, `:email`, `:phone`, `:url`
- `:number`
- `:date`, `:datetime`
- `:enum`, `:boolean`, `:json`

## Notes
- Validation metadata is optional; fields without it still work but skip enhanced UX/messages.
- Schema generation ignores validation metadata—it only affects validators and UI hints.
- Keep patterns/messages centralized to avoid backend/frontend drift.
