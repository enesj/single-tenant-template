(ns app.admin.frontend.pages.domain.expenses.receipts-test
  (:require
    [app.admin.frontend.pages.domain.expenses.receipts :as receipts-page]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [cljs.test :refer [async deftest is testing]]
    [re-frame.core :as rf]
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]))

(defn- mount!
  [component f]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)
        cleanup! (fn []
                   (test-utils/act (fn [] (.unmount root)))
                   (.removeChild (.-body js/document) container))]
    (.appendChild (.-body js/document) container)
    (test-utils/act (fn [] (.render root component)))
    (f container cleanup!)))

(deftest dispatch-admin-receipts-refresh-configures-server-pagination-and-loads-first-page
  (testing "admin receipts refresh enables server pagination, registers the refresh event, and loads page 1"
    (setup/reset-db!)
    (let [dispatches (atom [])
          sync-dispatches (atom [])]
      (receipts-page/dispatch-admin-receipts-refresh!
        #(swap! dispatches conj %)
        #(swap! sync-dispatches conj %))

      (is (= [[::ui-state/set-pagination-mode :receipts :server]
              [::ui-state/set-refresh-event :receipts [::receipts-events/load-list]]]
            @sync-dispatches))
      (is (= [[::receipts-events/load-list {:page 1}]]
            @dispatches)))))

(deftest render-receipt-actions-routes-edit-to-custom-detail-handler
  (async done
    (testing "admin receipt row actions call the custom detail handler instead of generic inline editing"
      (setup/reset-db!)
      (let [dispatches (atom [])
            opened (atom nil)
            receipt {:id "receipt-123"
                     :show-edit? true
                     :show-delete? true}]
        (with-redefs [rf/dispatch (fn [event]
                                    (swap! dispatches conj event))]
          (mount!
            (#'app.admin.frontend.pages.domain.expenses.receipts/render-receipt-actions
             (fn [item]
               (reset! opened item))
             receipt)
            (fn [container cleanup!]
              (let [edit-btn (.querySelector container "#btn-edit-receipts-receipt-123")]
                (is (some? edit-btn) "the admin receipt actions should render an Edit button")
                (test-utils/act (fn [] (.click edit-btn)))
                (js/setTimeout
                  (fn []
                    (is (= receipt @opened))
                    (is (not-any? #(= :app.template.frontend.events.config/set-editing (first %))
                          @dispatches))
                    (cleanup!)
                    (done))
                  0)))))))))