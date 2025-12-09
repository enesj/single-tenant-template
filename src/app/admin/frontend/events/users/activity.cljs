(ns app.admin.frontend.events.users.activity
  "User activity tracking and export functionality"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [app.frontend.utils.state :as state-utils]
    [re-frame.core :as rf]))

;; ============================================================================
;; View User Activity Events
;; ============================================================================

(rf/reg-event-fx
  :admin/view-user-activity
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Loading user activity for" user-id)
    {:db (state-utils/create-loading-state db :admin/loading-user-activity :admin/user-activity-error)
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/users/activity/" user-id)
                    :on-success [:admin/view-user-activity-success user-id]
                    :on-failure [:admin/view-user-activity-failure]})}))

(rf/reg-event-db
  :admin/view-user-activity-success
  (fn [db [_ user-id response]]
    (utils/log-user-operation "User activity loaded successfully" user-id)
    (-> db
      (state-utils/clear-loading-state :admin/loading-user-activity)
      (assoc :admin/current-user-activity (:activity response))
      (assoc :admin/current-user-activity-id user-id))))

(rf/reg-event-db
  :admin/view-user-activity-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/loading-user-activity :admin/user-activity-error "load user activity")
    (state-utils/handle-api-error db {:loading-key :admin/loading-user-activity
                                      :error-key :admin/user-activity-error
                                      :error-message "Failed to load user activity"})))

;; ============================================================================
;; Export User Activity Events
;; ============================================================================

(rf/reg-event-fx
  :admin/export-user-activity
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Exporting user activity for" user-id)
    {:db (state-utils/create-loading-state db :admin/exporting-user-activity :admin/user-activity-export-error)
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/users/" user-id "/activity/export")
                    :on-success [:admin/export-user-activity-success user-id]
                    :on-failure [:admin/export-user-activity-failure]})}))

(rf/reg-event-fx
  :admin/export-user-activity-success
  (fn [{:keys [db]} [_ user-id blob-response]]
    (utils/log-user-operation "User activity export successful for" user-id)
    (utils/create-download-link
      blob-response
      (utils/generate-export-filename "user-activity" :user-id user-id))
    (utils/show-user-success-message
      (state-utils/clear-loading-state db :admin/exporting-user-activity)
      "User activity exported successfully")))

(rf/reg-event-db
  :admin/export-user-activity-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/exporting-user-activity :admin/user-activity-export-error "export user activity")
    (state-utils/handle-api-error db {:loading-key :admin/exporting-user-activity
                                      :error-key :admin/user-activity-export-error
                                      :error-message "Failed to export user activity"})))

;; ============================================================================
;; Activity Tracking Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/current-user-activity
  (fn [db _]
    (:admin/current-user-activity db nil)))

(rf/reg-sub
  :admin/loading-user-activity
  (fn [db _]
    (:admin/loading-user-activity db false)))

(rf/reg-sub
  :admin/current-user-activity-id
  (fn [db _]
    (:admin/current-user-activity-id db nil)))

(rf/reg-sub
  :admin/user-activity-error
  (fn [db _]
    (:admin/user-activity-error db nil)))

(rf/reg-sub
  :admin/exporting-user-activity
  (fn [db _]
    (:admin/exporting-user-activity db false)))

(rf/reg-sub
  :admin/user-activity-export-error
  (fn [db _]
    (:admin/user-activity-export-error db nil)))

;; Activity tracking events to be migrated here:
;; - :admin/view-user-activity
;; - :admin/view-user-activity-success
;; - :admin/view-user-activity-failure
;; - :admin/export-user-activity
;; - :admin/export-user-activity-success
;; - :admin/export-user-activity-failure
;;
;; Related subscriptions:
;; - :admin/current-user-activity
;; - :admin/loading-user-activity
;; - :admin/current-user-activity-id
;; - :admin/user-activity-error
;; - :admin/exporting-user-activity
;; - :admin/user-activity-export-error
