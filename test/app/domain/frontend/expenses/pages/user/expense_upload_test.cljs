(ns app.domain.frontend.expenses.pages.user.expense-upload-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.pages.user.expense-upload :as upload-page]
    [app.template.frontend.components.file-drop-zone :as file-drop-zone]
    [app.template.frontend.utils.test-utils :as test-utils-common]
    [cljs.test :refer [async deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

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

(defn- seed-upload-page-db!
  [{:keys [upload-payer-id upload-expense-category-id upload-notes]
    :or {upload-payer-id "payer-1"
         upload-expense-category-id "cat-default"
         upload-notes "Global note"}}]
  (swap! rf-db/app-db
    (fn [db]
      (-> db
        (assoc :session {:authenticated? true
                         :user {:id "user-1" :email "user@example.com"}
                         :membership-role "member"})
        (assoc-in [:user-expenses :payers :items] [{:id "payer-1" :label "Personal"}])
        (assoc-in [:user-expenses :payers :user-payer-id] "payer-1")
        (assoc-in [:user-expenses :upload :payer-id] upload-payer-id)
        (assoc-in [:user-expenses :expense-categories :items]
          [{:id "cat-default" :name "Groceries" :is-default true}
           {:id "cat-other" :name "Office" :is-default false}])
        (assoc-in [:user-expenses :upload :expense-category-id] upload-expense-category-id)
        (assoc-in [:user-expenses :upload :notes] upload-notes)
        (assoc-in [:user-expenses :profile :data :settings :default-note] "Global note")
        (assoc-in [:user-expenses :receipts :items] [])))))

(deftest expense-upload-page-renders-category-and-notes-controls
  (testing "receipt upload page renders upload-time category and notes controls"
    (setup/reset-db!)
    (seed-upload-page-db! {})
    (with-redefs [rf/dispatch (fn [_] nil)
                  file-drop-zone/file-drop-zone (fn [_]
                                                  ($ :div {:id "stub-file-drop-zone"}
                                                    "drop"))]
      (mount!
        ($ upload-page/expense-upload-page)
        (fn [container cleanup!]
          (let [category-select (.querySelector container "#select-expense-category-expense-upload")
                notes-textarea (.querySelector container "#textarea-notes-expense-upload")]
            (is (some? category-select)
              "upload page should expose an expense-category chooser")
            (is (some? notes-textarea)
              "upload page should expose upload-time notes")
            (is (= "cat-default" (.-value category-select)))
            (is (= "Global note" (.-value notes-textarea)))
            (cleanup!)))))))

(deftest expense-upload-page-initializes-category-and-note-defaults
  (async done
    (testing "receipt upload page initializes category and note defaults from loaded defaults"
      (setup/reset-db!)
      (seed-upload-page-db! {:upload-payer-id nil
                             :upload-expense-category-id nil
                             :upload-notes nil})
      (let [dispatches (atom [])]
        (with-redefs [rf/dispatch (fn [event]
                                    (swap! dispatches conj event))
                      file-drop-zone/file-drop-zone (fn [_]
                                                      ($ :div {:id "stub-file-drop-zone"}
                                                        "drop"))]
          (mount!
            ($ upload-page/expense-upload-page)
            (fn [_ cleanup!]
              (js/setTimeout
                (fn []
                  (is (some #(= [:user-expenses/fetch-payers {:limit 100 :offset 0}] %)
                        @dispatches))
                  (is (some #(= [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}] %)
                        @dispatches))
                  (is (some #(= [:profile/fetch] %)
                        @dispatches))
                  (is (some #(= [:user-expenses/set-upload-payer-id "payer-1"] %)
                        @dispatches))
                  (is (some #(= [:user-expenses/set-upload-expense-category-id "cat-default"] %)
                        @dispatches))
                  (is (some #(= [:user-expenses/set-upload-notes "Global note"] %)
                        @dispatches))
                  (cleanup!)
                  (done))
                0))))))))