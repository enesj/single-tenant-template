#!/usr/bin/env clj

;; Deprecated shim.
;;
;; This file used to contain the full implementation, but its filename (with '-')
;; doesn't match the conventional namespace->filename mapping that clojure-lsp
;; enforces. The implementation now lives in `restore_db_script.clj`.
;;
;; Keep this wrapper so existing invocations continue to work.

(load-file "scripts/bb/database/restore_db_script.clj")

(let [main-fn (resolve 'restore-db-script/-main)]
  (when-not main-fn
    (binding [*out* *err*]
      (println "ERROR: restore-db-script/-main not found after loading restore_db_script.clj"))
    (System/exit 1))
  (apply main-fn *command-line-args*))
