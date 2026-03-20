(ns app.admin.frontend.components.audit-export-controls
  "Reusable header controls for the admin audit page"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui export-controls
  []
  (let [active-filters (use-subscribe [:admin/audit-active-filters])
        unread-count (use-subscribe [:admin/unread-api-failure-count])]
    ;; Refresh unread count when audit page mounts
    (use-effect
      (fn []
        (dispatch [:admin/fetch-unread-api-failures])
        js/undefined)
      [])
    ($ :div {:class "flex gap-2 items-center"}
      (when (and unread-count (pos? unread-count))
        ($ :div {:class "flex items-center gap-2 mr-2"}
          ($ :span {:class "text-sm text-error font-medium"}
            (str unread-count " unread API failure" (when (> unread-count 1) "s")))
          ($ button
            {:id "btn-dismiss-api-failures"
             :btn-type :ghost
             :class "ds-btn-sm text-error hover:bg-error/10"
             :on-click #(dispatch [:admin/acknowledge-api-failures])}
            "Dismiss")))
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
