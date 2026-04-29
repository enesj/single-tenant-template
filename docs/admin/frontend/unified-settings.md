<!-- ai: {:tags [:frontend :admin :settings :user-settings] :kind :guide} -->

# Unified Settings (Admin & User)

This guide covers the unified Settings UI — a shared page component that manages display options, form fields, table columns, list behavior, and left-sidebar navigation for the application, across both admin and user scopes.

## Routes

| Route | Scope | Purpose |
|-------|-------|---------|
| `/admin/admin-settings` | Admin | Configure defaults and locks for **admin panel** pages |
| `/admin/user-settings` | User | Configure defaults and locks for **user-facing** (domain) pages |

Both routes share the same page component (`unified_settings/page.cljs`) and editor components (`unified_settings/editors.cljs`), differing only in which scope they read from and write to.

## Architecture Overview

### Two Scopes

The settings system has two independent configuration scopes:

- **Admin scope** — Controls admin panel pages (`/admin/articles`, `/admin/suppliers`, etc.). Settings are stored with `scope=admin` in the database.
- **User scope** — Controls user-facing domain pages (`/expenses/articles`, `/t/:tenant/receipts`, etc.). Settings are stored with `scope=user` in the database.

Each scope has four configuration categories: **View Options**, **Form Fields**, **Table Columns**, and **Sidebar Navigation**.

### Data Flow

```
DB runtime store (frontend_runtime_configs)
  seeded from EDN defaults at bootstrap
        │
        ▼
Backend I/O layer (settings_io.clj)
  reads directly from DB
        │
        ▼
GET /admin/api/settings (admin) or /admin/api/settings/user-ui-config (user)
        │
        ▼
Frontend events store config in app-db
  [:admin :settings :*] (admin scope)
  [:domain :config :*]  (user scope)
        │
        ▼
Resolver (settings/resolver.cljs)
  route-aware resolution with precedence chain
        │
        ▼
Subscriptions (subs/ui.cljs, subs/config.cljs)
  effective display settings, column visibility, list config
        │
        ▼
Live pages consume resolved settings
```

### Config Storage

Settings are persisted in the `frontend_runtime_configs` database table:

| Column | Description |
|--------|-------------|
| `scope` | `admin` or `user` |
| `config_key` | `view-options`, `form-fields`, `table-columns`, or `navigation` |
| `config_edn` | EDN-encoded configuration map |

Source-controlled EDN files provide bootstrap defaults that seed the DB for fresh environments (read once at startup, not at request time):

| Scope | Files (bootstrap defaults) |
|-------|-------|
| Admin | `src/app/admin/frontend/config/{view-options,form-fields,table-columns,navigation}.edn` |
| User (template) | `src/app/template/frontend/config/{view-options,form-fields,table-columns,navigation}.edn` |
| User (domain) | `src/app/domain/frontend/expenses/config/{view-options,form-fields,table-columns,navigation}.edn` |

### Resolution Precedence

**Display settings** resolve with this precedence (highest wins):

1. Feature-constraint locks (e.g., `read-only?` locks out edit/delete/add)
2. View-options locks (admin- or user-configured locks)
3. User preferences (`[:ui :entity-prefs :entity :display]`, browser-local)
4. Entity defaults (from `entities.edn` model metadata)
5. Fallback defaults (hardcoded in `resolver.cljs`)

**Column visibility** resolves with this precedence:

1. Policy locks (`column-locks`)
2. User choice (explicit user-selected column set)
3. Policy defaults (`column-defaults`)
4. Config defaults (from admin or domain `table-columns.edn`)
5. Always-visible columns (always forced visible)

The resolver function `resolve-config-source` in `settings/resolver.cljs` is **route-aware**: on admin routes it reads from `[:admin :settings :*]` with domain fallback; on user routes it reads from `[:domain :config :*]` with admin fallback.

## Page Modes

### View Mode

Shows a read-only overview of all entity settings as cards grouped by domain:

| Group | Icon | Example Entities |
|-------|------|------------------|
| Expenses Management | 💰 | Articles, Suppliers, Stores, Categories, Receipts, ... |
| User Management | 👥 | Users, Admins, Tenants |
| Security & Audit | 🔒 | Audit Logs, Login Events |
| Project Management | 📋 | Backlog |

