(ns app.backend.services.admin.monitoring.transactions
  "Admin transaction monitoring and analytics.

   This namespace handles:
   - Transaction volume and trends analysis
   - High-value transaction monitoring
   - Suspicious transaction pattern detection
   - Tenant spending analytics"
  (:require
    [app.backend.services.admin.monitoring.shared :as monitoring-shared]
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]))

;; ============================================================================
;; Transaction Overview
;; ============================================================================

(defn get-transaction-overview
  "Get high-level transaction metrics for admin monitoring"
  [db]
  (monitoring-shared/with-monitoring-error-handling
    "get transaction overview"
    (fn [results]
      (let [[volume-30d volume-by-type top-spending-tenants high-value-transactions] results]
        {:volume-30d volume-30d
         :volume-by-type volume-by-type
         :top-spending-tenants top-spending-tenants
         :high-value-transactions high-value-transactions}))
    {:volume-30d {:total-volume 0 :transaction-count 0}
     :volume-by-type []
     :top-spending-tenants []
     :high-value-transactions []}
    (fn []
      (let [time-periods (monitoring-shared/standard-time-periods)

            ;; Total transaction volume last 30 days - Fixed HoneySQL aggregate syntax
            volume-30d (jdbc/execute-one! db
                         (hsql/format {:select [[[:sum :amount] :total-volume] [[:count :*] :transaction-count]]
                                       :from [:transactions_v2]
                                       :where [:>= :created_at (:days-30 time-periods)]}))

            ;; Transaction volume by type - Fixed HoneySQL aggregate syntax
            volume-by-type (jdbc/execute! db
                             (hsql/format {:select [:tt.name :tt.flow [[:sum :t.amount] :total-amount] [[:count :*] :count]]
                                           :from [[:transactions_v2 :t]]
                                           :join [[:transaction_types_v2 :tt] [:= :t.transaction_type_id :tt.id]]
                                           :where [:>= :t.created_at (:days-30 time-periods)]
                                           :group-by [:tt.name :tt.flow]
                                           :order-by [[[:sum :t.amount] :desc]]}))

            ;; Top spending tenants - Fixed HoneySQL aggregate syntax
            top-spending-tenants (jdbc/execute! db
                                   (hsql/format {:select [:tn.name :tn.id [[:sum :t.amount] :total-spending] [[:count :t.*] :transaction-count]]
                                                 :from [[:transactions_v2 :t]]
                                                 :join [[:tenants :tn] [:= :t.tenant_id :tn.id]]
                                                 :where [:>= :t.created_at (:days-30 time-periods)]
                                                 :group-by [:tn.name :tn.id]
                                                 :order-by [[[:sum :t.amount] :desc]]
                                                 :limit 10}))

            ;; Recent high-value transactions - Fixed column aliases and HoneySQL syntax
            high-value-transactions (jdbc/execute! db
                                      (hsql/format {:select [:t.id :t.description :t.amount :t.date
                                                             [:tn.name :tenant-name] [:tt.name :transaction-type] :u.full-name]
                                                    :from [[:transactions_v2 :t]]
                                                    :join [[:tenants :tn] [:= :t.tenant_id :tn.id]
                                                           [:transaction_types_v2 :tt] [:= :t.transaction_type_id :tt.id]
                                                           [:users :u] [:= :t.created_by :u.id]]
                                                    :where [:and
                                                            [:>= :t.created_at (:days-7 time-periods)]
                                                            [:> :t.amount 1000]]
                                                    :order-by [[:t.amount :desc]]
                                                    :limit 20}))]

        [volume-30d volume-by-type top-spending-tenants high-value-transactions]))))

;; ============================================================================
;; Transaction Trends
;; ============================================================================

