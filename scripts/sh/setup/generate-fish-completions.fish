#!/usr/bin/env fish

# Dynamic Fish completion generator for bb tasks
# This script generates completions based on actual available bb tasks

function __bb_task_completions
    # Get actual bb tasks dynamically
    bb tasks 2>/dev/null | awk '/^[a-z]/ {print $1}' | sort -u
end

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

# Main completion function
function __bb_complete
    set -l cmd (commandline -opc)
    set -l current (commandline -ct)

    # If no command yet, complete with available tasks
    if test (count $cmd) -eq 1
        __bb_task_completions
        return
    end

    # Handle specific subcommands
    switch $cmd[2]
        case script
            if test (count $cmd) -eq 2
                __bb_script_completions
            end
        case single-dep-upgrade show-versions add-lib-to-deps
            if test (count $cmd) -eq 2
                __bb_lib_completions
            end
        case list-alias-deps
            if test (count $cmd) -eq 2
                echo "list"
                echo "tree"
            end
        case commit
            echo "--test"
            echo "-h"
            echo "--help"
        case backup-db restore-db clean-restore-db
            echo "--dev"
            echo "--test"
            echo "-h"
            echo "--help"
        case clear-folder
            echo "--all"
            echo "--tmp"
            echo "--backups"
            echo "-h"
            echo "--help"
        case fix-lint
            echo "--interactive"
            echo "--dry-run"
            echo "-h"
            echo "--help"
        case create-new-app
            echo "--template"
            echo "--name"
            echo "--description"
            echo "-h"
            echo "--help"
        case fe-test-node
            echo "start"
            echo "-h"
            echo "--help"
        case format-edn
            echo "--indent"
            echo "--width"
            echo "-h"
            echo "--help"
        case md-to-pdf
            echo "--output"
            echo "--template"
            echo "-h"
            echo "--help"
        case clean-db
            echo "--dev"
            echo "--test"
            echo "--force"
            echo "-h"
            echo "--help"
        case find-lib
            echo "--limit"
            echo "--source"
            echo "-h"
            echo "--help"
        case show-versions
            echo "--latest"
            echo "--stable"
            echo "--limit"
            echo "-h"
            echo "--help"
        case set-model
            echo "--list"
            echo "--default"
            echo "-h"
            echo "--help"
        case "*"
            # Generic help options for all commands
            echo "-h"
            echo "--help"
    end
end

# Register the completion
complete -c bb -f -a '(__bb_complete)' -d 'Babashka task runner'

# Complete script names for 'bb script'
complete -c bb -n '__fish_seen_subcommand_from script' -a '(__bb_script_completions)' -d 'Available test scripts'

# Complete library names for dependency commands
complete -c bb -n '__fish_seen_subcommand_from single-dep-upgrade' -a '(__bb_lib_completions)' -d 'Library to upgrade'
complete -c bb -n '__fish_seen_subcommand_from show-versions' -a '(__bb_lib_completions)' -d 'Library to check'
complete -c bb -n '__fish_seen_subcommand_from add-lib-to-deps' -a '(__bb_lib_completions)' -d 'Library to add'
