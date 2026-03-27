(ns app.admin.frontend.pages.domain.expenses.unmapped-aliases-test
  (:require
    [app.admin.frontend.pages.domain.expenses.unmapped-aliases :as unmapped-aliases-page]
    [app.domain.frontend.expenses.events.unmapped-aliases :as unmapped-aliases-events]
    [app.template.frontend.events.list.ui-state :as ui-state])
  (:require-macros
    [cljs.test :refer [deftest is testing]]))

(deftest dispatch-admin-unmapped-aliases-refresh-configures-server-pagination-and-loads-first-page
  (testing "admin unmapped aliases refresh enables server pagination and loads page 1 with 50 rows"
    (let [dispatches (atom [])
          sync-dispatches (atom [])]
      (unmapped-aliases-page/dispatch-admin-unmapped-aliases-refresh!
        #(swap! dispatches conj %)
        #(swap! sync-dispatches conj %))

      (is (= [[::ui-state/set-pagination-mode :unmapped-aliases :server]
              [::ui-state/set-refresh-event :unmapped-aliases [::unmapped-aliases-events/load-list]]]
            @sync-dispatches))
      (is (= [[::unmapped-aliases-events/load-list {:page 1 :per-page 50}]]
            @dispatches)))))

(deftest admin-unmapped-aliases-list-props-are-explicitly-read-only
  (testing "admin unmapped aliases page passes the same read-only list-view contract as the tenant page"
    (let [props (unmapped-aliases-page/admin-unmapped-aliases-list-props
                  :unmapped-aliases
                  [{:id :raw-label :label "Raw label" :type :text}])]
      (is (= :unmapped-aliases (:entity-name props)))
      (is (= "Unmapped Aliases" (:title props)))
      (is (= false (:allow-add? props)))
      (is (= false (:allow-edit? props)))
      (is (= false (:allow-delete? props)))
      (is (= {:show-add-button? false
              :show-edit? false
              :show-delete? false
              :show-batch-edit? false
              :show-batch-delete? false}
            (:display-settings props))))))