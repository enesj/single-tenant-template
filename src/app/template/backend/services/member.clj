(ns app.template.backend.services.member
  "Tenant member management — role changes, ownership transfer, removal."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.security.privacy-subject :as privacy-subject]
    [app.template.backend.services.onboarding.core :as onboarding]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(def ^:private valid-assignable-roles #{"admin" "member" "viewer"})

(defn- role-name [membership]
  (or (:role membership)
    (:tenant_memberships/role membership)
    (:tenant-memberships/role membership)))

(defn- membership-id [membership]
  (or (:tenant_memberships/id membership)
    (:tenant-memberships/id membership)
    (:membership/id membership)
    (:id membership)))

(defn- membership-status [membership]
  (or (:status membership)
    (:tenant_memberships/status membership)
    (:tenant-memberships/status membership)))

(defn- membership-user-id [membership]
  (or (:tenant_memberships/user_id membership)
    (:tenant-memberships/user-id membership)
    (:user_id membership)
    (:user-id membership)))

(defn- membership-tenant-id [membership]
  (or (:tenant_memberships/tenant_id membership)
    (:tenant-memberships/tenant-id membership)
    (:tenant_id membership)
    (:tenant-id membership)))

(defn- normalize-membership-row [membership]
  (when membership
    (cond-> membership
      (membership-id membership) (assoc :id (membership-id membership))
      (role-name membership) (assoc :role (role-name membership))
      (membership-status membership) (assoc :status (membership-status membership))
      (membership-user-id membership) (assoc :user_id (membership-user-id membership))
      (membership-tenant-id membership) (assoc :tenant_id (membership-tenant-id membership)))))

(defn- active-membership?
  [membership]
  (= "active" (membership-status membership)))

(defn- ensure-active-membership!
  [membership {:keys [message field status] :or {status 400}}]
  (when-not (active-membership? membership)
    (throw (ex-info message
             {:type :validation-error
              :status status
              :errors {(or field :membership) [message]}})))
  membership)

(defn- ensure-not-owner-target!
  [target-membership message]
  (when (= "owner" (role-name target-membership))
    (throw (ex-info message
             {:type :forbidden
              :status 403
              :errors {:membership [message]}})))
  target-membership)

;; ============================================================================
;; Role Changes
;; ============================================================================

(defn change-role!
  "Change a member's role. Actor must be owner or admin with sufficient rank."
  [db {:keys [actor-membership target-membership new-role]}]
  ;; Guard: valid role
  (when-not (contains? valid-assignable-roles new-role)
    (throw (ex-info "Invalid role"
             {:type :validation-error
              :errors {:role [(str "Role must be one of: " (pr-str valid-assignable-roles))]}})))

  ;; Guard: both active
  (when (or (not= "active" (membership-status actor-membership))
          (not= "active" (membership-status target-membership)))
    (throw (ex-info "Both memberships must be active"
             {:type :validation-error
              :errors {:membership ["Both memberships must be active"]}})))

  ;; Guard: cannot change own role
  (when (= (membership-id actor-membership) (membership-id target-membership))
    (throw (ex-info "Cannot change your own role"
             {:type :validation-error
              :errors {:membership ["Cannot change your own role"]}})))

  ;; Guard: only owners can set admin role
  (when (and (= new-role "admin")
          (not= "owner" (role-name actor-membership)))
    (throw (ex-info "Only owners can promote to admin"
             {:type :forbidden
              :errors {:role ["Only owners can promote members to admin"]}})))

  ;; Guard: cannot change owner's role
  (when (= "owner" (role-name target-membership))
    (throw (ex-info "Cannot change the owner's role directly — use transfer-ownership"
             {:type :forbidden
              :errors {:role ["Cannot change the owner's role directly"]}})))

  (let [target-id (membership-id target-membership)
        user-id   (membership-user-id target-membership)
        now       (java.time.LocalDateTime/now)
        result    (convert-pg-objects
                    (jdbc/execute-one! db
                      (sql/format {:update [:tenant_memberships]
                                   :set    {:role       [:cast new-role :membership_role]
                                            :updated_at now}
                                   :where  [:= :id target-id]
                                   :returning [:*]})))]
    ;; Initialise delta onboarding for the new role (pre-marks completed steps)
    (when user-id
      (try
        (onboarding/initialise-delta-onboarding! db user-id new-role)
        (catch Exception e
          (log/warn e "Failed to initialise onboarding on role change"
            {:user-id user-id :new-role new-role}))))
    (normalize-membership-row result)))

(defn superpower-change-role!
  "Platform-admin role change that still preserves tenant ownership invariants."
  [db {:keys [target-membership new-role]}]
  (when-not (contains? valid-assignable-roles new-role)
    (throw (ex-info "Invalid role"
             {:type :validation-error
              :status 400
              :errors {:role [(str "Role must be one of: " (pr-str valid-assignable-roles))]}})))

  (ensure-active-membership!
    target-membership
    {:message "Target membership must be active"
     :status 400})
  (ensure-not-owner-target!
    target-membership
    "Cannot change the owner's role directly — use transfer-ownership")

  (let [target-id (membership-id target-membership)
        now       (java.time.LocalDateTime/now)]
    (normalize-membership-row
      (convert-pg-objects
        (jdbc/execute-one! db
          (sql/format {:update [:tenant_memberships]
                       :set    {:role       [:cast new-role :membership_role]
                                :updated_at now}
                       :where  [:= :id target-id]
                       :returning [:*]}))))))

(defn superpower-remove-member!
  "Platform-admin member removal that still preserves tenant ownership invariants."
  [db {:keys [target-membership]}]
  (ensure-active-membership!
    target-membership
    {:message "Target membership must be active"
     :status 400})
  (ensure-not-owner-target!
    target-membership
    "Cannot remove the tenant owner")

  (let [target-id (membership-id target-membership)
        now       (java.time.LocalDateTime/now)]
    (normalize-membership-row
      (convert-pg-objects
        (jdbc/execute-one! db
          (sql/format {:update [:tenant_memberships]
                       :set    {:status     [:cast "suspended" :membership_status]
                                :updated_at now}
                       :where  [:= :id target-id]
                       :returning [:*]}))))))

;; ============================================================================
;; Ownership Transfer
;; ============================================================================

(defn transfer-ownership!
  "Transfer ownership from actor (must be owner) to target (must be admin).
   Atomically swaps: actor → admin, target → owner."
  [db {:keys [actor-membership target-membership]}]
  ;; Guard: actor must be owner
  (when (not= "owner" (role-name actor-membership))
    (throw (ex-info "Only the current owner can transfer ownership"
             {:type :forbidden
              :errors {:membership ["Only the current owner can transfer ownership"]}})))

  ;; Guard: target must be admin
  (when (not= "admin" (role-name target-membership))
    (throw (ex-info "Ownership can only be transferred to an admin"
             {:type :validation-error
              :errors {:membership ["Target must be an admin to receive ownership"]}})))

  ;; Guard: both active
  (when (or (not= "active" (membership-status actor-membership))
          (not= "active" (membership-status target-membership)))
    (throw (ex-info "Both memberships must be active"
             {:type :validation-error
              :errors {:membership ["Both memberships must be active"]}})))

  (let [actor-id  (membership-id actor-membership)
        target-id (membership-id target-membership)
        now       (java.time.LocalDateTime/now)]
    (jdbc/transact db
      (fn [tx]
      ;; Swap both roles in one statement so ownership constraints never observe
      ;; an intermediate zero-owner or two-owner state.
        (let [updated-rows (map normalize-membership-row
                             (convert-pg-objects
                               (jdbc/execute! tx
                                 ["UPDATE tenant_memberships
                                   SET role = CASE
                                                WHEN id = ? THEN 'admin'::membership_role
                                                WHEN id = ? THEN 'owner'::membership_role
                                                ELSE role
                                              END,
                                       updated_at = ?
                                   WHERE id IN (?, ?)
                                   RETURNING *"
                                  actor-id target-id now actor-id target-id])))
              updated-actor (some #(when (= (membership-id %) actor-id) %) updated-rows)]
          (log/info "Ownership transferred from" actor-id "to" target-id)
          updated-actor)))))

;; ============================================================================
;; Member Removal
;; ============================================================================

(defn remove-member!
  "Remove a member from the tenant (set status to suspended)."
  [db {:keys [actor-membership target-membership]}]
  ;; Guard: both active
  (when (or (not= "active" (membership-status actor-membership))
          (not= "active" (membership-status target-membership)))
    (throw (ex-info "Both memberships must be active"
             {:type :validation-error
              :errors {:membership ["Both memberships must be active"]}})))

  ;; Guard: cannot remove self
  (when (= (membership-id actor-membership) (membership-id target-membership))
    (throw (ex-info "Cannot remove yourself"
             {:type :validation-error
              :errors {:membership ["Cannot remove yourself"]}})))

  ;; Guard: cannot remove an owner
  (when (= "owner" (role-name target-membership))
    (throw (ex-info "Cannot remove the tenant owner"
             {:type :forbidden
              :errors {:membership ["Cannot remove the tenant owner"]}})))

  ;; Guard: actor must have manage rights (owner or admin)
  (when-not (#{"owner" "admin"} (role-name actor-membership))
    (throw (ex-info "Insufficient permissions to remove members"
             {:type :forbidden
              :errors {:membership ["Only owners and admins can remove members"]}})))

  (let [target-id (membership-id target-membership)
        target-tenant-id (membership-tenant-id target-membership)
        target-user-id (membership-user-id target-membership)
        now       (java.time.LocalDateTime/now)]
    (jdbc/transact db
      (fn [tx]
      (let [result (convert-pg-objects
                     (jdbc/execute-one! tx
                       (sql/format {:update [:tenant_memberships]
                                    :set    {:status     [:cast "suspended" :membership_status]
                                             :updated_at now}
                                    :where  [:= :id target-id]
                                    :returning [:*]})))]
        ;; Deactivate the user's system-type payer for this tenant
        (when (and target-tenant-id target-user-id)
          (try
            (let [system-payers (jdbc/execute! tx
                                  (sql/format {:select [:p.id]
                                               :from   [[:payers :p]]
                                               :where  [:and
                                                        [:= :p.tenant_id target-tenant-id]
                                                        [:= :p.type [:cast "system" :payer_type]]
                                                        [:= :p.is_active true]]})
                                  {:builder-fn rs/as-unqualified-lower-maps})
                  ;; Find payers that belong to this user (by matching label or via user_expense_settings)
                  user-settings (jdbc/execute-one! tx
                                  (sql/format {:select [:default_payer_id]
                                               :from   [:user_expense_settings]
                                               :where  [:and
                                                        [:= :tenant_id target-tenant-id]
                                                        [:= :subject_ref (privacy-subject/user-subject-ref target-user-id)]]})
                                  {:builder-fn rs/as-unqualified-lower-maps})
                  user-payer-id (:default_payer_id user-settings)
                  system-payer-ids (set (map :id system-payers))]
              ;; Deactivate the user's payer if it's a system-type payer
              (when (and user-payer-id (contains? system-payer-ids user-payer-id))
                (jdbc/execute-one! tx
                  (sql/format {:update [:payers]
                               :set    {:is_active false :updated_at now}
                               :where  [:= :id user-payer-id]})))
              ;; Delete user_expense_settings for this tenant
              (jdbc/execute-one! tx
                (sql/format {:delete-from [:user_expense_settings]
                             :where [:and
                                     [:= :tenant_id target-tenant-id]
                                     [:= :subject_ref (privacy-subject/user-subject-ref target-user-id)]]})))
            (catch Exception e
              (log/warn e "Failed to cleanup payer/settings on member removal"
                {:tenant-id target-tenant-id :user-id target-user-id}))))
        (normalize-membership-row result))))))

(defn reinstate-member!
  "Reinstate a suspended membership (set status to active)."
  [db {:keys [actor-membership target-membership]}]
  ;; Guard: actor must be active
  (when (not= "active" (membership-status actor-membership))
    (throw (ex-info "Actor membership must be active"
             {:type :validation-error
              :errors {:membership ["Actor membership must be active"]}})))

  ;; Guard: target must be suspended
  (when (not= "suspended" (membership-status target-membership))
    (throw (ex-info "Target membership is not suspended"
             {:type :validation-error
              :errors {:membership ["Target membership is not suspended"]}})))

  ;; Guard: cannot reinstate an owner membership
  (ensure-not-owner-target!
    target-membership
    "Cannot reinstate the tenant owner")

  ;; Guard: actor must have manage rights (owner or admin)
  (when-not (#{"owner" "admin"} (role-name actor-membership))
    (throw (ex-info "Insufficient permissions to reinstate members"
             {:type :forbidden
              :errors {:membership ["Only owners and admins can reinstate members"]}})))

  (let [target-id (membership-id target-membership)
        now       (java.time.LocalDateTime/now)]
    (normalize-membership-row
      (convert-pg-objects
        (jdbc/execute-one! db
          (sql/format {:update [:tenant_memberships]
                       :set    {:status     [:cast "active" :membership_status]
                                :updated_at now}
                       :where  [:= :id target-id]
                       :returning [:*]}))))))

(comment
  ;; (require 'app.template.backend.services.member :reload)
  :rcf)
