(ns app.template.backend.services.invitation-token-hashing-test
  (:require
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.security.tokens :as token-security]
    [app.template.backend.services.invitation :as invitation-svc]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

(deftest create-invitation-stores-token-hash-without-db
  (testing "tenant invitations return raw token but persist only hash"
    (let [raw-token "tenant-invite-token-1"
          captured (atom nil)]
      (with-redefs-fn {#'auth-service/create-session-token (constantly raw-token)
                       #'invitation-svc/assert-no-pending-invite! (fn [& _] nil)
                       #'invitation-svc/assert-not-already-member! (fn [& _] nil)
                       #'jdbc/execute-one! (fn [_db statement]
                                             (reset! captured statement)
                                             {:id (java.util.UUID/randomUUID)
                                              :status "pending"
                                              :token (token-security/hash-token raw-token)})}
        (fn []
          (let [result (invitation-svc/create-invitation! :db
                         {:tenant-id (java.util.UUID/randomUUID)
                          :email "newbie@example.com"
                          :role "member"
                          :invited-by (java.util.UUID/randomUUID)})
                [_sql & params] @captured]
            (is (= raw-token (:token result)))
            (is (some #{(token-security/hash-token raw-token)} params))
            (is (not (some #{raw-token} params)))))))))

(deftest find-invitation-by-token-queries-token-hash-without-db
  (testing "tenant invitation lookup hashes the incoming raw token"
    (let [raw-token "tenant-invite-token-2"]
      (with-redefs [jdbc/execute-one! (fn [_db statement _opts]
                                        (let [[_sql & params] statement]
                                          (is (some #{(token-security/hash-token raw-token)} params))
                                          (is (not (some #{raw-token} params))))
                                        nil)]
        (is (nil? (invitation-svc/find-invitation-by-token :db raw-token)))))))