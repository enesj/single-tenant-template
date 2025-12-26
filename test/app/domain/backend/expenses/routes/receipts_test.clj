(ns app.domain.backend.expenses.routes.receipts-test
  (:require
    [app.domain.backend.expenses.routes.receipts :as receipts-routes]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.test :refer [deftest is testing]]))

(deftest lines-total-amount-guess-sums-line-totals
  (let [lines-total-amount-guess #'receipts-routes/lines-total-amount-guess]
    (testing "returns nil when no items"
      (is (nil? (lines-total-amount-guess {})))
      (is (nil? (lines-total-amount-guess {:raw-extract-json {:extraction {:items []}}}))))

    (testing "sums parseable line totals"
      (is (= 19.95M
            (lines-total-amount-guess
              {:raw-extract-json
               {:extraction {:items [{:line-total "10.00"}
                                     {:line-total "$9.95"}]}}}))))

    (testing "ignores non-parseable values"
      (is (= 10M
            (lines-total-amount-guess
              {:raw-extract-json
               {:extraction {:items [{:line-total "abc"}
                                     {:line-total "10.00"}]}}}))))))

(deftest enrich-receipt-for-detail-adds-supplier-match-and-total-check
  (let [enrich #'receipts-routes/enrich-receipt-for-detail
        supplier-id (java.util.UUID/randomUUID)]
    (with-redefs [suppliers/normalize-supplier-key (fn [_] "samon-promet")
                  suppliers/find-by-normalized-key (fn [_db _key]
                                                     {:id supplier-id
                                                      :display_name "SAMON PROMET"
                                                      :normalized_key "samon-promet"})]
      (let [receipt {:supplier-guess "SAMON PROMET"
                     :total-amount-guess 19.95M
                     :currency-guess "BAM"
                     :raw-extract-json
                     {:extraction
                      {:items [{:line-total "10.00"}
                               {:line-total "9.95"}]}}}
            enriched (enrich :db receipt)]
        (is (true? (:supplier-guess-has-supplier? enriched)))
        (is (= {:id supplier-id
                :display-name "SAMON PROMET"
                :normalized-key "samon-promet"}
              (:supplier-guess-supplier enriched)))
        (is (= 19.95M (:lines-total-amount-guess enriched)))
        (is (true? (:total-guess-equals-lines-total-guess? enriched)))))

    (with-redefs [suppliers/normalize-supplier-key (fn [_] "no-match")
                  suppliers/find-by-normalized-key (fn [_db _key] nil)]
      (let [receipt {:supplier-guess "UNKNOWN"
                     :total-amount-guess 19.95M
                     :raw-extract-json {:extraction {:items [{:line-total "10"}]}}}
            enriched (enrich :db receipt)]
        (is (false? (:supplier-guess-has-supplier? enriched)))
        (is (nil? (:supplier-guess-supplier enriched)))
        (is (= 10M (:lines-total-amount-guess enriched)))
        (is (false? (:total-guess-equals-lines-total-guess? enriched)))))))