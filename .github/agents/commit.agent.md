---
name: commit
description: Stage and commit all pending changes with a descriptive commit message
tools:
  - runInTerminal
---

Commit all pending changes in the repository.

Steps:
1. Run `git status` to see all changes
2. Stage all changes with `git add .`
3. Create a descriptive commit message based on the changes
4. Commit with `git commit -m "<message>"`
5. Print commit message and hash after successful commit.
