#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CURRENT_REPO_DIR="$(git -C "$PWD" rev-parse --show-toplevel 2>/dev/null || pwd)"
GIT_COMMON_DIR="$(git -C "$CURRENT_REPO_DIR" rev-parse --path-format=absolute --git-common-dir 2>/dev/null || true)"

if [ -n "$GIT_COMMON_DIR" ]; then
    MAIN_REPO_DIR="$(cd "$(dirname "$GIT_COMMON_DIR")" && pwd)"
else
    MAIN_REPO_DIR="$CURRENT_REPO_DIR"
fi

WORKTREES_ROOT="${MAIN_REPO_DIR}/worktrees"

APP_SCOPE="current"
TARGET_INSTANCE=""
for arg in "$@"; do
    case "$arg" in
        all|--all)
            APP_SCOPE="all"
            TARGET_INSTANCE=""
            ;;
        main|wk1|wk2)
            APP_SCOPE="single"
            TARGET_INSTANCE="$arg"
            ;;
    esac
done
WK1_REPO_DIR="${WORKTREES_ROOT}/wk1"
WK2_REPO_DIR="${WORKTREES_ROOT}/wk2"

MAIN_PORT=8085
WK1_PORT=8086
WK2_PORT=8087

CURRENT_REPO_BASENAME="$(basename "$CURRENT_REPO_DIR")"
CURRENT_INSTANCE_LABEL="main"
CURRENT_INSTANCE_PORT="$MAIN_PORT"

case "$CURRENT_REPO_BASENAME" in
    wk1)
        CURRENT_INSTANCE_LABEL="wk1"
        CURRENT_INSTANCE_PORT="$WK1_PORT"
        ;;
    wk2)
        CURRENT_INSTANCE_LABEL="wk2"
        CURRENT_INSTANCE_PORT="$WK2_PORT"
        ;;
esac

EFFECTIVE_INSTANCE_LABEL="$CURRENT_INSTANCE_LABEL"
EFFECTIVE_INSTANCE_PORT="$CURRENT_INSTANCE_PORT"
EFFECTIVE_INSTANCE_DIR="$CURRENT_REPO_DIR"

if [ "$APP_SCOPE" = "single" ]; then
    case "$TARGET_INSTANCE" in
        main)
            EFFECTIVE_INSTANCE_LABEL="main"
            EFFECTIVE_INSTANCE_PORT="$MAIN_PORT"
            EFFECTIVE_INSTANCE_DIR="$MAIN_REPO_DIR"
            ;;
        wk1)
            EFFECTIVE_INSTANCE_LABEL="wk1"
            EFFECTIVE_INSTANCE_PORT="$WK1_PORT"
            EFFECTIVE_INSTANCE_DIR="$WK1_REPO_DIR"
            ;;
        wk2)
            EFFECTIVE_INSTANCE_LABEL="wk2"
            EFFECTIVE_INSTANCE_PORT="$WK2_PORT"
            EFFECTIVE_INSTANCE_DIR="$WK2_REPO_DIR"
            ;;
    esac
fi

# Load local environment variables (optional; never committed).
# - `.env` supports KEY=VALUE lines (exported via `set -a`)
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

# PID file for PostCSS watcher
POSTCSS_PID_FILE="${MAIN_REPO_DIR}/tmp/postcss_watcher.pid"
POSTCSS_LOG_FILE="${MAIN_REPO_DIR}/tmp/postcss_watcher.log"
WORKTREE_PID_FILES=()
CLEANUP_ON_EXIT=true

# Shadow-cljs ports (unique per instance so worktrees can run concurrently).
# These are controlled via env vars read in `shadow-cljs.edn` (#shadow/env):
# - SHADOW_HTTP_PORT (Shadow server/UI) defaults to 9630
# - SHADOW_NREPL_PORT defaults to 8777
# - SHADOW_TEST_HTTP_PORT defaults to 9095 (dev-http for :test build)
# - SHADOW_DEVTOOLS_HTTP_PORT defaults to 9650 (optional; not the main server)
SHADOW_SERVER_PORT_BASE=9630
SHADOW_NREPL_PORT_BASE=8777
SHADOW_TEST_HTTP_PORT_BASE=9095
SHADOW_DEVTOOLS_HTTP_PORT_BASE=9650

