(ns app.template.backend.routes.tenant
  "Tenant management API routes mounted at /api/v1/tenant.
   All routes require user authentication."
  (:require
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.impersonation :as impersonation-routes]
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.invitation :as invitation-svc]
    [app.template.backend.services.invitation-email :as invitation-email]
    [app.template.backend.services.member :as member-svc]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log])
  (:import
    [java.net URLEncoder]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- sanitize
  "Walk a data structure and stringify Java types for JSON serialization."
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? java.util.UUID x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.ZonedDateTime x) (str x)
        (instance? java.time.LocalDate x) (str x)
        (instance? java.time.LocalTime x) (str x)
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.sql.Date x) (str x)
        (instance? java.math.BigDecimal x) (str x)
        (instance? java.math.BigInteger x) (str x)
        :else x))
    obj))

(defn- build-accept-url [base-url token]
  (str base-url "/invitation/accept?token=" (URLEncoder/encode token "UTF-8")))

(defn- get-user [req]
  (or (get-in req [:session :auth-session :user])
    (get-in req [:session :user])))

(defn- get-tenant [req]
  (get-in req [:session :auth-session :tenant]))

(defn- require-role!
  "Throw 403 if the membership role is not in `allowed-roles`."
  [membership allowed-roles]
  (let [role (or (:role membership) (:tenant_memberships/role membership))]
    (when-not (contains? (set allowed-roles) role)
      (throw (ex-info "Insufficient permissions"
               {:type :forbidden
                :errors {:role [(str "Requires one of: " (pr-str allowed-roles))]}}))))
  membership)

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- switch-tenant-handler
  "POST /tenant/switch — switch the active tenant in the session."
  [db config]
  (fn [req]
    (route-utils/with-error-handling "tenant-switch"
      (let [user      (get-user req)
            tenant-id (get-in req [:body-params :tenant-id])]
        (when-not tenant-id
          (throw (ex-info "Missing tenant-id" {:type :validation-error
                                               :errors {:tenant-id ["tenant-id is required"]}})))
        ;; Verify the user has an active membership in the target tenant
        (let [membership (tenant-svc/get-membership db tenant-id (:id user))]
          (when-not membership
            (throw (ex-info "Not a member of this tenant"
                     {:type :forbidden
                      :errors {:tenant-id ["You are not a member of this tenant"]}})))
          (let [tenant       (tenant-svc/find-tenant-by-id db tenant-id)
                auth-session (sanitize
                               (tenant-auth/build-auth-session
                                 {:user user}
                                 {:tenant tenant :membership membership :action :auto-set}))
                existing     (or (:session req) {})]
            (-> (response/response {:success true
                                    :tenant (sanitize tenant)})
              (assoc :session (assoc existing :auth-session auth-session)))))))))

(defn- list-memberships-handler
  "GET /tenant/memberships — list the current user's tenants."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-memberships"
      (let [user (get-user req)
            memberships (tenant-svc/get-user-memberships db (:id user))]
        (response/response {:memberships (sanitize memberships)})))))

(defn- list-members-handler
  "GET /tenant/members — list members of the active tenant.

   Owners/admins see suspended memberships; everyone else sees active only."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-members"
      (let [user   (get-user req)
            tenant (get-tenant req)]
        (when-not tenant
          (throw (ex-info "No active tenant" {:type :validation-error
                                              :errors {:tenant ["No active tenant in session"]}})))
        (let [tenant-id (or (:id tenant) (:tenants/id tenant))
              actor-m   (tenant-svc/get-membership db tenant-id (:id user))]
          (when-not actor-m
            (throw (ex-info "Not a member of this tenant"
                     {:type :forbidden
                      :errors {:tenant ["You are not a member of this tenant"]}})))
          (let [actor-role (or (:role actor-m) (:tenant_memberships/role actor-m))
                include-suspended? (contains? #{"owner" "admin"} (str actor-role))
                members (tenant-svc/get-tenant-members db tenant-id
                          {:include-suspended? include-suspended?})]
            (response/response {:members (sanitize members)})))))))

