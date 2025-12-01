#!/bin/bash

# Generate Fish completions for bb tasks with descriptions
# This script reads actual bb tasks and generates a Fish completion file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_FILE="$SCRIPT_DIR/bb-completion.fish"

echo "🔄 Generating dynamic Fish completions for bb tasks..."

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

# Generate the Fish completion file
cat > "$OUTPUT_FILE" << 'EOF'
#!/usr/bin/env fish

# Dynamic Fish completion for bb tasks
# Generated automatically by update-fish-completions.sh
# Last updated: $(date)

function __bb_script_completions
    set -l scripts_dir "cli-tools/test_scripts"
    if test -d $scripts_dir
        for script in $scripts_dir/*.sh
            basename $script .sh
        end
    end
end

function __bb_lib_completions
    echo "org.clojure/clojure"
    echo "re-frame/re-frame"
    echo "metosin/reitit"
    echo "http-kit/http-kit"
    echo "metosin/malli"
    echo "thheller/shadow-cljs"
end

# Generate completions for each bb task
EOF

# Add completions for each task
echo "# Complete bb tasks and scripts" >> "$OUTPUT_FILE"
while IFS= read -r command; do
    if [ -n "$command" ]; then
        # Check if it's a bb.edn task or a script file
        if echo "$BB_TASKS" | grep -q "^$command$"; then
            # It's a bb.edn task - get description from bb tasks
            description=$(extract_description "$command")
            type_indicator="task"
        else
            # It's a script file - create a generic description
            script_path=$(find scripts/bb -name "$command.clj" 2>/dev/null | head -1)
            if [ -n "$script_path" ]; then
                # Try to extract usage info from script comments
                script_desc=$(grep "Usage:" "$script_path" 2>/dev/null | head -1 | sed 's/.*Usage: *//; s/ *$//' | cut -c1-80)
                if [ -n "$script_desc" ]; then
                    description="$script_desc"
                else
                    description="Direct bb script execution"
                fi
                type_indicator="script"
            else
                description="Unknown command"
                type_indicator="unknown"
            fi
        fi

        # Escape single quotes in description for Fish
        description=$(echo "$description" | sed "s/'/'\\\\''/g")
        echo "complete -c bb -f -a '$command' -d '$description [$type_indicator]'" >> "$OUTPUT_FILE"
    fi
done <<< "$ALL_COMMANDS"

# Add subcommand completions
cat >> "$OUTPUT_FILE" << 'EOF'

# Complete script names for 'bb script'
complete -c bb -n '__fish_seen_subcommand_from script' -a '(__bb_script_completions)' -d 'Available test scripts'

# Complete library names for dependency commands
complete -c bb -n '__fish_seen_subcommand_from single-dep-upgrade' -a '(__bb_lib_completions)' -d 'Library to upgrade'
complete -c bb -n '__fish_seen_subcommand_from show-versions' -a '(__bb_lib_completions)' -d 'Library to check'
complete -c bb -n '__fish_seen_subcommand_from add-lib-to-deps' -a '(__bb_lib_completions)' -d 'Library to add'

# Complete options for specific commands
complete -c bb -n '__fish_seen_subcommand_from commit' -l test -d 'Run tests before commit'
complete -c bb -n '__fish_seen_subcommand_from list-alias-deps' -a 'list tree' -d 'Output format'
complete -c bb -n '__fish_seen_subcommand_from backup-db' -l dev test -d 'Database environment'
complete -c bb -n '__fish_seen_subcommand_from restore-db' -l dev test -d 'Database environment'
complete -c bb -n '__fish_seen_subcommand_from clean-restore-db' -l dev test -d 'Database environment'
complete -c bb -n '__fish_seen_subcommand_from clean-db' -l dev test -d 'Database environment'
complete -c bb -n '__fish_seen_subcommand_from fix-lint' -l interactive -d 'Interactive mode'
complete -c bb -n '__fish_seen_subcommand_from fe-test-node' -a start -d 'Start test server'
complete -c bb -n '__fish_seen_subcommand_from format-edn' -l indent width -d 'Formatting options'
complete -c bb -n '__fish_seen_subcommand_from md-to-pdf' -l output template -d 'PDF options'
complete -c bb -n '__fish_seen_subcommand_from set-model' -l list default -d 'Model options'
complete -c bb -n '__fish_seen_subcommand_from clear-folder' -l all tmp backups -d 'Folder options'
complete -c bb -n '__fish_seen_subcommand_from find-lib' -l limit source -d 'Search options'
complete -c bb -n '__fish_seen_subcommand_from show-versions' -l latest stable limit -d 'Version options'
EOF

echo "✅ Fish completions generated: $OUTPUT_FILE"
echo "📊 Completions created for $(echo "$ALL_COMMANDS" | wc -l) total commands"
echo "   ($(echo "$BB_TASKS" | wc -l) bb.edn tasks + $(echo "$BB_SCRIPTS" | wc -l) direct scripts)"

# Show sample of generated tasks
echo ""
echo "🔍 Sample of generated completions:"
head -10 "$OUTPUT_FILE" | tail -8

echo ""
echo "💡 To install completions, run:"
echo "   fish -c \"source $OUTPUT_FILE\""
echo "   or run: ./install-completions.sh"
