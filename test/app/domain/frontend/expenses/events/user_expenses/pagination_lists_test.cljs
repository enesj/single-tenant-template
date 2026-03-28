(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists-test
  (:require
    [app.domain.frontend.expenses.events.events-factory :as events-factory]
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

(deftest expense-items-refresh-list-expands-range-and-date-filters
  (testing "expense items refresh expands number/date filters into backend query params"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expense-items) 20)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expense-items) 3)
    (swap! rf-db/app-db assoc-in (paths/list-filters :expense-items)
      {:qty {:min 0.35 :max 0.35}
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
                  :expense-purchased-at-from "2026-03-01T00:00:00.000Z"
                  :expense-purchased-at-to "2026-03-31T23:59:59.999Z"
                  :raw-label "jagoda"}
                (select-keys params [:limit
                                     :offset
                                     :qty-min
                                     :qty-max
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
          (is (= "status" (:order-by params)))
          (is (= "asc" (:order-dir params)))
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

(deftest resolve-pagination-prefers-persisted-per-page-when-list-state-is-empty
  (testing "stored entity display prefs seed initial list pagination before the first load-list request"
    (let [db {:ui {:entity-prefs {:categories {:display {:per-page 50}}}
                   :lists {:categories {:current-page nil
                                        :per-page nil
                                        :pagination {:current-page nil
                                                     :per-page nil}}}}}
          pagination (events-factory/resolve-pagination
                       :categories
                       db
                       {}
                       {:default-per-page 25})]
      (is (= {:limit 50
              :offset 0
              :page 1
              :per-page 50}
            pagination))))

  (testing "explicit request params still override the persisted browser preference"
    (let [db {:ui {:entity-prefs {:categories {:display {:per-page 50}}}}}
          pagination (events-factory/resolve-pagination
                       :categories
                       db
                       {:per-page 20 :page 2}
                       {:default-per-page 25})]
      (is (= {:limit 20
              :offset 20
              :page 2
              :per-page 20}
            pagination)))))

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
  (testing "expense categories refresh derives limit/offset from template list state"
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
