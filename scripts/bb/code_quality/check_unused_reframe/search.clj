(ns code-quality.check-unused-reframe.search
  (:require
    [babashka.fs :as fs]
    [clojure.string :as str]))

(defn safe-slurp [file-path]
  (try
    (slurp file-path)
    (catch Exception _
      nil)))

(defn search-in-file
  "Search for a regex pattern in a file, return line numbers where found.

  pattern can be either a java.util.regex.Pattern or a string (compiled to a regex)."
  [file-path pattern]
  (when (fs/exists? file-path)
    (when-let [content (safe-slurp file-path)]
      (let [re (if (instance? java.util.regex.Pattern pattern)
                 pattern
                 (re-pattern (java.util.regex.Pattern/quote (str pattern))))
            lines (str/split-lines content)]
        (->> lines
          (map-indexed
            (fn [idx line]
              (when (re-find re line)
                {:line (inc idx)
                 :content (str/trim line)})))
          (filter some?)
          vec)))))

(defn parse-keyword-str
  "Parse a keyword string like ':admin/foo' or ':my.ns/bar' or ':foo'."
  [keyword-str]
  (let [s (cond-> keyword-str (str/starts-with? keyword-str ":") (subs 1))
        parts (str/split s #"/" 2)]
    (if (= 2 (count parts))
      {:qualified? true
       :ns (first parts)
       :name (second parts)
       :full s}
      {:qualified? false
       :name (first parts)
       :full s})))


(defn get-search-patterns
  "Convert keyword string to regex patterns.

  Handles:
  - literal keywords: :admin/foo
  - dispatch/subscribe vectors: [:admin/foo ...]
  - keyword in strings: admin/foo
  - auto-resolved keywords used via ::kw or ::alias/kw for qualified keywords.
    Example: :app.admin.frontend.subs.config/column-visible? may be written as
    ::column-visible? (inside that ns) or ::admin-subs/column-visible? (outside).
  "
  [keyword-str]
  (let [{:keys [qualified? ns name full]} (parse-keyword-str keyword-str)
        escaped-full (java.util.regex.Pattern/quote (str ":" full))
        ;; For unqualified keywords, full is the name.
        escaped-unqualified (java.util.regex.Pattern/quote (str ":" (if qualified? full name)))
        base (if qualified? escaped-full escaped-unqualified)
        ;; Auto-resolved forms for qualified keywords.
        escaped-name (when qualified? (java.util.regex.Pattern/quote name))]
    (cond->
      [;; literal keyword
       (re-pattern base)
       ;; dispatch/subscribe vector prefix
       (re-pattern (str "\\[" base))
       ;; string form without leading colon
       (re-pattern (str "\"" (java.util.regex.Pattern/quote (if qualified? full name)) "\""))]

      qualified?
      (into
        [;; ::name (in defining ns)
         (re-pattern (str "::" escaped-name "\\b"))
         ;; ::alias/name (in other namespaces)
         (re-pattern (str "::[A-Za-z0-9_.-]+/" escaped-name "\\b"))]))))

(defn search-codebase
  "Search for keyword usage across the codebase (src + test).

  Returns matches in *all* files (including the definition file), because internal dependencies
  between subscriptions/events matter when deciding what can be safely commented out." 
  [keyword-info]
  (let [keyword-str (:keyword keyword-info)
    patterns (get-search-patterns keyword-str)
    files (concat
        (fs/glob "src" "**/*.{cljs,cljc,clj,edn}")
        (fs/glob "test" "**/*.{cljs,cljc,clj,edn}"))]
    (for [file files
      :let [file-path (str file)]
      pattern patterns
      :let [matches (search-in-file file-path pattern)]
      :when (seq matches)]
  {:file file-path
   :pattern (str pattern)
   :matches matches})))

