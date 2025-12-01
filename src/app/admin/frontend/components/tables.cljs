(ns app.admin.frontend.components.tables
  "Lightweight admin tables used by the dashboard."
  (:require
    [app.template.frontend.components.states :refer [activity-empty-state
                                                     activity-loading-state]]
    [app.template.frontend.components.table-headers :refer [table-header]]
    [uix.core :refer [$ defui]]))

(defui activity-list-item
  "Single activity row."
  [{:keys [index activity]}]
  ($ :li {:key index :class "p-4 hover:bg-base-200/30 transition-colors duration-200"}
    ($ :div {:class "flex items-center justify-between"}
      ($ :div {:class "flex items-center gap-3"}
        ($ :div {:class "w-8 h-8 bg-gradient-to-br from-info to-info/80 rounded-full flex items-center justify-center text-white text-xs font-bold"}
          (str (inc index)))
        ($ :div {:class "flex-1"}
          ($ :div {:class "font-medium text-sm text-base-content"}
            (or (:action activity) "System Activity"))
          ($ :div {:class "text-xs text-base-content/60 mt-1"}
            (or (:timestamp activity) "Unknown time"))))
      ($ :div {:class "ds-badge ds-badge-ghost ds-badge-xs"}
        (or (:type activity) "Info")))))

(defui recent-activity-table
  "Recent admin activity list with loading/empty states."
  [{:keys [recent-activity loading?] :or {recent-activity []}}]
  ($ :div {:class "ds-card bg-base-100 shadow-xl hover:shadow-2xl transition-all duration-300 border border-base-300/50 hover:scale-[1.02] group"}
    ($ :div {:class "ds-card-body p-6"}
      ($ table-header {:title "Recent Activity"
                       :badge-text "📊 Activity Log"
                       :color "info"
                       :icon-gradient "from-info to-info/70"})
      ($ :p {:class "text-base-content/70 mb-6"} "Latest system events and administrative actions")
      (cond
        loading? ($ activity-loading-state {})
        (empty? recent-activity) ($ activity-empty-state {})
        :else ($ :div {:class "bg-base-100/50 backdrop-blur-sm rounded-lg border border-base-300/30 overflow-hidden"}
                ($ :ul {:class "divide-y divide-base-300/30"}
                  (into [] (map-indexed
                             (fn [idx activity]
                               ($ activity-list-item {:key idx :index idx :activity activity}))
                             recent-activity)))))
      ($ :div {:class "absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-info/30 via-info/20 to-transparent group-hover:from-info/60 group-hover:via-info/40 group-hover:to-transparent transition-all duration-300 rounded-b-2xl"}))))
