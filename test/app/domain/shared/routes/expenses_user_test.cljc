(ns app.domain.shared.routes.expenses-user-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [app.domain.shared.routes.contract :as route-contract]
    [app.domain.shared.routes.expenses-user :as expenses-user-routes]))

(deftest route-descriptors-contract-test
  (testing "route descriptors are non-empty and contract-valid"
    (is (seq expenses-user-routes/route-descriptors))
    (is (every? route-contract/valid-descriptor?
          expenses-user-routes/route-descriptors)))

  (testing "route descriptor paths are unique"
    (is (route-contract/unique-paths? expenses-user-routes/route-descriptors))
    (is (= [] (route-contract/duplicate-paths expenses-user-routes/route-descriptors))))

  (testing "derived path vectors are non-empty and aligned"
    (is (seq expenses-user-routes/all-paths))
    (is (seq expenses-user-routes/spa-fallback-paths))
    (is (= (set expenses-user-routes/spa-fallback-paths)
          (set expenses-user-routes/all-paths))))

  (testing "contract helpers support empty descriptor collections"
    (is (= [] (route-contract/descriptor-paths [])))
    (is (= [] (route-contract/spa-fallback-paths [])))
    (is (= [] (route-contract/duplicate-paths [])))
    (is (true? (route-contract/unique-paths? [])))))
