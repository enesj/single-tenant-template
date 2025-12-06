#!/bin/bash

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

# Ensure local dependencies are up
echo "Bringing up Docker services..."
docker compose up -d

# Run the app with monitoring
./scripts/sh/monitoring/monitor_terminal.sh "clojure -M:dev:migrations-dev"
