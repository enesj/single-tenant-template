(ns app.template.backend.services.invitation
  "Tenant invitation service — create, accept, revoke invitations."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.services.onboarding.core :as onboarding]
    [app.template.backend.services.tenant :as tenant-svc]
    [honey.sql :as sql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(def ^:private valid-invitation-roles #{"admin" "member" "viewer"})

;; ============================================================================
;; Guards
;; ============================================================================

(defn- assert-valid-role! [role]
  (when-not (contains? valid-invitation-roles role)
    (throw (ex-info "Invalid invitation role"
             {:type :validation-error
              :errors {:role [(str "Role must be one of: " (pr-str valid-invitation-roles))]}}))))

(defn- assert-no-pending-invite! [db tenant-id email]
  (let [existing (jdbc/execute-one! db
                   (sql/format {:select [:id]
                                :from   [:tenant_invitations]
                                :where  [:and
                                         [:= :tenant_id tenant-id]
                                         [:= :email email]
                                         [:= :status [:cast "pending" :invitation_status]]]
                                :limit  1}))]
    (when existing
      (throw (ex-info "A pending invitation already exists for this email"
               {:type :validation-error
                :errors {:email ["A pending invitation already exists for this email"]}})))))

(defn- assert-not-already-member! [db tenant-id email]
  (let [existing (jdbc/execute-one! db
                   (sql/format {:select [:tm.id]
                                :from   [[:tenant_memberships :tm]]
                                :join   [[:users :u] [:= :tm.user_id :u.id]]
                                :where  [:and
                                         [:= :tm.tenant_id tenant-id]
                                         [:= :u.email email]
                                         [:= :tm.status [:cast "active" :membership_status]]]
                                :limit  1}))]
    (when existing
      (throw (ex-info "User is already an active member of this tenant"
               {:type :validation-error
                :errors {:email ["User is already an active member of this tenant"]}})))))

;; ============================================================================
;; CRUD
;; ============================================================================

(defn create-invitation!
  "Create a pending invitation. Validates role, uniqueness, and membership."
  [db {:keys [tenant-id email role invited-by]}]
  (assert-valid-role! role)
  (let [t-id (if (string? tenant-id) (java.util.UUID/fromString tenant-id) tenant-id)]
    (assert-no-pending-invite! db t-id email)
    (assert-not-already-member! db t-id email)
    (let [token      (auth-service/create-session-token)
          expires-at (time/plus (time/local-date-time) (time/days 7))
          inv-id     (java.util.UUID/randomUUID)
          now        (java.time.LocalDateTime/now)
          inv-by     (if (string? invited-by) (java.util.UUID/fromString invited-by) invited-by)]
      (convert-pg-objects
        (jdbc/execute-one! db
          (sql/format {:insert-into [:tenant_invitations]
                       :values [{:id         inv-id
                                 :tenant_id  t-id
                                 :email      email
                                 :role       [:cast role :invitation_role]
                                 :invited_by inv-by
                                 :status     [:cast "pending" :invitation_status]
                                 :token      token
                                 :expires_at expires-at
                                 :created_at now
                                 :updated_at now}]
                       :returning [:*]}))))))

(defn find-invitation-by-token
  "Lookup an invitation by token, joined with tenant name/slug."
  [db token]
  (convert-pg-objects
    (jdbc/execute-one! db
      (sql/format {:select [:ti.*
                            [:t.name :tenant_name]
                            [:t.slug :tenant_slug]]
                   :from   [[:tenant_invitations :ti]]
                   :join   [[:tenants :t] [:= :ti.tenant_id :t.id]]
                   :where  [:= :ti.token token]})
      {:builder-fn rs/as-unqualified-maps})))

(defn accept-invitation!
  "Accept an invitation — creates a membership in a transaction.
   `user` must have :id and :email keys."
  [db user invitation]
  ;; Guard: must be pending
  (when (not= "pending" (or (:status invitation)
                          (:tenant_invitations/status invitation)))
    (throw (ex-info "Invitation is not pending"
             {:type :validation-error
              :errors {:token ["This invitation is no longer valid"]}})))

  ;; Guard: not expired
  (let [expires-raw (or (:expires_at invitation)
                      (:tenant_invitations/expires_at invitation))
        ;; Handle various time types from DB (LocalDateTime, OffsetDateTime, Instant, String)
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
                :errors {:token ["This invitation has expired"]}}))))

  ;; Guard: email matches
  (let [inv-email (or (:email invitation) (:tenant_invitations/email invitation))
        user-email (or (:email user) (:users/email user))]
    (when (not= (some-> inv-email clojure.string/lower-case)
            (some-> user-email clojure.string/lower-case))
      (throw (ex-info "Invitation email does not match your account"
               {:type :validation-error
                :errors {:email [(str "This invitation was sent to " inv-email " but you are logged in as " user-email)]}}))))

  ;; Guard: not already a member
  (let [tenant-id (or (:tenant_id invitation) (:tenant_invitations/tenant_id invitation))
        user-id   (let [id (or (:id user) (:users/id user))]
                    (if (string? id) (java.util.UUID/fromString id) id))
        existing  (jdbc/execute-one! db
                    (sql/format {:select [:id]
                                 :from   [:tenant_memberships]
                                 :where  [:and
                                          [:= :tenant_id tenant-id]
                                          [:= :user_id user-id]
                                          [:= :status [:cast "active" :membership_status]]]
                                 :limit  1}))]
    (when existing
      (throw (ex-info "You are already a member of this tenant"
               {:type :validation-error
                :errors {:email ["You are already a member of this tenant"]}}))))

  ;; All good — create membership and mark invitation accepted
  (let [tenant-id   (or (:tenant_id invitation) (:tenant_invitations/tenant_id invitation))
        inv-id      (or (:id invitation) (:tenant_invitations/id invitation))
        role        (or (:role invitation) (:tenant_invitations/role invitation))
        invited-by  (or (:invited_by invitation) (:tenant_invitations/invited_by invitation))
        user-id     (let [id (or (:id user) (:users/id user))]
                      (if (string? id) (java.util.UUID/fromString id) id))
        now         (java.time.LocalDateTime/now)
        member-id   (java.util.UUID/randomUUID)]
    (jdbc/with-transaction [tx db]
      ;; Create membership
      (let [membership (convert-pg-objects
                         (jdbc/execute-one! tx
                           (sql/format {:insert-into [:tenant_memberships]
                                        :values [{:id         member-id
                                                  :tenant_id  tenant-id
                                                  :user_id    user-id
                                                  :role       [:cast (name role) :membership_role]
                                                  :status     [:cast "active" :membership_status]
                                                  :invited_by invited-by
                                                  :created_at now
                                                  :updated_at now}]
                                        :returning [:*]})))]
        ;; Mark invitation accepted
        (jdbc/execute-one! tx
          (sql/format {:update [:tenant_invitations]
                       :set    {:status     [:cast "accepted" :invitation_status]
                                :updated_at now}
                       :where  [:= :id inv-id]}))

        ;; Auto-provision a payer + user_expense_settings for the new member
        (try
          (tenant-svc/provision-user-payer! tx tenant-id user-id
            (or (:email user) (:users/email user))
            :full-name (or (:full_name user) (:users/full_name user)))
          (catch Exception e
            (log/warn e "Failed to provision user payer on invitation accept"
              {:tenant-id tenant-id :user-id user-id})))

        ;; Initialise onboarding for the invited role (delta-aware: pre-marks
        ;; steps already completed in prior roles)
        (try
          (onboarding/initialise-delta-onboarding! tx user-id (name role))
          (catch Exception e
            (log/warn e "Failed to initialise onboarding on invitation accept"
              {:user-id user-id :role role})))

        (log/info "Invitation accepted — user" (or (:email user) (:users/email user)) "joined tenant" tenant-id "as" role)
        membership))))

