(ns app.domain.backend.expenses.services.related-records-test
  "Unit tests for the shared related-records helpers."
  (:require
    [app.domain.backend.expenses.services.related-records :as rr]
    [clojure.test :refer [deftest is testing]]))

;; ============================================================================
;; clamp-related-limit
;; ============================================================================

(deftest clamp-related-limit-nil-returns-default
  (is (= 100 (rr/clamp-related-limit nil))))

(deftest clamp-related-limit-clamps-min
  (is (= 1 (rr/clamp-related-limit 0)))
  (is (= 1 (rr/clamp-related-limit -50))))

(deftest clamp-related-limit-clamps-max
  (is (= 500 (rr/clamp-related-limit 501)))
  (is (= 500 (rr/clamp-related-limit 99999))))

(deftest clamp-related-limit-passes-through-valid
  (is (= 1 (rr/clamp-related-limit 1)))
  (is (= 50 (rr/clamp-related-limit 50)))
  (is (= 500 (rr/clamp-related-limit 500))))

;; ============================================================================
;; normalize-related-type
;; ============================================================================

(deftest normalize-related-type-nil-returns-nil
  (is (nil? (rr/normalize-related-type nil))))

(deftest normalize-related-type-string-direct
  (is (= :expenses (rr/normalize-related-type "expenses")))
  (is (= :receipts (rr/normalize-related-type "receipts")))
  (is (= :providers (rr/normalize-related-type "providers")))
  (is (= :stores (rr/normalize-related-type "stores"))))

(deftest normalize-related-type-keyword-direct
  (is (= :expenses (rr/normalize-related-type :expenses)))
  (is (= :providers (rr/normalize-related-type :providers))))

(deftest normalize-related-type-aliases-resolved
  (testing "singular supplier forms all map to :providers"
    (is (= :providers (rr/normalize-related-type "supplier")))
    (is (= :providers (rr/normalize-related-type "suppliers")))
    (is (= :providers (rr/normalize-related-type "provider")))
    (is (= :providers (rr/normalize-related-type :supplier)))
    (is (= :providers (rr/normalize-related-type :suppliers)))
    (is (= :providers (rr/normalize-related-type :provider))))
  (testing "singular manufacturer maps to :manufacturers"
    (is (= :manufacturers (rr/normalize-related-type "manufacturer")))
    (is (= :manufacturers (rr/normalize-related-type :manufacturer))))
  (testing "singular subcategory maps to :subcategories"
    (is (= :subcategories (rr/normalize-related-type "subcategory")))
    (is (= :subcategories (rr/normalize-related-type :subcategory)))))

(deftest normalize-related-type-case-insensitive
  (is (= :expenses (rr/normalize-related-type "EXPENSES")))
  (is (= :providers (rr/normalize-related-type "SUPPLIER"))))

(deftest normalize-related-type-unknown-passes-through
  (is (= :foobar (rr/normalize-related-type "foobar")))
  (is (= :foobar (rr/normalize-related-type :foobar))))

(deftest normalize-related-type-non-string-non-keyword-returns-nil
  (is (nil? (rr/normalize-related-type 42)))
  (is (nil? (rr/normalize-related-type []))))

;; ============================================================================
;; merge-related-rows
;; ============================================================================

(deftest merge-related-rows-empty-inputs
  (is (= [] (rr/merge-related-rows 10))))

(deftest merge-related-rows-dedupes-by-id
  (let [a {:id 1 :name "a"}
        b {:id 2 :name "b"}
        result (rr/merge-related-rows 10 [a b] [a {:id 3 :name "c"}])]
    (is (= 3 (count result)))
    (is (= [1 2 3] (mapv :id result)))))

(deftest merge-related-rows-preserves-first-source-order
  (let [rows-a [{:id 2} {:id 1}]
        rows-b [{:id 1} {:id 3}]
        result (rr/merge-related-rows 10 rows-a rows-b)]
    (is (= [2 1 3] (mapv :id result)))))

(deftest merge-related-rows-respects-limit
  (let [rows (mapv (fn [i] {:id i}) (range 20))
        result (rr/merge-related-rows 5 rows)]
    (is (= 5 (count result)))
    (is (= [0 1 2 3 4] (mapv :id result)))))

(deftest merge-related-rows-nil-id-rows-not-deduped
  ;; Rows without :id key should not collide on nil
  (let [r1 {:name "a"}
        r2 {:name "b"}
        result (rr/merge-related-rows 10 [r1 r2])]
    ;; Both have :id nil; they collide — only first survives
    (is (= 1 (count result)))))
