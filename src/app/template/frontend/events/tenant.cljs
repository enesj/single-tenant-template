(ns app.template.frontend.events.tenant
  "Re-frame events and subscriptions for tenant operations:
   switching, memberships, members, invitations, ownership transfer."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.auth.ids :as auth-ids]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :tenant/memberships
  (fn [db _]
    (get-in db [:tenant :memberships])))

(rf/reg-sub
  :tenant/members
  (fn [db _]
    (get-in db [:tenant :members])))

(rf/reg-sub
  :tenant/invitations
  (fn [db _]
    (get-in db [:tenant :invitations])))

(rf/reg-sub
  :tenant/loading?
  (fn [db _]
    (get-in db [:tenant :loading?] false)))

(rf/reg-sub
  :tenant/members-loading?
  (fn [db _]
    (get-in db [:tenant :members-loading?] false)))

(rf/reg-sub
  :tenant/error
  (fn [db _]
    (get-in db [:tenant :error])))

(rf/reg-sub
  :tenant/success-message
  (fn [db _]
    (get-in db [:tenant :success-message])))

(rf/reg-sub
  :tenant/accept-token
  (fn [db _]
    (get-in db [:tenant :accept-token])))

(rf/reg-sub
  :tenant/url-slug
  (fn [db _]
    (get-in db [:tenant :url-slug])))

;; ============================================================================
;; Switch Tenant
;; ============================================================================

(rf/reg-event-fx
  ::switch-tenant
  common-interceptors
  (fn [{:keys [db]} [{:keys [tenant-id]}]]
    {:db (-> db
           (assoc-in [:tenant :loading?] true)
           (assoc-in [:tenant :error] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/tenant/switch"
                    :params {:tenant-id tenant-id}
                    :on-success [::switch-tenant-success]
                    :on-failure [::switch-tenant-failure]})}))

(rf/reg-event-fx
  ::switch-tenant-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [tenant (:tenant response)
          slug (or (:slug tenant) (:tenants/slug tenant))]
      (log/info "Switched to tenant:" (:name tenant))
      {:db (-> db
             (assoc-in [:tenant :loading?] false)
             (assoc-in [:tenant :url-slug] slug)
             (assoc-in [:session :tenant] tenant)
             (assoc-in [:session :tenant-selection-required] nil)
             (assoc-in [:session :available-tenants] nil))
       :fx [;; Re-fetch auth status to get updated membership-role and permissions
            [:dispatch [auth-ids/fetch-auth-status]]
            ;; Navigate to dashboard with slug prefix
            [:dispatch [:navigate-to (if slug (str "/t/" slug "/dashboard") "/dashboard")]]]})))

(rf/reg-event-db
  ::switch-tenant-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to switch tenant:" response)
    (-> db
      (assoc-in [:tenant :loading?] false)
      (assoc-in [:tenant :error]
        (or (get-in response [:response :message])
          "Failed to switch tenant")))))

;; ============================================================================
;; Fetch Memberships (user's tenants for switcher)
;; ============================================================================

(rf/reg-event-fx
  ::fetch-memberships
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/tenant/memberships"
                    :on-success [::fetch-memberships-success]
                    :on-failure [::fetch-memberships-failure]})}))

(rf/reg-event-db
  ::fetch-memberships-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:tenant :loading?] false)
      (assoc-in [:tenant :memberships] (:memberships response)))))

(rf/reg-event-db
  ::fetch-memberships-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to fetch memberships:" response)
    (-> db
      (assoc-in [:tenant :loading?] false)
      (assoc-in [:tenant :error] "Failed to fetch memberships"))))

;; ============================================================================
;; Fetch Members (current tenant's members)
;; ============================================================================

(rf/reg-event-fx
  ::fetch-members
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :members-loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/tenant/members"
                    :on-success [::fetch-members-success]
                    :on-failure [::fetch-members-failure]})}))

(rf/reg-event-db
  ::fetch-members-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:tenant :members-loading?] false)
      (assoc-in [:tenant :members] (:members response)))))

(rf/reg-event-db
  ::fetch-members-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to fetch members:" response)
    (-> db
      (assoc-in [:tenant :members-loading?] false)
      (assoc-in [:tenant :error] "Failed to fetch members"))))

;; ============================================================================
;; Fetch Invitations
;; ============================================================================

(rf/reg-event-fx
  ::fetch-invitations
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :invitations-loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/api/v1/tenant/invitations"
                    :on-success [::fetch-invitations-success]
                    :on-failure [::fetch-invitations-failure]})}))

