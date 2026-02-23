#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

stop_pid_file_process() {
    local pid_file="$1"
    local label="$2"

    if [ ! -f "$pid_file" ]; then
        return
    fi

    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"

    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        echo "🛑 Stopping ${label} (PID: ${pid})..."
        kill -TERM "$pid" 2>/dev/null || true
        sleep 1
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "⚠️  Force stopping ${label} (PID: ${pid})..."
            kill -9 "$pid" 2>/dev/null || true
        fi
    fi

    rm -f "$pid_file"
}

stop_port_processes() {
    local port="$1"
    local pids

    pids="$(lsof -ti:"$port" 2>/dev/null || true)"

    if [ -z "$pids" ]; then
        echo "ℹ️  No process listening on port ${port}"
        return
    fi

    echo "🛑 Stopping process(es) on port ${port}: ${pids}"
    # shellcheck disable=SC2086
    kill -TERM $pids 2>/dev/null || true

    sleep 1

    local remaining
    remaining="$(lsof -ti:"$port" 2>/dev/null || true)"
    if [ -n "$remaining" ]; then
        echo "⚠️  Force stopping remaining process(es) on port ${port}: ${remaining}"
        # shellcheck disable=SC2086
        kill -9 $remaining 2>/dev/null || true
    fi
}

echo "Stopping app servers (main + wk1 + wk2)..."
for port in 8085 8086 8087; do
    stop_port_processes "$port"
done

echo "Stopping shadow-cljs servers (main + wk1 + wk2)..."
for port in 9630 9631 9632; do
    stop_port_processes "$port"
done

# Stop pid-file managed helpers
stop_pid_file_process "${REPO_ROOT}/worktrees/wk1/tmp/run-app-wk1.pid" "wk1 background app"
stop_pid_file_process "${REPO_ROOT}/worktrees/wk2/tmp/run-app-wk2.pid" "wk2 background app"
stop_pid_file_process "${REPO_ROOT}/tmp/postcss_watcher.pid" "PostCSS watcher"
stop_pid_file_process "${REPO_ROOT}/tmp/shadow_cljs.pid" "shadow-cljs (main)"
stop_pid_file_process "${REPO_ROOT}/worktrees/wk1/tmp/shadow_cljs.pid" "shadow-cljs (wk1)"
stop_pid_file_process "${REPO_ROOT}/worktrees/wk2/tmp/shadow_cljs.pid" "shadow-cljs (wk2)"

# Stop monitor_terminal wrapper process if metadata exists
ACTIVE_META_FILE="/tmp/active_command.txt"
if [ -f "$ACTIVE_META_FILE" ]; then
    monitor_pid_file="$(awk -F= '$1=="PID_FILE" {print substr($0, index($0, "=")+1)}' "$ACTIVE_META_FILE" 2>/dev/null || true)"
    if [ -n "$monitor_pid_file" ]; then
        stop_pid_file_process "$monitor_pid_file" "monitor command"
    fi
    rm -f "$ACTIVE_META_FILE" 2>/dev/null || true
fi

echo "✅ stop-app complete"
