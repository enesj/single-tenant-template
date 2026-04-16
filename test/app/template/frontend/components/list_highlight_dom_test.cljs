(ns app.template.frontend.components.list-highlight-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.list.rows :as list-rows]
    [app.template.frontend.components.table :as table]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.hooks.display-settings :as display-settings]
    [cljs.test :refer-macros [deftest is testing]]
    [re-frame.core :as rf]
    [uix.core :refer [$]]))

(defn- mount-component! [component assertions]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)]
    (.appendChild (.-body js/document) container)
    (try
      (test-utils/act (fn [] (.render root component)))
      (assertions container)
      (finally
        (.unmount root)
        (.removeChild (.-body js/document) container)))))

(deftest row-highlight-classes-test
  (testing "Row applies bg-green-200/50 when recently-updated? is true"
    (let [row-data {:id "1" :name "Test Item"}
          render-row-fn (fn [item _]
                          {:cells [(:name item)]
                           :recently-updated? true
                           :recently-created? false})]
      (mount-component!
        ($ table/table
          {:headers ["Name"]
           :rows [row-data]
           :row-key :id
           :render-row render-row-fn
           :show-highlights? true})
        (fn [container]
          (let [tr (.querySelector container "tbody tr")]
            (is (some? tr) "Row should be rendered")
            (is (.contains (.-classList tr) "bg-green-200/50")
              "Row should have green background class for updates"))))))

  (testing "Row applies bg-blue-200/50 when recently-created? is true"
    (let [row-data {:id "2" :name "New Item"}
          render-row-fn (fn [item _]
                          {:cells [(:name item)]
                           :recently-updated? false
                           :recently-created? true})]
      (mount-component!
        ($ table/table
          {:headers ["Name"]
           :rows [row-data]
           :row-key :id
           :render-row render-row-fn
           :show-highlights? true})
        (fn [container]
          (let [tr (.querySelector container "tbody tr")]
            (is (.contains (.-classList tr) "bg-blue-200/50")
              "Row should have blue background class for creation"))))))

  (testing "Row has no highlight class when show-highlights? is false"
    (let [row-data {:id "3" :name "No Highlight"}
          render-row-fn (fn [item _]
                          {:cells [(:name item)]
                           :recently-updated? true
                           :recently-created? false})]
      (mount-component!
        ($ table/table
          {:headers ["Name"]
           :rows [row-data]
           :row-key :id
           :render-row render-row-fn
           :show-highlights? false})
        (fn [container]
          (let [tr (.querySelector container "tbody tr")]
            (is (not (.contains (.-classList tr) "bg-green-200/50"))
              "Row should NOT have highlight class when highlights are disabled")
            (is (not (.contains (.-classList tr) "bg-blue-200/50")))))))))

(deftest custom-row-class-test
  (testing "row-class-fn classes are appended to the rendered table row"
    (let [row-props {:entity-spec {:fields [{:id :name
                                             :label "Name"}]}
                     :editing nil
                     :set-editing! (fn [_] nil)
                     :entity-name :items
                     :recently-updated-ids #{}
                     :recently-created-ids #{}
                     :selected-ids #{}
                     :on-select-change (fn [& _] nil)
                     :visible-columns {:name true}
                     :column-order nil
                     :show-filtering? false
                     :filterable-fields []
                     :user-filterable-settings {}
                     :row-class-fn (fn [item]
                                     (when (:system? item)
                                       "custom-system-row border-l-4"))
                     :show-edit? false
                     :show-delete? false}
          render-row-fn (fn [item _]
                          (list-rows/render-row row-props {:item item}))]
      (with-redefs [display-settings/use-display-settings (fn [_]
                                                            {:show-select? false})]
        (mount-component!
          ($ table/table
            {:headers ["Select" "Name" "Actions"]
             :rows [{:id "1" :name "System Payer" :system? true}]
             :row-key :id
             :render-row render-row-fn
             :entity-name :items})
          (fn [container]
            (let [tr (.querySelector container "tbody tr")]
              (is (some? tr) "Row should be rendered")
              (is (.contains (.-classList tr) "custom-system-row")
                "Custom row class should be appended to the rendered table row")
              (is (.contains (.-classList tr) "border-l-4")
                "Additional row classes should also be preserved"))))))))

