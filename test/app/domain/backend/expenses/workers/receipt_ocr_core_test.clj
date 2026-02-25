(ns app.domain.backend.expenses.workers.receipt-ocr-core-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as core]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util.concurrent CountDownLatch TimeUnit]))

(deftest process-extract-auto-retries-review-required-once
  (let [process-extract! #'core/process-extract!
        receipt-id (java.util.UUID/randomUUID)
        calls (atom {:claim 0 :ocr 0 :persist 0 :retry 0})]
    (with-redefs [receipt-status/claim-for-extracting! (fn [_db _rid _opts]
                                                         (swap! calls update :claim inc)
                                                         true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "x")})
                  image-preprocess/prepare-for-ocr (fn [{:keys [bytes content-type] :as _req}]
                                                     {:bytes bytes
                                                      :content-type content-type
                                                      :preprocessed? false})
                  mistral-ocr/ocr-extract! (fn [_cfg _req]
                                             (swap! calls update :ocr inc)
                                             {})
                  extraction/persist-extract-result! (fn [_db _rid _extract-result _opts]
                                                       (swap! calls update :persist inc)
                                                       (if (= 1 (:persist @calls))
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "review_required"}
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "extracted"}))
                  receipt-status/retry-extraction! (fn [_db _rid]
                                                     (swap! calls update :retry inc)
                                                     nil)]
      (let [res (process-extract! nil {:api-key "k"} {:id receipt-id :content_type "image/jpeg"} {:lease-seconds 900})]
        ;; Current implementation does not auto-retry review_required.
        (is (= "review_required" (:status res)))
        (is (= 1 (:claim @calls)))
        (is (= 1 (:ocr @calls)))
        (is (= 1 (:persist @calls)))
        (is (= 0 (:retry @calls)))))))

(deftest process-extract-preprocesses-image-before-mistral
  (let [process-extract! #'core/process-extract!
        receipt-id (java.util.UUID/randomUUID)
        seen (atom nil)
        prepared-bytes (.getBytes "prepared")]
    (with-redefs [receipt-status/claim-for-extracting! (fn [_db _rid _opts] true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "original")
                                                :path "/tmp/receipt-orig.heic"})
                  image-preprocess/prepare-for-ocr (fn [_req]
                                                     {:bytes prepared-bytes
                                                      :content-type "image/jpeg"
                                                      :preprocessed? true})
                  mistral-ocr/ocr-extract! (fn [_cfg req]
                                             (reset! seen req)
                                             {:raw {}
                                              :parsed-markdown ""})
                  extraction/persist-extract-result! (fn [_db _rid _extract-result _opts]
                                                       {:receipt-id receipt-id
                                                        :stage :extract
                                                        :result :ok
                                                        :status "extracted"})]
      (let [res (process-extract! nil
                  {:api-key "k" :auto-post-after-upload? false}
                  {:id receipt-id
                   :content_type "image/heic"
                   :original_filename "r.heic"}
                  {:lease-seconds 900
                   :defer-refine? true})]
        (is (= "extracted" (:status res)))
        (is (= "image/jpeg" (:content-type @seen)))
        (is (= "prepared" (String. ^bytes (:bytes @seen))))))))

