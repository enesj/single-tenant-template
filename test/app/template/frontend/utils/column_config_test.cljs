(ns app.template.frontend.utils.column-config-test
  (:require
    [app.admin.frontend.config.loader :as admin-config]
    [app.template.frontend.utils.column-config :as column-config]
    [cljs.test :refer [deftest is testing]]))

(deftest vector-config?-test
  (testing "vector-config? delegates to loader"
    (with-redefs [admin-config/has-vector-config? (fn [entity] (= entity :items))]
      (is (true? (column-config/vector-config? :items)))
      (is (false? (column-config/vector-config? :transactions))))))

(deftest get-visible-columns-test
  (testing "Vector mode converts vectors into boolean map and enforces always-visible"
    (with-redefs [admin-config/get-available-columns (fn [_] [:id :name :status])
                  admin-config/is-always-visible? (fn [_ column] (= column :id))]
      (let [result (column-config/get-visible-columns true :items [:name])]
        (is (= {:id true :name true :status false} result))
        (is (= true (:id result)) "Always visible column should be forced true regardless of input"))))

  (testing "Legacy mode returns stored map"
    (is (= {:name true}
          (column-config/get-visible-columns false :items {:name true})))))

(deftest toggle-events-test
  (testing "toggle-column-event switches between admin and template events"
    (is (= [:admin/toggle-column-visibility :items :name]
          (column-config/toggle-column-event true :items :name)))
    (is (= [:app.template.frontend.events.list.settings/toggle-column-visibility :items :name]
          (column-config/toggle-column-event false :items :name))))

  (testing "toggle-filter-event always targets template settings"
    (is (= [:app.template.frontend.events.list.settings/toggle-field-filtering :items :status]
          (column-config/toggle-filter-event true :items :status))))

  (testing "update-table-width-event returns template event"
    (is (= [:app.template.frontend.events.list.settings/update-table-width :items 900]
          (column-config/update-table-width-event :items 900)))))
