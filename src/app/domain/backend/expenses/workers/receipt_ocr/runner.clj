(ns app.domain.backend.expenses.workers.receipt-ocr.runner
  "Receipt OCR processing flows: uploaded/parsing/parsed/extracting and batch."
  (:require
    [app.domain.backend.expenses.integrations.cerebras :as cerebras]
    [app.domain.backend.expenses.integrations.ocr-provider :as ocr-provider]
    [app.domain.backend.expenses.services.global-settings :as global-settings]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.domain.backend.expenses.workers.receipt-ocr.provider :as provider]
    [app.domain.backend.expenses.workers.receipt-ocr.refine :as refine]
    [taoensso.timbre :as log]))

(defn- build-provider-deps
  "Build the shared provider-dependency map for worker opts."
  [db app-config ocr-cfg]
  (let [global (try
                 (global-settings/get-global-settings db)
                 (catch Exception e
                   (log/warn e "Failed to load global settings for receipt OCR worker")
                   nil))]
    {:cerebras-cfg (assoc (cerebras/build-config app-config) :db db)
     :places-cfg (assoc (places-api/build-config app-config) :db db)
     :auto-post-after-upload? (:auto-post-after-upload? ocr-cfg)
     :global-auto-publish-after-upload? (boolean (:auto-publish-after-upload global))
     :ai-receipt-enhancement? (boolean (:ai-receipt-enhancement global))
     :default-currency (or (:default-currency global) "BAM")}))

(defn- process-parse!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR parse skipped: missing API key"
          {:receipt-id receipt-id
           :provider (provider/provider-key ocr-cfg)
           :provider-name (:provider-name ocr-cfg)})
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
                parse-result (provider/parse-with-provider!
                               ocr-cfg
                               {:bytes bytes*
                                :filename (:original_filename receipt)
                                :content-type content-type*})]
            (receipt-status/store-extraction-results! db receipt-id {:parsed_markdown (:parsed-markdown parse-result)})
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
        (log/warn "Receipt OCR extract skipped: missing API key"
          {:receipt-id receipt-id
           :provider (provider/provider-key ocr-cfg)
           :provider-name (:provider-name ocr-cfg)})
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
                extract-result (provider/extract-with-provider!
                                 ocr-cfg
                                 {:bytes bytes*
                                  :filename (:original_filename receipt)
                                  :content-type content-type*})
                global-auto-post? (:global-auto-publish-after-upload? opts)
                auto-post-enabled? (and (:auto-post-after-upload? ocr-cfg) global-auto-post?)
                opts* (assoc opts :auto-post-after-upload? auto-post-enabled?)
                persist-result (-> (extraction/persist-extract-result! db receipt-id extract-result opts*)
                                 (assoc :auto-post-after-upload? auto-post-enabled?))]
            (if (:defer-refine? opts*)
              (assoc persist-result :receipt receipt :extract-result extract-result)
              (refine/maybe-refine-review-required db receipt extract-result persist-result opts*)))
          (catch Exception e
            (receipt-status/mark-failed! db receipt-id (or (.getMessage e) "Extraction failed") (common/safe-ex-data e))
            {:receipt-id receipt-id :stage :extract :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :not-claimed}))))

