#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
MAIN_REPO_DIR="${REPO_ROOT}"

# Shadow-cljs ports (unique per instance so instances can run concurrently).
# These are controlled via env vars read in `shadow-cljs.edn` (#shadow/env):
# - SHADOW_HTTP_PORT (Shadow server/UI) defaults to 9630
# - SHADOW_NREPL_PORT defaults to 8777
# - SHADOW_TEST_HTTP_PORT defaults to 9095 (dev-http for :test build)
# - SHADOW_DEVTOOLS_HTTP_PORT defaults to 9650 (optional; not the main server)
SHADOW_SERVER_PORT_BASE=9630
SHADOW_NREPL_PORT_BASE=8777
SHADOW_TEST_HTTP_PORT_BASE=9095
SHADOW_DEVTOOLS_HTTP_PORT_BASE=9650

load_local_env() {
    local repo_dir="$1"
    if [ -f "${repo_dir}/.env" ]; then
        echo "🔐 Loading .env from ${repo_dir}..."
        set -a
        # shellcheck disable=SC1090
        if ! source "${repo_dir}/.env"; then
            echo "⚠️  Failed to source .env (check syntax)"
        fi
        set +a
    fi
}

wait_for_port() {
    local port="$1"
    local max_wait_seconds="$2"
    local count=0

    until lsof -ti:"$port" > /dev/null 2>&1 || [ "$count" -ge "$max_wait_seconds" ]; do
        sleep 1
        ((count++))
    done

    lsof -ti:"$port" > /dev/null 2>&1
}

shadow_server_port_for_label() {
    local label="$1"
    local off
    case "$label" in
        wk1) off=1 ;;
        wk2) off=2 ;;
        *) off=0 ;;
    esac
    echo "$((SHADOW_SERVER_PORT_BASE + off))"
}

shadow_devtools_http_port_for_label() {
    local label="$1"
    local off
    case "$label" in
        wk1) off=1 ;;
        wk2) off=2 ;;
        *) off=0 ;;
    esac
    echo "$((SHADOW_DEVTOOLS_HTTP_PORT_BASE + off))"
}

shadow_nrepl_port_for_label() {
    local label="$1"
    local off
    case "$label" in
        wk1) off=1 ;;
        wk2) off=2 ;;
        *) off=0 ;;
    esac
    echo "$((SHADOW_NREPL_PORT_BASE + off))"
}

shadow_test_http_port_for_label() {
    local label="$1"
    local off
    case "$label" in
        wk1) off=1 ;;
        wk2) off=2 ;;
        *) off=0 ;;
    esac
    echo "$((SHADOW_TEST_HTTP_PORT_BASE + off))"
}

ensure_worktree_node_modules() {
    local label="$1"
    local repo_dir="$2"
    local worktree_node_modules="${repo_dir}/node_modules"
    local main_node_modules="${MAIN_REPO_DIR}/node_modules"

    if [ -d "$worktree_node_modules" ] || [ -L "$worktree_node_modules" ]; then
        return 0
    fi

    if [ ! -d "$main_node_modules" ]; then
        echo "⚠️  ${label}: main node_modules not found at ${main_node_modules}; shadow build may fail (missing react)"
        return 0
    fi

    if [ -e "$worktree_node_modules" ] && [ ! -L "$worktree_node_modules" ] && [ ! -d "$worktree_node_modules" ]; then
        echo "⚠️  ${label}: ${worktree_node_modules} exists but is not a directory; skipping node_modules link"
        return 0
    fi

    ln -sfn "$main_node_modules" "$worktree_node_modules"
    echo "🔗 ${label}: linked node_modules -> ${main_node_modules}"
}

