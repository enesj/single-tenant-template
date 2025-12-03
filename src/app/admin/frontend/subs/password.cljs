(ns app.admin.frontend.subs.password
  "Admin password reset subscriptions"
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :admin/password-reset-loading?
  (fn [db _]
    (:admin/password-reset-loading? db)))

(rf/reg-sub
  :admin/password-reset-error
  (fn [db _]
    (:admin/password-reset-error db)))

(rf/reg-sub
  :admin/password-reset-success?
  (fn [db _]
    (:admin/password-reset-success? db)))

(rf/reg-sub
  :admin/password-reset-token-verified?
  (fn [db _]
    (:admin/password-reset-token-verified? db)))
