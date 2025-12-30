(ns app.domain.frontend.expenses.events.user-expenses-test
  (:require
    ;; Ensure events are registered
    [app.domain.frontend.expenses.events.user-expenses]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [goog.object :as gobj]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce captured-http-requests (atom []))

(defn- install-fx-stubs! []
  ;; Keep tests deterministic: no real HTTP, no timers.
  ;; Note: other test setup namespaces also stub :http-xhrio; re-register here so
  ;; these tests always capture requests into this namespace-local atom.
  (rf/reg-fx :http-xhrio (fn [req]
                           (swap! captured-http-requests conj req)
                           nil))
  (rf/reg-fx :dispatch-later (fn [_] nil)))

(defn- reset-db! []
  (install-fx-stubs!)
  (reset! captured-http-requests [])
  (reset! rf-db/app-db helpers/valid-test-db-state))

(defn- last-http-request []
  (last @captured-http-requests))

(defn- req-method [req]
  (let [m (or (:method req) (gobj/get req "method"))]
    (cond-> m (string? m) keyword)))

(defn- req-uri [req]
  (or (:uri req) (gobj/get req "uri")))

(defn- req-body [req]
  (or (:body req) (gobj/get req "body")))

(defn- req-format-content-type [req]
  (let [fmt (or (:format req) (gobj/get req "format"))]
    (or (get fmt :content-type) (gobj/get fmt "content-type"))))

(deftest modal-create-tracks-recently-created
  (testing "create-expense-modal-success tracks :expenses in :ui :recently-created"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/create-expense-modal-success
                       nil
                       {:expense {:id "exp-1"}}])
    (is (= #{"exp-1"}
          (get-in @rf-db/app-db [:ui :recently-created :expenses])))))

(deftest modal-update-tracks-recently-updated
  (testing "update-expense-modal-success tracks :expenses in :ui :recently-updated"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/update-expense-modal-success
                       "exp-2"
                       nil
                       {:expense {:id "exp-2"}}])
    (is (= #{"exp-2"}
          (get-in @rf-db/app-db [:ui :recently-updated :expenses])))))

(deftest upload-receipt-sends-file-in-formdata
  (testing "upload-receipt sends multipart file (guards against trim-v arg loss)"
    (reset-db!)
    (let [file (js/File. #js ["abc"] "r.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipt file])
      (let [req (last-http-request)
            body (req-body req)
            uploaded (.get body "file")]
        (is (= :post (req-method req)))
        (is (string? (req-uri req)))
        (is (instance? js/FormData body))
        (is (instance? js/File uploaded))
        (is (= "r.jpg" (.-name uploaded)))
        (is (not (true? (req-format-content-type req))))))))

(deftest upload-receipt-success-queues-ocr
  (testing "upload-receipt-success automatically triggers OCR"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success {:data {:id "rec-1"}}])
    (is (= 1 (count @captured-http-requests)))
    (let [req (last-http-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/expenses/receipts/ocr" (req-uri req)))
      (is (= ["rec-1"] (get-in req [:params :receipt_ids]))))))

(deftest upload-receipts-batch-sends-multiple-requests
  (testing "upload-receipts queues files and uploads sequentially"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (is (= 1 (count @captured-http-requests)))
      (let [req1 (last-http-request)
            body1 (req-body req1)
            uploaded1 (.get body1 "file")]
        (is (= :post (req-method req1)))
        (is (instance? js/FormData body1))
        (is (instance? js/File uploaded1))
        (is (= "a.jpg" (.-name uploaded1))))

      ;; simulate success -> triggers next request
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [f2] {:data {:id "rec-1"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "b.jpg" (.-name uploaded2))))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      ;; Final success should also queue one OCR batch request for the receipts.
      (is (= 3 (count @captured-http-requests)))
      (let [req3 (last-http-request)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/ocr" (req-uri req3)))
        (is (= ["rec-1" "rec-2"] (get-in req3 [:params :receipt_ids]))))
      (is (false? (get-in @rf-db/app-db [:user-expenses :upload :loading?]))))))

(deftest upload-receipts-batch-continues-after-failure
  (testing "upload-receipts continues queue after a failure"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (rf/dispatch-sync [:user-expenses/upload-receipts-failure
                         [f2]
                         "a.jpg"
                         {:response {:error "Boom"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "b.jpg" (.-name uploaded2))))

      (is (= 1 (get-in @rf-db/app-db [:user-expenses :upload :batch :failed])))
      (let [msg (get-in @rf-db/app-db [:user-expenses :upload :error])]
        (is (string? msg))
        (is (.startsWith msg "a.jpg:")))

      ;; simulate success of the remaining file -> should queue OCR for receipts that did upload
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      (is (= 3 (count @captured-http-requests)))
      (let [req3 (last-http-request)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/ocr" (req-uri req3)))
        (is (= ["rec-2"] (get-in req3 [:params :receipt_ids])))))))
