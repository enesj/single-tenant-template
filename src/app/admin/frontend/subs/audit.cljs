(ns app.admin.frontend.subs.audit
  "Enhanced admin audit logs subscriptions"
  (:require
    [re-frame.core :as rf]))

;; ============================================================================
;; Filtering Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-active-filters
  (fn [db _]
    (let [filters (get-in db [:admin :audit :filters] {})]
      (->> filters
        (filter (fn [[_ v]] (and v (not= v ""))))
        (into {})))))

;; ============================================================================
;; Modal Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-details-modal-visible?
  (fn [db _]
    (get-in db [:admin :audit :details-modal :visible?] false)))

(rf/reg-sub
  :admin/audit-details-modal-audit-log
  (fn [db _]
    (get-in db [:admin :audit :details-modal :audit-log])))

;; ============================================================================
;; Operation Status Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-logs-error
  (fn [db _]
    (get-in db [:admin :audit :error])))

(rf/reg-sub
  :admin/loading-audit-details?
  (fn [db _]
    (get-in db [:admin :audit :loading-details?] false)))

(rf/reg-sub
  :admin/exporting-audit?
  (fn [db _]
    (get-in db [:admin :audit :exporting?] false)))

(rf/reg-sub
  :admin/deleting-audit?
  (fn [db _]
    (get-in db [:admin :audit :deleting?] false)))

;; ============================================================================
;; API Failure Badge
;; ============================================================================

(rf/reg-sub
  :admin/unread-api-failure-count
  (fn [db _]
    (get-in db [:admin :audit :unread-api-failure-count] 0)))
