#!/bin/bash

_bb_completion() {
    local cur="${COMP_WORDS[COMP_CWORD]}"
    local prev="${COMP_WORDS[COMP_CWORD-1]}"

    # Get all available bb tasks
    local tasks="check-deps upgrade-deps single-dep-upgrade find-lib show-versions add-lib-to-deps rename-ns list-alias-deps combine-files commit cljfmt-check cljfmt-fix lint nvd-check record-test clean-cache run-app kill-java fix-docstrings fix-str-warnings be-test fe-test script stage build-prod install-completions install-clj-kondo-imports create-new-app clear-folder push backup-db restore-db clean-restore-db format-edn cerebras-chat md-to-pdf clean-db"

    if [[ ${COMP_CWORD} == 1 ]]; then
        # Complete bb tasks
        COMPREPLY=($(compgen -W "$tasks" -- "$cur"))
    elif [[ "$prev" == "script" ]]; then
        # Complete test script names
        local scripts_dir="cli-tools/test_scripts"
        if [[ -d "$scripts_dir" ]]; then
            local scripts=$(find "$scripts_dir" -name "*.sh" -type f -exec basename {} .sh \; | sort)
            COMPREPLY=($(compgen -W "$scripts" -- "$cur"))
        fi
    elif [[ "$prev" == "single-dep-upgrade" ]]; then
        # Complete library names (basic ones)
        local libs="org.clojure/clojure re-frame/re-frame metosin/reitit http-kit/http-kit"
        COMPREPLY=($(compgen -W "$libs" -- "$cur"))
    elif [[ "$prev" == "show-versions" ]]; then
        # Complete library names for version check
        local libs="org.clojure/clojure re-frame/re-frame metosin/reitit http-kit/http-kit"
        COMPREPLY=($(compgen -W "$libs" -- "$cur"))
    elif [[ "$prev" == "list-alias-deps" ]]; then
        # Complete with list or tree options
        local options="list tree"
        COMPREPLY=($(compgen -W "$options" -- "$cur"))
    elif [[ "$prev" == "commit" ]]; then
        # Complete commit options
        local options="--test"
        COMPREPLY=($(compgen -W "$options" -- "$cur"))
    fi
}

complete -F _bb_completion bb
