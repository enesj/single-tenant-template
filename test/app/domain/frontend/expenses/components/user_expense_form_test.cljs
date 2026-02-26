(ns app.domain.frontend.expenses.components.user-expense-form-test
  (:require
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]
    [cljs.test :refer [deftest is testing]]))

(deftest qty-step-precision-test
  (testing "User expense form qty supports 3 decimal places"
    (let [spec (specs/get-expense-form-spec [] [] nil)
          items-field (some (fn [f] (when (= :items (:id f)) f)) spec)
          columns (:columns items-field)
          qty-col (some (fn [c] (when (= :qty (:id c)) c)) columns)]
      (is (= "0.001" (:step qty-col))))))
