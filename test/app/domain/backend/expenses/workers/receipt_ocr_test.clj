(ns app.domain.backend.expenses.workers.receipt-ocr-test
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr :as receipt-ocr]
    [clojure.test :refer [deftest is testing]]))

(deftest parse-money-handles-common-formats
  (let [parse-money #'receipt-ocr/parse-money]
    (is (= 10.26M (parse-money "10.26")))
    (is (= 10.26M (parse-money "$10.26")))
    (is (= 10.26M (parse-money "10,26")))
    (is (= 1234.56M (parse-money "1,234.56")))
    (is (nil? (parse-money "abc")))))

(deftest normalize-currency-applies-default
  (let [normalize-currency #'receipt-ocr/normalize-currency]
    (is (= "USD" (normalize-currency "usd" "BAM")))
    (is (= "BAM" (normalize-currency nil "BAM")))
    (is (= "EUR" (normalize-currency "GBP" "EUR")))
    (is (nil? (normalize-currency "GBP" "GBP")))))

(deftest review-required-heuristic
  (let [review-required? #'receipt-ocr/review-required?]
    (testing "missing critical fields"
      (is (true? (review-required? {:supplier_guess nil :total_amount_guess 1M :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess nil :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess nil :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 0}))))
    (testing "looks good"
      (is (false? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 2}))))))
