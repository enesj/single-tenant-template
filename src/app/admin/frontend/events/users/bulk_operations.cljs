(ns app.admin.frontend.events.users.bulk-operations
  "Bulk user operations and data export functionality"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [app.frontend.utils.state :as state-utils]
    [app.shared.frontend.crud.success :as crud-success]
    [re-frame.core :as rf]))

;; ============================================================================
;; Bulk Update User Status Events
;; ============================================================================

(rf/reg-event-fx
  :admin/bulk-update-user-status
  (fn [{:keys [db]} [_ user-ids new-status]]
    (utils/log-user-operation "Bulk updating user status" (count user-ids) "users to" new-status)
    {:db (state-utils/create-loading-state db :admin/bulk-updating-users :admin/bulk-update-error)
     :http-xhrio (admin-http/admin-request
                   {:method :put
                    :uri "/admin/api/users/actions/bulk-status"
                    :params {:user-ids user-ids :status new-status}
                    :on-success [:admin/bulk-update-user-status-success user-ids new-status]
                    :on-failure [:admin/bulk-update-user-status-failure]})}))

(rf/reg-event-fx
  :admin/bulk-update-user-status-success
  (fn [{:keys [db]} [_ user-ids new-status _response]]
    (utils/log-user-operation "Bulk user status update successful" (count user-ids) "users")
    (let [db' (-> db
                (state-utils/clear-loading-state :admin/bulk-updating-users)
                (utils/sync-bulk-admin-and-entity-stores user-ids #(assoc % :users/status new-status))
                (crud-success/handle-bulk-update-success :users user-ids))]
      (utils/show-user-success-message
        db'
        (str "Updated " (count user-ids) " users to " new-status)))))

(rf/reg-event-db
  :admin/bulk-update-user-status-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/bulk-updating-users :admin/bulk-update-error "bulk update user status")
    (state-utils/handle-api-error db {:loading-key :admin/bulk-updating-users
                                      :error-key :admin/bulk-update-error
                                      :error-message "Failed to bulk update user status"})))

;; ============================================================================
;; Bulk Update User Role Events
;; ============================================================================

(rf/reg-event-fx
  :admin/bulk-update-user-role
  (fn [{:keys [db]} [_ user-ids new-role]]
    (utils/log-user-operation "Bulk updating user role" (count user-ids) "users to" new-role)
    {:db (state-utils/create-loading-state db :admin/bulk-updating-users :admin/bulk-update-error)
     :http-xhrio (admin-http/admin-request
                   {:method :put
                    :uri "/admin/api/users/actions/bulk-role"
                    :params {:user-ids user-ids :role new-role}
                    :on-success [:admin/bulk-update-user-role-success user-ids new-role]
                    :on-failure [:admin/bulk-update-user-role-failure]})}))

(rf/reg-event-fx
  :admin/bulk-update-user-role-success
  (fn [{:keys [db]} [_ user-ids new-role _response]]
    (utils/log-user-operation "Bulk user role update successful" (count user-ids) "users")
    (let [db' (-> db
                (state-utils/clear-loading-state :admin/bulk-updating-users)
                (utils/sync-bulk-admin-and-entity-stores user-ids #(assoc % :users/role new-role))
                (crud-success/handle-bulk-update-success :users user-ids))]
      (utils/show-user-success-message
        db'
        (str "Updated " (count user-ids) " users to role " new-role)))))

(rf/reg-event-db
  :admin/bulk-update-user-role-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/bulk-updating-users :admin/bulk-update-error "bulk update user role")
    (state-utils/handle-api-error db {:loading-key :admin/bulk-updating-users
                                      :error-key :admin/bulk-update-error
                                      :error-message "Failed to bulk update user role"})))

;; ============================================================================
;; Export Users Events
;; ============================================================================

(rf/reg-event-fx
  :admin/export-users
  (fn [{:keys [db]} [_ user-ids]]
    (utils/log-user-operation "Exporting users" (count user-ids) "users")
    {:db (state-utils/create-loading-state db :admin/exporting-users :admin/export-error)
     :http-xhrio (admin-http/admin-post
                   {:uri "/admin/api/users/actions/export"
                    :params {:user-ids (or user-ids [])}
                    :on-success [:admin/export-users-success]
                    :on-failure [:admin/export-users-failure]})}))

(rf/reg-event-fx
  :admin/export-users-success
  (fn [{:keys [db]} [_ blob-response]]
    (utils/log-user-operation "Users export successful")
    (utils/create-download-link
      blob-response
      (utils/generate-export-filename "users-export"))
    (utils/show-user-success-message
      (state-utils/clear-loading-state db :admin/exporting-users)
      "Users exported successfully")))

(rf/reg-event-db
  :admin/export-users-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/exporting-users :admin/export-error "export users")
    (state-utils/handle-api-error db {:loading-key :admin/exporting-users
                                      :error-key :admin/export-error
                                      :error-message "Failed to export users"})))

;; ============================================================================
;; Batch User Actions Panel Events
;; ============================================================================

(rf/reg-event-db
  :admin/show-batch-user-actions
  (fn [db [_ selected-user-ids]]
    (utils/log-user-operation "Showing batch user actions panel for" (count selected-user-ids) "users")
    (-> db
      (assoc :admin/batch-user-actions-visible? true)
      (assoc :admin/batch-selected-user-ids selected-user-ids))))

(rf/reg-event-db
  :admin/hide-batch-user-actions
  (fn [db _]
    (utils/log-user-operation "Hiding batch user actions panel")
    (-> db
      (dissoc :admin/batch-user-actions-visible?)
      (dissoc :admin/batch-selected-user-ids))))

;; ============================================================================
;; Bulk Operations Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/bulk-updating-users
  (fn [db _]
    (:admin/bulk-updating-users db false)))

(rf/reg-sub
  :admin/batch-user-actions-visible?
  (fn [db _]
    (:admin/batch-user-actions-visible? db false)))

(rf/reg-sub
  :admin/batch-selected-user-ids
  (fn [db _]
    (:admin/batch-selected-user-ids db [])))

;; Bulk operations events to be migrated here:
;; - :admin/bulk-update-user-status
;; - :admin/bulk-update-user-status-success
;; - :admin/bulk-update-user-status-failure
;; - :admin/bulk-update-user-role
;; - :admin/bulk-update-user-role-success
;; - :admin/bulk-update-user-role-failure
;; - :admin/export-users
;; - :admin/export-users-success
;; - :admin/export-users-failure
;; - :admin/show-batch-user-actions
;; - :admin/hide-batch-user-actions
;;
;; Related subscriptions:
;; - :admin/bulk-updating-users
;; - :admin/batch-user-actions-visible?
;; - :admin/batch-selected-user-ids
