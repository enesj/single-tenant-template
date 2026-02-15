(ns app.template.frontend.utils.timestamp-test
  (:require
    [app.template.frontend.utils.timestamp :as timestamp]
    [cljs.test :refer [deftest is testing]]))

(deftest format-timestamp-string-canonical-style-test
  (testing "formats Created-style timestamp deterministically with UTC option"
    (is (= "Feb 15 15:10:55"
          (timestamp/format-timestamp-string "2026-02-15T15:10:55Z"
            {:use-utc? true}))))

  (testing "supports Date instances"
    (is (= "Feb 15 15:10:55"
          (timestamp/format-timestamp-string (js/Date. "2026-02-15T15:10:55Z")
            {:use-utc? true}))))

  (testing "handles nil and invalid values safely"
    (is (nil? (timestamp/format-timestamp-string nil)))
    (is (= "—" (timestamp/format-timestamp-string nil {:nil-text "—"})))
    (is (= "not-a-date" (timestamp/format-timestamp-string "not-a-date" {:use-utc? true})))))
