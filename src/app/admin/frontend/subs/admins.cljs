(ns app.admin.frontend.subs.admins
  "Admin account management subscriptions"
  (:require
    [re-frame.core :as rf]))

;; Most subscriptions are already defined in app.admin.frontend.events.admins
;; This file provides any additional computed subscriptions

(rf/reg-sub
  :admin/admins-count
  :<- [:admin/admins]
  (fn [admins _]
    (count admins)))

(rf/reg-sub
  :admin/owners-count
  :<- [:admin/admins]
  (fn [admins _]
    (count (filter #(= (or (:role %) (:admins/role %)) "owner") admins))))

(rf/reg-sub
  :admin/active-admins
  :<- [:admin/admins]
  (fn [admins _]
    (filter #(= (or (:status %) (:admins/status %)) "active") admins)))

(rf/reg-sub
  :admin/can-delete-admin?
  :<- [:admin/current-admin]
  :<- [:admin/owners-count]
  (fn [[current-admin owners-count] [_ target-admin]]
    (let [current-id (or (:id current-admin) (:admins/id current-admin))
          target-id (or (:id target-admin) (:admins/id target-admin))
          target-role (or (:role target-admin) (:admins/role target-admin))]
      (and
        ;; Cannot delete self
        (not= (str current-id) (str target-id))
        ;; Cannot delete last owner
        (not (and (= target-role "owner") (= owners-count 1)))))))

(rf/reg-sub
  :admin/can-change-role?
  :<- [:admin/current-admin]
  :<- [:admin/owners-count]
  (fn [[current-admin owners-count] [_ target-admin new-role]]
    (let [current-id (or (:id current-admin) (:admins/id current-admin))
          target-id (or (:id target-admin) (:admins/id target-admin))
          target-role (or (:role target-admin) (:admins/role target-admin))
          is-owner? (= (or (:role current-admin) (:admins/role current-admin)) "owner")]
      (and
        ;; Only owners can change roles
        is-owner?
        ;; Cannot change own role
        (not= (str current-id) (str target-id))
        ;; Cannot demote last owner
        (not (and (= target-role "owner") 
               (not= new-role "owner")
               (= owners-count 1)))))))
