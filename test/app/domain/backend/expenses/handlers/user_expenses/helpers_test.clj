(ns app.domain.backend.expenses.handlers.user-expenses.helpers-test
  "Unit tests for shared handler helpers — pure functions, no DB required."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.test :refer [deftest is]]))

;; ============================================================================
;; parse-page-limit
;; ============================================================================

(deftest parse-page-limit-nil-params-uses-default
  (is (= 50 (h/parse-page-limit nil 50)))
  (is (= 100 (h/parse-page-limit {} 100))))

(deftest parse-page-limit-reads-string-limit-param
  (is (= 25 (h/parse-page-limit {"limit" "25"} 50)))
  (is (= 25 (h/parse-page-limit {:limit "25"} 50))))

(deftest parse-page-limit-clamps-min-to-1
  (is (= 1 (h/parse-page-limit {:limit "0"} 50)))
  (is (= 1 (h/parse-page-limit {:limit "-10"} 50))))

(deftest parse-page-limit-clamps-max-to-500
  (is (= 500 (h/parse-page-limit {:limit "501"} 50)))
  (is (= 500 (h/parse-page-limit {:limit "99999"} 50))))

(deftest parse-page-limit-non-numeric-falls-back-to-default
  (is (= 50 (h/parse-page-limit {:limit "abc"} 50)))
  (is (= 50 (h/parse-page-limit {:limit ""} 50))))

(deftest parse-page-limit-default-also-clamped
  ;; Even the default is passed through the clamp
  (is (= 1 (h/parse-page-limit {} 0)))
  (is (= 500 (h/parse-page-limit {} 9999))))

;; ============================================================================
;; parse-page-offset
;; ============================================================================

(deftest parse-page-offset-nil-params-returns-zero
  (is (= 0 (h/parse-page-offset nil)))
  (is (= 0 (h/parse-page-offset {}))))

(deftest parse-page-offset-reads-string-offset-param
  (is (= 20 (h/parse-page-offset {"offset" "20"})))
  (is (= 20 (h/parse-page-offset {:offset "20"}))))

(deftest parse-page-offset-clamps-negative-to-zero
  (is (= 0 (h/parse-page-offset {:offset "-5"})))
  (is (= 0 (h/parse-page-offset {:offset "-1"}))))

(deftest parse-page-offset-non-numeric-returns-zero
  (is (= 0 (h/parse-page-offset {:offset "abc"})))
  (is (= 0 (h/parse-page-offset {:offset ""}))))

;; ============================================================================
;; parse-sort-params
;; ============================================================================

(deftest parse-sort-params-reads-canonical-sort-param
  (is (= {:sorts [{:field :created-at :direction :desc}
                  {:field :status :direction :asc}]
          :order-by :created-at
          :order-dir :desc}
        (h/parse-sort-params {:sort "created-at:desc,status:asc"}))))

(deftest parse-sort-params-ignores-legacy-order-by-order-dir
  (is (= {}
        (h/parse-sort-params {:order-by "created-at"
                              :order-dir "desc"}))))

(deftest parse-sort-params-drops-invalid-sort-entries
  (is (= {:sorts [{:field :status :direction :asc}]
          :order-by :status
          :order-dir :asc}
        (h/parse-sort-params {:sort "created-at:sideways,status:asc"}))))
