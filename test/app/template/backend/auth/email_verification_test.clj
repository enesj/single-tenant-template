(ns app.template.backend.auth.email-verification-test
  (:require
    [app.template.backend.auth.email-verification :as email-verification]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.security.tokens :as token-security]
    [clojure.test :refer [deftest is testing]]))

(deftest find-verification-token-normalizes-joined-user-fields
  (testing "joined user columns are exposed under stable plain keys"
    (let [user-id (java.util.UUID/randomUUID)
          row     {:email_verification_tokens/user_id user-id
                   :email_verification_tokens/used_at nil
                   :email_verification_tokens/attempts 0
                   :users/verification_email "user@example.com"
                   :users/verification_full_name "Test User"}]
      (with-redefs [db-protocols/execute!
                    (fn [_db sql params]
                      (is (string? sql))
                      (is (= [(token-security/hash-token "token-1")] params))
                      [row])]
        (let [result (email-verification/find-verification-token :db "token-1")]
          (is (= user-id (:user_id result)))
          (is (= "user@example.com" (:verification_email result)))
          (is (= "Test User" (:verification_full_name result))))))))

(deftest create-verification-token-stores-hash
  (testing "create-verification-token! returns raw token but stores only its hash"
    (let [user-id (java.util.UUID/randomUUID)
          raw-token "verification-token-1"
          created (atom nil)]
      (with-redefs [email-verification/generate-verification-token (constantly raw-token)
                    db-protocols/execute! (fn [& _] nil)
                    db-protocols/create (fn [_db _tx table data]
                                          (reset! created {:table table :data data})
                                          data)]
        (is (= raw-token (email-verification/create-verification-token! :db user-id)))
        (is (= :email_verification_tokens (:table @created)))
        (is (= (token-security/hash-token raw-token) (get-in @created [:data :token])))
        (is (not= raw-token (get-in @created [:data :token])))))))

