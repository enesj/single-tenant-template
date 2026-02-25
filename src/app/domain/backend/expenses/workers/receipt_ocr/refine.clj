(ns app.domain.backend.expenses.workers.receipt-ocr.refine
  "Cerebras-based receipt refinement: eligibility checks, single-result refine,
  parallel batch refine, and auto-post logic."
  (:require
    [app.domain.backend.expenses.integrations.cerebras :as cerebras]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.util.concurrent Executors TimeUnit TimeoutException]))

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
  [db receipt-id]
  (when-let [ctx (receipt-refine-context db receipt-id)]
    (try
      (receipt-status/store-receipt-refine-context! db receipt-id ctx)
      (catch Exception e
        (log/warn e "Failed to persist receipt refine context" {:receipt-id receipt-id})))
    ctx))

;; Shared helper: load one boolean flag from effective user expense settings.
;; Both refine and auto-post eligibility use the same lookup pattern; this keeps
;; their fallback behaviour consistent.
(defn- load-user-boolean-setting
  [db receipt setting-key log-msg]
  (try
    (let [user-id (:user_id receipt)]
      (when user-id
        (let [persisted (user-expense-settings/get-user-expense-settings db user-id)
              effective (user-expense-settings/effective-settings persisted)]
          (true? (get effective setting-key)))))
    (catch Exception e
      (log/warn e log-msg
        {:receipt-id (:id receipt)
         :user-id (:user_id receipt)})
      false)))

(defn- user-allows-receipt-refine?
  [db receipt]
  (load-user-boolean-setting db receipt :receipt-refine-enabled
    "Failed to load user expense settings; skipping receipt refine"))

(defn user-allows-auto-post?
  "Return true when the user has enabled auto-post-after-upload."
  [db receipt]
  (load-user-boolean-setting db receipt :auto-post-after-upload-enabled
    "Failed to load user expense settings; skipping auto-post"))

(defn receipt-eligible-for-refine?
  "Return true when the receipt is eligible for Cerebras refinement."
  [db receipt opts]
  (or (true? (:force-refine? opts))
    (user-allows-receipt-refine? db receipt)))

(defn- maybe-refine-with-cerebras
  [db receipt extract-result {:keys [cerebras-cfg] :as _opts}]
  (let [receipt-id (:id receipt)
        markdown (:parsed-markdown extract-result)
        user-id (:user_id receipt)
        filename (:original_filename receipt)
        user-enabled? (receipt-eligible-for-refine? db receipt _opts)
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

(defn maybe-refine-review-required
  "Run Cerebras refine on a review-required result, then re-persist if improved."
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

(defn strip-refine-metadata
  "Remove transient refine context keys from a result map."
  [result]
  (dissoc result :receipt :extract-result))

(defn refine-review-required-results!
  "Run parallel Cerebras refine for all review-required results in the batch."
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
        sample-ids (->> refineable (map :receipt-id) (take 5) vec)
        eligible-refine-count (if db
                                (->> refineable
                                  (filter (fn [{:keys [receipt]}]
                                            (receipt-eligible-for-refine? db receipt refine-opts)))
                                  count)
                                0)
        has-eligible-refine? (pos? eligible-refine-count)]
    (if (zero? total)
      (do
        (when (pos? review-required-count)
          (log/info "Cerebras parallel refine skipped"
            {:batch-id batch-id
             :review-required-count review-required-count
             :missing-context missing-context
             :total-results (count results)
             :eligible-refine-count eligible-refine-count}))
        results)
      (let [started (System/nanoTime)
            pool (Executors/newFixedThreadPool max-concurrent)]
        (try
          (if has-eligible-refine?
            (log/info "Cerebras parallel refine starting"
              {:batch-id batch-id
               :count total
               :eligible-refine-count eligible-refine-count
               :max-concurrent max-concurrent
               :timeout-ms timeout-ms
               :receipt-ids-sample sample-ids
               :receipt-ids-sample-count (count sample-ids)})
            (log/info "Review-required post-process starting (no eligible refine)"
              {:batch-id batch-id
               :count total
               :eligible-refine-count eligible-refine-count
               :max-concurrent max-concurrent
               :timeout-ms timeout-ms
               :receipt-ids-sample sample-ids
               :receipt-ids-sample-count (count sample-ids)}))
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
                                              (log/warn ce "Failed to clear refine_pending after timeout"
                                                {:batch-id batch-id
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
                                              (log/warn ce "Failed to clear refine_pending after failure"
                                                {:batch-id batch-id
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
                refined-count (count (filter (fn [result]
                                               (map? (get-in result [:extract-result :llm_refine])))
                                       results*))
                duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
            (if has-eligible-refine?
              (log/info "Cerebras parallel refine complete"
                {:batch-id batch-id
                 :count (count refine-map)
                 :total-count total
                 :eligible-refine-count eligible-refine-count
                 :refined-count refined-count
                 :duration-ms duration-ms})
              (log/info "Review-required post-process complete (no eligible refine)"
                {:batch-id batch-id
                 :count (count refine-map)
                 :total-count total
                 :eligible-refine-count eligible-refine-count
                 :refined-count refined-count
                 :duration-ms duration-ms}))
            results*)
          (finally
            (.shutdown pool)
            (.awaitTermination pool 10 TimeUnit/SECONDS)))))))

(defn auto-post-extracted-results!
  "Persist extraction results for all auto-postable receipts in the batch."
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
