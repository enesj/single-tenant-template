(ns app.domain.backend.expenses.services.article-aliases-test
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.time Instant]))

(deftest list-article-aliases-keeps-extra-filters-test
  (testing "list query keeps shared extra-filters such as date ranges"
    (let [captured-sql (atom nil)
          from-ts (Instant/parse "2026-04-02T00:00:00Z")]
      (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                    (reset! captured-sql sql-params)
                                    [])]
        (article-aliases/list-article-aliases
          :db
          {:extra-filters [[:>= :aa/created_at from-ts]]})
        (let [sql-lc (some-> @captured-sql first str str/lower-case)
              params (map str (rest @captured-sql))]
          (is (string? sql-lc))
          (is (re-find #"aa\.created_at >= \?" sql-lc))
          (is (some #(str/includes? % "2026-04-02T00:00:00Z") params)))))))

(deftest count-article-aliases-keeps-extra-filters-test
  (testing "count query keeps shared extra-filters such as date ranges"
    (let [captured-sql (atom nil)
          from-ts (Instant/parse "2026-04-02T00:00:00Z")]
      (with-redefs [jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:total 0})]
        (is (= 0 (article-aliases/count-article-aliases
                   :db
                   {:extra-filters [[:>= :aa/created_at from-ts]]})))
        (let [sql-lc (some-> @captured-sql first str str/lower-case)
              params (map str (rest @captured-sql))]
          (is (string? sql-lc))
          (is (re-find #"count\(\*\)" sql-lc))
          (is (re-find #"aa\.created_at >= \?" sql-lc))
          (is (some #(str/includes? % "2026-04-02T00:00:00Z") params)))))))