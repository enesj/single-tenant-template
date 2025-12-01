#!/usr/bin/env bash
# Synchronise project-local Codex MCP configuration into the global Codex config.
# Workaround while Codex lacks native project-level MCP server support.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../../.. && pwd)"
PROJECT_CONFIG="${PROJECT_ROOT}/.codex/codex.toml"
GLOBAL_CONFIG="${HOME}/.codex/config.toml"
BACKUP_CONFIG="${GLOBAL_CONFIG}.bak.$(date +%Y%m%d%H%M%S)"

if [[ ! -f "${PROJECT_CONFIG}" ]]; then
  if command -v git >/dev/null 2>&1; then
    PROJECT_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null || echo "")"
    PROJECT_CONFIG="${PROJECT_ROOT}/.codex/codex.toml"
  fi
fi

if [[ -z "${PROJECT_ROOT}" || ! -f "${PROJECT_CONFIG}" ]]; then
  echo "Project Codex config not found at ${PROJECT_CONFIG:-<unknown>}" >&2
  exit 1
fi

if [[ ! -d "${HOME}/.codex" ]]; then
  mkdir -p "${HOME}/.codex"
fi

if [[ ! -f "${GLOBAL_CONFIG}" ]]; then
  # Create an empty global config so we can write to it.
  touch "${GLOBAL_CONFIG}"
fi

# Extract the clojure-mcp block from the project config.
CLOJURE_BLOCK="$(awk '
  BEGIN { capture = 0 }
  /^\[mcp_servers\.clojure-mcp\]/ { capture = 1 }
  capture && /^\[mcp_servers\./ && $0 !~ /^\[mcp_servers\.clojure-mcp\]/ { exit }
  capture { print }
' "${PROJECT_CONFIG}")"

if [[ -z "${CLOJURE_BLOCK}" ]]; then
  echo "No [mcp_servers.clojure-mcp] block found in ${PROJECT_CONFIG}" >&2
  exit 1
fi

# Backup current global config for safety.
cp "${GLOBAL_CONFIG}" "${BACKUP_CONFIG}"

tmp_file="$(mktemp)"
trap 'rm -f "${tmp_file}"' EXIT

# Remove any existing clojure-mcp block from the global config.
awk '
  BEGIN { skip = 0 }
  /^\[mcp_servers\.clojure-mcp\]/ { skip = 1; next }
  skip && /^\[mcp_servers\./ { skip = 0 }
  skip { next }
  { print }
' "${BACKUP_CONFIG}" > "${tmp_file}"

# Ensure file ends with a newline before appending.
if [[ -s "${tmp_file}" && $(tail -c1 "${tmp_file}" | tr -d '\n') != '' ]]; then
  printf '\n' >> "${tmp_file}"
fi

printf '%s\n' "${CLOJURE_BLOCK}" >> "${tmp_file}"

mv "${tmp_file}" "${GLOBAL_CONFIG}"

echo "Synced clojure-mcp configuration into ${GLOBAL_CONFIG}"
