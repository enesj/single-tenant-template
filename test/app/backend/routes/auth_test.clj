(ns app.backend.routes.auth-test
  (:require
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.services.onboarding.core :as onboarding]
    [app.template.backend.routes.auth :as routes.auth]
    [app.template.backend.routes.oauth :as routes.oauth]
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

(deftest auth-status-repair-sanitizes-generic-java-values-test
  (testing "repair path uses the shared serializer fallback for generic java values"
    (let [portal-uri (java.net.URI. "https://example.com/tenant")
          req {:session {:auth-session {:user {:id "user-1"
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
                       #'tenant-auth/resolve-tenant-context (fn [_db _config user]
                                                              {:action :auto-set
                                                               :user user})
                       #'tenant-auth/build-auth-session (fn [{:keys [user]} _tenant-ctx]
                                                          {:user user
                                                           :tenant {:id "tenant-1"
                                                                    :portal-uri portal-uri}
                                                           :membership {:id "membership-1"
                                                                        :role "owner"}})}
        (fn []
          (let [resp (routes.auth/auth-status-handler req)
                body (parse-body resp)]
            (is (= 200 (:status resp)))
            (is (= "https://example.com/tenant"
                  (get-in body [:tenant :portal-uri])))
            (is (= "https://example.com/tenant"
                  (get-in resp [:session :auth-session :tenant :portal-uri])))))))))

(deftest auth-status-initializes-missing-onboarding-summary-test
  (testing "verified users with active memberships get onboarding initialized on demand"
    (let [req {:session {:auth-session {:user {:id "user-1"
                                               :email "user@example.com"}
                                        :tenant {:id "tenant-1"}
                                        :membership {:role "owner"}}}
               :service-container {:db :mock-db}}
          parse-body #(json/parse-string (:body %) true)
          summary {:active? true
                   :completed 0
                   :total 7
                   :dismissed false
                   :redirect-to-onboarding? true}
          init-calls (atom 0)]
      (with-redefs-fn {#'routes.auth/invalid-auth-session-message (fn [& _] nil)
                       #'onboarding/get-progress-summary
                       (let [calls (atom 0)]
                         (fn [_db user-id role]
                           (swap! calls inc)
                           (is (= "user-1" user-id))
                           (is (= "owner" role))
                           (when (> @calls 1)
                             summary)))
                       #'onboarding/initialise-delta-onboarding!
                       (fn [_db user-id role]
                         (swap! init-calls inc)
                         (is (= "user-1" user-id))
                         (is (= "owner" role))
                         :created)}
        (fn []
          (let [resp (routes.auth/auth-status-handler req)
                body (parse-body resp)]
            (is (= 200 (:status resp)))
            (is (= 1 @init-calls))
            (is (= true (:authenticated body)))
            (is (= true (get-in body [:onboarding :redirect-to-onboarding?])))))))))

(deftest oauth-callback-does-not-claim-email-was-sent-when-delivery-fails-test
  (testing "new OAuth signups redirect to an error page when verification delivery fails"
    (let [handler (app.template.backend.routes.oauth/oauth-callback-handler :auth-service nil {:oauth {:google {:redirect-uri "https://example.com/oauth/google/callback"}}})
          req {:uri "/oauth/google/callback"
               :query-params {"code" "oauth-code"}}]
      (with-redefs [app.template.backend.routes.oauth/exchange-code-for-token
                    (fn [_oauth-configs provider code redirect-uri _db]
                      (is (= :google provider))
                      (is (= "oauth-code" code))
                      (is (= "https://example.com/oauth/google/callback" redirect-uri))
                      {:access_token "access-token"})

                    app.template.backend.routes.oauth/fetch-google-user-info
                    (fn [access-token]
                      (is (= "access-token" access-token))
                      {:email "new.user@example.com"
                       :name "New User"})

                    app.template.backend.auth.service/process-oauth-callback
                    (fn [_auth-service user-info provider]
                      (is (= :google provider))
                      (is (= "new.user@example.com" (:email user-info)))
                      {:user {:id "user-1"
                              :email "new.user@example.com"
                              :full_name "New User"}
                       :is-new-signup true
                       :verification-required true
                       :verification-email-sent? false
                       :verification-email-error :gmail-api-error})]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/email-verified?error=email-send-failed"
                (get-in resp [:headers "Location"])))
          (is (= "new.user@example.com"
                (get-in resp [:session :auth-session :user :email]))))))))

