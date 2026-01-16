#!/usr/bin/env clj

(ns scripts.bb.expenses.receipt-ocr-worker
  (:require
    [aero.core :as aero]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [clojure.string :as str]
    [next.jdbc :as jdbc])
  (:import
    [java.time Instant]))

(defn- parse-pos-int [s]
  (when (and s (re-matches #"\d+" s))
    (Long/parseLong s)))

(defn- usage []
  (println "Usage:")
  (println "  bb receipt-ocr-worker [dev|test] [options]")
  (println "")
  (println "Options:")
  (println "  --max-receipts N         Default 25")
  (println "  --lease-seconds N        Default 900")
  (println "  --storage-base-dir PATH  Default upload/stripes (prepends to relative storage_key)")
  (println "  --max-file-size-mb N     Default 10")
  (println "  --default-currency CUR   Default BAM")
  (println "  --loop                   Run continuously")
  (println "  --interval-seconds N     Default 30 (only with --loop)")
  (println "")
  (println "Env vars:")
  (println "  MISTRAL_OCR_ENABLED=false to disable")
  (println "  MISTRAL_API_KEY=... for provider auth"))

(defn- parse-args [args]
  (loop [args args
         parsed {:profile :dev
                 :max-receipts 25
                 :lease-seconds 900
                 :storage-base-dir "upload/stripes"
                 :max-file-size-bytes (* 10 1024 1024)
                 :default-currency "BAM"
                 :loop? false
                 :interval-seconds 30}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{"dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--help")
        (do (usage) (System/exit 0))

        (= a "--max-receipts")
        (recur more (assoc parsed :max-receipts (or (parse-pos-int b) (:max-receipts parsed))))

        (= a "--lease-seconds")
        (recur more (assoc parsed :lease-seconds (or (parse-pos-int b) (:lease-seconds parsed))))

        (= a "--storage-base-dir")
        (recur more (assoc parsed :storage-base-dir (some-> b str/trim not-empty)))

        (= a "--max-file-size-mb")
        (let [mb (parse-pos-int b)
              bytes (when mb (* mb 1024 1024))]
          (recur more (assoc parsed :max-file-size-bytes (or bytes (:max-file-size-bytes parsed)))))

        (= a "--default-currency")
        (recur more (assoc parsed :default-currency (some-> b str/trim str/upper-case not-empty)))

        (= a "--loop")
        (recur (cons b more) (assoc parsed :loop? true))

        (= a "--interval-seconds")
        (recur more (assoc parsed :interval-seconds (or (parse-pos-int b) (:interval-seconds parsed))))

        :else
        (do
          (println "Unknown arg:" a)
          (usage)
          (System/exit 1))))))

(defn- datasource-from-config [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn- run-once! [ds config {:keys [max-receipts lease-seconds storage-base-dir max-file-size-bytes default-currency]}]
  (receipt-ocr/process-pending!
    ds
    config
    {:max-receipts max-receipts
     :lease-seconds lease-seconds
     :storage-base-dir storage-base-dir
     :max-file-size-bytes max-file-size-bytes
     :default-currency default-currency}))

(defn -main [& args]
  (let [{:keys [profile loop? interval-seconds] :as opts} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)]
    (if loop?
      (loop []
        (println (str "\n[" (Instant/now) "] Running receipt OCR..."))
        (try
          (prn (run-once! ds config opts))
          (catch Exception e
            (println "Receipt OCR run failed:" (.getMessage e))))
        (Thread/sleep (* 1000 (long (or interval-seconds 30))))
        (recur))
      (do
        (prn (run-once! ds config opts))
        (System/exit 0)))))

(apply -main *command-line-args*)
