---
name: debugging
description: "Short, evidence-based debugging playbook for Clojure/ClojureScript"
tags: ["debugging", "troubleshooting", "testing", "clojure", "clojurescript", "re-frame", "backend", "frontend"]
---

# debugging

## Goal
Find the root cause quickly, using evidence (not guesses).

## Default flow
1. Reproduce with the smallest steps.
2. Decide scope: frontend / backend / both.
3. Gather evidence (logs, eval, browser network).
4. Write one hypothesis and run one confirming/disproving test.
5. Fix, confirm end-to-end, then add/adjust a focused test.

## Tool choices (use specialized skills)
- Frontend state/auth/UI → `app-db-inspect`
- Frontend event/perf flow → `reframe-events-analysis`
- Build/runtime/logs (FE or BE) → `system-logs`

## Evaluation (preferred)

### Backend (Clojure)
Use the `mcp__clojure-mcp__clojure_eval` MCP tool to evaluate Clojure code.

### Frontend (ClojureScript)
Use the `mcp__clojure-mcp__clojurescript_eval` MCP tool to evaluate ClojureScript code.

## Logs
```bash
./scripts/sh/monitoring/read_output.sh -f
./scripts/sh/monitoring/read_output.sh | rg -i "error|exception"
```

## Tests (save output once)
```bash
# Frontend
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt

# Backend
bb be-test 2>&1 | tee /tmp/be-test.txt
```

Prefer focused runs over full suites:
```bash
npm run test:cljs -- --testNamePattern="..."
bb be-test --focus <namespace>
```

## Interactive browser checks (chrome-mcp)
Use `chrome-mcp` tools to validate UI flows in a real browser against the running app:
- Admin UI defaults to `http://localhost:8085`
- Prefer selecting elements by `:id` for stability (see `INTERACTIVE-COMPONENTS-ID-AUDIT.md`)

## What to collect before changing code
- Exact error/stack trace (logs) or failing test output
- Smallest repro steps (URL, clicks, payload)
- Current state snapshots (e.g. FE app-db, BE function outputs)
- One-sentence hypothesis
