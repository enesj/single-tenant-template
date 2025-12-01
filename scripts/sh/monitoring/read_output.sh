#!/bin/bash

# Check if we want to follow the output
FOLLOW=0
if [ "$1" = "-f" ]; then
    FOLLOW=1
fi

# Get the metadata file
META_FILE="/tmp/active_command.txt"

if [ ! -f "$META_FILE" ]; then
    echo "No active command found. Did you start the command with monitor_terminal.sh?"
    exit 1
fi

# Read the metadata without executing commands
while IFS='=' read -r key value; do
    case "$key" in
        RUN_ID) export RUN_ID="$value" ;;
        OUTPUT_FILE) export OUTPUT_FILE="$value" ;;
        PID_FILE) export PID_FILE="$value" ;;
        COMMAND) export COMMAND="$value" ;;
    esac
done < "$META_FILE"

if [ ! -f "$OUTPUT_FILE" ]; then
    echo "Output file not found: $OUTPUT_FILE"
    exit 1
fi

# Files to track state
POSITION_FILE="${OUTPUT_FILE}.pos"
LAST_OUTPUT_FILE="${OUTPUT_FILE}.last"

# Initialize position if not exists
if [ ! -f "$POSITION_FILE" ]; then
    echo "0" > "$POSITION_FILE"
fi

# Initialize last output if not exists
if [ ! -f "$LAST_OUTPUT_FILE" ]; then
    touch "$LAST_OUTPUT_FILE"
fi

function read_new_content() {
    # Get current size and last position
    CURRENT_SIZE=$(wc -c < "$OUTPUT_FILE")
    LAST_POSITION=$(cat "$POSITION_FILE")

    if [ "$CURRENT_SIZE" -gt "$LAST_POSITION" ]; then
        # There are new changes
        # Extract only the new content
        tail -c +$((LAST_POSITION + 1)) "$OUTPUT_FILE" > "$LAST_OUTPUT_FILE"
        # Update position
        echo "$CURRENT_SIZE" > "$POSITION_FILE"
        # Show new content
        cat "$LAST_OUTPUT_FILE"
    else
        # No new changes, show last output
        cat "$LAST_OUTPUT_FILE"
    fi
}

if [ $FOLLOW -eq 1 ]; then
    # Follow mode
    echo "Following output of command: $COMMAND"
    echo "Press Ctrl+C to stop"
    echo "---"

    while true; do
        read_new_content
        sleep 1
    done
else
    # One-time read
    read_new_content
fi
