(ns app.domain.backend.expenses.services.raw-labels-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.raw-labels :as raw-labels]
    [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
    [java.util UUID]))

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

(deftest list-raw-labels-orders-and-paginates
  (testing "orders by a stable column and supports limit/offset"
    (when-let [db fixtures/*test-db*]
      ;; Tests run against a shared migrated DB; avoid assuming the table is empty.
      (let [token (str "raw-labels-test-" (UUID/randomUUID))
            l1 (str token "-1")
            l2 (str token "-2")
            l3 (str token "-3")]
        ;; Insert 3 distinct normalized keys.
        (doseq [lbl [l1 l2 l3]]
          (raw-labels/find-or-create-raw-label! db lbl))

        ;; Filter to just our inserted rows, then paginate.
        (let [rows (raw-labels/list-raw-labels db {:search token
                                                   :limit 2
                                                   :offset 1
                                                   :order-by :raw_label
                                                   :order-dir :asc})
              labels (mapv :raw_label rows)]
          (is (= 2 (count rows)))
          (is (= [l2 l3] labels)))))))

(deftest list-raw-labels-search
  (testing "search matches raw_label (case-insensitive)"
    (when-let [db fixtures/*test-db*]
      (let [token (str "raw-labels-test-" (UUID/randomUUID))
            milk (str token "-Milk 1L")
            bread (str token "-Bread")]
        (raw-labels/find-or-create-raw-label! db milk)
        (raw-labels/find-or-create-raw-label! db bread)

        (let [rows (raw-labels/list-raw-labels db {:search "milk"
                                                   :order-by :raw_label
                                                   :order-dir :asc})
              labels (set (map :raw_label rows))]
          (is (contains? labels milk))
          ;; Only assert our control label does not appear in the milk search results.
          (is (not (contains? labels bread))))))))
