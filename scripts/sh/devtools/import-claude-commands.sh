#!/usr/bin/env bash
set -euo pipefail

# Import custom Claude slash commands from a project directory into
# common Claude Desktop/CLI locations by creating/updating symlinks.
#
# Defaults:
#   SOURCE_DIR = .claude/commands (project root)
#   TARGETS    =
#     - $HOME/.claude/commands
#     - $HOME/Library/Application Support/Claude/commands (macOS)
#     - $HOME/.config/claude-code/commands (CLI config)
#     - $HOME/.codex/commands (Codex CLI - project/user-level)
#
# Usage:
#   scripts/sh/devtools/import-claude-commands.sh [SOURCE_DIR]
#
# Notes:
# - Creates parent directories if missing
# - If a non-symlink directory exists at target, it will be backed up and replaced with a symlink
# - If a different symlink exists, it will be updated to point to SOURCE_DIR

SOURCE_DIR=${1:-".claude/commands"}

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "❌ Source directory not found: $SOURCE_DIR" >&2
  exit 1
fi

abs_path() {
  # Resolve absolute path for SOURCE_DIR
  python3 - <<'PY'
import os,sys
p = sys.argv[1]
print(os.path.abspath(p))
PY
}

SRC_ABS=$(abs_path "$SOURCE_DIR")

declare -a TARGETS=()

# Always include ~/.claude/commands
TARGETS+=("$HOME/.claude/commands")

# macOS Application Support path (create only on Darwin)
if [[ "$(uname)" == "Darwin" ]]; then
  TARGETS+=("$HOME/Library/Application Support/Claude/commands")
fi

# CLI config path (if base dir exists or we create it)
TARGETS+=("$HOME/.config/claude-code/commands")

# Codex CLI project/user-level commands directory (if supported)
TARGETS+=("$HOME/.codex/commands")

link_target() {
  local target="$1"
  local parent
  parent=$(dirname "$target")

  mkdir -p "$parent"

  if [[ -L "$target" ]]; then
    # Existing symlink – update if points elsewhere
    local current
    current=$(readlink "$target") || true
    if [[ "$current" != "$SRC_ABS" ]]; then
      rm -f "$target"
      ln -s "$SRC_ABS" "$target"
      echo "🔁 Updated symlink: $target -> $SRC_ABS"
    else
      echo "✅ Symlink already set: $target -> $SRC_ABS"
    fi
  elif [[ -e "$target" ]]; then
    # Exists but not a symlink – back up then replace with symlink
    local backup="${target}.backup-$(date +%Y%m%d-%H%M%S)"
    mv "$target" "$backup"
    ln -s "$SRC_ABS" "$target"
    echo "🗂️  Backed up existing and linked: $target (backup: $backup)"
  else
    ln -s "$SRC_ABS" "$target"
    echo "🔗 Created symlink: $target -> $SRC_ABS"
  fi
}

echo "📥 Importing Claude slash commands from: $SRC_ABS"
for t in "${TARGETS[@]}"; do
  link_target "$t"
done

echo "🎉 Done. Restart Claude Desktop/CLI to pick up commands if needed."