# Returns 0 when something is listening on a port, non-zero otherwise.
is_port_running() {
    local port="$1"
    lsof -ti:"$port" > /dev/null 2>&1
}

wait_for_port() {
    local port="$1"
    local max_wait_seconds="$2"
    local count=0

    until is_port_running "$port" || [ "$count" -ge "$max_wait_seconds" ]; do
        sleep 1
        ((count++))
    done

    is_port_running "$port"
}

shadow_offset_for_label() {
    local label="$1"
    case "$label" in
        main) echo 0 ;;
        wk1) echo 1 ;;
        wk2) echo 2 ;;
        *) echo 0 ;;
    esac
}

shadow_server_port_for_label() {
    local label="$1"
    local off
    off="$(shadow_offset_for_label "$label")"
    echo "$((SHADOW_SERVER_PORT_BASE + off))"
}

shadow_devtools_http_port_for_label() {
    local label="$1"
    local off
    off="$(shadow_offset_for_label "$label")"
    echo "$((SHADOW_DEVTOOLS_HTTP_PORT_BASE + off))"
}

shadow_nrepl_port_for_label() {
    local label="$1"
    local off
    off="$(shadow_offset_for_label "$label")"
    echo "$((SHADOW_NREPL_PORT_BASE + off))"
}

shadow_test_http_port_for_label() {
    local label="$1"
    local off
    off="$(shadow_offset_for_label "$label")"
    echo "$((SHADOW_TEST_HTTP_PORT_BASE + off))"
}

# The main dev process (`clojure -M:dev`) starts Shadow itself via `shadow.server/start!`.
# If a separate Shadow server is already running (e.g. from a prior script version),
# starting the main dev process will fail with "shadow-cljs already running in project".
# We only stop external Shadow for main when the main app (8085) is NOT running.
stop_conflicting_main_shadow() {
    local desired_shadow_port
    desired_shadow_port="$(shadow_server_port_for_label "main")"

    # If we have our own pid file from a previous run, stop it first.
    local old_pid_file="${MAIN_REPO_DIR}/tmp/shadow_cljs.pid"
    if [ -f "$old_pid_file" ]; then
        local old_pid
        old_pid="$(cat "$old_pid_file" 2>/dev/null || true)"
        if [ -n "$old_pid" ] && ps -p "$old_pid" > /dev/null 2>&1; then
            echo "🛑 Stopping old shadow-cljs (main) PID ${old_pid} so clojure -M:dev can start its integrated Shadow server..."
            kill -TERM "$old_pid" 2>/dev/null || true
            sleep 1
            if ps -p "$old_pid" > /dev/null 2>&1; then
                kill -9 "$old_pid" 2>/dev/null || true
            fi
        fi
        rm -f "$old_pid_file" 2>/dev/null || true
    fi

    # If something is still listening on the main Shadow port, stop it best-effort.
    if is_port_running "$desired_shadow_port"; then
        echo "🛑 Found an external shadow-cljs server on port ${desired_shadow_port}. Stopping it so clojure -M:dev can start its integrated Shadow server..."
        (
          cd "$MAIN_REPO_DIR" || exit 1
          clojure -M -m shadow.cljs.devtools.cli stop > /dev/null 2>&1 || true
        )
        sleep 1

        if is_port_running "$desired_shadow_port"; then
            local pids
            pids="$(lsof -tiTCP:"$desired_shadow_port" -sTCP:LISTEN 2>/dev/null || lsof -ti:"$desired_shadow_port" 2>/dev/null || true)"
            if [ -n "$pids" ]; then
                echo "⚠️  Force stopping shadow-cljs listener(s) on port ${desired_shadow_port}: ${pids}"
                # shellcheck disable=SC2086
                kill -TERM $pids 2>/dev/null || true
                sleep 1
                local remaining
                remaining="$(lsof -tiTCP:"$desired_shadow_port" -sTCP:LISTEN 2>/dev/null || lsof -ti:"$desired_shadow_port" 2>/dev/null || true)"
                if [ -n "$remaining" ]; then
                    # shellcheck disable=SC2086
                    kill -9 $remaining 2>/dev/null || true
                fi
            fi
        fi
    fi
}

