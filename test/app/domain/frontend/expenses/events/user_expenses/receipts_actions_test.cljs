(ns app.domain.frontend.expenses.events.user-expenses.receipts-actions-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest ocr-selected-sends-batch-request
  (testing "ocr-selected posts normalized receipt ids to /api/v1/expenses/receipts/ocr"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/ocr-selected ["rec-1" nil " rec-2 " "rec-1" ""]])
    (let [req (sup/last-http-request)]
      (is (= :post (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts/ocr" (sup/req-uri req)))
      (is (= {:receipt_ids ["rec-1" "rec-2"]}
            (sup/req-params req))))))

(deftest ocr-selected-empty-selection-is-safe
  (testing "ocr-selected with nil or empty selection does not send a request"
    (doseq [selection [nil []]]
      (sup/reset-db!)
      (rf/dispatch-sync [:user-expenses/ocr-selected selection])
      (is (= 0 (count @sup/captured-http-requests)))
      (is (= "Select at least one receipt to parse."
            (get-in @rf-db/app-db [:user-expenses :receipts :error])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-starts-with-first-fetch
  (testing "post-selected starts by loading the first selected receipt"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1" "rec-2"]])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts/rec-1" (sup/req-uri req)))
      (is (true? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-empty-selection-is-safe
  (testing "post-selected with nil or empty selection does not send a request"
    (doseq [selection [nil []]]
      (sup/reset-db!)
      (rf/dispatch-sync [:user-expenses/post-selected selection])
      (is (= 0 (count @sup/captured-http-requests)))
      (is (= "Select at least one receipt to post."
            (get-in @rf-db/app-db [:user-expenses :receipts :error])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-approves-and-continues-sequentially
  (testing "post-selected posts one receipt then continues with the next one"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1" "rec-2"]])

    (rf/dispatch-sync [:user-expenses/post-selected-receipt-loaded
                       "rec-1"
                       ["rec-2"]
                       []
                       []
                       {:data (sup/valid-receipt "rec-1")}])

    (let [approve-req (sup/last-http-request)]
      (is (= :post (sup/req-method approve-req)))
      (is (= "/api/v1/expenses/receipts/rec-1/approve" (sup/req-uri approve-req))))

    (rf/dispatch-sync [:user-expenses/post-selected-approve-success
                       "rec-1"
                       ["rec-2"]
                       []
                       []
                       {:data {:expense {:id "exp-1"}
                               :receipt {:id "rec-1" :status "posted"}}}])

    (let [next-req (sup/last-http-request)]
      (is (= :get (sup/req-method next-req)))
      (is (= "/api/v1/expenses/receipts/rec-2" (sup/req-uri next-req))))))

(deftest post-selected-validation-failure-is-reported
  (testing "invalid receipt data is reported and batch finishes safely"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1"]])
    (rf/dispatch-sync [:user-expenses/post-selected-receipt-loaded
                       "rec-1"
                       []
                       []
                       []
                       {:data {:id "rec-1"}}])
    (is (= 1 (count @sup/captured-http-requests)))
    (is (= "Failed to post selected receipt. Supplier, payer, and date are required."
          (get-in @rf-db/app-db [:user-expenses :receipts :error])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?])))))

(deftest upload-receipt-sends-file-in-formdata
  (testing "upload-receipt sends multipart file (guards against trim-v arg loss)"
    (sup/reset-db!)
    (let [file (js/File. #js ["abc"] "r.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-1"])
      (rf/dispatch-sync [:user-expenses/upload-receipt file])
      (let [req (sup/last-http-request)
            body (sup/req-body req)
            uploaded (.get body "file")
            payer-id (.get body "payer_id")]
        (is (= :post (sup/req-method req)))
        (is (string? (sup/req-uri req)))
        (is (instance? js/FormData body))
        (is (instance? js/File uploaded))
        (is (= "r.jpg" (.-name uploaded)))
        (is (= "payer-1" payer-id))
        (is (not (true? (sup/req-format-content-type req))))))))

(deftest upload-receipt-success-queues-ocr
  (testing "upload-receipt-success automatically triggers OCR"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success {:data {:id "rec-1"}}])
    (is (= 1 (count @sup/captured-http-requests)))
    (let [req (sup/last-http-request)]
      (is (= :post (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts/rec-1/ocr" (sup/req-uri req)))
      (is (nil? (sup/req-params req))))))

(deftest upload-receipt-duplicate-shows-notice-and-skips-ocr
  (testing "duplicate uploads show a notice and do not queue OCR"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success
                       {:data {:id "rec-1" :original_filename "r.jpg"}
                        :duplicate? true}])
    (is (= 0 (count @sup/captured-http-requests)))
    (let [notice (get-in @rf-db/app-db [:user-expenses :upload :notice])]
      (is (seq notice))
      (is (some #(re-find #"Already uploaded" (str %)) notice)))))

(deftest upload-receipts-batch-sends-multiple-requests
  (testing "upload-receipts queues files and uploads sequentially"
    (sup/reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-xyz"])
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (is (= 1 (count @sup/captured-http-requests)))
      (let [req1 (sup/last-http-request)
            body1 (sup/req-body req1)
            uploaded1 (.get body1 "file")
            payer1 (.get body1 "payer_id")]
        (is (= :post (sup/req-method req1)))
        (is (instance? js/FormData body1))
        (is (instance? js/File uploaded1))
        (is (= "payer-xyz" payer1))
        (is (= "a.jpg" (.-name uploaded1))))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [f2] {:data {:id "rec-1"}}])

      (is (= 2 (count @sup/captured-http-requests)))
      (let [req2 (sup/last-http-request)
            body2 (sup/req-body req2)
            uploaded2 (.get body2 "file")
            payer2 (.get body2 "payer_id")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "payer-xyz" payer2))
        (is (= "b.jpg" (.-name uploaded2))))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      (is (= 4 (count @sup/captured-http-requests)))
      (let [req3 (nth @sup/captured-http-requests 2)
            req4 (nth @sup/captured-http-requests 3)]
        (is (= :post (sup/req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-1/ocr" (sup/req-uri req3)))
        (is (= :post (sup/req-method req4)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (sup/req-uri req4))))
      (is (false? (get-in @rf-db/app-db [:user-expenses :upload :loading?]))))))

(deftest upload-receipts-batch-continues-after-failure
  (testing "upload-receipts continues queue after a failure"
    (sup/reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (rf/dispatch-sync [:user-expenses/upload-receipts-failure
                         [f2]
                         "a.jpg"
                         {:response {:error "Boom"}}])

      (is (= 2 (count @sup/captured-http-requests)))
      (let [req2 (sup/last-http-request)
            body2 (sup/req-body req2)
            uploaded2 (.get body2 "file")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "b.jpg" (.-name uploaded2))))

      (is (= 1 (get-in @rf-db/app-db [:user-expenses :upload :batch :failed])))
      (let [msg (get-in @rf-db/app-db [:user-expenses :upload :error])]
        (is (string? msg))
        (is (.startsWith msg "a.jpg:")))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      (is (= 3 (count @sup/captured-http-requests)))
      (let [req3 (sup/last-http-request)]
        (is (= :post (sup/req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (sup/req-uri req3)))
        (is (nil? (sup/req-params req3)))))))

(deftest update-settings-sends-json-params
  (testing "update-settings sends JSON payload in :params (not :body)"
    (sup/reset-db!)
    (let [settings {:default-currency "EUR"
                    :default-payer-id ""
                    :notifications-enabled true
                    :receipt-refine-enabled true}]
      (rf/dispatch-sync [:user-expenses/update-settings settings])
      (let [req (sup/last-http-request)]
        (is (= :put (sup/req-method req)))
        (is (= "/api/v1/expenses/settings" (sup/req-uri req)))
        (is (= settings (sup/req-params req)))
        (is (nil? (sup/req-body req)))))))
