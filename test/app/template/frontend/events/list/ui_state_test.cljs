(ns app.template.frontend.events.list.ui-state-test
  "Tests for list UI state management events"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.ui-state :as ui-state-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce ui-state-test-events-registered
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

(defn- current-page-path [entity]
  (paths/list-current-page entity))

(deftest resolved-list-pagination-prefers-persisted-display-prefs
  (testing "per-page falls back to persisted display prefs when list state is empty"
    (let [db {:ui {:entity-prefs {:items {:display {:per-page 50}}}
                   :lists {:items {:per-page nil
                                   :current-page nil
                                   :pagination {:per-page nil
                                                :current-page nil}}}}}]
      (is (= 50 (paths/resolved-list-per-page db :items 25)))
      (is (= 1 (paths/resolved-list-current-page db :items)))))

  (testing "admin routes read admin-scoped persisted prefs before user-scoped ones"
    (let [db {:current-route {:data {:name :admin/items}}
              :ui {:entity-prefs {:items {:display {:per-page 50}}
                                  :admin/items {:display {:per-page 25}}}
                   :lists {:items {:per-page nil
                                   :current-page nil
                                   :pagination {:per-page nil
                                                :current-page nil}}}}}]
      (is (= 25 (paths/resolved-list-per-page db :items 10)))
      (is (= 1 (paths/resolved-list-current-page db :items)))))

  (testing "explicit list state still wins over persisted browser prefs"
    (let [db {:ui {:entity-prefs {:items {:display {:per-page 50}}}
                   :lists {:items {:per-page 20
                                   :current-page 3
                                   :pagination {:per-page 20
                                                :current-page 3}}}}}]
      (is (= 20 (paths/resolved-list-per-page db :items 25)))
      (is (= 3 (paths/resolved-list-current-page db :items))))))

(deftest set-current-page-test
  (testing "Setting current page syncs all pagination paths"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    (rf/dispatch-sync [::ui-state-events/set-current-page :items 3])

    (let [db @rf-db/app-db
          pagination (get-in db (paths/list-ui-state :items))]
      (is (= 3 (get-in db (current-page-path :items))) "Should update compact current-page path")
      (is (= 3 (get-in pagination [:current-page])) "Should sync legacy :current-page")
      (is (= 3 (get-in pagination [:pagination :current-page])) "Should sync :pagination map"))

    (testing "Server mode dispatches configured refresh event"
      (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :server])
      (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :current-page]])
      (let [refresh-dispatches (atom [])]
        (rf/reg-fx :dispatch (fn [event]
                               (swap! refresh-dispatches conj event)))
        (try
          (rf/dispatch-sync [::ui-state-events/set-current-page :items 2])
          (is (= [[::test-refresh :current-page]] @refresh-dispatches)
            "Server mode should dispatch configured refresh event")
          (finally
            (rf/reg-fx :dispatch rf/dispatch)))))

    (testing "Client mode does not dispatch refresh event"
      (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :client])
      (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :client]])
      (let [refresh-dispatches (atom [])]
        (rf/reg-fx :dispatch (fn [event]
                               (swap! refresh-dispatches conj event)))
        (try
          (rf/dispatch-sync [::ui-state-events/set-current-page :items 4])
          (is (empty? @refresh-dispatches)
            "Client mode should not dispatch refresh event")
          (finally
            (rf/reg-fx :dispatch rf/dispatch)))))

    (testing "Page number is clamped to >= 1"
      (rf/dispatch-sync [::ui-state-events/set-current-page :items 0])
      (is (= 1 (get-in @rf-db/app-db (current-page-path :items))) "Should clamp to page 1 when non-positive"))

    (testing "Nil entity leaves db unchanged"
      (let [before @rf-db/app-db]
        (rf/dispatch-sync [::ui-state-events/set-current-page nil 5])
        (is (= before @rf-db/app-db) "Dispatch with nil entity should no-op")))))

(deftest set-per-page-test
  (testing "Setting per-page normalizes value and resets page"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])
    ;; Set current page to verify it resets to 1 later
    (rf/dispatch-sync [::ui-state-events/set-current-page :items 4])

    (rf/dispatch-sync [::ui-state-events/set-per-page :items "25"])
    (let [db @rf-db/app-db
          pagination (get-in db (paths/list-ui-state :items))]
      (is (= 25 (get-in db (paths/list-per-page :items))) "Should persist parsed integer per-page")
      (is (= 25 (get-in pagination [:per-page])) "Should sync legacy :per-page location")
      (is (= 25 (get-in pagination [:pagination :per-page])) "Should sync pagination map")
      (is (= 25 (get-in db [:ui :entity-prefs :items :display :per-page])) "Should persist user-route prefs under the entity key")
      (is (= 1 (get-in db (current-page-path :items))) "Setting per-page resets current page to 1"))

    (testing "Admin routes persist per-page under the admin-scoped entity key"
      (swap! rf-db/app-db assoc :current-route {:data {:name :admin/items}})
      (rf/dispatch-sync [::ui-state-events/set-per-page :items 30])
      (let [db @rf-db/app-db]
        (is (= 30 (get-in db [:ui :entity-prefs :admin/items :display :per-page]))
          "Admin per-page prefs should be isolated from user-route prefs")
        (is (= 25 (get-in db [:ui :entity-prefs :items :display :per-page]))
          "Existing user-route prefs should be preserved")))

    (testing "Server mode dispatches configured refresh event"
      (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :server])
      (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :per-page]])
      (let [refresh-dispatches (atom [])]
        (rf/reg-fx :dispatch (fn [event]
                               (swap! refresh-dispatches conj event)))
        (try
          (rf/dispatch-sync [::ui-state-events/set-per-page :items 30])
          (is (= [[::test-refresh :per-page]] @refresh-dispatches)
            "Server mode should dispatch configured refresh event")
          (finally
            (rf/reg-fx :dispatch rf/dispatch)))))

    (testing "Client mode does not dispatch refresh event"
      (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :client])
      (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [::test-refresh :client-per-page]])
      (let [refresh-dispatches (atom [])]
        (rf/reg-fx :dispatch (fn [event]
                               (swap! refresh-dispatches conj event)))
        (try
          (rf/dispatch-sync [::ui-state-events/set-per-page :items 15])
          (is (empty? @refresh-dispatches)
            "Client mode should not dispatch refresh event")
          (finally
            (rf/reg-fx :dispatch rf/dispatch)))))

    (testing "Non-numeric per-page falls back to default"
      (rf/dispatch-sync [::ui-state-events/set-per-page :items "bad-value"])
      (is (= 10 (get-in @rf-db/app-db (paths/list-per-page :items))) "Should fall back to 10 when parse fails"))

    (testing "Nil entity leaves db unchanged"
      (let [before @rf-db/app-db]
        (rf/dispatch-sync [::ui-state-events/set-per-page nil 20])
        (is (= before @rf-db/app-db) "Nil entity should not mutate state")))))

