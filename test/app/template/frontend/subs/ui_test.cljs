(ns app.template.frontend.subs.ui-test
  "Tests for UI subscriptions"
  (:require
    [app.template.frontend.subs.ui :as ui-subs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

(deftest recently-changed-tests
  (testing "recently updated/created derive sets from db"
    (reset-db! {:ui {:recently-updated {:items #{1 2}}
                     :recently-created {:items #{3}}}})
    (is (= #{1 2} @(rf/subscribe [::ui-subs/recently-updated-entities :items])))
    (is (= #{3} @(rf/subscribe [::ui-subs/recently-created-entities :items])))))

(deftest entity-display-settings-precedence-test
  (testing "Entity-specific overrides trump defaults and globals"
    (reset-db! {:ui {:show-edit? true
                     :show-delete? false
                     :defaults {:show-edit? false
                                :show-delete? true
                                :show-select? true
                                :controls {:show-select-control? true}}
                     :entity-configs {:items {:show-edit? false
                                              :controls {:show-select-control? false}}}}})
    (let [display @(rf/subscribe [::ui-subs/entity-display-settings :items])]
      (is (false? (:show-edit? display)) "Entity override should take precedence")
      (is (true? (:show-delete? display)) "Defaults should apply when entity override missing")
      (is (= true (:show-select? display)) "Defaults propagate to nested controls")
      (is (= false (get-in display [:controls :show-select-control?])) "Entity-specific control overrides default")))

  (testing "Fallback to provided default when values absent"
    (reset-db! {:ui {:entity-configs {} :defaults {} :show-timestamps? false}})
    (let [display @(rf/subscribe [::ui-subs/entity-display-settings :transactions])]
      (is (false? (:show-timestamps? display)) "Global fallback should be used when no overrides"))))

(deftest visible-columns-test
  (testing "visible-columns returns entity config map"
    (reset-db! {:ui {:entity-configs {:items {:visible-columns {:name true :id false}}}}})
    (is (= {:name true :id false}
          @(rf/subscribe [::ui-subs/visible-columns :items])))))

(deftest filterable-fields-vector-config-test
  (testing "filterable-fields reads from app-db config"
    ;; Set up app-db with table-columns config
    (reset-db! {:admin {:config {:table-columns {:items {:filterable-columns [:name :status]}}}}})
    (is (= [:name :status]
          @(rf/subscribe [::ui-subs/filterable-fields :items])))))
