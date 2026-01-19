---
description: "Session bootstrap: Morph discovery → bb audit-bundle → Lattice queries (Option A, agent-curated queries)"
tags: ["ai", "workflow", "morph", "lattice", "audit-bundle", "session", "evidence"]
---

# session-context-bundle

Use this at the **beginning of a session** to create a single “context bundle” file tailored to the user’s prompt, then query that bundle with Lattice.

## Default workflow (Option A)
1) Discover likely files/keywords with Morph search.
2) Generate a bundle with `bb audit-bundle` (writes to `target/audit-bundles/`).
3) Load the bundle into Lattice and query it for the rest of the session.

## Decision guide: when to use vs when to skip

Use **`session-context-bundle`** when **2+** of the following are true:
- The prompt is **multi-file by nature** (trace a flow, refactor, debug across layers, “where is this wired?”).
- The task is **cross-cutting** (docs + code + config, FE + BE, routes + events + services).
- You expect **iterative searching** (many “find the line that…” loops).
- You want stable **`path:line` evidence** for reporting or PR-ready notes.

Skip it when:
- The user points to a **single file/function** and asks for a small change.
- The task is **purely conceptual** (no repo evidence needed).
- You don’t have good candidate `--query` strings yet (do Morph discovery first).

**Rule of thumb:** if you expect more than ~5 repo searches during the session, bootstrap a bundle.

## What good looks like
- Bundle is narrow (few `--query`, few `--glob`).
- Evidence is always `path:line`.
- Lattice queries do the heavy lifting after bundling.
