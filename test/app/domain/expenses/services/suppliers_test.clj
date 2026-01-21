(ns app.domain.expenses.services.suppliers-test
  "Integration tests for suppliers service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id (:supplier first)) (:id (:supplier second))))))))

(deftest delete-supplier-blocked-when-expenses-exist
  (testing "delete is blocked when supplier has expenses (FK RESTRICT)"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (str "Delete Supplier Blocked " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
            payer (th/create-payer! db {:type "cash" :label "Cash"})]
        (expenses/create-expense!
          db
          {:supplier_id supplier-id
           :payer_id (:id payer)
           :purchased_at (java.time.Instant/now)
           :total_amount 10M
           :currency "BAM"
           :items [{:raw_label "Milk" :line_total 10M}]})

        (try
          (suppliers/delete-supplier! db supplier-id)
          (is false "Expected delete to fail due to FK restrict")
          (catch org.postgresql.util.PSQLException e
            (is (= "23503" (.getSQLState e)))))))))

(deftest delete-supplier-succeeds-without-expenses
  (testing "delete succeeds when supplier has no expenses"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (str "Delete Supplier OK " (java.util.UUID/randomUUID))
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
            deleted (suppliers/delete-supplier! db supplier-id)]
        (is deleted)
        (is (nil?
              (jdbc/execute-one!
                db
                ["select id from suppliers where id = ?" supplier-id]
                {:builder-fn rs/as-unqualified-lower-maps})))))))
