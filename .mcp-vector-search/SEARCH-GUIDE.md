# MCP Vector Search Guide for Agents

Semantic search across project documentation using **hybrid indexing**: chunked content for precise search + file paths for easy document identification.

## Hybrid Index Architecture

The index contains **two types of entries** for each document:

| Index Type | Content Returned | Use Case |
|------------|------------------|----------|
| **Chunks** | ~512 char text segments | Find specific facts, code examples, detailed content |
| **Files** | Full file path only | Identify which documents match a query |

Results are **mixed** and ranked by relevance—file paths and chunks appear together.

### Identifying Result Types

- **File path result**: `/Users/enes/Projects/single-tenant-template/docs/backend/http-api.md`
- **Chunk result**: Markdown content with headers, code, or prose

---

## Key Filters

| Filter | Purpose | Example |
|--------|---------|---------|
| `:section` | Find docs by area | `section: "backend"` |
| `:kind` | Find docs by type | `kind: "api-reference"` |
| `:skill` | Find specific skill | `skill: "app-db-inspect"` |

## Documentation Sections

| Section | Path | Use For | Kind(s) |
|---------|------|---------|---------|
| **architecture** | `docs/architecture/` | System design, routing, data flow | system-design |
| **backend** | `docs/backend/` | API routes, services, security, middleware | api-reference |
| **frontend** | `docs/frontend/` | Components, UI, state, re-frame | ui-reference |
| **shared** | `docs/shared/` | Cross-platform utilities, validation | utilities |
| **migrations** | `docs/migrations/` | Database schema, automigrate workflows | workflow |
| **libraries** | `docs/libs/` | Third-party tools, vendored libs | reference |
| **reference** | `docs/reference/` | API specs, database schema, glossary | api-reference |
| **operations** | `docs/operations/` | Dev commands, setup, workflows | runbook |
| **validation** | `docs/validation/` | Validation patterns and implementation | guide |
| **index** | `docs/*.md` | Overview, quick-access guides | guide |
| **skills** | `.claude/skills/` | Debugging (app-db, events), monitoring (logs) | debugging, monitoring |

---

## Search Examples & Patterns

### Quick Searches (No Filters)

```javascript
{ "query": "authentication bearer token login" }
{ "query": "migration models schema automigrate" }
{ "query": "list-view modal form button component" }
```

### By Section

```javascript
// Backend
{ "query": "user auth validation", "metadata": { "section": "backend" } }
{ "query": "routes endpoints", "metadata": { "section": "backend", "kind": "api-reference" } }

// Frontend
{ "query": "list view form input", "metadata": { "section": "frontend" } }

// Database
{ "query": "migration schema models", "metadata": { "section": "migrations" } }

// Architecture
{ "query": "routing middleware data flow", "metadata": { "section": "architecture" } }

// Utilities
{ "query": "pagination http validation", "metadata": { "section": "shared" } }
```

### By Kind

```javascript
{ "query": "routes endpoints", "metadata": { "kind": "api-reference" } }
{ "query": "how to implement", "metadata": { "kind": "guide" } }
{ "query": "button form input", "metadata": { "kind": "ui-reference" } }
```

### By Skill

```javascript
{ "query": "inspect re-frame state", "metadata": { "skill": "app-db-inspect" } }
{ "query": "debugging tracing", "metadata": { "section": "skills", "kind": "debugging" } }
{ "query": "logs monitoring", "metadata": { "skill": "system-logs" } }
```

### Combined Filters

```javascript
// Frontend UI components
{ "query": "modal list form", "metadata": { "section": "frontend", "kind": "ui-reference" } }

// Backend API specs
{ "query": "POST routes users", "metadata": { "section": "backend", "kind": "api-reference" } }

// Limit results for precision
{ "query": "authentication", "metadata": { "section": "backend" }, "limit": 5 }
```

### Filter by Specific File (doc-id)

```javascript
// Search within a specific document
{ "query": "routes middleware", "metadata": { "doc-id": "/Users/enes/Projects/single-tenant-template/docs/backend/http-api.md" } }

// Combine doc-id with other filters
{ "query": "validation", "metadata": { "doc-id": "/Users/enes/Projects/single-tenant-template/docs/validation/overview.md" }, "limit": 5 }
```

---

## Understanding Search Results

### What's Returned (Hybrid Results)

Each search returns a **mix of chunks and file paths** ranked by relevance:

```javascript
// Example results for "authentication login user"
[
  {"content": "This implementation provides a complete, secure user registration...", "score": 0.77},
  {"content": "/Users/enes/.../docs/implementation-plan-user-email-password-auth.md", "score": 0.76},
  {"content": "## Current State Analysis\n### ✅ Existing Infrastructure...", "score": 0.76}
]
```

- **Chunk results**: Detailed text content (~512 chars)
- **File path results**: Just the path string—use this to identify source documents

### Recommended Search Workflow

1. **Run a broad search** to see which files and chunks match
2. **Note file paths** in the results to identify relevant documents
3. **Read the full file** if needed using the file path
4. **Refine with filters** (`section`, `kind`) to narrow scope

### What's Indexed but Not Directly Filterable

The server stores additional metadata per segment:

| Field | Description |
|-------|-------------|
| `doc-id` | Source file path (same as `file-id`) |
| `file-id` | Source file path |
| `segment-id` | Unique chunk identifier within a file |
| `chunk-index` | Position of chunk in document (0-based) |
| `chunk-count` | Total chunks in the source document |
| `chunk-offset` | Character offset in original document |

### Section-to-Path Mapping

Use the `section` filter to narrow results to a known folder:

| Section | Maps to Folder |
|---------|----------------|
| `backend` | `docs/backend/` |
| `frontend` | `docs/frontend/` |
| `architecture` | `docs/architecture/` |
| `shared` | `docs/shared/` |
| `migrations` | `docs/migrations/` |
| `libraries` | `docs/libs/` |
| `reference` | `docs/reference/` |
| `operations` | `docs/operations/` |
| `validation` | `docs/validation/` |
| `index` | `docs/*.md` |
| `skills` | `.claude/skills/` |
