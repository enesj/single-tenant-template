(ns app.template.frontend.auto-test-data-test
  "Tests for the auto test data generation system"
  (:require
    [app.template.frontend.auto-test-data :as auto-test-data]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is run-tests testing]]))

(deftest test-entity-generation
  (testing "Generate single entity"
    (helpers/reset-test-data!)

    (testing "suppliers entity"
      (let [supplier (auto-test-data/generate-entity :suppliers)]
        (is (map? supplier) "Should return a map")
        (is (string? (:display_name supplier)) "Should have display_name")
        (is (string? (:normalized_key supplier)) "Should have normalized_key")))

    (testing "payers entity"
      (let [payer (auto-test-data/generate-entity :payers)]
        (is (map? payer) "Should return a map")
        (is (string? (:label payer)) "Should have label")
        (is (#{"system" "custom"} (:type payer)) "Should have valid payer type")))

    (testing "expenses entity with foreign keys"
      (auto-test-data/reset-id-counter!)
      (let [test-data (auto-test-data/get-auto-generated-data {:seed-count 1})
            expense (get-in test-data [:expenses :new-data])]
        (is (map? expense) "Should return a map")
        (is (string? (:supplier_id expense)) "Should have supplier_id foreign key")
        (is (string? (:payer_id expense)) "Should have payer_id foreign key")
        (is (string? (:purchased_at expense)) "Should have purchased_at")
        (is (number? (:total_amount expense)) "Should have total_amount")))))

(deftest test-multiple-entities-generation
  (testing "Generate multiple entities"
    (helpers/reset-test-data!)

    (let [suppliers (auto-test-data/generate-entities :suppliers 5)]
      (is (= 5 (count suppliers)) "Should generate exact count")
      (is (every? map? suppliers) "All should be maps")
      (is (every? #(string? (:display_name %)) suppliers) "All should have display_name")
      (is (every? #(string? (:normalized_key %)) suppliers) "All should have normalized_key"))))

(deftest test-invalid-entity-generation
  (testing "Generate invalid entities for validation testing"
    (helpers/reset-test-data!)

    (testing "invalid suppliers"
      (let [invalid-supplier (auto-test-data/generate-invalid-entity :suppliers)]
        (is (map? invalid-supplier) "Should return a map")
        ;; Check for invalid values
        (is (or (= "" (:display_name invalid-supplier))
              (not (string? (:display_name invalid-supplier))))
          "display_name should be invalid")))))

(deftest test-comprehensive-data-generation
  (testing "Generate comprehensive test data for all entities"
    (helpers/reset-test-data!)

    (let [all-data (auto-test-data/get-auto-generated-data {:seed-count 3})]
      (is (map? all-data) "Should return a map")

      (testing "users data"
        (let [users-data (:users all-data)]
          (is (= 3 (count (:seed-data users-data))) "Should have seed data")
          (is (map? (:new-data users-data)) "Should have new data")
          (is (map? (:update-data users-data)) "Should have update data")
          (is (map? (:invalid-create-data users-data)) "Should have invalid data")))

      (testing "expenses data with foreign keys"
        (let [expenses-data (:expenses all-data)]
          (is (= 3 (count (:seed-data expenses-data))) "Should have seed data")
          (is (every? #(string? (:supplier_id %)) (:seed-data expenses-data))
            "All expenses should have supplier_id foreign key")
          (is (every? #(string? (:payer_id %)) (:seed-data expenses-data))
            "All expenses should have payer_id foreign key"))))))

(deftest test-helper-integration
  (testing "Helper functions integration"
    (helpers/reset-test-data!)

    (testing "generate-test-entity with new format"
      (let [supplier (helpers/generate-test-entity :suppliers {:custom "value"})]
        (is (map? supplier) "Should return a map")
        (is (= "value" (:custom supplier)) "Should apply overrides")
        (is (string? (:display_name supplier)) "Should have auto-generated fields")))

    (testing "entity-specific helpers"
      (let [suppliers (helpers/generate-test-entities :suppliers 2)
            receipts (helpers/generate-test-receipts 2)
            expenses (helpers/generate-test-expenses 2)]
        (is (= 2 (count suppliers)) "Should generate correct count of suppliers")
        (is (= 2 (count receipts)) "Should generate correct count of receipts")
        (is (= 2 (count expenses)) "Should generate correct count of expenses")))))

(defn run-all-tests []
  (println "🧪 Running Auto Test Data Generation Tests...")
  (run-tests))

;; Export for browser testing
(set! js/window.runAutoTestDataTests run-all-tests)
