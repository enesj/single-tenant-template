(ns app.domain.frontend.expenses.pages.user.payers-test
  (:require
    [app.domain.frontend.expenses.pages.user.payers :as payers]
    [cljs.test :refer-macros [deftest is testing]]))

(deftest payer-row-class-highlights-system-rows-only
  (let [payer-row-class @#'payers/payer-row-class]
    (testing "system payer rows get the custom border class"
      (is (re-find #"border-l-4" (payer-row-class {:payer-type-is-system true})))
      (is (re-find #"border-y-primary/20" (payer-row-class {:payer_type_is_system true}))))

    (testing "non-system payer rows do not receive a custom class"
      (is (nil? (payer-row-class {:payer-type-is-system false})))
      (is (nil? (payer-row-class {:label "Regular payer"}))))))
