(ns app.backend.services.admin.dashboard
  "Admin dashboard statistics and overview metrics.

   This namespace handles:
   - Dashboard statistics collection
   - System overview metrics
   - Admin panel summary data"
  (:require
    [app.backend.services.admin.auth :as admin-auth]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Dashboard Statistics
;; ============================================================================

(defn get-dashboard-stats
  "Get statistics for admin dashboard based on simplified monitoring schema.

   Metrics include:
   - :total-admins      total number of admins
   - :active-sessions   number of in-memory admin sessions
   - :recent-activity   total audit log entries in the last 7 days
   - :recent-events     top actions in the last 7 days with counts"
  [db]
  (try
    (let [now (time/instant)
          seven-days-ago (time/minus now (time/days 7))
          ;; Total admins
          admin-count (try
                        (jdbc/execute-one! db
                          (hsql/format {:select [[[:count :*] :count]]
                                        :from [:admins]}))
                        (catch Exception _ {:count 0}))
          total-admins (or (:count admin-count) 0)

          ;; Active admin sessions are kept in an in-memory store
          active-sessions (try
                            (count @admin-auth/session-store)
                            (catch Exception _ 0))

          ;; Recent audit activity count
          recent-activity-count (try
                                  (let [row (jdbc/execute-one! db
                                              (hsql/format {:select [[[:count :*] :count]]
                                                            :from [:audit_logs]
                                                            :where [:>= :created_at seven-days-ago]}))]
                                    (or (:count row) 0))
                                  (catch Exception e
                                    (log/warn e "Failed to compute recent audit activity count")
                                    0))

          ;; Top recent events by action name for dashboard table
          recent-events (try
                          (jdbc/execute! db
                            (hsql/format {:select [:action [[:count :*] :count]]
                                          :from [:audit_logs]
                                          :where [:>= :created_at seven-days-ago]
                                          :group-by [:action]
                                          :order-by [[[:count :*] :desc]]
                                          :limit 10}))
                          (catch Exception e
                            (log/warn e "Failed to load recent audit events")
                            []))]
      {:total-admins total-admins
       :active-sessions active-sessions
       :recent-activity recent-activity-count
       :recent-events recent-events})
    (catch Exception e
      (log/error e "Failed to get dashboard stats")
      {:total-admins 0
       :active-sessions 0
       :recent-activity 0
       :recent-events []})))

(defn get-advanced-dashboard-data
  "Get advanced dashboard data with computed metrics and business intelligence"
  [_db]
  (try
    (let [;; Sample computed tenant metrics (in real implementation, these would be calculated)
          tenant-metrics {:active-tenants 42
                          :tenant-growth-rate 15.2
                          :tenant-growth-direction :up
                          :avg-health-score 87.5
                          :health-improvement-rate 5.8
                          :health-trend-direction :up
                          :monthly-revenue 125000
                          :revenue-growth-rate 12.3
                          :revenue-trend-direction :up
                          :top-healthy-tenants []}

          ;; Sample computed user metrics
          user-metrics {:high-risk-users 8
                        :risk-change-rate -2.1
                        :risk-trend-direction :down
                        :attention-required-users []}

          ;; Sample risk alerts
          risk-alerts [{:id "risk-1"
                        :title "High Risk Activity Detected"
                        :description "Unusual login patterns detected for multiple users"}
                       {:id "risk-2"
                        :title "Security Alert"
                        :description "Failed login attempts exceeding threshold"}]

          ;; Sample enhanced specs (in real implementation, these would be dynamic)
          tenant-spec {}
          user-spec {}]

      {:tenant-metrics tenant-metrics
       :user-metrics user-metrics
       :risk-alerts risk-alerts
       :tenant-spec tenant-spec
       :user-spec user-spec})
    (catch Exception e
      (log/error e "Failed to get advanced dashboard data")
      {:tenant-metrics {}
       :user-metrics {}
       :risk-alerts []
       :tenant-spec {}
       :user-spec {}})))
