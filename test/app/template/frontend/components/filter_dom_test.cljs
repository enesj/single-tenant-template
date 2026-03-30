(ns app.template.frontend.components.filter-dom-test
  (:require
    ["react-dom/client" :as rdom]
    ["react-dom/test-utils" :as test-utils]
    [app.template.frontend.components.filter :as filter]
    [app.template.frontend.components.filter.logic :as filter-logic]
    [app.template.frontend.components.filter.rendering :as filter-rendering]
    [app.template.frontend.components.filter.utils :as filter-utils]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.subs.list :as list-subs]
    [cljs.test :refer-macros [deftest is testing]]
    [re-frame.core :as rf]
    [uix.core :refer [$]]
    [uix.re-frame :as uix-rf]))

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

(deftest get-field-label-normalizes-db-style-filter-keys
  (testing "active filter labels resolve against kebab-case entity specs"
    (let [entity-config {:fields [{:id :supplier-display-name :label "Supplier"}
                                  {:id :store-display-name :label "Store"}]}]
      (is (= "Supplier" (filter-utils/get-field-label entity-config :supplier_display_name)))
      (is (= "Supplier" (filter-utils/get-field-label entity-config "supplier_display_name")))
      (is (= "Store" (filter-utils/get-field-label entity-config :store-display-name))))))

(deftest filter-form-wires-active-filter-clear-handler
  (testing "modal active-filter chips dispatch normalized clear events"
    (let [captured-props (atom nil)
          dispatched (atom [])]
      (with-redefs [uix-rf/use-subscribe (fn [query]
                                           (case (first query)
                                             ::entity-subs/entities []
                                             ::list-subs/active-filters {:supplier_display_name "bin"}
                                             ::list-subs/entity-ui-state {:pagination {:mode :server}}
                                             nil))
                    filter-logic/calculate-available-options (fn [_] [])
                    filter-logic/initialize-filter-state (fn [_]
                                                           {:filter-text ""
                                                            :filter-min nil
                                                            :filter-max nil
                                                            :filter-from-date nil
                                                            :filter-to-date nil
                                                            :filter-selected-options []})
                    filter-logic/use-entity-fetching (fn [& _] nil)
                    filter-logic/use-debug-logging (fn [& _] nil)
                    filter-logic/sync-state-with-initial-value (fn [& _] nil)
                    filter-logic/use-text-filter-auto-apply (fn [& _] nil)
                    filter-logic/use-number-range-auto-apply (fn [& _] nil)
                    filter-rendering/render-filter-form-layout (fn [props]
                                                                 (reset! captured-props props)
                                                                 ($ :div {:id "stub-filter-layout"}))
                    rf/dispatch (fn [event]
                                  (swap! dispatched conj event))]
        (mount-component!
          ($ filter/filter-form
            {:entity-type :store-aliases
             :field-spec {:id :store-display-name :label "Store" :type :text}
             :initial-value nil
             :on-close (fn [] nil)
             :on-apply (fn [& _] nil)})
          (fn [_container]
            (is (fn? (:on-clear-filter @captured-props))
              "filter form should pass a clear callback down to the active filter UI")
            (test-utils/act
              (fn []
                ((:on-clear-filter @captured-props) :supplier_display_name)))
            (is (= [[::filter-events/clear-filter :store-aliases :supplier-display-name]]
                  @dispatched)
              "clear callback should normalize DB-style keys before dispatching")))))))
