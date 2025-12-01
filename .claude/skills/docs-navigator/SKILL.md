---
description: Navigate and search project documentation efficiently using AI-optimized metadata, namespace filtering, and RAG search. Use when user asks about documentation, needs to find specific information, or wants to understand project structure and patterns.
allowed-tools:
  - clojure-mcp:bash
  - clojure-mcp:grep
  - clojure-mcp:read_file
  - clojure-mcp:glob_files
---

# Docs Navigator Skill

Intelligently navigates and searches the project documentation system using AI-optimized metadata, namespace filtering, and RAG search capabilities.

## When to Use This Skill

Use this skill when:
- User asks about documentation, architecture, or how to find information
- Searching for specific topics like authentication, routing, frontend/backend patterns
- Need to understand project structure, domains, or implementation patterns
- Looking for API references, guides, or runbooks
- User mentions "docs", "documentation", "architecture", "how to find", "where is"
- Investigating specific features, domains, or implementation details

## Tool Usage Guidelines

### Primary Tool Patterns

**clojure-mcp:read_file** - Use for:
```clojure
;; Read specific documentation files
(clojure-mcp/read_file {:file_path "docs/architecture/overview.md"})
(clojure-mcp/read_file {:file_path "docs/backend/http-api.md"})
```

**clojure-mcp:glob_files** - Use for:
```clojure
;; Find documentation by patterns
(clojure-mcp/glob_files {:pattern "docs/**/*.md"})
(clojure-mcp/glob_files {:pattern "docs/backend/*.md"})
(clojure-mcp/glob_files {:pattern "docs/frontend/*.md"})
```

**clojure-mcp:grep** - Use for:
```clojure
;; Search within documentation content
(clojure-mcp/grep {:pattern "authentication" :path "docs"})
(clojure-mcp/grep {:pattern "<!-- ai:.*:backend" :output_mode "files_with_matches"})
(clojure-mcp/grep {:pattern "admin.auth|login|service" :path "docs/backend" :output_mode "content"})
```

**clojure-mcp:bash** - Use for:
```clojure
;; Complex operations requiring shell commands
(clojure-mcp/bash {:command "find docs/ -name '*.md' | head -10"})
(clojure-mcp/bash {:command "bb cli-tools/rag.clj query -q \"pagination\" -k 5"})
```

## Core Search Strategies

### 1. RAG-Powered Search (Primary Method)

```bash
# Basic RAG query
bb cli-tools/rag.clj query -q "authentication" -k 5

# Domain-specific RAG queries
bb cli-tools/rag.clj query -q "OAuth2 flow" -t backend -k 3
bb cli-tools/rag.clj query -q "component patterns" -t frontend -k 5

# High-quality results with filtering
bb cli-tools/rag.clj query -q "tenant isolation" -t shared -k 3 -m 0.05
```

**RAG Troubleshooting:**
- **Setup**: Ensure `cli-tools/rag.clj` exists and is executable
- **Index issues**: Run `bb cli-tools/rag.clj rebuild` if queries return empty
- **Performance**: Use `-k 3` for faster results, `-m 0.1` to filter noise

### 2. Metadata-Driven Search

```bash
# Find docs by AI metadata tags
rg -n "<!-- ai:.*:backend" docs/
rg -n "<!-- ai:.*:frontend" docs/
rg -n "<!-- ai:.*:shared" docs/
rg -n "<!-- ai:.*:migrations" docs/

# Find specific namespace documentation
rg -n "app\.backend\.routes" docs/
rg -n "app\.frontend\.components" docs/
rg -n "app\.shared\.utilities" docs/
```

### 3. Pattern-Based Search

```bash
# Find implementation patterns
rg -n "service|handler|admin" docs/backend/
rg -n "component|authentication|admin" docs/frontend/

# Find configuration patterns
rg -n "routes?|handler|middleware" docs/backend/
rg -n "pagination|filtering|sorting" docs/
```

## Documentation Entry Points

### Stable Reference Documents (Check First)

| Path | Purpose | When to Use |
|------|---------|-------------|
| `docs/index.md` | Top-level information architecture | Understanding overall doc structure |
| `docs/architecture/overview.md` | System and domain architecture | High-level system understanding |
| `docs/backend/http-api.md` | Backend routing and HTTP surfaces | API endpoint and route questions |
| `docs/backend/services.md` | Service protocols and composition | Service architecture questions |
| `docs/frontend/app-shell.md` | Frontend entry points and navigation | Frontend structure and navigation |
| `docs/migrations/migration-overview.md` | Database models and workflow | Database schema questions |
| `docs/operations/README.md` | BB tasks and runbooks | Development workflow questions |
| `docs/reference/api-reference.md` | Stable API endpoint list | API reference needs |

