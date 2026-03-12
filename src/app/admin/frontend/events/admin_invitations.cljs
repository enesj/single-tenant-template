(ns app.admin.frontend.events.admin-invitations
  "Admin invitation management events + subscriptions.

   Two categories:
   1. Protected (owner): list, create, revoke, resend invitations
   2. Public (accept page): validate token, accept invitation"
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Protected: List Invitations
;; ============================================================================

(rf/reg-event-fx
  :admin/load-invitations
  (fn [{:keys [db]} _]
    {:db (assoc db :admin/invitations-loading? true
           :admin/invitations-error nil)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/invitations"
                    :on-success [:admin/load-invitations-success]
                    :on-failure [:admin/load-invitations-failure]})}))

(rf/reg-event-db
  :admin/load-invitations-success
  (fn [db [_ response]]
    (-> db
      (assoc :admin/invitations (:invitations response))
      (assoc :admin/invitations-loading? false)
      (dissoc :admin/invitations-error))))

(rf/reg-event-db
  :admin/load-invitations-failure
  (fn [db [_ error]]
    (log/error "Failed to load admin invitations" error)
    (-> db
      (assoc :admin/invitations-loading? false)
      (assoc :admin/invitations-error
        (admin-http/extract-error-message error)))))

;; ============================================================================
;; Protected: Create Invitation
;; ============================================================================

(rf/reg-event-fx
  :admin/invite-admin
  (fn [{:keys [db]} [_ {:keys [email role]}]]
    {:db (assoc db :admin/invite-loading? true
           :admin/invite-error nil)
     :http-xhrio (admin-http/admin-post
                   {:uri "/admin/api/invitations"
                    :params {:email email :role (or role "admin")}
                    :on-success [:admin/invite-admin-success]
                    :on-failure [:admin/invite-admin-failure]})}))

(rf/reg-event-fx
  :admin/invite-admin-success
  (fn [{:keys [db]} [_ _response]]
    {:db (-> db
           (assoc :admin/invite-loading? false)
           (dissoc :admin/invite-error))
     :dispatch-n [[:admin/load-invitations]
                  [:admin/show-success-message "Admin invitation sent successfully"]]}))

(rf/reg-event-fx
  :admin/invite-admin-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to create admin invitation" error)
    (let [message (admin-http/extract-error-message error)]
      {:db (-> db
             (assoc :admin/invite-loading? false)
             (assoc :admin/invite-error message))
       :dispatch [:admin/show-error-message message]})))

;; ============================================================================
;; Protected: Revoke Invitation
;; ============================================================================

(rf/reg-event-fx
  :admin/revoke-admin-invitation
  (fn [{:keys [db]} [_ {:keys [id]}]]
    {:db (assoc db :admin/invite-loading? true)
     :http-xhrio (admin-http/admin-delete
                   {:uri (str "/admin/api/invitations/" id)
                    :on-success [:admin/revoke-admin-invitation-success]
                    :on-failure [:admin/revoke-admin-invitation-failure]})}))

(rf/reg-event-fx
  :admin/revoke-admin-invitation-success
  (fn [{:keys [db]} [_ _response]]
    {:db (assoc db :admin/invite-loading? false)
     :dispatch-n [[:admin/load-invitations]
                  [:admin/show-success-message "Invitation revoked"]]}))

(rf/reg-event-fx
  :admin/revoke-admin-invitation-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to revoke admin invitation" error)
    {:db (assoc db :admin/invite-loading? false)
     :dispatch [:admin/show-error-message
                (admin-http/extract-error-message error)]}))

;; ============================================================================
;; Protected: Resend Invitation
;; ============================================================================

(rf/reg-event-fx
  :admin/resend-admin-invitation
  (fn [{:keys [db]} [_ {:keys [id]}]]
    {:db (assoc db :admin/invite-loading? true)
     :http-xhrio (admin-http/admin-post
                   {:uri (str "/admin/api/invitations/" id "/resend")
                    :on-success [:admin/resend-admin-invitation-success]
                    :on-failure [:admin/resend-admin-invitation-failure]})}))

