(ns app.admin.frontend.subs.admins
  "Admin account management subscriptions"
  (:require
    [re-frame.core :as rf]))

;; Most subscriptions are already defined in app.admin.frontend.events.admins
;; This file provides any additional computed subscriptions

(rf/reg-sub
  :admin/owners-count
  :<- [:admin/admins]
  (fn [admins _]
    (count (filter #(= (or (:role %) (:admins/role %)) "owner") admins))))
