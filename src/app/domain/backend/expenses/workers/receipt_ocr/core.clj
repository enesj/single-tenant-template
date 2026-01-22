(ns app.domain.backend.expenses.workers.receipt-ocr.core
  "Receipt OCR worker implementation.

  This namespace contains the orchestration/state-machine logic.

  This is the concrete implementation namespace (callers should require this
  namespace directly)."
  (:require
    [app.domain.backend.expenses.integrations.cerebras :as cerebras]
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn- user-allows-receipt-refine?
  [db receipt]
  (try
    (let [user-id (:user_id receipt)]
      (when user-id
        (let [persisted (user-expense-settings/get-user-expense-settings db user-id)
              effective (user-expense-settings/effective-settings persisted)]
          (true? (:receipt-refine-enabled effective)))))
    (catch Exception e
      (log/warn e "Failed to load user expense settings; skipping receipt refine"
        {:receipt-id (:id receipt)
         :user-id (:user_id receipt)})
      false)))

(defn- maybe-refine-with-cerebras
  [db receipt extract-result {:keys [cerebras-cfg] :as _opts}]
  (let [receipt-id (:id receipt)
        markdown (:parsed-markdown extract-result)
        user-id (:user_id receipt)
        user-enabled? (user-allows-receipt-refine? db receipt)
        has-markdown? (boolean (seq (some-> markdown str str/trim)))
        cerebras-cfg? (map? cerebras-cfg)
        has-api-key? (boolean (and cerebras-cfg? (seq (:api-key cerebras-cfg))))
        reasons (cond-> []
                  (not user-enabled?) (conj :user-disabled)
                  (not cerebras-cfg?) (conj :missing-cerebras-config)
                  (and cerebras-cfg? (not (seq (:api-key cerebras-cfg)))) (conj :missing-api-key)
                  (not has-markdown?) (conj :blank-markdown))]
    (if (seq reasons)
      (do
        ;; Log skip reasons at INFO when user has opted in (otherwise this would be noisy).
        (when user-enabled?
          (log/info "Cerebras receipt refine skipped"
            {:receipt-id receipt-id
             :user-id user-id
             :reasons reasons
             :has-api-key? has-api-key?
             :has-markdown? has-markdown?}))
        extract-result)
      (try
        (log/info "Cerebras receipt refine starting" {:receipt-id receipt-id :user-id user-id})
        (let [started (System/nanoTime)
              refine (cerebras/refine-receipt-markdown! cerebras-cfg markdown)
              duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
          (log/info "Cerebras receipt refine applied"
            {:receipt-id receipt-id
             :user-id user-id
             :duration-ms duration-ms
             :model (:model refine)
             :has-extraction? (boolean (:extraction refine))})
          (cond-> extract-result
            (:extraction refine) (assoc :extraction (:extraction refine))
            refine (assoc :llm_refine refine)))
        (catch Exception e
          (let [details (common/safe-ex-data e)]
            (log/warn e "Cerebras receipt refine failed; continuing without refine"
              (cond-> {:receipt-id receipt-id
                       :user-id user-id
                       :error_message (or (.getMessage e) (str (class e)))}
                (seq details) (assoc :error_details details))))
          extract-result)))))

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
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR extract skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipt-status/claim-for-extracting! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes]} (common/read-receipt-bytes! receipt opts)
                extract-result (mistral-ocr/ocr-extract! ocr-cfg {:bytes bytes
                                                                  :content-type (:content_type receipt)})]
            (extraction/persist-extract-result! db receipt-id (maybe-refine-with-cerebras db receipt extract-result opts)
              (assoc opts :auto-post-after-upload? (:auto-post-after-upload? ocr-cfg))))
          (catch Exception e
            (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Extraction failed") (common/safe-ex-data e))
            {:receipt-id receipt-id :stage :extract :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :not-claimed}))))

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
         cerebras-cfg (cerebras/build-config app-config)
         opts (merge {:max-receipts 25
                      :lease-seconds 900
                      :default-currency "BAM"
                      :cerebras-cfg cerebras-cfg}
                (or opts {}))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR worker disabled" {:enabled? false})
         {:enabled? false :processed 0})
       (if-not (:api-key ocr-cfg)
         (do
           (log/warn "Receipt OCR worker skipped: missing API key")
           {:enabled? true :error :missing-api-key :processed 0})
         (let [receipts (receipt-queries/list-pending-for-processing db {:limit (or max-receipts 25)})
               results (mapv (fn [receipt]
                               (try
                                 (process-receipt! db ocr-cfg receipt opts)
                                 (catch Exception e
                                   (log/error e "Failed to process receipt" {:receipt-id (:id receipt)})
                                   {:receipt-id (:id receipt) :result :failed :error (.getMessage e)})))
                         receipts)
               summary (->> results (map :result) (frequencies))]
           (log/info "Receipt OCR worker complete" {:summary summary})
           {:enabled? true
            :processed (count results)
            :summary summary
            :results results}))))))

(defn- process-receipts-by-ids-batch!
  [db ocr-cfg receipt-ids reset? opts]
  (let [prepared (mapv
                   (fn [rid]
                     (try
                       (let [receipt0 (receipt-queries/get-receipt db rid)
                             _ (when (and reset? receipt0)
                                 (receipt-status/reset-for-ocr! db rid))
                             receipt (or (receipt-queries/get-receipt db rid) receipt0)]
                         (cond
                           (nil? receipt)
                           {:receipt-id rid :stage :extract :result :skipped :reason :not-found}

                           (not (seq (:api-key ocr-cfg)))
                           {:receipt-id rid :stage :extract :result :skipped :reason :missing-api-key}

                           :else
                           (if-let [_claimed (receipt-status/claim-for-extracting! db rid {:lease-seconds (:lease-seconds opts)})]
                             (try
                               (let [{:keys [bytes content-type]} (common/read-receipt-bytes! receipt opts)
                                     request {:custom-id (str rid)
                                              :bytes bytes
                                              :content-type content-type}]
                                 {:receipt-id rid :receipt receipt :request request})
                               (catch Exception e
                                 ;; IMPORTANT: we already claimed (status -> extracting). If we fail
                                 ;; to prepare the request (e.g. missing file), we must fail the
                                 ;; receipt so it doesn't appear stuck forever.
                                 (receipt-status/mark-failed!
                                   db
                                   rid
                                   (or (.getMessage e) "Failed to prepare receipt for batch OCR")
                                   (common/safe-ex-data e))
                                 {:receipt-id rid
                                  :stage :extract
                                  :result :failed
                                  :error (or (.getMessage e) "Failed to prepare receipt for batch OCR")}))
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
                  (try
                    (let [receipt (or (:receipt m) (receipt-queries/get-receipt db receipt-id))
                          extract-result (if receipt
                                           (maybe-refine-with-cerebras db receipt extract-result opts)
                                           extract-result)]
                      (extraction/persist-extract-result! db receipt-id extract-result
                        (assoc opts :auto-post-after-upload? (:auto-post-after-upload? ocr-cfg))))
                    (catch Exception e
                      (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Persist failed") (common/safe-ex-data e))
                      {:receipt-id receipt-id :stage :extract :result :failed :error (or (.getMessage e) "Persist failed")}))

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
         cerebras-cfg (cerebras/build-config app-config)
         use-batch? (and reset? (:batch-enabled? ocr-cfg) (> (count receipt-ids) 1))
         opts (merge {:lease-seconds 900
                      :storage-base-dir "upload/stripes"
                      :default-currency "BAM"
                      :cerebras-cfg cerebras-cfg}
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
