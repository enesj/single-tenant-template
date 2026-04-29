(ns app.domain.frontend.expenses.events.user-expenses.pagination-lists.aliases-payers-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest payers-refresh-list-uses-fixed-params-unless-server-mode
  (testing "payers refresh ignores list UI state outside server mode"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :payers) 15)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :payers) 3)
    (swap! rf-db/app-db assoc-in (paths/list-filters :payers) {:label "  Main payer  "})
    (rf/dispatch-sync [::list-ui-state-events/set-sort-field :payers :label])
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-payers-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-payers event-id))
          (is (= {:limit 200 :offset 0 :include_inactive true}
                params)))
        (finally
          (rf/reg-fx :dispatch rf/dispatch)))))

  (testing "payers refresh derives pagination, filters, and sort in server mode"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :payers) 15)
    (swap! rf-db/app-db assoc-in (paths/list-filters :payers) {:label "  Main payer  "})
    (rf/dispatch-sync [::list-ui-state-events/set-pagination-mode :payers :server])
    (rf/dispatch-sync [::list-ui-state-events/set-sort-field :payers :label])
    (swap! rf-db/app-db assoc-in (paths/list-current-page :payers) 3)
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-payers-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-payers event-id))
          (is (= {:limit 15
                  :offset 30
                  :label "Main payer"
                  :sort "label:asc"
                  :include_inactive true}
                (select-keys params [:limit :offset :label :sort :include_inactive]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-payers-success-stores-user-payer-id-and-local-items
  (testing "payers fetch success stores local items, total fallback, and user payer id"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :payers :loading?] true)
    (rf/dispatch-sync
      [:user-expenses/fetch-payers-success
       {:data [{:id "payer-1"}
               {:id "payer-2"}]
        :user_payer_id 42}])
    (is (= [{:id "payer-1"}
            {:id "payer-2"}]
          (get-in @rf-db/app-db [:user-expenses :payers :items])))
    (is (= [{:id "payer-1"}
            {:id "payer-2"}]
          (get-in @rf-db/app-db [:user-expenses :payers :data])))
    (is (= 2 (get-in @rf-db/app-db (paths/list-total-items :payers))))
    (is (= "42" (get-in @rf-db/app-db [:user-expenses :payers :user-payer-id])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :payers :loading?])))
    (is (nil? (get-in @rf-db/app-db [:user-expenses :payers :error])))))

(deftest articles-refresh-list-flattens-canonical-name-and-created-at-filters
  (testing "articles refresh serializes canonical-name and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :articles) 40)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :articles) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :articles)
      {:canonical-name "  Jogurt  "
       :created-at {:from (js/Date. "2026-04-01T00:00:00.000Z")
                    :to (js/Date. "2026-04-02T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-articles-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-articles event-id))
          (is (= {:limit 40
                  :offset 40
                  :canonical-name "Jogurt"
                  :created-at-from "2026-04-01T00:00:00.000Z"
                  :created-at-to "2026-04-02T23:59:59.999Z"}
                (select-keys params [:limit :offset :canonical-name :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest article-aliases-refresh-list-flattens-raw-label-and-created-at-filters
  (testing "article aliases refresh serializes raw-label and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :article-aliases) 40)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :article-aliases) 3)
    (swap! rf-db/app-db assoc-in (paths/list-filters :article-aliases)
      {:raw-label "  jogurt  "
       :created-at {:from (js/Date. "2026-04-03T00:00:00.000Z")
                    :to (js/Date. "2026-04-04T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-article-aliases-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-article-aliases event-id))
          (is (= {:limit 40
                  :offset 80
                  :raw-label "jogurt"
                  :created-at-from "2026-04-03T00:00:00.000Z"
                  :created-at-to "2026-04-04T23:59:59.999Z"}
                (select-keys params [:limit :offset :raw-label :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest supplier-aliases-refresh-list-flattens-raw-label-and-created-at-filters
  (testing "supplier aliases refresh serializes raw-label and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :supplier-aliases) 40)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :supplier-aliases) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :supplier-aliases)
      {:raw-label "  Konzum Sarajevo  "
       :created-at {:from (js/Date. "2026-04-05T00:00:00.000Z")
                    :to (js/Date. "2026-04-06T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-supplier-aliases-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-supplier-aliases event-id))
          (is (= {:limit 40
                  :offset 40
                  :raw-label "Konzum Sarajevo"
                  :created-at-from "2026-04-05T00:00:00.000Z"
                  :created-at-to "2026-04-06T23:59:59.999Z"}
                (select-keys params [:limit :offset :raw-label :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest store-aliases-refresh-list-flattens-raw-label-and-created-at-filters
  (testing "store aliases refresh serializes raw-label and created-at filters for the API"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :store-aliases) 50)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :store-aliases) 2)
    (swap! rf-db/app-db assoc-in (paths/list-filters :store-aliases)
      {:raw-label "  Mega Market  "
       :created-at {:from (js/Date. "2026-04-07T00:00:00.000Z")
                    :to (js/Date. "2026-04-08T23:59:59.999Z")}})
    (let [dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! dispatches conj event)))
      (try
        (rf/dispatch-sync [:user-expenses/refresh-store-aliases-list])
        (let [[event-id params] (first @dispatches)]
          (is (= :user-expenses/fetch-store-aliases event-id))
          (is (= {:limit 50
                  :offset 50
                  :raw-label "Mega Market"
                  :created-at-from "2026-04-07T00:00:00.000Z"
                  :created-at-to "2026-04-08T23:59:59.999Z"}
                (select-keys params [:limit :offset :raw-label :created-at-from :created-at-to]))))
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))))

(deftest fetch-expense-categories-success-stores-local-items-and-date-highlights
  (testing "expense categories fetch success preserves local items cache and date highlights"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in [:user-expenses :expense-categories :loading?] true)
    (rf/dispatch-sync
      [:user-expenses/fetch-expense-categories-success
       {:data [{:id "ec-1"}
               {:id "ec-2"}]
        :date-highlights {:created-at ["2026-04-09"]}}])
    (is (= [{:id "ec-1"}
            {:id "ec-2"}]
          (get-in @rf-db/app-db [:user-expenses :expense-categories :items])))
    (is (= 2 (get-in @rf-db/app-db (paths/list-total-items :expense-categories))))
    (is (= {:created-at ["2026-04-09"]}
          (get-in @rf-db/app-db (conj (paths/list-ui-state :expense-categories) :date-highlights))))
    (is (false? (get-in @rf-db/app-db [:user-expenses :expense-categories :loading?])))
    (is (nil? (get-in @rf-db/app-db [:user-expenses :expense-categories :error])))))
