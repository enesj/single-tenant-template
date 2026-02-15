(ns app.domain.backend.expenses.workers.receipt-ocr.core
  "Receipt OCR worker implementation.

  This namespace contains the orchestration/state-machine logic.

  This is the concrete implementation namespace (callers should require this
  namespace directly)."
  (:require
    [app.domain.backend.expenses.integrations.cerebras :as cerebras]
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.util.concurrent Executors TimeUnit TimeoutException]))

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

(defn- env-truthy?
  [k]
  (let [v (some-> (System/getenv k) str str/trim str/lower-case)]
    (contains? #{"1" "true" "yes" "y" "on"} v)))

(defonce ^:private receipt-refine-include-store-context?
  (env-truthy? "RECEIPT_REFINE_INCLUDE_STORE_CONTEXT"))

(defn- receipt-refine-context
  [db receipt-id]
  (try
    (when-let [ctx (receipt-queries/get-receipt-refine-context db receipt-id)]
      (let [supplier-key (some-> (:supplier_key ctx) str str/trim not-empty)
            store-key (some-> (:store_key ctx) str str/trim not-empty)
            fingerprint (when (and supplier-key store-key)
                          (str supplier-key "/" store-key))]
        (cond-> ctx
          fingerprint (assoc :store_fingerprint fingerprint))))
    (catch Exception e
      (log/warn e "Failed to load receipt refine context" {:receipt-id receipt-id})
      nil)))

(defn- store-receipt-refine-context-best-effort!
  "Store receipt refine context into `receipts.raw_extract_json`.

  Returns the context map (possibly enriched with :store_fingerprint), or nil."
  [db receipt-id]
  (when-let [ctx (receipt-refine-context db receipt-id)]
    (try
      (receipt-status/store-receipt-refine-context! db receipt-id ctx)
      (catch Exception e
        (log/warn e "Failed to persist receipt refine context" {:receipt-id receipt-id})))
    ctx))

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
        filename (:original_filename receipt)
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
             :filename filename
             :reasons reasons
             :has-api-key? has-api-key?
             :has-markdown? has-markdown?}))
        extract-result)
      (let [ctx (store-receipt-refine-context-best-effort! db receipt-id)
            supplier-key (some-> (:supplier_key ctx) str str/trim not-empty)
            store-key (some-> (:store_key ctx) str str/trim not-empty)
            fingerprint (some-> (:store_fingerprint ctx) str str/trim not-empty)
            refine-opts (when (and receipt-refine-include-store-context? (map? ctx))
                          {:context ctx})]
        (try
          (log/info "Cerebras receipt refine starting"
            (cond-> {:receipt-id receipt-id
                     :user-id user-id
                     :filename filename
                     :include-store-context? receipt-refine-include-store-context?}
              supplier-key (assoc :supplier-key supplier-key)
              store-key (assoc :store-key store-key)
              fingerprint (assoc :fingerprint fingerprint)))
          (let [started (System/nanoTime)
                refine (if refine-opts
                         (cerebras/refine-receipt-markdown! cerebras-cfg markdown refine-opts)
                         (cerebras/refine-receipt-markdown! cerebras-cfg markdown))
                duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
            (log/info "Cerebras receipt refine applied"
              (cond-> {:receipt-id receipt-id
                       :user-id user-id
                       :filename filename
                       :duration-ms duration-ms
                       :model (:model refine)
                       :has-extraction? (boolean (:extraction refine))}
                supplier-key (assoc :supplier-key supplier-key)
                store-key (assoc :store-key store-key)
                fingerprint (assoc :fingerprint fingerprint)))
            (cond-> extract-result
              (:extraction refine) (assoc :extraction (:extraction refine))
              refine (assoc :llm_refine refine)))
          (catch Exception e
            ;; Keep this best-effort: we still want the fingerprint context persisted
            ;; for later inspection even when refine fails.
            (when db
              (store-receipt-refine-context-best-effort! db receipt-id))
            (let [details (common/safe-ex-data e)]
              (log/warn e "Cerebras receipt refine failed; continuing without refine"
                (cond-> {:receipt-id receipt-id
                         :user-id user-id
                         :filename filename
                         :error_message (or (.getMessage e) (str (class e)))}
                  (seq details) (assoc :error_details details))))
            extract-result))))))

