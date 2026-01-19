(ns app.domain.backend.expenses.services.raw-labels-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.raw-labels :as raw-labels]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest raw-labels-find-or-create-dedupes-by-normalized-key
  (testing "same normalized label returns same raw_labels row"
    (when-let [db fixtures/*test-db*]
      (let [r1 (raw-labels/find-or-create-raw-label! db "MILK 1L")
            r2 (raw-labels/find-or-create-raw-label! db "milk 1l")]
        (is (some? (:id r1)))
        (is (= (:id r1) (:id r2)))
        (is (some? (:normalized_key r1)))
        (is (= (:normalized_key r1) (:normalized_key r2)))))))
