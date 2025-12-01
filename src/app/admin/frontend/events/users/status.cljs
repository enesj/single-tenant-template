(ns app.admin.frontend.events.users.status
  "User status management (activate/deactivate/suspend) and role updates"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [re-frame.core :as rf]))

;; ============================================================================
;; Update User Status Events
;; ============================================================================

(rf/reg-event-fx
  :admin/update-user-status
  (fn [{:keys [db]} [_ user-id new-status]]
    (utils/log-user-operation "Updating user status" user-id "to" new-status)
    (let [status-value (if (keyword? new-status) (name new-status) (str new-status))]
      {:db (utils/create-loading-db-state db :admin/updating-user)
       :http-xhrio (utils/create-user-http-request
                     :put (str "/admin/api/users/" user-id)
                     :params {:status status-value}
                     :on-success [:admin/update-user-status-success user-id status-value]
                     :on-failure [:admin/update-user-status-failure])})))

(rf/reg-event-fx
  :admin/update-user-status-success
  (fn [{:keys [db]} [_ user-id new-status _response]]
    (utils/log-user-operation "User status updated successfully" user-id new-status)
    {:db (-> db
           (utils/clear-loading-db-state :admin/updating-user)
           (utils/sync-admin-and-entity-stores user-id #(-> %
                                                          (assoc :users/status new-status)
                                                          (assoc :status new-status))))}))

(rf/reg-event-db
  :admin/update-user-status-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/updating-user :admin/user-update-error "update user status")))

;; ============================================================================
;; Update User Role Events
;; ============================================================================

(rf/reg-event-fx
  :admin/update-user-role
  (fn [{:keys [db]} [_ user-id new-role]]
    (utils/log-user-operation "Updating user role" user-id "to" new-role)
    (let [role-value (if (keyword? new-role) (name new-role) (str new-role))]
      {:db (utils/create-loading-db-state db :admin/updating-user)
       :http-xhrio (utils/create-user-http-request
                     :put (str "/admin/api/user-management/role/" user-id)
                     :params {:role role-value}
                     :on-success [:admin/update-user-role-success user-id role-value]
                     :on-failure [:admin/update-user-role-failure])})))

(rf/reg-event-fx
  :admin/update-user-role-success
  (fn [{:keys [db]} [_ user-id new-role _response]]
    (utils/log-user-operation "User role updated successfully" user-id new-role)
    {:db (-> db
           (utils/clear-loading-db-state :admin/updating-user)
           (utils/sync-admin-and-entity-stores user-id #(-> %
                                                          (assoc :users/role new-role)
                                                          (assoc :role new-role))))}))

(rf/reg-event-db
  :admin/update-user-role-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/updating-user :admin/user-update-error "update user role")))

;; ============================================================================
;; Force Verify Email Events
;; ============================================================================

;; Status management events to be migrated here:
;; - :admin/update-user-status
;; - :admin/update-user-status-success
;; - :admin/update-user-status-failure
;; - :admin/update-user-role
;; - :admin/update-user-role-success
;; - :admin/update-user-role-failure