(deftest refine-review-required-results-respects-concurrency-limit
  (let [limit 2
        opts {:cerebras-cfg {:refine-concurrency limit :refine-timeout-ms 5000}}
        started (CountDownLatch. limit)
        release (CountDownLatch. 1)
        active (atom 0)
        max-active (atom 0)
        results (vec (for [_ (range 5)]
                       {:receipt {:id (java.util.UUID/randomUUID)}
                        :review-required? true
                        :extract-result {}}))
        runner (future
                 (clojure.core/with-redefs-fn
                   {#'core/maybe-refine-review-required
                    (fn [_db _receipt _extract-result persist-result _opts]
                      (let [n (swap! active inc)]
                        (swap! max-active max n)
                        (.countDown started)
                        (.await release 2 TimeUnit/SECONDS)
                        (swap! active dec)
                        (assoc persist-result :refined? true)))}
                   (fn []
                     (#'core/refine-review-required-results! nil opts results))))]
    (is (.await started 1 TimeUnit/SECONDS))
    (is (<= @max-active limit))
    (.countDown release)
    (let [res @runner]
      (is (= (count results) (count res)))
      (is (every? :refined? res)))))

(deftest refine-review-required-results-keeps-processing-on-failure
  (let [opts {:cerebras-cfg {:refine-concurrency 3 :refine-timeout-ms 5000}}
        ok-id (java.util.UUID/randomUUID)
        bad-id (java.util.UUID/randomUUID)
        results [{:receipt {:id ok-id}
                  :review-required? true
                  :extract-result {}}
                 {:receipt {:id bad-id}
                  :review-required? true
                  :extract-result {}}]
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-review-required
               (fn [_db receipt _extract-result persist-result _opts]
                 (if (= bad-id (:id receipt))
                   (throw (ex-info "boom" {:receipt-id bad-id}))
                   (assoc persist-result :refined? true)))}
              (fn []
                (#'core/refine-review-required-results! nil opts results)))]
    (is (true? (get-in res [0 :refined?])))
    (is (nil? (get-in res [1 :refined?])))))

(deftest maybe-refine-review-required-clears-refine-pending-when-skipped
  (let [receipt-id (java.util.UUID/randomUUID)
        cleared (atom [])
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-with-cerebras (fn [_db _receipt extract-result _opts]
                                                  ;; Skip/failed refine: no :llm_refine returned.
                                                   extract-result)
               #'receipt-status/clear-refine-pending! (fn [_db rid]
                                                        (swap! cleared conj rid)
                                                        {:id rid})}
              (fn []
                (#'core/maybe-refine-review-required
                 ::db
                 {:id receipt-id}
                 {:parsed-markdown "x"}
                 {:receipt-id receipt-id :review-required? true}
                 {:clear-refine-pending? true})))]
    (is (= {:receipt-id receipt-id :review-required? true} res))
    (is (= [receipt-id] @cleared))))

(deftest refine-review-required-results-times-out
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 50}}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        res (clojure.core/with-redefs-fn
              {#'core/maybe-refine-review-required
               (fn [_db _receipt _extract-result persist-result _opts]
                 (try
                   (Thread/sleep 200)
                   (catch InterruptedException _))
                 (assoc persist-result :refined? true))}
              (fn []
                (#'core/refine-review-required-results! nil opts results)))]
    (is (nil? (get-in res [0 :refined?])))))

(deftest refine-review-required-results-clears-refine-pending-on-timeout
  (let [receipt-id (java.util.UUID/randomUUID)
        cleared (atom [])
        opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 10}}
        results [{:receipt {:id receipt-id}
                  :review-required? true
                  :extract-result {}}]
        _res (clojure.core/with-redefs-fn
               {#'core/maybe-refine-review-required
                (fn [_db _receipt _extract-result persist-result _opts]
                  (try
                    (Thread/sleep 200)
                    (catch InterruptedException _))
                  persist-result)
                #'receipt-status/clear-refine-pending!
                (fn [_db rid]
                  (swap! cleared conj rid)
                  {:id rid})}
               (fn []
                 (#'core/refine-review-required-results! ::db opts results)))]
    (is (= [receipt-id] @cleared))))

(deftest refine-review-required-results-logs-post-process-when-no-eligible-refine
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        persist-result)}
                     (fn []
                       (#'core/refine-review-required-results! nil opts results)))
                   (str w)))]
    (is (str/includes? output "Review-required post-process starting (no eligible refine)"))
    (is (str/includes? output "Review-required post-process complete (no eligible refine)"))
    (is (not (str/includes? output "Cerebras parallel refine starting")))))

(deftest refine-review-required-results-logs-cerebras-when-eligible-refine
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}}
        results [{:receipt {:id (java.util.UUID/randomUUID)
                            :user_id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/user-allows-receipt-refine?
                      (fn [_db _receipt]
                        true)
                      #'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        (assoc persist-result :extract-result {:llm_refine {:model "test"}}))}
                     (fn []
                       (#'core/refine-review-required-results! ::db opts results)))
                   (str w)))]
    (is (str/includes? output "Cerebras parallel refine starting"))
    (is (str/includes? output "Cerebras parallel refine complete"))
    (is (not (str/includes? output "Review-required post-process starting (no eligible refine)")))))

(deftest refine-review-required-results-logs-cerebras-when-force-refine-without-user
  (let [opts {:cerebras-cfg {:refine-concurrency 1 :refine-timeout-ms 1000}
              :force-refine? true}
        results [{:receipt {:id (java.util.UUID/randomUUID)}
                  :review-required? true
                  :extract-result {}}]
        output (let [w (java.io.StringWriter.)]
                 (binding [*out* w
                           *err* w]
                   (clojure.core/with-redefs-fn
                     {#'core/user-allows-receipt-refine?
                      (fn [_db _receipt]
                        false)
                      #'core/maybe-refine-review-required
                      (fn [_db _receipt _extract-result persist-result _opts]
                        (assoc persist-result :extract-result {:llm_refine {:model "test"}}))}
                     (fn []
                       (#'core/refine-review-required-results! ::db opts results)))
                   (str w)))]
    (is (str/includes? output "Cerebras parallel refine starting"))
    (is (str/includes? output "Cerebras parallel refine complete"))
    (is (not (str/includes? output "Review-required post-process starting (no eligible refine)")))))

(deftest process-receipts-by-ids-defaults-force-refine
  (let [receipt-id (java.util.UUID/randomUUID)
        captured-opts (atom nil)]
    (clojure.core/with-redefs-fn
      {#'app.domain.backend.expenses.integrations.ocr-provider/build-provider
       (fn [_]
         {:provider :llamaparse
          :enabled? true
          :api-key "k"
          :auto-post-after-upload? false})
       #'app.domain.backend.expenses.integrations.cerebras/build-config
       (fn [_]
         {})
       #'app.domain.backend.expenses.services.places-api/build-config
       (fn [_]
         {})
       #'receipt-status/reset-for-ocr!
       (fn [_db _receipt-id]
         nil)
       #'receipt-queries/get-receipt
       (fn [_db _receipt-id]
         {:id receipt-id
          :status "uploaded"})
       #'core/process-receipt!
       (fn [_db _ocr-cfg _receipt opts]
         (reset! captured-opts opts)
         {:receipt-id receipt-id
          :result :ok})}
      (fn []
        (core/process-receipts-by-ids! ::db {} [receipt-id])))
    (is (true? (:force-refine? @captured-opts)))))