ensure_shadow_cljs() {
    local label="$1"
    local repo_dir="$2"

    local shadow_server_port
    local shadow_devtools_http_port
    local shadow_nrepl_port
    local shadow_test_http_port
    shadow_server_port="$(shadow_server_port_for_label "$label")"
    shadow_devtools_http_port="$(shadow_devtools_http_port_for_label "$label")"
    shadow_nrepl_port="$(shadow_nrepl_port_for_label "$label")"
    shadow_test_http_port="$(shadow_test_http_port_for_label "$label")"

    local shadow_pid_file="${repo_dir}/tmp/shadow_cljs.pid"
    local shadow_log_file="${repo_dir}/tmp/shadow_cljs.log"

    if lsof -ti:"$shadow_server_port" > /dev/null 2>&1; then
        echo "🟢 shadow-cljs (${label}) already running on port ${shadow_server_port}"
        return 0
    fi

    if [ -f "$shadow_pid_file" ]; then
        local existing_pid
        existing_pid="$(cat "$shadow_pid_file" 2>/dev/null || true)"
        if [ -n "$existing_pid" ] && ps -p "$existing_pid" > /dev/null 2>&1; then
            echo "🟢 shadow-cljs (${label}) already running (PID: ${existing_pid})"
            return 0
        fi
        rm -f "$shadow_pid_file"
    fi

    ensure_worktree_node_modules "$label" "$repo_dir"

    echo "🧠 Starting shadow-cljs (${label}) on server port ${shadow_server_port} (nREPL ${shadow_nrepl_port}, CLJS test HTTP ${shadow_test_http_port})..."
    mkdir -p "${repo_dir}/tmp"

    (
      cd "$repo_dir" || exit 1
      SHADOW_HTTP_PORT="$shadow_server_port" \
      SHADOW_NREPL_PORT="$shadow_nrepl_port" \
      SHADOW_TEST_HTTP_PORT="$shadow_test_http_port" \
      SHADOW_DEVTOOLS_HTTP_PORT="$shadow_devtools_http_port" \
            nohup clojure -M -m shadow.cljs.devtools.cli --force-spawn watch app admin > "$shadow_log_file" 2>&1 &
      echo $! > "$shadow_pid_file"
    )

    if wait_for_port "$shadow_server_port" 40; then
        echo "✅ shadow-cljs (${label}) running on port ${shadow_server_port}"
        echo "📄 Shadow logs: ${shadow_log_file}"
    else
        echo "⚠️  shadow-cljs (${label}) did not bind to port ${shadow_server_port} in time"
        echo "📄 Check logs: ${shadow_log_file}"
    fi
}

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

ensure_worktree_public_assets() {
    local label="$1"
    local repo_dir="$2"
    local worktree_public_dir="${repo_dir}/resources/public"
    local main_public_dir="${MAIN_REPO_DIR}/resources/public"

    if [ ! -d "$main_public_dir" ]; then
        echo "⚠️  ${label}: main public assets folder is missing at ${main_public_dir}"
        return
    fi

    if [ -L "$worktree_public_dir" ]; then
        rm -f "$worktree_public_dir"
    fi

    mkdir -p "$worktree_public_dir"

    if [ ! -f "${worktree_public_dir}/index.html" ] && [ -f "${main_public_dir}/index.html" ]; then
        cp "${main_public_dir}/index.html" "${worktree_public_dir}/index.html"
        echo "📄 ${label}: copied index.html"
    fi

    if [ ! -f "${worktree_public_dir}/admin.html" ] && [ -f "${main_public_dir}/admin.html" ]; then
        cp "${main_public_dir}/admin.html" "${worktree_public_dir}/admin.html"
        echo "📄 ${label}: copied admin.html"
    fi

    if [ ! -e "${worktree_public_dir}/assets" ] && [ -d "${main_public_dir}/assets" ]; then
        ln -sfn "${main_public_dir}/assets" "${worktree_public_dir}/assets"
        echo "🔗 ${label}: linked assets -> ${main_public_dir}/assets"
    fi

    if [ ! -e "${worktree_public_dir}/favicon.ico" ] && [ -f "${main_public_dir}/favicon.ico" ]; then
        ln -sfn "${main_public_dir}/favicon.ico" "${worktree_public_dir}/favicon.ico"
    fi
}

usage() {
    echo "Usage: bb restart-wt <worktree>"
    echo "Example: bb restart-wt wk2"
    echo "Supported worktrees: wk1, wk2"
}

