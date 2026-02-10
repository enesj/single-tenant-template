# scripts/bb/articles

Small, deterministic Babashka scripts for article/alias/taxonomy workflows.

These are designed to replace ad-hoc SQL snippets (and any Postgres MCP usage) in the `create-articles` skill.

They connect to Postgres by shelling out to `psql` (bb-compatible) with `-X` and `ON_ERROR_STOP=1` for more deterministic behavior.

Run them directly:

- `bb scripts/bb/articles/list_unmapped_aliases.clj dev`
- `bb scripts/bb/articles/create_articles.clj dev --canonical-name "..."`

All scripts read DB connection settings from `config/base.edn` (profile `dev`/`test`).
