(ns app.template.frontend.subs.form-test
  "Tests for form subscriptions"
  (:require
    [app.shared.validation.builder :as validation-builder]
    [app.shared.validation.core :as validation-core]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.subs.form :as form-subs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

;; Provide a minimal models-data subscription for tests
(rf/reg-sub :models-data (fn [db _] (:models-data db)))

(deftest field-state-subs-test
  (testing "field state subs read from db"
    (reset-db! {:forms {:items {:errors {:name "err"}
                                :success {:name true}}
                        :transactions {:server-errors {:amount {:message "invalid"}}
                                       :success {:amount false}}}})
    (is (= "err"
          (:error @(rf/subscribe [::form-subs/field-validation-state :items :name]))))
    (is (= {:message "invalid"}
          (:error @(rf/subscribe [::form-subs/field-server-validation-state :transactions :amount]))))))

(deftest all-fields-valid?-test
  (testing "Create mode validates required fields"
    (reset-db! {:models-data {:items {}}
                :forms {:items {}}})
    (rf/clear-subscription-cache!)
    (with-redefs [validation-builder/create-validators (fn [_] {:items {:name :validator}})
                  validation-core/validation-result (fn [_ value]
                                                      {:valid? (some? value)})]
      (let [spec [{:id "name" :required true}]]
        (is (true? @(rf/subscribe [::form-subs/all-fields-valid? :items spec false {:name "ok"}])))
        (is (false? @(rf/subscribe [::form-subs/all-fields-valid? :items spec false {:name nil}])))
        (is (true? @(rf/subscribe [::form-subs/all-fields-valid? :items spec false {:name "present" :extra 1}]))))))

  (testing "Edit mode only checks dirty fields"
    (reset-db! {:models-data {:items {}}
                :forms {:items {:dirty-fields #{:name}}}})
    (rf/clear-subscription-cache!)
    (with-redefs [validation-builder/create-validators (fn [_] {:items {:name :validator :email :validator}})
                  validation-core/validation-result (fn [_ value]
                                                      {:valid? (and (not= value :invalid) (some? value))})]
      (let [spec [{:id "name" :required true}
                  {:id "email" :required true}]
            _values {:name :invalid :email nil}]
        ;; Mark email dirty to ensure it's now considered
        (swap! rf-db/app-db assoc-in (paths/form-dirty-fields :items) #{:name :email})
        (rf/clear-subscription-cache!)
        (is (false? @(rf/subscribe [::form-subs/all-fields-valid? :items spec true {:name :valid :email nil}])))
        (is (true? @(rf/subscribe [::form-subs/all-fields-valid? :items spec true {:name :valid :email :value}])))))))

(deftest dirty-fields-sub-test
  (testing "dirty-fields defaults to empty set"
    (reset-db! {:forms {:items {}}})
    (is (= #{} @(rf/subscribe [::form-subs/dirty-fields :items]))))

  (testing "dirty-fields reflects stored set"
    (reset-db! {:forms {:items {:dirty-fields #{:name}}}})
    (is (= #{:name} @(rf/subscribe [::form-subs/dirty-fields :items])))))
