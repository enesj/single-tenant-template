(ns app.shared.pagination-test
  #?(:clj  (:require
            [app.shared.pagination :as pagination]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
             [app.shared.pagination :as pagination]
             [cljs.test :refer-macros [deftest is testing]])))

(deftest page-and-offset-conversion-test
  (testing "page->offset"
    (is (= 0 (pagination/page->offset 1 10)))
    (is (= 10 (pagination/page->offset 2 10)))
    (is (= 0 (pagination/page->offset 1 nil))))

  (testing "offset->page"
    (is (= 1 (pagination/offset->page 0 10)))
    (is (= 2 (pagination/offset->page 10 10)))
    (is (= 1 (pagination/offset->page -5 10)))
    (is (= 2 (pagination/offset->page 10 nil)))))

(deftest within-range-test
  (testing "within-range? guards user-provided page/per-page"
    (is (true? (pagination/within-range? 1 10 0)))
    (is (false? (pagination/within-range? 2 10 0)))
    (is (false? (pagination/within-range? 1 0 10)))))

(deftest paginate-test
  (testing "paginate (positional arity) returns stable shape"
    (is (= {:page 1
            :per-page 10
            :offset 0
            :limit 10
            :total 95
            :total-pages 10}
           (pagination/paginate 1 10 95))))

  (testing "paginate normalizes out-of-range page"
    (is (= 10 (:page (pagination/paginate 11 10 95))))
    (is (= 90 (:offset (pagination/paginate 11 10 95)))))

  (testing "paginate supports map arity"
    (is (= (pagination/paginate 1 10 95)
           (pagination/paginate {:page 1 :per-page 10 :total 95})))
    (is (= {:page 1
            :per-page 10
            :offset 0
            :limit 10
            :total 0
            :total-pages 1}
           (pagination/paginate {:page nil :per-page nil :total nil})))))
