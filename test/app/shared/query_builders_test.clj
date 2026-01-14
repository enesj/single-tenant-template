(ns app.shared.query-builders-test
  (:require
    [app.shared.query-builders :as qb]
    [clojure.test :refer [deftest is testing]]))

(deftest normalize-limit-respects-default-and-max
  (testing "normalize-limit applies default, min and max"
    (is (= 10 (qb/normalize-limit nil {:default 10 :max 100})))
    (is (= 1 (qb/normalize-limit 0 {:default 10 :max 100})))
    (is (= 100 (qb/normalize-limit 1000 {:default 10 :max 100})))
    (is (= 5 (qb/normalize-limit 5 {:default 10 :max 100})))
    (is (= nil (qb/normalize-limit nil {:default nil :max 100})))) )

(deftest normalize-offset-never-negative
  (testing "normalize-offset clamps to >= 0"
    (is (= 0 (qb/normalize-offset nil)))
    (is (= 0 (qb/normalize-offset -10)))
    (is (= 5 (qb/normalize-offset 5)))))

(deftest normalize-order-direction-handles-strings-and-default
  (testing "normalize-order-direction accepts keywords/strings"
    (is (= :asc (qb/normalize-order-direction :asc)))
    (is (= :asc (qb/normalize-order-direction "asc")))
    (is (= :desc (qb/normalize-order-direction :desc)))
    (is (= :desc (qb/normalize-order-direction "desc")))
    (is (= :asc (qb/normalize-order-direction :wat {:default :asc})))
    (is (= :desc (qb/normalize-order-direction :wat {:default :desc})))))

(deftest apply-search-where-merges-with-existing
  (testing "apply-search-where appends with [:and existing new]"
    (let [q1 {:select [:*] :from [:users]}
          q2 (qb/apply-search-where q1 [:u/email :u/name] "%john%")]
      (is (= [:or [:ilike :u/email "%john%"]
                  [:ilike :u/name "%john%"]]
             (:where q2))))

    (let [q1 {:select [:*] :from [:users] :where [:= :active true]}
          q2 (qb/apply-search-where q1 [:u/email] "%john%")]
      (is (= [:and [:= :active true]
               [:or [:ilike :u/email "%john%"]]]
             (:where q2))))))
