(ns code-quality.check-unused-reframe.comment-out
  (:require
    [babashka.fs :as fs]
    [clojure.string :as str]
    [code-quality.check-unused-reframe.search :as search]))

(defn- keyword-source-forms
  "Return possible textual keyword forms used in source for a keyword string.

  - Always includes the fully-qualified literal form (e.g. :admin/foo or :my.ns/bar).
  - For qualified keywords, also includes the auto-resolved local form ::name (common in its defining ns)."
  [keyword-str]
  (let [{:keys [qualified? ns name full]} (search/parse-keyword-str keyword-str)
        full-form (str ":" full)
        ;; Only include ::name when the keyword namespace looks like a fully-qualified
        ;; Clojure namespace (contains dots). For simple namespaces like :admin/foo,
        ;; ::foo would resolve to the *current* namespace and is unrelated.
        include-auto? (and qualified?
                        (string? ns)
                        (str/includes? ns "."))]
    (cond-> [full-form]
      include-auto?
      (conj (str "::" name)))))

(defn- candidate-reg-macros
  "Possible rf registration macros for a given keyword type." 
  [kw-type]
  (case kw-type
    :subscription ["reg-sub"]
    :event ["reg-event-db" "reg-event-fx" "reg-event"]
    ;; fallback
    ["reg-sub" "reg-event-db" "reg-event-fx" "reg-event"]))

(defn- find-and-comment-registration
  "Given file content, try to comment out exactly one rf registration form for keyword.

  Uses #_ reader macro inserted after indentation (so clojure-lsp and the reader ignore it).

  Returns {:status :changed, :content <new>} or {:status :skipped, :reason ...}"
  [{:keys [keyword type] :as kw-info} content]
  (if-not (string? content)
    {:status :skipped
     :reason :unreadable-content
     :keyword keyword}
    (let [kw-forms (keyword-source-forms keyword)
      macros (candidate-reg-macros type)
      ;; Build regexes that match either multiline form:
      ;; (rf/reg-sub\n  :kw ...)
      ;; or same-line: (rf/reg-sub :kw ...)
      patterns (for [m macros
         kwf kw-forms
         :let [kwq (java.util.regex.Pattern/quote kwf)
               ;; Important: use horizontal whitespace (\\h) so the same-line matcher does NOT
               ;; also match multiline forms (\\s includes newlines).
                               ;; Match any re-frame namespace alias (commonly rf, but sometimes re-frame.core).
                               ns-prefix "(?:[A-Za-z0-9_.-]+/)"
                               mline (re-pattern (str "(?ms)^(\\h*)(?!#_)(\\(" ns-prefix m "\\h*\\R\\h*" kwq "(?:\\s|\\R|\\))" ")"))
                               sline (re-pattern (str "(?m)^(\\h*)(?!#_)(\\(" ns-prefix m "\\h+" kwq "(?:\\s|$|\\))" ")"))
                               ;; Detect already-commented registrations (idempotent apply).
                               mline-commented (re-pattern (str "(?ms)^\\h*#_\\(" ns-prefix m "\\h*\\R\\h*" kwq "(?:\\s|\\R|\\))"))
                               sline-commented (re-pattern (str "(?m)^\\h*#_\\(" ns-prefix m "\\h+" kwq "(?:\\s|$|\\))"))]]
         {:macro m
          :kw-form kwf
          :regexes [mline sline]
          :commented-regexes [mline-commented sline-commented]})
      matches (->> patterns
        (mapcat
          (fn [{:keys [macro kw-form regexes] :as p}]
        (for [re regexes
          :let [m (re-find re content)]
          :when m]
          (assoc p :re re :match m))))
        vec)]
  (cond
    (empty? matches)
    (let [already-commented?
          (some true?
            (for [{:keys [commented-regexes]} patterns
                  re commented-regexes]
              (boolean (re-find re content))))]
      (if already-commented?
        {:status :unchanged
         :reason :already-commented
         :keyword keyword}
        {:status :skipped
         :reason :not-found
         :keyword keyword}))

    (> (count matches) 1)
    {:status :skipped
     :reason :ambiguous
     :keyword keyword
     :matches (mapv #(select-keys % [:macro :kw-form]) matches)}

    :else
    (let [{:keys [re match]} (first matches)
      ;; match is [full-match indent+form]
      indent (nth match 1)
      form-start (nth match 2)
      replacement (str indent "#_" form-start)
      new-content (str/replace-first content re (java.util.regex.Matcher/quoteReplacement replacement))]
      {:status :changed
       :keyword keyword
       :content new-content})))))

(defn apply-comment-outs!
  "Apply #_ comment-outs for all truly-unused items.

  Returns a report map with :changed and :skipped entries." 
  [unused-items]
  (let [candidates (->> unused-items
                     ;; never touch vendor code
                     (remove (fn [{:keys [file]}] (str/starts-with? (or file "") "vendor/")))
                     ;; only files that exist
                     (filter (fn [{:keys [file]}] (and file (fs/exists? file))))
                     vec)
        grouped (group-by :file candidates)]
    (reduce-kv
      (fn [acc file items]
        (let [orig (search/safe-slurp file)]
          (if-not (string? orig)
            (update acc :skipped into (mapv (fn [item]
                                             {:file file
                                              :keyword (:keyword item)
                                              :reason :unreadable-file})
                                           items))
            (let [{:keys [content changes skips]}
                  (reduce
                    (fn [{:keys [content changes skips] :as st} item]
                      (let [{:keys [status] :as result} (find-and-comment-registration item content)]
                        (case status
                          :changed {:content (:content result)
                                    :changes (conj changes (select-keys result [:keyword]))
                                    :skips skips}
                          :unchanged st
                          :skipped {:content content
                                    :changes changes
                                    :skips (conj skips (select-keys result [:keyword :reason :matches]))}
                          st)))
                    {:content orig :changes [] :skips []}
                    items)]
              (when (not= orig content)
                (spit file content))
              (-> acc
                (update :changed into (map (fn [c] (assoc c :file file)) changes))
                (update :skipped into (map (fn [s] (assoc s :file file)) skips)))))))
      {:changed [] :skipped []}
      grouped)))

