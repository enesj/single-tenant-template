#!/bin/bash

# Launch a dedicated Chrome instance that exposes the DevTools remote debugging endpoint.
#
# This is intended for chrome-devtools MCP usage with a stable local port and a
# dedicated profile directory so the browser stays isolated from the user's main
# Chrome session.

set -euo pipefail

HOST="127.0.0.1"
PORT="9222"
PROFILE_DIR="${PWD}/tmp/chrome-devtools-profile-9222"
LOG_FILE="${PWD}/tmp/chrome-devtools-9222.log"

usage() {
  cat <<'EOF'
Usage: run-chrome-devtools.sh

Starts Chrome with:
  - remote debugging on 127.0.0.1:9222
  - a dedicated profile under tmp/chrome-devtools-profile-9222
  - logging to tmp/chrome-devtools-9222.log
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "❌ curl is required but not installed" >&2
  exit 1
fi

CHROME_BIN=""
if [ -x "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" ]; then
  CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
elif command -v google-chrome >/dev/null 2>&1; then
  CHROME_BIN="$(command -v google-chrome)"
elif command -v chromium >/dev/null 2>&1; then
  CHROME_BIN="$(command -v chromium)"
fi

if [ -z "$CHROME_BIN" ]; then
  echo "❌ Chrome/Chromium not found" >&2
  exit 1
fi

mkdir -p "$PROFILE_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

if curl -sf "http://${HOST}:${PORT}/json/version" >/dev/null 2>&1; then
  echo "✅ Chrome DevTools is already available at http://${HOST}:${PORT}"
  echo "   Profile: $PROFILE_DIR"
  exit 0
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "⚠️ Port ${PORT} is already in use. Restarting the dedicated Chrome debug instance."
  pkill -f -- "--remote-debugging-port=${PORT}" || true
  pkill -f -- "--user-data-dir=${PROFILE_DIR}" || true
  sleep 1
fi

nohup "$CHROME_BIN" \
  --remote-debugging-port="$PORT" \
  --user-data-dir="$PROFILE_DIR" \
  --no-first-run \
  --no-default-browser-check \
  about:blank >>"$LOG_FILE" 2>&1 &

chrome_pid=$!
echo "$chrome_pid" > "$PROFILE_DIR/chrome.pid"

echo "🚀 Starting Chrome for DevTools MCP..."
echo "   PID: $chrome_pid"
echo "   Profile: $PROFILE_DIR"
echo "   Log: $LOG_FILE"

for _ in $(seq 1 30); do
  if curl -sf "http://${HOST}:${PORT}/json/version" >/tmp/chrome-devtools-version.json 2>/dev/null; then
    echo "✅ Chrome DevTools is ready at http://${HOST}:${PORT}"
    scripts/sh/monitoring/check-chrome-remote-debugging.sh -p "$PORT"
    exit 0
  fi
  sleep 1
done

echo "❌ Chrome did not expose the DevTools endpoint on ${HOST}:${PORT} within 30 seconds" >&2
if [ -f "$LOG_FILE" ]; then
  echo "--- Chrome log tail ---" >&2
  tail -n 40 "$LOG_FILE" >&2 || true
fi
exit 1
