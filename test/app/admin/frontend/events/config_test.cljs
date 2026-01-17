(ns app.admin.frontend.events.config-test
  (:require
    [app.admin.frontend.events.config] ;; ensure events are registered
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest toggle-column-visibility-persists-explicit-false
  (testing "Vector-config toggle writes an explicit true/false visibility map so hidden columns actually hide"
    (setup/reset-db!)

    ;; Minimal config for :admins entity.
    (swap! rf-db/app-db assoc-in
      [:admin :config :table-columns :admins]
      {:available-columns [:id :email :role]
       :always-visible [:id]
       ;; Start with everything visible.
       :visible-columns [:id :email :role]})

    ;; Hide :email.
    (rf/dispatch-sync [:app.admin.frontend.events.config/toggle-column-visibility :admins :email])

    (let [visible-order (get-in @rf-db/app-db [:ui :entity-prefs :admins :columns :visible-order])
          visible-map (get-in @rf-db/app-db [:ui :entity-prefs :admins :columns :visible])]
      (is (= [:id :role] visible-order)
        "Toggling a column off should remove it from :visible-order")

      (is (= false (get visible-map :email))
        "Hidden columns must have an explicit false entry in :visible")

      (is (= true (get visible-map :id))
        "Always-visible columns must remain visible")

      (is (= true (get visible-map :role))
        "Other visible columns should remain true")

      (is (= #{:id :email :role} (set (keys visible-map)))
        "Visibility map should cover all available columns (true/false)"))))
