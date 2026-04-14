(ns app.domain.frontend.expenses.pages.user.expenses-list-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.pages.user.expenses-list :as expenses-page]
    [app.template.frontend.utils.test-utils :as test-utils-common]
    [cljs.test :refer [async deftest is testing]]
    [re-frame.core :as rf]))

(test-utils-common/setup-test-environment!)

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

(deftest render-actions-keeps-manual-expenses-inline-editable
  (async done
    (testing "manual expenses keep the standard edit button and use the provided edit callback"
      (setup/reset-db!)
      (let [dispatches (atom [])
            edited (atom nil)
            expense {:id "expense-123"
                     :show-edit? true
                     :show-delete? false
                     :on-edit-click (fn [item]
                                      (reset! edited item))}]
        (with-redefs [rf/dispatch (fn [event]
                                    (swap! dispatches conj event))]
          (mount!
            (#'app.domain.frontend.expenses.pages.user.expenses-list/render-actions
             (fn [k] (name k))
             expense
             {:power-user? false})
            (fn [container cleanup!]
              (let [edit-btn (.querySelector container "#btn-edit-expenses-expense-123")]
                (is (some? edit-btn) "manual expenses should render the standard edit button")
                (test-utils/act (fn [] (.click edit-btn)))
                (js/setTimeout
                  (fn []
                    (is (= {:id "expense-123"} @edited)
                      "manual expenses should keep the direct expense edit callback")
                    (is (empty? @dispatches)
                      "manual edit should not redirect into the receipt flow")
                    (cleanup!)
                    (done))
                  0)))))))))

(deftest render-actions-routes-linked-expenses-to-receipt-editing
  (async done
    (testing "receipt-linked expenses render a receipt edit button and open the linked receipt"
      (setup/reset-db!)
      (let [dispatches (atom [])
            opened-receipt-id (atom nil)
            expense {:id "expense-456"
                     :receipt-id "receipt-789"
                     :show-edit? true
                     :show-delete? false}]
        (with-redefs [rf/dispatch (fn [event]
                                    (swap! dispatches conj event))]
          (mount!
            (#'app.domain.frontend.expenses.pages.user.expenses-list/render-actions
             (fn [k] (name k))
             expense
             {:power-user? false
              :open-linked-receipt! (fn [receipt-id]
                                      (reset! opened-receipt-id receipt-id))})
            (fn [container cleanup!]
              (let [edit-btn (.querySelector container "#btn-edit-receipt-for-expense-expense-456")]
                (is (some? edit-btn) "linked expenses should render the receipt edit button")
                (test-utils/act (fn [] (.click edit-btn)))
                (js/setTimeout
                  (fn []
                    (is (= "receipt-789" @opened-receipt-id)
                      "linked expense edits should open the linked receipt instead of inline expense editing")
                    (is (empty? @dispatches)
                      "linked receipt button should use the provided receipt opener directly")
                    (cleanup!)
                    (done))
                  0)))))))))