ensure_worktree_node_modules() {
    local label="$1"
    local repo_dir="$2"
    local worktree_node_modules="${repo_dir}/node_modules"
    local main_node_modules="${MAIN_REPO_DIR}/node_modules"

    if [ "$label" = "main" ]; then
        return 0
    fi

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

    if is_port_running "$shadow_server_port"; then
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

print_port_status() {
    local label="$1"
    local port="$2"
    local existing_pid
    existing_pid=$(lsof -ti:"$port" | head -n 1)

    if [ -n "$existing_pid" ]; then
        local process_info
        process_info=$(ps -p "$existing_pid" -o pid,comm,args --no-headers 2>/dev/null)
        if [ -n "$process_info" ]; then
            echo "🟢 ${label} already running on port ${port}: ${process_info}"
        else
            echo "🟢 ${label} already running on port ${port} (PID: ${existing_pid})"
        fi
    else
        echo "🚀 ${label} is not running on port ${port}"
    fi
}

# Function to start PostCSS watcher
start_postcss() {
    echo "🎨 Starting PostCSS watcher..."
    mkdir -p "${MAIN_REPO_DIR}/tmp"

    # Check if already running
    if [ -f "$POSTCSS_PID_FILE" ]; then
        local existing_pid=$(cat "$POSTCSS_PID_FILE")
        if ps -p "$existing_pid" > /dev/null 2>&1; then
            echo "   ℹ️  PostCSS watcher already running (PID: $existing_pid)"
            return
        else
            rm -f "$POSTCSS_PID_FILE"
        fi
    fi
    
    # Start in background with nohup to survive terminal
    (
        cd "$MAIN_REPO_DIR" || exit 1
        nohup npm run develop > "$POSTCSS_LOG_FILE" 2>&1 &
        echo $! > "$POSTCSS_PID_FILE"
    )
    local postcss_pid
    postcss_pid=$(cat "$POSTCSS_PID_FILE" 2>/dev/null)
    echo "   ✅ PostCSS watcher started (PID: $postcss_pid)"
    echo "   📄 Logs: ${POSTCSS_LOG_FILE}"
}

# Function to stop PostCSS watcher
stop_postcss() {
    if [ -f "$POSTCSS_PID_FILE" ]; then
        local postcss_pid=$(cat "$POSTCSS_PID_FILE")
        if ps -p "$postcss_pid" > /dev/null 2>&1; then
            echo "🛑 Stopping PostCSS watcher (PID: $postcss_pid)..."
            kill -TERM "$postcss_pid" 2>/dev/null
            # Wait a bit for graceful shutdown
            sleep 2
            # Force kill if still running
            if ps -p "$postcss_pid" > /dev/null 2>&1; then
                kill -9 "$postcss_pid" 2>/dev/null
            fi
        fi
        rm -f "$POSTCSS_PID_FILE"
    fi
}

stop_worktree_apps() {
    local pid_file
    for pid_file in "${WORKTREE_PID_FILES[@]}"; do
        if [ -f "$pid_file" ]; then
            local worktree_pid
            worktree_pid=$(cat "$pid_file")
            if [ -n "$worktree_pid" ] && ps -p "$worktree_pid" > /dev/null 2>&1; then
                echo "🛑 Stopping worktree app (PID: $worktree_pid)..."
                kill -TERM "$worktree_pid" 2>/dev/null
                sleep 2
                if ps -p "$worktree_pid" > /dev/null 2>&1; then
                    kill -9 "$worktree_pid" 2>/dev/null
                fi
            fi
            rm -f "$pid_file"
        fi
    done
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

cleanup() {
    if [ "$CLEANUP_ON_EXIT" != "true" ]; then
        return
    fi

    stop_worktree_apps
    stop_postcss
}

start_worktree_app() {
    local label="$1"
    local repo_dir="$2"
    local port="$3"

    if [ ! -d "$repo_dir" ]; then
        echo "⚠️  ${label}: ${repo_dir} does not exist; skipping"
        return
    fi

    ensure_worktree_public_assets "$label" "$repo_dir"

    print_port_status "$label" "$port"
    if is_port_running "$port"; then
        return
    fi

    mkdir -p "${repo_dir}/tmp"
    local pid_file="${repo_dir}/tmp/run-app-${label}.pid"
    local log_file="${repo_dir}/tmp/run-app-${label}.log"

    echo "🌿 Starting ${label} on port ${port}..."

    (
      cd "$repo_dir" || exit 1
      load_local_env "$MAIN_REPO_DIR"
      load_local_env "$repo_dir"

      APP_WEB_PORT="$port" MAIN_REPO_DIR="$MAIN_REPO_DIR" nohup clojure \
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
        > "$log_file" 2>&1 &

      echo $! > "$pid_file"
    )

    WORKTREE_PID_FILES+=("$pid_file")

    if wait_for_port "$port" 20; then
        echo "   ✅ ${label} started on port ${port}"
    else
        echo "   ⚠️  ${label} did not bind to port ${port} yet. Check logs: ${log_file}"
    fi
}

run_worktree_app_foreground() {
    local label="$1"
    local repo_dir="$2"
    local port="$3"

    if [ ! -d "$repo_dir" ]; then
        echo "⚠️  ${label}: ${repo_dir} does not exist; skipping"
        return 1
    fi

    ensure_worktree_public_assets "$label" "$repo_dir"

    print_port_status "$label" "$port"
    if is_port_running "$port"; then
        return 0
    fi

    mkdir -p "${repo_dir}/tmp"
    local log_file="${repo_dir}/tmp/run-app-${label}.log"

    echo "🌿 Starting ${label} on port ${port}..."

    cd "$repo_dir" || return 1
    load_local_env "$MAIN_REPO_DIR"
    load_local_env "$repo_dir"

    APP_WEB_PORT="$port" MAIN_REPO_DIR="$MAIN_REPO_DIR" clojure \
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
            @(backend/main))' 2>&1 | tee "$log_file"
}

# Set terminal title
printf "\033]0;🚀 Single-Tenant Template Server\007"

# Trap to ensure cleanup on exit
trap cleanup EXIT INT TERM

if [ "$APP_SCOPE" = "all" ]; then
    echo "Checking app status for main + worktrees..."
    print_port_status "main" "$MAIN_PORT"
    print_port_status "wk1" "$WK1_PORT"
    print_port_status "wk2" "$WK2_PORT"
    echo ""
else
    if [ "$APP_SCOPE" = "single" ]; then
        echo "Checking app status for requested target (${EFFECTIVE_INSTANCE_LABEL})..."
    else
        echo "Checking app status for current folder..."
    fi
    print_port_status "$EFFECTIVE_INSTANCE_LABEL" "$EFFECTIVE_INSTANCE_PORT"
    echo ""
fi

# Load API keys / local config before starting services
load_local_env "$MAIN_REPO_DIR"
if [ "$EFFECTIVE_INSTANCE_DIR" != "$MAIN_REPO_DIR" ]; then
    load_local_env "$EFFECTIVE_INSTANCE_DIR"
fi

# Ensure local dependencies are up
echo "Bringing up Docker services..."
(
  cd "$MAIN_REPO_DIR" || exit 1
  docker compose up -d
)

# Wait for Postgres to be ready
echo "Waiting for Postgres to be ready..."
MAX_RETRIES=30
COUNT=0
until pg_isready -h localhost -p 55432 -U app_user -d single_tenant_pos > /dev/null 2>&1 || [ $COUNT -eq $MAX_RETRIES ]; do
  echo -n "."
  sleep 1
  ((COUNT++))
done

if [ $COUNT -eq $MAX_RETRIES ]; then
  echo "❌ Postgres failed to become ready in time."
  exit 1
else
  echo " ✅ Postgres is ready!"
fi

if [ "$APP_SCOPE" = "all" ]; then
    # Start PostCSS watcher before the main app
    start_postcss
    echo ""

    # Ensure shadow-cljs for worktrees (main uses `clojure -M:dev` which starts Shadow itself)
    ensure_worktree_public_assets "wk1" "$WK1_REPO_DIR"
    ensure_shadow_cljs "wk1" "$WK1_REPO_DIR"
    ensure_worktree_public_assets "wk2" "$WK2_REPO_DIR"
    ensure_shadow_cljs "wk2" "$WK2_REPO_DIR"
    echo ""

    # Ensure worktree app instances are running
    start_worktree_app "wk1" "$WK1_REPO_DIR" "$WK1_PORT"
    start_worktree_app "wk2" "$WK2_REPO_DIR" "$WK2_PORT"
    echo ""

    # If main is already running, keep the newly ensured worktree apps alive and exit.
    if is_port_running "$MAIN_PORT"; then
        echo "🟢 Main app is already running on port ${MAIN_PORT}; skipping duplicate start."
        echo "✅ Ensured worktree app startup for wk1 (${WK1_PORT}) and wk2 (${WK2_PORT})."
        CLEANUP_ON_EXIT=false
        exit 0
    fi

    # Run the app with monitoring
    stop_conflicting_main_shadow
    "${MAIN_REPO_DIR}/scripts/sh/monitoring/monitor_terminal.sh" "cd \"${MAIN_REPO_DIR}\" && clojure -M:dev"
    exit $?
fi

# Default mode: only run app for current folder.
if is_port_running "$EFFECTIVE_INSTANCE_PORT"; then
    echo "🟢 ${EFFECTIVE_INSTANCE_LABEL} is already running on port ${EFFECTIVE_INSTANCE_PORT}; skipping duplicate start."
    CLEANUP_ON_EXIT=false
    exit 0
fi

if [ "$EFFECTIVE_INSTANCE_LABEL" = "main" ]; then
    start_postcss
    echo ""
    stop_conflicting_main_shadow
    "${MAIN_REPO_DIR}/scripts/sh/monitoring/monitor_terminal.sh" "cd \"${MAIN_REPO_DIR}\" && clojure -M:dev"
    exit $?
fi

if [ "$EFFECTIVE_INSTANCE_LABEL" = "wk1" ] || [ "$EFFECTIVE_INSTANCE_LABEL" = "wk2" ]; then
    ensure_worktree_public_assets "$EFFECTIVE_INSTANCE_LABEL" "$EFFECTIVE_INSTANCE_DIR"
    ensure_shadow_cljs "$EFFECTIVE_INSTANCE_LABEL" "$EFFECTIVE_INSTANCE_DIR"

    if [ "$CURRENT_REPO_DIR" = "$EFFECTIVE_INSTANCE_DIR" ]; then
        CLEANUP_ON_EXIT=false
        run_worktree_app_foreground "$EFFECTIVE_INSTANCE_LABEL" "$EFFECTIVE_INSTANCE_DIR" "$EFFECTIVE_INSTANCE_PORT"
        exit $?
    fi

    start_worktree_app "$EFFECTIVE_INSTANCE_LABEL" "$EFFECTIVE_INSTANCE_DIR" "$EFFECTIVE_INSTANCE_PORT"

    CLEANUP_ON_EXIT=false
    exit 0
fi

echo "❌ Could not determine instance mapping for current context."
echo "   Use one of: bb run-app | bb run-app main | bb run-app wk1 | bb run-app wk2 | bb run-app all"
exit 1
