(ns app.domain.backend.expenses.workers.receipt-ocr.core
  "Receipt OCR worker implementation.

  This namespace contains the orchestration/state-machine logic.

  This is the concrete implementation namespace (callers should require this
  namespace directly)."
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.shared.type-conversion :as type-conv]
    [taoensso.timbre :as log]))

(defn- process-parse!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR parse skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipt-status/claim-for-parsing! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes]} (common/read-receipt-bytes! receipt opts)
                parse-result (mistral-ocr/ocr-parse! ocr-cfg {:bytes bytes
                                                              :filename (:original_filename receipt)
                                                              :content-type (:content_type receipt)})]
            (receipt-status/store-extraction-results! db receipt-id {:raw_parse_json (:raw parse-result)
                                                                     :parsed_markdown (:parsed-markdown parse-result)})
            (receipt-status/update-status! db receipt-id "parsed" {:error_message nil :error_details nil})
            {:receipt-id receipt-id :stage :parse :result :ok})
          (catch Exception e
            (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Parse failed") (common/safe-ex-data e))
            {:receipt-id receipt-id :stage :parse :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :not-claimed}))))

(defn- process-extract!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)
        auto-retry? (if (contains? opts :review-required-auto-retry?)
                      (boolean (:review-required-auto-retry? opts))
                      true)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR extract skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :missing-api-key})
      (loop [attempt 0]
        (let [res (if-let [_claimed (receipt-status/claim-for-extracting! db receipt-id {:lease-seconds (:lease-seconds opts)})]
                    (try
                      (let [{:keys [bytes]} (common/read-receipt-bytes! receipt opts)
                            extract-result (mistral-ocr/ocr-extract! ocr-cfg {:bytes bytes
                                                                              :content-type (:content_type receipt)})]
                        (extraction/persist-extract-result! db receipt-id extract-result opts))
                      (catch Exception e
                        (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Extraction failed") (common/safe-ex-data e))
                        {:receipt-id receipt-id :stage :extract :result :failed :error (.getMessage e)}))
                    {:receipt-id receipt-id :stage :extract :result :skipped :reason :not-claimed})]
          (if (and auto-retry?
                (zero? attempt)
                (= :ok (:result res))
                (= "review_required" (:status res)))
            (do
              (log/info "Receipt OCR extraction needs review; retrying once" {:receipt-id receipt-id})
              (receipt-status/retry-extraction! db receipt-id)
              (recur (inc attempt)))
            res))))))

