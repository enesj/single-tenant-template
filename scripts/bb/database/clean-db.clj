#!/usr/bin/env clj

;; Deprecated shim.
;;
;; This file used to contain the full implementation, but its filename (with '-')
;; doesn't match the conventional namespace->filename mapping that clojure-lsp
;; enforces. The implementation now lives in `clean_db.clj`.
;;
;; Keep this wrapper so existing invocations continue to work.

(load-file "scripts/bb/database/clean_db.clj")

(let [main-fn (resolve 'clean-db/-main)]
  (when-not main-fn
    (binding [*out* *err*]
      (println "ERROR: clean-db/-main not found after loading clean_db.clj"))
    (System/exit 1))
  (apply main-fn *command-line-args*))
