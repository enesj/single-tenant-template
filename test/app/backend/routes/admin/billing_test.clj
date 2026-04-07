(ns app.backend.routes.admin.billing-test
  "Tests for admin billing provider-link routes and service behavior."
  (:require
    [app.admin.backend.services.admin.billing :as billing-service]
    [app.backend.test-helpers :as h]
    [app.template.backend.routes.admin.billing :as billing-routes]
    [app.template.backend.security.email :as email-privacy]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]))

(use-fixtures :each h/with-clean-test-state)

(def mock-admin
  {:id (random-uuid)
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(defn- json-link
  [link]
  (update link :id str))

(deftest list-provider-links-handler-test
  (testing "billing list handler returns links with pagination metadata"
    (let [db (h/mock-db)
          handler (billing-routes/list-provider-links-handler db)
          request (h/mock-admin-request :get "/admin/api/billing/provider-links" mock-admin
                    {:params {:account-kind "user"
                              :provider "stripe"
                              :limit "10"
                              :offset "5"}})
          sample-links [{:id (random-uuid)
                         :account-kind "user"
                         :account-ref "User-ABC12345"
                         :provider "stripe"
                         :provider-customer-ref "cus_123"
                         :status "active"}]]
      (with-redefs [billing-service/list-provider-links-page
                    (fn [_db opts]
                      (is (= "user" (:account-kind opts)))
                      (is (= "stripe" (:provider opts)))
                      (is (= 10 (:limit opts)))
                      (is (= 5 (:offset opts)))
                      {:links sample-links
                       :total 1
                       :limit 10
                       :offset 5})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= (mapv json-link sample-links) (:links body)))
          (is (= 1 (:total body)))
          (is (= 10 (:limit body)))
          (is (= 5 (:offset body))))))))

(deftest create-provider-link-handler-test
  (testing "billing create handler parses account id and returns created link"
    (let [db (h/mock-db)
          handler (billing-routes/create-provider-link-handler db)
          account-id (random-uuid)
          created-link {:id (random-uuid)
                        :account-kind "tenant"
                        :account-ref "Tenant-ABC12345"
                        :provider "stripe"
                        :provider-customer-ref "cus_456"
                        :status "active"}
          request (h/mock-admin-request :post "/admin/api/billing/provider-links" mock-admin
                    {:body {:account-kind "tenant"
                            :account-id (str account-id)
                            :provider "stripe"
                            :provider-customer-ref "cus_456"}})]
      (with-redefs [billing-service/create-provider-link!
                    (fn [_db payload admin-id ip-address user-agent]
                      (is (= "tenant" (:account-kind payload)))
                      (is (= account-id (:account-id payload)))
                      (is (= "stripe" (:provider payload)))
                      (is (= "cus_456" (:provider-customer-ref payload)))
                      (is (= (:id mock-admin) admin-id))
                        (is (or (nil? user-agent) (string? user-agent)))
                      (is (nil? ip-address))
                      created-link)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 201 (:status response)))
                  (is (= (json-link created-link) (:link body))))))))

(deftest list-provider-links-service-test
  (testing "service returns pseudonymous account refs without exposing account ids"
    (let [account-id (random-uuid)
          row {:id (random-uuid)
               :account_id account-id
               :account_kind "user"
               :provider "stripe"
               :provider_customer_ref "cus_789"
               :status "active"
               :created_at (java.time.Instant/parse "2026-01-01T00:00:00Z")}
          result (first (with-redefs [hsql/format identity
                                      jdbc/execute! (fn [_db _query _opts] [row])]
                          (billing-service/list-provider-links ::db {:limit 1 :offset 0})))]
      (is (= (email-privacy/user-ref account-id) (:account-ref result)))
      (is (= "user" (:account-kind result)))
      (is (= "stripe" (:provider result)))
      (is (not (contains? result :account-id))))))