(defn- maybe-refine-review-required
  [db receipt extract-result persist-result opts]
  (let [review-required? (true? (:review-required? persist-result))
        auto-post-present? (contains? persist-result :auto-post-after-upload?)
        auto-post-after-upload? (:auto-post-after-upload? persist-result)
        skip-auto-post? (true? (:skip-auto-post? opts))]
    (if (and receipt review-required?)
      (let [refined (maybe-refine-with-cerebras db receipt extract-result opts)]
        (if (map? (:llm_refine refined))
          (let [persist-opts (if skip-auto-post?
                               (assoc opts :auto-post-after-upload? false)
                               opts)
                updated (extraction/persist-extract-result! db (:id receipt) refined persist-opts)]
            ;; `persist-extract-result!` rewrites raw_extract_json; ensure the
            ;; refine context stays persisted for later inspection.
            (when (and db (:id receipt))
              (store-receipt-refine-context-best-effort! db (:id receipt)))
            (cond-> (assoc updated :receipt receipt :extract-result refined)
              auto-post-present? (assoc :auto-post-after-upload? auto-post-after-upload?)))
          (do
            (when (and db (true? (:clear-refine-pending? opts)))
              (try
                (receipt-status/clear-refine-pending! db (:id receipt))
                (catch Exception e
                  (log/warn e "Failed to clear refine_pending" {:receipt-id (:id receipt)}))))
            persist-result)))
      persist-result)))

(defn- result-receipt-id
  [result]
  (or (:receipt-id result)
    (some-> result :receipt :id)))

(defn- strip-refine-metadata
  [result]
  (dissoc result :receipt :extract-result))

