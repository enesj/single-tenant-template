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
        "audit_bundle:\n\""
        "audit-bundle:Create a provenance-preserving evidence bundle (ripgrep JSON) for Lattice-based audits."
        "auto_commit:Direct bb script execution"
        "backup_db:clj -M scripts/backup_db.clj \[dev|test\]\")"
        "backup-db:Create a backup of dev or test database. Usage\: bb backup-db --dev or bb backup-db --test"
        "be-test:Run backend tests using Kaocha"
        "build-prod:Build the hosting app for production deployment"
        "check_unused_reframe:Direct bb script execution"
        "check-deps:Check for available dependency upgrades without making any changes"
        "check-migrations:Check if database schema is aligned with migrations and models. Usage\: bb check-migrations \[dev|test\]"
        "clean_and_init_dev_db:Direct bb script execution"
        "clean_db:clj -M scripts/bb/database/clean-db.clj \[dev|test\]\")"
        "clean_restore_db:clj -M scripts/clean_restore_db.clj \[dev|test\] backup_file.s"
        "clean-cache:Delete Shadow-CLJS caches and compiled output"
        "clean-db:Clean database by truncating all tables. Usage\: bb clean-db --dev or bb clean-db --test"
        "clean-restore-db:Clean restore\: Drop target database completely, then restore from backup. Usage\: bb clean-restore-db --dev backup_file.sql or bb clean-restore-db --test backup_file.sql"
        "clear_rate_limits:Direct bb script execution"
        "clear-folder:Clear temporary project folders."
        "clear-rate-limits:Clear all rate limiting data (for development and testing)."
        "cljfmt-check:Check code formatting with cljfmt"
        "cljfmt-fix:Fix code formatting with cljfmt"
        "combine-files:Combine project files into a single text file. The output file name is based on the included folders."
        "comment_out:Direct bb script execution"
        "commit:Auto-commit all staged changes with a generated commit message. Use --test to run tests before commit."
        "compare_db_schemas:Direct bb script execution"
        "compare_with_models:Direct bb script execution"
        "config_audit:Direct bb script execution"
        "config-audit:Audit frontend config EDN keys for potential unused settings. Usage\: bb config-audit \[--strict\] \[--allowlist <path>\]"
        "core:Direct bb script execution"
        "create_new_app:bb create_new_app.clj <project-name> \[options\]\")"
        "create-new-app:Create a new Clojure application based on the hosting app template."
        "data:Direct bb script execution"
        "delete_articles:Direct bb script execution"
        "delete_stores:Direct bb script execution"
        "delete-articles:Delete ALL articles (and cascades) from the database. Dry-run by default. Usage\: bb delete-articles \[--dev|--test|dev|test\] \[--apply\] \[--yes\]"
        "delete-stores:Delete ALL stores from the database and unmap store_aliases. Dry-run by default. Usage\: bb delete-stores \[--dev|--test|dev|test\] \[--apply\] \[--yes\]"
        "empty_stores_suppliers_receipts:Direct bb script execution"
        "fe_test_parallel:bb scripts/bb/fe_test_parallel.clj \[options\]\""
        "fe-test:Alias for frontend tests (same as fe-test-node). Usage\: bb fe-test \[start\] \[lines\]"
        "fe-test-node:Run frontend tests once using Shadow CLJS (Node). Usage\: bb fe-test-node \[start\] \[lines\] (or bb fe-test)"
        "fe-test-parallel:Run frontend CLJS tests in parallel (Node workers, sharded). Pass through args to scripts/bb/fe_test_parallel.clj."
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
        "format:Direct bb script execution"
        "format_edn:bb scripts/format_edn.clj <file-path>\")"
        "format-edn:Format EDN files with pretty print. Usage\: bb format-edn <file-path>"
        "guard_no_concrete_domain:Direct bb script execution"
        "guard-no-concrete-domain:Fail if template/admin/shared contain concrete domain coupling. Usage\: bb guard-no-concrete-domain \[--strict\]"
        "install-completions:Install bb task autocompletion for your shell"
        "iterm-rename:Rename terminal session. Usage\: bb iterm-rename \[session-name\]"
        "kill-java:Kill all Java processes"
        "legacy-audit:Fail if new legacy occurrences are introduced compared to resources/legacy-inventory.edn. Use --strict to require zero occurrences."
        "legacy-inventory:Generate a machine-readable inventory of legacy patterns. Writes to resources/legacy-inventory.edn."
        "lint:Run clj-kondo lint across src, dev, test folders"
        "list_unmapped_article_aliases:Direct bb script execution"
        "list-alias-deps:List dependencies for specific aliases. First arg is 'list' or 'tree' (default\: tree). Usage\: bb list-alias-deps \[list|tree\] \[alias1\] \[alias2\] ..."
        "longest-files:Analyze and display the 10 longest Clojure files in the project."
        "md_to_pdf:bb scripts/bb/project-management/md-to-pdf.clj <input.md> \[o"
        "md-to-pdf:Convert Markdown files to PDF using pandoc."
        "migrate_and_sync_frontend_config:Direct bb script execution"
        "migrate-and-sync-frontend-config:Run migrations, apply frontend config sync, then validate. Usage\: bb migrate-and-sync-frontend-config \[--profile <profile>\] \[--only <domain>\] \[--skip <domain>\] \[--allowlist <path>\] \[--schema <path>\]"
        "nvd-check:Check dependencies for known vulnerabilities (requires NVD API key for full functionality)"
        "pretty_tasks:Direct bb script execution"
        "receipt-ocr-worker:Process pending receipts through OCR (one-shot by default). Usage\: bb receipt-ocr-worker \[dev|test\] \[--max-receipts N\] \[--loop\]"
        "refactor:Direct bb script execution"
        "rename-ns:Rename a namespace across the entire project. Usage\: bb rename-ns \[old-ns\] \[new-ns\]"
        "repair-lints:Fix clj-kondo unused binding warnings and run clojure-lsp clean-ns on affected files"
        "reset-session:Reset terminal session name to default"
        "restore_db:Direct bb script execution"
        "restore_db_legacy:clj -M scripts/bb/database/restore_db_legacy.clj \[dev|test\] "
        "restore_db_script:clj -M scripts/bb/database/restore-db.clj \[dev|test\] backup_"
        "restore-db:Restore a database from backup file. Usage\: bb restore-db --dev backup_file.sql or bb restore-db --test backup_file.sql"
        "rm-profile:Remove Chrome debug profiles to clean up temporary browser data."
        "run-app:Start the application server (add --clean to clear caches first)"
        "run-karma:Extract browser test failures as structured JSON data"
        "search:Direct bb script execution"
        "search_article_products:Direct bb script execution"
        "seed_admin:Direct bb script execution"
        "seed-admin:Seed or refresh the admin user (default env\: dev). Usage\: bb seed-admin \[dev|test\]"
        "serper_search:Direct bb script execution"
        "serper-search:Run a Serper.dev (Google SERP) API query and print results."
        "set-model:Switch between AI models for Claude CLI"
        "set-session-name:Internal helper to set session name with emoji support"
        "show-versions:Display all available versions for a specific library. Usage\: bb show-versions \[lib-name\]"
        "single-dep-upgrade:Upgrade a specific dependency to its latest version. Usage\: bb single-dep-upgrade \[dep-name\]"
        "spellcheck_article_canonical_names:\")"
        "spellcheck-article-names:Spellcheck `articles.canonical_name` using the JSpell Checker MCP server (RapidAPI via mcp-remote) and write suggestions to an EDN file in the project root."
        "stage:Stage all changes for git commit"
        "sync_frontend_config:Direct bb script execution"
        "sync-frontend-config:Sync frontend config EDNs with DB schema (dry-run by default). Usage\: bb sync-frontend-config \[--apply\] \[--only <domain>\] \[--skip <domain>\] \[--allowlist <path>\] \[--schema <path>\]"
        "tasks-pretty:Display available bb tasks in a nicely formatted, colored list."
        "upgrade-deps:Automatically upgrade all project dependencies to their latest versions"
        "validate_frontend_config:Direct bb script execution"
        "validate-frontend-config:Validate frontend config EDN files against Malli specs + DB schema alignment. Usage\: bb validate-frontend-config \[--only <domain>\] \[--skip <domain>\] \[--allowlist <path>\] \[--schema <path>\]"
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
        "audit_bundle"
        "audit-bundle"
        "auto_commit"
        "backup_db"
        "backup-db"
        "be-test"
        "build-prod"
        "check_unused_reframe"
        "check-deps"
        "check-migrations"
        "clean_and_init_dev_db"
        "clean_db"
        "clean_restore_db"
        "clean-cache"
        "clean-db"
        "clean-restore-db"
        "clear_rate_limits"
        "clear-folder"
        "clear-rate-limits"
        "cljfmt-check"
        "cljfmt-fix"
        "combine-files"
        "comment_out"
        "commit"
        "compare_db_schemas"
        "compare_with_models"
        "config_audit"
        "config-audit"
        "core"
        "create_new_app"
        "create-new-app"
        "data"
        "delete_articles"
        "delete_stores"
        "delete-articles"
        "delete-stores"
        "empty_stores_suppliers_receipts"
        "fe_test_parallel"
        "fe-test"
        "fe-test-node"
        "fe-test-parallel"
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
        "format"
        "format_edn"
        "format-edn"
        "guard_no_concrete_domain"
        "guard-no-concrete-domain"
        "install-completions"
        "iterm-rename"
        "kill-java"
        "legacy-audit"
        "legacy-inventory"
        "lint"
        "list_unmapped_article_aliases"
        "list-alias-deps"
        "longest-files"
        "md_to_pdf"
        "md-to-pdf"
        "migrate_and_sync_frontend_config"
        "migrate-and-sync-frontend-config"
        "nvd-check"
        "pretty_tasks"
        "receipt-ocr-worker"
        "refactor"
        "rename-ns"
        "repair-lints"
        "reset-session"
        "restore_db"
        "restore_db_legacy"
        "restore_db_script"
        "restore-db"
        "rm-profile"
        "run-app"
        "run-karma"
        "search"
        "search_article_products"
        "seed_admin"
        "seed-admin"
        "serper_search"
        "serper-search"
        "set-model"
        "set-session-name"
        "show-versions"
        "single-dep-upgrade"
        "spellcheck_article_canonical_names"
        "spellcheck-article-names"
        "stage"
        "sync_frontend_config"
        "sync-frontend-config"
        "tasks-pretty"
        "upgrade-deps"
        "validate_frontend_config"
        "validate-frontend-config"
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
