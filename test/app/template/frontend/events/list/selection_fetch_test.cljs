(ns app.template.frontend.events.list.selection-fetch-test
  "Tests for list selection fetch workflows"
  (:require
    [app.template.frontend.api.http :as http-api]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [day8.re-frame.http-fx :as http-fx]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [taoensso.timbre :as log]))

(defonce selection-test-events-registered
  (do
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))
    true))

(deftest fetch-item-by-id-test
  (testing "Fetch guard prevents duplicate requests and tracks loading state"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    (let [last-config (atom nil)]
      (with-redefs [println (fn [& _])
                    http-fx/http-effect (fn [_])
                    http-api/get-entity
                    (fn [config]
                      (reset! last-config config)
                      {:uri (:uri config)
                       :method :get})]
        (rf/dispatch-sync [::selection-events/fetch-item-by-id :items 42])

        (is (= {:entity-name "items" :id 42
                :on-success [::selection-events/fetch-item-success :items 42]
                :on-failure [:app.frontend.events.list.crud/fetch-failure :items]}
              (select-keys @last-config [:entity-name :id :on-success :on-failure]))
          "Should build http config with entity name and id")
        (is (= true (get-in @rf-db/app-db [:entity-fetches :items "42"]))
          "Fetch flag should be set for entity/id pair")
        (is (true? (get-in @rf-db/app-db (paths/entity-loading? :items)))
          "Entity loading flag should be true"))

      (reset! last-config nil)
      ;; Duplicate dispatch should not invoke HTTP again
      (rf/dispatch-sync [::selection-events/fetch-item-by-id :items 42])
      (is (nil? @last-config) "Duplicate fetch should be ignored when already loading")))

  (testing "Successful fetch clears guard and stores entity"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    ;; Seed initial guard state to mimic in-flight request
    (swap! rf-db/app-db assoc-in [:entity-fetches :items "42"] true)
    (swap! rf-db/app-db assoc-in (paths/entity-ids :items) [1])
    (swap! rf-db/app-db assoc-in (paths/entity-data :items) {1 {:id 1 :description "Existing"}})

    (let [response {:id 42 :description "Fetched"}
          before-data (get-in @rf-db/app-db (paths/entity-data :items))]
      (rf/dispatch-sync [::selection-events/fetch-item-success :items 42 response])

      (is (nil? (get-in @rf-db/app-db [:entity-fetches :items "42"]))
        "Fetch guard should be cleared after success")
      (is (= response (get-in @rf-db/app-db (conj (paths/entity-data :items) 42)))
        "Fetched item should be stored under id")
      (is (= [1 42] (get-in @rf-db/app-db (paths/entity-ids :items)))
        "Item id should be appended when absent")
      (is (false? (get-in @rf-db/app-db (paths/entity-loading? :items)))
        "Loading flag should be reset to false")
      (is (= 42 (get-in @rf-db/app-db [:ui :editing]))
        "Editing pointer should be updated")
      (is (= before-data (select-keys (get-in @rf-db/app-db (paths/entity-data :items)) (keys before-data)))
        "Existing entities should remain unchanged"))))

(deftest select-all-and-deselect-test
  (testing "Selection helpers respect provided item ids"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    (rf/dispatch-sync [::selection-events/select-all :items [{:id 1} {:items/id 2}]
                       true])
    (is (= #{1 2} (get-in @rf-db/app-db (paths/entity-selected-ids :items)))
      "Should collect ids from :id and namespaced keys")

    (rf/dispatch-sync [::selection-events/select-all :items [] false])
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :items)))
      "Clearing selection should reset to empty set")))