(defn- refine-review-required-results!
  [db opts results]
  (let [batch-id (str (java.util.UUID/randomUUID))
        refine-opts (assoc (dissoc opts :defer-refine? :skip-auto-post?)
                      :skip-auto-post? true
                      :clear-refine-pending? true
                      :refine-batch-id batch-id)
        cerebras-cfg (:cerebras-cfg opts)
        max-concurrent (long (max 1 (or (:refine-concurrency cerebras-cfg) 5)))
        timeout-ms (long (or (:refine-timeout-ms cerebras-cfg)
                           (:socket-timeout-ms cerebras-cfg)
                           60000))
        refineable (->> results
                     (keep (fn [result]
                             (let [receipt (:receipt result)
                                   extract-result (:extract-result result)
                                   receipt-id (result-receipt-id result)]
                               (when (and receipt-id receipt extract-result (true? (:review-required? result)))
                                 {:receipt-id receipt-id
                                  :receipt receipt
                                  :extract-result extract-result
                                  :result result})))))
        total (count refineable)
        review-required-count (count (filter (fn [result]
                                               (true? (:review-required? result)))
                                       results))
        missing-context (long (max 0 (- review-required-count total)))
        sample-ids (->> refineable (map :receipt-id) (take 5) vec)]
    (if (zero? total)
      (do
        (when (pos? review-required-count)
          (log/info "Cerebras parallel refine skipped"
            {:batch-id batch-id
             :review-required-count review-required-count
             :missing-context missing-context
             :total-results (count results)}))
        results)
      (let [started (System/nanoTime)
            pool (Executors/newFixedThreadPool max-concurrent)]
        (try
          (log/info "Cerebras parallel refine starting"
            {:batch-id batch-id
             :count total
             :max-concurrent max-concurrent
             :timeout-ms timeout-ms
             :receipt-ids-sample sample-ids
             :receipt-ids-sample-count (count sample-ids)})
          (let [futures (mapv
                          (fn [{:keys [receipt-id receipt extract-result result]}]
                            {:receipt-id receipt-id
                             :future (.submit pool
                                       ^java.util.concurrent.Callable
                                       (fn []
                                         [receipt-id (maybe-refine-review-required db receipt extract-result result refine-opts)]))})
                          refineable)
                refine-map (->> futures
                             (map (fn [{:keys [receipt-id future]}]
                                    (try
                                      (.get future timeout-ms TimeUnit/MILLISECONDS)
                                      (catch TimeoutException e
                                        (try (.cancel future true) (catch Exception _))
                                        (when db
                                          (try
                                            (receipt-status/clear-refine-pending! db receipt-id)
                                            (catch Exception ce
                                              (log/warn ce "Failed to clear refine_pending after timeout" {:batch-id batch-id
                                                                                                           :receipt-id receipt-id}))))
                                        (log/warn e "Cerebras refine timed out (parallel)"
                                          {:batch-id batch-id
                                           :receipt-id receipt-id
                                           :timeout-ms timeout-ms})
                                        nil)
                                      (catch Exception e
                                        (when db
                                          (try
                                            (receipt-status/clear-refine-pending! db receipt-id)
                                            (catch Exception ce
                                              (log/warn ce "Failed to clear refine_pending after failure" {:batch-id batch-id
                                                                                                           :receipt-id receipt-id}))))
                                        (log/warn e "Cerebras refine failed (parallel)"
                                          {:batch-id batch-id
                                           :receipt-id receipt-id})
                                        nil))))
                             (keep identity)
                             (into {}))
                results* (mapv (fn [result]
                                 (let [receipt-id (result-receipt-id result)]
                                   (if receipt-id
                                     (get refine-map receipt-id result)
                                     result)))
                           results)
                duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
            (log/info "Cerebras parallel refine complete"
              {:batch-id batch-id
               :count (count refine-map)
               :total-count total
               :duration-ms duration-ms})
            results*)
          (finally
            (.shutdown pool)
            (.awaitTermination pool 10 TimeUnit/SECONDS)))))))

(defn- auto-post-extracted-results!
  [db opts results]
  (let [post-opts (dissoc opts :defer-refine? :skip-auto-post?)
        post-map (->> results
                   (keep (fn [result]
                           (let [receipt-id (result-receipt-id result)
                                 receipt (:receipt result)
                                 extract-result (:extract-result result)
                                 auto-post? (if (contains? result :auto-post-after-upload?)
                                              (:auto-post-after-upload? result)
                                              (get opts :auto-post-after-upload? true))]
                             (when (and receipt-id receipt extract-result
                                     (= :ok (:result result))
                                     (= :extract (:stage result))
                                     (not (:review-required? result))
                                     (true? auto-post?))
                               (let [updated (extraction/persist-extract-result!
                                               db
                                               receipt-id
                                               extract-result
                                               (assoc post-opts :auto-post-after-upload? auto-post?))]
                                 [receipt-id updated])))))
                   (into {}))]
    (mapv (fn [result]
            (let [receipt-id (result-receipt-id result)]
              (if (and receipt-id (contains? post-map receipt-id))
                (get post-map receipt-id)
                result)))
      results)))

(defn- process-parse!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR parse skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipt-status/claim-for-parsing! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes path]} (common/read-receipt-bytes! receipt opts)
                prepared (image-preprocess/prepare-for-ocr {:bytes bytes
                                                            :path path
                                                            :content-type (:content_type receipt)
                                                            :filename (:original_filename receipt)})
                bytes* (:bytes prepared)
                content-type* (or (:content-type prepared) (:content_type receipt))
                parse-result (mistral-ocr/ocr-parse! ocr-cfg {:bytes bytes*
                                                              :filename (:original_filename receipt)
                                                              :content-type content-type*})]
            (receipt-status/store-extraction-results! db receipt-id {:parsed_markdown (:parsed-markdown parse-result)})
            (receipt-status/update-status! db receipt-id "parsed" {:error_message nil :error_details nil})
            {:receipt-id receipt-id :stage :parse :result :ok})
          (catch Exception e
            (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Parse failed") (common/safe-ex-data e))
            {:receipt-id receipt-id :stage :parse :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :not-claimed}))))

(defn- user-allows-auto-post?
  [db receipt]
  (try
    (let [user-id (:user_id receipt)]
      (when user-id
        (let [persisted (user-expense-settings/get-user-expense-settings db user-id)
              effective (user-expense-settings/effective-settings persisted)]
          (true? (:auto-post-after-upload-enabled effective)))))
    (catch Exception e
      (log/warn e "Failed to load user expense settings; skipping auto-post"
        {:receipt-id (:id receipt)
         :user-id (:user_id receipt)})
      false)))

