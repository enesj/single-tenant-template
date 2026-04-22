(ns app.domain.backend.expenses.handlers.user-expenses.settings-test
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.settings :as settings]
    [app.shared.adapters.database :as db-adapter]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [next.jdbc :as jdbc]))

(deftest load-export-expenses-uses-tenant-scope-when-present
  (let [user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        captured (atom nil)]
    (with-redefs [jdbc/execute! (fn [_db sqlvec]
                                  (reset! captured sqlvec)
                                  [{:row 1}])
                  db-adapter/to-app identity]
      (is (= [{:row 1}]
            (#'settings/load-export-expenses :mock-db user-id tenant-id)))
      (let [[sql & params] @captured]
        (is (str/includes? sql "WHERE e.tenant_id = ?"))
        (is (not (str/includes? sql "e.user_id = ? AND e.tenant_id = ?")))
        (is (= [tenant-id] params))))))

(deftest load-export-expenses-falls-back-to-user-scope-without-tenant
  (let [user-id (java.util.UUID/randomUUID)
        captured (atom nil)]
    (with-redefs [jdbc/execute! (fn [_db sqlvec]
                                  (reset! captured sqlvec)
                                  [{:row 1}])
                  db-adapter/to-app identity]
      (is (= [{:row 1}]
            (#'settings/load-export-expenses :mock-db user-id nil)))
      (let [[sql & params] @captured]
        (is (str/includes? sql "WHERE e.user_id = ?"))
        (is (= [user-id] params))))))

(deftest delete-all-expenses-uses-tenant-scope-when-present
  (let [user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        calls (atom [])]
    (with-redefs [jdbc/execute-one! (fn [_tx sqlvec]
                                      (swap! calls conj sqlvec)
                                      (if (str/includes? (first sqlvec) "DELETE FROM expenses")
                                        {:next.jdbc/update-count 7}
                                        {:next.jdbc/update-count 0}))]
      (is (= {:next.jdbc/update-count 7}
            (#'settings/delete-all-expenses! :mock-tx user-id tenant-id)))
      (let [[receipt-sql delete-sql] @calls]
        (is (str/includes? (first receipt-sql) "WHERE expense_id IN (SELECT id FROM expenses WHERE tenant_id = ?)"))
        (is (= [tenant-id] (vec (rest receipt-sql))))
        (is (str/includes? (first delete-sql) "DELETE FROM expenses WHERE tenant_id = ?"))
        (is (= [tenant-id] (vec (rest delete-sql))))))))

(deftest delete-all-expenses-falls-back-to-user-scope-without-tenant
  (let [user-id (java.util.UUID/randomUUID)
        calls (atom [])]
    (with-redefs [jdbc/execute-one! (fn [_tx sqlvec]
                                      (swap! calls conj sqlvec)
                                      (if (str/includes? (first sqlvec) "DELETE FROM expenses")
                                        {:next.jdbc/update-count 3}
                                        {:next.jdbc/update-count 0}))]
      (is (= {:next.jdbc/update-count 3}
            (#'settings/delete-all-expenses! :mock-tx user-id nil)))
      (let [[receipt-sql delete-sql] @calls]
        (is (str/includes? (first receipt-sql) "WHERE expense_id IN (SELECT id FROM expenses WHERE user_id = ?)"))
        (is (= [user-id] (vec (rest receipt-sql))))
        (is (str/includes? (first delete-sql) "DELETE FROM expenses WHERE user_id = ?"))
        (is (= [user-id] (vec (rest delete-sql))))))))