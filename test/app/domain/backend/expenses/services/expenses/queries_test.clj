(ns app.domain.backend.expenses.services.expenses.queries-test
  (:require
    [app.domain.backend.expenses.services.expenses.queries :as queries]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

(deftest list-expenses-selects-item-count
  (testing "admin expense list includes a per-expense line item count"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                    (reset! captured-sql sql-params)
                                    [])]
        (queries/list-expenses ::db {:limit 10 :offset 0 :order-dir :desc})
        (let [sql-text (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-text))
          (is (str/includes? sql-text "expense_items"))
          (is (str/includes? sql-text "item_count"))
          (is (or (str/includes? sql-text "ei.expense_id = e.id")
                (str/includes? sql-text "\"ei\".\"expense_id\" = \"e\".\"id\"")))
          (is (or (str/includes? sql-text "ei.tenant_id = e.tenant_id")
                (str/includes? sql-text "\"ei\".\"tenant_id\" = \"e\".\"tenant_id\""))))))))

        (deftest list-expenses-orders-by-item-count
          (testing "admin expense list allows sorting by the computed item count"
            (let [captured-sql (atom nil)]
          (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                    (reset! captured-sql sql-params)
                    [])]
            (queries/list-expenses ::db {:limit 10
                     :offset 0
                     :order-by :item-count
                     :order-dir :asc})
            (let [sql-text (some-> @captured-sql first str str/lower-case)]
              (is (string? sql-text))
              (is (str/includes? sql-text "order by"))
              (is (or (str/includes? sql-text "order by item_count asc")
                (str/includes? sql-text "order by \"item_count\" asc")))
              (is (or (str/includes? sql-text "e.id asc")
                (str/includes? sql-text "\"e\".\"id\" asc"))))))))