#!/usr/bin/env clj

(ns receipt-ocr-reparse-batch
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.runner :as runner]
    [app.template.backend.core :as backend]
    [app.template.backend.utils.json-config :as json-config]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str])
  (:import
    [java.util UUID]
    [java.util.concurrent Callable Executors TimeUnit]))

(def ^:private default-max-concurrent 6)

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Reparse one receipt OCR batch using bounded parallelism.")
   (println)
   (println "Usage:")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_reparse_batch.clj [dev|test|prod] [options]")
   (println)
   (println "Required input:")
   (println "  Either:")
   (println "    --batch-number N --batches-file PATH")
   (println "  Or one or more:")
   (println "    --receipt-id UUID")
   (println)
   (println "Options:")
   (println "  --batch-label LABEL    Optional label for output (defaults to batch-<N> or manual-batch)")
   (println "  --batches-file PATH    EDN file produced by receipt_ocr_extract_batches.clj")
   (println "  --batch-number N       1-based batch number inside --batches-file")
   (println "  --receipt-id UUID      Explicit receipt ID; repeatable")
   (println "  --max-concurrent N     Parallel workers (default 6 or RECEIPT_OCR_UI_MAX_CONCURRENT)")
   (println "  --no-defer-refine      Allow inline refine instead of deferring it")
   (println "  --no-reset             Skip reset-for-ocr! before rerun")
   (println "  --help                 Show this help")
   (println)
   (println "Examples:")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_reparse_batch.clj dev --batches-file tmp/receipt-ocr-batches.edn --batch-number 1")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_reparse_batch.clj dev --batch-label sample --receipt-id 00000000-0000-0000-0000-000000000000")))

(defn- parse-long!
  [label s]
  (try
    (Long/parseLong (str s))
    (catch Exception _
      (throw (ex-info (str "Invalid value for " label)
               {:label label
                :value s})))))

