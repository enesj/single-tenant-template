(ns app.template.frontend.components.list-highlight-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.table :as table]
    [cljs.test :refer-macros [deftest is testing]]
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
