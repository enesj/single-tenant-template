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

(defn- unique-supplier-name
  []
  (str "Purge Test Supplier " (java.util.UUID/randomUUID)))

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id (:supplier first)) (:id (:supplier second))))))))

(deftest purge-supplier-blocked-when-active-expenses-test
  (testing "purge is blocked when supplier has active expenses"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (unique-supplier-name)
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
        payer (th/create-payer! db {:type "cash" :label "Cash"})
            _expense (expenses/create-expense!
                       db
                       {:supplier_id supplier-id
                        :payer_id (:id payer)
                        :purchased_at (java.time.Instant/now)
                        :total_amount 10M
                        :currency "BAM"
                        :items [{:raw_label "Milk" :line_total 10M}]})]

        (is (true? (suppliers/delete-supplier! db supplier-id)))

        (let [preview (suppliers/purge-supplier-preview db supplier-id)]
          (is (true? (:archived? preview)))
          (is (= 1 (:active-expenses preview)))
          (is (false? (:can-purge? preview))))

        (is (thrown-with-msg?
              clojure.lang.ExceptionInfo
              #"active expenses"
              (suppliers/purge-supplier! db supplier-id)))))))

(deftest purge-supplier-succeeds-after-soft-delete-test
  (testing "purge succeeds when supplier is archived and only soft-deleted expenses reference it"
    (when-let [db fixtures/*test-db*]
      (let [supplier-name (unique-supplier-name)
            {:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-name {})
            supplier-id (:id supplier)
        payer (th/create-payer! db {:type "cash" :label "Cash"})
            expense (expenses/create-expense!
                      db
                      {:supplier_id supplier-id
                       :payer_id (:id payer)
                       :purchased_at (java.time.Instant/now)
                       :total_amount 10M
                       :currency "BAM"
                       :items [{:raw_label "Milk" :line_total 10M}]})
            expense-id (:id expense)]

        (is (true? (suppliers/delete-supplier! db supplier-id)))
        (is (some? (expenses/soft-delete-expense! db expense-id)))

        (let [preview (suppliers/purge-supplier-preview db supplier-id)]
          (is (true? (:archived? preview)))
          (is (= 0 (:active-expenses preview)))
          (is (= 1 (:soft-deleted-expenses-total preview)))
          (is (= 1 (:soft-deleted-expense-items-total preview)))
          (is (true? (:can-purge? preview))))

        (let [result (suppliers/purge-supplier! db supplier-id)]
          (is (= true (:purged result)))
          (is (= supplier-id (:supplier-id result)))
          (is (= 1 (:deleted-expenses result)))
          (is (= 1 (:deleted-expense-items result))))

        ;; Verify supplier + expenses are truly gone (hard deleted).
        (is (nil?
              (jdbc/execute-one!
                db
                ["select id from suppliers where id = ?" supplier-id]
                {:builder-fn rs/as-unqualified-lower-maps})))

        (is (= 0
              (:c
               (jdbc/execute-one!
                 db
                 ["select count(*)::int as c from expenses where supplier_id = ?" supplier-id]
                 {:builder-fn rs/as-unqualified-lower-maps}))))

        (is (= 0
              (:c
               (jdbc/execute-one!
                 db
                 ["select count(*)::int as c from expense_items where expense_id = ?" expense-id]
                 {:builder-fn rs/as-unqualified-lower-maps}))))))))
