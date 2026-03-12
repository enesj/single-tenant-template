(ns app.admin.backend.services.admin.admin-invitation
  "Admin invitation service — create, accept, revoke, resend.

   Unlike tenant invitations (where the user already has an account),
   accepting an admin invitation creates the admin account + session atomically."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.auth :as auth]
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [honey.sql :as sql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(def ^:private valid-invitation-roles #{"support" "admin"})

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
                                :from   [:admin_invitations]
                                :where  [:and
                                         [:= :email email]
                                         [:= :status [:cast "pending" :admin_invitation_status]]]
                                :limit  1}))]
    (when existing
      (throw (ex-info "A pending invitation already exists for this email"
               {:type :validation-error
                :errors {:email ["A pending invitation already exists for this email"]}})))))

(defn- assert-not-already-admin! [db email]
  (let [existing (jdbc/execute-one! db
                   (sql/format {:select [:id]
                                :from   [:admins]
                                :where  [:= :email email]
                                :limit  1}))]
    (when existing
      (throw (ex-info "An admin account already exists with this email"
               {:type :validation-error
                :errors {:email ["An admin account already exists with this email"]}})))))

;; ============================================================================
;; CRUD
;; ============================================================================

(defn create-invitation!
  "Create a pending admin invitation. Validates role, uniqueness, and no existing admin."
  [db {:keys [email role invited-by]}]
  (assert-valid-role! role)
  (assert-no-pending-invite! db email)
  (assert-not-already-admin! db email)
  (let [token      (auth/generate-session-token)
        expires-at (time/plus (time/instant) (time/days 7))
        inv-id     (UUID/randomUUID)
        now        (time/instant)
        inv-by     (if (string? invited-by) (UUID/fromString invited-by) invited-by)]
    (let [result (convert-pg-objects
                   (jdbc/execute-one! db
                     (sql/format {:insert-into [:admin_invitations]
                                  :values [{:id         inv-id
                                            :email      email
                                            :role       [:cast role :admin_invitation_role]
                                            :invited_by inv-by
                                            :status     [:cast "pending" :admin_invitation_status]
                                            :token      token
                                            :expires_at expires-at
                                            :created_at now
                                            :updated_at now}]
                                  :returning [:*]})
                     {:builder-fn rs/as-unqualified-maps}))]
      (audit/log-audit! db
        {:admin_id inv-by
         :action "create_admin_invitation"
         :entity-type "admin_invitation"
         :entity-id inv-id
         :changes {:email email :role role}})
      result)))

(defn find-invitation-by-token
  "Lookup an invitation by token, joined with the inviter's name."
  [db token]
  (convert-pg-objects
    (jdbc/execute-one! db
      (sql/format {:select [:ai.*
                            [:a.full_name :inviter_name]
                            [:a.email :inviter_email]]
                   :from   [[:admin_invitations :ai]]
                   :join   [[:admins :a] [:= :ai.invited_by :a.id]]
                   :where  [:= :ai.token token]})
      {:builder-fn rs/as-unqualified-maps})))

(defn find-invitation-by-id
  "Lookup an invitation by its UUID."
  [db invitation-id]
  (let [inv-id (if (string? invitation-id) (UUID/fromString invitation-id) invitation-id)]
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:select [:*]
                     :from   [:admin_invitations]
                     :where  [:= :id inv-id]})
        {:builder-fn rs/as-unqualified-maps}))))

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

  (let [invitation (find-invitation-by-token db token)]
    (when-not invitation
      (throw (ex-info "Invalid invitation token"
               {:type :validation-error
                :errors {:token ["This invitation token is invalid"]}})))

    (assert-invitation-valid! invitation)

    ;; Guard: not already an admin (race condition protection)
    (assert-not-already-admin! db (:email invitation))

    (jdbc/with-transaction [tx db]
      (let [admin-id (UUID/randomUUID)
            now      (time/instant)
            ;; Create admin account
            admin (convert-pg-objects
                    (jdbc/execute-one! tx
                      (sql/format {:insert-into [:admins]
                                   :values [{:id            admin-id
                                             :email         (:email invitation)
                                             :full_name     full-name
                                             :password_hash (auth/hash-password password)
                                             :role          [:cast (str (:role invitation)) :admin_role]
                                             :status        [:cast "active" :admin_status]
                                             :created_at    now
                                             :updated_at    now}]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-maps}))
            ;; Mark invitation accepted
            _ (jdbc/execute-one! tx
                (sql/format {:update [:admin_invitations]
                             :set    {:status     [:cast "accepted" :admin_invitation_status]
                                      :updated_at now}
                             :where  [:= :id (:id invitation)]}))
            ;; Create admin session (auto-login)
            session (auth/create-admin-session! tx admin-id
                      (or ip-address "invitation-accept")
                      (or user-agent "invitation-accept"))]
        (log/info "Admin invitation accepted — created admin" (:email admin) "as" (:role invitation)
          "invited by" (:invited_by invitation))
        (audit/log-audit! tx
          {:admin_id (:invited_by invitation)
           :action "admin_invitation_accepted"
           :entity-type "admin"
           :entity-id admin-id
           :changes {:email (:email admin) :role (str (:role invitation))
                     :invited_by (str (:invited_by invitation))}})
        {:admin   (dissoc admin :password_hash)
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
   Throws if the invitation is not pending."
  [db invitation-id admin-id]
  (let [inv (find-invitation-by-id db invitation-id)]
    (when-not inv
      (throw (ex-info "Invitation not found" {:type :entity-not-found})))
    (when (not= "pending" (str (:status inv)))
      (throw (ex-info "Only pending invitations can be resent"
               {:type :validation-error
                :errors {:status ["This invitation is no longer pending"]}})))
    (let [now         (time/instant)
          new-expires (time/plus now (time/days 7))]
      (jdbc/execute-one! db
        (sql/format {:update [:admin_invitations]
                     :set    {:expires_at new-expires
                              :updated_at now}
                     :where  [:= :id (:id inv)]}))
      (audit/log-audit! db
        {:admin_id admin-id
         :action "resend_admin_invitation"
         :entity-type "admin_invitation"
         :entity-id (:id inv)
         :changes {:email (:email inv)}})
      (assoc inv :expires_at new-expires :updated_at now))))

(defn list-pending-invitations
  "List all pending admin invitations, joined with inviter info."
  [db]
  (mapv convert-pg-objects
    (jdbc/execute! db
      (sql/format {:select [:ai.*
                            [:a.full_name :inviter_name]
                            [:a.email :inviter_email]]
                   :from   [[:admin_invitations :ai]]
                   :join   [[:admins :a] [:= :ai.invited_by :a.id]]
                   :where  [:= :ai.status [:cast "pending" :admin_invitation_status]]
                   :order-by [[:ai.created_at :desc]]})
      {:builder-fn rs/as-unqualified-maps})))

(comment
  ;; (require 'app.admin.backend.services.admin.admin-invitation :reload)
  :rcf)
