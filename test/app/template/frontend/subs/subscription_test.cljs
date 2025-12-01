(ns app.template.frontend.subs.subscription-test
  "Tests for subscription state subscriptions"
  (:require
    [app.template.frontend.subs.subscription]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

(deftest status-derived-subs-test
  (testing "Status and tier derived from db"
    (reset-db! {:subscription {:status {:subscription-tier :pro
                                        :limits {:properties 100}
                                        :subscription-status :active
                                        :trial-ends-at "2024-12-01"}
                               :usage {:properties 80}}})
    (is (= {:subscription-tier :pro
            :limits {:properties 100}
            :subscription-status :active
            :trial-ends-at "2024-12-01"}
          @(rf/subscribe [:subscription/status])))
    (is (= :pro @(rf/subscribe [:subscription/tier])))
    (is (= {:properties 100}
          @(rf/subscribe [:subscription/limits])))
    (is (= {:properties 80}
          @(rf/subscribe [:subscription/usage])))
    (is (= "2024-12-01"
          @(rf/subscribe [:subscription/trial-ends-at])))))

(deftest status-flags-test
  (testing "Derived status flags reflect subscription state"
    (reset-db! {:subscription {:status {:subscription-status :trialing}}})
    (is (true? @(rf/subscribe [:subscription/is-trial?])))
    (reset-db! {:subscription {:status {:subscription-status :active}}})
    (is (true? @(rf/subscribe [:subscription/is-active?])))
    (reset-db! {:subscription {:status {:subscription-status :past-due}}})
    (is (true? @(rf/subscribe [:subscription/is-past-due?])))))

(deftest usage-metrics-test
  (testing "Usage percentage and warnings"
    (reset-db! {:subscription {:limits {:properties 100}
                               :usage {:properties 85}}})
    (is (= 85.0 @(rf/subscribe [:subscription/usage-percentage :properties])))
    (is (true? @(rf/subscribe [:subscription/usage-warning? :properties])))
    (is (false? @(rf/subscribe [:subscription/is-usage-limit-exceeded? :properties])))
    (swap! rf-db/app-db assoc-in [:subscription :usage :properties] 120)
    (rf/clear-subscription-cache!)
    (is (true? @(rf/subscribe [:subscription/is-usage-limit-exceeded? :properties])))))

(deftest ui-flags-test
  (testing "Loading and error defaults"
    (reset-db! {:subscription {:loading? true :error "oops"}})
    (is (true? @(rf/subscribe [:subscription/loading?])))
    (is (= "oops" @(rf/subscribe [:subscription/error]))))

  (testing "Modal toggles"
    (reset-db! {:subscription {:ui {:show-upgrade-modal? true
                                    :show-billing-modal? false}}})
    (is (true? @(rf/subscribe [:subscription/show-upgrade-modal?])))
    (is (false? @(rf/subscribe [:subscription/show-billing-modal?])))))
