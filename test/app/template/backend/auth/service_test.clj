(ns app.template.backend.auth.service-test
  (:require
    [app.template.backend.auth.email-verification :as email-verification]
    [app.template.backend.auth.protocols :as auth-protocols]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.security.email :as email-privacy]
    [clojure.test :refer [deftest is testing]]))

(deftest process-oauth-callback-flags-verification-email-delivery-failure-test
  (testing "new OAuth signups report failed verification delivery instead of pretending success"
    (let [user-id (java.util.UUID/randomUUID)
          svc {:db :db
               :metadata :metadata
               :password-manager :password-manager
               :email-service :email-service}
          oauth-data {:email "new.user@example.com"
                      :name "New User"
                      :picture "https://example.com/avatar.png"}]
      (with-redefs [db-protocols/find-by-field
                    (fn [_db entity field value]
                      (is (= :users entity))
                      (is (= :email_lookup_hash field))
                      (is (= (email-privacy/email->lookup-hash "new.user@example.com") value))
                      nil)

                    auth-protocols/hash-password
                    (fn [password-manager password]
                      (is (= :password-manager password-manager))
                      (is (= "oauth-google-user" password))
                      "hashed-placeholder")

                    db-protocols/create
                    (fn [_db _metadata entity data]
                      (is (= :users entity))
                      (assoc data :id user-id))

                    email-verification/create-verification-token!
                    (fn [_db uid]
                      (is (= user-id uid))
                      "verification-token")

                    email-verification/send-verification-email
                    (fn [email-service user token]
                      (is (= :email-service email-service))
                      (is (= "new.user@example.com" (:email user)))
                      (is (= "verification-token" token))
                      {:success false
                       :error :gmail-api-error})]
        (let [result (auth-service/process-oauth-callback svc oauth-data :google)]
          (is (= true (:is-new-signup result)))
          (is (= true (:verification-required result)))
          (is (= false (:verification-email-sent? result)))
          (is (= :gmail-api-error (:verification-email-error result)))
          (is (= "new.user@example.com" (get-in result [:user :email]))))))))