(defn- normalize-profile
  [s]
  (let [profile (keyword (str s))]
    (when-not (contains? #{:dev :test :prod} profile)
      (throw (ex-info "Profile must be one of dev, test, or prod"
               {:value s})))
    profile))

(defn- parse-uuid!
  [label s]
  (try
    (UUID/fromString (str s))
    (catch Exception _
      (throw (ex-info (str "Invalid UUID for " label)
               {:label label
                :value s})))))

(defn- env->pos-int
  [k default-val]
  (try
    (let [v (some-> (System/getenv k) str str/trim not-empty)]
      (if v
        (max 1 (Long/parseLong v))
        default-val))
    (catch Exception _
      default-val)))

(defn- parse-args
  [args]
  (loop [remaining args
         parsed {:profile :dev
                 :batch-label nil
                 :batches-file nil
                 :batch-number nil
                 :receipt-ids []
                 :max-concurrent (env->pos-int "RECEIPT_OCR_UI_MAX_CONCURRENT" default-max-concurrent)
                 :defer-refine? true
                 :reset? true}]
    (let [a (first remaining)
          b (second remaining)
          more (nnext remaining)]
      (cond
        (nil? a)
        parsed

        (or (= a "--help") (= a "-h"))
        (do
          (usage)
          (System/exit 0))

        (contains? #{"dev" "test" "prod"} a)
        (recur (rest remaining) (assoc parsed :profile (normalize-profile a)))

        (= a "--batch-label")
        (do
          (when-not b
            (throw (ex-info "Missing value for --batch-label" {})))
          (recur more (assoc parsed :batch-label b)))

        (= a "--batches-file")
        (do
          (when-not b
            (throw (ex-info "Missing value for --batches-file" {})))
          (recur more (assoc parsed :batches-file b)))

        (= a "--batch-number")
        (do
          (when-not b
            (throw (ex-info "Missing value for --batch-number" {})))
          (recur more (assoc parsed :batch-number (max 1 (parse-long! "--batch-number" b)))))

        (= a "--receipt-id")
        (do
          (when-not b
            (throw (ex-info "Missing value for --receipt-id" {})))
          (recur more (update parsed :receipt-ids conj (parse-uuid! "--receipt-id" b))))

        (= a "--max-concurrent")
        (do
          (when-not b
            (throw (ex-info "Missing value for --max-concurrent" {})))
          (recur more (assoc parsed :max-concurrent (max 1 (parse-long! "--max-concurrent" b)))))

        (= a "--no-defer-refine")
        (recur (rest remaining) (assoc parsed :defer-refine? false))

        (= a "--no-reset")
        (recur (rest remaining) (assoc parsed :reset? false))

        :else
        (throw (ex-info (str "Unknown arg: " a) {:arg a}))))))

(defn- load-batch-receipt-ids
  [{:keys [batches-file batch-number]}]
  (when-not (and batches-file batch-number)
    (throw (ex-info "Both --batches-file and --batch-number are required when using batch selection" {})))
  (let [data (edn/read-string (slurp (io/file batches-file)))
        batch (first (filter #(= batch-number (:batch-number %)) (:batches data)))]
    (when-not batch
      (throw (ex-info "Batch number not found in batches file"
               {:batch-number batch-number
                :batches-file batches-file})))
    (mapv #(parse-uuid! "receipt-id-from-batch" %) (:receipt-ids batch))))

(defn- resolve-receipt-ids
  [{:keys [receipt-ids batches-file batch-number]}]
  (cond
    (seq receipt-ids)
    (vec receipt-ids)

    (or batches-file batch-number)
    (load-batch-receipt-ids {:batches-file batches-file
                             :batch-number batch-number})

    :else
    (throw (ex-info "Provide either --receipt-id or (--batches-file + --batch-number)" {}))))

(defn- summarize-results
  [results]
  {:summary (frequencies (map :result results))
   :status-counts (frequencies (map :effective-status results))
   :review-required-count (count (filter :review-required? results))})

(defn- submit-runs
  [pool database config receipt-ids {:keys [defer-refine? reset?]}]
  (mapv (fn [rid]
          {:receipt-id rid
           :future (.submit pool
                     ^Callable
                     (fn []
                       (runner/run-by-ids! database config [rid] {:defer-refine? defer-refine?
                                                                  :reset? reset?})))})
    receipt-ids))

(defn -main
  [& args]
  (try
    (let [{:keys [profile batch-number batch-label max-concurrent defer-refine? reset?] :as opts} (parse-args args)
          receipt-ids (resolve-receipt-ids opts)
          batch-label (or batch-label
                        (when batch-number (str "batch-" batch-number))
                        "manual-batch")]
      (with-open [config (backend/load-config {:profile profile})
                  database (backend/create-conn-pool @config)]
        (json-config/init!)
        (let [started-at (System/nanoTime)
              pool (Executors/newFixedThreadPool max-concurrent)]
          (try
            (println "Running receipt OCR batch" batch-label "with" (count receipt-ids) "receipts")
            (println "Parallelism:" max-concurrent)
            (println "Receipt IDs:")
            (doseq [rid receipt-ids]
              (println "-" rid))
            (println "Batch result:")
            (let [futures (submit-runs pool database @config receipt-ids {:defer-refine? defer-refine?
                                                                          :reset? reset?})
                  receipt-results (mapv (fn [{:keys [receipt-id future]}]
                                          {:receipt-id receipt-id
                                           :batch-result (.get future)})
                                    futures)
                  item-results (mapv (fn [{:keys [receipt-id batch-result]}]
                                       (or (some-> batch-result :results first)
                                         {:receipt-id receipt-id
                                          :result :missing-result
                                          :raw batch-result}))
                                 receipt-results)
                  {:keys [summary status-counts review-required-count]} (summarize-results item-results)
                  duration-ms (/ (- (System/nanoTime) started-at) 1000000.0)]
              (pprint/pprint {:batch batch-label
                              :profile (name profile)
                              :max-concurrent max-concurrent
                              :defer-refine? defer-refine?
                              :reset? reset?
                              :processed (count item-results)
                              :summary summary
                              :status-counts status-counts
                              :review-required-count review-required-count
                              :duration-ms duration-ms
                              :receipt-ids (mapv str receipt-ids)
                              :results item-results}))
            (finally
              (.shutdown pool)
              (.awaitTermination pool 5 TimeUnit/SECONDS))))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (usage (or (.getMessage e) "receipt-ocr-reparse-batch failed")))
      (System/exit 1))
    (catch Exception e
      (binding [*out* *err*]
        (println "receipt-ocr-reparse-batch failed:")
        (println (.getMessage e)))
      (System/exit 1))))

(apply -main *command-line-args*)