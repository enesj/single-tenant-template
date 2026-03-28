(ns app.domain.frontend.expenses.components.user-expense-form-test
  (:require
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]
    [cljs.test :refer [deftest is testing]]))

(deftest qty-step-precision-test
  (testing "User expense form qty supports 3 decimal places"
    (let [spec (specs/get-expense-form-spec [] [] nil)
          items-field (some (fn [f] (when (= :items (:id f)) f)) spec)
          columns (:columns items-field)
          qty-col (some (fn [c] (when (= :qty (:id c)) c)) columns)]
      (is (= "0.001" (:step qty-col))))))

(deftest normalize-receipt-data-preserves-item-unit-test
  (testing "Receipt normalization keeps OCR-derived unit on line items"
    (let [normalized (norm/normalize-receipt-data
                       {:raw_extract_json {:extraction {:items [{:raw_label "JAGODA SVJEZA"
                                                                :qty 0.750
                                                                :unit "kg"
                                                                :line_total 5.25}]}}})]
      (is (= "kg" (-> normalized :items first :unit))))))

(deftest prepare-expense-submit-values-preserves-item-unit-test
  (testing "Prepared submit payload keeps hidden unit values for receipt items"
    (let [prepared (norm/prepare-expense-submit-values
                     {:items [{:raw_label "JAGODA SVJEZA"
                               :qty "0.750"
                               :unit "kg"
                               :line_total "5.25"}]})]
      (is (= "kg" (-> prepared :items first :unit)))
      (is (= 5.25 (:total_amount prepared))))))
