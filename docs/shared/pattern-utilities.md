<!-- ai: {:namespaces [app.shared.patterns] :tags [:shared :validation :single-tenant] :kind :reference} -->

# Pattern Utilities

Regex patterns and validation helpers shared across the single-tenant admin app.

- **Location**: `src/app/shared/patterns.cljc`
- **Contents**: email/slug/url patterns, phone/date formats, lightweight validators used by both backend validation and admin UI forms.

## Notes
- Keep new patterns here to avoid drift between backend and frontend validation.
- Prefer these shared predicates over re-implementing regexes in feature code.
