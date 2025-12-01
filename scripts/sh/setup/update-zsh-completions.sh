#!/bin/bash

# Generate dynamic Zsh completion for bb tasks
# This script reads actual bb tasks and generates a Zsh completion file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_FILE="$SCRIPT_DIR/_bb-completion.zsh"

echo "🔄 Generating dynamic Zsh completions for bb tasks..."

# Function to extract task description from bb tasks output
extract_description() {
    local task_name="$1"
    bb tasks 2>/dev/null | grep "^$task_name " | sed "s/^$task_name *//"
}

# Check if bb is available
if ! command -v bb &> /dev/null; then
    echo "❌ Error: bb (Babashka) not found in PATH"
    exit 1
fi

# Get all available bb tasks
echo "📋 Discovering available bb tasks..."
BB_TASKS=$(bb tasks 2>/dev/null | awk '/^[a-z]/ {print $1}' | sort -u)

if [ -z "$BB_TASKS" ]; then
    echo "❌ Error: Could not retrieve bb tasks"
    exit 1
fi

echo "✅ Found $(echo "$BB_TASKS" | wc -l) bb tasks from bb.edn"

# Get all direct bb script files
echo "📂 Discovering direct bb script files..."
BB_SCRIPTS=$(find scripts/bb -name "*.clj" -exec basename {} .clj \; 2>/dev/null | sort -u)

if [ -z "$BB_SCRIPTS" ]; then
    echo "⚠️  No bb script files found"
else
    echo "✅ Found $(echo "$BB_SCRIPTS" | wc -l) bb script files"
fi

# Combine tasks and scripts
ALL_COMMANDS=$(echo -e "$BB_TASKS\n$BB_SCRIPTS" | grep -v '^$' | sort -u)
echo "📊 Total available bb commands: $(echo "$ALL_COMMANDS" | wc -l)"

# Generate the Zsh completion file
cat > "$OUTPUT_FILE" << 'EOF'
#compdef bb

# Dynamic Zsh completion for bb tasks
# Generated automatically by update-zsh-completions.sh

_bb() {
    local context state state_descr line
    typeset -A opt_args

    # Get all available commands
    local commands=(
EOF

# Add commands to the array
while IFS= read -r command; do
    if [ -n "$command" ]; then
        # Check if it's a bb.edn task or a script file
        if echo "$BB_TASKS" | grep -q "^$command$"; then
            # It's a bb.edn task - get description from bb tasks
            description=$(extract_description "$command")
        else
            # It's a script file - create a generic description
            script_path=$(find scripts/bb -name "$command.clj" 2>/dev/null | head -1)
            if [ -n "$script_path" ]; then
                # Try to extract usage info from script comments
                script_desc=$(grep "Usage:" "$script_path" 2>/dev/null | head -1 | sed 's/.*Usage: *//; s/ *$//' | cut -c1-60)
                if [ -n "$script_desc" ]; then
                    description="$script_desc"
                else
                    description="Direct bb script execution"
                fi
            else
                description="Unknown command"
            fi
        fi

        # Escape quotes and special characters for Zsh
        description=$(echo "$description" | sed 's/"/\\"/g; s/:/\\:/g; s/\[/\\[/g; s/\]/\\]/g')
        echo "        \"$command:$description\"" >> "$OUTPUT_FILE"
    fi
done <<< "$ALL_COMMANDS"

cat >> "$OUTPUT_FILE" << 'EOF'
    )

    # Get script names for bb script subcommand
    local script_names=()
    if [[ -d "cli-tools/test_scripts" ]]; then
        script_names=($(find cli-tools/test_scripts -name "*.sh" -type f -exec basename {} .sh \; 2>/dev/null | sort))
    fi

    # Main completion logic
    case $state in
        command)
            _describe 'command' commands
            ;;
        script)
            _describe 'test script' script_names
            ;;
        *)
            case $line[1] in
                script)
                    _arguments '1: :_bb_script' && ret=0
                    ;;
                *)
                    _arguments '1: :_bb_command' && ret=0
                    ;;
            esac
            ;;
    esac
}

_bb_command() {
    local commands=(
EOF

# Add commands again for subfunction
while IFS= read -r command; do
    if [ -n "$command" ]; then
        echo "        \"$command\"" >> "$OUTPUT_FILE"
    fi
done <<< "$ALL_COMMANDS"

cat >> "$OUTPUT_FILE" << 'EOF'
    )
    _describe 'command' commands
}

_bb_script() {
    local script_names=()
    if [[ -d "cli-tools/test_scripts" ]]; then
        script_names=($(find cli-tools/test_scripts -name "*.sh" -type f -exec basename {} .sh \; 2>/dev/null | sort))
    fi
    _describe 'test script' script_names
}

_bb "$@"
EOF

echo "✅ Zsh completions generated: $OUTPUT_FILE"
echo "📊 Completions created for $(echo "$ALL_COMMANDS" | wc -l) total commands"
echo "   ($(echo "$BB_TASKS" | wc -l) bb.edn tasks + $(echo "$BB_SCRIPTS" | wc -l) direct scripts)"

# Show sample of generated completions
echo ""
echo "🔍 Sample of generated completions:"
head -20 "$OUTPUT_FILE" | tail -10

echo ""
echo "💡 To install completions, run:"
echo "   cp $OUTPUT_FILE ~/.oh-my-zsh/completions/_bb"
echo "   # or run: ./install-completions.sh"
echo ""
echo "🔄 To refresh completions later, run:"
echo "   $0"
