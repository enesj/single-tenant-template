(ns app.admin.backend.services.admin.admin-invitation-token-hashing-test
  (:require
    [app.admin.backend.services.admin.admin-invitation :as inv-svc]
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.auth :as auth]
    [app.template.backend.security.tokens :as token-security]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

(deftest create-invitation-stores-token-hash-without-db
  (testing "admin invitations return raw token but persist only hash"
    (let [raw-token "admin-invite-token-1"
          captured (atom nil)]
      (with-redefs-fn {#'auth/generate-session-token (constantly raw-token)
                       #'inv-svc/assert-no-pending-invite! (fn [& _] nil)
                       #'inv-svc/assert-not-already-admin! (fn [& _] nil)
                       #'audit/log-audit! (fn [& _] nil)
                       #'jdbc/execute-one! (fn [_db statement _opts]
                                             (reset! captured statement)
                                             {:id (java.util.UUID/randomUUID)
                                              :status "pending"
                                              :role "admin"
                                              :token (token-security/hash-token raw-token)})}
        (fn []
          (let [result (inv-svc/create-invitation! :db
                         {:email "newadmin@example.com"
                          :role "admin"
                          :invited-by (java.util.UUID/randomUUID)})
                [_sql & params] @captured]
            (is (= raw-token (:token result)))
            (is (some #{(token-security/hash-token raw-token)} params))
            (is (not (some #{raw-token} params)))))))))

(deftest find-invitation-by-token-queries-token-hash-without-db
  (testing "admin invitation lookup hashes the incoming raw token"
    (let [raw-token "admin-invite-token-2"]
      (with-redefs [jdbc/execute-one! (fn [_db statement _opts]
                                        (let [[_sql & params] statement]
                                          (is (some #{(token-security/hash-token raw-token)} params))
                                          (is (not (some #{raw-token} params))))
                                        nil)]
        (is (nil? (inv-svc/find-invitation-by-token :db raw-token)))))))