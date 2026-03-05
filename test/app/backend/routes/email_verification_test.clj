(ns app.backend.routes.email-verification-test
  (:require
    [app.template.backend.auth.email-verification :as email-verify]
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
