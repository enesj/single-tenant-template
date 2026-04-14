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

(deftest list-user-receipts-applies-created-by-and-date-filters-test
  (testing "user receipts queries apply created-by text and date range filters"
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
           :order-dir :desc
           :created-by-name "Enes"
           :purchased-at-guess-from "2026-03-21T00:00:00.000Z"
           :purchased-at-guess-to "2026-03-22T00:00:00.000Z"
           :created-at-from "2026-03-30T00:00:00.000Z"
           :updated-at-to "2026-03-31T00:00:00.000Z"})
        (let [sql-lc (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-lc))
          (is (re-find #"coalesce\(cb.full_name, cb.email\)" sql-lc))
          (is (re-find #"receipts\.purchased_at_guess >= \?" sql-lc))
          (is (re-find #"receipts\.purchased_at_guess <= \?" sql-lc))
          (is (re-find #"receipts\.created_at >= \?" sql-lc))
          (is (re-find #"receipts\.updated_at <= \?" sql-lc)))))))

(deftest get-receipt-refine-context-prefers-supplier-alias-and-hides-mismatched-store-context-test
  (testing "supplier alias wins over a conflicting store alias in refine context"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        nil)]
        (receipt-queries/get-receipt-refine-context :db (UUID/randomUUID))
        (let [sql-lc (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-lc))
          (is (re-find #"coalesce\(sup_from_alias\.id, sup_from_store\.id\)" sql-lc))
          (is (re-find #"coalesce\(sup_from_alias\.normalized_key, sup_from_store\.normalized_key\)" sql-lc))
          (is (re-find #"case when .*sup_from_alias\.id is null.*sup_from_store\.id = sup_from_alias\.id.* then st\.id else null end" sql-lc)))))))
