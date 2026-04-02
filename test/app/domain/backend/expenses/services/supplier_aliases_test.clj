(ns app.domain.backend.expenses.services.supplier-aliases-test
  (:require
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(deftest count-supplier-aliases-keeps-text-filters-test
  (testing "count query keeps text filters when supplier display name is provided"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:total 1})]
        (is (= 1 (supplier-aliases/count-supplier-aliases
                   :db
                   {:supplier-display-name "APOTEKE SARAJEVO"})))
        (let [sql-lc (some-> @captured-sql first str str/lower-case)
              params (map str (rest @captured-sql))]
          (is (string? sql-lc))
          (is (re-find #"count\(\*\)" sql-lc))
          (is (re-find #"s\.display_name" sql-lc))
          (is (re-find #"like" sql-lc))
          (is (some #(str/includes? % "APOTEKE SARAJEVO") params)))))))

(deftest count-supplier-aliases-combines-base-and-text-filters-test
  (testing "count query keeps custom supplier-id filters while still applying text filters"
    (let [captured-sql (atom nil)
          supplier-id (str (UUID/randomUUID))]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:total 1})]
        (is (= 1 (supplier-aliases/count-supplier-aliases
                   :db
                   {:supplier-id supplier-id
                    :supplier-display-name "APOTEKE SARAJEVO"})))
        (let [sql-lc (some-> @captured-sql first str str/lower-case)
              params (map str (rest @captured-sql))]
          (is (string? sql-lc))
          (is (re-find #"sa\.supplier_id" sql-lc))
          (is (re-find #"s\.display_name" sql-lc))
          (is (some #(str/includes? % supplier-id) params))
          (is (some #(str/includes? % "APOTEKE SARAJEVO") params)))))))