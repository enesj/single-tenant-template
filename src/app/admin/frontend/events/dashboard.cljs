(ns app.admin.frontend.events.dashboard
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.frontend.utils.state :as state-utils]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(rf/reg-event-fx
  :admin/load-dashboard
  (fn [{:keys [db]} _]
    (let [authenticated? (:admin/authenticated? db)]
      (if authenticated?
        {:db (state-utils/create-loading-state db :admin/dashboard-loading? :admin/dashboard-error)
         :http-xhrio (admin-http/dashboard-request
                       {:on-success [:admin/dashboard-loaded]
                        :on-failure [:admin/dashboard-load-failed]})}
        ;; If not authenticated, don't make the API call
        {:db (state-utils/create-error-state db :admin/dashboard-error nil "Authentication required")}))))

(rf/reg-event-db
  :admin/dashboard-loaded
  (fn [db [_ stats]]
    (let [recent-events (get stats :recent-events [])
          recent-activity (mapv (fn [event-map]
                                  (let [action (if (map? event-map)
                                                 (get event-map :action "unknown")
                                                 (str event-map))
                                        count (if (map? event-map)
                                                (get event-map :count 0)
                                                1)]
                                    {:id (str (random-uuid))
                                     :type (str action)
                                     :description (str "Admin action: " action " (" count ")")
                                     :timestamp "Recent"
                                     :status "completed"}))
                            (take 5 recent-events))]
      (-> db
        (assoc :admin/dashboard-stats stats
          :admin/recent-activity recent-activity)
        (state-utils/clear-loading-state :admin/dashboard-loading?)))))

(rf/reg-event-db
  :admin/dashboard-load-failed
  (fn [db [_ _]]
    (state-utils/create-error-state db :admin/dashboard-error :admin/dashboard-loading? "Failed to load dashboard")))

;; Advanced Dashboard Events
(rf/reg-event-fx
  :admin/load-advanced-dashboard
  (fn [{:keys [db]} _]
    (let [authenticated? (:admin/authenticated? db)]
      (if authenticated?
        {:db (state-utils/create-loading-state db :admin/advanced-dashboard-loading? :admin/advanced-dashboard-error)
         :http-xhrio (admin-http/dashboard-request
                       {:uri "/admin/api/advanced-dashboard"
                        :on-success [:admin/advanced-dashboard-loaded]
                        :on-failure [:admin/advanced-dashboard-load-failed]})}
        ;; If not authenticated, don't make the API call
        {:db (state-utils/create-error-state db :admin/advanced-dashboard-error nil "Authentication required")}))))

(rf/reg-event-db
  :admin/advanced-dashboard-loaded
  (fn [db [_ data]]
    (let [tenant-metrics (get data :tenant-metrics {})
          user-metrics (get data :user-metrics {})
          risk-alerts (get data :risk-alerts [])
          tenant-spec (get data :tenant-spec {})
          user-spec (get data :user-spec {})]
      (-> db
        (assoc :admin/computed-tenant-metrics tenant-metrics
          :admin/computed-user-metrics user-metrics
          :admin/high-risk-alerts risk-alerts
          :admin/enhanced-tenant-spec tenant-spec
          :admin/enhanced-user-spec user-spec)
        (state-utils/clear-loading-state :admin/advanced-dashboard-loading?)))))

(rf/reg-event-db
  :admin/advanced-dashboard-load-failed
  (fn [db [_ _]]
    (state-utils/create-error-state db :admin/advanced-dashboard-error :admin/advanced-dashboard-loading? "Failed to load advanced dashboard")))

;; Load raw models data with metadata for admin interface
