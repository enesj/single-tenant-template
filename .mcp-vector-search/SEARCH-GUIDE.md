# Documentation Search Guide (Morph MCP / Warp Grep)

This repo standardizes on **Morph MCP (Warp Grep)** for searching documentation and skill guides.

The primary tool is `mcp__morph-mcp__warpgrep_codebase_search` (natural-language search over the repo).

## Workflow

1. Search broadly with a short natural-language query.
2. Open the returned files and read the relevant sections.
3. Refine the query with folder hints (e.g. “in docs/migrations”, “in .claude/skills”) if needed.

## Examples

- “where is the migrations overview and complete guide”
- “admin settings UI configuration docs”
- “expenses domain API endpoints /admin/api/expenses”
- “how does admin auth work in single-tenant template”
- “system-logs skill restart system”

## Where to Look

- Docs hub: `docs/index.md`
- Documentation: `docs/**` (backend, frontend, migrations, testing, operations, etc.)
- Skills: `.claude/skills/**` (e.g., app-db-inspect, reframe-events-analysis, system-logs)

## Legacy Note

The `.mcp-vector-search/` folder name is historical. Use Morph MCP (Warp Grep) for documentation search going forward.

