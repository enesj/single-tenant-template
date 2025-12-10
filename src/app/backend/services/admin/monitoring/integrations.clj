(ns app.backend.services.admin.monitoring.integrations
  "Admin integration and API monitoring.

   This namespace handles:
   - External integration health monitoring
   - API performance and response time tracking
   - Webhook delivery status and failure patterns
   - Integration error analysis"
  (:require
    [app.backend.services.admin.audit :as audit-service]
    [app.backend.services.admin.monitoring.shared :as monitoring-shared]
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Integration Overview
;; ============================================================================

(defn get-integration-overview
  "Get overview of external integration health and status"
  [db]
  (monitoring-shared/with-monitoring-error-handling
    "get integration overview"
    (fn [[integration-events active-integrations integration-errors api-usage]]
      {:integration-events integration-events
       :active-integrations active-integrations
       :integration-errors integration-errors
       :api-usage api-usage
       :health-status "operational"})
    {:integration-events []
     :active-integrations []
     :integration-errors []
     :api-usage {:total-api-calls 0 :active-tenants 0}
     :health-status "unknown"}
    (fn []
      (let [time-periods (monitoring-shared/standard-time-periods)

            ;; Recent integration-related audit events
            integration-events (->> (jdbc/execute! db
                                      (hsql/format {:select [:action :entity_type [[:count :*] :event_count]]
                                                    :from [:audit_logs]
                                                    :where [:and
                                                            [:or
                                                             [:like :action "%sync%"]
                                                             [:like :action "%import%"]
                                                             [:like :action "%export%"]
                                                             [:like :action "%webhook%"]
                                                             [:like :action "%api%"]]
                                                            [:>= :created_at (:days-7 time-periods)]]
                                                    :group-by [:action :entity_type]
                                                    :order-by [[[:count :*] :desc]]}))
                                 (mapv db-adapter/convert-pg-objects))

            ;; Active integrations using shared tenant activity summary
            active-integrations (monitoring-shared/tenant-activity-summary
                                  db (:hour-24 time-periods) 20)

            ;; Integration errors using shared error patterns query
            integration-errors (monitoring-shared/audit-error-patterns-query
                                 db (:days-7 time-periods)
                                 {:action-pattern "%integration%"})

            ;; API usage using shared API usage summary
            api-usage (monitoring-shared/api-usage-summary db (:hour-24 time-periods))]

        [integration-events active-integrations integration-errors api-usage]))))

;; ============================================================================
;; Integration Performance
;; ============================================================================

(defn get-integration-performance
  "Get integration performance metrics and response times"
  [db {:keys [hours] :or {hours 24}}]
  (try
    (let [hours-ago (time/minus (time/instant) (time/hours hours))

          ;; API call frequency by hour
          api-frequency (->> (jdbc/execute! db
                               ["SELECT date_trunc('hour', created_at) as hour,
                           COUNT(*) as api_calls,
                           COUNT(DISTINCT tenant_id) as unique_tenants
                           FROM audit_logs
                           WHERE created_at >= ?
                           GROUP BY date_trunc('hour', created_at)
                           ORDER BY hour DESC"
                                hours-ago])
                          (mapv db-adapter/convert-pg-objects))

          ;; Error rates by integration type (using action patterns)
          error-rates (->> (jdbc/execute! db
                             (hsql/format {:select [:action
                                                    [[:count :*] :total_calls]
                                                    [[:count :*] :error_count]]
                                           :from [:audit_logs]
                                           :where [:and
                                                   [:or
                                                    [:like :action "%api%"]
                                                    [:like :action "%sync%"]
                                                    [:like :action "%webhook%"]]
                                                   [:>= :created_at hours-ago]]
                                           :group-by [:action]
                                           :order-by [[:action :asc]]}))
                        (mapv db-adapter/convert-pg-objects))

          ;; Slowest operations (using created_at vs updated_at if available)
          slow-operations (->> (jdbc/execute! db
                                 (hsql/format {:select [:id :action :entity_type :tenant_id :created_at]
                                               :from [:audit_logs]
                                               :where [:and
                                                       [:like :action "%sync%"]
                                                       [:>= :created_at hours-ago]]
                                               :order-by [[:created_at :desc]]
                                               :limit 20}))
                            (mapv audit-service/db-audit-log->app))]

      ;; Return kebab-case keys consistently
      {:api-frequency api-frequency
       :error-rates error-rates
       :slow-operations slow-operations})
    (catch Exception e
      (log/error e "Failed to get integration performance")
      {:api-frequency []
       :error-rates []
       :slow-operations []})))

;; ============================================================================
;; Webhook Status
;; ============================================================================

(defn get-webhook-status
  "Get webhook delivery status and failure patterns"
  [db {:keys [limit offset] :or {limit 50 offset 0}}]
  (try
    (let [;; Recent webhook-related events (using audit logs as proxy)
          webhook-events (->> (jdbc/execute! db
                                (hsql/format {:select [:id :tenant_id :action :entity_type :changes :created_at]
                                              :from [:audit_logs]
                                              :where [:and
                                                      [:like :action "%webhook%"]
                                                      [:>= :created_at (time/minus (time/instant) (time/days 7))]]
                                              :order-by [[:created_at :desc]]
                                              :limit limit
                                              :offset offset}))
                           (mapv audit-service/db-audit-log->app))

          ;; Webhook failure patterns
          webhook-failures (->> (jdbc/execute! db
                                  (hsql/format {:select [:tenant_id [[:count :*] :failure_count]]
                                                :from [:audit_logs]
                                                :where [:and
                                                        [:like :action "%webhook%"]
                                                        [:or
                                                         [:like :changes "%failed%"]
                                                         [:like :changes "%error%"]
                                                         [:like :changes "%timeout%"]]
                                                        [:>= :created_at (time/minus (time/instant) (time/days 7))]]
                                                :group-by [:tenant_id]
                                                :order-by [[[:count :*] :desc]]
                                                :limit 10}))
                             (mapv db-adapter/convert-pg-objects))

          ;; Webhook delivery success rate
          success-rate (some-> (jdbc/execute-one! db
                                 (hsql/format {:select [[[:count :*] :total_webhooks]
                                                        [[:count :*] :successful_webhooks]]
                                               :from [:audit_logs]
                                               :where [:and
                                                       [:like :action "%webhook%"]
                                                       [:>= :created_at (time/minus (time/instant) (time/days 1))]]}))
                         db-adapter/convert-pg-objects)]

      ;; Return kebab-case keys consistently
      {:webhook-events webhook-events
       :webhook-failures webhook-failures
       :success-rate success-rate})
    (catch Exception e
      (log/error e "Failed to get webhook status")
      {:webhook-events []
       :webhook-failures []
       :success-rate {:total-webhooks 0 :successful-webhooks 0}})))
