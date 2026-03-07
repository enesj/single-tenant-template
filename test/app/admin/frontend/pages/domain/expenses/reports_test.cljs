(ns app.admin.frontend.pages.domain.expenses.reports-test
  (:require
    [app.admin.frontend.pages.domain.expenses.reports :as reports]
    [cljs.test :refer [deftest is testing]]))

(def top-item-unit-label @#'reports/top-item-unit-label)
(def top-item-unit-price @#'reports/top-item-unit-price)
(def enrich-top-item-row @#'reports/enrich-top-item-row)
(def sort-data @#'reports/sort-data)

(defn- approx=
  [expected actual]
  (< (js/Math.abs (- expected actual)) 1.0e-6))

(deftest top-items-report-detects-common-unit-labels
  (testing "item labels map to the intended display units"
    (is (= "kg" (top-item-unit-label {:article-canonical-name "Juneci vrat/kg"})))
    (is (= "kom" (top-item-unit-label {:alias-label "BULLDOG GIN SA ČAŠOM 0,7/KO"})))
    (is (= "l" (top-item-unit-label {:alias-label "Maslinovo ulje /L"})))
    (is (nil? (top-item-unit-label {:alias-label "Unknown thing"})))))

(deftest top-items-report-derives-weighted-unit-prices
  (testing "unit prices come from total divided by qty when qty is usable"
    (is (approx= 1.55 (top-item-unit-price {:total-amount 37.20 :qty-total 24})))
    (is (approx= 40.0 (top-item-unit-price {:total-amount 22.04 :qty-total 0.551})))
    (is (nil? (top-item-unit-price {:total-amount 10 :qty-total 0})))
    (let [row (enrich-top-item-row {:alias-label "Teleci sol/kg"
                                    :total-amount 22.04
                                    :qty-total 0.551})]
      (is (= "kg" (:unit-label row)))
      (is (approx= 40.0 (:derived-unit-price row))))))

(deftest top-items-report-sorts-article-display-labels-safely
  (testing "article labels that start with quantities still sort as labels"
    (let [rows (mapv enrich-top-item-row
                 [{:alias-label "0,5L Coca Cola"}
                  {:article-canonical-name "Laminiranje obrva + farbanje obrva + botox/kom"}
                  {:alias-label "Apple Juice"}
                  {:article-canonical-name "2Fast 2Furious"}])
          sorted (sort-data rows {:column :article-display-name :direction :asc})]
      (is (= ["0,5L Coca Cola"
              "2Fast 2Furious"
              "Apple Juice"
              "Laminiranje obrva + farbanje obrva + botox/kom"]
            (mapv :article-display-name sorted))))))

(deftest top-items-report-sorts-numeric-columns-numerically
  (testing "numeric columns still sort by numeric value when the backend sends strings"
    (let [rows [{:total-amount "10,5"}
                {:total-amount 2}
                {:total-amount "7"}]
          sorted (sort-data rows {:column :total-amount :direction :asc})]
      (is (= [2 "7" "10,5"]
            (mapv :total-amount sorted))))))
