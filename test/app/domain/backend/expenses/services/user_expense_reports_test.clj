(ns app.domain.backend.expenses.services.user-expense-reports-test
  (:require
    [app.domain.backend.expenses.services.user-expense-reports :as service]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(defn- sql-text
  [sql-params]
  (-> sql-params first str str/lower-case))

(defn- has-distinct-receipt-count?
  [sql]
  (boolean
    (re-find #"count\s*\(\s*distinct\s+(?:\"?e\"?\.)?\"?id\"?\s*\)"
      (or sql ""))))

(deftest line-count-uses-distinct-posted-receipts-test
  (let [user-id (str (UUID/randomUUID))
        supplier-id (str (UUID/randomUUID))]
    (testing "supplier deep-dive top aliases :line_count counts distinct receipt ids"
      (let [captured-sql (atom [])]
        (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                      (swap! captured-sql conj (sql-text sql-params))
                                      [])
                      jdbc/execute-one! (fn [_db _sql-params _opts]
                                          {:display_name "Any Supplier"})]
          (service/get-user-supplier-deep-dive
            :db
            user-id
            {:supplier-id supplier-id
             :alias-limit 5})
          (let [top-aliases-sql (first (filter #(str/includes? % "from expense_items") @captured-sql))]
            (is (string? top-aliases-sql))
            (is (has-distinct-receipt-count? top-aliases-sql))
            (is (str/includes? top-aliases-sql "as line_count"))))))

    (testing "top global items :line_count counts distinct receipt ids"
      (let [captured-sql (atom nil)]
        (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                      (reset! captured-sql (sql-text sql-params))
                                      [])]
          (service/get-user-top-item-spending :db user-id {:limit 20})
          (is (string? @captured-sql))
          (is (str/includes? @captured-sql "from expense_items"))
          (is (has-distinct-receipt-count? @captured-sql))
          (is (str/includes? @captured-sql "as line_count")))))

    (testing "category allocation :line_count counts distinct receipt ids"
      (let [captured-sql (atom nil)]
        (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                      (reset! captured-sql (sql-text sql-params))
                                      [])]
          (service/get-user-category-allocation :db user-id {})
          (is (string? @captured-sql))
          (is (str/includes? @captured-sql "from expense_items"))
          (is (has-distinct-receipt-count? @captured-sql))
          (is (str/includes? @captured-sql "as line_count")))))))
