(ns app.template.frontend.components.settings.list-view-settings-test
  (:require
    [app.template.frontend.components.settings.list-view-settings :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest displayed-table-width-prefers-live-measurement-test
  (testing "positive measured width wins over missing or non-positive stored width"
    (is (= 1155 (sut/displayed-table-width 1155 0)))
    (is (= 950 (sut/displayed-table-width 950 nil)))
    (is (= 1400 (sut/displayed-table-width nil 1400)))
    (is (nil? (sut/displayed-table-width 0 0)))
    (is (nil? (sut/displayed-table-width nil nil)))))

(deftest displayed-table-height-prefers-live-measurement-test
  (testing "positive measured height wins over missing or non-positive stored height"
    (is (= 1155 (sut/displayed-table-height 1155 0)))
    (is (= 450 (sut/displayed-table-height 450 nil)))
    (is (= 320 (sut/displayed-table-height nil 320)))
    (is (nil? (sut/displayed-table-height 0 0)))
    (is (nil? (sut/displayed-table-height nil nil)))))