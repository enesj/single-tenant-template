(ns app.backend.routes.session-isolation-test
  "Regression tests for mixing user + admin sessions in the same browser.

  The app uses a single Ring session cookie. If login handlers overwrite the
  session map instead of merging, logging in on another tab (e.g. admin UI)
  can unintentionally wipe the existing user session, and vice versa."
  (:require
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.admin.auth :as admin-auth-routes]
    [app.template.backend.routes.admin.utils :as admin-utils]
    [app.template.backend.routes.auth :as user-auth-routes]
    [app.template.backend.routes.utils :as routes-utils]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(deftest admin-login-preserves-existing-user-session
  (testing "Admin login merges :admin-token into existing session"
    (let [admin-id (UUID/randomUUID)
          handler (admin-auth-routes/login-handler :mock-db)
          req {:body {:email "admin@example.com" :password "pw"}
               :headers {"user-agent" "ua"}
               :remote-addr "127.0.0.1"
               :session {:auth-session {:user {:id "user-1"}}}}]
      (with-redefs [admin-auth/authenticate-admin (fn [_db _email _password]
                                                    {:id admin-id
                                                     :email "admin@example.com"
                                                     :full_name "Admin"
                                                     :role "admin"})
                    admin-auth/create-admin-session! (fn [_db _admin-id _ip _ua]
                                                       {:token "admin-token-1"})
                    login-monitoring/record-login-event! (fn [& _] nil)
                    admin-utils/log-admin-action (fn [& _] nil)]
        (let [resp (handler req)]
          (is (= "admin-token-1" (get-in resp [:session :admin-token])))
          (is (= {:user {:id "user-1"}}
                (get-in resp [:session :auth-session]))))))))

(deftest user-login-preserves-existing-admin-session
  (testing "User login merges :auth-session into existing session"
    (let [login (user-auth-routes/login-handler :mock-auth-service)
          req {:body-params {:email "user@example.com" :password "pw"}
               :headers {"user-agent" "ua"}
               :remote-addr "127.0.0.1"
               :session {:admin-token "admin-token-1"}}]
      (with-redefs [routes-utils/get-service-container (fn [_req] {:db :mock-db})
                    auth-service/login-with-password (fn [_svc _params]
                                                       {:user {:id "user-1"
                                                               :email "user@example.com"}})
                    tenant-auth/resolve-tenant-context (fn [& _] {:action :auto-set})
                    tenant-auth/build-auth-session (fn [base _ctx] base)
                    login-monitoring/record-login-event! (fn [& _] nil)]
        (let [resp (login req)]
          (is (= "admin-token-1" (get-in resp [:session :admin-token])))
          (is (= {:user {:id "user-1" :email "user@example.com"}}
                (get-in resp [:session :auth-session]))))))))

(deftest admin-logout-does-not-wipe-user-session
  (testing "Admin logout removes only :admin-token"
    (let [handler (admin-auth-routes/logout-handler :mock-db)
          req {:session {:auth-session {:user {:id "user-1"}}
                         :admin-token "admin-token-1"}
               :headers {}
               :admin {:id (UUID/randomUUID)}}]
      (with-redefs [admin-auth/invalidate-session! (fn [& _] nil)
                    admin-utils/log-admin-action (fn [& _] nil)]
        (let [resp (handler req)]
          (is (nil? (get-in resp [:session :admin-token])))
          (is (= {:user {:id "user-1"}}
                (get-in resp [:session :auth-session]))))))))

(deftest user-logout-clears-entire-session
  (testing "User logout clears the full session, including any admin token"
    (let [req {:session {:auth-session {:user {:id "user-1"}}
                         :admin-token "admin-token-1"}}
          resp (user-auth-routes/logout-handler req)]
      (is (nil? (:session resp)))
      (is (nil? (get-in resp [:session :auth-session])))
      (is (nil? (get-in resp [:session :admin-token]))))))
