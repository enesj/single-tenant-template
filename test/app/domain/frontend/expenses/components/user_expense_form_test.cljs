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

(deftest receipt-review-changed-test
  (testing "Receipt review changes are detected from meaningful payload differences"
    (let [initial {:supplier_id "supplier-1"
                   :payer_id "payer-1"
                   :expense_category_id "category-1"
                   :purchased_at "2026-03-29T14:17"
                   :currency "BAM"
                   :notes "Extracted from receipt: IMG_4184.jpeg"
                   :total_amount "9.00"
                   :items [{:id "line-1"
                            :raw_label "Espresso kafa"
                            :qty "3"
                            :unit_price "3.00"
                            :line_total "9.00"}]}
          changed-currency (assoc initial :currency "EUR")
          changed-line-total (assoc-in initial [:items 0 :line_total] "8.00")]
      (is (false? (norm/receipt-review-changed? initial initial)))
      (is (true? (norm/receipt-review-changed? initial changed-currency)))
      (is (true? (norm/receipt-review-changed? initial changed-line-total))))))