(defn revoke-invitation!
  "Revoke a pending invitation. No-op if already non-pending."
  [db invitation-id]
  (let [inv-id (if (string? invitation-id) (java.util.UUID/fromString invitation-id) invitation-id)]
    (jdbc/execute-one! db
      (sql/format {:update [:tenant_invitations]
                   :set    {:status     [:cast "revoked" :invitation_status]
                            :updated_at (java.time.LocalDateTime/now)}
                   :where  [:and
                            [:= :id inv-id]
                            [:= :status [:cast "pending" :invitation_status]]]}))))

(defn find-invitation-by-id
  "Lookup an invitation by its UUID."
  [db invitation-id]
  (let [inv-id (if (string? invitation-id) (java.util.UUID/fromString invitation-id) invitation-id)]
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:select [:*]
                     :from   [:tenant_invitations]
                     :where  [:= :id inv-id]})
        {:builder-fn rs/as-unqualified-maps}))))

(defn resend-invitation!
  "Refresh the expiry of a pending invitation and return it (so the caller can re-send the email).
   Throws if the invitation is not pending."
  [db invitation-id]
  (let [inv (find-invitation-by-id db invitation-id)]
    (when-not inv
      (throw (ex-info "Invitation not found" {:type :entity-not-found})))
    (when (not= "pending" (:status inv))
      (throw (ex-info "Only pending invitations can be resent"
               {:type :validation-error
                :errors {:status ["This invitation is no longer pending"]}})))
    (let [now (java.time.LocalDateTime/now)
          new-expires (time/plus (time/local-date-time) (time/days 7))]
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_invitations]
                     :set    {:expires_at new-expires
                              :updated_at now}
                     :where  [:= :id (:id inv)]}))
      ;; Return the invitation with the refreshed expiry
      (assoc inv :expires_at new-expires :updated_at now))))

(defn list-pending-invitations
  "List all pending invitations for a tenant."
  [db tenant-id]
  (let [t-id (if (string? tenant-id) [:cast tenant-id :uuid] tenant-id)]
    (mapv convert-pg-objects
      (jdbc/execute! db
        (sql/format {:select [:*]
                     :from   [:tenant_invitations]
                     :where  [:and
                              [:= :tenant_id t-id]
                              [:= :status [:cast "pending" :invitation_status]]]
                     :order-by [[:created_at :desc]]})))))

(comment
  ;; (require 'app.template.backend.services.invitation :reload)
  :rcf)
