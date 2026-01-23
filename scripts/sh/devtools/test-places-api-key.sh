#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Test Google Places API v1 key via places:searchText.

Usage:
  PLACES_API_KEY=... scripts/sh/devtools/test-places-api-key.sh "bingo sarajevo"

If PLACES_API_KEY is not set, this script will try to read it from:
  config/.secrets.edn  (requires: bb)

Optional env vars:
  PLACES_REGION_CODE   (default: BA)
  PLACES_LANGUAGE_CODE (default: bs)
  PLACES_MAX_RESULTS   (default: 5)
  PLACES_BASE_URL      (default: https://places.googleapis.com/v1/places:searchText)
  PLACES_FIELD_MASK    (default: places.displayName,places.id)

Exit codes:
  0 on successful request execution (regardless of HTTP status)
  1 on missing arguments/env
USAGE
}

json_escape() {
  # Minimal JSON string escaping for common inputs.
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//"/\\"}"
  s="${s//$'\n'/\\n}"
  s="${s//$'\r'/\\r}"
  s="${s//$'\t'/\\t}"
  printf '%s' "$s"
}

QUERY_RAW="${1:-}"
if [[ -z "$QUERY_RAW" ]] || [[ "$QUERY_RAW" == "-h" ]] || [[ "$QUERY_RAW" == "--help" ]]; then
  usage
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

if [[ -z "${PLACES_API_KEY:-}" ]]; then
  if command -v bb >/dev/null 2>&1 && [[ -f "$REPO_ROOT/config/.secrets.edn" ]]; then
    # Read from config/.secrets.edn without printing the key.
    # NOTE: This assumes config/.secrets.edn is a plain EDN map.
    PLACES_API_KEY="$(
      bb -e '(-> "config/.secrets.edn" slurp clojure.edn/read-string :places :api-key)' \
        --classpath "$REPO_ROOT" \
        2>/dev/null || true
    )"
  fi
fi

: "${PLACES_API_KEY:?Set PLACES_API_KEY env var (recommended) or add :places {:api-key ...} to config/.secrets.edn (requires bb). Do not commit it to git.}"

REGION_CODE="${PLACES_REGION_CODE:-BA}"
LANGUAGE_CODE="${PLACES_LANGUAGE_CODE:-bs}"
MAX_RESULTS="${PLACES_MAX_RESULTS:-5}"
BASE_URL="${PLACES_BASE_URL:-https://places.googleapis.com/v1/places:searchText}"
FIELD_MASK="${PLACES_FIELD_MASK:-places.displayName,places.id}"

QUERY="$(json_escape "$QUERY_RAW")"

BODY=$(
  cat <<JSON
{"textQuery":"$QUERY","regionCode":"$REGION_CODE","languageCode":"$LANGUAGE_CODE","maxResultCount":$MAX_RESULTS}
JSON
)

# Print HTTP status on a separate line for quick inspection.
# Note: this does not print the API key.
response=$(curl -sS -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $PLACES_API_KEY" \
  -H "X-Goog-FieldMask: $FIELD_MASK" \
  --data "$BODY" \
  -w "\nHTTP_STATUS:%{http_code}\n")

echo "$response"
