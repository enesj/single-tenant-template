<!-- ai: {:tags [:operations :dev :single-tenant] :kind :runbook} -->

# Operations & Setup (Single-Tenant)

Quick guide for configuring and running the template locally. Defaults match `config/base.edn` (added 2025‑12‑01).

## Initial Setup

1) **Secrets file** – create `config/.secrets.edn` (or `~/.secrets.edn`) with keys referenced in `config/base.edn` (edit this yourself; agents should only provide precise instructions with placeholder values):
   - `:db {:dev-password "...", :test-password "..."}`  
   - Optional: `:oauth {:google {...} :github {...}}`, `:stripe {...}`, `:gmail {...}`, `:postmark {...}`
2) **PostgreSQL** – ensure ports **55432 (dev)** and **55433 (test)** are available. Start your DB (e.g., `docker compose up db`) before running the app.

   `docker-compose.yml` uses env vars for container passwords:
   - `POSTGRES_PASSWORD` (dev)
   - `POSTGRES_TEST_PASSWORD` (test)

   You can set these in your shell or via a local `.env` file (gitignored). See `.env.example`.
3) **Install deps** – `bb tasks` (Babashka) and `npm install` if frontend work is needed.

## Runtime Defaults (from `config/base.edn`)

- Web server: **localhost:8085** (dev) / **8086** (test)  
- DB names: `multi_tenant_pos` (dev) / `multi_tenant_pos_test` (test)  
- HikariCP: max pool 20, idle 5, leak detection 60s  
- Metrics: Prometheus on **9190** (enabled)  
- Email: `:type :gmail-smtp` by default; Postmark keys are optional  
- OAuth/Stripe: optional; only load when secrets are present

## Common Commands

- Start full stack (hot reload): `bb run-app`  → backend + Shadow CLJS `:app` + nREPL 7888 on port 8085.
- Backend tests: `bb be-test`
- Frontend tests (node): `npm run test:cljs`
- Database helpers: `bb backup-db`, `bb restore-db` (see `scripts/bb/database/README.md`)
- Reference data (optional, recommended for Expenses ZIP/city lookups):
  - `bb seed-geo-reference dev` (countries + Bosnia & Herzegovina cities)
  - or individually: `bb seed-countries dev`, `bb seed-bh-cities dev`
- Receipt OCR worker (optional): `bb receipt-ocr-worker dev` (one-shot) or `bb receipt-ocr-worker dev --loop`
  - Select workflow with `RECEIPT_OCR_WORKFLOW=mistral|llamaparse`
  - `mistral` requires `MISTRAL_API_KEY`
  - `llamaparse` requires `LLAMA_CLOUD_API_KEY`
- Frontend config checks (fast):
  - `bb validate-frontend-config`
  - `bb config-audit --strict`
  - guard against concrete-domain coupling: `bb guard-no-concrete-domain`
  - CI: `npm run test:config-audit` (runs the guard + strict config audit)

### 🚨 Testing - Always Save Output First

```bash
# Save once, analyze many times - NEVER re-run tests!
mkdir -p tmp
bb be-test 2>&1 | tee tmp/ops-be-test.txt
npm run test:cljs 2>&1 | tee tmp/ops-fe-test.txt
# Then: grep "FAIL" tmp/ops-*-test.txt
```

## Config Tips

- Override profiles via `:dev`/`:test` in `config/base.edn`; never commit real secrets.
- Admin settings (admin scope) are merged from:
  - `src/app/admin/frontend/config/*.edn` (system/admin-owned)
  - `src/app/domain/**/admin/config/*.edn` (domain-owned additions)
   and edited via `/admin/admin-settings`.
- Domain-owned user UI configuration (currently Expenses) lives in `src/app/domain/**/config/*.edn` and is editable via `/admin/user-settings`.
- Keep `config/base.edn` in sync with docs; update this file if ports/envs change.
