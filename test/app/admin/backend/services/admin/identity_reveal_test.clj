(ns app.admin.backend.services.admin.identity-reveal-test
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.identity-reveal :as identity-reveal]
    [app.template.backend.security.email :as email-privacy]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]))

(deftest reveal-email-requires-structured-break-glass-reason
  (testing "missing reason code is rejected before any DB lookup"
    (let [lookups (atom 0)]
      (with-redefs [jdbc/execute-one! (fn [& _]
                                        (swap! lookups inc)
                                        nil)]
        (try
          (identity-reveal/reveal-email! :db :user (java.util.UUID/randomUUID)
            {:admin-id (java.util.UUID/randomUUID)
             :reason "Investigating a legitimate support case"})
          (is false "Expected missing reason code to throw")
          (catch clojure.lang.ExceptionInfo e
            (is (= 400 (:status (ex-data e))))
            (is (= :reason-code (:field (ex-data e))))
            (is (seq (:allowed (ex-data e))))
            (is (zero? @lookups)))))))

  (testing "short reason details are rejected"
    (try
      (identity-reveal/reveal-email! :db :user (java.util.UUID/randomUUID)
        {:admin-id (java.util.UUID/randomUUID)
         :reason-code :legal-request
         :reason "too short"})
      (is false "Expected short reason to throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= 400 (:status (ex-data e))))
        (is (= :reason (:field (ex-data e))))
        (is (= 20 (:min-length (ex-data e))))))))

(deftest reveal-email-audits-structured-break-glass-metadata
  (testing "successful reveal includes structured reason metadata but audit omits raw email"
    (let [admin-id (java.util.UUID/randomUUID)
          user-id #uuid "00000000-0000-0000-0000-000000000001"
          audit-event (atom nil)]
      (with-redefs [jdbc/execute-one! (fn [_db _statement _opts]
                                        {:id user-id
                                         :email "private@example.test"})
                    audit/log-audit! (fn [_db event]
                                       (reset! audit-event event))]
        (let [result (identity-reveal/reveal-email! :db :user user-id
                       {:admin-id admin-id
                        :reason_code "LEGAL_REQUEST"
                        :reason "Responding to a documented legal identity request"
                        :ip-address "127.0.0.1"
                        :user-agent "test-agent"})]
          (is (= "private@example.test" (get-in result [:reveal :email])))
          (is (= :legal-request (:reason-code result)))
          (is (= "Legal or compliance request" (:reason-label result)))
          (is (= admin-id (:admin_id @audit-event)))
          (is (= "reveal_user_email" (:action @audit-event)))
          (is (= :legal-request (get-in @audit-event [:changes :reason_code])))
          (is (= "Legal or compliance request" (get-in @audit-event [:changes :reason_label])))
          (is (= "User-00000000" (get-in @audit-event [:changes :entity_ref])))
          (is (= (email-privacy/mask-email "private@example.test")
                (get-in @audit-event [:changes :email_masked])))
          (is (not= "private@example.test" (get-in @audit-event [:changes :email])))
          (is (true? (get-in @audit-event [:changes :revealed]))))))))
