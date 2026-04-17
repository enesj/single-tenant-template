(ns app.admin.frontend.adapters.admins-test
  (:require
    [app.admin.frontend.adapters.admins :as admins-adapter]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest sync-admins-to-template-normalizes-entities
  (testing "sync event loads normalized admins into template entity store"
    (setup/reset-db!)
    (rf/dispatch-sync [::admins-adapter/sync-admins-to-template
                       [{:id 1 :email "owner@example.com"}
                        {:admins/id "uuid-2" :email "admin@example.com"}]])
    (let [db @rf-db/app-db
          data (get-in db (paths/entity-data :admins))
          ids (get-in db (paths/entity-ids :admins))]
      (is (= ["1" "uuid-2"] ids))
      (is (= "owner@example.com" (get-in data ["1" :email])))
      (is (= "admin@example.com" (get-in data ["uuid-2" :email]))))))

(deftest initialize-admins-adapter-sets-server-list-ui
  (testing "initialize event enables server pagination and refresh for admins"
    (setup/reset-db!)
    (rf/dispatch-sync [::admins-adapter/initialize-admins-adapter-with-config])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :admins)]
      (is (= 1 (get-in db (conj base :pagination :current-page))))
      (is (nil? (get-in db (conj base :pagination :per-page)))
        "per-page should be left unset so list-view can seed it from configured defaults")
      (is (= :server (get-in db (paths/list-pagination-mode :admins))))
      (is (= [:admin/load-admins] (get-in db (paths/list-refresh-event :admins)))))))

(deftest initialize-admins-adapter-migrates-legacy-email-visible-prefs
  (testing "initialize event upgrades legacy email-masked column prefs to email"
    (setup/reset-db!)
    (swap! rf-db/app-db assoc-in [:ui :entity-prefs :admin/admins :columns :visible-order]
      [:admin-ref :email-masked :role])
    (swap! rf-db/app-db assoc-in [:ui :entity-prefs :admin/admins :columns :order]
      [:admin-ref :email-masked :role])
    (swap! rf-db/app-db assoc-in [:ui :entity-prefs :admin/admins :columns :visible]
      {:admin-ref true :email-masked true :role true})

    (rf/dispatch-sync [::admins-adapter/initialize-admins-adapter-with-config])

    (let [prefs (get-in @rf-db/app-db [:ui :entity-prefs :admin/admins :columns])]
      (is (= [:admin-ref :email :role] (:visible-order prefs)))
      (is (= [:admin-ref :email :role] (:order prefs)))
      (is (= {:admin-ref true :email true :role true} (:visible prefs))))))
