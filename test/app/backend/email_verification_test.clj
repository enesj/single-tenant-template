(ns app.backend.email-verification-test
  "Tests for email verification functionality"
  (:require
    [app.template.backend.auth.email-verification :as email-verify]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.email.service :as email-service]
    [clojure.test :refer [deftest is testing]]
    [java-time :as time]))

;; Mock database for testing
(def mock-db (atom {}))

(defn mock-db-protocols [operation & args]
  (case operation
    :execute-query! (let [[query params] args]
                      (cond
                        (re-find #"DELETE FROM email_verification_tokens" query)
                        1

                        (re-find #"INSERT INTO email_verification_tokens" query)
                        {:id (java.util.UUID/randomUUID)
                         :token (second params)}

                        (re-find #"SELECT.*email_verification_tokens.*WHERE.*token" query)
                        [{:id (java.util.UUID/randomUUID)
                          :user_id (java.util.UUID/randomUUID)
                          :tenant_id (java.util.UUID/randomUUID)
                          :token (first params)
                          :expires_at (time/plus (time/instant) (time/hours 1))
                          :attempts 0
                          :used_at nil
                          :email "test@example.com"
                          :full_name "Test User"
                          :tenant_name "Test Tenant"
                          :tenant_slug "test-tenant"}]

                        (re-find #"UPDATE.*email_verification_tokens.*SET used_at" query)
                        1

                        (re-find #"UPDATE.*users.*SET email_verified" query)
                        1

                        :else []))

    :create-item! {:id (java.util.UUID/randomUUID)
                   :token (get (second args) :token)}))

(deftest test-generate-verification-token
  (testing "Token generation"
    (let [token1 (email-verify/generate-verification-token)
          token2 (email-verify/generate-verification-token)]
      (is (string? token1))
      (is (string? token2))
      (is (not= token1 token2))
      (is (>= (count token1) 32)))))

(deftest test-user-needs-verification
  (testing "User verification status check"
    (is (true? (email-verify/user-needs-verification?
                 {:email_verified false :email_verification_status "unverified"})))

    (is (true? (email-verify/user-needs-verification?
                 {:email_verified false :email_verification_status "pending"})))

    (is (false? (email-verify/user-needs-verification?
                  {:email_verified true :email_verification_status "verified"})))

    (is (false? (email-verify/user-needs-verification?
                  {:email_verified false :email_verification_status "verified"})))))

(deftest test-create-verification-token
  (testing "Creating verification token"
    (with-redefs [db-protocols/execute! mock-db-protocols
                  db-protocols/create mock-db-protocols]
      (let [tenant-id (java.util.UUID/randomUUID)
            user-id (java.util.UUID/randomUUID)
            token (email-verify/create-verification-token! mock-db tenant-id user-id)]
        (is (string? token))
        (is (>= (count token) 32))))))

(deftest test-verify-email-token
  (testing "Token verification - valid token"
    (with-redefs [db-protocols/execute! mock-db-protocols]
      (let [result (email-verify/verify-email-token! mock-db "valid-token")]
        (is (:success result))
        (is (= "Email successfully verified" (:message result))))))

  (testing "Token verification - invalid token"
    (with-redefs [db-protocols/execute!
                  (fn [_db query _params]
                    (if (re-find #"SELECT.*email_verification_tokens" query)
                      []  ; No token found
                      1))]
      (let [result (email-verify/verify-email-token! mock-db "invalid-token")]
        (is (false? (:success result)))
        (is (= :token-not-found (:error result)))))))

(deftest test-email-service
  (testing "Email service creation"
    ;; Test that we can create email services without error
    (let [postmark-service (email-service/create-email-service
                             {:type :postmark
                              :postmark {:api-key "test-key" :from-email "test@example.com"}})
          gmail-service (email-service/create-email-service
                          {:type :gmail-smtp
                           :smtp {:host "smtp.gmail.com"}
                           :from-email "test@example.com"})]
      (is (not (nil? postmark-service)))
      (is (not (nil? gmail-service))))))

(deftest test-integration-flow
  (testing "Token creation and verification flow"
    (with-redefs [db-protocols/execute! mock-db-protocols
                  db-protocols/create mock-db-protocols]
      (let [tenant-id (java.util.UUID/randomUUID)
            user-id (java.util.UUID/randomUUID)
            token (email-verify/create-verification-token! mock-db tenant-id user-id)
            verify-result (email-verify/verify-email-token! mock-db token)]
        (is (string? token))
        (is (:success verify-result))
        (is (= "Email successfully verified" (:message verify-result)))))))

(comment
  ;; Run tests manually
  (clojure.test/run-tests 'app.backend.email-verification-test))