Each entity card shows:
- **Badge summary** — e.g., "4 defaults, 1 lock"
- **Setting rows** — Edit, Delete, Selection, Filtering, Pagination, Highlights, Add Button, Batch Edit, Batch Delete, Rows per page — each showing `Default state | Lock state`
- **List Behavior** section — Form display, Disallowed action mode, Action gates with gate count badge

### Edit Mode

Activated by clicking **"Edit Settings"** (button toggles to **"Stop Editing"**).

- An **entity selector** dropdown appears for choosing which entity to configure
- Four **editor tabs** appear: View Options, Form Fields, Table Columns, Sidebar
- **Save/Discard** buttons appear when changes are pending (dirty state)

**Important behaviors:**
- Dirty state is **global** (not per-entity). Discard reverts all pending changes across all entities.
- Clicking **"Stop Editing" auto-saves** any pending View Options changes without a confirmation dialog. Use "Discard" first to revert.
- Entity switching does **not** block when changes are pending.

## Editor Tabs

### View Options (📋)

Controls display toggles, per-page setting, and list behavior for the selected entity.

#### Display Toggles

Nine boolean settings with **tristate** controls (two buttons per setting: Default and Lock):

| Setting | Controls |
|---------|----------|
| `show-edit?` | Row edit action buttons |
| `show-delete?` | Row delete action buttons |
| `show-select?` | Row + header checkboxes |
| `show-filtering?` | Filter controls (icons, inputs) |
| `show-pagination?` | Page controls and page-count text |
| `show-highlights?` | Row highlight classes for recently created/updated |
| `show-add-button?` | Add/New entity button in toolbar |
| `show-batch-edit?` | Batch edit button (visible when rows selected) |
| `show-batch-delete?` | Batch delete button (visible when rows selected) |

**Tristate cycles:**
- Defaults: `Inherit → Default On → Default Off → Inherit`
- Locks: `Inherit → Locked On → Locked Off → Inherit`

A **"All toggles"** bulk row toggles all nine settings simultaneously.

#### Rows Per Page

Two dropdowns: Default and Lock. Options: — (inherit), 5, 10, 20, 25, 50, 100.

The hardcoded fallback default is `25` (defined in `resolver.cljs`).

#### List Behavior

| Setting | Type | Options |
|---------|------|---------|
| Form display | Dropdown | —, Inline, Modal |
| Disallowed action mode | Dropdown | —, Hide, Disable |
| Add gate | Dropdown | No gate + 22 permission gates |
| Edit gate | Dropdown | No gate + 22 permission gates |
| Delete gate | Dropdown | No gate + 22 permission gates |
| Selection gate | Dropdown | No gate + 22 permission gates |

**Action gates** restrict actions based on user permissions. Available gates are defined in `definitions.cljs` and include expenses-domain permissions like `:expenses/can-write`, `:expenses/power-user`, `:expenses/articles.manage`, etc.

**Note:** Action gates are **bypassed on admin routes** — admin users always have full access. Gates only have observable effect on user-facing pages where the gate check evaluates against the user's membership role.

#### Save Semantics

View Options and List Behavior use a **draft model**: changes are accumulated locally and persisted only when clicking **"Save settings"**. The save dispatches `PUT /admin/api/settings` with the full payload.

### Sidebar (🧭)

Configures the left sidebar structure for the current scope.

Supported edits:

- **Sidebar title** — changes the title shown at the top of the sidebar.
- **Group label** — changes section/group headings, including Bosnian user-facing labels.
- **Item label** — changes individual navigation item labels.
- **Show** — hides or reveals an individual sidebar link without deleting it from the config.
- **Up / Down** — moves an item within its current group.
- **Group dropdown** — moves an item between sidebar groups.

The navigation config intentionally stores only editable structure (`:title`, `:sections`, item `:id`, item `:label`, optional item `:visible?`). Runtime-only data such as icons, routes, active states, permission checks, badges, and tenant URL prefixes remains in code so settings changes cannot create broken links or bypass access control.

Save semantics: **draft model** with the shared Save/Discard buttons. Admin scope saves `navigation` through `PUT /admin/api/settings`; user scope saves it through `PUT /admin/api/settings/user-ui-config`.

### Form Fields (📄)

Configures which fields appear in the Add (create) and Edit forms for the selected entity.

Two sections:
- **Create Form Fields** — checkboxes for fields shown when creating a new record
- **Edit Form Fields** — checkboxes for fields shown when editing an existing record

