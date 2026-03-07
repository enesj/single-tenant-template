(ns app.admin.frontend.subs.reports
  (:require
    [re-frame.core :as rf]))

(defn- detail-path
  [detail-key detail-id]
  [:admin/reports :details detail-key (str detail-id)])

(rf/reg-sub
  :admin/reports
  (fn [db _]
    (:admin/reports db)))

(rf/reg-sub
  :admin/reports-filters
  :<- [:admin/reports]
  (fn [reports _]
    (or (:filters reports) {:months-back 6})))

(rf/reg-sub
  :admin/report-data
  :<- [:admin/reports]
  (fn [reports [_ report-key]]
    (get-in reports [report-key :data])))

(rf/reg-sub
  :admin/report-loading?
  :<- [:admin/reports]
  (fn [reports [_ report-key]]
    (boolean (get-in reports [report-key :loading?]))))

(rf/reg-sub
  :admin/report-error
  :<- [:admin/reports]
  (fn [reports [_ report-key]]
    (get-in reports [report-key :error])))

(rf/reg-sub
  :admin/report-sort
  :<- [:admin/reports]
  (fn [reports [_ report-key]]
    (get-in reports [report-key :sort])))

(rf/reg-sub
  :admin/report-detail
  (fn [[_ detail-key detail-id]]
    (rf/subscribe [:admin/reports]))
  (fn [reports [_ detail-key detail-id]]
    (get-in reports (rest (detail-path detail-key detail-id)))))

(rf/reg-sub
  :admin/report-detail-data
  (fn [[_ detail-key detail-id]]
    (rf/subscribe [:admin/report-detail detail-key detail-id]))
  (fn [detail _]
    (:data detail)))

(rf/reg-sub
  :admin/report-detail-loading?
  (fn [[_ detail-key detail-id]]
    (rf/subscribe [:admin/report-detail detail-key detail-id]))
  (fn [detail _]
    (boolean (:loading? detail))))

(rf/reg-sub
  :admin/report-detail-error
  (fn [[_ detail-key detail-id]]
    (rf/subscribe [:admin/report-detail detail-key detail-id]))
  (fn [detail _]
    (:error detail)))
