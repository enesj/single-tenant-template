#!/bin/bash

# Load local environment variables (optional; never committed).
# - `.env` supports KEY=VALUE lines (exported via `set -a`)
# - `.api_credentials.sh` is a regular shell script with exports
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

    if [ -f ".api_credentials.sh" ]; then
        echo "🔐 Loading .api_credentials.sh..."
        # shellcheck disable=SC1091
        if ! source .api_credentials.sh; then
            echo "⚠️  Failed to source .api_credentials.sh"
        fi
    fi
}

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

# Set terminal title
printf "\033]0;🚀 Single-Tenant Template Server\007"

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
until pg_isready -h localhost -p 55432 -U app_user -d single_tenant_pos > /dev/null 2>&1 || [ $COUNT -eq $MAX_RETRIES ]; do
  echo -n "."
  sleep 1
  ((COUNT++))
done

if [ $COUNT -eq $MAX_RETRIES ]; then
  echo "❌ Postgres failed to become ready in time."
else
  echo " ✅ Postgres is ready!"
fi

# Run the app with monitoring
./scripts/sh/monitoring/monitor_terminal.sh "clojure -M:dev"
