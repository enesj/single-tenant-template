(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.review-currency-test
  (:require
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.review :as review]
    [clojure.test :refer [deftest is]]))

(deftest auto-approve-marks-review-required-for-non-bam-currency
  (let [receipt-id (java.util.UUID/randomUUID)
        persisted (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "extracted"
                                                 :expense_id nil
                                                 :currency_guess "EUR"
                                                 :total_amount_guess 10.00M
                                                 :purchased_at_guess "2026-03-19"
                                                 :payer_id (java.util.UUID/randomUUID)
                                                 :user_id (java.util.UUID/randomUUID)
                                                 :tenant_id (java.util.UUID/randomUUID)})
                  receipt-status/update-status! (fn [_db rid status extra]
                                                  (reset! persisted {:receipt-id rid
                                                                     :status status
                                                                     :extra extra})
                                                  extra)]
      (let [result (review/auto-approve-extracted-receipt!
                     ::db
                     receipt-id
                     {:items [{:raw_label "Item" :line_total 10.00M}]}
                     (java.util.UUID/randomUUID)
                     nil
                     {:default-currency "BAM"})]
        (is (= "review_required" (:status result)))
        (is (= receipt-id (:receipt-id @persisted)))
        (is (= "review_required" (:status @persisted)))
        (is (= "Receipt currency (EUR) is not BAM. Please review the amount and currency before posting."
              (get-in @persisted [:extra :error_message])))))))
