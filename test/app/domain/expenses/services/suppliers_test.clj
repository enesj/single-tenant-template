(ns app.domain.expenses.services.suppliers-test
  "Integration tests for suppliers service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest suppliers-normalization-and-dedupe-test
  (testing "normalize-supplier-key trims, lowercases, strips punctuation"
    (is (= "dm-drogerie" (suppliers/normalize-supplier-key "  DM Drogerie!  "))))
  (testing "find-or-create-supplier! dedupes by normalized key"
    (when-let [db fixtures/*test-db*]
      (let [first (suppliers/find-or-create-supplier! db "Bingo Centar" {:address "Main"})
            second (suppliers/find-or-create-supplier! db "  bingo-centar " {})]
        (is (= (:id (:supplier first)) (:id (:supplier second))))))))
