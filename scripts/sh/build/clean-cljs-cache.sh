#!/usr/bin/env bash
# scripts/clean-cljs-cache.sh
# -------------------------------------------
# Removes all Shadow-CLJS build artefacts and compiled JS output.
# Run this before restarting the app when you need to guarantee
# a completely fresh ClojureScript compilation (e.g. to pick up
# schema changes in resources/db/models.edn).
set -euo pipefail

CLEAN_TARGETS=(
  ".shadow-cljs"               # Shadow compilation & macro cache
  "resources/public/assets/js" # Generated frontend bundle
  "target"                     # Misc build output (tests etc.)
)

for dir in "${CLEAN_TARGETS[@]}"; do
  if [ -e "$dir" ]; then
    echo "🧹 Removing $dir ..."
    rm -rf "$dir"
  fi
done

echo "✅ Shadow-CLJS caches cleared. Re-start the app to force a full recompile."
