#!/usr/bin/env bb

(require '[babashka.fs :as fs])
(require '[babashka.process :as process])
(require '[clojure.string :as str])

(defn parse-long' [s]
  (Long/parseLong s))

(def extra-id-chars
  (set "_-?!*+'<>$%&=.#"))

(defn id-char? [ch]
  (when ch
    (or (Character/isLetterOrDigit ch)
        (contains? extra-id-chars ch))))

(defn char-at [s idx]
  (when (and (>= idx 0) (< idx (count s)))
    (.charAt s idx)))

(defn symbol-boundary? [line idx len]
  (let [prev (char-at line (dec idx))
        next (char-at line (+ idx len))]
    (and (not (id-char? prev))
         (not (id-char? next)))))

(defn find-symbol-index [line sym start]
  (let [len (count sym)]
    (loop [idx (.indexOf line sym start)]
      (when (<= 0 idx)
        (if (symbol-boundary? line idx len)
          idx
          (recur (.indexOf line sym (+ idx len))))))))

(defn locate-symbol [line sym col]
  (let [start (max 0 (dec col))
        len (count sym)]
    (cond
      (and (<= (+ start len) (count line))
           (= sym (subs line start (+ start len)))
           (symbol-boundary? line start len))
      start

      :else
      (or (find-symbol-index line sym start)
          (find-symbol-index line sym 0)))))

(defn prefix-symbol [{:keys [symbol col]} line]
  (let [idx (locate-symbol line symbol col)]
    (cond
      (nil? idx)
      {:line line :status :not-found}

      (and (> idx 0)
           (= (char-at line (dec idx)) \_))
      {:line line :status :already-prefixed}

      :else
      {:line (str (subs line 0 idx) "_" symbol (subs line (+ idx (count symbol))))
       :status :updated})))

(def unused-binding-warning
  (re-pattern "^(.+?):(\\d+):(\\d+):\\s+\\w+: unused binding ([^ ]+)$"))

(def duplicate-require-warning
  (re-pattern "^(.+?):(\\d+):(\\d+):\\s+\\w+: duplicate require of .+$"))

(def unused-namespace-warning
  (re-pattern "^(.+?):(\\d+):(\\d+):\\s+\\w+: namespace .+ is required but never use(?:d)?$"))

(defn parse-warning [line]
  (if-let [[_ file line-str col-str symbol] (re-matches unused-binding-warning line)]
    {:type :unused-binding
     :file file
     :line (parse-long' line-str)
     :col (parse-long' col-str)
     :symbol symbol}
    (if-let [[_ file line-str col-str] (re-matches duplicate-require-warning line)]
      {:type :clean-ns
       :file file
       :line (parse-long' line-str)
       :col (parse-long' col-str)}
      (if-let [[_ file line-str col-str] (re-matches unused-namespace-warning line)]
        {:type :clean-ns
         :file file
         :line (parse-long' line-str)
         :col (parse-long' col-str)}
        nil))))

(defn trailing-newline? [s]
  (or (str/ends-with? s "\n")
      (str/ends-with? s "\r")))

(defn apply-warnings-to-file [file entries]
  (let [path (fs/file file)]
    (if-not (fs/exists? path)
      (binding [*out* *err*]
        (println "Skipping" file "(not found)"))
      (let [contents (slurp path)
            original-lines (vec (str/split-lines contents))
            sorted (sort-by (juxt :line :col) entries)
            result (reduce
                     (fn [{:keys [lines statuses]} entry]
                       (let [line-idx (dec (:line entry))
                             current (get lines line-idx "")
                             {:keys [line status]} (prefix-symbol entry current)]
                         {:lines (assoc lines line-idx line)
                          :statuses (conj statuses (assoc entry :status status))}))
                     {:lines original-lines :statuses []}
                     sorted)
            updated-lines (:lines result)
            statuses (:statuses result)
            new-contents (let [joined (str/join "\n" updated-lines)
                               ends-newline? (trailing-newline? contents)]
                           (if ends-newline?
                             (str joined "\n")
                             joined))]
        (doseq [{:keys [status line col symbol]} statuses]
          (case status
            :updated (println "Prefixed" (str file ":" line ":" col) "->" (str "_" symbol))
            :already-prefixed (binding [*out* *err*]
                                (println "Already prefixed" (str file ":" line ":" col)))
            :not-found (binding [*out* *err*]
                         (println "Could not locate" symbol "in" (str file ":" line ":" col)))))
        (when (not= contents new-contents)
          (spit path new-contents))))))

(defn run-clean-ns [files]
  (try
    (let [file-arg (str/join "," files)
          cmd ["clojure-lsp" "clean-ns" "--filenames" file-arg]
          {:keys [exit]} (process/sh cmd {:out :inherit :err :inherit :check false})]
      (when (not= 0 exit)
        (binding [*out* *err*]
          (println "clojure-lsp clean-ns failed with exit code" exit))))
    (catch java.io.IOException _
      (binding [*out* *err*]
        (println "clojure-lsp executable not found; skipping clean-ns for" (str/join ", " files))))))

(def lint-command ["clj-kondo" "--parallel" "--cache" "false" "--lint" "src" "dev" "test"])

(let [{:keys [out err exit]} (process/sh lint-command {:out :string :err :string :check false})
      output (str out (when (seq err) err))]
  (print output)
  (flush)
  (let [warnings (->> (str/split-lines output)
                      (keep parse-warning))
        unused-binding-warnings (filter #(= :unused-binding (:type %)) warnings)
        clean-ns-files (->> warnings
                            (filter #(= :clean-ns (:type %)))
                            (map :file)
                            distinct
                            vec)]
    (doseq [[file entries] (group-by :file unused-binding-warnings)]
      (apply-warnings-to-file file entries))
    (when (seq clean-ns-files)
      (println "Running clojure-lsp clean-ns on:" (str/join ", " clean-ns-files))
      (flush)
      (run-clean-ns clean-ns-files))
    (System/exit exit)))