#### Save Semantics

Form field changes **save immediately** via `PATCH /admin/api/settings/form-fields/entity`. There are no Save/Discard buttons for this tab.

#### Known Limitation

The form-fields editor builds its field checklist from model specs and table-columns config. On certain entities (e.g., Articles), the field universe may not perfectly match what the live form actually renders, because forms use a separate `form-entity-specs` resolution path. This is a known gap documented in `ADMIN-ARTICLES-SETTINGS-VERIFICATION-RESULTS.md`.

### Table Columns (📊)

Configures column presentation for the selected entity's list/table view.

Two sections:

#### Columns Policy Card

Controls column **visibility defaults and locks** with tristate toggles (same cycle as View Options). Shows badges for defaults, locks, and enforced (always-visible) columns.

Save semantics: **draft model** with Save/Discard buttons.

#### Table Columns Configuration Grid

A table with drag-reorder support. Each column row has:

| Control | Description |
|---------|-------------|
| Column key | Technical column name (e.g., `canonical_name`) |
| Display Label | Text input — custom label override for the column header |
| In table | Checkbox — whether the column is available in the table |
| Always Visible | Checkbox — column cannot be hidden by users |
| Default Visible | Checkbox — column is shown by default |
| Filterable | Checkbox — column has a filter control |
| Sortable | Checkbox — column header is clickable for sorting |
| Drag handle | ⋮⋮ button for reordering columns |

A **"Toggle All"** row provides header-level checkboxes for bulk toggling.

#### Save Semantics

Structural edits (label, filterable, sortable, available/default-visible checkboxes) **save immediately** via `PATCH /admin/api/settings/table-columns/entity`.

## User Settings (`/admin/user-settings`)

The user-settings page configures defaults and locks for **user-facing domain pages** — the pages regular users interact with (e.g., `/t/:tenant/receipts`, `/t/:tenant/expenses/list`).

It shares the same page and editor components as admin settings but operates on the **user scope** with different save semantics, different entity groups, and admin lock awareness.

### Entity Groups

User settings organize entities into different groups than admin settings:

| Group | Icon | Example Entities |
|-------|------|------------------|
| Workspace | 🏢 | (template-level entities if any) |
| Domain (Expenses) | 💰 | Articles, Suppliers, Stores, Categories, Receipts, Expenses, etc. |

The entity list comes from `entities-for-scope :user` in `definitions.cljs` and includes domain-registered entities that have user-facing list pages.

### How It Differs from Admin Settings

#### Draft-Based Save Pattern

Unlike admin settings where form-fields and table-columns save immediately via PATCH, **all** user settings changes use a **draft model**:

1. Changes accumulate in `[:admin :user-settings :draft]`
2. The saved state is preserved in `[:admin :user-settings :saved]`
3. Clicking **"Save settings"** dispatches `PUT /admin/api/settings/user-ui-config` with the full draft
4. **"Discard"** resets the draft to the last saved state

This means form-field toggles and table-column checkboxes are **not** immediately persisted — they require an explicit save.

#### Admin Lock Overlay

When viewing user settings, admin-imposed locks are visible as **"Enforced"** labels:

- If an admin has locked `show-delete? = false` in `/admin/admin-settings`, the Delete row in the user-settings editor shows a non-interactive "Enforced Off" label instead of a tristate button
- The user cannot override the admin lock — the toggle is read-only
- This works because the user-settings page loads admin config (`load-admin? true` in `page.cljs`) and the `user-entity-editor` component reads `admin-view-options` to build an `immutable-locks` overlay

#### Config Loading

On page init, two configs are loaded:

| Config | Source | Stored In |
|--------|--------|-----------|
| User config | `GET /admin/api/settings/user-ui-config` | `[:domain :config :*]` + `[:admin :user-settings :draft]` |
| Admin config | `GET /admin/api/settings` + form-fields + table-columns | `[:admin :settings :*]` |

The admin config is read-only on this page — it is only used to display inherited lock state.

#### Config Destination

When user settings are saved, the backend writes to the `frontend_runtime_configs` table with `scope=user`. On the next page load of a user-facing domain page, `/api/v1/config` returns this config, which is stored in `[:domain :config :*]` and consumed by the resolver for user routes.

### What User Settings Control on Live Pages

