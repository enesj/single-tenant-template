(ns app.admin.frontend.subs.reports
  (:require
    [re-frame.core :as rf]))

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