(defn process-receipt!
  "Process a single receipt based on its current status.

  If status is 'uploaded', the worker uses extraction (for Mistral, that call
  also returns markdown) to do the full pipeline.

  Returns a small result map describing what happened."
  [db ocr-cfg receipt opts]
  (case (:status receipt)
    "uploaded"
    (process-extract! db ocr-cfg receipt opts)

    "parsing"
    (let [parse-res (process-parse! db ocr-cfg receipt opts)
          ;; Reload status for potential extraction in the same run.
          receipt* (receipt-queries/get-receipt db (:id receipt))]
      (if (and (= :ok (:result parse-res)) (#{"parsed" "extracting"} (:status receipt*)))
        (merge parse-res (process-extract! db ocr-cfg receipt* opts))
        parse-res))

    ("parsed" "extracting")
    (process-extract! db ocr-cfg receipt opts)

    {:receipt-id (:id receipt) :result :skipped :reason :status :status (:status receipt)}))

(defn process-pending!
  "Process pending receipts.

  opts:
  - :max-receipts (default 25)
  - :lease-seconds (default 900)
  - :storage-base-dir (optional)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)

  Returns a summary map."
  ([db app-config]
   (process-pending! db app-config nil))
  ([db app-config {:keys [max-receipts] :as opts}]
   (let [ocr-cfg (mistral-ocr/build-config app-config)
         opts (merge {:max-receipts 25
                      :lease-seconds 900
                      :default-currency "BAM"}
                (or opts {}))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR worker disabled" {})
         {:enabled? false
          :processed 0})
       (let [candidates (receipt-queries/list-pending-for-processing db)
             candidates (take (long (or max-receipts 25)) candidates)
             results (mapv #(process-receipt! db ocr-cfg % opts) candidates)
             summary (->> results
                       (map :result)
                       (frequencies))]
         (log/info "Receipt OCR run complete" {:candidates (count candidates)
                                               :summary summary})
         {:enabled? true
          :candidates (count candidates)
          :summary summary
          :results results})))))

(defn- process-receipts-by-ids-batch!
  [db ocr-cfg receipt-ids reset? opts]
  (let [prepared
        (mapv
          (fn [rid]
            (try
              (when reset?
                (receipt-status/reset-for-ocr! db rid))
              (let [receipt (receipt-queries/get-receipt db rid)]
                (cond
                  (nil? receipt)
                  {:receipt-id rid :stage :extract :result :skipped :reason :not-found}

                  (not (seq (:api-key ocr-cfg)))
                  {:receipt-id rid :stage :extract :result :skipped :reason :missing-api-key}

                  :else
                  (if-let [_claimed (receipt-status/claim-for-extracting! db rid {:lease-seconds (:lease-seconds opts)})]
                    (try
                      (let [{:keys [bytes]} (common/read-receipt-bytes! receipt opts)]
                        {:receipt-id rid
                         :receipt receipt
                         :request {:custom-id (str rid)
                                   :bytes bytes
                                   :content-type (:content_type receipt)}})
                      (catch Exception e
                        (receipt-status/mark-failed! db rid (or (.getMessage e) "Extraction preparation failed") (common/safe-ex-data e))
                        {:receipt-id rid :stage :extract :result :failed :error (.getMessage e)}))
                    {:receipt-id rid :stage :extract :result :skipped :reason :not-claimed})))
              (catch Exception e
                (log/error e "Failed to prepare receipt for batch OCR" {:receipt-id rid})
                {:receipt-id rid :stage :extract :result :failed :error (.getMessage e)})))
          receipt-ids)
        batch-reqs (->> prepared (keep :request) vec)
        batch-ids (set (map :custom-id batch-reqs))]
    (if (empty? batch-reqs)
      (mapv (fn [m] (dissoc m :receipt :request)) prepared)
      (let [batch-res (try
                        (mistral-ocr/ocr-extract-batch! ocr-cfg batch-reqs)
                        (catch Exception e
                          {:exception e}))]
        (if-let [e (:exception batch-res)]
          (do
            (doseq [cid batch-ids]
              (let [rid (type-conv/try-parse-uuid cid)]
                (when rid
                  (receipt-status/mark-failed! db rid (or (.getMessage e) "Batch extraction failed") (common/safe-ex-data e)))))
            (mapv
              (fn [{:keys [receipt-id request] :as m}]
                (if (seq (some-> request :custom-id))
                  {:receipt-id receipt-id :stage :extract :result :failed :error (or (.getMessage e) "Batch extraction failed")}
                  (dissoc m :receipt :request)))
              prepared))
          (mapv
            (fn [{:keys [receipt-id request] :as m}]
              (let [cid (some-> request :custom-id)
                    extract-result (when (seq cid) (get-in batch-res [:results cid]))
                    err (when (seq cid) (get-in batch-res [:errors cid]))]
                (cond
                  (nil? cid)
                  (dissoc m :receipt :request)

                  extract-result
                  (let [res (extraction/persist-extract-result! db receipt-id extract-result opts)]
                    (if (and (not= false (:review-required-auto-retry? opts))
                          (= :ok (:result res))
                          (= "review_required" (:status res)))
                      (do
                        (log/info "Receipt OCR batch extraction needs review; retrying once" {:receipt-id receipt-id})
                        (receipt-status/retry-extraction! db receipt-id)
                        (if-let [receipt* (receipt-queries/get-receipt db receipt-id)]
                          (process-extract! db ocr-cfg receipt* (assoc opts :review-required-auto-retry? false))
                          res))
                      res))

                  :else
                  (do
                    (receipt-status/mark-failed! db receipt-id "Batch extraction failed" (or err {:type :mistral/batch-unknown-error}))
                    {:receipt-id receipt-id :stage :extract :result :failed :error "Batch extraction failed"}))))
            prepared))))))

(defn process-receipts-by-ids!
  "Process receipts by explicit IDs (for UI-triggered OCR).

  Each receipt is first reset to 'uploaded' status (clearing OCR fields) and then
  processed through the OCR pipeline.

  opts:
  - :lease-seconds (default 900)
  - :storage-base-dir (optional, default upload/stripes)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)
  - :reset? (default true) - whether to reset receipt before processing

  Returns a summary map similar to process-pending!."
  ([db app-config receipt-ids]
   (process-receipts-by-ids! db app-config receipt-ids nil))
  ([db app-config receipt-ids {:keys [reset?] :or {reset? true} :as opts}]
   (let [ocr-cfg (mistral-ocr/build-config app-config)
         use-batch? (and reset? (:batch-enabled? ocr-cfg) (> (count receipt-ids) 1))
         opts (merge {:lease-seconds 900
                      :storage-base-dir "upload/stripes"
                      :default-currency "BAM"}
                (dissoc opts :reset?))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR disabled (by-ids)" {:receipt-ids receipt-ids})
         {:enabled? false
          :processed 0
          :receipt-ids receipt-ids})
       (if-not (:api-key ocr-cfg)
         (do
           (log/warn "Receipt OCR skipped: missing API key" {:receipt-ids receipt-ids})
           {:enabled? true
            :error :missing-api-key
            :processed 0
            :receipt-ids receipt-ids})
         (let [results
               (if use-batch?
                 (do
                   (log/info "Receipt OCR by-ids using Mistral Batch API" {:receipt-ids receipt-ids})
                   (process-receipts-by-ids-batch! db ocr-cfg receipt-ids reset? opts))
                 (mapv
                   (fn [rid]
                     (try
                       (let [;; Optionally reset the receipt first
                             _ (when reset?
                                   (receipt-status/reset-for-ocr! db rid))
                               receipt (receipt-queries/get-receipt db rid)]
                         (if receipt
                           (process-receipt! db ocr-cfg receipt opts)
                           {:receipt-id rid :result :skipped :reason :not-found}))
                       (catch Exception e
                         (log/error e "Failed to process receipt" {:receipt-id rid})
                         {:receipt-id rid :result :failed :error (.getMessage e)})))
                   receipt-ids))
               summary (->> results
                         (map :result)
                         (frequencies))]
           (log/info "Receipt OCR by-ids complete" {:receipt-ids receipt-ids
                                                    :summary summary
                                                    :batch? use-batch?})
           {:enabled? true
            :processed (count results)
            :summary summary
            :results results
            :receipt-ids receipt-ids}))))))
