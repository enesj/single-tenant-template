(ns app.domain.backend.expenses.services.receipts.queries-test
  (:require
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(deftest list-user-receipts-total-display-sorts-by-total-amount-guess-test
  (testing "total-display order-by maps to total_amount_guess, not created_at"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                    (reset! captured-sql sql-params)
                                    [])]
        (receipt-queries/list-user-receipts
          :db
          (UUID/randomUUID)
          {:limit 20
           :offset 0
           :order-by "total-display"
           :order-dir :asc})
        (let [sql-lc (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-lc))
          (is (re-find #"order by .*total_amount_guess.*asc" sql-lc))
          (is (not (re-find #"order by .*created_at" sql-lc))))))))

(deftest list-user-receipts-hides-purged-by-default-test
  (testing "purged receipts are excluded unless show-purged? is enabled"
    (let [captured-sql (atom [])
          run! (fn [opts]
                 (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                               (swap! captured-sql conj sql-params)
                                               [])]
                   (receipt-queries/list-user-receipts
                     :db
                     (UUID/randomUUID)
                     (merge {:limit 20
                             :offset 0
                             :order-by "created-at"
                             :order-dir :desc}
                       opts))))]
      (run! {})
      (run! {:show-purged? true})
      (let [[default-sql show-purged-sql] (map (comp str/lower-case first) @captured-sql)]
        (is (re-find #"file_purged_at is null" default-sql))
        (is (not (re-find #"file_purged_at is null" show-purged-sql)))))))

(deftest list-user-receipts-selects-purchased-at-guess-for-list-view-test
  (testing "user receipts list projection includes purchased_at_guess so the visible column can render"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                    (reset! captured-sql sql-params)
                                    [])]
        (receipt-queries/list-user-receipts
          :db
          (UUID/randomUUID)
          {:limit 20
           :offset 0
           :order-by "created-at"
           :order-dir :desc})
        (let [sql-lc (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-lc))
          (is (re-find #"purchased_at_guess" sql-lc)))))))
