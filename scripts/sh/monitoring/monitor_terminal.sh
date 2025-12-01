#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 'command'"
    echo "Example: $0 'ls -la | grep \"file\"'"
    exit 1
fi

# Create unique identifiers for this run
RUN_ID=$(date +%s)
OUTPUT_FILE="/tmp/command_output_${RUN_ID}.log"
PID_FILE="/tmp/command_pid_${RUN_ID}.txt"
META_FILE="/tmp/command_meta_${RUN_ID}.txt"

# Function to cleanup processes and files
cleanup() {
    local pid_to_kill=$(cat "$PID_FILE" 2>/dev/null)
    if [ ! -z "$pid_to_kill" ]; then
        echo "Cleaning up processes..."
        # Kill the main process and its children
        pkill -P "$pid_to_kill" 2>/dev/null
        kill -9 "$pid_to_kill" 2>/dev/null

        # Find and kill any Java processes using port 8080
#        local java_pid=$(lsof -ti:8080)
#        if [ ! -z "$java_pid" ]; then
#            echo "Killing Java process on port 8080..."
#            kill -9 "$java_pid" 2>/dev/null
#        fi
    fi

    # Remove temp files
    rm -f "$OUTPUT_FILE" "$PID_FILE" "$META_FILE" "/tmp/active_command.txt" "${OUTPUT_FILE}.pos" "${OUTPUT_FILE}.last"
    exit 0
}

# Set up trap for Ctrl+C and other termination signals
trap cleanup SIGINT SIGTERM EXIT

# Save metadata for the reader script
echo "RUN_ID=$RUN_ID" > "$META_FILE"
echo "OUTPUT_FILE=$OUTPUT_FILE" >> "$META_FILE"
echo "PID_FILE=$PID_FILE" >> "$META_FILE"
echo "COMMAND=$1" >> "$META_FILE"

# Make this the active command
ln -sf "$META_FILE" "/tmp/active_command.txt"

# Set terminal title
printf "\033]0;🚀 Hosting App Server\007"

# Run the command and capture output
echo "Starting command: $1"
echo "Output will be captured to: $OUTPUT_FILE"
echo "To read the output, use: ./scripts/sh/monitoring/read_output.sh"
echo "Press Ctrl+C to stop"

# Start the command with title monitoring integrated
(
  # Set title initially and let it be
  echo -ne "\033]0;🚀 Hosting App Server\007"

  # Run the main command
  eval "$1" 2>&1 | tee $OUTPUT_FILE
) &
COMMAND_PID=$!
echo $COMMAND_PID > "$PID_FILE"

# Wait for the command to finish or be interrupted
wait $COMMAND_PID