### Domain-Specific Documentation

**Backend Documentation:**
```bash
# Search backend patterns
rg -n "<!-- ai:.*:backend" docs/backend/
rg -n "service|protocol|handler" docs/backend/

# Key backend documents
docs/backend/financial-domain.md
docs/backend/hosting-domain.md
docs/backend/integration-domain.md
docs/backend/security-middleware.md
docs/backend/services.md
```

**Frontend Documentation:**
```bash
# Search frontend patterns
rg -n "<!-- ai:.*:frontend" docs/frontend/
rg -n "component|uix|re-frame" docs/frontend/

# Key frontend documents
docs/frontend/feature-guides/admin.md
docs/frontend/feature-guides/billing.md
docs/frontend/feature-guides/hosting.md
docs/frontend/feature-guides/integrations.md
```

## User Intent-Based Search Patterns

### 🔍 **"How do I..." Questions (Implementation Guides)**

```clojure
;; Start with feature guides
(clojure-mcp/glob_files {:pattern "docs/frontend/feature-guides/*.md"})

;; Then search domain-specific docs
(clojure-mcp/grep {:pattern "authentication|login|auth" :path "docs/backend/"})
(clojure-mcp/grep {:pattern "component|uix|frontend" :path "docs/frontend/"})
```

### 🏗️ **"Architecture" Questions (System Design)**

```clojure
;; High-level architecture first
(clojure-mcp/read_file {:file_path "docs/architecture/overview.md"})

;; Then specific domain architecture
(clojure-mcp/glob_files {:pattern "docs/architecture/*.md"})
```

### 🐛 **"Debug/Issue" Questions (Troubleshooting)**

```clojure
;; Search validation and debugging docs
(clojure-mcp/grep {:pattern "debug|troubleshoot|error" :path "docs/"})
(clojure-mcp/grep {:pattern "validation|problem|issue" :path "docs/validation/"})

;; Search for specific error patterns
(clojure-mcp/grep {:pattern "error|troubleshoot|validation" :path "docs/"})
```

### 🔧 **"API/Reference" Questions (Technical Details)**

```clojure
;; API references first
(clojure-mcp/read_file {:file_path "docs/reference/api-reference.md"})

;; Then specific API docs
(clojure-mcp/grep {:pattern "endpoint|route|handler" :path "docs/backend/http-api.md"})
```

## Advanced Search Techniques

### 1. Multi-Tool Integration

```clojure
;; Step 1: Find relevant files
(def files (clojure-mcp/glob_files {:pattern "docs/backend/*.md"}))

;; Step 2: Search within found files
(clojure-mcp/grep {:pattern "admin.auth|login|service" :path "docs/backend/" :output_mode "content"})

;; Step 3: Read most relevant files
(clojure-mcp/read_file {:file_path "docs/backend/security-middleware.md"})
```

### 2. Contextual Search with Results

```clojure
;; Find related concepts with context
(clojure-mcp/grep {:pattern "pagination" :path "docs/" :output_mode "content" :C 3})

;; Search for authentication flows
(clojure-mcp/grep {:pattern "authentication.*flow" :path "docs/" :output_mode "content" :A 2 :B 2})
```

### 3. Multi-Pattern Search

```clojure
;; Search for multiple related concepts
(clojure-mcp/grep {:pattern "(migration|schema|database)" :path "docs/migrations/"})
(clojure-mcp/grep {:pattern "(component|uix|re-frame)" :path "docs/frontend/"})
```

## Documentation Quality Assessment

### High-Quality Sources (Priority)
1. Files with `<!-- ai: {...} -->` metadata blocks
2. Documents in `/docs/reference/` (stable APIs)
3. Documents in `/docs/architecture/` (system design)
4. Files with recent modification dates

### Supplemental Sources
1. `/docs/frontend/feature-guides/` (implementation patterns)
2. `/docs/backend/` (domain-specific patterns)
3. `/docs/shared/` (cross-platform utilities)

## Structured Output Format

### Search Results Format

