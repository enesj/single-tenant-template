#!/usr/bin/env bb
;; Babashka helper to delete files that PATCH deletion can't touch.
;;
;; Preferred usage:
;;   bb scripts/bb/delete_files.bb path/to/file1 path/to/file2
;;   bb scripts/bb/delete_files.bb --dry-run path1 path2
;;
(ns scripts.bb.delete-files
  (:require
    [babashka.fs :as fs]
    [clojure.string :as str]))

(defn- usage []
  (println "Usage: bb scripts/bb/delete_files.bb [--dry-run] [--yes] path1 path2 ...")
  (System/exit 1))

(defn- prompt-delete [paths]
  (println "About to delete:" (str/join ", " paths))
  (print "Press ENTER to continue, ctrl+c to abort: ")
  (flush)
  (read-line))

(defn -main
  [& args]
  (let [{:keys [dry-run paths yes] :or {dry-run false yes false}}
        (loop [left args
               opts {:dry-run false :yes false :paths []}]
          (if (empty? left)
            opts
            (let [[curr & rest] left]
              (case curr
                "--dry-run" (recur rest (assoc opts :dry-run true))
                "--yes" (recur rest (assoc opts :yes true))
                (recur rest (update opts :paths conj curr))))))]

    (when (empty? paths)
      (usage))

    (doseq [path paths]
      (when-not (fs/exists? path)
        (println "Skipping missing path:" path)
        (System/exit 1)))

    (when-not yes
      (prompt-delete paths))

    (doseq [path paths]
      (if dry-run
        (println "Dry run: would delete" path)
        (do
          (fs/delete path)
          (println "Deleted" path))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
