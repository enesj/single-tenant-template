(ns app.domain.frontend.expenses.components.expense-form-test
  (:require
    [app.domain.frontend.expenses.components.expense-form :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest normalize-initial-data-test
  (testing "Normalizes snake_case data from backend"
    (let [raw-expense {:supplier_id "s1"
                       :payer_id "p1"
                       :purchased_at "2023-10-01T12:00:00"
                       :total_amount 123.45
                       :currency "EUR"
                       :items [{:raw_label "Item 1" :line_total 123.45}]}
          normalized (sut/normalize-initial-data raw-expense)]
      (is (= "s1" (:supplier_id normalized)))
      (is (= "2023-10-01T12:00" (:purchased_at normalized)))
      (is (= 123.45 (:total_amount normalized)))
      (is (= "123.45" (-> normalized :items first :line_total)))))

  (testing "Normalizes kebab-case data"
    (let [raw-expense {:supplier-id "s2"
                       :payer-id "p2"
                       :purchased-at "2023-10-02T13:00"
                       :total-amount 50.0
                       :items [{:raw-label "Item 2" :line-total 50.0}]}
          normalized (sut/normalize-initial-data raw-expense)]
      (is (= "s2" (:supplier_id normalized)))
      (is (= "50.00" (-> normalized :items first :line_total)))))

  (testing "Provides default line item if items are empty"
    (let [normalized (sut/normalize-initial-data {})]
      (is (= 1 (count (:items normalized))))
      (is (= "" (-> normalized :items first :raw_label))))))

(deftest validate-expense-values-test
  (testing "Fails if required fields are missing"
    (let [values {:supplier_id "" :payer_id "p1" :purchased_at "2023-10-01T12:00" :items []}]
      (is (false? (:ok? (sut/validate-expense-values values))))
      (is (re-find #"required" (:error (sut/validate-expense-values values))))))

  (testing "Fails if there are no prepared line items"
    (let [values {:supplier_id "s1" :payer_id "p1" :purchased_at "2023-10-01" 
                  :items [{:raw_label "" :line_total "0"}]}]
      (is (false? (:ok? (sut/validate-expense-values values))))
      (is (re-find #"at least one line item" (:error (sut/validate-expense-values values))))))

  (testing "Total Mismatch Validation - EXACT MATCH"
    (let [values {:supplier_id "s1" :payer_id "p1" :purchased_at "2023-10-01T12:00"
                  :total_amount "100.00"
                  :items [{:raw_label "Item 1" :line_total "100.00"}]}]
      (is (:ok? (sut/validate-expense-values values)))))

  (testing "Total Mismatch Validation - WITHIN TOLERANCE (0.005)"
    (let [values {:supplier_id "s1" :payer_id "p1" :purchased_at "2023-10-01T12:00"
                  :total_amount "100.005"
                  :items [{:raw_label "Item 1" :line_total "100.00"}]}]
      (is (:ok? (sut/validate-expense-values values)))))

  (testing "Total Mismatch Validation - EXCEEDS TOLERANCE (0.02)"
    (let [values {:supplier_id "s1" :payer_id "p1" :purchased_at "2023-10-01T12:00"
                  :total_amount "100.02"
                  :items [{:raw_label "Item 1" :line_total "100.00"}]}
          result (sut/validate-expense-values values)]
      (is (false? (:ok? result)))
      (is (re-find #"must match line items" (:error result)))))

  (testing "Passes if total_amount is blank but items sum up to positive value"
    (let [values {:supplier_id "s1" :payer_id "p1" :purchased_at "2023-10-01T12:00"
                  :total_amount ""
                  :items [{:raw_label "Item 1" :line_total "100.00"}]}]
      (is (:ok? (sut/validate-expense-values values))))))

(deftest prepare-expense-submit-values-test
  (testing "Filters blank line items and parses numbers"
    (let [values {:supplier_id "s1"
                  :items [{:raw_label "Item 1" :line_total "100.5"}
                          {:raw_label "" :line_total "50"} ; should be filtered
                          {:raw_label "Item 2" :line_total "invalid"}]}
          prepared (sut/prepare-expense-submit-values values)]
      (is (= 1 (count (:items prepared))))
      (is (= 100.5 (-> prepared :items first :line_total)))
      (is (= 100.5 (:total_amount prepared)))))

  (testing "Uses manual total if provided"
    (let [values {:total_amount "150.00"
                  :items [{:raw_label "Item 1" :line_total "150.00"}]}
          prepared (sut/prepare-expense-submit-values values)]
      (is (= 150.0 (:total_amount prepared))))))
