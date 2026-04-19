(ns app.domain.frontend.expenses.pages.user.expenses-list-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.components.manual-expense-form.core :as manual-form]
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
            (#'expenses-page/render-actions
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
            (#'expenses-page/render-actions
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

(deftest render-add-form-uses-smart-input-and-dispatches-create
  (testing "expense list add flow returns the smart input form and dispatches the modal create event"
    (setup/reset-db!)
    (let [dispatches (atom [])
          success-callback (fn [])
          form-data {:payer_id "payer-1"
                     :currency "BAM"
                     :items [{:raw_label "Milk" :line_total 12.5}]}
          element (#'expenses-page/render-add-form
                   {:on-success success-callback
                    :on-cancel (fn [])})
          passed-props (unchecked-get (unchecked-get element "props") "argv")
          submit-fn (:on-submit passed-props)]
      (with-redefs [rf/dispatch (fn [event]
                                  (swap! dispatches conj event))]
        (is (identical? (.-type element) manual-form/manual-expense-form)
          "render-add-form should return the smart input component")
        (is (fn? submit-fn)
          "render-add-form should provide the smart form submit callback")
        (submit-fn form-data)
        (is (= 1 (count @dispatches))
          "smart input submit should dispatch exactly one create event")
        (let [[event-id submitted-data submitted-success] (first @dispatches)]
          (is (= :user-expenses/create-expense-modal event-id)
            "smart input submit should target the user-expenses modal create event")
          (is (= form-data submitted-data)
            "smart input submit should forward the prepared smart form payload unchanged")
          (is (identical? success-callback submitted-success)
            "smart input submit should preserve the list-view success callback"))))))