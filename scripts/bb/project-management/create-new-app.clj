#!/usr/bin/env bb

;; Deprecated shim.
;;
;; This file used to contain the full implementation, but its filename (with '-')
;; doesn't match the conventional namespace->filename mapping that clojure-lsp
;; enforces. The implementation now lives in `create_new_app.clj`.
;;
;; Keep this wrapper so existing invocations continue to work.

(load-file "scripts/bb/project-management/create_new_app.clj")

(let [main-fn (resolve 'create-new-app/-main)]
  (when-not main-fn
    (binding [*out* *err*]
      (println "ERROR: create-new-app/-main not found after loading create_new_app.clj"))
    (System/exit 1))
  (apply main-fn *command-line-args*))
