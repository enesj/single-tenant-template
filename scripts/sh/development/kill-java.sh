#!/bin/bash

# Kill all Java processes script
echo "Finding Java processes..."

# Find all Java processes, excluding clojure-mcp
JAVA_PIDS=$(ps aux | grep java | grep -v grep | grep -v "clojure-mcp" | awk '{print $2}')

if [ -z "$JAVA_PIDS" ]; then
    echo "No Java processes found."
    exit 0
fi

echo "Found Java processes with PIDs: $JAVA_PIDS"

# Kill each Java process
for pid in $JAVA_PIDS; do
    echo "Killing Java process with PID: $pid"
    kill -9 $pid
    if [ $? -eq 0 ]; then
        echo "Successfully killed process $pid"
    else
        echo "Failed to kill process $pid"
    fi
done

echo "All Java processes terminated."
