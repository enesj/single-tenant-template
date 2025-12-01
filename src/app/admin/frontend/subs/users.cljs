(ns app.admin.frontend.subs.users
  "User-related subscriptions for admin panel"
  (:require
    [re-frame.core :as rf]))

;; =============================================================================
;; User Loading States
;; =============================================================================

(rf/reg-sub
  :admin/updating-user?
  (fn [db _]
    (boolean (and db (get db :admin/updating-user false)))))

(rf/reg-sub
  :admin/impersonating-user?
  (fn [db _]
    (boolean (and db (get db :admin/impersonating-user false)))))

(rf/reg-sub
  :admin/loading-user-details?
  (fn [db _]
    (boolean (and db (get db :admin/loading-user-details false)))))

(rf/reg-sub
  :admin/user-details-modal-open?
  (fn [db _]
    (boolean (and db (get db :admin/user-details-modal-open? false)))))

;; =============================================================================
;; User Error States
;; =============================================================================

(rf/reg-sub
  :admin/user-update-error
  (fn [db _]
    (and db (get db :admin/user-update-error nil))))

(rf/reg-sub
  :admin/impersonate-error
  (fn [db _]
    (and db (get db :admin/impersonate-error nil))))

(rf/reg-sub
  :admin/user-details-error
  (fn [db _]
    (and db (get db :admin/user-details-error nil))))

;; =============================================================================
;; User Data
;; =============================================================================

(rf/reg-sub
  :admin/current-user-details
  (fn [db _]
    (and db (get db :admin/current-user-details nil))))

;; =============================================================================
;; User Management Success States
;; =============================================================================

(rf/reg-sub
  :admin/user-password-reset-message
  (fn [db _]
    (and db (get db :admin/user-password-reset-message nil))))
