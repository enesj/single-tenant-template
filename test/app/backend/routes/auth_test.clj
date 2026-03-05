(ns app.backend.routes.auth-test
  (:require
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.auth :as routes.auth]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest auth-status-auth-session-test
  (testing "auth-session marks user authenticated"
    (let [req {:session {:auth-session {:user {:id "user-1"
                                               :role "admin"
                                               :auth_provider "github"}}}}
          resp (routes.auth/auth-status-handler req)
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= true (:authenticated body)))
      (is (= "github" (:provider body)))
      (is (= "admin" (get-in body [:user :role])))))

  (testing "missing auth-session returns unauthenticated"
    (let [resp (routes.auth/auth-status-handler {})
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= false (:authenticated body))))))

(deftest auth-status-clears-unverified-incomplete-session-test
  (testing "bare pending OAuth sessions are cleared until verification completes"
    (let [req {:session {:auth-session {:user {:id "user-1"
                                               :email "user@example.com"}}}
               :service-container {:db :mock-db}}
          pending-user {:id "user-1"
                        :email "user@example.com"
                        :status "active"
                        :email_verified false}
          parse-body #(json/parse-string (:body %) true)]
      (with-redefs-fn {#'routes.auth/resolve-db (fn [_] :mock-db)
                       #'routes.auth/invalid-auth-session-message (fn [& _] nil)
                       #'routes.auth/fetch-user-record (fn [_ _] pending-user)}
        (fn []
          (let [resp (routes.auth/auth-status-handler req)
                body (parse-body resp)]
            (is (= 200 (:status resp)))
            (is (= false (:authenticated body)))
            (is (= true (:verification-required body)))
            (is (nil? (get-in resp [:session :auth-session])))))))))

(deftest auth-status-repairs-verified-incomplete-session-test
  (testing "bare sessions are rebuilt after verification creates tenant memberships"
    (let [req {:session {:admin-token "admin-token-1"
                         :auth-session {:user {:id "user-1"
                                               :email "user@example.com"}}}
               :service-container {:db :mock-db
                                   :config {:tenant-defaults {}}}}
          verified-user {:id "user-1"
                         :email "user@example.com"
                         :full_name "User Example"
                         :status "active"
                         :email_verified true}
          parse-body #(json/parse-string (:body %) true)]
      (with-redefs-fn {#'routes.auth/resolve-db (fn [_] :mock-db)
                       #'routes.auth/invalid-auth-session-message (fn [& _] nil)
                       #'routes.auth/fetch-user-record (fn [_ _] verified-user)
                       #'tenant-auth/resolve-tenant-context
                       (fn [_db _config user]
                         {:action :auto-set
                          :tenant {:id "tenant-1"
                                   :name "Acme"}
                          :membership {:id "membership-1"
                                       :role "owner"}
                          :user user})}
        (fn []
          (let [resp (routes.auth/auth-status-handler req)
                body (parse-body resp)]
            (is (= 200 (:status resp)))
            (is (= true (:authenticated body)))
            (is (= "owner" (:membership-role body)))
            (is (= "tenant-1" (get-in body [:tenant :id])))
            (is (= "owner" (get-in resp [:session :auth-session :membership :role])))
            (is (= "tenant-1" (get-in resp [:session :auth-session :tenant :id])))
            (is (= "admin-token-1" (get-in resp [:session :admin-token])))))))))
