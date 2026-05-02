(ns app.admin.backend.services.admin.admin-invitation-test
  "Tests for admin invitation create/accept/revoke/resend guards."
  (:require
    [app.admin.backend.services.admin.admin-invitation :as inv-svc]
    [app.admin.backend.services.admin.auth :as auth]
    [app.backend.fixtures :as fixtures]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.security.tokens :as token-security]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- create-admin! [db suffix & [{:keys [role] :or {role "admin"}}]]
  (let [id (java.util.UUID/randomUUID)
        now (java.time.Instant/now)
        email (str "inv-test-" suffix "-" id "@example.com")]
    (assoc
      (jdbc/execute-one! db
        (sql/format {:insert-into [:admins]
                     :values [(merge {:id            id
                                      :full_name     (str "Admin " suffix)
                                      :password_hash (auth/hash-password "testpassword123")
                                      :role          [:cast role :admin_role]
                                      :status        [:cast "active" :admin_status]
                                      :created_at    now
                                      :updated_at    now}
                                (email-privacy/email-storage email))]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-maps})
      :email email)))

(defn- unique-email [prefix]
  (str prefix "-" (java.util.UUID/randomUUID) "@example.com"))

;; ============================================================================
;; create-invitation!
;; ============================================================================

(deftest create-invitation-happy-path
  (let [db           fixtures/*test-db*
        owner        (create-admin! db "owner")
        invite-email (unique-email "newadmin")
        inv          (inv-svc/create-invitation! db
                       {:email      invite-email
                        :role       "admin"
                        :invited-by (:id owner)})]
    (testing "creates a pending invitation with raw token in the return value"
      (is (some? inv))
      (is (= "pending" (str (:status inv))))
      (is (some? (:token inv)))
      (is (nil? (:email inv)))
      (is (= (email-privacy/mask-email invite-email) (:email_masked inv))))

    (testing "stores only a hash of the invitation token"
      (let [stored (jdbc/execute-one! db
                     (sql/format {:select [:token]
                                  :from [:admin_invitations]
                                  :where [:= :id (:id inv)]})
                     {:builder-fn rs/as-unqualified-maps})]
        (is (= (token-security/hash-token (:token inv)) (:token stored)))
        (is (not= (:token inv) (:token stored)))))))

(deftest create-invitation-invalid-role
  (let [db    fixtures/*test-db*
        owner (create-admin! db "owner2")]
    (testing "rejects owner role"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid invitation role"
            (inv-svc/create-invitation! db
              {:email "x@x.com" :role "owner" :invited-by (:id owner)}))))
    (testing "rejects unknown role"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid invitation role"
            (inv-svc/create-invitation! db
              {:email "x@x.com" :role "superuser" :invited-by (:id owner)}))))))

(deftest create-invitation-duplicate-pending
  (let [db           fixtures/*test-db*
        owner        (create-admin! db "owner3")
        invite-email (unique-email "dup")]
    (inv-svc/create-invitation! db
      {:email invite-email :role "admin" :invited-by (:id owner)})
    (testing "rejects duplicate pending invitation for same email"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"pending invitation already exists"
            (inv-svc/create-invitation! db
              {:email invite-email :role "support" :invited-by (:id owner)}))))))

(deftest create-invitation-email-already-admin
  (let [db    fixtures/*test-db*
        owner (create-admin! db "owner4")
        existing (create-admin! db "existing" {:role "admin"})]
    (testing "rejects invitation if admin already exists with that email"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"already exists"
            (inv-svc/create-invitation! db
              {:email (:email existing) :role "admin" :invited-by (:id owner)}))))))

;; ============================================================================
;; accept-invitation!
;; ============================================================================

(deftest accept-invitation-happy-path
  (let [db           fixtures/*test-db*
        owner        (create-admin! db "accept-owner")
        invite-email (unique-email "acceptme")
        inv          (inv-svc/create-invitation! db
                       {:email      invite-email
                        :role       "support"
                        :invited-by (:id owner)})
        token        (:token inv)
        result       (inv-svc/accept-invitation! db
                       {:token     token
                        :full-name "New Support Admin"
                        :password  "securepassword123"})]
    (testing "creates admin and session"
      (is (some? (:admin result)))
      (is (some? (:session result)))
      (is (= invite-email (:email (:admin result))))
      (is (some? (:token (:session result)))))

    (testing "invitation is marked accepted"
      (let [fetched (inv-svc/find-invitation-by-token db token)]
        (is (= "accepted" (str (:status fetched))))))))

(deftest accept-invitation-expired
  (let [db    fixtures/*test-db*
        owner (create-admin! db "exp-owner")
        inv   (inv-svc/create-invitation! db
                {:email      (unique-email "expired")
                 :role       "admin"
                 :invited-by (:id owner)})]
    ;; Manually expire the invitation
    (jdbc/execute-one! db
      (sql/format {:update [:admin_invitations]
                   :set    {:expires_at (.minus (java.time.Instant/now) (java.time.Duration/ofDays 1))}
                   :where  [:= :id (:id inv)]}))
    (testing "rejects expired invitation"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"expired"
            (inv-svc/accept-invitation! db
              {:token     (:token inv)
               :full-name "Late Admin"
               :password  "securepassword123"}))))))

(deftest accept-invitation-revoked
  (let [db    fixtures/*test-db*
        owner (create-admin! db "rev-owner")
        inv   (inv-svc/create-invitation! db
                {:email      (unique-email "revoked")
                 :role       "admin"
                 :invited-by (:id owner)})]
    (inv-svc/revoke-invitation! db (:id inv) (:id owner))
    (testing "rejects revoked invitation"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not pending"
            (inv-svc/accept-invitation! db
              {:token     (:token inv)
               :full-name "Revoked Admin"
               :password  "securepassword123"}))))))

(deftest accept-invitation-short-password
  (let [db    fixtures/*test-db*
        owner (create-admin! db "pwd-owner")
        inv   (inv-svc/create-invitation! db
                {:email      (unique-email "shortpwd")
                 :role       "admin"
                 :invited-by (:id owner)})]
    (testing "rejects password shorter than 10 chars"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"at least 10"
            (inv-svc/accept-invitation! db
              {:token     (:token inv)
               :full-name "Short Pwd"
               :password  "short"}))))))

;; ============================================================================
;; revoke-invitation!
;; ============================================================================

(deftest revoke-invitation-test
  (let [db    fixtures/*test-db*
        owner (create-admin! db "revoke-owner")
        inv   (inv-svc/create-invitation! db
                {:email (unique-email "revokee") :role "admin" :invited-by (:id owner)})]
    (inv-svc/revoke-invitation! db (:id inv) (:id owner))
    (testing "invitation status is revoked"
      (let [fetched (inv-svc/find-invitation-by-token db (:token inv))]
        (is (= "revoked" (str (:status fetched))))))))

;; ============================================================================
;; resend-invitation!
;; ============================================================================

(deftest resend-invitation-test
  (let [db    fixtures/*test-db*
        owner (create-admin! db "resend-owner")
        inv   (inv-svc/create-invitation! db
                {:email (unique-email "resendee") :role "support" :invited-by (:id owner)})
        original-token (:token inv)
        original-expires (:expires_at inv)
        ;; Wait briefly to ensure different timestamp
        _ (Thread/sleep 50)
        updated (inv-svc/resend-invitation! db (:id inv) (:id owner))]
    (testing "extends expiry and returns a fresh raw token"
      (is (some? (:expires_at updated)))
      (is (not= (str original-expires) (str (:expires_at updated))))
      (is (some? (:token updated)))
      (is (not= original-token (:token updated))))

    (testing "stores only the new token hash"
      (let [stored (jdbc/execute-one! db
                     (sql/format {:select [:token]
                                  :from [:admin_invitations]
                                  :where [:= :id (:id inv)]})
                     {:builder-fn rs/as-unqualified-maps})]
        (is (= (token-security/hash-token (:token updated)) (:token stored)))
        (is (not= (:token updated) (:token stored)))))))

(deftest list-pending-invitations-does-not-expose-token-hashes
  (let [db    fixtures/*test-db*
        owner (create-admin! db "token-list-owner")]
    (inv-svc/create-invitation! db
      {:email (unique-email "token-list") :role "admin" :invited-by (:id owner)})
    (testing "list results omit token values entirely"
      (is (every? #(nil? (:token %))
            (inv-svc/list-pending-invitations db))))))

;; ============================================================================
;; list-pending-invitations
;; ============================================================================

(deftest list-pending-invitations-test
  (let [db      fixtures/*test-db*
        owner   (create-admin! db "list-owner")
        email-a (unique-email "pending-a")
        email-b (unique-email "pending-b")]
    (inv-svc/create-invitation! db
      {:email email-a :role "admin" :invited-by (:id owner)})
    (inv-svc/create-invitation! db
      {:email email-b :role "support" :invited-by (:id owner)})
    (testing "returns all pending invitations with inviter info"
      (let [pending (inv-svc/list-pending-invitations db)]
        (is (= 2 (count pending)))
        (is (every? #(some? (:inviter_name %)) pending))))))
