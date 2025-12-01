<!-- ai: {:namespaces [app.shared.type-conversion] :tags [:shared :data :single-tenant] :kind :reference} -->

# Type Conversion

Centralized casts/coercions used by the single-tenant admin backend and UI.

- **Location**: `src/app/shared/type_conversion.cljc`
- **Purpose**: Provide HoneySQL-friendly casts and safe value coercion so inserts/updates and JSON responses stay consistent.

## Core helpers
- `cast-field-value` – wrap values with the correct SQL cast (uuid, timestamptz, jsonb, etc.).
- `prepare-data-for-db` – apply casts to a data map before insert/update; optional flags to include nils or preserve unknown fields.
- `convert-to-type` / `detect-field-type` – convenience coercions for common types (string/number/boolean/json/uuid/inst).

## Usage notes
- Use casts when building HoneySQL maps for write operations to avoid PG type errors.
- Keep conversion logic centralized here instead of ad hoc `:raw` SQL fragments.
- No tenant-specific behavior exists; everything operates on the single-tenant schema.
