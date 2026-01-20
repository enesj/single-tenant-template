#!/usr/bin/env bb

(ns fe-test-parallel
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]
    [clojure.string :as str]))

(def usage
  (str/join
    "\n"
    ["Run CLJS tests in parallel by sharding namespaces across Node workers."
     ""
     "Usage: bb scripts/bb/fe_test_parallel.clj [options]"
     ""
     "Options:"
     "  -j, --workers N        Number of parallel workers (default: CPUs or 4)"
     "  --grep REGEX           Only run namespaces matching REGEX"
     "  --randomize            Shuffle namespaces before sharding"
     "  --skip-compile         Skip shadow-cljs compile step (expects target/test-node.cjs)"
     "  --dry-run              Show shard assignment and exit"
     "  --help                 Show this help"
     ""
     "Notes:"
     "- Compiles :test-node once, then runs shards via 'node target/test-node.cjs --test=ns1,ns2'"
     "- Produces a single aggregated log: test-results/cljs-parallel-<timestamp>.log (no per-shard files)"
     "- Exit code is non-zero if any shard fails."]))

(defn parse-args [args]
  (loop [m {:workers nil
            :grep nil
            :randomize false
            :skip-compile false
            :dry-run false
            :help false}
         xs args]
    (if-let [x (first xs)]
      (cond
        (or (= x "--help") (= x "-h"))
        (recur (assoc m :help true) (rest xs))

        (or (= x "-j") (= x "--workers"))
        (recur (assoc m :workers (some-> (second xs) Integer/parseInt)) (nnext xs))

        (= x "--grep")
        (recur (assoc m :grep (second xs)) (nnext xs))

        (= x "--randomize")
        (recur (assoc m :randomize true) (rest xs))

        (= x "--skip-compile")
        (recur (assoc m :skip-compile true) (rest xs))

        (= x "--dry-run")
        (recur (assoc m :dry-run true) (rest xs))

        :else
        (do (println (str "Unknown arg: " x))
          (recur m (rest xs))))
      m)))

