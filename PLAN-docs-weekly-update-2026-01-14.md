# Plan: Weekly Docs Update (2026-01-07 → 2026-01-14)

## Why
This week included several user-visible feature additions (Expenses workflows, settings persistence, role/capability gating) and a set of internal refactors (shared HTTP/adapters, domain registry cleanup) that change contributor-facing APIs and code pointers.

This plan ensures the `docs/` folder reflects the current codebase state and that “entry point” docs (especially `docs/index.md` and `docs/backend/http-api.md`) are accurate.

## Scope
- Update markdown docs under `docs/**` to reflect changes made during the last ~7 days.
- Focus on **user-visible behavior**, **HTTP endpoints**, and **contributor-facing code pointers**.
- Avoid unrelated doc rewrites; keep diffs reviewable.

## Inputs used to compile change inventory
- `git log --since='7 days ago' --name-only` (commit messages + changed files)
- Current working tree status (uncommitted changes may include additional refactors; we’ll document only what changes behavior or public APIs).

## Change inventory (high level)

### Expenses domain (user + admin)
- Receipt review/approval workflow improvements (UI + service behavior; preserve original guesses).
- POS integration + auto-matching of expense items to articles via supplier aliases.
- Unmapped items management + batch alias creation.
- Quantity support: 3-decimal precision for line-item quantities.
- Supplier enhancements: archiving + detail view support.
- Timestamp formatting improvements on expenses list tables.
- Role/capability gating for expenses pages/endpoints.
- User expense settings persistence (DB + BE/FE).

### Backend / shared infra
- Foreign key violation handling on deletes (improved error semantics).
- Consolidated shared HTTP helpers + JSON response builders.
- Consolidated shared adapters/utilities.
- Domain registry cleanup: remove legacy 2-arity support.

### Tooling / docs already touched this week
- Admin settings docs updated (verify completeness).
- Migrations docs updated (verify any new workflow notes are correct).
- FE testing and Babashka tooling docs updated (verify accuracy).

## Docs update mapping (what to change where)

### 1) `docs/index.md`
Add new “New (YYYY-MM-DD)” entries for the week’s highlights, with pointers to deeper docs:
- Receipt review/approval workflow + POS auto-matching + unmapped items tooling
- Supplier archiving
- User expense settings persistence
- Role/capability gating

### 2) `docs/expenses/index.md`
Update the domain guide to include:
- Receipt review vs approve semantics (what users can edit, what is preserved)
- POS integration flows:
  - auto-matching via supplier aliases
  - unmapped items UI + batch alias creation
- 3-decimal quantity behavior (where it matters: forms, totals, OCR, etc.)
- Supplier archiving behavior (and how it affects lists/search/detail)
- User expense settings persistence (what settings exist, where stored, and how edited)
- Role/capability gating summary (what’s gated, what roles can do)

### 3) `docs/backend/http-api.md`
Audit endpoints and update for this week:
- Any new user settings endpoints (likely under `/api/v1/expenses/...`)
- Any new receipt review endpoints (distinct from approve)
- Any new supplier archive/unarchive endpoints or fields
- Any new bulk alias / unmapped items endpoints
- Note improved deletion failure semantics (FK violation) where relevant

### 4) `docs/frontend/list-view-controls-configuration.md`
Verify code pointers still match current locations after refactors:
- Resolver path(s)
- Entity prefs storage paths (ensure the doc matches current writes)

### 5) Optional: `docs/ai-index.yaml`
If key namespaces were renamed/moved this week (or new ones are now primary entry points), add/update pointers.

## Verification checklist
- Search docs for stale pointers to moved namespaces/files.
- Ensure `docs/backend/http-api.md` endpoint lists match actual route files.
- Ensure “New (date)” highlights in `docs/index.md` are consistent and link to updated docs.

## Implementation order (small diffs, easy review)
1. Update `docs/index.md` highlights.
2. Update `docs/expenses/index.md` (feature/workflow sections).
3. Update `docs/backend/http-api.md` (endpoint audit).
4. Fix any stale pointers in `docs/frontend/list-view-controls-configuration.md`.
5. Optional: update `docs/ai-index.yaml`.

## Acceptance criteria
- Docs accurately describe this week’s features and endpoint changes.
- No obvious stale code pointers (file paths/namespaces) in the touched docs.
- Changes are limited to docs (no behavior changes as part of this task).