(rf/reg-event-fx
  :admin/resend-admin-invitation-success
  (fn [{:keys [db]} [_ _response]]
    {:db (assoc db :admin/invite-loading? false)
     :dispatch-n [[:admin/load-invitations]
                  [:admin/show-success-message "Invitation resent"]]}))

(rf/reg-event-fx
  :admin/resend-admin-invitation-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to resend admin invitation" error)
    {:db (assoc db :admin/invite-loading? false)
     :dispatch [:admin/show-error-message
                (admin-http/extract-error-message error)]}))

;; ============================================================================
;; Public: Validate Invitation Token
;; ============================================================================

(rf/reg-event-fx
  :admin/validate-invitation-token
  (fn [{:keys [db]} [_ token]]
    {:db (-> db
           (assoc :admin/invitation-accept
             {:loading? true :token token :valid? nil :error nil}))
     :http-xhrio (admin-http/auth-request
                   {:method :get
                    :uri (str "/admin/api/invitations/validate?token=" token)
                    :on-success [:admin/validate-invitation-token-success]
                    :on-failure [:admin/validate-invitation-token-failure]})}))

(rf/reg-event-db
  :admin/validate-invitation-token-success
  (fn [db [_ response]]
    (assoc db :admin/invitation-accept
      {:loading? false
       :token    (get-in db [:admin/invitation-accept :token])
       :valid?   (:valid response)
       :email    (:email response)
       :role     (:role response)
       :inviter-name (:inviter-name response)
       :error    (:error response)})))

(rf/reg-event-db
  :admin/validate-invitation-token-failure
  (fn [db [_ error]]
    (log/error "Failed to validate invitation token" error)
    (assoc db :admin/invitation-accept
      {:loading? false
       :token    (get-in db [:admin/invitation-accept :token])
       :valid?   false
       :error    (or (admin-http/extract-error-message error)
                   "Failed to validate invitation")})))

;; ============================================================================
;; Public: Accept Invitation
;; ============================================================================

(rf/reg-event-fx
  :admin/accept-invitation
  (fn [{:keys [db]} [_ {:keys [token full-name password]}]]
    {:db (update db :admin/invitation-accept merge
           {:accepting? true :accept-error nil})
     :http-xhrio (admin-http/auth-request
                   {:method :post
                    :uri "/admin/api/invitations/accept"
                    :params {:token token
                             :full-name full-name
                             :password password}
                    :on-success [:admin/accept-invitation-success]
                    :on-failure [:admin/accept-invitation-failure]})}))

(rf/reg-event-fx
  :admin/accept-invitation-success
  (fn [{:keys [db]} [_ response]]
    (let [token (:token response)
          admin (:admin response)]
      ;; Persist the admin session + redirect
      (auth-persist/store-auth-state!
        {:token token
         :user admin
         :authenticated? true})
      {:db (-> db
             (assoc :admin/token token)
             (assoc :admin/current-user admin)
             (assoc :admin/authenticated? true)
             (assoc :admin/current-user-role (keyword (:role admin)))
             (update :admin/invitation-accept merge
               {:accepting? false :accepted? true}))
       :dispatch [:routing/push-state :admin-dashboard]})))

(rf/reg-event-db
  :admin/accept-invitation-failure
  (fn [db [_ error]]
    (log/error "Failed to accept admin invitation" error)
    (update db :admin/invitation-accept merge
      {:accepting? false
       :accept-error (admin-http/extract-error-message error)})))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/invitations
  (fn [db _] (:admin/invitations db [])))

(rf/reg-sub
  :admin/invitations-loading?
  (fn [db _] (:admin/invitations-loading? db false)))

(rf/reg-sub
  :admin/invitations-error
  (fn [db _] (:admin/invitations-error db nil)))

(rf/reg-sub
  :admin/invite-loading?
  (fn [db _] (:admin/invite-loading? db false)))

(rf/reg-sub
  :admin/invite-error
  (fn [db _] (:admin/invite-error db nil)))

(rf/reg-sub
  :admin/invitation-accept
  (fn [db _] (:admin/invitation-accept db {})))
