(ns app.domain.backend.expenses.workers.receipt-ocr.ui-queue
  "Bounded executor for UI-triggered OCR tasks."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.runner :as runner]
    [taoensso.timbre :as log])
  (:import
    [java.util.concurrent Executors]))

(defn- env->pos-int
  [k default-val]
  (try
    (let [v (System/getenv k)]
      (if (seq v)
        (max 1 (Long/parseLong v))
        default-val))
    (catch Exception _
      default-val)))

(defonce ^:private ui-ocr-max-concurrent
  (env->pos-int "RECEIPT_OCR_UI_MAX_CONCURRENT" 6))

(defonce ^:private ui-ocr-executor
  (Executors/newFixedThreadPool ui-ocr-max-concurrent))

(defn enqueue!
  "Queue receipt OCR work from UI endpoints.

  UI endpoints can submit multiple receipt IDs (e.g. multi-file upload). We run
  each receipt in the shared bounded executor so the user sees parallel progress
  without overwhelming the OCR provider.

  Concurrency is controlled by `RECEIPT_OCR_UI_MAX_CONCURRENT` (default 6).

  Returns immediately; background tasks log progress."
  ([db app-config receipt-ids]
   (enqueue! db app-config receipt-ids nil))
  ([db app-config receipt-ids opts]
   (let [receipt-ids (into [] (remove nil?) receipt-ids)]
     (doseq [rid receipt-ids]
       (.submit ui-ocr-executor
         ^java.util.concurrent.Callable
         (fn []
           (try
             (log/info "UI OCR task starting" {:receipt-id rid})
             (let [result (runner/run-by-ids! db app-config [rid] opts)]
               (log/info "UI OCR task complete" {:receipt-id rid
                                                 :summary (:summary result)})
               result)
             (catch Exception e
               (log/error e "UI OCR task failed" {:receipt-id rid})
               {:receipt-id rid
                :error (.getMessage e)})))))
     {:queued true
      :queued-count (count receipt-ids)
      :receipt-ids receipt-ids
      :max-concurrent ui-ocr-max-concurrent})))
