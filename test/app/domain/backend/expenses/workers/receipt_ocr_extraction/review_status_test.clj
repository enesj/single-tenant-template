(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.review-status-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [clojure.test :refer [deftest is testing]]))

(deftest persist-extract-result-does-not-create-article-aliases-when-supplier-unknown
  (let [receipt-id (java.util.UUID/randomUUID)
        unknown-supplier-id (java.util.UUID/randomUUID)
        calls (atom {:resolve-supplier 0
                     :supplier-aliases 0
                     :article-aliases 0})]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [& _] nil)
                  supplier-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :supplier-aliases inc)
                    {:id (java.util.UUID/randomUUID)
                     :supplier_id nil})
                  suppliers/resolve-or-create-supplier-with-places!
                  (fn [& _]
                    (swap! calls update :resolve-supplier inc)
                    {:supplier {:id (java.util.UUID/randomUUID)}
                     :source :places-api})
                  article-aliases/get-unknown-supplier-id (fn [& _] unknown-supplier-id)
                  article-aliases/find-or-create-alias!
                  (fn [& _]
                    (swap! calls update :article-aliases inc)
                    {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            ;; No merchant name -> supplier_guess nil -> :unknown source.
                            :extraction {:totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        ;; No supplier guess -> no alias lookup or Places resolution.
        (is (= 0 (:supplier-aliases @calls)))
        (is (= 0 (:resolve-supplier @calls)))
        ;; Critically: don't create article aliases under "Unknown Supplier" during extraction.
        (is (= 0 (:article-aliases @calls)))))))

(deftest persist-extract-result-marks-review-required-when-supplier-is-undefined
  (let [receipt-id (java.util.UUID/randomUUID)
        unknown-supplier-id (java.util.UUID/randomUUID)
        persisted-status (atom nil)]
    (with-redefs [receipt-queries/get-receipt (fn [_db _rid]
                                                {:id receipt-id
                                                 :status "uploaded"})
                  receipt-status/store-extraction-results! (fn [& _] nil)
                  receipt-status/update-status! (fn [_db _rid status _extra]
                                                  (reset! persisted-status status)
                                                  nil)
                  article-aliases/get-unknown-supplier-id (fn [& _] unknown-supplier-id)
                  supplier-aliases/find-or-create-alias! (fn [& _]
                                                           (throw (ex-info "supplier resolution failed" {})))
                  article-aliases/find-or-create-alias! (fn [& _]
                                                          {:id (java.util.UUID/randomUUID)})]
      (let [extract-result {:parsed-markdown ""
                            :extraction {:merchant {:name "Known Label But Unresolved Supplier"}
                                         :totals {:total 1.00}
                                         :items [{:raw_label "ITEM" :line_total 1.00}]}}
            res (extraction/persist-extract-result!
                  ::db
                  receipt-id
                  extract-result
                  {:default-currency "BAM"
                   :places-cfg {}
                   :user-region "BA"
                   :defer-refine? true})]
        (is (= receipt-id (:receipt-id res)))
        (is (= "review_required" (:status res)))
        (is (= "review_required" @persisted-status))))))

(deftest parse-money-handles-common-formats
  (let [parse-money #'common/parse-money]
    (is (= 10.26M (parse-money "10.26")))
    (is (= 10.26M (parse-money "$10.26")))
    (is (= 10.26M (parse-money "10,26")))
    (is (= 1234.56M (parse-money "1,234.56")))
    (is (nil? (parse-money "abc")))))

(deftest normalize-currency-applies-default
  (let [normalize-currency #'common/normalize-currency]
    (is (= "USD" (normalize-currency "usd" "BAM")))
    (is (= "BAM" (normalize-currency nil "BAM")))
    (is (= "EUR" (normalize-currency "GBP" "EUR")))
    (is (nil? (normalize-currency "GBP" "GBP")))))

(deftest review-required-heuristic
  (let [review-required? #'extraction/review-required?]
    (testing "missing critical fields"
      (is (true? (review-required? {:supplier_guess nil :total_amount_guess 1M :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess nil :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess nil :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 0}))))
    (testing "looks good"
      (is (false? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 2}))))))

(deftest lines-total-mismatch-detects-absolute-difference
  (let [mismatch? #'extraction/lines-total-mismatch?]
    (testing "overage mismatch"
      (is (true? (mismatch? [{:raw_label "A" :line_total 12.00M}] 10.00M))))
    (testing "underage mismatch"
      (is (true? (mismatch? [{:raw_label "A" :line_total 8.00M}] 10.00M))))
    (testing "exact total"
      (is (false? (mismatch? [{:raw_label "A" :line_total 10.00M}] 10.00M))))))