(ns app.domain.frontend.expenses.pages.user.unmapped-items-test
  (:require
    [app.domain.frontend.expenses.pages.user.unmapped-items :as unmapped-items-page]
    [app.template.frontend.events.list.ui-state :as ui-state])
  (:require-macros
    [cljs.test :refer [deftest is testing]]))

(deftest dispatch-tenant-unmapped-aliases-refresh-configures-read-only-list-view-state
  (testing "tenant unmapped aliases refresh enables server pagination, seeds page state, and loads page 1 with 50 rows"
    (let [dispatches (atom [])
          sync-dispatches (atom [])]
      (unmapped-items-page/dispatch-tenant-unmapped-aliases-refresh!
        #(swap! dispatches conj %)
        #(swap! sync-dispatches conj %))

      (is (= [[::ui-state/set-pagination-mode :unmapped-aliases :server]
              [::ui-state/set-refresh-event :unmapped-aliases [:user-expenses/refresh-unmapped-aliases-list]]
              [::ui-state/set-per-page :unmapped-aliases 50]
              [::ui-state/set-current-page :unmapped-aliases 1]]
            @sync-dispatches))
      (is (= [[:user-expenses/refresh-unmapped-aliases-list {:page 1 :per-page 50}]]
            @dispatches)))))
