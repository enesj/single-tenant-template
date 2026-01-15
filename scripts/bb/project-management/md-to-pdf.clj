#!/usr/bin/env bb

;; Deprecated shim.
;;
;; This file used to contain the full implementation, but its filename (with '-')
;; doesn't match the conventional namespace->filename mapping that clojure-lsp
;; enforces. The implementation now lives in `md_to_pdf.clj`.
;;
;; Keep this wrapper so existing invocations continue to work.

(load-file "scripts/bb/project-management/md_to_pdf.clj")

(let [main-fn (resolve 'md-to-pdf/-main)]
  (when-not main-fn
    (binding [*out* *err*]
      (println "ERROR: md-to-pdf/-main not found after loading md_to_pdf.clj"))
    (System/exit 1))
  (apply main-fn *command-line-args*))
