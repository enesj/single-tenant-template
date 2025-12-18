<!-- ai: {:namespaces [app.shared.type-conversion] :tags [:shared :data :single-tenant] :kind :reference} -->

# Type Conversion

Centralized casts/coercions used by the single-tenant admin backend and UI.

- **Location**: `src/app/shared/type_conversion.cljc` (entrypoint) + split implementation in `src/app/shared/type_conversion_db.cljc`
- **Purpose**: Provide HoneySQL-friendly casts and safe value coercion so inserts/updates and JSON responses stay consistent.

## Core helpers
- `cast-field-value` – wrap values with the correct SQL cast (uuid, timestamptz, jsonb, etc.).
- `prepare-data-for-db` – apply casts to a data map before insert/update; optional flags to include nils or preserve unknown fields.
- `parse-number` – robust parsing of numeric strings to integer or decimal types. It uses regex validation to ensure consistent behavior across CLJ and CLJS (e.g. preventing `js/parseFloat` from incorrectly parsing "123.45.67").
- `convert-to-type` / `detect-field-type` – convenience coercions for common types (string/number/boolean/json/uuid/inst).

## Usage notes
- Use casts when building HoneySQL maps for write operations to avoid PG type errors.
- Keep conversion logic centralized here instead of ad hoc `:raw` SQL fragments.
- No tenant-specific behavior exists; everything operates on the single-tenant schema.
