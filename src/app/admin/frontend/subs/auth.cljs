(ns app.admin.frontend.subs.auth
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :admin/login-loading?
  (fn [db _]
    (:admin/login-loading? db)))

(rf/reg-sub
  :admin/login-error
  (fn [db _]
    (:admin/login-error db)))

(rf/reg-sub
  :admin/authenticated?
  (fn [db _]
    (:admin/authenticated? db)))

(rf/reg-sub
  :admin/current-user
  (fn [db _]
    (:admin/current-user db)))

(rf/reg-sub
  :admin/session
  (fn [db _]
    (:admin/session db)))

(rf/reg-sub
  :admin/loading?
  (fn [db _]
    (or (:admin/login-loading? db)
      (:admin/auth-checking? db))))

(rf/reg-sub
  :admin/token
  (fn [db _]
    (:admin/token db)))

;; Success message subscription moved to app.admin.frontend.events.users.template.messages

(rf/reg-sub
  :admin/error-message
  (fn [db _]
    (:admin/error-message db)))
