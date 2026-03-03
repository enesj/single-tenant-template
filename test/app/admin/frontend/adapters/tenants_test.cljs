(ns app.admin.frontend.adapters.tenants-test
  (:require
    [app.admin.frontend.adapters.tenants]
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- enter-admin-route! []
  (swap! rf-db/app-db assoc :current-route {:data {:name :admin/tenants}}))

(deftest crud-batch-delete-bridges-to-admin-tenants-endpoint
  (testing "template batch delete routes tenant deletes through the admin endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (enter-admin-route!)

    (rf/dispatch-sync
      [:app.template.frontend.events.list.crud/batch-delete
       :tenants
       ["tenant-1" "tenant-2"]])

    (let [req (setup/last-http-request)]
      (is (= :delete (:method req)))
      (is (= "/admin/api/tenants/batch" (:uri req)))
      (is (= {:ids ["tenant-1" "tenant-2"]} (:params req)))
      (is (= [:app.template.frontend.events.list.crud/batch-delete-success
              :tenants
              ["tenant-1" "tenant-2"]]
            (take 3 (:on-success req))))
      (is (= [:app.template.frontend.events.list.crud/batch-delete-failure
              :tenants
              ["tenant-1" "tenant-2"]]
            (take 3 (:on-failure req)))))))

(deftest crud-update-bridges-to-admin-tenant-endpoint
  (testing "template update routes tenant edits through the admin endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (enter-admin-route!)

    (rf/dispatch-sync
      [:app.template.frontend.events.list.crud/update-entity
       :tenants
       "tenant-42"
       {:name "Updated Tenant"
        :slug "updated-tenant"
        :status "active"}])

    (let [req (setup/last-http-request)]
      (is (= :put (:method req)))
      (is (= "/admin/api/tenants/tenant-42" (:uri req)))
      (is (= {:name "Updated Tenant"
              :slug "updated-tenant"
              :status "active"}
            (:params req)))
      (is (= [:app.template.frontend.events.list.crud/update-success
              :tenants
              "tenant-42"]
            (take 3 (:on-success req))))
      (is (= [:app.template.frontend.events.list.crud/update-failure
              :tenants]
            (take 2 (:on-failure req)))))))
