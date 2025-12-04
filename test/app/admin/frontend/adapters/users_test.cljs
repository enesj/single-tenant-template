(ns app.admin.frontend.adapters.users-test
  (:require
    [app.admin.frontend.adapters.users :as users-adapter]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest sync-users-to-template-normalizes-entities
  (testing "sync event loads normalized users into template entity store"
    (setup/reset-db!)
    (rf/dispatch-sync [::users-adapter/sync-users-to-template
                       [{:id 1 :email "owner@example.com"}
                        {:users/id "uuid-2" :email "member@example.com"}]])
    (let [db @rf-db/app-db
          data (get-in db (paths/entity-data :users))
          ids (get-in db (paths/entity-ids :users))]
      (is (= ["1" "uuid-2"] ids))
      (is (= "owner@example.com" (get-in data ["1" :email])))
      (is (= "member@example.com" (get-in data ["uuid-2" :email]))))))

(deftest crud-create-bridges-to-admin-endpoint-when-token-present
  (testing "template create event routes through admin adapter when token present"
    (setup/reset-db!)
    (setup/install-http-stub!)

    (users-adapter/init-users-adapter!)
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/create-entity
                       :users
                       {:email "new@example.com"}])

    (let [req (setup/last-http-request)]
      (is (= :post (:method req)))
      (is (= "/admin/api/users" (:uri req)))
      (is (= {:email "new@example.com"} (:params req)))
      (is (= [:app.template.frontend.events.list.crud/create-success :users]
            (take 2 (:on-success req))))
      (is (= [:app.template.frontend.events.list.crud/create-failure :users]
            (take 2 (:on-failure req)))))))

(deftest initialize-users-adapter-sets-default-ui
  (testing "initialize event seeds list pagination defaults"
    (setup/reset-db!)
    (rf/dispatch-sync [::users-adapter/initialize-users-adapter-with-config])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :users)]
      (is (= 1 (get-in db (conj base :pagination :current-page))))
      (is (= 10 (get-in db (conj base :pagination :per-page)))))))
