(ns app.admin.frontend.pages.domain.expenses.stores-test
  (:require
    [app.admin.frontend.pages.domain.expenses.stores :as stores-page]
    [app.domain.frontend.expenses.events.stores :as stores-events]
    [app.template.frontend.events.list.ui-state :as ui-state])
  (:require-macros
    [cljs.test :refer [deftest is testing]]))

(deftest dispatch-admin-stores-refresh-configures-server-pagination-and-loads-first-page
  (testing "admin stores refresh enables server pagination, registers the refresh event, and loads page 1"
    (let [dispatches (atom [])
          sync-dispatches (atom [])]
      (stores-page/dispatch-admin-stores-refresh!
        #(swap! dispatches conj %)
        #(swap! sync-dispatches conj %))

      (is (= [[::ui-state/set-pagination-mode :stores :server]
              [::ui-state/set-refresh-event :stores [::stores-events/load-list]]]
            @sync-dispatches))
      (is (= [[:app.domain.frontend.expenses.events.cities/load-list
               {:fetch-limit 200 :fetch-offset 0}]
              [::stores-events/load-list {:page 1 :per-page 25}]]
            @dispatches)))))
