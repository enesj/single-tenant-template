(ns app.backend.routes.admin.password-test
  "Tests for admin password management routes.
   
   Tests cover:
   - Forgot password (request reset)
   - Verify reset token
   - Reset password with token
   - Change password (authenticated)"
  (:require
    [app.backend.routes.admin.password :as password]
    [app.template.backend.auth.password-reset :as pwd-reset]
    [app.backend.test-helpers :as h]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

;; ============================================================================
;; Test Data
;; ============================================================================

(def test-admin-id (h/random-uuid))
(def test-token "valid-reset-token-12345")

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"
   :status "active"})

(def mock-email-service
  {:smtp-config {:host "smtp.test.com" :port 587}
   :from-email "noreply@test.com"})

;; ============================================================================
;; Forgot Password Tests
;; ============================================================================

(deftest forgot-password-handler-test
  (testing "forgot-password sends reset email for valid admin"
    (let [db (h/mock-db)
          handler (password/forgot-password-handler db mock-email-service "http://localhost:8085")
          request {:body {:email "admin@example.com"}}]
      (with-redefs [pwd-reset/request-password-reset! 
                    (fn [_db email principal-type _send-fn _base-url]
                      (is (= "admin@example.com" email))
                      (is (= :admin principal-type))
                      {:success true :message "If an account exists, a reset email has been sent"})]
        (let [response (handler request)
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "forgot-password fails without email"
    (let [db (h/mock-db)
          handler (password/forgot-password-handler db mock-email-service "http://localhost:8085")
              request {:body {}}
              response (handler request)
              body (json/parse-string (:body response) true)]
        (is (= 400 (:status response)))
        (is (= "Email is required" (:error body)))))
  
  (testing "forgot-password fails with empty email"
    (let [db (h/mock-db)
          handler (password/forgot-password-handler db mock-email-service "http://localhost:8085")
                              request {:body {:email ""}}
                              response (handler request)]
                  (is (= 400 (:status response))))))

;; ============================================================================
;; Verify Reset Token Tests
;; ============================================================================

(deftest verify-reset-token-handler-test
  (testing "verify-token returns valid for good token"
    (let [db (h/mock-db)
          handler (password/verify-reset-token-handler db)
          request {:query-params {"token" test-token}}]
      (with-redefs [pwd-reset/verify-reset-token 
                    (fn [_db token]
                      (is (= test-token token))
                      {:valid? true})]
        (let [response (handler request)
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (true? (:valid body)))))))
  
  (testing "verify-token returns invalid for expired token"
    (let [db (h/mock-db)
          handler (password/verify-reset-token-handler db)
          request {:query-params {"token" "expired-token"}}]
      (with-redefs [pwd-reset/verify-reset-token 
                    (constantly {:valid? false :error "Token has expired"})]
        (let [response (handler request)
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (false? (:valid body)))
          (is (= "Token has expired" (:error body)))))))
  
  (testing "verify-token fails without token"
    (let [db (h/mock-db)
          handler (password/verify-reset-token-handler db)
              request {:query-params {}}
              response (handler request)
              body (json/parse-string (:body response) true)]
        (is (= 400 (:status response)))
        (is (= "Token is required" (:error body))))))

;; ============================================================================
;; Reset Password Tests
;; ============================================================================

(deftest reset-password-handler-test
  (testing "reset-password succeeds with valid token and password"
    (let [db (h/mock-db)
          handler (password/reset-password-handler db nil "http://localhost:8085")
          request {:body {:token test-token :password "newpassword123"}}]
      (with-redefs [pwd-reset/reset-password! 
                    (fn [_db token _password]
                      (is (= test-token token))
                      {:success true :principal-id test-admin-id})]
        (let [response (handler request)
              body (json/parse-string (:body response) true)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "reset-password fails without token"
      (let [db (h/mock-db)
              handler (password/reset-password-handler db nil "http://localhost:8085")
              request {:body {:password "newpassword123"}}
              response (handler request)
              body (json/parse-string (:body response) true)]
        (is (= 400 (:status response)))
        (is (= "Reset token is required" (:error body)))))
  
  (testing "reset-password fails without password"
      (let [db (h/mock-db)
              handler (password/reset-password-handler db nil "http://localhost:8085")
              request {:body {:token test-token}}
              response (handler request)
              body (json/parse-string (:body response) true)]
        (is (= 400 (:status response)))
        (is (= "New password is required" (:error body)))))
  
  (testing "reset-password fails with invalid token"
    (let [db (h/mock-db)
          handler (password/reset-password-handler db nil "http://localhost:8085")
          request {:body {:token "invalid" :password "newpassword123"}}]
      (with-redefs [pwd-reset/reset-password! 
                    (constantly {:success false :error "Invalid or expired token"})]
        (let [response (handler request)
              body (json/parse-string (:body response) true)]
          (is (= 400 (:status response)))
          (is (= "Invalid or expired token" (:error body))))))))
