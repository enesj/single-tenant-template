#!/usr/bin/env bash
# Build a production uberjar.
#
# Usage:
#   ./scripts/sh/build/build-production.sh [version]
#
# Examples:
#   ./scripts/sh/build/build-production.sh          # version from APP_VERSION env or 0.1.0-SNAPSHOT
#   APP_VERSION=1.2.3 ./scripts/sh/build/build-production.sh
#   ./scripts/sh/build/build-production.sh 1.2.3
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

VERSION="${1:-${APP_VERSION:-}}"

if [[ -n "$VERSION" ]]; then
  echo "🏗️  Building production uberjar (version: $VERSION)..."
  clj -T:build uber :version "\"$VERSION\""
else
  echo "🏗️  Building production uberjar..."
  clj -T:build uber
fi