(defn run-receipt!
  "Process a single receipt based on its current status.

  If status is 'uploaded', the worker uses provider extraction (which may also
  include markdown) to do the full pipeline.

  Returns a small result map describing what happened."
  [db ocr-cfg receipt opts]
  (case (:status receipt)
    "uploaded"
    (process-extract! db ocr-cfg receipt opts)

    "parsing"
    (let [parse-res (process-parse! db ocr-cfg receipt opts)
          receipt* (receipt-queries/get-receipt db (:id receipt))]
      (if (and (= :ok (:result parse-res)) (#{"parsed" "extracting"} (:status receipt*)))
        (merge parse-res (process-extract! db ocr-cfg receipt* opts))
        parse-res))

    ("parsed" "extracting")
    (process-extract! db ocr-cfg receipt opts)

    {:receipt-id (:id receipt) :result :skipped :reason :status :status (:status receipt)}))

(defn run-pending!
  "Process pending receipts.

  opts:
  - :max-receipts (default 25)
  - :lease-seconds (default 900)
  - :storage-base-dir (optional)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)

  Returns a summary map."
  ([db app-config]
   (run-pending! db app-config nil))
  ([db app-config {:keys [max-receipts] :as opts}]
   (let [ocr-cfg (ocr-provider/build-provider app-config {:db db})
         deps (build-provider-deps db app-config ocr-cfg)
         opts (merge {:max-receipts 25
                      :lease-seconds 900
                      :default-currency "BAM"}
                deps
                (or opts {}))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR worker disabled"
           {:enabled? false
            :provider (provider/provider-key ocr-cfg)})
         {:enabled? false :processed 0})
       (if-not (:api-key ocr-cfg)
         (do
           (log/warn "Receipt OCR worker skipped: missing API key"
             {:provider (provider/provider-key ocr-cfg)})
           {:enabled? true :error :missing-api-key :processed 0})
         (let [receipts (receipt-queries/list-pending-for-processing db {:limit (or max-receipts 25)})
               defer-refine? (if (contains? opts :defer-refine?)
                               (:defer-refine? opts)
                               true)
               opts* (assoc opts :defer-refine? defer-refine?)
               results0 (mapv (fn [receipt]
                                (try
                                  (run-receipt! db ocr-cfg receipt opts*)
                                  (catch Exception e
                                    (log/error e "Failed to process receipt" {:receipt-id (:id receipt)})
                                    {:receipt-id (:id receipt) :result :failed :error (.getMessage e)})))
                          receipts)
               results1 (if defer-refine?
                          (refine/refine-review-required-results! db opts* results0)
                          results0)
               results2 (if defer-refine?
                          (refine/auto-post-extracted-results! db opts* results1)
                          results1)
               results3 (mapv refine/strip-refine-metadata results2)
               summary (->> results3 (map :result) (frequencies))]
           (log/info "Receipt OCR worker complete" {:summary summary})
           {:enabled? true
            :processed (count results3)
            :summary summary
            :results results3}))))))

(defn run-by-ids!
  "Process receipts by explicit IDs (for UI-triggered OCR).

  Each receipt is first reset to 'uploaded' status (clearing OCR fields) and then
  processed through the OCR pipeline.

  opts:
  - :lease-seconds (default 900)
  - :storage-base-dir (optional, default upload/stripes)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)
  - :reset? (default true) - whether to reset receipt before processing

  Returns a summary map similar to run-pending!."
  ([db app-config receipt-ids]
   (run-by-ids! db app-config receipt-ids nil))
  ([db app-config receipt-ids {:keys [reset?] :or {reset? true} :as opts}]
   (let [ocr-cfg (ocr-provider/build-provider app-config {:db db})
         deps (build-provider-deps db app-config ocr-cfg)
         defer-refine? (if (contains? opts :defer-refine?)
                         (:defer-refine? opts)
                         true)
         opts (merge {:lease-seconds 900
                      :storage-base-dir "upload/stripes"
                      :default-currency "BAM"
                      :defer-refine? defer-refine?
                      :force-refine? true}
                deps
                (dissoc opts :reset?))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR disabled (by-ids)"
           {:receipt-ids receipt-ids
            :provider (provider/provider-key ocr-cfg)})
         {:enabled? false
          :processed 0
          :receipt-ids receipt-ids})
       (if-not (:api-key ocr-cfg)
         (do
           (log/warn "Receipt OCR skipped: missing API key"
             {:receipt-ids receipt-ids
              :provider (provider/provider-key ocr-cfg)})
           {:enabled? true
            :error :missing-api-key
            :processed 0
            :receipt-ids receipt-ids})
         (let [results0 (mapv
                          (fn [rid]
                            (try
                              (let [_ (when reset?
                                        (receipt-status/reset-for-ocr! db rid))
                                    receipt (receipt-queries/get-receipt db rid)]
                                (if receipt
                                  (run-receipt! db ocr-cfg receipt opts)
                                  {:receipt-id rid :result :skipped :reason :not-found}))
                              (catch Exception e
                                (log/error e "Failed to process receipt" {:receipt-id rid})
                                {:receipt-id rid :result :failed :error (.getMessage e)})))
                          receipt-ids)
               results1 (if defer-refine?
                          (refine/refine-review-required-results! db opts results0)
                          results0)
               results2 (if defer-refine?
                          (refine/auto-post-extracted-results! db opts results1)
                          results1)
               results3 (mapv refine/strip-refine-metadata results2)
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
