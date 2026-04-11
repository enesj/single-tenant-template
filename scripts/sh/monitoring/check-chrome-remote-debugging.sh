#!/bin/bash

# Check whether a running Chrome instance exposes the remote debugging endpoint.
#
# Default behavior:
# - inspect the running Chrome processes for remote-debugging flags
# - probe the DevTools discovery endpoint on the detected port (or 9222)
#
# Usage: ./check-chrome-remote-debugging.sh [-H host] [-p port]

set -euo pipefail

HOST="127.0.0.1"
PORT=""

usage() {
  cat <<'EOF'
Usage: check-chrome-remote-debugging.sh [-H host] [-p port]

Options:
  -H host   Host to check (default: 127.0.0.1)
  -p port   Remote debugging port to check (default: auto-detect, then 9222)
  -h        Show this help message
EOF
}

while getopts ":H:p:h" opt; do
  case "$opt" in
    H) HOST="$OPTARG" ;;
    p) PORT="$OPTARG" ;;
    h)
      usage
      exit 0
      ;;
    \?)
      echo "Unknown option: -$OPTARG" >&2
      usage >&2
      exit 2
      ;;
    :)
      echo "Option -$OPTARG requires an argument" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ! command -v curl >/dev/null 2>&1; then
  echo "❌ curl is required but not installed" >&2
  exit 1
fi

chrome_cmds="$(ps -ww -ax -o command= | grep 'Google Chrome' | grep -v grep || true)"

if printf '%s\n' "$chrome_cmds" | grep -q -- '--remote-debugging-pipe'; then
  echo "✅ Remote debugging is enabled on a running Chrome instance (pipe mode)."
  exit 0
fi

detected_port="$(printf '%s\n' "$chrome_cmds" \
  | sed -n 's/.*--remote-debugging-port=\([0-9][0-9]*\).*/\1/p' \
  | head -n 1)"

if [ -z "$PORT" ]; then
  if [ -n "$detected_port" ]; then
    PORT="$detected_port"
  else
    PORT="9222"
  fi
fi

endpoint="http://${HOST}:${PORT}/json/version"
response="$(curl -sS --max-time 3 --connect-timeout 2 -w $'\n%{http_code}' "$endpoint" 2>/dev/null || true)"

if [ -n "$response" ]; then
  http_code="${response##*$'\n'}"
  body="${response%$'\n'*}"

  if [ "$http_code" = "200" ]; then
    browser="$(printf '%s' "$body" | sed -n 's/.*"Browser":"\([^"]*\)".*/\1/p' | head -n 1)"
    ws_url="$(printf '%s' "$body" | sed -n 's/.*"webSocketDebuggerUrl":"\([^"]*\)".*/\1/p' | head -n 1)"

    echo "✅ Remote debugging is enabled on ${HOST}:${PORT}"
    if [ -n "$browser" ]; then
      echo "   Browser: $browser"
    fi
    if [ -n "$ws_url" ]; then
      echo "   WebSocket: $ws_url"
    fi
    exit 0
  fi

  if [ "$http_code" = "404" ]; then
    if [ -n "$detected_port" ]; then
      echo "❌ Chrome is running with --remote-debugging-port=${detected_port}, but the DevTools endpoint at ${HOST}:${PORT} returned 404." >&2
    else
      echo "❌ Chrome is reachable on ${HOST}:${PORT}, but remote debugging is not enabled (DevTools JSON endpoint returned 404)." >&2
    fi
  else
    if [ -n "$detected_port" ]; then
      echo "❌ Chrome is running with --remote-debugging-port=${detected_port}, but the DevTools endpoint at ${HOST}:${PORT} is not reachable (HTTP ${http_code})." >&2
    else
      echo "❌ Chrome remote debugging is not enabled or not reachable at ${HOST}:${PORT} (HTTP ${http_code})." >&2
    fi
  fi
else
  if [ -n "$detected_port" ]; then
    echo "❌ Chrome is running with --remote-debugging-port=${detected_port}, but the DevTools endpoint at ${HOST}:${PORT} is not reachable." >&2
  else
    echo "❌ No running Chrome instance appears to have remote debugging enabled." >&2
    echo "   No --remote-debugging-port or --remote-debugging-pipe flag found in Chrome command lines." >&2
    echo "   Hint: start Chrome with --remote-debugging-port=9222 --user-data-dir=/path/to/profile" >&2
  fi
fi

exit 1
