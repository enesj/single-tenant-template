(ns app.domain.frontend.expenses.events.user-expenses-test
  (:require
    ;; Ensure events are registered
    [app.domain.frontend.expenses.events.unmapped-items :as unmapped-items-events]
    [app.domain.frontend.expenses.events.user-expenses]
    ;; Ensure template list events are registered
    app.template.frontend.events.list.crud
    app.template.frontend.events.list.batch
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [cljs.test :refer [deftest is testing]]
    [goog.object :as gobj]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce captured-http-requests (atom []))

(defn- install-fx-stubs! []
  ;; Keep tests deterministic: no real HTTP, no timers.
  ;; Note: other test setup namespaces also stub :http-xhrio; re-register here so
  ;; these tests always capture requests into this namespace-local atom.
  (rf/reg-fx :http-xhrio (fn [req]
                           (swap! captured-http-requests conj req)
                           nil))
  (rf/reg-fx :dispatch-later (fn [_] nil)))

(defn- reset-db! []
  (install-fx-stubs!)
  (reset! captured-http-requests [])
  (reset! rf-db/app-db helpers/valid-test-db-state))

(defn- last-http-request []
  (last @captured-http-requests))

(defn- req-method [req]
  (let [m (or (:method req) (gobj/get req "method"))]
    (cond-> m (string? m) keyword)))

(defn- req-uri [req]
  (or (:uri req) (gobj/get req "uri")))

(defn- req-body [req]
  (or (:body req) (gobj/get req "body")))

(defn- req-params [req]
  (or (:params req) (gobj/get req "params")))

(defn- req-ids [req]
  (or (get-in (req-params req) [:ids])
    (let [body (req-body req)]
      (when (string? body)
        (try
          (-> (js/JSON.parse body)
            (js->clj :keywordize-keys true)
            :ids)
          (catch :default _ nil))))))

(defn- req-format-content-type [req]
  (let [fmt (or (:format req) (gobj/get req "format"))]
    (or (get fmt :content-type) (gobj/get fmt "content-type"))))

(defn- valid-receipt
  [id]
  {:id id
   :supplier-guess-supplier {:id "supplier-1"}
   :payer-id "payer-1"
   :raw-extract-json {:extraction {:purchased-at "2026-01-01T10:00:00Z"
                                   :totals {:total-amount 5.25
                                            :currency "BAM"}
                                   :items [{:raw-label "Bread"
                                            :line-total 5.25}]}}})

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

(deftest template-delete-expenses-is-bridged
  (testing "template delete-entity for :expenses uses /api/v1/expenses/batch (not generic /api/v1/entities)"
    (reset-db!)
    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :expenses) {"exp-1" {:id "exp-1"}
                                                                "exp-2" {:id "exp-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :expenses) ["exp-1" "exp-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :expenses) #{"exp-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :expenses) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :expenses "exp-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/batch" (req-uri req)))
      (is (= ["exp-1"] (req-ids req))))

    ;; Simulate the HTTP success event that the template CRUD delete would dispatch.
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :expenses "exp-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :expenses) "exp-1"))))
    (is (= ["exp-2"] (get-in @rf-db/app-db (paths/entity-ids :expenses))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :expenses))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :expenses))))))

(deftest template-delete-receipts-is-bridged
  (testing "template delete-entity for :receipts uses /api/v1/expenses/receipts/batch (not generic /api/v1/entities)"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-delete-receipts-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"receipts\" (string) is still bridged to /api/v1/expenses/receipts/batch"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "receipts" "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-batch-delete-stores-is-routed-to-expenses-endpoint
  (testing "template batch-delete for :stores uses /api/v1/expenses/stores/batch (not generic /api/v1/entities/stores/batch)"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/batch-delete :stores ["store-1" "store-2"]])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/stores/batch" (req-uri req)))
      (is (= ["store-1" "store-2"] (req-ids req))))))

(deftest template-batch-delete-stores-uses-admin-endpoint-in-admin-context
  (testing "in admin route context, stores batch-delete uses /admin/api/expenses/stores/batch"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/current-route-name) :admin-users)
    (swap! rf-db/app-db assoc :admin/token "test-token")

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/batch-delete :stores ["store-1"]])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/admin/api/expenses/stores/batch" (req-uri req)))
      (is (= ["store-1"] (req-ids req))))))

