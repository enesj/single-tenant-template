(ns app.template.frontend.utils.formatting-test
  (:require
    [app.template.frontend.utils.formatting :as formatting]
    [cljs.test :refer [deftest is testing]]))

(deftest format-currency-test
  (testing "format-currency prepends dollar sign"
    (is (= "$25" (formatting/format-currency 25)))
    (is (= "$0" (formatting/format-currency nil)))))

(deftest format-percentage-test
  (testing "format-percentage respects precision"
    (is (= "12.3%" (formatting/format-percentage 12.345 :precision 1)))
    (is (= "12.35%" (formatting/format-percentage 12.345 :precision 2)))))

(deftest format-date-month-year-test
  (testing "format-date-month-year renders short month/year"
    (is (= "Mar 2024" (formatting/format-date-month-year "2024-03-15T00:00:00Z")))
    (is (nil? (formatting/format-date-month-year nil)))))

(deftest status-color-test
  (testing "get-status-color chooses correct bucket"
    (let [thresholds {:warning 70 :error 90}
          colors {:success "success" :warning "warn" :error "err"}]
      (is (= "success" (formatting/get-status-color 60 thresholds colors)))
      (is (= "warn" (formatting/get-status-color 75 thresholds colors)))
      (is (= "err" (formatting/get-status-color 95 thresholds colors))))))
