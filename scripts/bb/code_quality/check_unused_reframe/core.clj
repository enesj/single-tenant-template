(ns code-quality.check-unused-reframe.core
  (:require
    [code-quality.check-unused-reframe.comment-out :as comment-out]
    [code-quality.check-unused-reframe.data :as data]
    [code-quality.check-unused-reframe.format :as format]
    [code-quality.check-unused-reframe.search :as search]))

(defn analyze-keyword
  "Analyze a single keyword for usage"
  [keyword-info]
  (let [usages (search/search-codebase keyword-info)
        dynamic-admin-subs (data/admin-dynamic-subscription-keyword-strings)
        dynamic? (and (= (:type keyword-info) :subscription)
                   (contains? dynamic-admin-subs (:keyword keyword-info)))
        def-file (:file keyword-info)
        matches-in-def-file (->> usages
                              (filter (fn [u]
                                        ;; Normalize paths for comparison: keyword-info has src/... style.
                                        (= (:file u) def-file)))
                              vec)
        external-usages (->> usages
                         (remove (fn [u] (= (:file u) def-file)))
                         vec)]
    (assoc keyword-info
      :usages (vec usages)
      :external-usages external-usages
      :definition-usages matches-in-def-file
      :used-dynamically? dynamic?
      :used? (or dynamic? (boolean (seq external-usages))))))

(defn -main
  [& args]
  (let [verbose? (some #{"--verbose" "-v"} args)
        apply? (some #{"--apply" "--fix"} args)
        results (mapv analyze-keyword data/unused-keywords)
        formatted (format/format-results results)]

    (if verbose?
      (format/print-results formatted)
      (do
        (println "\n=== Re-frame Unused Keyword Analysis ===\n")
        (println (format "Total keywords analyzed: %d" (get-in formatted [:summary :total])))
        (println (format "Actually used: %d" (get-in formatted [:summary :used])))
        (println (format "Used dynamically (heuristic): %d" (get-in formatted [:summary :used-dynamically])))
        (println (format "Truly unused: %d" (get-in formatted [:summary :unused])))

        (println "\n--- TRULY UNUSED (grouped by file) ---")
        (doseq [[file items] (format/group-by-file (:unused formatted))]
          (println (format "\n%s:" file))
          (doseq [item items]
            (println (format "  - %s (%s)" (:keyword item) (name (:type item))))))

        (println "\n--- ACTUALLY USED (summary) ---")
        (doseq [item (:used formatted)]
          (println (format "  ✓ %s - used in %d files"
                     (:keyword item)
                     (count (:usages item)))))))

    (when apply?
      (let [unused-items (:unused formatted)
            report (comment-out/apply-comment-outs! unused-items)
            changed (:changed report)
            skipped (:skipped report)]
        (println "\n--- APPLY MODE ---")
        (println (format "Commented out: %d registrations" (count changed)))
        (println (format "Skipped: %d registrations" (count skipped)))
        (when (seq skipped)
          (println "\nSkipped details (first 25):")
          (doseq [s (take 25 skipped)]
            (println (format "  - %s (%s) in %s" (:keyword s) (name (:reason s)) (:file s)))))))

    ;; Return exit code based on results
    (System/exit 0)))
