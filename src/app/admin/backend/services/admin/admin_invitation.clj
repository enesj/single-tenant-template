(ns app.admin.backend.services.admin.admin-invitation
  "Admin invitation service — create, accept, revoke, resend.

   Unlike tenant invitations (where the user already has an account),
   accepting an admin invitation creates the admin account + session atomically."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.auth :as auth]
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.security.tokens :as token-security]
    [honey.sql :as sql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(def ^:private valid-invitation-roles #{"support" "admin"})

(defn- with-resolved-invitation-identities
  [invitation]
  (let [email (some-> invitation email-privacy/resolve-email)
        inviter-email (or (:inviter_email invitation)
                        (some-> (:inviter_email_ciphertext invitation)
                          email-privacy/decrypt-email))]
    (cond-> (dissoc invitation :token)
      email (assoc :email email)
      inviter-email (assoc :inviter_email inviter-email))))

(defn- routine-invitation-view
  [invitation]
  (let [email (email-privacy/resolve-email invitation)
        inviter-email (some-> (:inviter_email invitation) email-privacy/mask-email)]
    (cond-> (dissoc invitation
              :token
              :email
              :email_ciphertext
              :email_lookup_hash
              :email_key_version
              :inviter_email
              :inviter_email_ciphertext)
      email (assoc :email_masked (email-privacy/mask-email email))
      inviter-email (assoc :inviter_email_masked inviter-email))))

;; ============================================================================
;; Guards
;; ============================================================================

(defn- assert-valid-role! [role]
  (when-not (contains? valid-invitation-roles role)
    (throw (ex-info "Invalid invitation role"
             {:type :validation-error
              :errors {:role [(str "Role must be one of: " (pr-str valid-invitation-roles))]}}))))

(defn- assert-no-pending-invite! [db email]
  (let [existing (jdbc/execute-one! db
                   (sql/format {:select [:id]
                                :from [:admin_invitations]
                                :where [:and
                                        (email-privacy/email-match-clause :email_lookup_hash :email email)
                                        [:= :status [:cast "pending" :admin_invitation_status]]]
                                :limit 1}))]
    (when existing
      (throw (ex-info "A pending invitation already exists for this email"
               {:type :validation-error
                :errors {:email ["A pending invitation already exists for this email"]}})))))

(defn- assert-not-already-admin! [db email]
  (when (auth/find-admin-by-email db email)
    (throw (ex-info "An admin account already exists with this email"
             {:type :validation-error
              :errors {:email ["An admin account already exists with this email"]}}))))

;; ============================================================================
;; CRUD
;; ============================================================================

(defn create-invitation!
  "Create a pending admin invitation. Validates role, uniqueness, and no existing admin."
  [db {:keys [email role invited-by]}]
  (let [normalized-email (email-privacy/normalize-email email)]
    (assert-valid-role! role)
    (assert-no-pending-invite! db normalized-email)
    (assert-not-already-admin! db normalized-email)
    (let [token (auth/generate-session-token)
          token-storage (token-security/hash-token token)
          expires-at (time/plus (time/instant) (time/days 7))
          inv-id (UUID/randomUUID)
          now (time/instant)
          inv-by (if (string? invited-by) (UUID/fromString invited-by) invited-by)
          result (convert-pg-objects
                   (jdbc/execute-one! db
                     (sql/format {:insert-into [:admin_invitations]
                                  :values [(merge
                                             {:id inv-id
                                              :role [:cast role :admin_invitation_role]
                                              :invited_by inv-by
                                              :status [:cast "pending" :admin_invitation_status]
                                              :token token-storage
                                              :expires_at expires-at
                                              :created_at now
                                              :updated_at now}
                                             (email-privacy/email-storage normalized-email))]
                                  :returning [:*]})
                     {:builder-fn rs/as-unqualified-maps}))]
      (audit/log-audit! db
        {:admin_id inv-by
         :action "create_admin_invitation"
         :entity-type "admin_invitation"
         :entity-id inv-id
         :changes (merge {:role role}
                    (email-privacy/redact-email-change normalized-email))})
      (assoc (routine-invitation-view result) :token token))))

(defn find-invitation-by-token
  "Lookup an invitation by token, joined with the inviter's name."
  [db token]
  (some-> (jdbc/execute-one! db
            (sql/format {:select [:ai.*
                                  [:a.full_name :inviter_name]
                                  [:a.email_ciphertext :inviter_email_ciphertext]]
                         :from   [[:admin_invitations :ai]]
                         :join   [[:admins :a] [:= :ai.invited_by :a.id]]
                         :where  [:= :ai.token (token-security/hash-token token)]})
            {:builder-fn rs/as-unqualified-maps})
    convert-pg-objects
    with-resolved-invitation-identities))

(defn find-invitation-by-id
  "Lookup an invitation by its UUID."
  [db invitation-id]
  (let [inv-id (if (string? invitation-id) (UUID/fromString invitation-id) invitation-id)]
    (some-> (jdbc/execute-one! db
              (sql/format {:select [:*]
                           :from   [:admin_invitations]
                           :where  [:= :id inv-id]})
              {:builder-fn rs/as-unqualified-maps})
      convert-pg-objects
      with-resolved-invitation-identities)))

(defn- assert-invitation-valid! [invitation]
  ;; Guard: must be pending
  (when (not= "pending" (str (:status invitation)))
    (throw (ex-info "Invitation is not pending"
             {:type :validation-error
              :errors {:token ["This invitation is no longer valid"]}})))
  ;; Guard: not expired
  (let [expires-raw (:expires_at invitation)
        expires-inst (when expires-raw
                       (cond
                         (instance? java.time.Instant expires-raw) expires-raw
                         (instance? java.time.OffsetDateTime expires-raw) (.toInstant expires-raw)
                         (instance? java.time.LocalDateTime expires-raw)
                         (.toInstant (.atZone expires-raw (java.time.ZoneId/systemDefault)))
                         :else (.toInstant (java.time.OffsetDateTime/parse (str expires-raw)))))]
    (when (and expires-inst (.isBefore expires-inst (java.time.Instant/now)))
      (throw (ex-info "Invitation has expired"
               {:type :validation-error
                :errors {:token ["This invitation has expired"]}})))))

(defn accept-invitation!
  "Accept an admin invitation — atomically creates admin + session in a transaction.

   Returns {:admin admin-map, :session session-map}."
  [db {:keys [token full-name password ip-address user-agent]}]
  (when (or (nil? full-name) (empty? (str full-name)))
    (throw (ex-info "Full name is required"
             {:type :validation-error
              :errors {:full-name ["Full name is required"]}})))
  (when (or (nil? password) (< (count (str password)) 10))
    (throw (ex-info "Password must be at least 10 characters"
             {:type :validation-error
              :errors {:password ["Password must be at least 10 characters"]}})))

  (let [invitation (find-invitation-by-token db token)
        invitation-email (email-privacy/resolve-email invitation)]
    (when-not invitation
      (throw (ex-info "Invalid invitation token"
               {:type :validation-error
                :errors {:token ["This invitation token is invalid"]}})))

    (assert-invitation-valid! invitation)
    (assert-not-already-admin! db invitation-email)

    (jdbc/with-transaction [tx db]
      (let [admin-id (UUID/randomUUID)
            now (time/instant)
            admin (convert-pg-objects
                    (jdbc/execute-one! tx
                      (sql/format {:insert-into [:admins]
                                   :values [(merge
                                              {:id admin-id
                                               :full_name full-name
                                               :password_hash (auth/hash-password password)
                                               :role [:cast (str (:role invitation)) :admin_role]
                                               :status [:cast "active" :admin_status]
                                               :created_at now
                                               :updated_at now}
                                              (email-privacy/email-storage invitation-email))]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-maps}))
            _ (jdbc/execute-one! tx
                (sql/format {:update [:admin_invitations]
                             :set {:status [:cast "accepted" :admin_invitation_status]
                                   :updated_at now}
                             :where [:= :id (:id invitation)]}))
            session (auth/create-admin-session! tx admin-id
                      (or ip-address "invitation-accept")
                      (or user-agent "invitation-accept"))]
        (log/info "Admin invitation accepted"
          {:admin-ref (email-privacy/admin-ref admin-id)
           :role (str (:role invitation))
           :invited-by (:invited_by invitation)})
        (audit/log-audit! tx
          {:admin_id (:invited_by invitation)
           :action "admin_invitation_accepted"
           :entity-type "admin"
           :entity-id admin-id
           :changes (merge {:role (str (:role invitation))
                            :invited_by (str (:invited_by invitation))}
                      (email-privacy/redact-email-change invitation-email))})
        {:admin (-> admin
                  (dissoc :password_hash :email_ciphertext :email_lookup_hash :email_key_version)
                  (assoc :email invitation-email))
         :session session}))))

(defn revoke-invitation!
  "Revoke a pending invitation. No-op if already non-pending."
  [db invitation-id admin-id]
  (let [inv-id (if (string? invitation-id) (UUID/fromString invitation-id) invitation-id)]
    (jdbc/execute-one! db
      (sql/format {:update [:admin_invitations]
                   :set    {:status     [:cast "revoked" :admin_invitation_status]
                            :updated_at (time/instant)}
                   :where  [:and
                            [:= :id inv-id]
                            [:= :status [:cast "pending" :admin_invitation_status]]]}))
    (audit/log-audit! db
      {:admin_id admin-id
       :action "revoke_admin_invitation"
       :entity-type "admin_invitation"
       :entity-id inv-id
       :changes {:action "revoked"}})))

(defn resend-invitation!
  "Refresh the expiry of a pending invitation and return it for re-sending.
   Throws if the invitation is not pending. Generates a new raw token because the
   stored token value is a non-recoverable hash."
  [db invitation-id admin-id]
  (let [inv (find-invitation-by-id db invitation-id)]
    (when-not inv
      (throw (ex-info "Invitation not found" {:type :entity-not-found})))
    (when (not= "pending" (str (:status inv)))
      (throw (ex-info "Only pending invitations can be resent"
               {:type :validation-error
                :errors {:status ["This invitation is no longer pending"]}})))
    (let [token       (auth/generate-session-token)
          token-storage (token-security/hash-token token)
          now         (time/instant)
          new-expires (time/plus now (time/days 7))]
      (jdbc/execute-one! db
        (sql/format {:update [:admin_invitations]
                     :set    {:token token-storage
                              :expires_at new-expires
                              :updated_at now}
                     :where  [:= :id (:id inv)]}))
      (audit/log-audit! db
        {:admin_id admin-id
         :action "resend_admin_invitation"
         :entity-type "admin_invitation"
         :entity-id (:id inv)
         :changes (email-privacy/redact-email-change (email-privacy/resolve-email inv))})
      (assoc (routine-invitation-view (assoc inv :expires_at new-expires :updated_at now)) :token token))))

(defn list-pending-invitations
  "List all pending admin invitations, joined with inviter info."
  [db]
  (->> (jdbc/execute! db
         (sql/format {:select [:ai.*
                               [:a.full_name :inviter_name]
                               [:a.email_ciphertext :inviter_email_ciphertext]]
                      :from   [[:admin_invitations :ai]]
                      :join   [[:admins :a] [:= :ai.invited_by :a.id]]
                      :where  [:= :ai.status [:cast "pending" :admin_invitation_status]]
                      :order-by [[:ai.created_at :desc]]})
         {:builder-fn rs/as-unqualified-maps})
    (mapv #(-> % convert-pg-objects with-resolved-invitation-identities routine-invitation-view))))

(comment
  ;; (require 'app.admin.backend.services.admin.admin-invitation :reload)
  :rcf)