(deftest oauth-callback-existing-user-prefers-onboarding-redirect-test
  (testing "existing OAuth users go to onboarding when the summary says they should"
    (let [handler (routes.oauth/oauth-callback-handler :auth-service :mock-db {:oauth {:google {:redirect-uri "https://example.com/oauth/google/callback"}}})
          req {:uri "/oauth/google/callback"
               :query-params {"code" "oauth-code"}
               :service-container {:db :mock-db}}]
      (with-redefs [routes.oauth/exchange-code-for-token
                    (fn [_oauth-configs provider code redirect-uri _db]
                      (is (= :google provider))
                      (is (= "oauth-code" code))
                      (is (= "https://example.com/oauth/google/callback" redirect-uri))
                      {:access_token "access-token"})

                    routes.oauth/fetch-google-user-info
                    (fn [access-token]
                      (is (= "access-token" access-token))
                      {:email "existing.user@example.com"
                       :name "Existing User"})

                    auth-service/process-oauth-callback
                    (fn [_auth-service user-info provider]
                      (is (= :google provider))
                      (is (= "existing.user@example.com" (:email user-info)))
                      {:user {:id "user-1"
                              :email "existing.user@example.com"
                              :full_name "Existing User"}
                       :is-new-signup false
                       :verification-required false})

                    tenant-auth/resolve-tenant-context
                    (fn [_db _config user _opts]
                      (is (= "existing.user@example.com" (:email user)))
                      {:action :auto-set
                       :tenant {:id "tenant-1" :name "Acme"}
                       :membership {:id "membership-1" :role "owner"}})

                    onboarding/get-progress-summary
                    (fn [_db user-id role]
                      (is (= "user-1" user-id))
                      (is (= "owner" role))
                      {:active? true
                       :completed 0
                       :total 7
                       :dismissed false
                       :redirect-to-onboarding? true})]
        (let [resp (handler req)]
          (is (= 302 (:status resp)))
          (is (= "/onboarding" (get-in resp [:headers "Location"])))
          (is (= "existing.user@example.com"
                (get-in resp [:session :auth-session :user :email]))))))))

(deftest oauth-callback-sanitizes-generic-java-values-in-session-test
  (testing "oauth callback uses the shared serializer fallback for auth-session data"
    (let [portal-uri (java.net.URI. "https://example.com/tenant")
          handler (routes.oauth/oauth-callback-handler
                    :auth-service
                    :mock-db
                    {:oauth {:google {:redirect-uri "https://example.com/oauth/google/callback"}}})
          req {:uri "/oauth/google/callback"
               :query-params {"code" "oauth-code"}
               :service-container {:db :mock-db}}]
      (with-redefs-fn {#'routes.oauth/exchange-code-for-token
                       (fn [_oauth-configs provider code redirect-uri _db]
                         (is (= :google provider))
                         (is (= "oauth-code" code))
                         (is (= "https://example.com/oauth/google/callback" redirect-uri))
                         {:access_token "access-token"})
                       #'routes.oauth/fetch-google-user-info
                       (fn [access-token]
                         (is (= "access-token" access-token))
                         {:email "existing.user@example.com"
                          :name "Existing User"})
                       #'auth-service/process-oauth-callback
                       (fn [_auth-service user-info provider]
                         (is (= :google provider))
                         (is (= "existing.user@example.com" (:email user-info)))
                         {:user {:id "user-1"
                                 :email "existing.user@example.com"
                                 :full_name "Existing User"}
                          :is-new-signup false
                          :verification-required false})
                       #'tenant-auth/resolve-tenant-context
                       (fn [_db _config user _opts]
                         (is (= "existing.user@example.com" (:email user)))
                         {:action :auto-set
                          :user user})
                       #'tenant-auth/build-auth-session
                       (fn [{:keys [user]} _tenant-ctx]
                         {:user user
                          :tenant {:id "tenant-1"
                                   :portal-uri portal-uri}
                          :membership {:id "membership-1"
                                       :role "owner"}})
                       #'routes.oauth/post-login-redirect-url
                       (fn [_db _tenant-ctx _auth-session]
                         "/expenses")}
        (fn []
          (let [resp (handler req)]
            (is (= 302 (:status resp)))
            (is (= "/expenses" (get-in resp [:headers "Location"])))
            (is (= "https://example.com/tenant"
                  (get-in resp [:session :auth-session :tenant :portal-uri])))))))))