```clojure
{:search-term "authentication"
 :results [
   {:doc-path "docs/backend/security-middleware.md"
    :section "OAuth2 Implementation"
    :relevance :high
    :excerpt "OAuth2 flow with PKCE..."}
   {:doc-path "docs/authentication/architecture-overview.md"
    :section "Session Management"
    :relevance :medium
    :excerpt "Token-based authentication..."}]
 :recommendations [
   "Also check docs/frontend/http-standards.md for client-side auth"
   "Review docs/shared/auth-utilities.md for common patterns"]}
```

### Navigation Guidance Structure
- **Primary sources**: Documents that directly answer the question
- **Secondary sources**: Related concepts and implementation details
- **Next steps**: Recommended reading order and follow-up documentation

## Integration with Other Skills

This skill works well with:
- **app-db-inspect**: For frontend state documentation
- **system-logs**: For operational documentation
- **reframe-events-analysis**: For frontend behavior docs
- **codex**: For complex architectural questions (escalate when stuck)

## Common Workflows

### Workflow 1: Find Implementation Pattern
1. Check feature guides: `(clojure-mcp/glob_files {:pattern "docs/frontend/feature-guides/*.md"})`
2. Search domain docs: `(clojure-mcp/grep {:pattern "pattern|example|implementation" :path "docs/"})`
3. Check shared utilities: `(clojure-mcp/glob_files {:pattern "docs/shared/*.md"})`
4. Verify with references: `(clojure-mcp/read_file {:file_path "docs/reference/api-reference.md"})`

### Workflow 2: Understand System Architecture
1. Start with overview: `(clojure-mcp/read_file {:file_path "docs/architecture/overview.md"})`
2. Check specific domains: `(clojure-mcp/glob_files {:pattern "docs/architecture/*.md"})`
3. Review services: `(clojure-mcp/read_file {:file_path "docs/backend/services.md"})`
4. Cross-reference with frontend: `(clojure-mcp/read_file {:file_path "docs/frontend/app-shell.md"})`

### Workflow 3: Troubleshoot Issue
1. Search validation docs: `(clojure-mcp/grep {:pattern "error|troubleshoot" :path "docs/validation/"})`
2. Check migration patterns: `(clojure-mcp/grep {:pattern "rollback|issue" :path "docs/migrations/"})`
3. Review debugging guides: `(clojure-mcp/grep {:pattern "debug" :path "docs/"})`

## Troubleshooting

### Documentation Not Found
1. Verify file paths: Use `(clojure-mcp/bash {:command "ls docs/"})` to check actual structure
2. Check for moved content: Look in `docs/archive/` for older docs
3. Search broader: Use more general search terms

### Search Returns Too Many Results
1. Add specific terms: Include technology or domain keywords
2. Use filters: Restrict to specific directories or file types
3. Leverage metadata: Filter by AI tags and namespaces

### RAG System Issues
1. **First-time setup**: Run `bb cli-tools/rag.clj index` to initialize the documentation index
2. **Index missing**: Run `bb cli-tools/rag.clj rebuild`
3. **Empty results**: Check if RAG is properly configured with `bb cli-tools/rag.clj status`
4. **Performance issues**: Reduce result count with `-k 3` or increase quality threshold with `-m 0.1`

### Tool Limitations and Fallbacks

**When tools fail:**
1. Escalate to **codex** skill for complex reasoning
2. Use **app-db-inspect** for frontend state questions
3. Use **system-logs** for operational issues
4. Fall back to direct file exploration with `glob_files` and `read_file`

**Escalation criteria:**
- Multiple failed search attempts (>2)
- Complex architectural questions requiring synthesis
- Cross-domain integration challenges
- Performance issues with RAG system

## Best Practices

### Search Efficiency
1. **Use specific queries** - "OAuth2 implementation" vs "auth"
2. **Leverage metadata** - Filter by tags and namespaces
3. **Start broad, then narrow** - Overview → Domain → Implementation
4. **Check entry points first** - Use stable reference documents

### Quality Assurance
1. **Verify file existence** before recommending
2. **Check modification dates** for current information
3. **Cross-reference** between related documents
4. **Provide alternatives** when primary sources are limited

### Performance Considerations
1. **Limit result scope** for large documentation sets
2. **Use caching** for frequently accessed documents
3. **Prefer RAG** for complex queries over multiple tool calls
4. **Batch operations** when searching multiple patterns