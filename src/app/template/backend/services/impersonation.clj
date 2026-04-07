(ns app.template.backend.services.impersonation
  "Impersonation grant lifecycle — create, revoke, activate/deactivate.

   Platform admins need an active impersonation grant from a tenant owner
   before they can access tenant-scoped resources in the admin panel.
   All grant operations are audit-logged."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.security.email :as email-privacy]
    [cheshire.core :as json]
    [honey.sql :as sql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- uuid-cast [id]
  (if (string? id) [:cast id :uuid] id))

;; ============================================================================
;; Grant Queries
;; ============================================================================

(defn find-active-grant
  "Find an active grant by ID."
  [db grant-id]
  (convert-pg-objects
    (jdbc/execute-one! db
      (sql/format {:select [:*]
                   :from   [:impersonation_grants]
                   :where  [:and
                            [:= :id (uuid-cast grant-id)]
                            [:= :status [:cast "active" :impersonation_status]]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-active-grant-for-admin-tenant
  "Find the active grant for a specific admin+tenant pair."
  [db admin-id tenant-id]
  (convert-pg-objects
    (jdbc/execute-one! db
      (sql/format {:select [:*]
                   :from   [:impersonation_grants]
                   :where  [:and
                            [:= :admin_id (uuid-cast admin-id)]
                            [:= :tenant_id (uuid-cast tenant-id)]
                            [:= :status [:cast "active" :impersonation_status]]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-grants-for-tenant
  "List all grants (active + revoked) for a tenant, joined with admin info."
  [db tenant-id]
  (->> (jdbc/execute! db
         (sql/format {:select [:ig.*
                               [:a.email_ciphertext :admin_email_ciphertext]
                               [:a.full_name :admin_name]]
                      :from   [[:impersonation_grants :ig]]
                      :join   [[:admins :a] [:= :ig.admin_id :a.id]]
                      :where  [:= :ig.tenant_id (uuid-cast tenant-id)]
                      :order-by [[:ig.created_at :desc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv convert-pg-objects)
    (mapv (fn [grant]
            (let [admin-email (some-> (:admin_email_ciphertext grant)
                                email-privacy/decrypt-email)]
              (cond-> grant
                admin-email (assoc :admin_email admin-email)))))))

(defn list-active-grants-for-admin
  "List active grants for an admin, joined with tenant info."
  [db admin-id]
  (mapv convert-pg-objects
    (jdbc/execute! db
      (sql/format {:select [:ig.*
                            [:t.name :tenant_name]
                            [:t.slug :tenant_slug]]
                   :from   [[:impersonation_grants :ig]]
                   :join   [[:tenants :t] [:= :ig.tenant_id :t.id]]
                   :where  [:and
                            [:= :ig.admin_id (uuid-cast admin-id)]
                            [:= :ig.status [:cast "active" :impersonation_status]]]
                   :order-by [[:ig.created_at :desc]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ============================================================================
;; Impersonation Session Management
;; ============================================================================

(defn activate-impersonation!
  "Set the impersonation context on the admin's session.

   Validates:
   - Grant is active
   - Grant belongs to the requesting admin"
  [db admin-id grant-id]
  (let [grant (find-active-grant db grant-id)]
    (when-not grant
      (throw (ex-info "Grant not found or revoked"
               {:type :entity-not-found
                :errors {:grant ["No active grant with this ID"]}})))
    (when-not (= (str (:admin_id grant)) (str admin-id))
      (throw (ex-info "Grant does not belong to this admin"
               {:type :forbidden
                :errors {:grant ["This grant belongs to a different admin"]}})))
    (let [context {:tenant-id (str (:tenant_id grant))
                   :role      (str (:role grant))
                   :grant-id  (str (:id grant))}
          context-json (json/generate-string context)]
      (jdbc/execute-one! db
        (sql/format {:update :admin_sessions
                     :set    {:impersonation_context [:cast context-json :jsonb]}
                     :where  [:= :admin_id (uuid-cast admin-id)]}))
      (audit/log-audit! db {:admin_id    admin-id
                            :action      "impersonation_activated"
                            :entity-type "impersonation_grant"
                            :entity-id   (:id grant)
                            :changes     context})
      context)))

(defn deactivate-impersonation!
  "Clear impersonation context from all sessions of the given admin."
  [db admin-id]
  (jdbc/execute-one! db
    (sql/format {:update :admin_sessions
                 :set    {:impersonation_context nil}
                 :where  [:= :admin_id (uuid-cast admin-id)]}))
  (audit/log-audit! db {:admin_id    admin-id
                        :action      "impersonation_deactivated"
                        :entity-type "admin_session"
                        :entity-id   admin-id
                        :changes     {}})
  nil)

;; ============================================================================
;; Grant Mutations
;; ============================================================================

(defn create-grant!
  "Create an impersonation grant for an admin on a tenant.

   Guards: rejects if an active grant already exists for the same admin+tenant.
   Requires `granted-by` (the tenant owner's user-id)."
  [db {:keys [admin-email tenant-id role granted-by]}]
  (let [admin (admin-auth/find-admin-by-email db admin-email)]
    (when-not admin
      (throw (ex-info "Admin not found"
               {:type :entity-not-found
                :errors {:admin-email ["No admin with this email"]}})))
    (let [admin-id (:id admin)
          existing (find-active-grant-for-admin-tenant db admin-id tenant-id)]
      (when existing
        (throw (ex-info "Active grant already exists for this admin and tenant"
                 {:type :conflict
                  :errors {:grant ["An active impersonation grant already exists"]}})))
      (let [grant-id (UUID/randomUUID)
            now      (time/instant)
            grant    (convert-pg-objects
                       (jdbc/execute-one! db
                         (sql/format {:insert-into :impersonation_grants
                                      :values [{:id         grant-id
                                                :tenant_id  (uuid-cast tenant-id)
                                                :admin_id   admin-id
                                                :granted_by (uuid-cast granted-by)
                                                :role       [:cast (or role "viewer") :impersonation_role]
                                                :status     [:cast "active" :impersonation_status]
                                                :created_at now
                                                :updated_at now}]
                                      :returning [:*]})
                         {:builder-fn rs/as-unqualified-lower-maps}))]
        (audit/log-audit! db {:user_id   granted-by
                              :action    "impersonation_granted"
                              :entity-type "impersonation_grant"
                              :entity-id (:id grant)
                              :changes   {:admin-id    (str admin-id)
                                          :admin-email admin-email
                                          :tenant-id   (str tenant-id)
                                          :role        (or role "viewer")}})
        grant))))

(defn revoke-grant-by-owner!
  "Revoke a grant (called by tenant owner). Also clears any active impersonation session."
  [db grant-id user-id]
  (let [now   (time/instant)
        grant (find-active-grant db grant-id)]
    (when-not grant
      (throw (ex-info "Grant not found or already revoked"
               {:type :entity-not-found
                :errors {:grant ["No active grant with this ID"]}})))
    (jdbc/execute-one! db
      (sql/format {:update :impersonation_grants
                   :set    {:status          [:cast "revoked" :impersonation_status]
                            :revoked_by_user (uuid-cast user-id)
                            :updated_at      now}
                   :where  [:= :id (uuid-cast grant-id)]}))
    (deactivate-impersonation! db (:admin_id grant))
    (audit/log-audit! db {:user_id     user-id
                          :action      "impersonation_revoked_by_owner"
                          :entity-type "impersonation_grant"
                          :entity-id   (:id grant)
                          :changes     {:admin-id  (str (:admin_id grant))
                                        :tenant-id (str (:tenant_id grant))}})
    grant))

(defn revoke-grant-by-admin!
  "Revoke a grant (called by the admin themselves). Also clears impersonation session."
  [db grant-id admin-id]
  (let [now   (time/instant)
        grant (find-active-grant db grant-id)]
    (when-not grant
      (throw (ex-info "Grant not found or already revoked"
               {:type :entity-not-found
                :errors {:grant ["No active grant with this ID"]}})))
    (when-not (= (str (:admin_id grant)) (str admin-id))
      (throw (ex-info "Cannot revoke another admin's grant"
               {:type :forbidden
                :errors {:grant ["This grant belongs to a different admin"]}})))
    (jdbc/execute-one! db
      (sql/format {:update :impersonation_grants
                   :set    {:status           [:cast "revoked" :impersonation_status]
                            :revoked_by_admin (uuid-cast admin-id)
                            :updated_at       now}
                   :where  [:= :id (uuid-cast grant-id)]}))
    (deactivate-impersonation! db admin-id)
    (audit/log-audit! db {:admin_id    admin-id
                          :action      "impersonation_revoked_by_admin"
                          :entity-type "impersonation_grant"
                          :entity-id   (:id grant)
                          :changes     {:tenant-id (str (:tenant_id grant))}})
    grant))

(comment
  ;; (require 'app.template.backend.services.impersonation :reload)
  :rcf)
