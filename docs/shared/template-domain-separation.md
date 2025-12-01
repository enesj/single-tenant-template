<!-- ai: {:tags [:shared :single-tenant] :kind :note} -->

# Template/Domain Separation

The current codebase is single-tenant and does not maintain a template/domain split. Shared utilities are used directly by the admin backend and UI. If a future multi-app separation is needed, document the new boundaries here and ensure shared helpers remain platform-neutral.