(defn- create-invitation-handler
  "POST /tenant/invitations — invite a user to the current tenant."
  [db email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "tenant-invite"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant" {:type :validation-error
                                                            :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            ;; Verify actor has admin/owner role
            actor-m   (tenant-svc/get-membership db tenant-id (:id user))
            _         (require-role! actor-m ["owner" "admin"])
            {:keys [email role]} (:body-params req)
            invitation (invitation-svc/create-invitation!
                         db {:tenant-id  tenant-id
                             :email      email
                             :role       (or role "member")
                             :invited-by (:id user)})]
        ;; Send invitation email asynchronously (best-effort, must not block HTTP response)
        (if email-service
          (let [token      (or (:token invitation) (:tenant_invitations/token invitation))
                accept-url (build-accept-url base-url token)]
            (future
              (try
                (invitation-email/send-invitation-email!
                  email-service
                  {:to-email     email
                   :inviter-name (or (:full_name user) (:email user))
                   :tenant-name  (or (:name tenant) (:tenants/name tenant))
                   :accept-url   accept-url
                   :role         (or role "member")})
                (catch Exception e
                  (log/warn "Failed to send invitation email:" (.getMessage e))))))
          (log/warn "Email service not configured — invitation email NOT sent" {:to-email email}))
        (response/response {:success true :invitation (sanitize invitation)})))))

(defn- list-invitations-handler
  "GET /tenant/invitations — list pending invitations."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-invitations-list"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant" {:type :validation-error
                                                            :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            actor-m   (tenant-svc/get-membership db tenant-id (:id user))
            _         (require-role! actor-m ["owner" "admin"])
            invitations (invitation-svc/list-pending-invitations db tenant-id)]
        (response/response {:invitations (sanitize invitations)})))))

(defn- accept-invitation-handler
  "POST /tenant/invitations/accept — accept an invitation by token."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-invitation-accept"
      (let [user  (get-user req)
            token (get-in req [:body-params :token])]
        (when-not token
          (throw (ex-info "Missing token" {:type :validation-error
                                           :errors {:token ["token is required"]}})))
        (let [invitation (invitation-svc/find-invitation-by-token db token)]
          (when-not invitation
            (throw (ex-info "Invalid invitation token"
                     {:type :validation-error
                      :errors {:token ["This invitation is not valid"]}})))
          (let [membership (invitation-svc/accept-invitation! db user invitation)
                ;; Auto-switch to the newly joined tenant
                tenant-id  (or (:tenant_id invitation) (:tenant_invitations/tenant_id invitation))
                tenant     (tenant-svc/find-tenant-by-id db tenant-id)
                auth-session (sanitize
                               (tenant-auth/build-auth-session
                                 {:user user}
                                 {:tenant tenant :membership membership :action :auto-set}))
                existing   (or (:session req) {})]
            (-> (response/response {:success true
                                    :tenant (sanitize tenant)
                                    :membership (sanitize membership)})
              (assoc :session (assoc existing :auth-session auth-session)))))))))

(defn- resend-invitation-handler
  "POST /tenant/invitations/:id/resend — resend a pending invitation email."
  [db email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "tenant-invitation-resend"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant" {:type :validation-error
                                                            :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            actor-m   (tenant-svc/get-membership db tenant-id (:id user))
            _         (require-role! actor-m ["owner" "admin"])
            inv-id    (get-in req [:path-params :id])
            invitation (invitation-svc/resend-invitation! db inv-id)]
        ;; Re-send the email asynchronously (must not block HTTP response)
        (if email-service
          (let [token      (:token invitation)
                accept-url (build-accept-url base-url token)]
            (future
              (try
                (invitation-email/send-invitation-email!
                  email-service
                  {:to-email     (:email invitation)
                   :inviter-name (or (:full_name user) (:email user))
                   :tenant-name  (or (:name tenant) (:tenants/name tenant))
                   :accept-url   accept-url
                   :role         (:role invitation)})
                (catch Exception e
                  (log/warn "Failed to resend invitation email:" (.getMessage e))))))
          (log/warn "Email service not configured — invitation email NOT resent" {:to-email (:email invitation)}))
        (response/response {:success true :invitation (sanitize invitation)})))))

(defn- revoke-invitation-handler
  "DELETE /tenant/invitations/:id — revoke a pending invitation."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-invitation-revoke"
      (let [user      (get-user req)
            tenant    (get-tenant req)
            _         (when-not tenant
                        (throw (ex-info "No active tenant" {:type :validation-error
                                                            :errors {:tenant ["No active tenant in session"]}})))
            tenant-id (or (:id tenant) (:tenants/id tenant))
            actor-m   (tenant-svc/get-membership db tenant-id (:id user))
            _         (require-role! actor-m ["owner" "admin"])
            inv-id    (get-in req [:path-params :id])]
        (invitation-svc/revoke-invitation! db inv-id)
        (response/response {:success true})))))

