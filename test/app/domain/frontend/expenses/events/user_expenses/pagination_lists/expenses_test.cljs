(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists.expenses-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest fetch-recent-success-normalizes-recent-items-for-rows-override
  (testing "fetch-recent-success stores kebab-case aliases needed by list rows-override"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-recent-success
       {:data [{:id "exp-1"
                :purchased_at "2026-02-16T10:30:00Z"
                :supplier_display_name "Konzum"
                :expense_category_name "Groceries"}]
        :total 1
        :limit 25
        :offset 0}])
    (let [item (first (get-in @rf-db/app-db [:user-expenses :recent :items]))]
      (is (= "2026-02-16T10:30:00Z" (:purchased-at item)))
      (is (= "Konzum" (:supplier-display-name item)))
      (is (= "Groceries" (:expense-category-name item))))))

(deftest expenses-refresh-list-forwards-highlight-request-params
  (testing "expenses refresh wrapper merges server highlight request params into the fetch event"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expenses) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expenses) 2)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-expenses-list
                           {:highlight-date-field "purchased-at"
                            :highlight-timezone "Europe/Sarajevo"}])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-expenses event-id))
          (is (= {:limit 25
                  :offset 25
                  :highlight-date-field "purchased-at"
                  :highlight-timezone "Europe/Sarajevo"}
                (select-keys params [:limit :offset :highlight-date-field :highlight-timezone]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest expenses-refresh-list-flattens-select-text-and-range-filters
  (testing "expenses refresh unwraps select filters and expands date/number ranges"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expenses) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expenses) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :expenses)
      {:currency [{:value "BAM" :label "BAM"}]
       :supplier-display-name "  Konzum  "
       :created-at {:from (js/Date. "2026-03-14T00:00:00.000Z")
                    :to (js/Date. "2026-03-15T23:59:59.999Z")}
       :total-amount {:min 10 :max 25}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-expenses-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-expenses event-id))
          (is (= {:limit 25
                  :offset 25
                  :currency "BAM"
                  :supplier-display-name "Konzum"
                  :created-at-from "2026-03-14T00:00:00.000Z"
                  :created-at-to "2026-03-15T23:59:59.999Z"
                  :total-amount-min 10
                  :total-amount-max 25}
                (select-keys params [:limit
                                     :offset
                                     :currency
                                     :supplier-display-name
                                     :created-at-from
                                     :created-at-to
                                     :total-amount-min
                                     :total-amount-max]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-expenses-success-stores-server-date-highlights
  (testing "expenses fetch success persists returned date highlight metadata"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-expenses-success
       {:data [{:id "exp-1"}]
        :total 1
        :date-highlights {:purchased-at ["2026-03-10" "2026-03-11"]}}])
    (is (= {:purchased-at ["2026-03-10" "2026-03-11"]}
          (get-in @rf-db/app-db (conj (paths/list-ui-state :expenses) :date-highlights))))))

(deftest stores-refresh-list-forwards-highlight-request-params
  (testing "stores refresh wrapper merges server highlight request params into the fetch event"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :stores) 40)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :stores) 3)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-stores-list
                           {:highlight-date-field "created-at"
                            :highlight-timezone "UTC"}])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-stores event-id))
          (is (= {:limit 40
                  :offset 80
                  :highlight-date-field "created-at"
                  :highlight-timezone "UTC"}
                (select-keys params [:limit :offset :highlight-date-field :highlight-timezone]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest stores-refresh-list-flattens-name-and-created-at-filters
  (testing "stores refresh serializes name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :stores) 40)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :stores) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :stores)
      {:display-name "Mega Market"
       :created-at {:from (js/Date. "2026-03-05T00:00:00.000Z")
                    :to (js/Date. "2026-03-06T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-stores-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-stores event-id))
          (is (= {:limit 40
                  :offset 40
                  :display-name "Mega Market"
                  :created-at-from "2026-03-05T00:00:00.000Z"
                  :created-at-to "2026-03-06T23:59:59.999Z"}
                (select-keys params [:limit :offset :display-name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-stores-success-stores-server-date-highlights
  (testing "stores fetch success persists returned date highlight metadata"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-stores-success
       {:data [{:id "store-1"}]
        :total 1
        :date-highlights {:created-at ["2026-03-07"]}}])
    (is (= {:created-at ["2026-03-07"]}
          (get-in @rf-db/app-db (conj (paths/list-ui-state :stores) :date-highlights))))))

(deftest fetch-receipts-success-stores-server-date-highlights
  (testing "receipts fetch success persists returned date highlight metadata"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-success
       {:data [{:id "rec-1"}]
        :total 1
        :limit 10
        :offset 0
        :date-highlights {:uploaded-at ["2026-03-08"]}}])
    (is (= {:uploaded-at ["2026-03-08"]}
          (get-in @rf-db/app-db (conj (paths/list-ui-state :receipts) :date-highlights))))))

