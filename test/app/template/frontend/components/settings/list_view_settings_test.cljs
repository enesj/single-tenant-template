(ns app.template.frontend.components.settings.list-view-settings-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.settings.list-view-settings :as sut]
    [cljs.test :refer [deftest is testing]]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]))

(deftest displayed-table-width-prefers-live-measurement-test
  (testing "positive measured width wins over missing or non-positive stored width"
    (is (= 1155 (sut/displayed-table-width 1155 0)))
    (is (= 950 (sut/displayed-table-width 950 nil)))
    (is (= 1400 (sut/displayed-table-width nil 1400)))
    (is (nil? (sut/displayed-table-width 0 0)))
    (is (nil? (sut/displayed-table-width nil nil)))))

(deftest displayed-table-height-prefers-live-measurement-test
  (testing "positive measured height wins over missing or non-positive stored height"
    (is (= 1155 (sut/displayed-table-height 1155 0)))
    (is (= 450 (sut/displayed-table-height 450 nil)))
    (is (= 320 (sut/displayed-table-height nil 320)))
    (is (nil? (sut/displayed-table-height 0 0)))
    (is (nil? (sut/displayed-table-height nil nil)))))

(defn- mount-component! [component assertions]
  (let [container (.createElement js/document "div")
        root (rdom/createRoot container)
        original-raf (when (exists? js/requestAnimationFrame) js/requestAnimationFrame)
        original-caf (when (exists? js/cancelAnimationFrame) js/cancelAnimationFrame)]
    (.appendChild (.-body js/document) container)
    (try
      (when-not (some? original-raf)
        (set! js/requestAnimationFrame (fn [callback]
                                         (js/setTimeout callback 0))))
      (when-not (some? original-caf)
        (set! js/cancelAnimationFrame (fn [handle]
                                        (js/clearTimeout handle))))
      (test-utils/act (fn [] (.render root component)))
      (assertions container)
      (finally
        (.unmount root)
        (if (some? original-raf)
          (set! js/requestAnimationFrame original-raf)
          (js-delete js/globalThis "requestAnimationFrame"))
        (if (some? original-caf)
          (set! js/cancelAnimationFrame original-caf)
          (js-delete js/globalThis "cancelAnimationFrame"))
        (.removeChild (.-body js/document) container)))))

(deftest list-view-settings-panel-renders-grouped-extra-toggles-test
  (let [clicked (atom [])]
    (with-redefs [uix-rf/use-subscribe (fn [_query] nil)
                  sut/column-visibility-settings (fn [_props]
                                                   ($ :div {:id "column-visibility-settings-stub"}))]
      (mount-component!
        ($ sut/list-view-settings-panel
          {:entity-name :receipts
           :current-entity-name :receipts
           :entity-spec {:fields [{:id :amount :label "Amount"}]}
           :extra-toggle-groups [{:id "upload-defaults-group"
                                  :label "Upload Defaults"
                                  :toggles [{:id "toggle-payer-default"
                                             :label "Payer"
                                             :active? true
                                             :on-click #(swap! clicked conj :payer)}
                                            {:id "toggle-category-default"
                                             :label "Category"
                                             :active? false
                                             :on-click #(swap! clicked conj :category)}]}]})
        (fn [container]
          (let [group (.querySelector container "#upload-defaults-group")
                payer-btn (.querySelector container "#toggle-payer-default")
                category-btn (.querySelector container "#toggle-category-default")]
            (is (some? group) "Expected grouped toggle wrapper to render")
            (is (= "Upload DefaultsPayerCategory"
                  (.-textContent group))
              "Expected group label and toggle labels to render together")
            (is (some? payer-btn) "Expected first grouped toggle to render")
            (is (some? category-btn) "Expected second grouped toggle to render")
            (is (re-find #"btn-primary" (.-className payer-btn))
              "Active grouped toggles should render with the active pill styling")
            (is (re-find #"btn-ghost" (.-className category-btn))
              "Inactive grouped toggles should render with the inactive pill styling")
            (test-utils/act (fn [] (.click category-btn)))
            (is (= [:category] @clicked)
              "Clicking a grouped toggle should call its handler")))))))