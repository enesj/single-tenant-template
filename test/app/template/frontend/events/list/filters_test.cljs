(ns app.template.frontend.events.list.filters-test
  "Tests for list filter management events"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.filters :as filters-events]
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

  (testing "clear-filter without field removes filters key"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (rf/dispatch-sync [::filters-events/apply-filter :items :description "foo"])
    (rf/dispatch-sync [::filters-events/apply-filter :items :status {:value "bar" :label "Bar"}])

    (rf/dispatch-sync [::filters-events/clear-filter :items nil])
    (is (nil? (get-in @rf-db/app-db (filters-path :items)))
      "Clearing with nil field should remove filters key")))

(deftest error-handling-test
  (testing "Missing entity or field should leave db unchanged"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (let [before @rf-db/app-db]
      (rf/dispatch-sync [::filters-events/apply-filter nil :any "value"])
      (rf/dispatch-sync [::filters-events/apply-filter :items nil "value"])
      (is (= before @rf-db/app-db) "Invalid parameters should no-op"))))
