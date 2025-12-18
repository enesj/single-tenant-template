(ns app.domain.frontend.expenses.events.expenses-test
  (:require
    ;; Ensure events are registered
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce ^:private _fx-stubs-installed
  (do
    ;; Keep tests deterministic: no real HTTP, no timers.
    (rf/reg-fx :http-xhrio (fn [_] nil))
    (rf/reg-fx :dispatch-later (fn [_] nil))
    ;; Avoid warnings / failures due to admin-only wiring not present in tests.
    (rf/reg-event-fx :admin/refresh-entity (fn [_ _] {}))
    true))

(defn- reset-db! []
  (reset! rf-db/app-db helpers/valid-test-db-state))

(deftest admin-modal-update-tracks-recently-updated
  (testing "update-entry-modal-success tracks :expenses in :ui :recently-updated"
    (reset-db!)
    ;; event vector shape: [::update-entry-modal-success expense-id on-success response]
    (rf/dispatch-sync [::expenses-events/update-entry-modal-success
                       "exp-3"
                       nil
                       {:expense {:id "exp-3"}}])
    (is (= #{"exp-3"}
          (get-in @rf-db/app-db [:ui :recently-updated :expenses])))))

(deftest admin-modal-create-tracks-recently-created
  (testing "create-entry-modal-success tracks :expenses in :ui :recently-created"
    (reset-db!)
    (rf/dispatch-sync [::expenses-events/create-entry-modal-success
                       nil
                       {:expense {:id "exp-4"}}])
    (is (= #{"exp-4"}
          (get-in @rf-db/app-db [:ui :recently-created :expenses])))))