(defn now-timestamp []
  (let [fmt (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")]
    (.format (java.time.LocalDateTime/now) fmt)))

(defn ensure-log-dir! [dir]
  (fs/create-dirs dir))

(defn compile-once! []
  (println "🧱 Compiling Shadow-CLJS :test-node build (one-time)...")
  (let [{:keys [exit]} @(p/process ["npx" "shadow-cljs" "compile" "test-node"]
                          {:out :inherit :err :inherit})]
    (when (not= 0 exit)
      (throw (ex-info "shadow-cljs compile failed" {:exit exit})))))

(defn list-test-namespaces []
  (let [{:keys [exit out err]} @(p/process ["node" "target/test-node.cjs" "--list"]
                                  {:out :string :err :string})]
    (when (not= 0 exit)
      (binding [*out* *err*]
        (println (or (not-empty err) out)))
      (throw (ex-info "Listing tests failed" {:exit exit})))
    (->> (str/split-lines out)
      (keep (fn [l]
              (when (str/starts-with? l "Namespace: ")
                (-> l (subs (count "Namespace: ")) str/trim))))
      distinct
      sort
      vec)))

(defn shard [n xs]
  (let [n (max 1 n)
        xs (vec xs)
        buckets (vec (repeat n []))]
    (reduce (fn [acc [i v]]
              (update acc (mod i n) conj v))
      buckets
      (map-indexed vector xs))))

(defn run-shard! [{:keys [idx ns-list]}]
  (let [test-arg (str "--test=" (str/join "," ns-list))
        started (System/nanoTime)
        {:keys [exit out err]} @(p/process ["node" "target/test-node.cjs" test-arg]
                                  {:out :string :err :string})
        dur-ms (long (/ (- (System/nanoTime) started) 1e6))
        header (format "===== SHARD %d | %d namespaces | exit=%d | %d ms =====\n" idx (count ns-list) exit dur-ms)
        body (str header
               (when (seq ns-list) (str "--test=" (str/join "," ns-list) "\n\n"))
               out
               (when (not (str/blank? err))
                 (str "\n[stderr]\n" err))
               "\n===== END SHARD " idx " =====\n")]
    {:idx idx :exit exit :dur-ms dur-ms :text body}))

(defn parse-summary [text]
  (let [re-tests #"(?m)^Ran\s+(\d+)\s+tests?\s+containing\s+(\d+)\s+assertions?\."
        re-fe    #"(?m)^(\d+)\s+failures?,\s+(\d+)\s+errors?\."
        re-point #"(?m)^(FAIL|ERROR) in \(([^\)]+)\) \(([^\):]+):(\d+)\)"
        tests (reduce (fn [{:keys [tests assertions]} [_ t a]]
                        {:tests (+ tests (Long/parseLong t))
                         :assertions (+ assertions (Long/parseLong a))})
                {:tests 0 :assertions 0}
                (re-seq re-tests text))
        fe (reduce (fn [{:keys [failures errors]} [_ f e]]
                     {:failures (+ failures (Long/parseLong f))
                      :errors   (+ errors (Long/parseLong e))})
             {:failures 0 :errors 0}
             (re-seq re-fe text))
        points (map (fn [[_ kind test f l]]
                      {:kind kind :test test :file f :line (Long/parseLong l)})
                 (re-seq re-point text))]
    (merge tests fe {:points (vec points)})))

(defn summary->lines [{:keys [tests assertions failures errors points]}]
  (let [header [(format "===== SUMMARY =====")
                (format "Ran %d tests containing %d assertions." tests assertions)
                (format "%d failures, %d errors." failures errors)]
        points-lines (if (seq points)
                       (cons "Failing points:" (for [{:keys [kind test file line]} points]
                                                 (format "  - %s: %s @ %s:%d" kind test file line)))
                       ["Failing points: (none)"])
        last-line [(format "SUMMARY tests=%d assertions=%d failures=%d errors=%d failing=[%s]"
                     tests assertions failures errors
                     (if (seq points)
                       (str/join "; " (map (fn [{:keys [kind test file line]}]
                                             (format "%s:%s@%s:%d" kind test file line)) points))
                       ""))]]
    (concat header points-lines last-line)))

(defn -main [& args]
  (let [{:keys [workers grep randomize skip-compile dry-run help]} (parse-args args)]
    (when help
      (println usage)
      (System/exit 0))

    (let [cpu (.. Runtime getRuntime availableProcessors)
          default-w (or (some-> workers int) (min 4 cpu))
          log-dir (fs/path "test-results")
          _ (ensure-log-dir! log-dir)
          ts (now-timestamp)]
      (when-not skip-compile
        (compile-once!))

      (println "🔎 Discovering test namespaces via node --list ...")
      (let [all-ns (list-test-namespaces)
            ns* (cond->> all-ns
                  grep (filter #(re-find (re-pattern grep) %))
                  randomize (shuffle))
            total (count ns*)
            w (max 1 (min default-w total))
            shards (->> ns* (shard w) (map-indexed (fn [i xs] [i xs])) vec)]
        (println (format "🧪 %d test namespaces, %d worker(s)" total w))

        (doseq [[i xs] shards]
          (println (format "  • shard %d => %d namespaces" i (count xs))))

        (when dry-run
          (println "(dry-run) Nothing executed.")
          (System/exit 0))

        (println "🚀 Running shards in parallel...")
        (let [futs (for [[i xs] shards]
                     (future (run-shard! {:idx i :ns-list xs})))
              results (mapv deref futs)
              exit (if (every? #(zero? (:exit %)) results) 0 1)
              agg-file (str (fs/path log-dir (format "cljs-parallel-%s.log" ts)))]
          (println (format "📄 Aggregating logs -> %s" agg-file))
          (let [agg-text (apply str (map :text results))
                summary  (parse-summary agg-text)
                final    (str agg-text "\n" (str/join "\n" (summary->lines summary)) "\n")]
            (spit agg-file final)
            (println (last (summary->lines summary))))
          (doseq [{:keys [idx exit dur-ms]} results]
            (println (format "  • shard %d done: exit=%d (%d ms)" idx exit dur-ms)))
          (if (zero? exit)
            (do (println "✅ All shards passed") (System/exit 0))
            (do (println "❌ Some shards failed (see logs)") (System/exit 1))))))))

(apply -main *command-line-args*)