(deftest double-click-cell-dispatches-filter-test
  (testing "double-clicking a filterable list cell dispatches an equality filter"
    (let [dispatched (atom [])
          row-props {:entity-spec {:fields [{:id :status
                                             :label "Status"
                                             :input-type "select"
                                             :options [{:value "active" :label "Active"}
                                                       {:value "pending" :label "Pending"}]}]}
                     :editing nil
                     :set-editing! (fn [_] nil)
                     :entity-name :items
                     :recently-updated-ids #{}
                     :recently-created-ids #{}
                     :selected-ids #{}
                     :on-select-change (fn [& _] nil)
                     :visible-columns {:status true}
                     :column-order nil
                     :show-filtering? true
                     :filterable-fields [:status]
                     :user-filterable-settings {}
                     :show-edit? false
                     :show-delete? false}
          render-row-fn (fn [item _]
                          (list-rows/render-row row-props {:item item}))]
      (with-redefs [rf/dispatch (fn [event]
                                  (swap! dispatched conj event))
                    display-settings/use-display-settings (fn [_]
                                                            {:show-select? false})]
        (mount-component!
          ($ table/table
            {:headers ["Select" "Status" "Actions"]
             :rows [{:id "1" :status "active"}]
             :row-key :id
             :render-row render-row-fn
             :entity-name :items})
          (fn [container]
            (let [filter-target (.querySelector container "[title='Double-click to filter by this value']")]
              (is (some? filter-target) "Filterable cell wrapper should be rendered")
              (test-utils/act
                (fn []
                  (.dispatchEvent filter-target
                    (js/MouseEvent. "dblclick" #js {:bubbles true}))))
              (is (= [[::filter-events/apply-filter
                       :items
                       :status
                       [{:value "active" :label "Active"}]
                       false]]
                    @dispatched)
                "Double-click should dispatch an exact-match filter event"))))))))

(deftest display-source-field-uses-item-and-field-in-correct-order
  (testing "rows resolve display-source-field values without crashing"
    (let [row-props {:entity-spec {:fields [{:id :status
                                             :label "Status"
                                             :type :select
                                             :input-type "select"
                                             :display-source-field :status-label
                                             :options [{:value "active" :label "Active"}
                                                       {:value "pending" :label "Pending"}]}]}
                     :editing nil
                     :set-editing! (fn [_] nil)
                     :entity-name :items
                     :recently-updated-ids #{}
                     :recently-created-ids #{}
                     :selected-ids #{}
                     :on-select-change (fn [& _] nil)
                     :visible-columns {:status true}
                     :column-order nil
                     :show-filtering? false
                     :filterable-fields []
                     :user-filterable-settings {}
                     :show-edit? false
                     :show-delete? false}
          render-row-fn (fn [item _]
                          (list-rows/render-row row-props {:item item}))]
      (with-redefs [display-settings/use-display-settings (fn [_]
                                                            {:show-select? false})]
        (mount-component!
          ($ table/table
            {:headers ["Select" "Status" "Actions"]
             :rows [{:id "1" :status "active" :status-label "Active"}]
             :row-key :id
             :render-row render-row-fn
             :entity-name :items})
          (fn [container]
            (is (some? (.querySelector container "tbody tr"))
              "Row should render successfully")
            (is (some? (.querySelector container ".ds-badge"))
              "Status badge should render when display-source-field is present")
            (is (re-find #"Active" (.-textContent container))
              "Display-source field value should be rendered")))))))

(deftest display-source-field-status-badges-keep-raw-colors-and-translated-labels
  (testing "status badges use display-source-field for labels but raw values for badge colors"
    (let [row-props {:entity-spec {:fields [{:id :status
                                             :label "Status"
                                             :type :select
                                             :input-type "select"
                                             :display-source-field :receipt-status-display
                                             :options [{:value "review_required" :label "Review Required"}
                                                       {:value "posted" :label "Posted"}]}]}
                     :editing nil
                     :set-editing! (fn [_] nil)
                     :entity-name :receipts
                     :recently-updated-ids #{}
                     :recently-created-ids #{}
                     :selected-ids #{}
                     :on-select-change (fn [& _] nil)
                     :visible-columns {:status true}
                     :column-order nil
                     :show-filtering? false
                     :filterable-fields []
                     :user-filterable-settings {}
                     :show-edit? false
                     :show-delete? false}
          render-row-fn (fn [item _]
                          (list-rows/render-row row-props {:item item}))]
      (with-redefs [display-settings/use-display-settings (fn [_]
                                                            {:show-select? false})]
        (mount-component!
          ($ table/table
            {:headers ["Select" "Status" "Actions"]
             :rows [{:id "1"
                     :status "review_required"
                     :receipt-status-display "Potrebna provjera"}]
             :row-key :id
             :render-row render-row-fn
             :entity-name :receipts})
          (fn [container]
            (let [badge (.querySelector container ".ds-badge")]
              (is (some? badge)
                "Status badge should render")
              (is (.contains (.-classList badge) "ds-badge-warning")
                "Badge variant should still come from the raw status value")
              (is (re-find #"Potrebna provjera" (.-textContent badge))
                "Badge label should come from the translated display-source field"))))))))

