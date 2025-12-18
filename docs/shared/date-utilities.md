<!-- ai: {:namespaces [app.shared.date] :tags [:shared :datetime :single-tenant] :kind :reference} -->

# Date/Time Utilities

Shared date/time helpers for the single-tenant admin app.

- **Location**: `src/app/shared/date.cljc` (entrypoint) + split implementation in `src/app/shared/date_{core,arithmetic,range}.cljc`
- **Capabilities**: instant/ISO parsing, formatting helpers, timezone-safe utilities for analytics timestamps, and human-readable displays in the admin UI.

## Usage notes
- Normalize server timestamps to epoch millis or ISO strings before sending to the UI.
- Prefer these helpers over ad hoc `java-time` / `js/Date` handling to keep formatting consistent.
