(ns app.domain.backend.expenses.services.receipts-test
  (:require
    [app.domain.backend.expenses.services.receipts :as receipts]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

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