(defn- change-role-handler
  "PUT /tenant/members/:id/role — change a member's role."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-change-role"
      (let [user       (get-user req)
            tenant     (get-tenant req)
            _          (when-not tenant
                         (throw (ex-info "No active tenant" {:type :validation-error
                                                             :errors {:tenant ["No active tenant in session"]}})))
            tenant-id  (or (:id tenant) (:tenants/id tenant))
            actor-m    (tenant-svc/get-membership db tenant-id (:id user))
            _          (require-role! actor-m ["owner" "admin"])
            target-id  (get-in req [:path-params :id])
            new-role   (get-in req [:body-params :role])
            target-m   (tenant-svc/get-membership db tenant-id
                         ;; target-id is the membership id, but we need to find by tenant+user
                         ;; Actually the plan says :id is the membership id, so look it up directly
                         nil)]
        ;; We need to find the target membership by its id
        ;; Since get-membership is by tenant+user, let's query directly
        (let [target-membership (first (filter #(= (str (or (:id %) (:tenant_memberships/id %))) (str target-id))
                                         (tenant-svc/get-tenant-members db tenant-id)))]
          (when-not target-membership
            (throw (ex-info "Member not found" {:type :entity-not-found})))
          (let [result (member-svc/change-role! db {:actor-membership  actor-m
                                                    :target-membership target-membership
                                                    :new-role          new-role})]
            (response/response {:success true :membership (sanitize result)})))))))

(defn- transfer-ownership-handler
  "POST /tenant/transfer-ownership — transfer ownership to an admin."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-transfer-ownership"
      (let [user       (get-user req)
            tenant     (get-tenant req)
            _          (when-not tenant
                         (throw (ex-info "No active tenant" {:type :validation-error
                                                             :errors {:tenant ["No active tenant in session"]}})))
            tenant-id  (or (:id tenant) (:tenants/id tenant))
            actor-m    (tenant-svc/get-membership db tenant-id (:id user))
            target-user-id (get-in req [:body-params :user-id])
            target-m   (tenant-svc/get-membership db tenant-id target-user-id)]
        (when-not target-m
          (throw (ex-info "Target user is not a member" {:type :entity-not-found})))
        (let [result (member-svc/transfer-ownership! db {:actor-membership  actor-m
                                                         :target-membership target-m})]
          (response/response {:success true :membership (sanitize result)}))))))

(defn- remove-member-handler
  "DELETE /tenant/members/:id — remove a member from the tenant."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-remove-member"
      (let [user       (get-user req)
            tenant     (get-tenant req)
            _          (when-not tenant
                         (throw (ex-info "No active tenant" {:type :validation-error
                                                             :errors {:tenant ["No active tenant in session"]}})))
            tenant-id  (or (:id tenant) (:tenants/id tenant))
            actor-m    (tenant-svc/get-membership db tenant-id (:id user))
            _          (require-role! actor-m ["owner" "admin"])
            target-id  (get-in req [:path-params :id])
            ;; Find target membership from members list
            target-membership (first (filter #(= (str (or (:id %) (:tenant_memberships/id %))) (str target-id))
                                       (tenant-svc/get-tenant-members db tenant-id)))]
        (when-not target-membership
          (throw (ex-info "Member not found" {:type :entity-not-found})))
        (let [result (member-svc/remove-member! db {:actor-membership  actor-m
                                                    :target-membership target-membership})]
          (response/response {:success true :membership (sanitize result)}))))))

(defn- set-member-status-handler
  "PUT /tenant/members/:id/status — set a member's membership status.

   Currently supports re-enabling a suspended membership (status = \"active\")."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-set-member-status"
      (let [user   (get-user req)
            tenant (get-tenant req)]
        (when-not tenant
          (throw (ex-info "No active tenant" {:type :validation-error
                                              :errors {:tenant ["No active tenant in session"]}})))
        (let [tenant-id (or (:id tenant) (:tenants/id tenant))
              actor-m   (tenant-svc/get-membership db tenant-id (:id user))
              _         (require-role! actor-m ["owner" "admin"])
              member-id (get-in req [:path-params :id])
              new-status-raw (get-in req [:body-params :status])
              new-status (cond
                           (keyword? new-status-raw) (name new-status-raw)
                           (string? new-status-raw) new-status-raw
                           (some? new-status-raw) (str new-status-raw)
                           :else nil)]
          (when-not (= "active" new-status)
            (throw (ex-info "Invalid status"
                     {:type :validation-error
                      :errors {:status ["Only 'active' is supported"]}})))
          (let [target-membership (first
                                    (filter #(= (str (or (:id %) (:tenant_memberships/id %))) (str member-id))
                                      (tenant-svc/get-tenant-members db tenant-id
                                        {:include-suspended? true})))]
            (when-not target-membership
              (throw (ex-info "Member not found" {:type :entity-not-found})))
            (let [result (member-svc/reinstate-member! db
                           {:actor-membership  actor-m
                            :target-membership target-membership})]
              (response/response {:success true
                                  :membership (sanitize result)}))))))))

