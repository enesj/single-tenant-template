#!/usr/bin/env bb
;; Backwards-compatible wrapper.
;;
;; This script used to contain the implementation, but Clojure tooling expects
;; hyphens in namespaces to map to underscores in filenames.
;;
;; Prefer running:
;;   bb scripts/bb/delete_files.bb ...

(let [this-file (or (System/getProperty "babashka.file") *file*)
      dir (.getParent (java.io.File. this-file))
      impl (str dir "/delete_files.bb")]
  (load-file impl)
    (if-let [main-fn (resolve 'scripts.bb.delete-files/-main)]
        (apply main-fn *command-line-args*)
        (do
            (binding [*out* *err*]
                (println "Failed to resolve scripts.bb.delete-files/-main after load-file" {:impl impl}))
            (System/exit 2))))