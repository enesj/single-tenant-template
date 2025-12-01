#compdef bb

# Dynamic Zsh completion for bb tasks
# Generated automatically by update-zsh-completions.sh

_bb() {
    local context state state_descr line
    typeset -A opt_args

    # Get all available commands
    local commands=(
        "add-lib-to-deps:Add a library to your project's deps.edn file. Usage\: bb add-lib-to-deps \[lib-name\]"
        "analyze_longest_files:Direct bb script execution"
        "auto_commit:Direct bb script execution"
        "backup_db:clj -M scripts/backup_db.clj \[dev|test\]\")"
        "backup-db:Create a backup of dev or test database. Usage\: bb backup-db --dev or bb backup-db --test"
        "be-test:Run backend tests using Kaocha"
        "build-prod:Build the hosting app for production deployment"
        "check-deps:Check for available dependency upgrades without making any changes"
        "clean_restore_db:clj -M scripts/clean_restore_db.clj \[dev|test\] backup_file.s"
        "clean-and-init-dev-db:Direct bb script execution"
        "clean-cache:Delete Shadow-CLJS caches and compiled output"
        "clean-db:Clean database by truncating all tables. Usage\: bb clean-db --dev or bb clean-db --test"
        "clean-restore-db:Clean restore\: Drop target database completely, then restore from backup. Usage\: bb clean-restore-db --dev backup_file.sql or bb clean-restore-db --test backup_file.sql"
        "clear_rate_limits:Direct bb script execution"
        "clear-folder:Clear temporary project folders."
        "clear-rate-limits:Clear all rate limiting data (for development and testing)."
        "cljfmt-check:Check code formatting with cljfmt"
        "cljfmt-fix:Fix code formatting with cljfmt"
        "combine-files:Combine project files into a single text file. The output file name is based on the included folders."
        "commit:Auto-commit all staged changes with a generated commit message. Use --test to run tests before commit."
        "compare_db_schemas:Direct bb script execution"
        "compare_with_models:Direct bb script execution"
        "create-new-app:Create a new Clojure application based on the hosting app template."
        "fe-test-node:Run frontend tests once using Shadow CLJS. Usage\: bb fe-test \[start\] \[lines\]"
        "file_combiner:Direct bb script execution"
        "find_snake_case_keywords:Direct bb script execution"
        "find-lib:Search for a Clojure library in public repositories. Usage\: bb find-lib \[search-term\]"
        "find-snake-case:Find snake_case keywords in Clojure files and analyze their usage patterns."
        "fix_lint_warnings:Direct bb script execution"
        "fix_misplaced_docstrings:Direct bb script execution"
        "fix_str_warnings:Direct bb script execution"
        "fix-docstrings:Automatically fix clj-kondo 'Misplaced docstring.' warnings"
        "fix-lint:Fix lint warnings automatically using clojure-lsp clean-ns and custom unused binding fixes. Use --interactive for interactive mode."
        "fix-str-warnings:Remove redundant (str ...) calls flagged by clj-kondo"
        "format_edn:bb scripts/format_edn.clj <file-path>\")"
        "format-edn:Format EDN files with pretty print. Usage\: bb format-edn <file-path>"
        "init-dev-db:Direct bb script execution"
        "install-completions:Install bb task autocompletion for your shell"
        "kill-java:Kill all Java processes"
        "lint:Run clj-kondo lint across src, dev, test folders"
        "list-alias-deps:List dependencies for specific aliases. First arg is 'list' or 'tree' (default\: tree). Usage\: bb list-alias-deps \[list|tree\] \[alias1\] \[alias2\] ..."
        "list-tenants:Direct bb script execution"
        "longest-files:Analyze and display the 10 longest Clojure files in the project."
        "md-to-pdf:Convert Markdown files to PDF using pandoc."
        "nvd-check:Check dependencies for known vulnerabilities (requires NVD API key for full functionality)"
        "pretty_tasks:Direct bb script execution"
        "rename-ns:Rename a namespace across the entire project. Usage\: bb rename-ns \[old-ns\] \[new-ns\]"
        "repair-lints:Fix clj-kondo unused binding warnings and run clojure-lsp clean-ns on affected files"
        "restore_db:clj -M scripts/restore_db.clj \[dev|test\] backup_file.sql\")"
        "restore-db:Restore a database from backup file. Usage\: bb restore-db --dev backup_file.sql or bb restore-db --test backup_file.sql"
        "rm-profile:Remove Chrome debug profiles to clean up temporary browser data."
        "run-app:Start the application server (add --clean to clear caches first)"
        "run-karma:Extract browser test failures as structured JSON data"
        "set-model:Switch between AI models for Claude CLI"
        "show-versions:Display all available versions for a specific library. Usage\: bb show-versions \[lib-name\]"
        "single-dep-upgrade:Upgrade a specific dependency to its latest version. Usage\: bb single-dep-upgrade \[dep-name\]"
        "stage:Stage all changes for git commit"
        "tasks-pretty:Display available bb tasks in a nicely formatted, colored list."
        "upgrade-deps:Automatically upgrade all project dependencies to their latest versions"
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
        "add-lib-to-deps"
        "analyze_longest_files"
        "auto_commit"
        "backup_db"
        "backup-db"
        "be-test"
        "build-prod"
        "check-deps"
        "clean_restore_db"
        "clean-and-init-dev-db"
        "clean-cache"
        "clean-db"
        "clean-restore-db"
        "clear_rate_limits"
        "clear-folder"
        "clear-rate-limits"
        "cljfmt-check"
        "cljfmt-fix"
        "combine-files"
        "commit"
        "compare_db_schemas"
        "compare_with_models"
        "create-new-app"
        "fe-test-node"
        "file_combiner"
        "find_snake_case_keywords"
        "find-lib"
        "find-snake-case"
        "fix_lint_warnings"
        "fix_misplaced_docstrings"
        "fix_str_warnings"
        "fix-docstrings"
        "fix-lint"
        "fix-str-warnings"
        "format_edn"
        "format-edn"
        "init-dev-db"
        "install-completions"
        "kill-java"
        "lint"
        "list-alias-deps"
        "list-tenants"
        "longest-files"
        "md-to-pdf"
        "nvd-check"
        "pretty_tasks"
        "rename-ns"
        "repair-lints"
        "restore_db"
        "restore-db"
        "rm-profile"
        "run-app"
        "run-karma"
        "set-model"
        "show-versions"
        "single-dep-upgrade"
        "stage"
        "tasks-pretty"
        "upgrade-deps"
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
