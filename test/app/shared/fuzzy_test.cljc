(ns app.shared.fuzzy-test
  #?(:clj  (:require
            [app.shared.fuzzy :as fuzzy]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
            [app.shared.fuzzy :as fuzzy]
            [cljs.test :refer-macros [deftest is testing]])))

(deftest levenshtein-distance-test
  (testing "exact and empty inputs"
    (is (= 0 (fuzzy/levenshtein-distance "" "")))
    (is (= 0 (fuzzy/levenshtein-distance "abc" "abc")))
    (is (= 3 (fuzzy/levenshtein-distance "" "abc")))
    (is (= 3 (fuzzy/levenshtein-distance "abc" ""))))

  (testing "standard edit-distance examples"
    (is (= 3 (fuzzy/levenshtein-distance "kitten" "sitting")))
    (is (= 2 (fuzzy/levenshtein-distance "flaw" "lawn"))))

  (testing "nil handling"
    (is (= 0 (fuzzy/levenshtein-distance nil nil)))
    (is (= 3 (fuzzy/levenshtein-distance nil "abc")))
    (is (= 3 (fuzzy/levenshtein-distance "abc" nil)))))

(deftest levenshtein-ratio-test
  (testing "ratio edge cases"
    (is (= 1.0 (fuzzy/levenshtein-ratio "" "")))
    (is (= 1.0 (fuzzy/levenshtein-ratio nil nil)))
    (is (= 1.0 (fuzzy/levenshtein-ratio "abc" "abc")))
    (is (= 0.0 (fuzzy/levenshtein-ratio "" "abc")))
    (is (= 0.0 (fuzzy/levenshtein-ratio "abc" ""))))

  (testing "ratio tracks distance / max-length"
    (is (= (/ 4.0 7.0)
          (fuzzy/levenshtein-ratio "kitten" "sitting")))
    (is (= 0.5
          (fuzzy/levenshtein-ratio "flaw" "lawn")))))