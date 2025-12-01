<!-- ai: {:tags [:shared :platform] :kind :guide} -->

# Platform-Specific Notes

Shared code aims to be cross-platform; when JVM or browser specifics are required, split into `.clj` and `.cljs` with the same API surface.

## Guidelines
- Keep the public API identical across platforms; hide platform details behind the same function names.
- JVM-only pieces (e.g., JDBC/PG handling) belong in `.clj` namespaces inside `app.shared.adapters` or similar.
- Browser-only helpers (e.g., DOM or window access) should stay in frontend code, not in `app.shared`.
- Prefer pure functions in `app.shared` so they can be used by both backend services and the admin UI.
