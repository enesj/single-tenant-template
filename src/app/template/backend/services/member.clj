(ns app.template.backend.services.member
  "Tenant member management — role changes, ownership transfer, removal."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(def ^:private valid-assignable-roles #{"admin" "member" "viewer"})

(defn- role-name [membership]
  (or (:role membership)
    (:tenant_memberships/role membership)))

(defn- membership-id [membership]
  (or (:id membership)
    (:tenant_memberships/id membership)))

(defn- membership-status [membership]
  (or (:status membership)
    (:tenant_memberships/status membership)))

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
        now       (java.time.LocalDateTime/now)]
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_memberships]
                     :set    {:role       [:cast new-role :membership_role]
                              :updated_at now}
                     :where  [:= :id target-id]
                     :returning [:*]})))))

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
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_memberships]
                     :set    {:role       [:cast new-role :membership_role]
                              :updated_at now}
                     :where  [:= :id target-id]
                     :returning [:*]})))))

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
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_memberships]
                     :set    {:status     [:cast "suspended" :membership_status]
                              :updated_at now}
                     :where  [:= :id target-id]
                     :returning [:*]})))))

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
    (jdbc/with-transaction [tx db]
      ;; Actor → admin first so the partial unique owner index never sees two active owners.
      (let [updated-actor (convert-pg-objects
                            (jdbc/execute-one! tx
                              (sql/format {:update [:tenant_memberships]
                                           :set    {:role [:cast "admin" :membership_role]
                                                    :updated_at now}
                                           :where  [:= :id actor-id]
                                           :returning [:*]})))]
        ;; Target → owner
        (jdbc/execute-one! tx
          (sql/format {:update [:tenant_memberships]
                       :set    {:role [:cast "owner" :membership_role]
                                :updated_at now}
                       :where  [:= :id target-id]}))
        (log/info "Ownership transferred from" actor-id "to" target-id)
        updated-actor))))

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
        target-tenant-id (or (:tenant_id target-membership)
                           (:tenant_memberships/tenant_id target-membership))
        target-user-id (or (:user_id target-membership)
                         (:tenant_memberships/user_id target-membership))
        now       (java.time.LocalDateTime/now)]
    (jdbc/with-transaction [tx db]
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
                                               :join   [[:payer_types :pt] [:= :pt.id :p.payer_type_id]]
                                               :where  [:and
                                                        [:= :p.tenant_id target-tenant-id]
                                                        [:= :pt.is_system true]
                                                        [:= :p.is_active true]]})
                                  {:builder-fn rs/as-unqualified-lower-maps})
                  ;; Find payers that belong to this user (by matching label or via user_expense_settings)
                  user-settings (jdbc/execute-one! tx
                                  (sql/format {:select [:default_payer_id]
                                               :from   [:user_expense_settings]
                                               :where  [:and
                                                        [:= :tenant_id target-tenant-id]
                                                        [:= :user_id target-user-id]]})
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
                                     [:= :user_id target-user-id]]})))
            (catch Exception e
              (log/warn e "Failed to cleanup payer/settings on member removal"
                {:tenant-id target-tenant-id :user-id target-user-id}))))
        result))))

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
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_memberships]
                     :set    {:status     [:cast "active" :membership_status]
                              :updated_at now}
                     :where  [:= :id target-id]
                     :returning [:*]})))))

(comment
  ;; (require 'app.template.backend.services.member :reload)
  :rcf)
