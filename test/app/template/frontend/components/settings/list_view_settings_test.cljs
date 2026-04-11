(ns app.template.frontend.components.settings.list-view-settings-test
  (:require
    [app.template.frontend.components.settings.list-view-settings :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest displayed-table-height-prefers-live-measurement-test
  (testing "positive measured height wins over missing or non-positive stored height"
    (is (= 1155 (sut/displayed-table-height 1155 0)))
    (is (= 450 (sut/displayed-table-height 450 nil)))
    (is (= 320 (sut/displayed-table-height nil 320)))
    (is (nil? (sut/displayed-table-height 0 0)))
    (is (nil? (sut/displayed-table-height nil nil)))))