(ns app.template.frontend.auto-test-data-test
  "Tests for the auto test data generation system"
  (:require
    [app.template.frontend.auto-test-data :as auto-test-data]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is run-tests testing]]))

(deftest test-entity-generation
  (testing "Generate single entity"
    (helpers/reset-test-data!)

    (testing "properties entity"
      (let [property (auto-test-data/generate-entity :properties)]
        (is (map? property) "Should return a map")
        (is (string? (:name property)) "Should have name")
        (is (string? (:tenant_id property)) "Should have tenant_id")
        (is (string? (:owner_id property)) "Should have owner_id")
        (is (#{"apartment" "house" "room" "commercial" "other"} (:property_type property)) "Should have valid property type")))

    (testing "transaction_types entity"
      (let [tt (auto-test-data/generate-entity :transaction_types)]
        (is (map? tt) "Should return a map")
        (is (string? (:name tt)) "Should have name")
        (is (#{"income" "expense"} (:flow tt)) "Should have valid flow")))

    (testing "transactions entity with foreign key"
      ;; First create a transaction type for foreign key
      (auto-test-data/reset-id-counter!)
      (let [test-data (auto-test-data/generate-comprehensive-test-data {:seed-count 1})
            transaction (get-in test-data [:transactions :new-data])]
        (is (map? transaction) "Should return a map")
        (is (string? (:description transaction)) "Should have description")
        (is (number? (:amount transaction)) "Should have amount")
        (is (string? (:transaction_type_id transaction)) "Should have transaction_type_id foreign key")
        (is (string? (:date transaction)) "Should have date")))))

(deftest test-multiple-entities-generation
  (testing "Generate multiple entities"
    (helpers/reset-test-data!)

    (let [properties (auto-test-data/generate-entities :properties 5)]
      (is (= 5 (count properties)) "Should generate exact count")
      (is (every? map? properties) "All should be maps")
      (is (every? #(string? (:name %)) properties) "All should have names")
      (is (every? #(string? (:tenant_id %)) properties) "All should have tenant_id"))))

(deftest test-invalid-entity-generation
  (testing "Generate invalid entities for validation testing"
    (helpers/reset-test-data!)

    (testing "invalid properties"
      (let [invalid-property (auto-test-data/generate-invalid-entity :properties)]
        (is (map? invalid-property) "Should return a map")
        ;; Check for invalid values
        (is (or (= "" (:name invalid-property))
              (not (string? (:name invalid-property))))
          "Name should be invalid")))))

(deftest test-comprehensive-data-generation
  (testing "Generate comprehensive test data for all entities"
    (helpers/reset-test-data!)

    (let [all-data (auto-test-data/get-auto-generated-data {:seed-count 3})]
      (is (map? all-data) "Should return a map")

      (testing "transaction_types data"
        (let [tt-data (:transaction_types all-data)]
          (is (= 3 (count (:seed-data tt-data))) "Should have seed data")
          (is (map? (:new-data tt-data)) "Should have new data")
          (is (map? (:update-data tt-data)) "Should have update data")
          (is (map? (:invalid-create-data tt-data)) "Should have invalid data")))

      (testing "properties data"
        (let [properties-data (:properties all-data)]
          (is (= 3 (count (:seed-data properties-data))) "Should have seed data")
          (is (map? (:new-data properties-data)) "Should have new data")
          (is (map? (:update-data properties-data)) "Should have update data")
          (is (map? (:invalid-create-data properties-data)) "Should have invalid data")))

      (testing "transactions data with foreign keys"
        (let [trans-data (:transactions all-data)]
          (is (= 3 (count (:seed-data trans-data))) "Should have seed data")
          (is (every? #(string? (:transaction_type_id %)) (:seed-data trans-data))
            "All transactions should have foreign key"))))))

(deftest test-helper-integration
  (testing "Helper functions integration"
    (helpers/reset-test-data!)

    (testing "generate-test-entity with new format"
      (let [property (helpers/generate-test-entity :properties {:custom "value"})]
        (is (map? property) "Should return a map")
        (is (= "value" (:custom property)) "Should apply overrides")
        (is (string? (:name property)) "Should have auto-generated fields")))

    (testing "entity-specific helpers"
      (let [properties (helpers/generate-test-entities :properties 2)
            transactions (helpers/generate-test-transactions 2)
            types (helpers/generate-test-transaction-types 2)]
        (is (= 2 (count properties)) "Should generate correct count of properties")
        (is (= 2 (count transactions)) "Should generate correct count of transactions")
        (is (= 2 (count types)) "Should generate correct count of transaction types")))))

(defn run-all-tests []
  (println "🧪 Running Auto Test Data Generation Tests...")
  (run-tests))

;; Export for browser testing
(set! js/window.runAutoTestDataTests run-all-tests)