if [ "$#" -ne 1 ]; then
    usage
    exit 1
fi

WORKTREE_NAME="$1"
case "$WORKTREE_NAME" in
    wk1)
        WORKTREE_PORT=8086
        ;;
    wk2)
        WORKTREE_PORT=8087
        ;;
    *)
        echo "❌ Unsupported worktree: ${WORKTREE_NAME}"
        usage
        exit 1
        ;;
esac

WORKTREE_DIR="${REPO_ROOT}/worktrees/${WORKTREE_NAME}"
if [ ! -d "$WORKTREE_DIR" ]; then
    echo "❌ Worktree directory not found: ${WORKTREE_DIR}"
    exit 1
fi

PID_FILE="${WORKTREE_DIR}/tmp/run-app-${WORKTREE_NAME}.pid"
LOG_FILE="${WORKTREE_DIR}/tmp/run-app-${WORKTREE_NAME}.log"

printf "\033]0;🔁 Restarting ${WORKTREE_NAME}\007"

load_local_env "$MAIN_REPO_DIR"
load_local_env "$WORKTREE_DIR"

ensure_worktree_public_assets "$WORKTREE_NAME" "$WORKTREE_DIR"

# Ensure local dependencies are up
(
  cd "$MAIN_REPO_DIR" || exit 1
  echo "Bringing up Docker services..."
  docker compose up -d
)

echo "Waiting for Postgres to be ready..."
MAX_RETRIES=30
COUNT=0
until pg_isready -h localhost -p 55432 -U app_user -d single_tenant_pos > /dev/null 2>&1 || [ "$COUNT" -eq "$MAX_RETRIES" ]; do
  echo -n "."
  sleep 1
  ((COUNT++))
done

if [ "$COUNT" -eq "$MAX_RETRIES" ]; then
  echo "❌ Postgres failed to become ready in time."
  exit 1
else
  echo " ✅ Postgres is ready!"
fi

ensure_shadow_cljs "$WORKTREE_NAME" "$WORKTREE_DIR"

stop_pid_file_process "$PID_FILE" "${WORKTREE_NAME} app"
stop_port_processes "$WORKTREE_PORT"

mkdir -p "${WORKTREE_DIR}/tmp"

echo "🌿 Starting ${WORKTREE_NAME} on port ${WORKTREE_PORT}..."
(
  cd "$WORKTREE_DIR" || exit 1

  APP_WEB_PORT="$WORKTREE_PORT" MAIN_REPO_DIR="$MAIN_REPO_DIR" nohup clojure \
    -Sdeps '{:deps {org.clojure/tools.namespace {:mvn/version "1.5.0"}}}' \
    -M \
    -e '(do
          (require (quote [aero.core :as aero])
                   (quote [app.template.backend.core :as backend])
                   (quote [clojure.java.io :as io]))
          (let [desired-port (Integer/parseInt (or (System/getenv "APP_WEB_PORT") "8085"))
                main-repo (or (System/getenv "MAIN_REPO_DIR") (System/getProperty "user.dir"))
                config-file (io/file main-repo "config/base.edn")]
            (alter-var-root
             (var app.template.backend.core/load-config)
             (fn [_]
               (fn [opts]
                 (let [profile (get opts :profile :dev)
                       cfg (aero/read-config config-file {:profile profile})
                       overridden-config (assoc-in cfg [:webserver :port] desired-port)]
                   (backend/closeable-data overridden-config))))))
          @(backend/main))' \
    > "$LOG_FILE" 2>&1 &

  echo $! > "$PID_FILE"
)

if wait_for_port "$WORKTREE_PORT" 20; then
    echo "✅ ${WORKTREE_NAME} restarted on port ${WORKTREE_PORT}"
    echo "📄 Logs: ${LOG_FILE}"
else
    echo "❌ ${WORKTREE_NAME} failed to bind to port ${WORKTREE_PORT}"
    echo "📄 Check logs: ${LOG_FILE}"
    exit 1
fi