(deftest pagination-mode-and-refresh-event-infra-test
  (testing "Pagination mode defaults/coercion and refresh-event storage"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :server])
    (is (= :server (get-in @rf-db/app-db (paths/list-pagination-mode :items)))
      "Should store explicit :server mode")

    (rf/dispatch-sync [::ui-state-events/set-pagination-mode :items :unexpected])
    (is (= :client (get-in @rf-db/app-db (paths/list-pagination-mode :items)))
      "Unknown mode should normalize to :client")

    (rf/dispatch-sync [::ui-state-events/set-refresh-event :items [:expenses/fetch-page]])
    (is (= [:expenses/fetch-page]
          (get-in @rf-db/app-db (paths/list-refresh-event :items)))
      "Should store vector refresh event")

    (is (= [:expenses/fetch-page]
          (ui-state-events/list-refresh-dispatch @rf-db/app-db :items))
      "Helper should return stored vector dispatch")

    (rf/dispatch-sync [::ui-state-events/set-refresh-event :items :expenses/fetch-page])
    (is (= [:expenses/fetch-page]
          (ui-state-events/list-refresh-dispatch @rf-db/app-db :items))
      "Helper should wrap keyword event ids as dispatch vectors")))

(deftest sort-config-test
  (testing "Setting sort field toggles direction correctly"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    (rf/dispatch-sync [::ui-state-events/set-sort-field :items :name])
    (let [db @rf-db/app-db]
      (is (= [{:field :name :direction :asc}]
            (get-in db (paths/list-sorts :items)))
        "Should initialize the ordered sort stack with an ascending primary sort"))

    (rf/dispatch-sync [::ui-state-events/set-sort-field :items :name])
    (is (= [{:field :name :direction :desc}]
          (get-in @rf-db/app-db (paths/list-sorts :items)))
      "Repeated field toggles the primary sort direction to descending")

    (rf/dispatch-sync [::ui-state-events/set-sort-field :items :amount])
    (let [db @rf-db/app-db]
      (is (= [{:field :amount :direction :asc}]
            (get-in db (paths/list-sorts :items)))
        "Switching the primary sort field should replace the stack with an ascending sort for that field"))

    (testing "Nil entity leaves db unchanged"
      (let [before @rf-db/app-db]
        (rf/dispatch-sync [::ui-state-events/set-sort-field nil :any])
        (is (= before @rf-db/app-db))))))

(deftest toggle-flags-test
  (testing "Toggling entity-specific flags respects defaults"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [::test-initialize-db])

    ;; Global defaults true for show-edit?, false for show-select?
    (rf/dispatch-sync [::ui-state-events/toggle-edit :items])
    (rf/dispatch-sync [::ui-state-events/toggle-select :items])
    (let [db @rf-db/app-db]
      ;; toggle-entity-flag writes to new path [:ui :entity-prefs entity :display ...]
      (is (false? (get-in db [:ui :entity-prefs :items :display :show-edit?])) "Should toggle entity override from default true to false")
      (is (true? (get-in db [:ui :entity-prefs :items :display :show-select?])) "Should toggle entity override from default false to true"))

    (testing "Admin routes isolate display toggles under the admin-scoped key"
      (swap! rf-db/app-db assoc :current-route {:data {:name :admin/items}})
      (rf/dispatch-sync [::ui-state-events/toggle-delete :items])
      (let [db @rf-db/app-db]
        (is (false? (get-in db [:ui :entity-prefs :admin/items :display :show-delete?]))
          "Admin toggle should write under the admin-scoped prefs key")
        (is (nil? (get-in db [:ui :entity-prefs :items :display :show-delete?]))
          "User-route prefs should remain untouched")))

    (testing "Global toggles mutate ui root when entity nil"
      (rf/dispatch-sync [::ui-state-events/toggle-highlights nil])

      (rf/dispatch-sync [::ui-state-events/toggle-delete nil])
      (let [db @rf-db/app-db]
        (is (false? (get-in db [:ui :show-highlights?])) "Global highlight toggle flips existing true → false")

        (is (false? (get-in db [:ui :show-delete?])) "Global delete toggle flips existing true → false")))))
