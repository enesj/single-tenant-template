(ns app.admin.frontend.components.user-activity-modal
  (:require
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui activity-summary
  "Display user activity summary stats"
  [{:keys [summary]}]
  ($ :div {:class "ds-stats ds-stats-vertical lg:ds-stats-horizontal shadow mb-4"}
    ($ :div {:class "ds-stat"}
      ($ :div {:class "ds-stat-title"} "Total Actions")
      ($ :div {:class "ds-stat-value text-primary"} (or (:total-actions summary) 0)))

    ($ :div {:class "ds-stat"}
      ($ :div {:class "ds-stat-title"} "Recent Logins")
      ($ :div {:class "ds-stat-value text-secondary"} (or (:recent-logins summary) 0)))

    ($ :div {:class "ds-stat"}
      ($ :div {:class "ds-stat-title"} "Last Activity")
      ($ :div {:class "ds-stat-desc"}
        (if (:last-activity summary)
          (.toLocaleDateString (js/Date. (:last-activity summary)))
          "Never")))

    ($ :div {:class "ds-stat"}
      ($ :div {:class "ds-stat-title"} "Last Login")
      ($ :div {:class "ds-stat-desc"}
        (if (:last-login summary)
          (.toLocaleDateString (js/Date. (:last-login summary)))
          "Never")))))

(defui audit-log-entry
  "Display a single audit log entry"
  [{:keys [entry]}]
  ($ :tr
    ($ :td
      ($ :div {:class "text-sm font-medium"} (:action entry))
      ($ :div {:class "text-xs text-gray-500"}
        (.toLocaleString (js/Date. (:created-at entry)))))

    ($ :td
      ($ :div {:class "text-sm"} (or (:admin-email entry) "System"))
      ($ :div {:class "text-xs text-gray-500"} (or (:admin-name entry) "")))

    ($ :td
      (if (:changes entry)
        ($ :div {:class "text-xs"}
          ($ :div {:class "text-green-600"}
            "After: " (str (:after (:changes entry))))
          ($ :div {:class "text-red-600"}
            "Before: " (str (:before (:changes entry)))))
        ($ :span {:class "text-gray-400"} "No changes")))

    ($ :td
      (when (:ip-address entry)
        ($ :span {:class "text-xs font-mono bg-gray-100 px-2 py-1 rounded"}
          (str (:ip-address entry)))))))

(defui login-history-entry
  "Display a single login history entry"
  [{:keys [entry]}]
  ($ :tr
    ($ :td
      (.toLocaleString (js/Date. (:created-at entry))))

    ($ :td
      (when (:ip-address entry)
        ($ :span {:class "text-xs font-mono bg-gray-100 px-2 py-1 rounded"}
          (str (:ip-address entry)))))

    ($ :td
      ($ :div {:class "text-xs text-gray-600 max-w-xs truncate"}
        (or (:user-agent entry) "Unknown")))))

(defui user-activity-modal
  "Modal displaying comprehensive user activity and analytics"
  []
  (let [activity (use-subscribe [:admin/current-user-activity])
        loading? (use-subscribe [:admin/loading-user-activity])
        user-id (use-subscribe [:admin/current-user-activity-id])
        error (use-subscribe [:admin/user-activity-error])]

    ($ modal-wrapper
      {:visible? (or loading? activity error)
       :title "User Activity & Analytics"
       :size :extra-large
       :on-close [:admin/clear-user-activity]
       :close-button-id "btn-close-user-activity-modal"}

      ;; Loading state
      (when loading?
        ($ :div {:class "flex items-center justify-center py-8"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

      ;; Error state
      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"}
          ($ :span error)))

      ;; Activity content
      (when activity
        ($ :div
          ;; Activity summary
          ($ activity-summary {:summary (:summary activity)})

          ;; Tabs for different views
          ($ :div {:class "ds-tabs ds-tabs-boxed mb-4"}
            ($ :a {:class "ds-tab ds-tab-active"} "Audit Logs")
            ($ :a {:class "ds-tab"} "Login History"))

          ;; Audit logs table
          ($ :div {:class "overflow-x-auto mb-6"}
            ($ :h3 {:class "text-lg font-semibold mb-2"} "Recent Admin Actions")
            (if (seq (:audit-logs activity))
              ($ :table {:class "ds-table ds-table-zebra ds-table-compact"}
                ($ :thead
                  ($ :tr
                    ($ :th "Action & Time")
                    ($ :th "Admin")
                    ($ :th "Changes")
                    ($ :th "IP Address")))
                ($ :tbody
                  (map-indexed
                    (fn [idx entry]
                      ($ audit-log-entry {:key idx :entry entry}))
                    (:audit-logs activity))))
              ($ :div {:class "text-center py-4 text-gray-500"}
                "No audit log entries found")))

          ;; Login history table
          ($ :div {:class "overflow-x-auto"}
            ($ :h3 {:class "text-lg font-semibold mb-2"} "Recent Logins")
            (if (seq (:login-history activity))
              ($ :table {:class "ds-table ds-table-zebra ds-table-compact"}
                ($ :thead
                  ($ :tr
                    ($ :th "Login Time")
                    ($ :th "IP Address")
                    ($ :th "User Agent")))
                ($ :tbody
                  (map-indexed
                    (fn [idx entry]
                      ($ login-history-entry {:key idx :entry entry}))
                    (:login-history activity))))
              ($ :div {:class "text-center py-4 text-gray-500"}
                "No login history found")))))

      ;; Footer actions
      ($ :div {:class "ds-modal-action"}
        ($ button {:btn-type :primary
                   :on-click #(dispatch [:admin/export-user-activity user-id])}
          "Export Activity")
        ($ button {:btn-type :ghost
                   :on-click #(dispatch [:admin/clear-user-activity])}
          "Close")))))

;; Event handlers for the modal
(re-frame.core/reg-event-db
  :admin/clear-user-activity
  (fn [db _]
    (-> db
      (dissoc :admin/current-user-activity)
      (dissoc :admin/current-user-activity-id)
      (dissoc :admin/loading-user-activity)
      (dissoc :admin/user-activity-error))))
