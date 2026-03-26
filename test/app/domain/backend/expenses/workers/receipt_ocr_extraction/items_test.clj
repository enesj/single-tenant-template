(ns app.domain.backend.expenses.workers.receipt-ocr-extraction.items-test
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.items :as extraction-items]
    [clojure.test :refer [deftest is testing]]))

(def repair-item-prices #'extraction-items/repair-item-prices)

(deftest repair-item-prices-keeps-consistent-item
  (let [item {:raw_label "ITEM"
              :qty 2M
              :unit_price 6.60M
              :line_total 13.20M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is (false? repaired?))
    (is (= item repaired-item))))

(deftest repair-item-prices-recalculates-unit-price-from-line-total
  (let [item {:raw_label "CIG DUNHILL ESSENCE BRONZE"
              :qty 1M
              :unit_price 3.00M
              :line_total 19.80M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is repaired?)
    (is (= 19.80M (:unit_price repaired-item)))
    (is (= true (:price_repaired repaired-item)))))

(deftest repair-item-prices-skips-when-qty-is-nil
  (let [item {:raw_label "ITEM"
              :qty nil
              :unit_price 3.00M
              :line_total 19.80M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is (false? repaired?))
    (is (= item repaired-item))))

(deftest repair-item-prices-skips-when-qty-is-zero
  (let [item {:raw_label "ITEM"
              :qty 0M
              :unit_price 3.00M
              :line_total 19.80M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is (false? repaired?))
    (is (= item repaired-item))))

(deftest repair-item-prices-skips-when-unit-price-is-nil
  (let [item {:raw_label "ITEM"
              :qty 3M
              :unit_price nil
              :line_total 19.80M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is (false? repaired?))
    (is (= item repaired-item))))

(deftest repair-item-prices-keeps-values-within-tolerance
  (let [item {:raw_label "ITEM"
              :qty 3M
              :unit_price 6.60M
              :line_total 19.81M}
        [repaired-item repaired?] (repair-item-prices item)]
    (is (false? repaired?))
    (is (= item repaired-item))))

(deftest clean-extraction-items-tracks-price-repairs
  (testing "price repairs produce post-processing metadata even when nothing was dropped"
    (let [{:keys [items post-processing]}
          (extraction-items/clean-extraction-items
            [{:raw_label "CIG DUNHILL ESSENCE BRONZE"
              :qty 1M
              :unit_price 3.00M
              :line_total 19.80M}]
            {:items-count 1
             :grand-total 19.80M})]
      (is (= 1 (count items)))
      (is (= 19.80M (get-in items [0 :unit_price])))
      (is (= true (get-in items [0 :price_repaired])))
      (is (= 1 (:price-repairs post-processing))))))