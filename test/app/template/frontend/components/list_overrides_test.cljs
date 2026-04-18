(ns app.template.frontend.components.list-overrides-test
  (:require
    [app.template.frontend.components.list.overrides :as overrides]
    [cljs.test :refer [deftest is testing]]))

(deftest sort-rows-by-sorts-orders-backlog-status-by-workflow-rank
  (testing "backlog status sorts follow workflow order instead of alphabetical order"
    (let [rows [{:id 4 :number 4 :status "Completed"}
                {:id 2 :number 2 :status "Waiting"}
                {:id 1 :number 1 :status "In progress"}
                {:id 3 :number 3 :status "Need improvements"}
                {:id 5 :number 5 :status "Waiting"}]
          sorted (overrides/sort-rows-by-sorts
                   rows
                   {:sorts [{:field :status :direction :asc}
                            {:field :number :direction :asc}]
                    :entity-name :backlog})]
      (is (= [1 2 5 3 4]
            (mapv :number sorted))))))
