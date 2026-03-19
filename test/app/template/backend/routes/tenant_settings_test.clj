(ns app.template.backend.routes.tenant-settings-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.tenant-settings :as tenant-settings]
    [app.domain.expenses.test-helpers :as th]
    [app.template.backend.services.tenant :as tenant-svc]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- add-membership!
  [db {:keys [tenant-id user-id role]}]
  (jdbc/execute-one!
    db
    ["insert into tenant_memberships (id, tenant_id, user_id, role, status, created_at, updated_at) values (?, ?, ?, ?::membership_role, 'active'::membership_status, now(), now()) returning *"
     (UUID/randomUUID) tenant-id user-id role]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- create-owner-context!
  [db]
  (let [email (str "tenant-owner-" (UUID/randomUUID) "@test.com")
        user (th/ensure-test-user! db {:email email :name "Tenant Owner"})
        {:keys [tenant-id]} (th/ensure-test-tenant! db user)]
    {:user user :tenant-id tenant-id}))

(defn- req
  [{:keys [user tenant-id role body-params]}]
  {:session {:auth-session {:user {:id (:id user)
                                   :email (:email user)
                                   :full_name (or (:full_name user) (:full-name user))}
                            :tenant {:id tenant-id}
                            :membership {:id (UUID/randomUUID)
                                         :role role}}}
   :body-params body-params})

(defn- parse-body [resp]
  (let [body (:body resp)]
    (cond
      (string? body) (json/parse-string body true)
      (map? body) body
      :else body)))

(deftest tenant-settings-handler-updates-email-notifications
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user tenant-id]} (create-owner-context! db)
          handler ((resolve 'app.template.backend.routes.tenant/update-tenant-settings-handler) db)
          response (handler (req {:user user
                                  :tenant-id tenant-id
                                  :role "owner"
                                  :body-params {:email-notifications false}}))
          body (parse-body response)
          persisted (tenant-settings/get-tenant-settings db tenant-id)]
      (is (= 200 (:status response)))
      (is (= false (get-in body [:settings :email-notifications])))
      (is (= false (:email-notifications persisted))))))

(deftest tenant-settings-handler-forbids-viewer
  (when-let [db fixtures/*test-db*]
    (let [{:keys [tenant-id]} (create-owner-context! db)
          viewer (th/ensure-test-user! db {:email (str "tenant-viewer-" (UUID/randomUUID) "@test.com")
                                           :name "Tenant Viewer"})
          _ (add-membership! db {:tenant-id tenant-id :user-id (:id viewer) :role "viewer"})
          handler ((resolve 'app.template.backend.routes.tenant/update-tenant-settings-handler) db)
          response (handler (req {:user viewer
                                  :tenant-id tenant-id
                                  :role "viewer"
                                  :body-params {:email-notifications false}}))]
      (is (= 403 (:status response))))))

(deftest tenant-name-handler-renames-workspace-for-owner-only
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user tenant-id]} (create-owner-context! db)
          another-user (th/ensure-test-user! db {:email (str "tenant-admin-" (UUID/randomUUID) "@test.com")
                                                 :name "Tenant Admin"})
          _ (add-membership! db {:tenant-id tenant-id :user-id (:id another-user) :role "admin"})
          update-name-handler ((resolve 'app.template.backend.routes.tenant/update-tenant-name-handler) db)
          owner-response (update-name-handler (req {:user user
                                                    :tenant-id tenant-id
                                                    :role "owner"
                                                    :body-params {:name "Renamed Workspace"}}))
          owner-body (parse-body owner-response)
          admin-response (update-name-handler (req {:user another-user
                                                    :tenant-id tenant-id
                                                    :role "admin"
                                                    :body-params {:name "Should Not Work"}}))
          tenant (tenant-svc/find-tenant-by-id db tenant-id)
          response-name (or (get-in owner-body [:tenant :name])
                          (get-in owner-body [:tenant :tenants/name]))
          persisted-name (or (:name tenant) (:tenants/name tenant))]
      (is (= 200 (:status owner-response)))
      (is (= "Renamed Workspace" response-name))
      (is (= "Renamed Workspace" persisted-name))
      (is (= 403 (:status admin-response))))))
