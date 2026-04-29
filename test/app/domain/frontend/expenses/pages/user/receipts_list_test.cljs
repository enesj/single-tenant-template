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

(deftest receipts-entity-spec-status-filter-keeps-only-stable-options
  (testing "receipts status filter exposes only stable user-facing states"
    (let [entity-spec (#'app.domain.frontend.expenses.pages.user.receipts-list/receipts-entity-spec (fn [k] (name k)))
          status-field (some #(when (= :status (:id %)) %) (:fields entity-spec))
          option-values (mapv :value (:options status-field))]
      (is (= ["extracted" "review_required" "failed"]
            option-values))
      (is (not-any? #{"uploaded" "parsing" "parsed" "extracting" "refining" "approved" "posted"}
            option-values)))))

(deftest receipt-post-allowed-only-for-unposted-approvable-receipts
  (testing "row Objavi button is limited to extracted/review-required receipts that are not linked or purged"
    (let [post-allowed? #'app.domain.frontend.expenses.pages.user.receipts-list/receipt-post-allowed?]
      (is (true? (post-allowed? {:status "extracted"})))
      (is (true? (post-allowed? {:status "review_required"})))
      (is (false? (post-allowed? {:status "posted"})))
      (is (false? (post-allowed? {:status "uploaded"})))
      (is (false? (post-allowed? {:status "extracted"
                                  :expense-id "expense-1"})))
      (is (false? (post-allowed? {:status "extracted"
                                  :file-purged-at "2026-04-29T00:00:00Z"})))
      (is (false? (post-allowed? {:status "review_required"
                                  :refine-pending true}))))))

(deftest present-receipt-populates-translated-status-display
  (testing "presented receipts carry translated status labels for badge rendering"
    (let [t (fn [k] ({:receipts/status-review-required "Potrebna provjera"
                      :receipts/status-refining "Poboljšanje"
                      :receipts/show-purged "Prikaži obrisane"} k))]
      (is (= "Potrebna provjera"
            (:receipt-status-display
             (#'app.domain.frontend.expenses.pages.user.receipts-list/present-receipt
              t
              {:status "review_required"}))))
      (is (= "Poboljšanje"
            (:receipt-status-display
             (#'app.domain.frontend.expenses.pages.user.receipts-list/present-receipt
              t
              {:status "review_required"
               :refine-pending true})))))))