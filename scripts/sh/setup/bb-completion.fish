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
# Complete bb tasks and scripts
complete -c bb -f -a 'add-lib-to-deps' -d 'Add a library to your project'\''s deps.edn file. Usage: bb add-lib-to-deps [lib-name] [task]'
complete -c bb -f -a 'analyze_longest_files' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'auto_commit' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'backup_db' -d 'clj -M scripts/backup_db.clj [dev|test]") [script]'
complete -c bb -f -a 'bb_chat' -d 'bb cerebras-chat.clj [options] [message]") [script]'
complete -c bb -f -a 'be-test' -d 'Run backend tests using Kaocha [task]'
complete -c bb -f -a 'build-prod' -d 'Build the hosting app for production deployment [task]'
complete -c bb -f -a 'check-deps' -d 'Check for available dependency upgrades without making any changes [task]'
complete -c bb -f -a 'clean_restore_db' -d 'clj -M scripts/clean_restore_db.clj [dev|test] backup_file.sql") [script]'
complete -c bb -f -a 'clean-and-init-dev-db' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'clean-cache' -d 'Delete Shadow-CLJS caches and compiled output [task]'
complete -c bb -f -a 'clean-db' -d 'clj -M scripts/clean-db.clj [dev|test]") [script]'
complete -c bb -f -a 'clear_rate_limits' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'cljfmt-check' -d 'Check code formatting with cljfmt [task]'
complete -c bb -f -a 'cljfmt-fix' -d 'Fix code formatting with cljfmt [task]'
complete -c bb -f -a 'combine-files' -d 'Combine project files into a single text file. The output file name is based on the included folders. [task]'
complete -c bb -f -a 'commit' -d 'Auto-commit all staged changes with a generated commit message. Use --test to run tests before commit. [task]'
complete -c bb -f -a 'compare_db_schemas' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'compare_with_models' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'create-new-app' -d 'bb create-new-app.clj <project-name> [options]") [script]'
complete -c bb -f -a 'extract-test-failures' -d 'Extract browser test failures as structured JSON data [task]'
complete -c bb -f -a 'fe-test-node' -d 'Run frontend tests once using Shadow CLJS. Usage: bb fe-test [start] [lines] [task]'
complete -c bb -f -a 'file_combiner' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'find_snake_case_keywords' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'find-lib' -d 'Search for a Clojure library in public repositories. Usage: bb find-lib [search-term] [task]'
complete -c bb -f -a 'fix_lint_warnings' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'fix_misplaced_docstrings' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'fix_str_warnings' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'fix-docstrings' -d 'Automatically fix clj-kondo '\''Misplaced docstring.'\'' warnings [task]'
complete -c bb -f -a 'fix-str-warnings' -d 'Remove redundant (str ...) calls flagged by clj-kondo [task]'
complete -c bb -f -a 'format_edn' -d 'bb scripts/format_edn.clj <file-path>") [script]'
complete -c bb -f -a 'init-dev-db' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'install-completions' -d 'Install bb task autocompletion for your shell (generates dynamic Fish completions from actual bb tasks) [task]'
complete -c bb -f -a 'kill-java' -d 'Kill all Java processes [task]'
complete -c bb -f -a 'lint' -d 'Run clj-kondo lint across src, dev, test and cli-tools folders [task]'
complete -c bb -f -a 'list-alias-deps' -d 'List dependencies for specific aliases. First arg is '\''list'\'' or '\''tree'\'' (default: tree). Usage: bb list-alias-deps [list|tree] [alias1] [alias2] ... [task]'
complete -c bb -f -a 'list-tenants' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'md-to-pdf' -d 'bb md-to-pdf.clj <input.md> [output.pdf]") [script]'
complete -c bb -f -a 'nvd-check' -d 'Check dependencies for known vulnerabilities (requires NVD API key for full functionality) [task]'
complete -c bb -f -a 'pretty_tasks' -d 'Direct bb script execution [script]'
complete -c bb -f -a 'record-test' -d 'Record browser test session on localhost:8080/entities [task]'
complete -c bb -f -a 'rename-ns' -d 'Rename a namespace across the entire project. Usage: bb rename-ns [old-ns] [new-ns] [task]'
complete -c bb -f -a 'restore_db' -d 'clj -M scripts/restore_db.clj [dev|test] backup_file.sql") [script]'
complete -c bb -f -a 'restore-db' -d 'clj -M scripts/restore-db.clj [dev|test] backup_file.sql") [script]'
complete -c bb -f -a 'run-app' -d 'Start the application server (add --clean to clear caches first) [task]'
complete -c bb -f -a 'script' -d 'Run test scripts from cli-tools/test_scripts/. Usage: bb script [script-name-without-.sh] [task]'
complete -c bb -f -a 'show-versions' -d 'Display all available versions for a specific library. Usage: bb show-versions [lib-name] [task]'
complete -c bb -f -a 'single-dep-upgrade' -d 'Upgrade a specific dependency to its latest version. Usage: bb single-dep-upgrade [dep-name] [task]'
complete -c bb -f -a 'stage' -d 'Stage all changes for git commit [task]'
complete -c bb -f -a 'upgrade-deps' -d 'Automatically upgrade all project dependencies to their latest versions [task]'

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
