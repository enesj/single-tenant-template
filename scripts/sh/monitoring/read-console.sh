#!/bin/bash

# Script to read the running dev browser console
# Usage: ./scripts/read-console.sh [options]
# Options:
#   --follow, -f     Follow console output (continuous monitoring)
#   --clear, -c      Clear console before reading
#   --errors-only    Show only error messages
#   --debug, -d      Show debug output
#   --help, -h       Show this help message

set -e

# Default options
FOLLOW=false
CLEAR=false
ERRORS_ONLY=false
DEBUG=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --follow|-f)
            FOLLOW=true
            shift
            ;;
        --clear|-c)
            CLEAR=true
            shift
            ;;
        --errors-only)
            ERRORS_ONLY=true
            shift
            ;;
        --debug|-d)
            DEBUG=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --follow, -f     Follow console output (continuous monitoring)"
            echo "  --clear, -c      Clear console before reading"
            echo "  --errors-only    Show only error messages"
            echo "  --debug, -d      Show debug output"
            echo "  --help, -h       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

# Check if development server is running
if ! curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "❌ Development server is not running on http://localhost:8080"
    echo "💡 Start the dev server with: ./scripts/sh/development/run-app.sh"
    exit 1
fi

# Build the Clojure command
CLJ_OPTS=""
if [ "$CLEAR" = true ]; then
    CLJ_OPTS="$CLJ_OPTS --clear"
fi

if [ "$ERRORS_ONLY" = true ]; then
    CLJ_OPTS="$CLJ_OPTS --errors-only"
fi

if [ "$FOLLOW" = true ]; then
    CLJ_OPTS="$CLJ_OPTS --follow"
fi

echo "🔍 Reading browser console logs..."
echo "📱 Connecting to http://localhost:8080"
echo ""

# Run the console reader with Babashka
if [ "$DEBUG" = true ]; then
    bb -v -m fe-tools.console-reader $CLJ_OPTS
else
    bb -m fe-tools.console-reader $CLJ_OPTS
fi