;; ============================================================================
;; Route Tree
;; ============================================================================

;; ---------------------------------------------------------------------------
;; Tenant settings handlers (Phase 2 — settings hierarchy)
;; ---------------------------------------------------------------------------

(defn- update-tenant-settings-handler
  "PUT /tenant/settings — update tenant-level settings (owner/admin only).
   Currently supports: email-notifications (boolean)."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-update-settings"
      (let [user   (get-user req)
            tenant (get-tenant req)]
        (when-not tenant
          (throw (ex-info "No active tenant" {:type :validation-error
                                              :errors {:tenant ["No active tenant in session"]}})))
        (let [tenant-id (or (:id tenant) (:tenants/id tenant))
              actor-m   (tenant-svc/get-membership db tenant-id (:id user))
              _         (require-role! actor-m ["owner" "admin"])
              body      (:body-params req)
              ;; Normalize body keys
              updates   (cond-> {}
                          (contains? body :email-notifications)
                          (assoc :email-notifications (:email-notifications body))
                          (contains? body :email_notifications)
                          (assoc :email-notifications (:email_notifications body))
                          (contains? body (keyword "emailNotifications"))
                          (assoc :email-notifications (get body (keyword "emailNotifications"))))]
          (when (empty? updates)
            (throw (ex-info "No settings fields provided"
                     {:type :validation-error
                      :errors {:body ["Provide at least one setting to update"]}})))
          ;; Lazy-require domain service to avoid template→domain compile-time coupling
          (let [update-fn! (requiring-resolve 'app.domain.backend.expenses.services.tenant-settings/update-tenant-settings!)
                result     (update-fn! db tenant-id updates)]
            (log/info "Updated tenant settings" {:tenant-id tenant-id :keys (keys updates)})
            (response/response {:success true :settings (sanitize result)})))))))

(defn- update-tenant-name-handler
  "PUT /tenant/name — rename the workspace (owner only)."
  [db]
  (fn [req]
    (route-utils/with-error-handling "tenant-update-name"
      (let [user   (get-user req)
            tenant (get-tenant req)]
        (when-not tenant
          (throw (ex-info "No active tenant" {:type :validation-error
                                              :errors {:tenant ["No active tenant in session"]}})))
        (let [tenant-id (or (:id tenant) (:tenants/id tenant))
              actor-m   (tenant-svc/get-membership db tenant-id (:id user))
              _         (require-role! actor-m ["owner"])
              body      (:body-params req)
              new-name  (or (:name body) (get body "name"))]
          (when (or (nil? new-name) (clojure.string/blank? new-name))
            (throw (ex-info "Name is required"
                     {:type :validation-error
                      :errors {:name ["Workspace name cannot be blank"]}})))
          (let [updated (tenant-svc/update-tenant! db tenant-id {:name new-name})]
            (log/info "Updated tenant name" {:tenant-id tenant-id :name new-name})
            (response/response {:success true :tenant (sanitize updated)})))))))

(defn tenant-routes
  "Create tenant management routes. All protected by `wrap-user-auth`."
  [db wrap-user-auth email-service base-url config]
  ["/tenant"
   {:middleware [wrap-user-auth]}

   ["/switch"       {:post {:handler (switch-tenant-handler db config)}}]
   ["/memberships"  {:get  {:handler (list-memberships-handler db)}}]
   ["/members"      {:get {:handler (list-members-handler db)}}]
   ["/members/:id"       {:delete {:handler (remove-member-handler db)}}]
   ["/members/:id/status" {:put {:handler (set-member-status-handler db)}}]
   ["/members/:id/role"  {:put    {:handler (change-role-handler db)}}]
   ["/invitations"       {:get  {:handler (list-invitations-handler db)}
                          :post {:handler (create-invitation-handler db email-service base-url)}}]
   ["/invitations/accept" {:post {:handler (accept-invitation-handler db)}}]
   ["/invitations/:id"    {:delete {:handler (revoke-invitation-handler db)}}]
   ["/invitations/:id/resend" {:post {:handler (resend-invitation-handler db email-service base-url)}}]
   ["/transfer-ownership" {:post {:handler (transfer-ownership-handler db)}}]

   ;; Tenant-level settings (Phase 2 — settings hierarchy)
   ["/settings" {:put {:handler (update-tenant-settings-handler db)}}]
   ["/name"     {:put {:handler (update-tenant-name-handler db)}}]

   (impersonation-routes/routes db)])

(comment
  ;; (require 'app.template.backend.routes.tenant :reload)
  :rcf)
