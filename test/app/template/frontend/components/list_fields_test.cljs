(ns app.template.frontend.components.list-fields-test
  (:require
    [app.template.frontend.components.list.fields :as list-fields]
    [cljs.test :refer [are deftest is testing]]))

(deftest resolve-backlog-status-badge-test
  (testing "maps known backlog statuses to expected variants"
    (are [status expected-variant]
      (= expected-variant
        (-> (list-fields/resolve-backlog-badge "backlog-status-badge" status)
          :variant))
      "Waiting" "ds-badge-ghost"
      "In progress" "ds-badge-info"
      "Completed" "ds-badge-success"
      "Need improvements" "ds-badge-warning"))

  (testing "falls back to outline for unknown status values"
    (is (= "ds-badge-outline"
          (-> (list-fields/resolve-backlog-badge "backlog-status-badge" "Blocked")
            :variant))))

  (testing "uses Unknown label for nil status"
    (is (= "Unknown"
          (-> (list-fields/resolve-backlog-badge "backlog-status-badge" nil)
            :label)))))

(deftest resolve-backlog-type-badge-test
  (testing "maps known backlog types to expected variants"
    (are [type-value expected-variant]
      (= expected-variant
        (-> (list-fields/resolve-backlog-badge "backlog-type-badge" type-value)
          :variant))
      "Issue" "ds-badge-error"
      "Feature" "ds-badge-success"
      "Refactoring" "ds-badge-warning"
      "Review" "ds-badge-info"
      "Improvement" "ds-badge-secondary"))

  (testing "falls back to outline for unknown type values"
    (is (= "ds-badge-outline"
          (-> (list-fields/resolve-backlog-badge "backlog-type-badge" "Spike")
            :variant)))))

(deftest resolve-backlog-priority-badge-removed-test
  (testing "priority badge formatter returns nil after priority removal"
    (is (nil? (list-fields/resolve-backlog-badge "backlog-priority-badge" 1)))
    (is (nil? (list-fields/resolve-backlog-badge "backlog-priority-badge" "3")))))

(deftest resolve-backlog-badge-ignores-non-backlog-formatters
  (testing "returns nil when formatter is not a backlog formatter"
    (is (nil? (list-fields/resolve-backlog-badge "status-badge" "active")))))
