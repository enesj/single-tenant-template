(ns app.domain.expenses.handlers.user-handlers-test
  "Regression tests for user-facing handler auth/role plumbing.

  These tests intentionally avoid hitting the DB. They verify that handlers return
  consistent JSON error responses and that auth/role extraction works for both
  session-based and :identity-based request shapes."
  (:require
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(defn- parse-json-body
  [resp]
  (when-let [body (:body resp)]
    (cond
      (map? body) body
      (string? body) (json/parse-string body true)
      :else (json/parse-string (slurp body) true))))

(deftest user-articles-unauthorized-when-no-user
  (testing "user articles handlers return 401 when request has no user"
    (let [handler (user-articles/list-articles-handler nil)
          resp (handler {})
          body (parse-json-body resp)]
      (is (= 401 (:status resp)))
      (is (= "Authentication required" (:error body))))))

(deftest user-articles-forbidden-when-role-missing-even-with-identity
  (testing "user articles handlers accept :identity but still require a membership role"
    (let [handler (user-articles/list-articles-handler nil)
          resp (handler {:identity {:id (UUID/randomUUID)}})
          body (parse-json-body resp)]
      (is (= 403 (:status resp)))
      (is (= "Role assignment required" (:error body))))))

(deftest user-receipts-forbidden-when-role-missing-even-with-identity
  (testing "user receipts handlers accept :identity for user-id extraction but still role-gate"
    (let [handler (user-receipts/list-receipts-handler nil)
          resp (handler {:identity {:id (UUID/randomUUID)}})
          body (parse-json-body resp)]
      (is (= 403 (:status resp)))
      (is (= "Role assignment required" (:error body))))))

(deftest user-receipts-list-includes-pagination-metadata
  (testing "list receipts returns data with total/limit/offset and purged metadata"
    (let [handler (user-receipts/list-receipts-handler :mock-db)
          user-id (UUID/randomUUID)
          request {:identity {:id user-id
                              :role "viewer"}
                   :query-params {:status "uploaded"
                                  :limit "2"
                                  :offset "1"}}
          sample-row {:id (UUID/randomUUID)
                      :status "uploaded"
                      :original_filename "receipt-1.jpg"}]
      (with-redefs [receipt-queries/list-user-receipts-page
                    (fn [_db actual-user-id opts]
                      (is (= user-id actual-user-id))
                      (is (= "uploaded" (:status opts)))
                      (is (= "posted" (:exclude-status opts)))
                      (is (false? (:show-purged? opts)))
                      (is (= 2 (:limit opts)))
                      (is (= 1 (:offset opts)))
                      {:rows [sample-row]
                       :total 3
                       :purged-total 1
                       :limit (:limit opts)
                       :offset (:offset opts)})]
        (let [resp (handler request)
              body (parse-json-body resp)]
          (is (= 200 (:status resp)))
          (is (vector? (:data body)))
          (is (= 1 (count (:data body))))
          (is (= 3 (:total body)))
          (is (= 1 (:purged-total body)))
          (is (= 2 (:limit body)))
          (is (= 1 (:offset body))))))))

(deftest user-receipts-list-parses-string-query-param-keys
  (testing "list receipts parses string-keyed limit/offset params and show-purged flag"
    (let [handler (user-receipts/list-receipts-handler :mock-db)
          user-id (UUID/randomUUID)
          request {:identity {:id user-id
                              :role "viewer"}
                   :query-params {"status" "uploaded"
                                  "show-purged" "true"
                                  "limit" "20"
                                  "offset" "40"}}
          sample-row {:id (UUID/randomUUID)
                      :status "uploaded"
                      :original_filename "receipt-2.jpg"}]
      (with-redefs [receipt-queries/list-user-receipts-page
                    (fn [_db actual-user-id opts]
                      (is (= user-id actual-user-id))
                      (is (= "uploaded" (:status opts)))
                      (is (= "posted" (:exclude-status opts)))
                      (is (true? (:show-purged? opts)))
                      (is (= 20 (:limit opts)))
                      (is (= 40 (:offset opts)))
                      {:rows [sample-row]
                       :total 42
                       :purged-total 5
                       :limit (:limit opts)
                       :offset (:offset opts)})]
        (let [resp (handler request)
              body (parse-json-body resp)]
          (is (= 200 (:status resp)))
          (is (= 42 (:total body)))
          (is (= 5 (:purged-total body)))
          (is (= 20 (:limit body)))
          (is (= 40 (:offset body))))))))

(deftest user-receipts-list-parses-total-amount-range-filters
  (testing "list receipts normalizes total amount range params for receipt amount filters"
    (let [handler (user-receipts/list-receipts-handler :mock-db)
          user-id (UUID/randomUUID)
          request {:identity {:id user-id
                              :role "viewer"}
                   :query-params {"total-display-min" "2"
                                  "total-amount-guess-max" "10.50"}}]
      (with-redefs [receipt-queries/list-user-receipts-page
                    (fn [_db actual-user-id opts]
                      (is (= user-id actual-user-id))
                      (is (= 2M (:total-amount-guess-min opts)))
                      (is (= 10.50M (:total-amount-guess-max opts)))
                      {:rows []
                       :total 0
                       :purged-total 0
                       :limit (:limit opts)
                       :offset (:offset opts)})]
        (let [resp (handler request)
              body (parse-json-body resp)]
          (is (= 200 (:status resp)))
          (is (= 0 (:total body)))
          (is (= 50 (:limit body)))
          (is (= 0 (:offset body))))))))

(deftest user-receipts-batch-ocr-empty-selection-is-safe
  (testing "batch OCR returns 400 when no receipt ids are provided"
    (let [handler (user-receipts/ocr-batch-receipts-handler nil nil)]
      (doseq [selection [nil []]]
        (let [resp (handler {:identity {:id (UUID/randomUUID)
                                        :role "member"}
                             :body-params {:receipt_ids selection}})
              body (parse-json-body resp)]
          (is (= 400 (:status resp)))
          (is (= "No receipt ids provided" (:error body))))))))

(deftest user-receipts-single-ocr-blocks-linked-receipt
  (testing "single OCR returns 409 when a receipt is already linked to an expense"
    (let [handler (user-receipts/ocr-single-receipt-handler :mock-db :mock-app)
          receipt-id (UUID/randomUUID)
          expense-id (UUID/randomUUID)
          queued? (atom false)]
      (with-redefs [receipt-queries/get-receipt
                    (fn [_db actual-id & _]
                      {:id actual-id})
                    receipt-status/linked-expense-id
                    (fn [_db actual-id]
                      (is (= receipt-id actual-id))
                      expense-id)
                    receipt-ocr/queue-ui-ocr!
                    (fn [& _]
                      (reset! queued? true)
                      {:queued true})]
        (let [resp (handler {:identity {:id (UUID/randomUUID)
                                        :role "member"}
                             :path-params {:id (str receipt-id)}})
              body (parse-json-body resp)]
          (is (= 409 (:status resp)))
          (is (= "Receipt already linked to an expense. Unlink it first before reparsing" (:error body)))
          (is (= (str receipt-id) (get-in body [:details :receipt-id])))
          (is (= (str expense-id) (get-in body [:details :expense-id])))
          (is (false? @queued?)))))))

(deftest user-receipts-batch-ocr-blocks-when-all-selected-receipts-are-linked
  (testing "batch OCR returns 409 when every accessible receipt is already linked"
    (let [handler (user-receipts/ocr-batch-receipts-handler :mock-db :mock-app)
          id-a (UUID/randomUUID)
          id-b (UUID/randomUUID)
          queued? (atom false)]
      (with-redefs [receipt-queries/get-receipt
                    (fn [_db actual-id & _]
                      {:id actual-id})
                    receipt-status/linked-expense-id
                    (fn [_db _receipt-id]
                      (UUID/randomUUID))
                    receipt-ocr/queue-ui-ocr!
                    (fn [& _]
                      (reset! queued? true)
                      {:queued true})]
        (let [resp (handler {:identity {:id (UUID/randomUUID)
                                        :role "member"}
                             :body-params {:receipt_ids [(str id-a) (str id-b)]}})
              body (parse-json-body resp)]
          (is (= 409 (:status resp)))
          (is (= "One or more receipts are already linked to expenses. Unlink them first before reparsing" (:error body)))
          (is (= #{(str id-a) (str id-b)} (set (get-in body [:details :blocked_receipt_ids]))))
          (is (false? @queued?)))))))
