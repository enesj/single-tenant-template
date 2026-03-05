(ns app.backend.routes.email-verification-test
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.template.backend.auth.email-verification :as email-verify]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.email-verification :as routes.email-verification]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.test :refer [deftest is testing]]))

(deftest verify-email-handler-uses-underlying-connection-test
  (testing "verify-email handler unwraps :connection for tenant lookups/provisioning"
    (let [db-adapter {:connection :db-conn}
          user-id    (java.util.UUID/randomUUID)
          handler    (routes.email-verification/verify-email-handler db-adapter nil {:tenant-defaults {}})
          req        {:query-params {"token" "tok"}}]
      (with-redefs [email-verify/verify-email-token!
                    (fn [db token]
                      (is (= db-adapter db))
                      (is (= "tok" token))
                      {:success true :user-id user-id :email "a@example.com"})

                    tenant-svc/get-user-memberships
                    (fn [db user-id*]
                      (is (= :db-conn db))
                      (is (= user-id user-id*))
                      [])

                    tenant-svc/provision-tenant!
                    (fn [db _config user]
                      (is (= :db-conn db))
                      (is (= user-id (:id user)))
                      {:tenant {} :membership {}})]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/email-verified?success=true" (get-in resp [:headers "Location"]))))))))

(deftest verify-email-handler-is-idempotent-on-used-token-test
  (testing "already-used tokens still ensure a workspace exists (no 500, safe retry)"
    (let [db-adapter   {:connection :db-conn}
          user-id      (java.util.UUID/randomUUID)
          email-service :email-service
          handler      (routes.email-verification/verify-email-handler db-adapter email-service {:tenant-defaults {}})
          req          {:query-params {"token" "tok"}}]
      (with-redefs [email-verify/verify-email-token!
                    (fn [_db _token]
                      {:success false
                       :error :token-already-used
                       :user-id user-id
                       :email "a@example.com"})

                    tenant-svc/get-user-memberships
                    (fn [db user-id*]
                      (is (= :db-conn db))
                      (is (= user-id user-id*))
                      [])

                    tenant-svc/provision-tenant!
                    (fn [db _config user]
                      (is (= :db-conn db))
                      (is (= user-id (:id user)))
                      {:tenant {} :membership {}})

                    email-verify/send-verification-success-email
                    (fn [& _]
                      (is false "Should not send success email on used token"))]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/email-verified?success=true" (get-in resp [:headers "Location"]))))))))

(deftest verify-email-handler-updates-current-session-test
  (testing "logged-in user gets a tenant-aware session after verification"
    (let [db-adapter {:connection :db-conn}
          user-id    (java.util.UUID/randomUUID)
          handler    (routes.email-verification/verify-email-handler db-adapter nil {:tenant-defaults {}})
          req        {:query-params {"token" "tok"}
                      :session {:auth-session {:user {:id (str user-id)
                                                      :email "old@example.com"}}}}]
      (with-redefs [email-verify/verify-email-token!
                    (fn [_db _token]
                      {:success true
                       :user-id user-id
                       :email "new@example.com"
                       :full_name "Test User"})

                    tenant-svc/get-user-memberships
                    (fn [_db user-id*]
                      (is (= user-id user-id*))
                      [])

                    tenant-svc/provision-tenant!
                    (fn [_db _config user]
                      (is (= "new@example.com" (:email user)))
                      {:tenant {:id "tenant-1"} :membership {:id "membership-1"}})

                    tenant-auth/resolve-tenant-context
                    (fn [_db _config user]
                      (is (= "new@example.com" (:email user)))
                      (is (= true (:email_verified user)))
                      {:action :auto-set
                       :tenant {:id "tenant-1" :name "Tenant 1"}
                       :membership {:id "membership-1" :role "owner"}})

                    domain-registry/get-post-login-path
                    (fn [] "/dashboard")]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/dashboard" (get-in resp [:headers "Location"])))
          (is (= "new@example.com" (get-in resp [:session :auth-session :user :email])))
          (is (= true (get-in resp [:session :auth-session :user :email_verified])))
          (is (= "tenant-1" (get-in resp [:session :auth-session :tenant :id])))
          (is (= "owner" (get-in resp [:session :auth-session :membership :role]))))))))

(deftest verify-email-handler-fails-cleanly-when-email-missing-test
  (testing "verification does not falsely succeed when provisioning data is incomplete"
    (let [db-adapter {:connection :db-conn}
          user-id    (java.util.UUID/randomUUID)
          handler    (routes.email-verification/verify-email-handler db-adapter nil {:tenant-defaults {}})
          req        {:query-params {"token" "tok"}}]
      (with-redefs [email-verify/verify-email-token!
                    (fn [_db _token]
                      {:success true
                       :user-id user-id
                       :email nil})

                    tenant-svc/get-user-memberships
                    (fn [_db user-id*]
                      (is (= user-id user-id*))
                      [])]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/email-verified?error=workspace-provisioning-failed"
                (get-in resp [:headers "Location"]))))))))
