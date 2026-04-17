(ns app.admin.frontend.events.config-test
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.core :as admin-core]
    [app.admin.frontend.events.config]
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.registry :as domain-registry]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest toggle-column-visibility-persists-explicit-false
  (testing "Vector-config toggle writes an explicit true/false visibility map so hidden columns actually hide"
    (setup/reset-db!)
    (swap! rf-db/app-db assoc :current-route {:data {:name :admin/admins}})

    ;; Minimal config for :admins entity.
    (swap! rf-db/app-db assoc-in
      [:admin :config :table-columns :admins]
      {:available-columns [:id :email :role]
       :always-visible [:id]
       ;; Start with everything visible.
       :visible-columns [:id :email :role]})

    ;; Hide :email.
    (rf/dispatch-sync [:app.admin.frontend.events.config/toggle-column-visibility :admins :email])

    (let [visible-order (get-in @rf-db/app-db [:ui :entity-prefs :admin/admins :columns :visible-order])
          visible-map (get-in @rf-db/app-db [:ui :entity-prefs :admin/admins :columns :visible])]
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

(deftest load-ui-configs-waits-for-authentication
  (testing "Admin login bootstrap skips protected config fetches until a token exists"
    (setup/reset-db!)
    (swap! rf-db/app-db dissoc :admin/token :admin/authenticated?)
    (.clear js/localStorage)
    (.replaceState js/window.history nil "" "/admin/login")

    (rf/dispatch-sync [:admin/load-ui-configs])

    (is (false? (:admin/config-loading? @rf-db/app-db))
      "Login bootstrap should not start protected config fetches without a token")

    (is (true? (get-in @rf-db/app-db [:admin :config :bootstrap :awaiting-auth?]))
      "Bootstrap state should remember that config loading is deferred until auth succeeds")))

(deftest update-configs-from-cache-normalizes-legacy-audit-columns
  (testing "admin bootstrap upgrades legacy audit table-column defaults from cache"
    (setup/reset-db!)
    (with-redefs [config-loader/load-all-configs
                  (fn []
                    {:table-columns
                     {:audit-logs
                      {:available-columns ["action"
                                           "entity-name"
                                           "admin-email"
                                           "admin-name"
                                           "user-agent"
                                           "id"
                                           "actor-type"
                                           "actor-id"
                                           "target-type"
                                           "target-id"
                                           "metadata"
                                           "created-at"
                                           "updated-at"]
                       :default-visible-columns ["action" "entity-name" "admin-email" "admin-name"]
                       :filterable-columns ["action" "entity-name" "admin-email" "admin-name" "user-agent"]
                       :sortable-columns ["action" "entity-name" "admin-email" "admin-name" "user-agent"]
                       :always-visible ["action"]
                       :computed-fields {}
                       :column-config {:action {:width "140px"}}}}
                     :view-options {}
                     :form-fields {}})]
      (rf/dispatch-sync [:app.admin.frontend.events.config/update-configs-from-cache])
      (let [audit-config (get-in @rf-db/app-db [:admin :config :table-columns :audit-logs])]
        (is (= ["created-at" "action" "actor-display-name" "entity-name" "context-summary"]
              (:default-visible-columns audit-config)))
        (is (some #{"actor-display-name"} (:available-columns audit-config)))
        (is (some #{"context-summary"} (:available-columns audit-config)))
        (is (= {:label "Actor"}
              (get-in audit-config [:column-metadata :actor-display-name])))))))

(deftest init-admin-skips-ui-config-bootstrap-without-session
  (testing "Admin startup does not dispatch protected config loads before a session exists"
    (setup/reset-db!)
    (.clear js/localStorage)
    (.replaceState js/window.history nil "" "/admin/login")
    (reset! @#'app.admin.frontend.core/admin-initialized? false)
    (let [dispatches (atom [])]
      (with-redefs [re-frame.core/dispatch (fn [event] (swap! dispatches conj event))
                    re-frame.core/dispatch-sync (fn [event] (swap! dispatches conj event))
                    app.domain.frontend.registry/init-all-domains! (fn [] nil)]
        (admin-core/init-admin!))
      (is (some #(= [:admin/init-auth-persistence] %) @dispatches)
        "Admin init should still restore auth persistence")
      (is (not-any? #(= [:admin/load-ui-configs] %) @dispatches)
        "Admin init should not request protected UI configs before a valid session exists"))))
