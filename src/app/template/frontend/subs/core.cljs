(ns app.template.frontend.subs.core
  "Subscription registration entrypoint; ensure subs are loaded."
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  ::entity-type
  (fn [db _]
    (get-in db [:ui :current-entity-type])))

;; Auth status subscription
;; Auth status subscription - updated for multi-tenant support
(rf/reg-sub
  :auth-status
  (fn [db _]
    (let [auth-session (get-in db [:session])
          authenticated? (get auth-session :authenticated? false)
          session-valid? (get auth-session :session-valid? true)

          ;; Get user and tenant information
          user (get auth-session :user)
          tenant (get auth-session :tenant)
          permissions (get auth-session :permissions)

          provider (get auth-session :provider)

          ;; Loading & error state (for forms/auth flows)
          loading? (get auth-session :loading? false)
          error    (get auth-session :error)

          has-session? authenticated?]

      {:session auth-session
       :authenticated has-session?
       :session-valid session-valid?
       :provider provider
       :user user
       :tenant tenant
       :permissions permissions
       :loading? loading?
       :error error})))

;; Multi-tenant subscriptions
(rf/reg-sub
  :current-user
  (fn [db _]
    (get-in db [:session :user])))

(rf/reg-sub
  :current-tenant
  (fn [db _]
    (get-in db [:session :tenant])))

(rf/reg-sub
  :user-role
  (fn [db _]
    (or (get-in db [:session :membership-role])
      (get-in db [:session :user :role]))))

(rf/reg-sub
  :is-tenant-owner?
  :<- [:user-role]
  (fn [role _]
    (= role "owner")))

(rf/reg-sub
  :tenant-name
  :<- [:current-tenant]
  (fn [tenant _]
    (or (:name tenant) (:tenants/name tenant))))

(rf/reg-sub
  :available-tenants
  (fn [db _]
    (get-in db [:session :available-tenants])))

(rf/reg-sub
  :tenant-selection-required?
  (fn [db _]
    (get-in db [:session :tenant-selection-required] false)))

;; Models metadata
(rf/reg-sub
  :models-data
  (fn [db _]
    (:models-data db)))

;; ========================================================================
;; Password Reset Subscriptions
;; ========================================================================

(rf/reg-sub
  :password-reset/loading?
  (fn [db _]
    (get-in db [:password-reset :loading?] false)))

(rf/reg-sub
  :password-reset/success?
  (fn [db _]
    (get-in db [:password-reset :success?] false)))

(rf/reg-sub
  :password-reset/error
  (fn [db _]
    (get-in db [:password-reset :error])))

(rf/reg-sub
  :password-reset/token-verified?
  (fn [db _]
    (get-in db [:password-reset :token-verified?] false)))

;; ========================================================================
;; Change Password Subscriptions
;; ========================================================================

(rf/reg-sub
  :change-password/loading?
  (fn [db _]
    (get-in db [:change-password :loading?] false)))

(rf/reg-sub
  :change-password/success?
  (fn [db _]
    (get-in db [:change-password :success?] false)))

(rf/reg-sub
  :change-password/error
  (fn [db _]
    (get-in db [:change-password :error])))

;; ========================================================================
;; Role-Based Access Subscriptions
;; ========================================================================
