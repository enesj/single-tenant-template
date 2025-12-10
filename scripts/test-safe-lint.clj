#!/usr/bin/env bb

;; Test script that processes only one file
(require '[babashka.process :as process])

(defn lint-single-file [file-path]
  "Run clj-kondo on a single file and parse output"
  (let [{:keys [out err exit]} (process/sh ["clj-kondo" "--cache" "false" "--lint" file-path]
                                          {:out :string :err :string :check false})
        output (str out (when (seq err) err))
        lines (str/split-lines output)]
    (doseq [line lines]
      (when (and (str/includes? line "unused binding")
                 (str/includes? line file-path))
        (println "Found unused binding:" line)))))

;; Test on a simple file first
(let [test-file "src/app/admin/frontend/components/admin_actions.cljs"]
  (println "Testing safe lint on:" test-file)
  (lint-single-file test-file))