User settings configure the **same three categories** as admin settings, but for user-facing pages:

| Category | Effect on Live User Pages |
|----------|--------------------------|
| **View Options** | Display toggles (edit, delete, selection, filtering, pagination, highlights, add, batch edit/delete), per-page, list behavior (form display mode, disallowed action mode, action gates) |
| **Form Fields** | Which fields appear in create/edit forms on domain list pages |
| **Table Columns** | Column visibility, display labels, filterable/sortable flags, column order |

#### Action Gates on User Pages

Unlike admin routes where gates are bypassed, action gates on user-facing pages are **enforced**:

- The gate check evaluates the user's **membership role** from the session
- If the user lacks the required permission, the action is either **hidden** or **disabled** depending on the `disallowed-action-mode` setting
- The `gate-allows-action?` function in `list.cljs` performs the check (with an `admin-route?` short-circuit for admin pages)

#### Per-Page on User Pages

The `::seed-per-page-from-config` event seeds the list component's per-page value from domain config on first load. The resolution path:

1. `/api/v1/config` loads domain config → `[:domain :config :view-options :entity :display-defaults :per-page]`
2. `::entity-display-settings` subscription resolves per-page from domain view-options
3. `configured-per-page` in `list.cljs` uses the resolved value
4. A `local-per-page?` guard skips seeding when the user has explicit browser-local preferences

#### Column Label Overrides on User Pages

Custom display labels saved in user-settings table-columns propagate to live page headers:

1. Admin saves label → `[:domain :config :table-columns :entity :column-metadata :col :label]`
2. `entity_specs.cljs` reads from domain config on user routes via `resolve-config-source`
3. `resolve-column-label-override` prioritizes explicit `:label` over `:label-key` (i18n translation)
4. Live page headers reflect the override; clearing the label falls back to i18n translation

### Events

User settings events are in `src/app/admin/frontend/events/user_settings.cljs` (facade) with sub-files:

| Sub-file | Purpose |
|----------|---------|
| `load_save.cljs` | Init, load from API, save to API, discard draft |
| `view_options.cljs` | Display toggle drafts, list config drafts, action gate drafts |
| `form_fields.cljs` | Form field toggle drafts |
| `table_columns.cljs` | Table column drafts (structural + policy) |
| `entities.cljs` | Entity selection and entity-specific reset |
| `tabs.cljs` | Tab switching |
| `subs.cljs` | Subscriptions for dirty state, loading, saving, errors |

### Verified Behaviors

From `USER-SETTINGS-OPTIMIZATION-RESULTS.md`:

| Feature | Status |
|---------|--------|
| Admin lock visibility in user-settings editor | Working — locks show as "Enforced" |
| Admin lock propagation to live user pages (admin session) | Working — `overlay-admin-locks` merges locks |
| Form-fields editor field universe | Working — uses route-independent model specs |
| Per-page seeding on user-facing pages | Working — domain config path resolves correctly |
| Column label overrides on user routes | Working — explicit `:label` wins over `:label-key` |

## Admin vs User Scope Differences

| Aspect | Admin Scope | User Scope |
|--------|-------------|------------|
| Target pages | `/admin/*` routes | User-facing domain routes |
| View Options save | Immediate PUT | Draft-based PUT |
| Form Fields save | Immediate PATCH | Draft-based |
| Table Columns structural save | Immediate PATCH | Draft-based |
| Admin lock visibility | N/A (is the source) | Shows admin locks as "Enforced" (non-interactive) |
| Action gate effect | Bypassed (admin always has access) | Enforced against membership role |

### Admin Lock Propagation

When an admin sets a lock in `/admin/admin-settings`:

1. The lock is stored in `[:admin :settings :view-options]`
2. On `/admin/user-settings`, the lock renders as an **"Enforced"** static label (non-interactive) — the user cannot override it
3. On live user-facing pages, the lock cascades via `overlay-admin-locks` in `subs/ui.cljs` (only when admin settings are loaded in app-db)

**Limitation:** Admin lock propagation to live user-facing pages only works when admin settings are loaded in app-db (i.e., during admin sessions). For regular users visiting domain pages directly, admin locks do not cascade unless the backend includes them in the `/api/v1/config` response.

## API Endpoints

