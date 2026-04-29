(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists.catalog-test
  (:require
    [app.domain.frontend.expenses.events.events-factory :as events-factory]
    [app.domain.frontend.expenses.events.unmapped-items :as unmapped-items-events]
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

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

(deftest unmapped-aliases-refresh-list-flattens-text-and-number-range-filters
  (testing "unmapped aliases refresh serializes raw-label and occurrence-count filters"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :unmapped-aliases) 25)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :unmapped-aliases) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :unmapped-aliases)
      {:raw-label "jogurt"
       :occurrence-count {:min 2 :max 9}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-unmapped-aliases-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-unmapped-aliases event-id))
          (is (= {:limit 25
                  :offset 25
                  :raw-label "jogurt"
                  :occurrence-count-min 2
                  :occurrence-count-max 9}
                (select-keys params [:limit :offset :raw-label :occurrence-count-min :occurrence-count-max]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-unmapped-aliases-success-stores-server-total-items
  (testing "unmapped aliases fetch success persists server totals with row-count fallback"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:user-expenses/fetch-unmapped-aliases-success
       {:data [{:id "ua-1"}
               {:id "ua-2"}]
        :total 64}])
    (is (= 64 (get-in @rf-db/app-db (paths/list-total-items :unmapped-aliases))))

    (rf/dispatch-sync
      [:user-expenses/fetch-unmapped-aliases-success
       {:data [{:id "ua-1"}
               {:id "ua-2"}
               {:id "ua-3"}]}])
    (is (= 3 (get-in @rf-db/app-db (paths/list-total-items :unmapped-aliases))))))

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

(deftest categories-refresh-list-flattens-name-and-created-at-filters
  (testing "categories refresh serializes name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :categories) 30)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :categories) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :categories)
      {:name "Household"
       :created-at {:from (js/Date. "2026-03-10T00:00:00.000Z")
                    :to (js/Date. "2026-03-11T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-categories-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-categories event-id))
          (is (= {:limit 30
                  :offset 30
                  :name "Household"
                  :created-at-from "2026-03-10T00:00:00.000Z"
                  :created-at-to "2026-03-11T23:59:59.999Z"}
                (select-keys params [:limit :offset :name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest subcategories-refresh-list-flattens-name-and-created-at-filters
  (testing "subcategories refresh serializes name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :subcategories) 30)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :subcategories) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :subcategories)
      {:name "Frozen"
       :created-at {:from (js/Date. "2026-03-08T00:00:00.000Z")
                    :to (js/Date. "2026-03-09T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-subcategories-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-subcategories event-id))
          (is (= {:limit 30
                  :offset 30
                  :name "Frozen"
                  :created-at-from "2026-03-08T00:00:00.000Z"
                  :created-at-to "2026-03-09T23:59:59.999Z"}
                (select-keys params [:limit :offset :name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

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

(deftest cities-refresh-list-flattens-name-and-created-at-filters
  (testing "cities refresh serializes name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :cities) 30)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :cities) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :cities)
      {:name "Sarajevo"
       :created-at {:from (js/Date. "2026-03-12T00:00:00.000Z")
                    :to (js/Date. "2026-03-13T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-cities-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-cities event-id))
          (is (= {:limit 30
                  :offset 30
                  :name "Sarajevo"
                  :created-at-from "2026-03-12T00:00:00.000Z"
                  :created-at-to "2026-03-13T23:59:59.999Z"}
                (select-keys params [:limit :offset :name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest manufacturers-refresh-list-flattens-display-name-and-created-at-filters
  (testing "manufacturers refresh serializes display-name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :manufacturers) 30)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :manufacturers) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :manufacturers)
      {:display-name "Argeta"
       :created-at {:from (js/Date. "2026-03-16T00:00:00.000Z")
                    :to (js/Date. "2026-03-17T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-manufacturers-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-manufacturers event-id))
          (is (= {:limit 30
                  :offset 30
                  :display-name "Argeta"
                  :created-at-from "2026-03-16T00:00:00.000Z"
                  :created-at-to "2026-03-17T23:59:59.999Z"}
                (select-keys params [:limit :offset :display-name :created-at-from :created-at-to]))))
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

(deftest expense-categories-refresh-list-flattens-created-at-range-filters
  (testing "expense categories refresh serializes created-at date-range filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :expense-categories) 12)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :expense-categories) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :expense-categories)
      {:name "Ivanica"
       :created-at {"from" (js/Date. "2026-03-14T00:00:00.000Z")
                    "to" (js/Date. "2026-03-15T00:00:00.000Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-expense-categories-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-expense-categories event-id))
          (is (= {:limit 12
                  :offset 12
                  :name "Ivanica"
                  :created-at-from "2026-03-14T00:00:00.000Z"
                  :created-at-to "2026-03-15T00:00:00.000Z"}
                (select-keys params [:limit :offset :name :created-at-from :created-at-to]))))
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

(deftest expense-category-create-normalizes-mixed-checkbox-keys
  (testing "expense category creates send a single snake_case checkbox param to the API"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/create-expense-category-modal
                       {:name "Travel"
                        :exclude_from_reports false
                        :exclude-from-reports true
                        :is_default false
                        :is-default true}
                       nil])
    (let [req (sup/last-http-request)]
      (is (= :post (sup/req-method req)))
      (is (= "/api/v1/expenses/expense-categories" (sup/req-uri req)))
      (is (= {:name "Travel"
              :exclude_from_reports true
              :is_default true}
            (sup/req-params req))))))

(deftest expense-category-update-normalizes-mixed-checkbox-keys
  (testing "expense category updates send a single snake_case checkbox param to the API"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/update-expense-category-modal
                       "cat-1"
                       {:name "Travel"
                        :exclude_from_reports false
                        :exclude-from-reports true
                        :is_default false
                        :is-default true}
                       nil])
    (let [req (sup/last-http-request)]
      (is (= :put (sup/req-method req)))
      (is (= "/api/v1/expenses/expense-categories/cat-1" (sup/req-uri req)))
      (is (= {:name "Travel"
              :exclude_from_reports true
              :is_default true}
            (sup/req-params req))))))

