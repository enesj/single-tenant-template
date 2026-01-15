(ns code-quality.check-unused-reframe.format)

(defn format-results
  "Format results for display."
  [results]
  (let [unused (filter (complement :used?) results)
        used (filter :used? results)
        dynamic (filter :used-dynamically? results)]
    {:summary {:total (count results)
               :used (count used)
               :used-dynamically (count dynamic)
               :unused (count unused)}
     :used (map #(select-keys % [:keyword :file :type :usages :used-dynamically?]) used)
     :unused (map #(select-keys % [:keyword :file :type]) unused)}))

(defn print-results
  "Print results to console."
  [formatted]
  (println "\n=== Re-frame Unused Keyword Analysis ===\n")
  (println (format "Total keywords analyzed: %d" (get-in formatted [:summary :total])))
  (println (format "Actually used: %d" (get-in formatted [:summary :used])))
  (println (format "Used dynamically (heuristic): %d" (get-in formatted [:summary :used-dynamically])))
  (println (format "Truly unused: %d" (get-in formatted [:summary :unused])))
  
  (println "\n--- USED KEYWORDS (can keep) ---")
  (doseq [item (:used formatted)]
    (println (format "\n✓ %s (%s)" (:keyword item) (:type item)))
    (println (format "  Defined in: %s" (:file item)))
    (println "  Used in:")
    (doseq [usage (:usages item)]
      (println (format "    - %s" (:file usage)))
      (doseq [match (:matches usage)]
        (println (format "      Line %d: %s" 
                        (:line match) 
                        (subs (:content match) 0 (min 80 (count (:content match)))))))))
  
  (println "\n\n--- UNUSED KEYWORDS (candidates for removal) ---")
  (doseq [item (:unused formatted)]
    (println (format "\n✗ %s (%s)" (:keyword item) (:type item)))
    (println (format "  Defined in: %s" (:file item)))))

(defn group-by-file
  "Group unused items by their definition file."
  [unused-items]
  (->> unused-items
       (group-by :file)
       (sort-by key)))

