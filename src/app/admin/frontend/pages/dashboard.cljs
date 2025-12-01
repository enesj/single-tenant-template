(ns app.admin.frontend.pages.dashboard
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.stats-components :refer [page-header
                                                            recent-activity-table
                                                            stats-card]]
    [app.admin.frontend.events.dashboard]
    [app.admin.frontend.subs.dashboard]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-dashboard-content []
  (let [stats (use-subscribe [:admin/dashboard-stats])
        loading? (use-subscribe [:admin/dashboard-loading?])
        recent-activity (use-subscribe [:admin/recent-activity])]

    ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
      ;; Header using shared component
      ($ page-header {:title "Admin Dashboard"
                      :subtitle "System overview and administrative controls"
                      :icon "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                      :icon-color "from-primary to-secondary"
                      :bg-gradient "from-primary/10 to-secondary/10"})

      ;; Stats Grid with enhanced styling
      ($ :div {:class "mt-6 px-4 sm:px-6 lg:px-8"}
        (if loading?
          ($ :div {:class "text-center py-12"}
            ($ :div {:class "bg-base-100/50 backdrop-blur-sm rounded-2xl p-12 shadow-xl border border-base-300"}
              ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary mb-4"})
              ($ :p {:class "text-xl font-medium text-base-content/70"} "Loading dashboard...")
              ($ :p {:class "text-base-content/50 text-sm mt-2"} "Please wait while we fetch your statistics")))
          (when stats
            ($ :div {:class "grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4"}
              ($ stats-card {:title "Total Admins"
                             :value (str (get stats :total-admins 0))
                             :subtitle "System administrators"
                             :color "bg-info"
                             :icon "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"})
              ($ stats-card {:title "Active Sessions"
                             :value (str (get stats :active-sessions 0))
                             :subtitle "Currently logged in"
                             :color "bg-success"
                             :icon "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"})
              ($ stats-card {:title "Recent Activity"
                             :value (str (get stats :recent-activity 0))
                             :subtitle "Last 7 days"
                             :color "bg-warning"
                             :icon "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"})
              ($ stats-card {:title "Recent Events"
                             :value (str (count (get stats :recent-events [])))
                             :subtitle "This month"
                             :color "bg-secondary"
                             :icon "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"})))))

      ;; Recent Activity Section with enhanced styling
      ($ :div {:class "mt-8 px-4 sm:px-6 lg:px-8 pb-12"}
        ($ :div {:class "transform hover:scale-[1.01] transition-all duration-300"}
          ($ recent-activity-table {:recent-activity recent-activity
                                    :loading? loading?}))))))

(defui admin-dashboard-page []
  ($ layout/admin-layout
    ($ admin-dashboard-content)))
