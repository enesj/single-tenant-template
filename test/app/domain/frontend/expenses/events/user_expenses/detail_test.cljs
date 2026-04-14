(ns app.domain.frontend.expenses.events.user-expenses.detail-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.detail :as detail]
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest preserve-existing-item-count-keeps-list-summary-signal
  (testing "detail payloads inherit item_count from the existing list entity when omitted"
    (let [db {:entities {:expenses {:data {"exp-1" {:id "exp-1"
                                                    :item-count 3}}}}}
          payload {:id "exp-1"
                   :supplier_display_name "After"}
          preserved (detail/preserve-existing-item-count db payload)]
      (is (= 3 (:item_count preserved)))
      (is (= "After" (:supplier_display_name preserved))))))

(deftest fetch-expense-success-preserves-item-count-in-current-expense
  (testing "fetch-expense-success keeps item-count available for follow-up UI consumers"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (conj (paths/entity-data :expenses) "exp-1")
      {:id "exp-1"
       :item-count 3})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :expenses) ["exp-1"])

    (rf/dispatch-sync [:user-expenses/fetch-expense-success
                       {:data {:id "exp-1"
                               :supplier_display_name "After"}}])

    (let [current-expense (get-in @rf-db/app-db [:user-expenses :current-expense :data])]
      (is (= 3 (:item-count current-expense)))
      (is (= "After" (:supplier-display-name current-expense))))))