(defn get-transaction-trends
  "Get transaction trends and analytics over time"
  [db {:keys [months] :or {months 12}}]
  (monitoring-shared/with-monitoring-error-handling
    "get transaction trends"
    (fn [results]
      (let [[volume-trends flow-trends error-patterns] results]
        {:volume-trends volume-trends
         :flow-trends flow-trends
         :error-patterns error-patterns}))
    {:volume-trends []
     :flow-trends []
     :error-patterns []}
    (fn []
      (let [months-ago (-> (time/offset-date-time)
                         (time/minus (time/months months))
                         (time/instant))

            ;; Transaction volume trends by month
            volume-trends (jdbc/execute! db
                            ["SELECT date_trunc('month', created_at) as month,
                              SUM(amount) as total_volume,
                              COUNT(*) as transaction_count,
                              AVG(amount) as avg_transaction_size
                              FROM transactions_v2
                              WHERE created_at >= ?
                              GROUP BY date_trunc('month', created_at)
                              ORDER BY month DESC"
                             months-ago])

            ;; Income vs Expense trends
            flow-trends (jdbc/execute! db
                          ["SELECT date_trunc('month', t.created_at) as month,
                            tt.flow,
                            SUM(t.amount) as total_amount,
                            COUNT(*) as count
                            FROM transactions_v2 t
                            JOIN transaction_types_v2 tt ON t.transaction_type_id = tt.id
                            WHERE t.created_at >= ?
                            GROUP BY date_trunc('month', t.created_at), tt.flow
                            ORDER BY month DESC"
                           months-ago])

            ;; Failed transaction patterns - Fixed LIKE pattern escaping
            error-patterns (->> (jdbc/execute! db
                                  ["SELECT tenant_id, COUNT(*) as error_count
                                    FROM audit_logs
                                    WHERE action LIKE ?
                                    AND CAST(changes AS TEXT) LIKE ?
                                    GROUP BY tenant_id
                                    HAVING COUNT(*) >= 1
                                    ORDER BY COUNT(*) DESC
                                    LIMIT 10"
                                   "%transaction%" "%error%"])
                             (mapv db-adapter/convert-pg-objects))]

        [volume-trends flow-trends error-patterns]))))

;; ============================================================================
;; Suspicious Transaction Detection
;; ============================================================================

(defn get-suspicious-transactions
  "Identify potentially suspicious transaction patterns"
  [db {:keys [limit offset] :or {limit 50 offset 0}}]
  (monitoring-shared/with-monitoring-error-handling
    "get suspicious transactions"
    (fn [results]
      (let [[rapid-large-transactions unusual-patterns] results]
        {:rapid-large-transactions rapid-large-transactions
         :unusual-patterns unusual-patterns}))
    {:rapid-large-transactions []
     :unusual-patterns []}
    (fn []
      (let [;; Large transactions in short timeframe
            rapid-large-transactions (jdbc/execute! db
                                       (hsql/format {:select [:t.id :t.description :t.amount :t.date :tn.name :u.full-name :t.created_at]
                                                     :from [[:transactions_v2 :t]]
                                                     :join [[:tenants :tn] [:= :t.tenant_id :tn.id]
                                                            [:users :u] [:= :t.created_by :u.id]]
                                                     :where [:and
                                                             [:> :t.amount 5000]
                                                             [:>= :t.created_at (time/minus (time/instant) (time/hours 24))]]
                                                     :order-by [[:t.created_at :desc]]
                                                     :limit limit
                                                     :offset offset}))

            ;; Unusual spending patterns (much higher than tenant average)
            unusual-patterns (jdbc/execute! db
                               ["WITH tenant_averages AS (
                                  SELECT tenant_id, AVG(amount) as avg_amount, STDDEV(amount) as stddev_amount
                                  FROM transactions_v2
                                  WHERE created_at >= ?
                                  GROUP BY tenant_id
                                )
                                SELECT t.id, t.description, t.amount, t.date, tn.name, u.full_name as full_name,
                                       ta.avg_amount, t.created_at
                                FROM transactions_v2 t
                                JOIN tenant_averages ta ON t.tenant_id = ta.tenant_id
                                JOIN tenants tn ON t.tenant_id = tn.id
                                JOIN users u ON t.created_by = u.id
                                WHERE t.amount > (ta.avg_amount + (2 * COALESCE(ta.stddev_amount, ta.avg_amount)))
                                AND t.created_at >= ?
                                ORDER BY t.created_at DESC
                                LIMIT ? OFFSET ?"
                                (time/minus (time/instant) (time/days 30))
                                (time/minus (time/instant) (time/days 7))
                                limit offset])]

        [rapid-large-transactions unusual-patterns]))))
