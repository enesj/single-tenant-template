(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists-test
  (:require
    [app.domain.frontend.expenses.events.unmapped-items :as unmapped-items-events]
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest recent-go-to-page-fetches-next-server-page
  (testing "recent-go-to-page computes offset from page + limit"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2 :limit 25}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 25 :offset 25}
            (sup/req-params req))))
    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))

(deftest recent-go-to-page-applies-new-limit-and-reuses-it
  (testing "page-size changes fetch page 1 with offset 0, and later page changes reuse stored limit"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 1 :limit 50}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 50 :offset 0}
            (sup/req-params req))))
    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2}])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses" (sup/req-uri req)))
      (is (= {:limit 50 :offset 50}
            (sup/req-params req))))
    (is (= 2 (count @sup/captured-http-requests)))
    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))

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

(deftest receipts-refresh-list-forwards-status-filter
  (testing "refresh wrapper forwards template status filter to fetch params"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :receipts) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :receipts) 1)
    (swap! rf-db/app-db assoc-in (paths/list-filters :receipts)
      {:status [{:value "review_required" :label "Review Required"}]})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-receipts-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-receipts event-id))
          (is (= ["review_required"] (:status params)))
          (is (= {:limit 25 :offset 0}
                (select-keys params [:limit :offset]))))
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
  (testing "fetch success persists server :total for template server pagination"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-success
       {:data [{:id "rec-1"}
               {:id "rec-2"}]
        :total 87
        :limit 25
        :offset 50}])
    (is (= 87 (get-in @rf-db/app-db (paths/list-total-items :receipts))))
    (is (= 87 (get-in @rf-db/app-db [:user-expenses :receipts :total])))))

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

(deftest unmapped-refresh-list-derives-limit-offset-and-supplier-filter
  (testing "unmapped refresh wrapper derives limit/offset from list UI state and forwards supplier filter"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :unmapped-items) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :unmapped-items) 3)
    (swap! rf-db/app-db assoc-in [:admin :expenses :unmapped-items :filters :supplier-id] "sup-1")
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [::unmapped-items-events/refresh-unmapped-items-list])
        (let [[event-id params] (first @dispatches)]
          (is (= ::unmapped-items-events/load-unmapped-items event-id))
          (is (= {:limit 25 :offset 50 :supplier-id "sup-1"}
                (select-keys params [:limit :offset :supplier-id]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest unmapped-items-loaded-stores-server-total-items
  (testing "unmapped success stores server total with fallback to returned item count"
    (sup/reset-db!)
    (rf/dispatch-sync
      [::unmapped-items-events/unmapped-items-loaded
       {:data [{:id "um-1"}
               {:id "um-2"}]
        :total 47}])
    (is (= 47 (get-in @rf-db/app-db (paths/list-total-items :unmapped-items))))

    (rf/dispatch-sync
      [::unmapped-items-events/unmapped-items-loaded
       {:data [{:id "um-1"}
               {:id "um-2"}
               {:id "um-3"}]}])
    (is (= 3 (get-in @rf-db/app-db (paths/list-total-items :unmapped-items))))))

(deftest cities-refresh-list-uses-template-pagination-state
  (testing "cities refresh wrapper derives limit/offset from template list state"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :cities) 30)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :cities) 4)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-cities-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-cities event-id))
          (is (= {:limit 30 :offset 90}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-cities-success-stores-server-total-items
  (testing "cities fetch success persists total for server pagination"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-cities-success
       {:data [{:id "city-1"}
               {:id "city-2"}]
        :total 91}])
    (is (= 91 (get-in @rf-db/app-db (paths/list-total-items :cities))))

    (rf/dispatch-sync
      [:user-expenses/fetch-cities-success
       {:data [{:id "city-1"}
               {:id "city-2"}
               {:id "city-3"}]}])
    (is (= 3 (get-in @rf-db/app-db (paths/list-total-items :cities))))))

(deftest expense-categories-refresh-list-uses-template-pagination-state
  (testing "expense categories refresh wrapper derives limit/offset from template list state"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expense-categories) 12)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expense-categories) 5)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-expense-categories-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-expense-categories event-id))
          (is (= {:limit 12 :offset 48}
                (select-keys params [:limit :offset]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-expense-categories-success-stores-server-total-items
  (testing "expense categories fetch success persists total for server pagination"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-expense-categories-success
       {:data [{:id "ec-1"}
               {:id "ec-2"}
               {:id "ec-3"}]
        :total 123}])
    (is (= 123 (get-in @rf-db/app-db (paths/list-total-items :expense-categories))))

    (rf/dispatch-sync
      [:user-expenses/fetch-expense-categories-success
       {:data [{:id "ec-1"}
               {:id "ec-2"}]}])
    (is (= 2 (get-in @rf-db/app-db (paths/list-total-items :expense-categories))))))

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
