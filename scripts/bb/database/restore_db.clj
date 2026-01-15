#!/usr/bin/env clj

;; Deprecated shim.
;;
;; The implementation has moved to `restore_db_legacy.clj` so that the filename
;; matches the declared namespace (as required by clojure-lsp). Keep this file
;; as a compatibility wrapper.

(load-file "scripts/bb/database/restore_db_legacy.clj")

(let [main-fn (resolve 'restore-db-legacy/-main)]
  (when-not main-fn
    (binding [*out* *err*]
      (println "ERROR: restore-db-legacy/-main not found after loading restore_db_legacy.clj"))
    (System/exit 1))
  (apply main-fn *command-line-args*))
