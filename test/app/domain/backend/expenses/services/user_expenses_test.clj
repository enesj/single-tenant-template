(ns app.domain.backend.expenses.services.user-expenses-test
  (:require
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(deftest list-user-expenses-selects-store-display-name
  (let [captured-sql (atom nil)]
    (with-redefs [jdbc/execute! (fn [_db sql _opts]
                                  (reset! captured-sql sql)
                                  [])]
      (user-expenses/list-user-expenses :db nil (UUID/randomUUID) {:limit 10 :offset 0})
      (let [sql-str (first @captured-sql)]
        (testing "query includes store display name projection"
          (is (re-find #"(?i)store_display_name" sql-str)))
        (testing "query joins stores table through expense store_id"
          (is (re-find #"(?i)join\s+stores\s+(?:as\s+)?st\s+on\s+st\.id\s*=\s*e\.store_id" sql-str)))))))

(deftest list-user-expenses-supports-store-display-name-sorting
  (let [captured-sql (atom nil)]
    (with-redefs [jdbc/execute! (fn [_db sql _opts]
                                  (reset! captured-sql sql)
                                  [])]
      (user-expenses/list-user-expenses :db nil (UUID/randomUUID)
        {:limit 10
         :offset 0
         :order-by :store-display-name
         :order-dir :asc})
      (let [sql-str (first @captured-sql)]
        (is (re-find #"(?i)order\s+by\s+st\.display_name\s+asc" sql-str))))))

(deftest get-user-expense-summary-respects-report-range-and-filters
  (let [captured-count-sql (atom nil)
        captured-total-sql (atom nil)
        tenant-id (UUID/randomUUID)
        user-id (UUID/randomUUID)
        supplier-id (UUID/randomUUID)
        expense-category-id (UUID/randomUUID)]
    (with-redefs [jdbc/execute-one! (fn [_db sql _opts]
                                      (reset! captured-count-sql sql)
                                      {:total 7})
                  jdbc/execute! (fn [_db sql _opts]
                                  (reset! captured-total-sql sql)
                                  [{:currency "BAM" :total 42.50M}])]
      (let [summary (user-expenses/get-user-expense-summary
                      :db
                      tenant-id
                      user-id
                      {:from "2026-03-01T00:00:00Z"
                       :to "2026-03-31T23:59:59Z"
                       :supplier-id supplier-id
                       :expense-category-id expense-category-id})
            count-sql-str (first @captured-count-sql)
            total-sql-str (first @captured-total-sql)]
        (is (= 7 (:total-expenses summary)))
        (is (= {"BAM" 42.50M} (:currency-totals summary)))
        (is (= 7 (:recent-count summary)))
        (is (re-find #"(?i)is_posted" count-sql-str))
        (is (re-find #"(?i)purchased_at\s*>=|purchased_at\s*>=" count-sql-str))
        (is (re-find #"(?i)purchased_at\s*<=|purchased_at\s*<=" count-sql-str))
        (is (re-find #"(?i)supplier_id" count-sql-str))
        (is (re-find #"(?i)expense_category_id" count-sql-str))
        (is (re-find #"(?i)group\s+by\s+currency" total-sql-str))))))
