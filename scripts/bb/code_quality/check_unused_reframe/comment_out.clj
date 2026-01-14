(ns code-quality.check-unused-reframe.comment-out
  (:require
    [babashka.fs :as fs]))

(defn apply-comment-outs!
  "Apply-mode is intentionally disabled.

  This repository is being cleaned to remove reader-discarded code, and we
  don't want tooling that introduces new reader-discard tokens back into
  source files.

  Returns a report map with :changed and :skipped entries, but does not mutate
  any files." 
  [unused-items]
  (let [items (->> unused-items
                (filter (fn [{:keys [file]}] (and file (fs/exists? file))))
                vec)]
    {:changed []
     :skipped (mapv (fn [{:keys [file keyword type]}]
                      {:file file
                       :keyword keyword
                       :type type
                       :reason :apply-disabled})
                    items)}))

