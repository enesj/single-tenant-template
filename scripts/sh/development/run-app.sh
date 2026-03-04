#!/bin/bash

# Load local environment variables (optional; never committed).
# - `.env` supports KEY=VALUE lines (exported via `set -a`)
load_local_env() {
    if [ -f ".env" ]; then
        echo "🔐 Loading .env..."
        set -a
        # shellcheck disable=SC1091
        if ! source .env; then
            echo "⚠️  Failed to source .env (check syntax)"
        fi
        set +a
    fi
}

# PID file for PostCSS watcher
POSTCSS_PID_FILE="/tmp/postcss_watcher.pid"

# Function to check if app is already running
check_app_running() {
    echo "Checking if app is already running..."
    # Check if any process is using port 8085 (single-tenant template)
    local existing_pid=$(lsof -ti:8085)
    if [ ! -z "$existing_pid" ]; then
        local process_info=$(ps -p "$existing_pid" -o pid,comm,args --no-headers 2>/dev/null)
        if [ ! -z "$process_info" ]; then
            echo ""
            echo "🟢 App is already running!"
            echo "Process details: $process_info"
            echo ""
            echo "ℹ️  The development server automatically restarts after any file changes."
            echo "   You don't need to manually restart it - just edit your code and it will reload."
            echo "⚠️  Environment changes in .env are NOT applied until you fully restart the running process."
            echo ""
            exit 0
        else
            echo "🔍 Found process ID $existing_pid but could not get process details"
            echo "   Proceeding with startup..."
        fi
    else
        echo "🚀 No app found running on port 8085"
        echo "   Starting development server..."
    fi
}

# Function to cleanup any existing processes (only used when explicitly needed)
cleanup_existing() {
    echo "Checking for existing processes..."
    # Kill any existing Java processes on port 8085
    local existing_pid=$(lsof -ti:8085)
    if [ ! -z "$existing_pid" ]; then
        echo "Killing existing process on port 8085..."
        kill -9 "$existing_pid" 2>/dev/null
        sleep 1
    fi
}

# Function to start PostCSS watcher
start_postcss() {
    echo "🎨 Starting PostCSS watcher..."
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
    nohup npm run develop > /tmp/postcss_watcher.log 2>&1 &
    local postcss_pid=$!
    echo $postcss_pid > "$POSTCSS_PID_FILE"
    echo "   ✅ PostCSS watcher started (PID: $postcss_pid)"
    echo "   📄 Logs: /tmp/postcss_watcher.log"
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

# Set terminal title
printf "\033]0;🚀 Single-Tenant Template Server\007"

# Trap to ensure PostCSS is stopped on exit
trap stop_postcss EXIT INT TERM

# Check if app is already running before starting
check_app_running

# Load API keys / local config before starting services
load_local_env

# Ensure local dependencies are up
echo "Bringing up Docker services..."
docker compose up -d

# Wait for Postgres to be ready
echo "Waiting for Postgres to be ready..."
MAX_RETRIES=30
COUNT=0
until pg_isready -h localhost -p 55432 -U app_user -d multi_tenant_pos > /dev/null 2>&1 || [ $COUNT -eq $MAX_RETRIES ]; do
  echo -n "."
  sleep 1
  ((COUNT++))
done

if [ $COUNT -eq $MAX_RETRIES ]; then
  echo "❌ Postgres failed to become ready in time."
else
  echo " ✅ Postgres is ready!"
fi

# Start PostCSS watcher before the main app
start_postcss
echo ""

# Run the app with monitoring
./scripts/sh/monitoring/monitor_terminal.sh "clojure -M:dev"
