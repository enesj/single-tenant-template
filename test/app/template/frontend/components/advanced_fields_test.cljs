(ns app.template.frontend.components.advanced-fields-test
  (:require
    [app.template.frontend.components.advanced-fields :as advanced-fields]
    [cljs.test :refer [are deftest is testing]]))

(deftest status->badge-class-test
  (testing "status->badge-class maps common statuses to DaisyUI variants"
    (are [status expected] (= expected (advanced-fields/status->badge-class status))
      "active" "ds-badge-success"
      "verified" "ds-badge-success"
      "success" "ds-badge-success"
      "complete" "ds-badge-success"

      "inactive" "ds-badge-warning"
      "trialing" "ds-badge-warning"
      "unverified" "ds-badge-warning"

      "suspended" "ds-badge-error"
      "cancelled" "ds-badge-error"
      "canceled" "ds-badge-error"
      "failed" "ds-badge-error"
      "error" "ds-badge-error"

      "invited" "ds-badge-info"
      "pending" "ds-badge-info"
      "in-progress" "ds-badge-info"

      "archived" "ds-badge-ghost"))

  (testing "status->badge-class is case-insensitive and supports keywords"
    (is (= "ds-badge-success" (advanced-fields/status->badge-class "ACTIVE")))
    (is (= "ds-badge-success" (advanced-fields/status->badge-class :active)))
    (is (= "ds-badge-error" (advanced-fields/status->badge-class :suspended))))

  (testing "status->badge-class falls back to default"
    (is (= "ds-badge-neutral" (advanced-fields/status->badge-class nil)))
    (is (= "ds-badge-neutral" (advanced-fields/status->badge-class "wat")))
    (is (= "ds-badge-outline" (advanced-fields/status->badge-class "wat" {:default-class "ds-badge-outline"}))))
  )