(defn- process-extract!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR extract skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipt-status/claim-for-extracting! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes path]} (common/read-receipt-bytes! receipt opts)
                prepared (image-preprocess/prepare-for-ocr {:bytes bytes
                                                            :path path
                                                            :content-type (:content_type receipt)
                                                            :filename (:original_filename receipt)})
                bytes* (:bytes prepared)
                content-type* (or (:content-type prepared) (:content_type receipt))
                extract-result (mistral-ocr/ocr-extract! ocr-cfg {:bytes bytes*
                                                                  :content-type content-type*})
                user-auto-post? (user-allows-auto-post? db receipt)
                auto-post-enabled? (and (:auto-post-after-upload? ocr-cfg) user-auto-post?)
                opts* (assoc opts :auto-post-after-upload? auto-post-enabled?)
                persist-result (-> (extraction/persist-extract-result! db receipt-id extract-result opts*)
                                 (assoc :auto-post-after-upload? auto-post-enabled?))]
            (if (:defer-refine? opts*)
              (assoc persist-result :receipt receipt :extract-result extract-result)
              (maybe-refine-review-required db receipt extract-result persist-result opts*)))
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
         places-cfg (places-api/build-config app-config)
         opts (merge {:max-receipts 25
                      :lease-seconds 900
                      :default-currency "BAM"
                      :cerebras-cfg cerebras-cfg
                      :places-cfg places-cfg
                      :auto-post-after-upload? (:auto-post-after-upload? ocr-cfg)}
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
               defer-refine? (if (contains? opts :defer-refine?)
                               (:defer-refine? opts)
                               true)
               opts* (assoc opts :defer-refine? defer-refine?)
               results0 (mapv (fn [receipt]
                                (try
                                  (process-receipt! db ocr-cfg receipt opts*)
                                  (catch Exception e
                                    (log/error e "Failed to process receipt" {:receipt-id (:id receipt)})
                                    {:receipt-id (:id receipt) :result :failed :error (.getMessage e)})))
                          receipts)
               results1 (if defer-refine?
                          (refine-review-required-results! db opts* results0)
                          results0)
               results2 (if defer-refine?
                          (auto-post-extracted-results! db opts* results1)
                          results1)
               results3 (mapv strip-refine-metadata results2)
               summary (->> results3 (map :result) (frequencies))]
           (log/info "Receipt OCR worker complete" {:summary summary})
           {:enabled? true
            :processed (count results3)
            :summary summary
            :results results3}))))))

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
         places-cfg (places-api/build-config app-config)
         defer-refine? (if (contains? opts :defer-refine?)
                         (:defer-refine? opts)
                         true)
         opts (merge {:lease-seconds 900
                      :storage-base-dir "upload/stripes"
                      :default-currency "BAM"
                      :cerebras-cfg cerebras-cfg
                      :places-cfg places-cfg
                      :auto-post-after-upload? (:auto-post-after-upload? ocr-cfg)
                      :defer-refine? defer-refine?}
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
         (let [results0 (mapv
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
                          receipt-ids)
               results1 (if defer-refine?
                          (refine-review-required-results! db opts results0)
                          results0)
               results2 (if defer-refine?
                          (auto-post-extracted-results! db opts results1)
                          results1)
               results3 (mapv strip-refine-metadata results2)
               summary (->> results3
                         (map :result)
                         (frequencies))]
           (log/info "Receipt OCR by-ids complete" {:receipt-ids receipt-ids
                                                    :summary summary})
           {:enabled? true
            :processed (count results3)
            :summary summary
            :results results3
            :receipt-ids receipt-ids}))))))

(defn queue-ui-ocr!
  "Queue receipt OCR work from UI endpoints.

  UI endpoints can submit multiple receipt IDs (e.g. multi-file upload). We run
  each receipt in the shared bounded executor so the user sees parallel progress
  without overwhelming the OCR provider.

  Concurrency is controlled by `RECEIPT_OCR_UI_MAX_CONCURRENT` (default 6).

  Returns immediately; background tasks log progress."
  ([db app-config receipt-ids]
   (queue-ui-ocr! db app-config receipt-ids nil))
  ([db app-config receipt-ids opts]
   (let [receipt-ids (into [] (remove nil?) receipt-ids)]
     (doseq [rid receipt-ids]
       (.submit ui-ocr-executor
         ^java.util.concurrent.Callable
         (fn []
           (try
             (log/info "UI OCR task starting" {:receipt-id rid})
             (let [result (process-receipts-by-ids! db app-config [rid] opts)]
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

