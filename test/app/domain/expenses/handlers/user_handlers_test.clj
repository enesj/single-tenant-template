(ns app.domain.expenses.handlers.user-handlers-test
  "Regression tests for user-facing handler auth/role plumbing.

  These tests intentionally avoid hitting the DB. They verify that handlers return
  consistent JSON error responses and that auth/role extraction works for both
  session-based and :identity-based request shapes."
  (:require
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
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
  (testing "list receipts returns data with total/limit/offset"
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
                      (is (= 2 (:limit opts)))
                      (is (= 1 (:offset opts)))
                      {:rows [sample-row]
                       :total 3
                       :limit (:limit opts)
                       :offset (:offset opts)})]
        (let [resp (handler request)
              body (parse-json-body resp)]
          (is (= 200 (:status resp)))
          (is (vector? (:data body)))
          (is (= 1 (count (:data body))))
          (is (= 3 (:total body)))
          (is (= 2 (:limit body)))
          (is (= 1 (:offset body))))))))

(deftest user-receipts-list-parses-string-query-param-keys
  (testing "list receipts parses string-keyed limit/offset params"
    (let [handler (user-receipts/list-receipts-handler :mock-db)
          user-id (UUID/randomUUID)
          request {:identity {:id user-id
                              :role "viewer"}
                   :query-params {"status" "uploaded"
                                  "limit" "20"
                                  "offset" "40"}}
          sample-row {:id (UUID/randomUUID)
                      :status "uploaded"
                      :original_filename "receipt-2.jpg"}]
      (with-redefs [receipt-queries/list-user-receipts-page
                    (fn [_db actual-user-id opts]
                      (is (= user-id actual-user-id))
                      (is (= "uploaded" (:status opts)))
                      (is (= 20 (:limit opts)))
                      (is (= 40 (:offset opts)))
                      {:rows [sample-row]
                       :total 42
                       :limit (:limit opts)
                       :offset (:offset opts)})]
        (let [resp (handler request)
              body (parse-json-body resp)]
          (is (= 200 (:status resp)))
          (is (= 42 (:total body)))
          (is (= 20 (:limit body)))
          (is (= 40 (:offset body))))))))

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