| Action | Method | Endpoint |
|--------|--------|----------|
| Load admin view options | GET | `/admin/api/settings` |
| Save admin view options | PUT | `/admin/api/settings` |
| Patch admin entity setting | PATCH | `/admin/api/settings/entity` |
| Remove admin entity setting | DELETE | `/admin/api/settings/entity` |
| Load admin form fields | GET | `/admin/api/settings/form-fields` |
| Patch admin form fields | PATCH | `/admin/api/settings/form-fields/entity` |
| Load admin table columns | GET | `/admin/api/settings/table-columns` |
| Patch admin table columns | PATCH | `/admin/api/settings/table-columns/entity` |
| Load user UI config | GET | `/admin/api/settings/user-ui-config` |
| Save user UI config | PUT | `/admin/api/settings/user-ui-config` |

All endpoints are in `src/app/template/backend/routes/admin/settings.clj` with I/O logic in `settings_io.clj`.

## Key Element IDs

| Element | ID Pattern | Example |
|---------|-----------|---------|
| Admin sidebar toggle | `admin-sidebar-toggle` | — |
| Settings gear | `admin-settings-gear` | — |
| Reload button | `btn-admin-reload-everything` | — |
| Sidebar logout | `admin-sidebar-logout` | — |
| Table column row | `table-columns-row-{entity}-{col}` | `table-columns-row-articles-name` |
| Column label input | `col-label-{entity}-{col}` | `col-label-articles-name` |
| Column drag handle | `col-order-drag-{entity}-{col}` | `col-order-drag-articles-name` |
| Clear local prefs | `btn-clear-local-display-prefs-{entity}` | `btn-clear-local-display-prefs-articles` |

## Key Source Files

| File | Purpose |
|------|---------|
| `src/app/admin/frontend/pages/unified_settings/page.cljs` | Unified settings page component (view/edit modes, scope switching) |
| `src/app/admin/frontend/pages/unified_settings/editors.cljs` | Three editor tab components (View Options, Form Fields, Table Columns) |
| `src/app/template/frontend/settings/resolver.cljs` | Route-aware config resolution (`resolve-config-source`, `resolve-display-settings`, `resolve-list-config`, `resolve-column-label-override`) |
| `src/app/template/frontend/subs/ui.cljs` | Display settings subscriptions (`::entity-display-settings`, `::locked-display-settings`, `::visible-columns`, `overlay-admin-locks`) |
| `src/app/template/frontend/db/entity_specs.cljs` | Entity specs resolution with route-aware table-columns merging |
| `src/app/admin/frontend/events/settings.cljs` | Admin settings events facade (view-options, form-fields, table-columns sub-files) |
| `src/app/admin/frontend/events/user_settings.cljs` | User settings events facade (load-save, view-options, form-fields, table-columns sub-files) |
| `src/app/admin/frontend/events/unified_settings.cljs` | Unified page events (init, mode, scope, save, discard) |
| `src/app/admin/frontend/settings/definitions.cljs` | Setting keys, labels, domain groups, action gates catalog |
| `src/app/template/backend/routes/admin/settings.clj` | Backend API route handlers |
| `src/app/template/backend/routes/admin/settings_io.clj` | Backend I/O layer (DB-only reads/writes) |
| `src/app/template/backend/routes/admin/settings_bootstrap.clj` | Bootstrap: seeds DB from EDN defaults for fresh environments |

## Local Overrides

Users can store display preferences locally in the browser via `ui-entity-prefs`. These **override admin defaults** (but not admin locks) on a per-entity basis.

When testing settings, local overrides can mask admin-default changes. The "Clear local overrides" button (visible in edit mode when overrides exist) removes them for the selected entity.

## Known Gaps

These are documented findings from verification sessions:

1. **Form fields editor mismatch** — The form-fields checklist may not match the live form's field set on some entities (Articles). The editor uses model specs while forms use `form-entity-specs` resolution.
2. **Admin lock propagation to standalone user pages** — Admin locks only cascade when admin settings are loaded in app-db. Regular users on domain pages do not see admin-scope locks unless the backend embeds them in `/api/v1/config`.

For full test results, see:
- `ADMIN-SETTINGS-TEST-RESULTS.md`
- `ADMIN-ARTICLES-SETTINGS-VERIFICATION-RESULTS.md`
- `ADMIN-ARTICLES-SETTINGS-POST-OPTIMIZATION-RESULTS.md`
- `USER-SETTINGS-OPTIMIZATION-RESULTS.md`