(rf/reg-event-db
  ::fetch-invitations-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:tenant :invitations-loading?] false)
      (assoc-in [:tenant :invitations] (:invitations response)))))

(rf/reg-event-db
  ::fetch-invitations-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to fetch invitations:" response)
    (-> db
      (assoc-in [:tenant :invitations-loading?] false)
      (assoc-in [:tenant :error] "Failed to fetch invitations"))))

;; ============================================================================
;; Invite Member
;; ============================================================================

(rf/reg-event-fx
  ::invite-member
  common-interceptors
  (fn [{:keys [db]} [{:keys [email role]}]]
    {:db (-> db
           (assoc-in [:tenant :invite-loading?] true)
           (assoc-in [:tenant :error] nil)
           (assoc-in [:tenant :success-message] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/tenant/invitations"
                    :params {:email email :role (or role "member")}
                    :on-success [::invite-member-success]
                    :on-failure [::invite-member-failure]})}))

(rf/reg-event-fx
  ::invite-member-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (-> db
           (assoc-in [:tenant :invite-loading?] false)
           (assoc-in [:tenant :success-message] "Invitation sent successfully"))
     :fx [[:dispatch [::fetch-invitations]]]}))

(rf/reg-event-db
  ::invite-member-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to invite member:" response)
    (-> db
      (assoc-in [:tenant :invite-loading?] false)
      (assoc-in [:tenant :error]
        (or (get-in response [:response :message])
          (get-in response [:response :errors :email 0])
          "Failed to send invitation")))))

;; ============================================================================
;; Revoke Invitation
;; ============================================================================

(rf/reg-event-fx
  ::revoke-invitation
  common-interceptors
  (fn [{:keys [db]} [{:keys [id]}]]
    {:db (assoc-in db [:tenant :error] nil)
     :http-xhrio (http/api-request
                   {:method :delete
                    :uri (str "/api/v1/tenant/invitations/" id)
                    :on-success [::revoke-invitation-success]
                    :on-failure [::revoke-invitation-failure]})}))

(rf/reg-event-fx
  ::revoke-invitation-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Invitation revoked")
     :fx [[:dispatch [::fetch-invitations]]]}))

(rf/reg-event-db
  ::revoke-invitation-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to revoke invitation:" response)
    (assoc-in db [:tenant :error] "Failed to revoke invitation")))

;; ============================================================================
;; Resend Invitation
;; ============================================================================

(rf/reg-event-fx
  ::resend-invitation
  common-interceptors
  (fn [{:keys [db]} [{:keys [id]}]]
    {:db (-> db
           (assoc-in [:tenant :error] nil)
           (assoc-in [:tenant :success-message] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri (str "/api/v1/tenant/invitations/" id "/resend")
                    :params {}
                    :on-success [::resend-invitation-success]
                    :on-failure [::resend-invitation-failure]})}))

(rf/reg-event-fx
  ::resend-invitation-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Invitation resent successfully")
     :fx [[:dispatch [::fetch-invitations]]]}))

(rf/reg-event-db
  ::resend-invitation-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to resend invitation:" response)
    (assoc-in db [:tenant :error]
              (or (get-in response [:response :message])
                "Failed to resend invitation"))))

;; ============================================================================
;; Change Member Role
;; ============================================================================

(rf/reg-event-fx
  ::change-member-role
  common-interceptors
  (fn [{:keys [db]} [{:keys [member-id role]}]]
    {:db (assoc-in db [:tenant :error] nil)
     :http-xhrio (http/api-request
                   {:method :put
                    :uri (str "/api/v1/tenant/members/" member-id "/role")
                    :params {:role role}
                    :on-success [::change-member-role-success]
                    :on-failure [::change-member-role-failure]})}))

(rf/reg-event-fx
  ::change-member-role-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Role updated")
     :fx [[:dispatch [::fetch-members]]]}))

(rf/reg-event-db
  ::change-member-role-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to change role:" response)
    (assoc-in db [:tenant :error]
              (or (get-in response [:response :message])
                "Failed to change role"))))

;; ============================================================================
;; Remove Member
;; ============================================================================

(rf/reg-event-fx
  ::remove-member
  common-interceptors
  (fn [{:keys [db]} [{:keys [member-id]}]]
    {:db (assoc-in db [:tenant :error] nil)
     :http-xhrio (http/api-request
                   {:method :delete
                    :uri (str "/api/v1/tenant/members/" member-id)
                    :on-success [::remove-member-success]
                    :on-failure [::remove-member-failure]})}))

