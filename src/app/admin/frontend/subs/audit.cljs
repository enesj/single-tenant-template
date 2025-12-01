(ns app.admin.frontend.subs.audit
  "Enhanced admin audit logs subscriptions"
  (:require
    [clojure.string :as str]
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
  :admin/audit-filters
  (fn [db _]
    (get-in db [:admin :audit :filters] {})))

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
  :admin/audit-pagination
  (fn [db _]
    (get-in db [:admin :audit :pagination] {:page 1 :per-page 50})))

(rf/reg-sub
  :admin/audit-current-page
  (fn [db _]
    (get-in db [:admin :audit :pagination :page] 1)))

(rf/reg-sub
  :admin/audit-page-size
  (fn [db _]
    (get-in db [:admin :audit :pagination :per-page] 50)))

(rf/reg-sub
  :admin/audit-total-pages
  :<- [:admin/audit-total-count]
  :<- [:admin/audit-page-size]
  (fn [[total-count page-size] _]
    (if (and total-count page-size (> page-size 0))
      (Math/ceil (/ total-count page-size))
      1)))

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

(rf/reg-sub
  :admin/batch-audit-actions-visible?
  (fn [db _]
    (get-in db [:admin :audit :batch-actions :visible?] false)))

(rf/reg-sub
  :admin/batch-selected-audit-ids
  (fn [db _]
    (get-in db [:admin :audit :batch-actions :selected-ids] [])))

;; ============================================================================
;; Operation Status Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-deleting?
  (fn [db _]
    (get-in db [:admin :audit :deleting?] false)))

(rf/reg-sub
  :admin/audit-bulk-deleting?
  (fn [db _]
    (get-in db [:admin :audit :bulk-deleting?] false)))

(rf/reg-sub
  :admin/audit-exporting?
  (fn [db _]
    (get-in db [:admin :audit :exporting?] false)))

;; ============================================================================
;; Legacy Compatibility Subscriptions
;; ============================================================================

;; Derived subscription for filtered and formatted logs (legacy)
(rf/reg-sub
  :admin/audit-logs-formatted
  :<- [:admin/audit-logs]
  (fn [logs _]
    (mapv (fn [log]
            (let [changes (when (:changes log)
                            (if (string? (:changes log))
                              (try
                                (js/JSON.parse (:changes log))
                                (catch js/Error _ (:changes log)))
                              (:changes log)))]
              (assoc log :changes-parsed changes)))
      logs)))

;; Get unique entity types for filter dropdown (legacy)
(rf/reg-sub
  :admin/audit-entity-types
  :<- [:admin/audit-logs]
  (fn [logs _]
    (->> logs
      (map :entity-type)
      (remove nil?)
      distinct
      sort
      vec)))

;; Get unique actions for filter dropdown (legacy)
(rf/reg-sub
  :admin/audit-actions
  :<- [:admin/audit-logs]
  (fn [logs _]
    (->> logs
      (map :action)
      (remove nil?)
      distinct
      sort
      vec)))

;; ============================================================================
;; Derived Data Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/audit-stats
  :<- [:admin/audit-data]
  (fn [audit-logs _]
    (when (seq audit-logs)
      (let [grouped-by-action (group-by :action audit-logs)
            grouped-by-admin (group-by :admin-email audit-logs)
            grouped-by-entity (group-by :entity-type audit-logs)]
        {:total-count (count audit-logs)
         :actions (into {} (map (fn [[k v]] [k (count v)]) grouped-by-action))
         :admins (into {} (map (fn [[k v]] [k (count v)]) grouped-by-admin))
         :entities (into {} (map (fn [[k v]] [k (count v)]) grouped-by-entity))
         :latest-entry (first (sort-by :created-at #(compare %2 %1) audit-logs))}))))

(rf/reg-sub
  :admin/audit-filtered-count
  :<- [:admin/audit-data]
  :<- [:admin/audit-active-filters]
  (fn [[audit-logs active-filters] _]
    (if (empty? active-filters)
      (count audit-logs)
      ;; Simple client-side filtering for display purposes
      (count (filter (fn [log]
                       (every? (fn [[filter-key filter-value]]
                                 (let [log-value (get log filter-key)]
                                   (and log-value
                                     (clojure.string/includes?
                                       (str log-value)
                                       (str filter-value)))))
                         active-filters))
               audit-logs)))))

;; ============================================================================
;; Security and Permission Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/can-delete-audit-logs?
  (fn [db _]
    ;; For now, all authenticated admins can delete audit logs
    ;; In production, this should check specific permissions
    (boolean (get-in db [:admin :token]))))

(rf/reg-sub
  :admin/can-export-audit-logs?
  (fn [db _]
    ;; For now, all authenticated admins can export audit logs
    ;; In production, this should check specific permissions
    (boolean (get-in db [:admin :token]))))

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

;; ============================================================================
;; Template Compatibility Subscriptions
;; These subscriptions provide the naming pattern expected by use-entity-state
;; ============================================================================

;; Template compatibility - error state for audit-logs entity
(rf/reg-sub
  :admin/audit-logs-error
  :<- [:admin/audit-error]
  (fn [error _]
    error))

;; Template compatibility - loading state for audit-logs entity
(rf/reg-sub
  :admin/audit-logs-loading?
  :<- [:admin/audit-loading?]
  (fn [loading? _]
    loading?))
