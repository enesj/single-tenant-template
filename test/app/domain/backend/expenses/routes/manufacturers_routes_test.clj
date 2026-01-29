(ns app.domain.backend.expenses.routes.manufacturers-routes-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [app.domain.backend.expenses.routes.manufacturers :as manufacturers]))

(deftest manufacturers-routes-base-path
  (testing "manufacturers routes mount under /manufacturers"
    (let [route (manufacturers/routes nil)]
      (is (vector? route))
      (is (= "/manufacturers" (first route))))))