(deftest template-delete-receipts-not-bridged-in-admin-context
  (testing "in admin route context, receipts delete uses the admin receipts API (not the user receipts API)"
    (reset-db!)
    ;; Use the route name convention used by the admin router (e.g. :admin-users).
    (swap! rf-db/app-db assoc-in (paths/current-route-name) :admin-users)
    (swap! rf-db/app-db assoc :admin/token "test-token")

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/admin/api/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-delete-article-aliases-is-bridged
  (testing "template delete-entity for :article-aliases uses /api/v1/expenses/article-aliases/batch (not generic /api/v1/entities)"
    (reset-db!)

    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :article-aliases)
      {"aa-1" {:id "aa-1"}
       "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :article-aliases) ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :article-aliases) #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :article-aliases) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :article-aliases "aa-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (req-uri req)))
      (is (= ["aa-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :article-aliases "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :article-aliases) "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids :article-aliases))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :article-aliases))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :article-aliases))))))

(deftest template-delete-article-aliases-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"article-aliases\" (string) is still bridged to /api/v1/expenses/article-aliases/batch"
    (reset-db!)

    ;; Seed state under the string entity-type key to simulate callers that
    ;; use route params/UI widget values before coercion.
    (swap! rf-db/app-db assoc-in (paths/entity-data "article-aliases")
      {"aa-1" {:id "aa-1"}
       "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "article-aliases") ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "article-aliases") #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "article-aliases") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "article-aliases" "aa-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (req-uri req)))
      (is (= ["aa-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "article-aliases" "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "article-aliases") "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids "article-aliases"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "article-aliases"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "article-aliases"))))))

(deftest template-delete-articles-is-bridged
  (testing "template delete-entity for :articles uses /api/v1/expenses/articles/batch (not generic /api/v1/entities)"
    (reset-db!)

    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :articles)
      {"a-1" {:id "a-1"}
       "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :articles) ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :articles) #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :articles) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :articles "a-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (req-uri req)))
      (is (= ["a-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :articles "a-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :articles) "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids :articles))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :articles))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :articles))))))

(deftest template-delete-articles-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"articles\" (string) is still bridged to /api/v1/expenses/articles/batch"
    (reset-db!)

    ;; Seed state under the string entity-type key to simulate callers that
    ;; use route params/UI widget values before coercion.
    (swap! rf-db/app-db assoc-in (paths/entity-data "articles")
      {"a-1" {:id "a-1"}
       "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "articles") ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "articles") #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "articles") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "articles" "a-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (req-uri req)))
      (is (= ["a-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "articles" "a-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "articles") "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids "articles"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "articles"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "articles"))))))

(deftest template-batch-update-expenses-is-bridged
  (testing "template batch update for :expenses uses /api/v1/expenses/batch (not generic /api/v1/entities/expenses/batch)"
    (reset-db!)

    (rf/dispatch-sync
      [:app.template.frontend.events.list.batch/batch-update
       {:entity-name :expenses
        :item-ids ["exp-1"]
        :values {:notes "hello"}}])

    (let [req (last-http-request)
          items (get-in req [:params :items])
          item (first items)]
      (is (= :put (req-method req)))
      (is (= "/api/v1/expenses/batch" (req-uri req)))
      (is (= 1 (count items)))
      (is (= "exp-1" (:id item)))
      (is (= "hello" (:notes item)))
      (is (instance? js/Date (:updated-at item))))))

(deftest template-fetch-lookups-is-bridged
  (testing "template fetch for payers/suppliers/receipts uses /api/v1/expenses/* (not generic /api/v1/entities/*)"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :payers])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/payers?limit=500&offset=0" (req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :suppliers])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/suppliers?limit=500&offset=0" (req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :receipts])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/receipts?limit=500&offset=0" (req-uri req))))))

(deftest recent-go-to-page-fetches-next-server-page
  (testing "recent-go-to-page computes offset from page + limit"
    (reset-db!)

    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2 :limit 25}])

    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses" (req-uri req)))
      (is (= {:limit 25 :offset 25}
            (req-params req))))

    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 25 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))

(deftest recent-go-to-page-applies-new-limit-and-reuses-it
  (testing "page-size changes fetch page 1 with offset 0, and later page changes reuse stored limit"
    (reset-db!)

    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 1 :limit 50}])

    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses" (req-uri req)))
      (is (= {:limit 50 :offset 0}
            (req-params req))))

    (rf/dispatch-sync [:user-expenses/recent-go-to-page {:page 2}])

    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses" (req-uri req)))
      (is (= {:limit 50 :offset 50}
            (req-params req))))

    (is (= 2 (count @captured-http-requests)))
    (is (= 2 (get-in @rf-db/app-db [:user-expenses :recent :page])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :limit])))
    (is (= 50 (get-in @rf-db/app-db [:user-expenses :recent :offset])))))

