#!/usr/bin/env bb

(ns ai.audit-bundle
  (:require
    [babashka.fs :as fs]
    [babashka.process :as bp]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- usage!
  []
  (die!
    (str
      "Usage:\n"
      "  bb audit-bundle --query <regex> [--query <regex> ...] [options]\n\n"
      "Options:\n"
      "  --glob <glob>         Repeatable. Restrict search via ripgrep --glob\n"
      "  --context <n>          Context lines before/after matches (default: 2)\n"
      "  --label <slug>         Optional slug used in default output filename\n"
      "  --out <path>           Optional explicit output file path\n\n"
      "Examples:\n"
      "  bb audit-bundle --query 'app\\.admin\\.frontend\\.core/init' --glob docs/** --glob src/**\n"
      "  bb audit-bundle --query ':init-fn' --glob shadow-cljs.edn --context 4 --label routing\n")))

(defn- now-stamp
  []
  (let [fmt (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")]
    (.format fmt (java.time.LocalDateTime/now))))

(defn- parse-int
  [s default]
  (try
    (Integer/parseInt (str s))
    (catch Exception _ default)))

(defn- parse-args
  [args]
  (loop [args args
         opts {:queries []
               :globs []
               :context 2
               :label nil
               :out nil}]
    (if (empty? args)
      opts
      (let [[a & more] args]
        (case a
          "--query" (let [[q & more2] more]
                      (when-not q (usage!))
                      (recur more2 (update opts :queries conj q)))
          "--glob" (let [[g & more2] more]
                     (when-not g (usage!))
                     (recur more2 (update opts :globs conj g)))
          "--context" (let [[n & more2] more]
                        (when-not n (usage!))
                        (recur more2 (assoc opts :context (parse-int n 2))))
          "--label" (let [[l & more2] more]
                      (when-not l (usage!))
                      (recur more2 (assoc opts :label l)))
          "--out" (let [[p & more2] more]
                    (when-not p (usage!))
                    (recur more2 (assoc opts :out p)))
          (usage!))))))

(defn- default-out-path
  [{:keys [label]}]
  (let [stamp (now-stamp)
        slug (if (and label (not (str/blank? label)))
               (-> label
                 (str/lower-case)
                 (str/replace #"[^a-z0-9._-]+" "-")
                 (str/replace #"^-+|-+$" ""))
               "audit")]
    (str (fs/path "target" "audit-bundles" (str stamp "-" slug ".txt")))))

(defn- rg-args
  [{:keys [queries globs context]}]
  (when (empty? queries)
    (usage!))
  (cond-> ["rg" "--json" "--no-messages" "--smart-case" "-C" (str context)]
    (seq globs) (into (mapcat (fn [g] ["--glob" g]) globs))
    true (into (mapcat (fn [q] ["-e" q]) queries))
    true (conj ".")))

(defn- write-header!
  [^java.io.Writer w {:keys [queries globs context]} out-path]
  (.write w "# audit-bundle\n")
  (.write w (str "# generated-at: " (now-stamp) "\n"))
  (.write w (str "# out: " out-path "\n"))
  (.write w (str "# context: " context "\n"))
  (.write w (str "# queries:\n"))
  (doseq [q queries]
    (.write w (str "#   - " q "\n")))
  (when (seq globs)
    (.write w (str "# globs:\n"))
    (doseq [g globs]
      (.write w (str "#   - " g "\n"))))
  (.write w "\n")
  (.flush w))

(defn- safe-line
  [s]
  (-> s
    (str/replace #"\r\n" "\n")
    (str/replace #"\r" "\n")
    (str/replace #"\n$" "")))

(defn- event->line
  [event]
  (let [t (get event "type")
        data (get event "data")
        path (get-in data ["path" "text"])
        line-number (get data "line_number")
        line-text (safe-line (get-in data ["lines" "text"] ""))]
    (case t
      "match" {:kind :match :line (format "[MATCH] %s:%s: %s\n" path line-number line-text)}
      "context" {:kind :ctx :line (format "[CTX]   %s:%s: %s\n" path line-number line-text)}
      nil)))

(defn- check-rg-exit!
  [exit-code stderr-text]
  ;; ripgrep conventions:
  ;; 0 -> matches found
  ;; 1 -> no matches found
  ;; 2 -> error
  (when-not (#{0 1} exit-code)
    (die!
      (str
        "ripgrep failed with exit code " exit-code "\n"
        (when-not (str/blank? stderr-text)
          (str "stderr:\n" (safe-line stderr-text) "\n"))
        "\n"
        "Tip: ensure `rg` is installed and the query/globs are valid."))))

(defn- run-audit!
  [opts]
  (let [out-path (or (:out opts) (default-out-path opts))
        _ (fs/create-dirs (fs/parent out-path))
        args (rg-args opts)
        proc (bp/process args {:out :pipe :err :string})]
    (with-open [w (io/writer out-path)]
      (write-header! w opts out-path)
      (with-open [r (io/reader (:out proc))]
        (loop [lines (line-seq r)
               match-count 0
               ctx-count 0]
          (if (empty? lines)
            (let [{:keys [exit err]} @proc
                  stderr-text err]
              (check-rg-exit! exit stderr-text)
              (when (and stderr-text (not (str/blank? stderr-text)))
                (.write w (str "\n# rg-stderr:\n" (safe-line stderr-text) "\n")))
              (.write w (str "\n# summary:\n#   matches: " match-count "\n#   ctx: " ctx-count "\n"))
              (.flush w)
              {:out out-path :matches match-count :ctx ctx-count :exit exit})
            (let [line (first lines)
                  event (try
                          (json/read-str line)
                          (catch Exception _ nil))
                  parsed (when event (event->line event))]
              (if parsed
                (do
                  (.write w ^String (:line parsed))
                  (recur (rest lines)
                    (if (= :match (:kind parsed)) (inc match-count) match-count)
                    (if (= :ctx (:kind parsed)) (inc ctx-count) ctx-count)))
                (recur (rest lines) match-count ctx-count)))))))))

(let [opts (parse-args *command-line-args*)
      result (run-audit! opts)]
  (println (str "Wrote audit bundle: " (:out result)))
  (println (str "Matches: " (:matches result) ", context lines: " (:ctx result) ", rg-exit: " (:exit result))))
