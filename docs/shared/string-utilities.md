<!-- ai: {:namespaces [app.shared.string] :tags [:shared :string :single-tenant] :kind :reference} -->

# String Utilities

Cross-platform string helpers used by the admin backend and UI.

- **Location**: `src/app/shared/string.cljc`
- **Highlights**: casing (kebab/snake/camel), slug/email helpers, trimming/blank checks, safe string formatting used in tables and exports.

## Notes
- Keep UI-visible formatting (e.g., slugs, titles) consistent by reusing these helpers.
- Functions are Clojure/ClojureScript compatible unless noted.
