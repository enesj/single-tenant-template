(ns app.admin.frontend.events.users.security
  "Security operations - impersonation, password resets, email verification"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [re-frame.core :as rf]))

;; ============================================================================
;; Impersonate User Events
;; ============================================================================

(rf/reg-event-fx
  :admin/impersonate-user
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Impersonating user" user-id)
    {:db (utils/create-loading-db-state db :admin/impersonating-user)
     :http-xhrio (utils/create-user-http-request
                   :post (str "/admin/api/user-management/impersonate/" user-id)
                   :on-success [:admin/impersonate-user-success]
                   :on-failure [:admin/impersonate-user-failure])}))

(rf/reg-event-fx
  :admin/impersonate-user-success
  (fn [{:keys [db]} [_ response]]
    (utils/log-user-operation "User impersonation successful, redirecting...")
    (set! (.-location js/window) (:redirect-url response "/app/dashboard"))
    {:db (utils/clear-loading-db-state db :admin/impersonating-user)}))

(rf/reg-event-db
  :admin/impersonate-user-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/impersonating-user :admin/impersonate-error "impersonate user")))

;; ============================================================================
;; Reset User Password Events
;; ============================================================================

(rf/reg-event-fx
  :admin/reset-user-password
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Resetting password for user" user-id)
    {:db (utils/create-loading-db-state db :admin/updating-user)
     :http-xhrio (utils/create-user-http-request
                   :post (str "/admin/api/user-management/reset-password/" user-id)
                   :on-success [:admin/reset-user-password-success user-id]
                   :on-failure [:admin/reset-user-password-failure])}))

(rf/reg-event-db
  :admin/reset-user-password-success
  (fn [db [_ user-id response]]
    (utils/log-user-operation "Password reset successfully for user" user-id)
    (-> db
      (utils/clear-loading-db-state :admin/updating-user)
      (assoc :admin/user-password-reset-message (:message response)))))

(rf/reg-event-db
  :admin/reset-user-password-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/updating-user :admin/user-update-error "reset password")))

;; ============================================================================
;; Force Verify Email Events (moved from status.cljs for better organization)
;; ============================================================================

(rf/reg-event-fx
  :admin/force-verify-email
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Force verifying email for user" user-id)
    {:db (utils/create-loading-db-state db :admin/updating-user)
     :http-xhrio (utils/create-user-http-request
                   :post (str "/admin/api/user-management/verify-email/" user-id)
                   :on-success [:admin/force-verify-email-success user-id]
                   :on-failure [:admin/force-verify-email-failure])}))

(rf/reg-event-fx
  :admin/force-verify-email-success
  (fn [{:keys [db]} [_ user-id _response]]
    (utils/log-user-operation "Email verified successfully for user" user-id)
    {:db (-> db
           (utils/clear-loading-db-state :admin/updating-user)
           (utils/sync-admin-and-entity-stores user-id #(-> %
                                                          (assoc :users/email-verified true)
                                                          (assoc :email-verified true))))}))

(rf/reg-event-db
  :admin/force-verify-email-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/updating-user :admin/user-update-error "verify email")))

;; ============================================================================
;; Security Operations Subscriptions
;; ============================================================================

;; Subscription moved to app.admin.frontend.subs.users

;; Security operations events to be migrated here:
;; - :admin/impersonate-user
;; - :admin/impersonate-user-success
;; - :admin/impersonate-user-failure
;; - :admin/reset-user-password
;; - :admin/reset-user-password-success
;; - :admin/reset-user-password-failure
;; - :admin/force-verify-email
;; - :admin/force-verify-email-success
;; - :admin/force-verify-email-failure
;;
;; Related subscriptions:
;; - :admin/user-password-reset-message
