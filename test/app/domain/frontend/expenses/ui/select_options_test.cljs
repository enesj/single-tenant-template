(ns app.domain.frontend.expenses.ui.select-options-test
  (:require
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [cljs.test :refer [deftest is testing]]))

(deftest supplier-label-prefers-kebab-case
  (testing "Uses :display-name when present"
    (is (= "DM" (select-options/supplier-label {:id "1" :display-name "DM" :display_name "ignored"})))))

(deftest supplier-label-falls-back-to-snake-case
  (testing "Uses :display_name when kebab-case is missing"
    (is (= "Konzum" (select-options/supplier-label {:id "1" :display_name "Konzum"})))))

(deftest supplier-label-falls-back-to-id
  (testing "Uses :id as a last resort"
    (is (= "abc" (select-options/supplier-label {:id "abc"})))))
