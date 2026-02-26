(ns app.admin.frontend.events.users.bulk-operations
  "Bulk user operations and data export functionality"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [re-frame.core :as rf]))

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
