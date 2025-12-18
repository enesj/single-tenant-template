(ns app.shared.patterns.common)

(defn matches-pattern?
  "Check if a string matches a given regex pattern"
  [pattern string]
  (when (and pattern string)
    (boolean (re-matches pattern string))))
