(ns app.admin.frontend.events.settings-test
  (:require
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest settings-ui-smoke-test
  (testing "toggle-editing updates :editing? state"
    (setup/reset-db!)
    (is (not (get-in @rf-db/app-db [:admin :settings :editing?])))
    (rf/dispatch-sync [:app.admin.frontend.events.settings/toggle-editing])
    (is (true? (get-in @rf-db/app-db [:admin :settings :editing?])))
    (rf/dispatch-sync [:app.admin.frontend.events.settings/toggle-editing])
    (is (false? (get-in @rf-db/app-db [:admin :settings :editing?]))))

  (testing "set-config-tab and set-domain-tab update state"
    (setup/reset-db!)
    (rf/dispatch-sync [:app.admin.frontend.events.settings/set-config-tab :view-options])
    (is (= :view-options (get-in @rf-db/app-db [:admin :settings :config-tab])))
    
    (rf/dispatch-sync [:app.admin.frontend.events.settings/set-domain-tab :expenses])
    (is (= :expenses (get-in @rf-db/app-db [:admin :settings :domain-tab])))))

(deftest settings-load-save-smoke-test
  (testing "load-view-options triggers GET request"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (rf/dispatch-sync [:app.admin.frontend.events.settings/load-view-options])
    
    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/settings" (:uri req)))
      (is (true? (get-in @rf-db/app-db [:admin :settings :loading?]))))))
