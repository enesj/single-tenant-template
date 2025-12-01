(ns app.template.frontend.subs.core
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  ::entity-type
  (fn [db _]
    (get-in db [:ui :entity-name])))

(rf/reg-sub
  ::editing-id
  (fn [db _]
    (get-in db [:ui :editing-id])))

(rf/reg-sub
  ::show-add-form
  (fn [db _]
    (get-in db [:ui :show-add-form])))

(rf/reg-sub
  ::get-db
  (fn [db _]
    db))

;; Auth status subscription
;; Auth status subscription - updated for multi-tenant support
(rf/reg-sub
  :auth-status
  (fn [db _]
    (let [;; Check for new multi-tenant session first
          auth-session (get-in db [:session])
          authenticated? (get auth-session :authenticated? false)
          session-valid? (get auth-session :session-valid? true)
          legacy-session? (get auth-session :legacy-session? false)

          ;; Get user and tenant information
          user (get auth-session :user)
          tenant (get auth-session :tenant)
          permissions (get auth-session :permissions)

          ;; Legacy OAuth token support
          tokens (or (get-in db [:session :ring.middleware.oauth2/access-tokens])
                   (get-in db [:session :oauth2/access-tokens]))
          provider (get auth-session :provider)

          ;; Loading & error state (for forms/auth flows)
          loading? (get auth-session :loading? false)
          error    (get auth-session :error)

          ;; Determine authentication method
          has-new-session? (and authenticated? (not legacy-session?))
          has-legacy-tokens? (boolean tokens)
          has-legacy-session-with-user? (and legacy-session? authenticated? user)]

      {:session auth-session
       :authenticated (or has-new-session? has-legacy-tokens? has-legacy-session-with-user?)
       :session-valid session-valid?
       :legacy-session legacy-session?
       :provider provider
       :tokens tokens
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
  :user-permissions
  (fn [db _]
    (get-in db [:session :permissions] #{})))

(rf/reg-sub
  :user-can?
  :<- [:user-permissions]
  (fn [permissions [_ permission]]
    (contains? permissions permission)))

(rf/reg-sub
  :user-role
  :<- [:current-user]
  (fn [user _]
    (:role user)))

(rf/reg-sub
  :is-tenant-owner?
  :<- [:user-role]
  (fn [role _]
    (= role "owner")))

(rf/reg-sub
  :tenant-name
  :<- [:current-tenant]
  (fn [tenant _]
    (:name tenant)))

(rf/reg-sub
  :tenant-subscription-tier
  :<- [:current-tenant]
  (fn [tenant _]
    (:subscription-tier tenant)))

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
  :password-reset/message
  (fn [db _]
    (get-in db [:password-reset :message])))

(rf/reg-sub
  :password-reset/token
  (fn [db _]
    (get-in db [:password-reset :token])))

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

(rf/reg-sub
  :change-password/message
  (fn [db _]
    (get-in db [:change-password :message])))
