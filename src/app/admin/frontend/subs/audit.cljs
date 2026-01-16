(ns app.admin.frontend.subs.audit
  "Enhanced admin audit logs subscriptions"
  (:require
    [re-frame.core :as rf]))

;; ============================================================================
;; Basic Audit State Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-loading?
  (fn [db _]
    (try
      (get-in db [:admin :audit :loading?] false)
      (catch :default _
        false))))

(rf/reg-sub
  :admin/audit-error
  (fn [db _]
    (get-in db [:admin :audit :error])))

(rf/reg-sub
  :admin/audit-data
  (fn [db _]
    (get-in db [:admin :audit :data] [])))

(rf/reg-sub
  :admin/audit-total-count
  (fn [db _]
    (get-in db [:admin :audit :total-count] 0)))

;; Legacy compatibility subscriptions
(rf/reg-sub
  :admin/audit-logs
  :<- [:admin/audit-data]
  (fn [audit-data _]
    audit-data))

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
;; Pagination Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-page-size
  (fn [db _]
    (get-in db [:admin :audit :pagination :per-page] 50)))

;; ============================================================================
;; Sorting Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-sort
  (fn [db _]
    (get-in db [:admin :audit :sort] {:field :created-at :direction :desc})))

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
;; Batch Operations Subscriptions
;; ============================================================================

;; ============================================================================
;; Operation Status Subscriptions
;; ============================================================================

;; ============================================================================
;; Legacy Compatibility Subscriptions
;; ============================================================================

;; ============================================================================
;; Derived Data Subscriptions
;; ============================================================================

;; ============================================================================
;; Security and Permission Subscriptions
;; ============================================================================

;; ============================================================================
;; Missing Subscription Handlers (for audit actions component)
;; ============================================================================

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
