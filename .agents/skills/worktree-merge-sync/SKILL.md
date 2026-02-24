---
name: worktree-merge-sync
description: Safely merge one worktree branch into another (e.g., wk1 -> allium), then optionally re-sync the source worktree for future development.
user-invocable: true
---

# Worktree Merge Sync Skill

Use this skill when you need a repeatable, low-risk process for:

- merging a source worktree branch into a target branch,
- resolving conflicts predictably,
- and keeping the source worktree usable afterwards.

## Instruction precedence

1. Follow `AGENTS.md` for policy/workflow hard rules.
2. Follow `.github/copilot-instructions.md` for implementation conventions.
3. If there is tension, follow the stricter repo rule.

## Safety principles

- **Approval gate required** before destructive or branch-moving actions.
- Prefer **small, explicit steps** with status checks between steps.
- Do **not push** unless explicitly requested.
- Expect that `worktrees/<name>` may appear modified as a gitlink pointer; treat it deliberately.

## Standard workflow (approval-gated)

### 1) Discover current git topology

Collect and report before acting:

- `git worktree list`
- `git branch -vv`
- `git status -sb`
- divergence between source and target (`rev-list --left-right --count target...source`)

Then ask for confirmation of:

- source branch (example: `wk1`)
- target branch (example: `allium`)
- merge policy (`--no-ff` recommended)

### 2) Pre-merge clean check on target

- Ensure target worktree is the one being operated on.
- If `worktrees/<source>` appears modified, explain why (gitlink drift) and ask whether to:
  - clean/reset drift first (**recommended**), or
  - proceed as-is.

### 3) Execute merge

- Run merge on target branch: source -> target (recommended `--no-ff`).
- If conflicts occur:
  - enumerate conflicted files,
  - inspect both sides,
  - resolve conservatively according to approved preference,
  - stage resolutions,
  - complete merge commit.

### 4) Post-merge verification

Report:

- merge commit hash,
- short log (`log -n 3 --oneline --decorate`),
- target branch status (`status -sb`).

### 5) Optional but recommended: sync source worktree to target

If source worktree will be reused soon, align it to merged target:

- move source branch pointer to target tip,
- check out source branch in source worktree (avoid detached HEAD),
- verify both refs point to same commit.

### 6) Explain expected gitlink behavior

If `M worktrees/<source>` appears in the superproject after syncing, explain:

- this is expected gitlink pointer drift,
- it is metadata about nested checkout position,
- not necessarily a code conflict.

## Conflict resolution rubric

When asked for "safe default":

1. Keep target branch behavior for operational scripts/config unless user says otherwise.
2. Keep deletions from target if source reintroduces stale files.
3. Preserve source feature code additions that are the merge objective.
4. Re-check unmerged file list is empty before commit.

## Completion report contract

Always return:

1. What was merged (`source -> target`).
2. Whether conflicts occurred and how they were resolved.
3. Final commit hashes for target and source.
4. Current status of source worktree branch attachment (attached vs detached).
5. Whether push was performed (should be "no" unless explicitly requested).

## Reusable checklist

- [ ] Inspect worktrees, branches, and status
- [ ] Confirm source/target and get approval
- [ ] Merge source into target
- [ ] Resolve conflicts (if any)
- [ ] Verify merge commit and status
- [ ] Sync source worktree to target (if requested)
- [ ] Final report with hashes + worktree state
