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
