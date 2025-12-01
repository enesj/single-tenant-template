#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# install-clj-kondo-imports.sh
# ---------------------------------------------------------------------------
# Downloads and installs the community clj-kondo import configurations that
# provide linting information for popular third-party libraries.
#
# The script fetches the latest version of the clj-kondo/config repository
# (https://github.com/clj-kondo/config), then copies all library import
# directories into your project's local `.clj-kondo/imports` folder, *only*
# when they are not present yet.
#
# Usage:
#   bash scripts/install-clj-kondo-imports.sh        # run from project root
#
# The script is idempotent – running it multiple times will simply skip
# libraries that are already installed.
# ---------------------------------------------------------------------------

set -euo pipefail

# Resolve project root (git top-level if available, otherwise current dir)
PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
IMPORTS_DIR="$PROJECT_ROOT/.clj-kondo/imports"
TMP_DIR=$(mktemp -d)

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

mkdir -p "$IMPORTS_DIR"

echo "📥 Downloading clj-kondo community configs …"
curl -sSL "https://github.com/clj-kondo/config/archive/refs/heads/master.tar.gz" \
  | tar -xz -C "$TMP_DIR"

CONFIG_SRC=$(find "$TMP_DIR" -maxdepth 1 -type d -name "config-*" | head -n 1)
if [[ -z "$CONFIG_SRC" ]]; then
  echo "❌ Could not locate extracted config directory." >&2
  exit 1
fi

echo "🚚 Installing configs into $IMPORTS_DIR …"
# Iterate over top-level directories inside config repo (each represents a group)
shopt -s dotglob
for DIR in "$CONFIG_SRC"/*; do
  [[ -d "$DIR" ]] || continue
  BASENAME=$(basename "$DIR")
  DEST="$IMPORTS_DIR/$BASENAME"

  if [[ -e "$DEST" ]]; then
    echo "   ➡️  $BASENAME already present – skipping"
  else
    cp -R "$DIR" "$DEST"
    echo "   ✅ Installed $BASENAME"
  fi
done
shopt -u dotglob

echo "🎉 All available clj-kondo import configs are now installed."