(rf/reg-event-fx
  ::remove-member-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Member disabled")
     :fx [[:dispatch [::fetch-members]]]}))

(rf/reg-event-db
  ::remove-member-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to disable member:" response)
    (assoc-in db [:tenant :error]
              (or (get-in response [:response :message])
                "Failed to disable member"))))

(rf/reg-event-fx
  ::set-member-status
  common-interceptors
  (fn [{:keys [db]} [{:keys [member-id status]}]]
    {:db (assoc-in db [:tenant :error] nil)
     :http-xhrio (http/api-request
                   {:method :put
                    :uri (str "/api/v1/tenant/members/" member-id "/status")
                    :params {:status status}
                    :on-success [::set-member-status-success]
                    :on-failure [::set-member-status-failure]})}))

(rf/reg-event-fx
  ::set-member-status-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Member enabled")
     :fx [[:dispatch [::fetch-members]]]}))

(rf/reg-event-db
  ::set-member-status-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to enable member:" response)
    (assoc-in db [:tenant :error]
              (or (get-in response [:response :message])
                "Failed to enable member"))))

;; ============================================================================
;; Transfer Ownership
;; ============================================================================

(rf/reg-event-fx
  ::transfer-ownership
  common-interceptors
  (fn [{:keys [db]} [{:keys [user-id]}]]
    {:db (assoc-in db [:tenant :error] nil)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/tenant/transfer-ownership"
                    :params {:user-id user-id}
                    :on-success [::transfer-ownership-success]
                    :on-failure [::transfer-ownership-failure]})}))

(rf/reg-event-fx
  ::transfer-ownership-success
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:tenant :success-message] "Ownership transferred")
     :fx [[:dispatch [::fetch-members]]
          [:dispatch [auth-ids/fetch-auth-status]]]}))

(rf/reg-event-db
  ::transfer-ownership-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to transfer ownership:" response)
    (assoc-in db [:tenant :error]
              (or (get-in response [:response :message])
                "Failed to transfer ownership"))))

;; ============================================================================
;; Accept Invitation
;; ============================================================================

(rf/reg-event-fx
  ::accept-invitation-init
  common-interceptors
  (fn [{:keys [db]} [token]]
    {:db (-> db
           (assoc-in [:tenant :accept-token] token)
           (assoc-in [:tenant :accept-loading?] false)
           (assoc-in [:tenant :error] nil))}))

(rf/reg-event-fx
  ::accept-invitation
  common-interceptors
  (fn [{:keys [db]} [{:keys [token]}]]
    {:db (-> db
           (assoc-in [:tenant :accept-loading?] true)
           (assoc-in [:tenant :error] nil))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/tenant/invitations/accept"
                    :params {:token token}
                    :on-success [::accept-invitation-success]
                    :on-failure [::accept-invitation-failure]})}))

(rf/reg-event-fx
  ::accept-invitation-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [tenant (:tenant response)]
      (log/info "Accepted invitation to tenant:" (:name tenant))
      {:db (-> db
             (assoc-in [:tenant :accept-loading?] false)
             (assoc-in [:tenant :accept-token] nil)
             (assoc-in [:session :tenant] tenant))
       :fx [[:dispatch [auth-ids/fetch-auth-status]]
            [:dispatch [:navigate-to "/dashboard"]]]})))

(rf/reg-event-db
  ::accept-invitation-failure
  common-interceptors
  (fn [db [response]]
    (log/error "Failed to accept invitation:" response)
    (-> db
      (assoc-in [:tenant :accept-loading?] false)
      (assoc-in [:tenant :error]
        (or (get-in response [:response :errors :token 0])
          (get-in response [:response :message])
          "Failed to accept invitation")))))

;; ============================================================================
;; URL Slug
;; ============================================================================

(rf/reg-event-db
  :tenant/set-url-slug
  common-interceptors
  (fn [db [slug]]
    (assoc-in db [:tenant :url-slug] slug)))

;; ============================================================================
;; Clear messages
;; ============================================================================

(rf/reg-event-db
  ::clear-messages
  common-interceptors
  (fn [db _]
    (-> db
      (update :tenant dissoc :error :success-message))))

(comment
  ;; (require 'app.template.frontend.events.tenant :reload)
  :rcf)
