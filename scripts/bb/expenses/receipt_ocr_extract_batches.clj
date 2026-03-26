#!/usr/bin/env clj

(ns receipt-ocr-extract-batches
  (:require
    [app.domain.backend.expenses.services.receipts.storage :as storage]
    [app.template.backend.core :as backend]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private default-statuses ["posted" "extracted"])

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Plan batched receipt IDs for OCR extract workflow validation.")
   (println)
   (println "Usage:")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_extract_batches.clj [dev|test|prod] [options]")
   (println)
   (println "Options:")
   (println "  --batch-size N     Batch size (default 20)")
   (println "  --status STATUS    Receipt status to include; repeatable (default posted + extracted)")
   (println "  --limit N          Optional row limit after filtering")
   (println "  --offset N         Row offset before batching (default 0)")
   (println "  --output PATH      Optional EDN output file")
   (println "  --help             Show this help")
   (println)
   (println "Examples:")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_extract_batches.clj dev")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_extract_batches.clj dev --batch-size 20 --output tmp/receipt-ocr-batches.edn")
   (println "  clj -M scripts/bb/expenses/receipt_ocr_extract_batches.clj dev --status extracted --status posted --limit 40")))

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

(defn- normalize-status
  [s]
  (let [status (some-> s str str/trim str/lower-case)]
    (when-not (contains? #{"uploaded" "parsing" "parsed" "extracting" "extracted" "review_required" "posted" "failed"} status)
      (throw (ex-info "Unsupported receipt status"
               {:value s})))
    status))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:profile :dev
                 :batch-size 20
                 :statuses []
                 :limit nil
                 :offset 0
                 :output nil}]
    (let [[a b & more] args]
      (cond
        (nil? a)
        (update parsed :statuses #(vec (if (seq %) % default-statuses)))

        (or (= a "--help") (= a "-h"))
        (do
          (usage)
          (System/exit 0))

        (contains? #{"dev" "test" "prod"} a)
        (recur (cons b more) (assoc parsed :profile (normalize-profile a)))

        (= a "--batch-size")
        (do
          (when-not b
            (throw (ex-info "Missing value for --batch-size" {})))
          (recur more (assoc parsed :batch-size (max 1 (parse-long! "--batch-size" b)))))

        (= a "--status")
        (do
          (when-not b
            (throw (ex-info "Missing value for --status" {})))
          (recur more (update parsed :statuses conj (normalize-status b))))

        (= a "--limit")
        (do
          (when-not b
            (throw (ex-info "Missing value for --limit" {})))
          (recur more (assoc parsed :limit (max 1 (parse-long! "--limit" b)))))

        (= a "--offset")
        (do
          (when-not b
            (throw (ex-info "Missing value for --offset" {})))
          (recur more (assoc parsed :offset (max 0 (parse-long! "--offset" b)))))

        (= a "--output")
        (do
          (when-not b
            (throw (ex-info "Missing value for --output" {})))
          (recur more (assoc parsed :output b)))

        :else
        (throw (ex-info (str "Unknown arg: " a) {:arg a}))))))

(defn- fetch-receipts
  [db {:keys [statuses limit offset]}]
  (jdbc/execute!
    db
    (sql/format
      (cond-> {:select [:id :status :original_filename :created_at]
               :from [:receipts]
               :where [:in :status (mapv storage/receipt-status-cast statuses)]
               :order-by [[:created_at :asc] [:id :asc]]
               :offset offset}
        limit (assoc :limit limit)))
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- row->public
  [{:keys [id status original_filename created_at]}]
  {:receipt-id (str id)
   :status (str status)
   :original-filename original_filename
   :created-at (some-> created_at str)})

(defn- build-batches
  [rows batch-size]
  (->> rows
    (map row->public)
    (partition-all batch-size)
    (map-indexed (fn [idx batch]
                   {:batch-number (inc idx)
                    :batch-label (str "batch-" (inc idx))
                    :receipt-count (count batch)
                    :status-counts (frequencies (map :status batch))
                    :first-created-at (:created-at (first batch))
                    :last-created-at (:created-at (last batch))
                    :receipt-ids (mapv :receipt-id batch)
                    :receipts (vec batch)}))
    vec))

(defn- emit!
  [{:keys [output] :as _opts} data]
  (let [rendered (with-out-str (pprint/pprint data))]
    (when output
      (io/make-parents output)
      (spit output rendered))
    (print rendered)
    (flush)))

(defn -main
  [& args]
  (try
    (let [{:keys [profile batch-size statuses] :as opts} (parse-args args)]
      (with-open [config (backend/load-config {:profile profile})
                  db (backend/create-conn-pool @config)]
        (let [rows (fetch-receipts db opts)
              batches (build-batches rows batch-size)]
          (emit! opts {:profile (name profile)
                       :statuses statuses
                       :batch-size batch-size
                       :total-receipts (count rows)
                       :total-batches (count batches)
                       :batches batches}))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (usage (or (.getMessage e) "receipt-ocr-extract-batches failed")))
      (System/exit 1))
    (catch Exception e
      (binding [*out* *err*]
        (println "receipt-ocr-extract-batches failed:")
        (println (.getMessage e)))
      (System/exit 1))))

(apply -main *command-line-args*)