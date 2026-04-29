(ns app.template.frontend.components.list-vector-mode-rows-override-test
  (:require
    [app.template.frontend.components.list :as list]
    [cljs.test :refer-macros [deftest is]]))

(deftest rows-override-server-mode-skips-local-transforms
  (let [apply-transforms @#'list/apply-rows-override-transforms
        rows [{:id 1 :status "posted"}
              {:id 2 :status "review_required"}
              {:id 3 :status "failed"}]
        entity-spec {:fields [{:id :status :label "Status" :type :text}]}
        result (apply-transforms {:rows rows
                                  :active-filters {}
                                  :sorts [{:field :status :direction :asc}]
                                  :server-pagination? true
                                  :entity-name :receipts
                                  :entity-spec entity-spec})]
    (is (= rows result)
      "Server-paginated rows-override lists should preserve backend ordering")))