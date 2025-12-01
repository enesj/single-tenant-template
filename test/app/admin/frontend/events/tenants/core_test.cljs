(ns app.admin.frontend.events.tenants.core-test
  (:require
    [app.admin.frontend.events.tenants.core]
    [app.admin.frontend.events.users.template.messages]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Ensure admin session subscriptions are registered for tests that rely on them
(when-not (get-in @rf-db/app-db [:test :admin-session-registered?])
  (swap! rf-db/app-db assoc-in [:test :admin-session-registered?] true))

(deftest delete-tenant-dispatches-admin-entity-request
  (testing ":admin/delete-tenant issues admin entity delete request and resets state on success"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [tenant-id "aaaa1111-bbbb-2222-cccc-333333333333"]
      (swap! rf-db/app-db
        (fn [db]
          (-> db
            (assoc :admin/tenants [{:tenants/id tenant-id :name "Tenant X"}])
            (assoc-in (paths/entity-data :tenants) {tenant-id {:id tenant-id :name "Tenant X"}})
            (assoc-in (paths/entity-ids :tenants) [tenant-id])
            (assoc-in (paths/entity-selected-ids :tenants) #{tenant-id}))))

      (rf/dispatch-sync [:admin/delete-tenant tenant-id])

      (let [delete-req (setup/last-http-request)]
        (is (= :delete (:method delete-req)))
        (is (= (str "/admin/api/entities/tenants/" tenant-id) (:uri delete-req)))
        (is (= [:admin/delete-tenant-success tenant-id] (:on-success delete-req)))
        (is (= [:admin/delete-tenant-failure tenant-id] (:on-failure delete-req))))

      (setup/respond-success! {:success true :message "Deleted"})

      (let [db @rf-db/app-db]
        (is (= [] (:admin/tenants db)))
        (is (= {} (get-in db (paths/entity-data :tenants))))
        (is (= [] (get-in db (paths/entity-ids :tenants))))
        (is (or (nil? (get-in db (paths/entity-selected-ids :tenants)))
              (empty? (get-in db (paths/entity-selected-ids :tenants)))))
        (is (not (get db :admin/updating-tenant)))
        (is (nil? (:admin/tenant-update-error db)))))))

(deftest delete-tenant-failure-surface-error
  (testing ":admin/delete-tenant-failure clears loading state and records error message"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [tenant-id "ffff0000-eeee-4444-dddd-999999999999"]
      (swap! rf-db/app-db assoc :admin/tenants [{:tenants/id tenant-id :name "Fail Tenant"}])

      (rf/dispatch-sync [:admin/delete-tenant tenant-id])

      (setup/respond-failure! {:status 500 :response {:message "Cannot delete"}})

      (let [db @rf-db/app-db]
        (is (not (get db :admin/updating-tenant)))
        (is (= "Cannot delete" (:admin/tenant-update-error db)))
        (is (= [{:tenants/id tenant-id :name "Fail Tenant"}] (:admin/tenants db)))))))
