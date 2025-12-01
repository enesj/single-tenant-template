<!-- ai: {:tags [:shared :single-tenant] :kind :note} -->

# Template/Domain Integration

This single-tenant template does not implement a separate template/domain layering or tenant-aware utilities. Shared code is focused on cross-platform helpers (database adapters, HTTP, auth, pagination, strings, dates). If you add multi-application reuse in the future, create a new guide that reflects the updated architecture.
