(ns app.template.frontend.events.list.filters-test
  "Tests for list filter management events"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.filters :as filters-events]
    [app.template.frontend.events.list.ui-state :as ui-state-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce filters-test-events-registered
  (do
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))
    (rf/reg-event-db
      ::test-refresh
      (fn [db [_ marker]]
        (assoc-in db [:test :last-refresh] marker)))
    true))

(defn- filters-path [entity]
  (conj (paths/list-ui-state entity) :filters))

(defn- set-entity-fields!
  [entity fields]
  (swap! rf-db/app-db assoc-in [:ui :entity-configs entity :fields] fields))

(deftest apply-filter-text-test
  (testing "Text filters normalize field ids and close modal by default"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    (rf/dispatch-sync [::filters-events/apply-filter :items :description "apple"])

    (let [db @rf-db/app-db]
      (is (= "apple" (get-in db (conj (filters-path :items) :description)))
        "Should store string filter under keyword field id")
      (is (= {:open? false} (get-in db [:ui :filter-modal]))
        "Modal should be closed by default")))

  (testing "Explicit keep-modal-open? leaves modal state unchanged"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (let [before (get-in @rf-db/app-db [:ui :filter-modal])]
      (rf/dispatch-sync [::filters-events/apply-filter :items :description "keep" true])
      (is (= before (get-in @rf-db/app-db [:ui :filter-modal]))
        "Modal should remain unchanged when keep flag provided"))))

(deftest apply-select-filter-test
  (testing "Select filters expand to value/label maps using entity config"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (set-entity-fields! :items
      [{:id "status" :input-type "select" :options {"active" "Active" "pending" "Pending"}}
       {:id "amount" :input-type "number"}])

    (rf/dispatch-sync [::filters-events/apply-filter :items "status" ["active" "pending"]])
    (let [stored (get-in @rf-db/app-db (conj (filters-path :items) :status))]
      (is (= [{:value "active" :label "Active"}
              {:value "pending" :label "Pending"}]
            stored)
        "Should map raw option values to {:value :label} format"))

    (testing "Reapplying identical filter should be idempotent"
      (let [before @rf-db/app-db]
        (rf/dispatch-sync [::filters-events/apply-filter :items "status" ["active" "pending"]])
        (is (= before @rf-db/app-db) "Applying identical filter should not mutate db"))))

  (testing "Select filter with single value map stays unchanged"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (set-entity-fields! :items [{:id "status" :input-type "select"}])

    (rf/dispatch-sync [::filters-events/apply-filter :items :status {:value "active" :label "Active"}])
    (is (= {:value "active" :label "Active"}
          (get-in @rf-db/app-db (conj (filters-path :items) :status)))
      "Should keep existing map values intact")))

(deftest clear-filter-test
  (testing "Nil or empty values remove specific filter"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    ;; Seed with value to clear
    (rf/dispatch-sync [::filters-events/apply-filter :items :description "seed"])

    (rf/dispatch-sync [::filters-events/apply-filter :items :description nil])
    (is (nil? (get-in @rf-db/app-db (conj (filters-path :items) :description)))
      "Nil value should remove field filter")

    (rf/dispatch-sync [::filters-events/apply-filter :items :amount ""])
    (is (nil? (get-in @rf-db/app-db (conj (filters-path :items) :amount)))
      "Empty string should also remove filter"))

  (testing "clear-filter removes legacy snake_case/string keys too"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (swap! rf-db/app-db assoc-in (filters-path :items) {"supplier_display_name" "bin"})

    (rf/dispatch-sync [::filters-events/clear-filter :items :supplier-display-name])
    (is (= {} (get-in @rf-db/app-db (filters-path :items)))
      "Clearing should remove legacy string/snake_case filter keys"))

  (testing "clear-filter without field removes filters key"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::filters-events/apply-filter :items :description "foo"])
    (rf/dispatch-sync [::filters-events/apply-filter :items :status {:value "bar" :label "Bar"}])

    (rf/dispatch-sync [::filters-events/clear-filter :items nil])
    (is (nil? (get-in @rf-db/app-db (filters-path :items)))
      "Clearing with nil field should remove filters key")))

(deftest server-mode-filter-refresh-test
  (testing "In server mode apply/clear filter resets page and dispatches refresh"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :server])
    (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :filters]])
    (rf/dispatch-sync [::ui-state-events/set-current-page :items 5])

    (let [refresh-dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! refresh-dispatches conj event)))
      (try
        (rf/dispatch-sync [::filters-events/apply-filter :items :description "server-side"])
        (is (= 1 (get-in @rf-db/app-db (paths/list-current-page :items)))
          "Applying filter in server mode should reset page to 1")
        (is (= [[::test-refresh :filters]] @refresh-dispatches)
          "Applying filter in server mode should dispatch refresh")
        (finally
          (rf/reg-fx :dispatch rf/dispatch))))

    (let [refresh-dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! refresh-dispatches conj event)))
      (try
        (rf/dispatch-sync [::filters-events/clear-filter :items :description])
        (is (= 1 (get-in @rf-db/app-db (paths/list-current-page :items))))
        (is (= [[::test-refresh :filters]] @refresh-dispatches)
          "Clearing filter in server mode should dispatch refresh")
        (finally
          (rf/reg-fx :dispatch rf/dispatch)))))

  (testing "In client mode apply filter does not dispatch refresh but still resets page"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :client])
    (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :client-filters]])
    (rf/dispatch-sync [::ui-state-events/set-current-page :items 5])

    (let [refresh-dispatches (atom [])]
      (rf/reg-fx :dispatch (fn [event]
                             (swap! refresh-dispatches conj event)))
      (try
        (rf/dispatch-sync [::filters-events/apply-filter :items :description "client-side"])
        (is (empty? @refresh-dispatches)
          "Client mode should not dispatch refresh on filter apply")
        (is (= 1 (get-in @rf-db/app-db (paths/list-current-page :items)))
          "Client mode should still reset page to 1 on filter apply")
        (finally
          (rf/reg-fx :dispatch rf/dispatch)))))

  (testing "In client mode clear filter resets page to 1"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :client])
    (rf/dispatch-sync [::filters-events/apply-filter :items :description "seed"])
    (rf/dispatch-sync [::ui-state-events/set-current-page :items 3])

    (rf/dispatch-sync [::filters-events/clear-filter :items :description])
    (is (= 1 (get-in @rf-db/app-db (paths/list-current-page :items)))
      "Client mode should reset page to 1 on filter clear"))

  (testing "Reapplying identical filter does not reset page (one-shot guard)"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::filters-events/apply-filter :items :description "stable"])
    (rf/dispatch-sync [::ui-state-events/set-current-page :items 4])

    (rf/dispatch-sync [::filters-events/apply-filter :items :description "stable"])
    (is (= 4 (get-in @rf-db/app-db (paths/list-current-page :items)))
      "Identical filter value should not reset page — prevents lock-to-page-1 bug")))

(deftest error-handling-test
  (testing "Missing entity or field should leave db unchanged"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (let [before @rf-db/app-db]
      (rf/dispatch-sync [::filters-events/apply-filter nil :any "value"])
      (rf/dispatch-sync [::filters-events/apply-filter :items nil "value"])
      (is (= before @rf-db/app-db) "Invalid parameters should no-op"))))
