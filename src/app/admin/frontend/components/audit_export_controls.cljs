(ns app.admin.frontend.components.audit-export-controls
  "Reusable header controls for the admin audit page"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui export-controls
  []
  (let [active-filters (use-subscribe [:admin/audit-active-filters])]
    ($ :div {:class "flex gap-2"}
      ($ button
        {:id "btn-export-all-audit-logs"
         :btn-type :outline
         :class "ds-btn-sm"
         :on-click #(dispatch [:admin/export-all-audit-logs])}
        "Export All")
      (when (seq active-filters)
        ($ button
          {:id "btn-clear-audit-filters"
           :btn-type :ghost
           :class "ds-btn-sm"
           :on-click #(dispatch [:admin/clear-audit-filters])}
          "Clear Filters")))))
