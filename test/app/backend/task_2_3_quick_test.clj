(ns app.backend.task-2-3-quick-test
  "Quick validation tests for Task 2.3 Stripe Integration Dependencies"
  (:require
    [app.domain.backend.billing-test-helpers :as helpers]
    [app.shared.schemas.domain.subscription :as sub-schemas]
    [app.shared.schemas.primitives :as prim-schemas]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [malli.core :as m]))

(deftest quick-dependency-check
  (testing "Core dependencies and configuration are in place"
    (testing "Stripe dependency exists"
      (let [deps-content (slurp "deps.edn")]
        (is (re-find #"stripe-clojure" deps-content))))

    (testing "Configuration files exist"
      (is (.exists (io/file "config/base.edn")))
      (let [config-content (slurp "config/base.edn")]
        (is (re-find #":stripe" config-content))))

    (testing "Secrets file exists"
      (is (.exists (io/file ".secrets.edn"))))))

(deftest quick-schema-validation
  (testing "Core schemas work correctly"
    (is (m/validate sub-schemas/subscription-tier :starter))
    (is (m/validate sub-schemas/subscription-status :active))
    (is (m/validate prim-schemas/currency-code "USD"))
    (is (m/validate sub-schemas/stripe-id "sub_1234567890abcdef"))))

(deftest quick-test-helpers
  (testing "Test helpers function correctly"
    (let [customer-id (helpers/generate-stripe-id "cus")
          customer    (helpers/mock-stripe-customer)
          scenario    (helpers/create-subscription-test-scenario :starter :month)]

      (is (string? customer-id))
      (is (.startsWith customer-id "cus_"))
      (is (map? customer))
      (is (contains? customer :id))
      (is (map? scenario))
      (is (contains? scenario :customer)))))

;; Quick test runner
(defn run-quick-task-2-3-tests
  "Run quick Task 2.3 validation tests"
  []
  (println "⚡ Quick Task 2.3 Validation Tests")
  (println "===================================")
  (let [results (clojure.test/run-tests 'app.backend.task-2-3-quick-test)]
    (println "\n📊 Quick Test Results:")
    (println "Tests:" (:test results))
    (println "Pass:" (:pass results))
    (println "Fail:" (:fail results))
    (if (and (zero? (:fail results)) (zero? (:error results)))
      (println "✅ Quick tests PASSED!")
      (println "❌ Quick tests FAILED!"))
    results))
