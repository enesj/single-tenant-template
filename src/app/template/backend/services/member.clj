(ns app.template.backend.services.member
  "Tenant member management — role changes, ownership transfer, removal."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
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

;; ============================================================================
;; Ownership Transfer
;; ============================================================================

(defn transfer-ownership!
  "Transfer ownership from actor (must be owner) to target (must be admin).
   Atomically swaps: target → owner, actor → admin."
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
      ;; Target → owner
      (jdbc/execute-one! tx
        (sql/format {:update [:tenant_memberships]
                     :set    {:role [:cast "owner" :membership_role] :updated_at now}
                     :where  [:= :id target-id]}))
      ;; Actor → admin
      (let [updated-actor (convert-pg-objects
                            (jdbc/execute-one! tx
                              (sql/format {:update [:tenant_memberships]
                                           :set    {:role [:cast "admin" :membership_role] :updated_at now}
                                           :where  [:= :id actor-id]
                                           :returning [:*]})))]
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
        now       (java.time.LocalDateTime/now)]
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:update [:tenant_memberships]
                     :set    {:status     [:cast "suspended" :membership_status]
                              :updated_at now}
                     :where  [:= :id target-id]
                     :returning [:*]})))))

(comment
  ;; (require 'app.template.backend.services.member :reload)
  :rcf)
