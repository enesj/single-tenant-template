(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists.receipts-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest receipts-refresh-list-uses-template-pagination-state
  (testing "refresh wrapper converts current-page/per-page into offset/limit"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 20)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 3)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts) {})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-receipts-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-receipts event-id))
          (is (= {:limit 20 :offset 40}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest suppliers-server-mode-page-change-triggers-refresh-and-derived-fetch
  (testing "server mode page change dispatches refresh, and refresh derives limit/offset"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :suppliers) 15)
    (rf/dispatch-sync [::list-ui-state-events/set-pagination-mode :suppliers :server])
    (rf/dispatch-sync [::list-ui-state-events/set-refresh-event :suppliers [:user-expenses/refresh-suppliers-list]])

    (let [refresh-dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! refresh-dispatches conj event)))
      (try
        (rf/dispatch-sync [::list-ui-state-events/set-current-page :suppliers 3])
        (is (= [[:user-expenses/refresh-suppliers-list]] @refresh-dispatches)
          "Changing page in server mode should dispatch configured refresh event")
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))

    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-suppliers-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-suppliers event-id))
          (is (= {:limit 15 :offset 30}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest suppliers-refresh-list-flattens-display-name-and-created-at-filters
  (testing "suppliers refresh serializes display-name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :suppliers) 15)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :suppliers) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :suppliers)
      {:display-name "Konzum"
       :created-at {:from (js/Date. "2026-03-01T00:00:00.000Z")
                    :to (js/Date. "2026-03-02T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-suppliers-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-suppliers event-id))
          (is (= {:limit 15
                  :offset 15
                  :display-name "Konzum"
                  :created-at-from "2026-03-01T00:00:00.000Z"
                  :created-at-to "2026-03-02T23:59:59.999Z"}
                (select-keys params [:limit :offset :display-name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-refresh-list-forwards-status-filter
  (testing "refresh wrapper forwards template status filter and show-purged flag to fetch params"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 1)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
      {:status [{:value "review_required" :label "Review Required"}]})
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :show-purged?] true)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-receipts-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-receipts event-id))
          (is (= ["review_required"] (:status params)))
          (is (true? (:show-purged params)))
          (is (= {:limit 25 :offset 0}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-refresh-list-flattens-date-and-created-by-filters
  (testing "refresh wrapper serializes receipts date ranges and created-by filter"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
      {:created-by-name "Enes Jakić"
       :purchased-at-guess {"from" (js/Date. "2026-03-21T00:00:00.000Z")
                            "to" (js/Date. "2026-03-22T00:00:00.000Z")}
       :created-at {"from" (js/Date. "2026-03-30T00:00:00.000Z")
                    "to" (js/Date. "2026-03-31T00:00:00.000Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-receipts-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-receipts event-id))
          (is (= {:limit 25
                  :offset 25
                  :created-by-name "Enes Jakić"
                  :purchased-at-guess-from "2026-03-21T00:00:00.000Z"
                  :purchased-at-guess-to "2026-03-22T00:00:00.000Z"
                  :created-at-from "2026-03-30T00:00:00.000Z"
                  :created-at-to "2026-03-31T00:00:00.000Z"}
                (select-keys params [:limit :offset
                                     :created-by-name
                                     :purchased-at-guess-from :purchased-at-guess-to
                                     :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest expense-items-refresh-list-expands-range-and-date-filters
  (testing "expense items refresh expands number/date filters into backend query params"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expense-items) 20)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expense-items) 3)
    (swap! rf-db/app-db assoc-in (paths/list-filters :expense-items)
      {:qty {:min 0.35 :max 0.35}
       :unit-price {:min 8 :max 9}
       :unit "kg"
       :expense-purchased-at {:from "2026-03-01T00:00:00.000Z"
                              :to "2026-03-31T23:59:59.999Z"}
       :raw-label "jagoda"})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-expense-items-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-expense-items event-id))
          (is (= {:limit 20
                  :offset 40
                  :qty-min 0.35
                  :qty-max 0.35
                  :unit-price-min 8
                  :unit-price-max 9
                  :unit "kg"
                  :expense-purchased-at-from "2026-03-01T00:00:00.000Z"
                  :expense-purchased-at-to "2026-03-31T23:59:59.999Z"
                  :raw-label "jagoda"}
                (select-keys params [:limit
                                     :offset
                                     :qty-min
                                     :qty-max
                                     :unit-price-min
                                     :unit-price-max
                                     :unit
                                     :expense-purchased-at-from
                                     :expense-purchased-at-to
                                     :raw-label]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-refresh-list-forwards-sort-config
  (testing "refresh wrapper forwards the current template sort config to fetch params"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 1)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts) {})
    (rf/dispatch-sync [::list-ui-state-events/set-sort-field :receipts :status])
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-receipts-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-receipts event-id))
          (is (= "status:asc" (:sort params)))
          (is (= {:limit 25 :offset 0}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-toggle-show-purged-resets-to-first-page-and-refreshes
  (testing "toggling purged receipts resets pagination and schedules a refresh"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 4)
    (swap! rf-db/app-db assoc-in (paths/list-pagination-mode :receipts) :server)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/toggle-show-purged-receipts])
        (is (true? (get-in @rf-db/app-db [:user-expenses :receipts :show-purged?])))
        (is (= 1 (get-in @rf-db/app-db (paths/list-current-page :receipts))))
        (is (= [[:user-expenses/refresh-receipts-list]] @dispatches))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-processing-check-uses-template-pagination-state
  (testing "processing check derives unfiltered limit/offset from current receipts list UI state"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
      {:status [{:value "parsing" :label "Parsing"}]})
    (rf/dispatch-sync [:user-expenses/check-receipts-processing-complete])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts" (sup/req-uri req)))
      (is (= {:limit 25 :offset 25}
            (select-keys (sup/req-params req) [:limit :offset]))))))

(deftest receipts-processing-check-success-no-refresh-when-still-processing
  (testing "completion check updates rows and does not refresh list while any receipt is still processing"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] true)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync
          [:user-expenses/check-receipts-processing-complete-success
           {:data [{:id "rec-1"
                    :status "review_required"
                    :refine-pending true}]}])
        (is (= 1 (count @dispatches)))
        (is (not= :user-expenses/refresh-receipts-list (ffirst @dispatches)))
        (is (= "refining" (get-in @rf-db/app-db [:user-expenses :receipts :items 0 :status])))
        (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :loading?])))
        (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :refresh-pending?])))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-processing-check-success-refreshes-once-when-finished
  (testing "completion check triggers one refresh after processing finishes"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] true)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync
          [:user-expenses/check-receipts-processing-complete-success
           {:data [{:id "rec-1" :status "extracted"}]}])
        (is (= [[:user-expenses/refresh-receipts-list]] @dispatches))
        (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :loading?])))
        (is (true? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :refresh-pending?])))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest receipts-processing-check-noop-when-check-in-flight-or-refresh-pending
  (testing "processing check does not enqueue duplicate requests while guarded"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] true)
    (rf/dispatch-sync [:user-expenses/check-receipts-processing-complete])
    (is (= 0 (count @sup/captured-http-requests)))
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] false)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :refresh-pending?] true)
    (rf/dispatch-sync [:user-expenses/check-receipts-processing-complete])
    (is (= 0 (count @sup/captured-http-requests)))))

(deftest fetch-receipts-success-stores-server-total-items
  (testing "fetch success persists server totals and purged metadata for receipts"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-success
       {:data [{:id "rec-1"}
               {:id "rec-2"}]
        :total 87
        :purged-total 10
        :limit 25
        :offset 50}])
    (is (= 87 (get-in @rf-db/app-db (paths/list-total-items :receipts))))
    (is (= 87 (get-in @rf-db/app-db [:user-expenses :receipts :total])))
    (is (= 10 (get-in @rf-db/app-db [:user-expenses :receipts :purged-total])))))

(deftest fetch-receipts-clears-processing-check-guards
  (testing "fetch success and failure reset processing-check guard flags"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] true)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :refresh-pending?] true)
    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-success
       {:data [{:id "rec-1"}]
        :total 1
        :limit 10
        :offset 0}])
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :loading?])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :refresh-pending?])))

    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :loading?] true)
    (swap! rf-db/app-db assoc-in [:user-expenses :receipts :processing-check :refresh-pending?] true)
    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-failure
       {:response {:error "boom"}}])
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :loading?])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :processing-check :refresh-pending?])))))

