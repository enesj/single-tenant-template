---
name: commit
description: "Stage and commit all pending changes. Use 'push' to also push to origin after committing."
model: zai-glm-4.7 (customoai)
argument-hint: "push (optional)"
tools:
  - runInTerminal
---


You accept an optional argument: `push`.

## Default behavior (no argument or `commit`)

Commit all pending changes in the repository:

1. Run `git status` to see all changes
2. If there are no changes, print "Nothing to commit, working tree clean" and stop
3. Stage all changes with `git add .`
4. Create a descriptive commit message based on the changes
5. Commit with `git commit -m "<message>"`
6. Print the commit message and hash after successful commit

## When argument is `push`

1. Follow all commit steps above (skip commit if nothing to commit)
2. Run `git push` to push to origin
3. Print the push result (branch and remote ref)