(ns app.domain.frontend.expenses.pages.user.receipts-list-test
  (:require
    [app.domain.frontend.expenses.pages.user.receipts-list :as page]
    [cljs.test :refer [deftest is testing]]))

(deftest receipt-total-display-prefers-total-and-appends-currency
  (testing "receipt total display renders guessed total for the list column"
    (is (= "23.58 BAM"
          (page/receipt-total-display {:total-amount-guess 23.58
                                       :currency-guess "BAM"})))))

(deftest receipt-total-display-falls-back-to-lines-total
  (testing "receipt total display still renders when only line totals are available"
    (is (= "5.55 EUR"
          (page/receipt-total-display {:lines-total-amount-guess 5.55
                                       :currency-guess "EUR"})))))

(deftest receipt-total-display-shows-lines-annotation-when-values-differ
  (testing "receipt total display keeps the main total and surfaces a differing line-total sum"
    (is (= "20.00 BAM (lines 19.50)"
          (page/receipt-total-display {:total-amount-guess 20
                                       :lines-total-amount-guess 19.5
                                       :currency-guess "BAM"})))))

(deftest receipt-total-display-is-nil-when-no-amounts-exist
  (testing "receipt total display remains empty when neither total nor line totals exist"
    (is (nil? (page/receipt-total-display {:status "failed"})))))

(deftest receipts-entity-spec-includes-purchased-at-guess-column
  (testing "receipts list entity spec exposes the purchased-at-guess column so always-visible settings can apply"
    (let [entity-spec (#'app.domain.frontend.expenses.pages.user.receipts-list/receipts-entity-spec (fn [k] (name k)))
          field-ids (mapv :id (:fields entity-spec))]
      (is (some #{:purchased-at-guess} field-ids)))))