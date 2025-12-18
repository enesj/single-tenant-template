(ns app.template.frontend.events.user-expenses-test
  (:require
    ;; Ensure events are registered
    [app.template.frontend.events.user-expenses]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce ^:private _fx-stubs-installed
  (do
    ;; Keep tests deterministic: no real HTTP, no timers.
    (rf/reg-fx :http-xhrio (fn [_] nil))
    (rf/reg-fx :dispatch-later (fn [_] nil))
    true))

(defn- reset-db! []
  (reset! rf-db/app-db helpers/valid-test-db-state))

(deftest modal-create-tracks-recently-created
  (testing "create-expense-modal-success tracks :expenses in :ui :recently-created"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/create-expense-modal-success
                       nil
                       {:expense {:id "exp-1"}}])
    (is (= #{"exp-1"}
          (get-in @rf-db/app-db [:ui :recently-created :expenses])))))

(deftest modal-update-tracks-recently-updated
  (testing "update-expense-modal-success tracks :expenses in :ui :recently-updated"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/update-expense-modal-success
                       "exp-2"
                       nil
                       {:expense {:id "exp-2"}}])
    (is (= #{"exp-2"}
          (get-in @rf-db/app-db [:ui :recently-updated :expenses])))))