(deftest receipts-refresh-list-uses-template-pagination-state
  (testing "refresh wrapper converts current-page/per-page into offset/limit"
    (reset-db!)
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
    (reset-db!)
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
    (reset-db!)
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

(deftest fetch-receipts-success-stores-server-total-items
  (testing "fetch success persists server :total for template server pagination"
    (reset-db!)

    (rf/dispatch-sync
      [:user-expenses/fetch-receipts-success
       {:data [{:id "rec-1"}
               {:id "rec-2"}]
        :total 87
        :limit 25
        :offset 50}])

    (is (= 87 (get-in @rf-db/app-db (paths/list-total-items :receipts))))
    (is (= 87 (get-in @rf-db/app-db [:user-expenses :receipts :total])))))

(deftest unmapped-refresh-list-derives-limit-offset-and-supplier-filter
  (testing "unmapped refresh wrapper derives limit/offset from list UI state and forwards supplier filter"
    (reset-db!)
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
    (reset-db!)

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
    (reset-db!)
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
    (reset-db!)

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
    (reset-db!)
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
    (reset-db!)

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
    (reset-db!)

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

(deftest ocr-selected-sends-batch-request
  (testing "ocr-selected posts normalized receipt ids to /api/v1/expenses/receipts/ocr"
    (reset-db!)

    (rf/dispatch-sync [:user-expenses/ocr-selected ["rec-1" nil " rec-2 " "rec-1" ""]])

    (let [req (last-http-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/expenses/receipts/ocr" (req-uri req)))
      (is (= {:receipt_ids ["rec-1" "rec-2"]}
            (req-params req))))))

(deftest ocr-selected-empty-selection-is-safe
  (testing "ocr-selected with nil or empty selection does not send a request"
    (doseq [selection [nil []]]
      (reset-db!)
      (rf/dispatch-sync [:user-expenses/ocr-selected selection])
      (is (= 0 (count @captured-http-requests)))
      (is (= "Select at least one receipt to parse."
            (get-in @rf-db/app-db [:user-expenses :receipts :error])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-starts-with-first-fetch
  (testing "post-selected starts by loading the first selected receipt"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1" "rec-2"]])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/receipts/rec-1" (req-uri req)))
      (is (true? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-empty-selection-is-safe
  (testing "post-selected with nil or empty selection does not send a request"
    (doseq [selection [nil []]]
      (reset-db!)
      (rf/dispatch-sync [:user-expenses/post-selected selection])
      (is (= 0 (count @captured-http-requests)))
      (is (= "Select at least one receipt to post."
            (get-in @rf-db/app-db [:user-expenses :receipts :error])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?]))))))

(deftest post-selected-approves-and-continues-sequentially
  (testing "post-selected posts one receipt then continues with the next one"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1" "rec-2"]])

    (rf/dispatch-sync [:user-expenses/post-selected-receipt-loaded
                       "rec-1"
                       ["rec-2"]
                       []
                       []
                       {:data (valid-receipt "rec-1")}])

    (let [approve-req (last-http-request)]
      (is (= :post (req-method approve-req)))
      (is (= "/api/v1/expenses/receipts/rec-1/approve" (req-uri approve-req))))

    (rf/dispatch-sync [:user-expenses/post-selected-approve-success
                       "rec-1"
                       ["rec-2"]
                       []
                       []
                       {:data {:expense {:id "exp-1"}
                               :receipt {:id "rec-1" :status "posted"}}}])

    (let [next-req (last-http-request)]
      (is (= :get (req-method next-req)))
      (is (= "/api/v1/expenses/receipts/rec-2" (req-uri next-req))))))

(deftest post-selected-validation-failure-is-reported
  (testing "invalid receipt data is reported and batch finishes safely"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/post-selected ["rec-1"]])
    (rf/dispatch-sync [:user-expenses/post-selected-receipt-loaded
                       "rec-1"
                       []
                       []
                       []
                       {:data {:id "rec-1"}}])
    (is (= 1 (count @captured-http-requests)))
    (is (= "Failed to post selected receipt. Supplier, payer, and date are required."
          (get-in @rf-db/app-db [:user-expenses :receipts :error])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :receipts :action-loading?])))))

(deftest upload-receipt-sends-file-in-formdata
  (testing "upload-receipt sends multipart file (guards against trim-v arg loss)"
    (reset-db!)
    (let [file (js/File. #js ["abc"] "r.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-1"])
      (rf/dispatch-sync [:user-expenses/upload-receipt file])
      (let [req (last-http-request)
            body (req-body req)
            uploaded (.get body "file")
            payer-id (.get body "payer_id")]
        (is (= :post (req-method req)))
        (is (string? (req-uri req)))
        (is (instance? js/FormData body))
        (is (instance? js/File uploaded))
        (is (= "r.jpg" (.-name uploaded)))
        (is (= "payer-1" payer-id))
        (is (not (true? (req-format-content-type req))))))))

(deftest upload-receipt-success-queues-ocr
  (testing "upload-receipt-success automatically triggers OCR"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success {:data {:id "rec-1"}}])
    (is (= 1 (count @captured-http-requests)))
    (let [req (last-http-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/expenses/receipts/rec-1/ocr" (req-uri req)))
      (is (nil? (req-params req))))))

(deftest upload-receipt-duplicate-shows-notice-and-skips-ocr
  (testing "duplicate uploads show a notice and do not queue OCR"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success
                       {:data {:id "rec-1" :original_filename "r.jpg"}
                        :duplicate? true}])
    (is (= 0 (count @captured-http-requests)))
    (let [notice (get-in @rf-db/app-db [:user-expenses :upload :notice])]
      (is (seq notice))
      (is (some #(re-find #"Already uploaded" (str %)) notice)))))

(deftest upload-receipts-batch-sends-multiple-requests
  (testing "upload-receipts queues files and uploads sequentially"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-xyz"])
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (is (= 1 (count @captured-http-requests)))
      (let [req1 (last-http-request)
            body1 (req-body req1)
            uploaded1 (.get body1 "file")
            payer1 (.get body1 "payer_id")]
        (is (= :post (req-method req1)))
        (is (instance? js/FormData body1))
        (is (instance? js/File uploaded1))
        (is (= "payer-xyz" payer1))
        (is (= "a.jpg" (.-name uploaded1))))

      ;; simulate success -> triggers next request
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [f2] {:data {:id "rec-1"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")
            payer2 (.get body2 "payer_id")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "payer-xyz" payer2))
        (is (= "b.jpg" (.-name uploaded2))))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      ;; Final success should queue OCR for each uploaded receipt.
      (is (= 4 (count @captured-http-requests)))
      (let [req3 (nth @captured-http-requests 2)
            req4 (nth @captured-http-requests 3)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-1/ocr" (req-uri req3)))

        (is (= :post (req-method req4)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (req-uri req4))))
      (is (false? (get-in @rf-db/app-db [:user-expenses :upload :loading?]))))))

(deftest upload-receipts-batch-continues-after-failure
  (testing "upload-receipts continues queue after a failure"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (rf/dispatch-sync [:user-expenses/upload-receipts-failure
                         [f2]
                         "a.jpg"
                         {:response {:error "Boom"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "b.jpg" (.-name uploaded2))))

      (is (= 1 (get-in @rf-db/app-db [:user-expenses :upload :batch :failed])))
      (let [msg (get-in @rf-db/app-db [:user-expenses :upload :error])]
        (is (string? msg))
        (is (.startsWith msg "a.jpg:")))

      ;; simulate success of the remaining file -> should queue OCR for receipts that did upload
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      (is (= 3 (count @captured-http-requests)))
      (let [req3 (last-http-request)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (req-uri req3)))
        (is (nil? (req-params req3)))))))

(deftest update-settings-sends-json-params
  (testing "update-settings sends JSON payload in :params (not :body)"
    (reset-db!)
    (let [settings {:default-currency "EUR"
                    :default-payer-id ""
                    :notifications-enabled true
                    :receipt-refine-enabled true}]
      (rf/dispatch-sync [:user-expenses/update-settings settings])
      (let [req (last-http-request)]
        (is (= :put (req-method req)))
        (is (= "/api/v1/expenses/settings" (req-uri req)))
        (is (= settings (req-params req)))
        (is (nil? (req-body req)))))))

(deftest reports-init-fetches-expanded-report-endpoints
  (testing "expanded report fetch events hit /api/v1/expenses/reports/* endpoints"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (reset! captured-http-requests [])

    (rf/dispatch-sync [:user-expenses/fetch-report-day-of-week])
    (rf/dispatch-sync [:user-expenses/fetch-report-top-items])
    (rf/dispatch-sync [:user-expenses/fetch-report-monthly-comparison])
    (rf/dispatch-sync [:user-expenses/fetch-report-size-distribution])
    (rf/dispatch-sync [:user-expenses/fetch-report-daily-heatmap])
    (rf/dispatch-sync [:user-expenses/fetch-report-category-allocation])

    (let [uris (set (map req-uri @captured-http-requests))]
      (is (contains? uris "/api/v1/expenses/reports/day-of-week"))
      (is (contains? uris "/api/v1/expenses/reports/top-items"))
      (is (contains? uris "/api/v1/expenses/reports/monthly-comparison"))
      (is (contains? uris "/api/v1/expenses/reports/size-distribution"))
      (is (contains? uris "/api/v1/expenses/reports/daily-heatmap"))
      (is (contains? uris "/api/v1/expenses/reports/category-allocation")))))

(deftest reports-set-filter-refreshes-and-passes-supplier-param
  (testing "report requests forward supplier and item filter params"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    ;; no supplier selected -> deep-dive does not issue HTTP request
    (reset! captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-deep-dive])
    (is (= 0 (count @captured-http-requests)))

    ;; set all filters that should be propagated via common-report-params
    (rf/dispatch-sync [:user-expenses/reports-set-filter :supplier-id "supplier-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :category-id "category-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :subcategory-id "subcategory-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :expense-category-id "expense-category-42"])
    (rf/dispatch-sync [:user-expenses/reports-set-filter :manufacturer-id "manufacturer-42"])

    (reset! captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-deep-dive])
    (rf/dispatch-sync [:user-expenses/fetch-report-top-items])
    (rf/dispatch-sync [:user-expenses/fetch-report-category-allocation])
    (rf/dispatch-sync [:user-expenses/fetch-report-monthly-comparison])

    (let [requests @captured-http-requests
          deep-dive-req (first (filter #(= "/api/v1/expenses/reports/supplier-deep-dive" (req-uri %)) requests))
          top-items-req (first (filter #(= "/api/v1/expenses/reports/top-items" (req-uri %)) requests))
          category-allocation-req (first (filter #(= "/api/v1/expenses/reports/category-allocation" (req-uri %)) requests))
          monthly-req (first (filter #(= "/api/v1/expenses/reports/monthly-comparison" (req-uri %)) requests))
          deep-dive-params (req-params deep-dive-req)
          top-items-params (req-params top-items-req)
          category-allocation-params (req-params category-allocation-req)
          monthly-params (req-params monthly-req)]
      (is (some? deep-dive-req))
      (is (= "supplier-42" (:supplier_id deep-dive-params)))
      (is (= "category-42" (:category_id deep-dive-params)))
      (is (= "subcategory-42" (:subcategory_id deep-dive-params)))
      (is (= "expense-category-42" (:expense_category_id deep-dive-params)))
      (is (= "manufacturer-42" (:manufacturer_id deep-dive-params)))
      (is (string? (:from deep-dive-params)))
      (is (string? (:to deep-dive-params)))

      (is (some? top-items-req))
      (is (= "supplier-42" (:supplier_id top-items-params)))
      (is (= "category-42" (:category_id top-items-params)))
      (is (= "subcategory-42" (:subcategory_id top-items-params)))
      (is (= "expense-category-42" (:expense_category_id top-items-params)))
      (is (= "manufacturer-42" (:manufacturer_id top-items-params)))

      (is (some? category-allocation-req))
      (is (= "category-42" (:category_id category-allocation-params)))
      (is (= "subcategory-42" (:subcategory_id category-allocation-params)))
      (is (= "manufacturer-42" (:manufacturer_id category-allocation-params)))

      (is (some? monthly-req))
      (is (= "supplier-42" (:supplier_id monthly-params)))
      (is (= "expense-category-42" (:expense_category_id monthly-params)))
      (is (string? (:month_a monthly-params)))
      (is (string? (:month_b monthly-params))))))

(deftest reports-toggle-expanded-supplier-fetches-and-clears-stores
  (testing "expanding a supplier fetches /reports/supplier-stores, toggling same supplier clears drilldown state"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    (reset! captured-http-requests [])
    (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-99"])
    ;; Deterministic fetch assertion: dispatch the fetch event directly.
    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-stores])

    (let [req (last-http-request)
          params (req-params req)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/reports/supplier-stores" (req-uri req)))
      (is (= "supplier-99" (:supplier_id params)))
      (is (= 20 (:limit params)))
      (is (string? (:from params)))
      (is (string? (:to params))))

    (is (= "supplier-99"
          (get-in @rf-db/app-db [:user-expenses :reports :filters :expanded-supplier-id])))

    ;; Toggling the same supplier collapses and clears the report slice without another HTTP request.
    (swap! rf-db/app-db assoc-in [:user-expenses :reports :supplier-stores :data]
      [{:store_name "Store A"}])
    (let [request-count (count @captured-http-requests)]
      (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-99"])
      (is (= request-count (count @captured-http-requests)))
      (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :filters :expanded-supplier-id])))
      (is (= [] (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :data])))
      (is (false? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :loading?])))
      (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :error]))))))

(deftest fetch-report-supplier-stores-without-expanded-supplier-is-safe
  (testing "explicit supplier-stores fetch does not fire HTTP when no expanded supplier is selected"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])
    (reset! captured-http-requests [])

    (rf/dispatch-sync [:user-expenses/fetch-report-supplier-stores])

    (is (= 0 (count @captured-http-requests)))
    (is (= [] (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :data])))
    (is (false? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :loading?])))
    (is (nil? (get-in @rf-db/app-db [:user-expenses :reports :supplier-stores :error])))))

(deftest reports-refresh-requests-supplier-stores-only-when-expanded-supplier-exists
  (testing "reports-refresh includes supplier-stores fetch only for expanded supplier state"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/init-reports])

    (let [captured-dispatch-n (atom nil)]
      (rf/reg-fx :dispatch-n (fn [events]
                               (reset! captured-dispatch-n events)))
      (try
        (rf/dispatch-sync [:user-expenses/reports-refresh])
        (is (not-any? #(= :user-expenses/fetch-report-supplier-stores (first %))
              @captured-dispatch-n))

        (rf/dispatch-sync [:user-expenses/reports-toggle-expanded-supplier "supplier-42"])
        (rf/dispatch-sync [:user-expenses/reports-refresh])
        (is (some #(= :user-expenses/fetch-report-supplier-stores (first %))
              @captured-dispatch-n))
        (finally
          (rf/reg-fx :dispatch-n (fn [events]
                                   (doseq [event events]
                                     (rf/dispatch event)))))))))
