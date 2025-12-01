(ns app.template.frontend.api.batch-update-test
  (:require
    [app.template.frontend.api.http :as http]
    [cljs.test :refer [deftest is testing]]))

(deftest batch-update-request-test
  (testing "batch-update-entities builds request with merged ids"
    (let [cfg (http/batch-update-entities {:entity-name "transaction-types"
                                           :item-ids [1 2]
                                           :values {:name "Updated"}
                                           :on-success [:ok]
                                           :on-failure [:err]})]
      (is (= :post (:method cfg)))
      (is (= "/api/v1/entities/transaction-types/batch" (:uri cfg)))
      (is (= [{:id 1 :name "Updated"}
              {:id 2 :name "Updated"}]
            (get-in cfg [:params :transaction_types]))
        "Entity key should be underscored and contain merged entries")
      (is (= [:ok] (:on-success cfg)))
      (is (= [:err] (:on-failure cfg))))))
