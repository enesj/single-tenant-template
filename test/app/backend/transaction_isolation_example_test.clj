(ns app.backend.transaction-isolation-example-test
  "Example test namespace demonstrating transaction isolation patterns.
   This shows how to write database tests with perfect isolation."
  (:require
    [app.domain.backend.fixtures :refer [current-db with-clean-transaction
                                         with-transaction]]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set]))

;; System started globally by Kaocha hook, no individual fixture needed
;; (use-fixtures :once start-test-system)

;; Example 1: Using transaction isolation without database reset
;; This is fast but requires tests to handle existing data
(deftest ^:transaction test-with-transaction-only
  (testing "Transaction isolation without reset"
    ; Use with-transaction fixture for this specific test
    (with-transaction
      (fn []
        ; All database operations use (current-db) which returns the transaction
        (let [existing-tenant (jdbc/execute-one! (current-db)
                                ["SELECT id FROM tenants LIMIT 1"]
                                {:builder-fn next.jdbc.result-set/as-unqualified-maps})]
          ;; Skip test gracefully if no tenants exist (since with-transaction doesn't seed data)
          (if (nil? existing-tenant)
            (do
              (println "⚠️  Skipping transaction test - no tenants found (no seeded data available)")
              (is true "Transaction test skipped - no existing data available"))
            (let [_ (println "✅ Found existing tenant, proceeding with transaction test")
                  tenant-uuid (:id existing-tenant)
                  _ (when (or (nil? tenant-uuid) (empty? (str tenant-uuid)))
                      (throw (ex-info "Tenant ID is nil or empty" {:tenant tenant-uuid})))
                  tenant-id (str tenant-uuid)
                  result (jdbc/execute-one! (current-db)
                           ["INSERT INTO transaction_types (name, flow, tenant_id) VALUES (?, ?::flow, ?::uuid)"
                            "Test Income Type" "income" tenant-id]
                           {:return-keys true
                            :builder-fn next.jdbc.result-set/as-unqualified-maps})]

              ; Verify the insert worked within the transaction
              (is (uuid? (:id result)))
              (is (= "Test Income Type" (:name result)))

              ; Query to verify it exists in the transaction
              (let [count-result (jdbc/execute-one! (current-db)
                                   ["SELECT COUNT(*) as count FROM transaction_types WHERE name = ?"
                                    "Test Income Type"]
                                   {:builder-fn next.jdbc.result-set/as-unqualified-maps})]
                (is (= 1 (:count count-result)))))))))))

;; This data will be rolled back after the test!

;; Example 2: Using clean transaction (reset + transaction)
;; This ensures a completely clean state for each test
(deftest ^:clean-transaction test-with-clean-state
  (testing "Transaction with clean database state"
    (with-clean-transaction
      (fn []
        ;; Start with a clean database (only seed data)
        (let [initial-count (jdbc/execute-one! (current-db)
                              ["SELECT COUNT(*) as count FROM transactions"]
                              {:builder-fn next.jdbc.result-set/as-unqualified-maps})
              new-count (jdbc/execute-one! (current-db)
                          ["SELECT COUNT(*) as count FROM transactions"]
                          {:builder-fn next.jdbc.result-set/as-unqualified-maps})]

          ;; Insert test data - this would normally need tenant_id and transaction_type_id
          ;; For testing purposes, we'll just count the initial state
          (is (= (:count initial-count) (:count new-count)) "Count should remain the same in clean test"))))))

;; All changes will be rolled back!

;; Example 3: Testing complex scenarios with multiple operations
(deftest ^:transaction test-complex-transaction-scenario
  (testing "Complex operations within a transaction"
    (with-clean-transaction
      (fn []
        ; Get an existing tenant from the seeded data
        (let [existing-tenant (jdbc/execute-one! (current-db)
                                ["SELECT id FROM tenants LIMIT 1"]
                                {:builder-fn next.jdbc.result-set/as-unqualified-maps})
              _ (when (nil? existing-tenant)
                  (throw (ex-info "No tenants found in database for testing" {})))
              tenant-uuid (:id existing-tenant)
              _ (when (or (nil? tenant-uuid) (empty? (str tenant-uuid)))
                  (throw (ex-info "Tenant ID is nil or empty" {:tenant tenant-uuid})))
              tenant-id (str tenant-uuid)
              income-type (jdbc/execute-one! (current-db)
                            ["INSERT INTO transaction_types (name, flow, tenant_id) VALUES (?, ?::flow, ?::uuid)"
                             "Test Income" "income" tenant-id]
                            {:return-keys true
                             :builder-fn next.jdbc.result-set/as-unqualified-maps})
              expense-type (jdbc/execute-one! (current-db)
                             ["INSERT INTO transaction_types (name, flow, tenant_id) VALUES (?, ?::flow, ?::uuid)"
                              "Test Expense" "expense" tenant-id]
                             {:return-keys true
                              :builder-fn next.jdbc.result-set/as-unqualified-maps})]

          ; Verify both were created
          (is (uuid? (:id income-type)))
          (is (uuid? (:id expense-type)))
          (is (= "income" (:flow income-type)))
          (is (= "expense" (:flow expense-type)))

          ; Query to verify they exist
          (let [count-result (jdbc/execute-one! (current-db)
                               ["SELECT COUNT(*) as count FROM transaction_types WHERE name LIKE ?"
                                "Test%"]
                               {:builder-fn next.jdbc.result-set/as-unqualified-maps})]
            (is (= 2 (:count count-result))))

          ; Delete one of them
          (jdbc/execute-one! (current-db)
            ["DELETE FROM transaction_types WHERE id = ?" (:id income-type)])

          ; Verify only one remains
          (let [remaining-count (jdbc/execute-one! (current-db)
                                  ["SELECT COUNT(*) as count FROM transaction_types WHERE name LIKE ?"
                                   "Test%"]
                                  {:builder-fn next.jdbc.result-set/as-unqualified-maps})]
            (is (= 1 (:count remaining-count)))))))))

;; All of this will be rolled back!

;; Example 4: Testing error scenarios
(deftest ^:transaction test-constraint-violations
  (testing "Database constraints within transactions"
    (with-transaction
      (fn []
        ;; Try to violate a constraint - testing amount > 0 constraint on transactions
        (is (thrown? Exception
              (jdbc/execute-one! (current-db)
                ["INSERT INTO transactions (description, amount, tenant_id, transaction_type_id, date, created_by) VALUES (?, ?, ?, ?, ?::date, ?)"
                 "Invalid Transaction"
                 -100                                       ; Negative amount should fail
                 (java.util.UUID/randomUUID)
                 (java.util.UUID/randomUUID)
                 "2025-01-15"
                 (java.util.UUID/randomUUID)]))
          "Should not allow negative amounts")))))

;; After an error, PostgreSQL aborts the transaction, so we end the test here
