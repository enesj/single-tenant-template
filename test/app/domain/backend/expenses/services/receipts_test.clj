(ns app.domain.backend.expenses.services.receipts-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- insert-receipt!
  [db {:keys [id status]}]
  (let [file-hash (str (UUID/randomUUID) (UUID/randomUUID))
        file-hash (-> file-hash (str/replace "-" "") (subs 0 64))]
    (jdbc/execute-one!
      db
      ["insert into receipts (id, storage_key, file_hash, status) values (?, ?, ?, ?::receipt_status)"
       id
       (str "test/" id ".png")
       file-hash
       status]
      {:builder-fn rs/as-unqualified-lower-maps})))

(deftest store-extraction-results-patch-style-and-casts
  (testing "does not wipe absent fields and uses jsonb/currency casts"
    (let [captured (atom nil)
          receipt-id (java.util.UUID/randomUUID)]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured sql-params)
                                        {:ok true})]
        ;; Only raw_extract_json should be set.
        (receipts/store-extraction-results! :db receipt-id {:raw_extract_json {:a 1}})
        (let [[sql & _] @captured
              sql-lc (str/lower-case sql)]
          (is (str/includes? sql-lc "raw_extract_json"))
          (is (not (str/includes? sql-lc "raw_parse_json")))
          (is (str/includes? sql-lc "jsonb")))

        ;; Currency should be cast to enum.
        (receipts/store-extraction-results! :db receipt-id {:currency_guess "USD"})
        (let [[sql & _] @captured
              sql-lc (str/lower-case sql)]
          (is (str/includes? sql-lc "currency_guess"))
          (is (str/includes? sql-lc "cast"))
          (is (str/includes? sql-lc "currency")))))))

(deftest claim-status-includes-lease-interval
  (let [captured (atom nil)
        receipt-id (java.util.UUID/randomUUID)]
    (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                      (reset! captured sql-params)
                                      {:ok true})]
      (receipts/claim-for-parsing! :db receipt-id {:lease-seconds 60})
      (let [[sql & _] @captured]
        (is (str/includes? sql "NOW() - INTERVAL '60 seconds'"))))))

(deftest save-review-parses-datetime-local-and-validates-currency
  (testing "datetime-local purchased_at is parsed and invalid currency yields a 400"
    (let [db fixtures/*test-db*
          supplier (suppliers/create-supplier! db {:display_name (str "Test Supplier " (UUID/randomUUID))})
          receipt-id (UUID/randomUUID)
          _receipt (insert-receipt! db {:id receipt-id :status "review_required"})
          items [{:raw_label "Line 1" :line_total "10.00"}]]

      (testing "valid review saves and can flip review_required -> extracted"
        (let [updated (receipts/save-review!
                        db
                        receipt-id
                        {:supplier_id (:id supplier)
                         :purchased_at "2026-01-07T12:34"
                         :total_amount "10.00"
                         :currency "eur"
                         :items items})]
          (is (= "extracted" (str (:status updated))))
          (is (some? (:purchased_at_guess updated))
            "purchased_at_guess should be persisted (parsed from datetime-local)")))

      (testing "invalid currency returns a 400 (no JDBC cast exception/500)"
        (try
          (receipts/save-review!
            db
            receipt-id
            {:supplier_id (:id supplier)
             :purchased_at "2026-01-07T12:34"
             :total_amount "10.00"
             :currency "ZZZ"
             :items items})
          (is false "Expected ExceptionInfo")
          (catch clojure.lang.ExceptionInfo e
            (is (= 400 (:status (ex-data e))))
            (is (= :currency (:field (ex-data e))))))))))
