(ns app.domain.frontend.expenses.events.expenses-test
  (:require
    ;; Ensure events are registered
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.expense-items :as expense-items-events]
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

    (deftest admin-expense-items-expansion-fetches-detail-items
      (testing "admin expense expansion uses the expense detail endpoint and caches nested items"
        (reset-db!)
        (swap! rf-db/app-db assoc :admin/token "test-admin-token")
        (let [captured-request (atom nil)]
      (rf/reg-fx :http-xhrio #(reset! captured-request %))

      (rf/dispatch-sync [::expense-items-events/fetch-admin-items-for-expense "exp-5"])
      (is (true? (get-in @rf-db/app-db [:expense-items :by-expense "exp-5" :loading?])))
      (is (= :get (:method @captured-request)))
      (is (= "/admin/api/expenses/entries/exp-5" (:uri @captured-request)))
      (is (= [::expense-items-events/fetch-admin-items-for-expense-success "exp-5"]
        (:on-success @captured-request)))

      (rf/dispatch-sync [::expense-items-events/fetch-admin-items-for-expense-success
             "exp-5"
             {:expense {:items [{:id "item-1" :raw-label "Milk"}]}}])
      (is (= [{:id "item-1" :raw-label "Milk"}]
        (get-in @rf-db/app-db [:expense-items :by-expense "exp-5" :items])))
      (is (false? (get-in @rf-db/app-db [:expense-items :by-expense "exp-5" :loading?])))

      (reset! captured-request :not-called)
      (rf/dispatch-sync [::expense-items-events/fetch-admin-items-for-expense "exp-5"])
      (is (= :not-called @captured-request)))))
