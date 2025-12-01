#!/bin/bash

# Validate commit message format according to conventional commits
# This script is used by the commit-msg git hook

commit_regex='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\(.+\))?: .{1,50}'

if ! grep -qE "$commit_regex" "$1"; then
    echo "Invalid commit message format!"
    echo ""
    echo "The commit message must follow the conventional commits format:"
    echo "  <type>(<scope>): <subject>"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert"
    echo "Scope: optional noun describing a section of the codebase"
    echo "Subject: imperative, present tense, no period at the end, max 50 chars"
    echo ""
    echo "Examples:"
    echo "  feat(auth): add login functionality"
    echo "  fix: resolve database connection issue"
    echo "  docs: update README with setup instructions"
    echo ""
    exit 1
fi
