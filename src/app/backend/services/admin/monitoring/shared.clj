(ns app.backend.services.admin.monitoring.shared
  "Shared utilities for admin monitoring services.

   This namespace provides common patterns used across multiple monitoring services:
   - Standardized error handling with normalization
   - Common audit log query patterns
   - Time-based aggregation helpers
   - Standard monitoring result structure"
  (:require
    [app.shared.adapters.database :as db-adapter]
    [clojure.string :as str]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Error Handling Patterns
;; ============================================================================

(defn with-monitoring-error-handling
  "Standardized error handling wrapper for monitoring functions.
   Applies recursive normalization to both success and error cases."
  [operation-name success-data error-data thunk]
  (try
    (log/info (str "🔧 Starting monitoring operation: " operation-name))
    (let [raw-result (thunk)]
      (log/info (str "✅ Raw result for " operation-name ": " (pr-str raw-result)))
      (let [processed-result (success-data raw-result)]
        (log/info (str "🔄 Processed result for " operation-name ": " (pr-str processed-result)))
        ;; Use the database adapter for normalization instead of non-existent function
        (db-adapter/convert-pg-objects processed-result)))
    (catch Exception e
      (log/error e (str "❌ Failed to " operation-name))
      (log/error (str "❌ Exception type: " (.getName (class e))))
      (log/error (str "❌ Exception message: " (.getMessage e)))
      (log/error (str "❌ Exception data: " (ex-data e)))
      (when-let [cause (.getCause e)]
        (log/error (str "❌ Caused by: " (.getName (class cause)) " - " (.getMessage cause))))
      ;; Use the database adapter for normalization instead of non-existent function
      (db-adapter/convert-pg-objects error-data))))

(defmacro def-monitoring-fn
  "Macro to define monitoring functions with standardized error handling."
  [fn-name doc-string args success-keys error-defaults & body]
  `(defn ~fn-name
     ~doc-string
     ~args
     (with-monitoring-error-handling
       ~(str fn-name)
       (fn [data#] (zipmap ~success-keys data#))
       ~error-defaults
       (fn [] [~@body]))))

;; ============================================================================
;; Common Query Patterns
;; ============================================================================

(defn audit-error-patterns-query
  "Standard query for finding error patterns in audit logs by tenant.
   Returns normalized error counts grouped by tenant_id."
  [db time-threshold & [{:keys [action-pattern changes-patterns limit]
                         :or {changes-patterns ["%error%" "%failed%" "%timeout%"]
                              limit 10}}]]
  ;; Use raw SQL to properly handle LIKE patterns with % characters
  (let [base-sql "SELECT tenant_id, COUNT(*) as error_count
                  FROM audit_logs
                  WHERE created_at >= ?"

        ;; Build WHERE conditions based on provided patterns
        action-condition (when action-pattern " AND action LIKE ?")

        ;; Build OR conditions for changes patterns
        changes-conditions (when (seq changes-patterns)
                             (str " AND ("
                               (str/join " OR " (repeat (count changes-patterns) "CAST(changes AS TEXT) LIKE ?"))
                               ")"))

        ;; Complete SQL
        full-sql (str base-sql
                   action-condition
                   changes-conditions
                   " GROUP BY tenant_id"
                   " ORDER BY COUNT(*) DESC"
                   " LIMIT ?")

        ;; Build parameters
        params (concat [time-threshold]
                 (when action-pattern [action-pattern])
                 changes-patterns
                 [limit])]

    (->> (jdbc/execute! db (into [full-sql] params))
      (mapv #(db-adapter/convert-pg-objects %)))))

(defn time-aggregated-query
  "Standard time-based aggregation query pattern.
   period: :hour, :day, :month
   Returns data aggregated by time periods."
  [db table period time-threshold select-clauses & [{:keys [where-clauses join-clauses group-by-extra limit]
                                                     :or {limit 50}}]]
  (let [period-fn (case period
                    :hour "date_trunc('hour', created_at)"
                    :day "date_trunc('day', created_at)"
                    :month "date_trunc('month', created_at)"
                    :week "date_trunc('week', created_at)")
        base-select (concat [[period-fn :period]] select-clauses)
        base-where (concat [[:>= :created_at time-threshold]] (or where-clauses []))
        base-group-by (concat [:period] (or group-by-extra []))]

    (jdbc/execute! db
      [(str "SELECT " (clojure.string/join ", " (mapv (fn [clause]
                                                        (if (vector? clause)
                                                          (str (second clause) " as " (name (first clause)))
                                                          (str clause))) base-select))
         " FROM " (name table)
         (when join-clauses (str " " (clojure.string/join " " join-clauses)))
         " WHERE " (clojure.string/join " AND " (mapv str base-where))
         " GROUP BY " (clojure.string/join ", " (mapv str base-group-by))
         " ORDER BY period DESC"
         (when limit (str " LIMIT " limit)))
       time-threshold])))

;; ============================================================================
;; Standard Monitoring Metrics
;; ============================================================================

(defn tenant-activity-summary
  "Get summary of tenant activity from audit logs.
   Returns top active tenants with normalized keys."
  [db time-threshold limit]
  (->> (jdbc/execute! db
         (hsql/format {:select [:t.name :t.id :t.subscription_tier [[:count :al.id] :activity_count]]
                       :from [[:tenants :t]]
                       :left-join [[:audit_logs :al] [:= :t.id :al.tenant_id]]
                       :where [:>= :al.created_at time-threshold]
                       :group-by [:t.name :t.id :t.subscription_tier]
                       :having [:> [:count :al.id] 5]
                       :order-by [[[:count :al.id] :desc]]
                       :limit limit}))
    (mapv #(db-adapter/convert-pg-objects %))))

(defn api-usage-summary
  "Get API usage summary from audit logs for a time period.
   Returns total calls and active tenants with normalized keys."
  [db time-threshold]
  (some-> (jdbc/execute-one! db
            (hsql/format {:select [[[:count :*] :total_api_calls]
                                   [[:count-distinct :tenant_id] :active_tenants]]
                          :from [:audit_logs]
                          :where [:>= :created_at time-threshold]}))
    db-adapter/convert-pg-objects))

(defn high-activity-items-query
  "Generic query for finding high-activity items above a threshold.
   table: table to query
   group-by-columns: columns to group results by
   activity-threshold: minimum activity count to include
   time-threshold: time cutoff for activity"
  [db table group-by-columns activity-threshold time-threshold limit]
  (->> (jdbc/execute! db
         (hsql/format {:select (concat group-by-columns [[[:count :*] :activity_count]])
                       :from [table]
                       :where [:>= :created_at time-threshold]
                       :group-by group-by-columns
                       :having [:> [:count :*] activity-threshold]
                       :order-by [[[:count :*] :desc]]
                       :limit limit}))
    (mapv #(db-adapter/convert-pg-objects %))))

;; ============================================================================
;; Time Period Helpers
;; ============================================================================

(defn period->instant
  "Convert a period keyword to an instant relative to now."
  [period amount]
  (case period
    :hours (time/minus (time/instant) (time/hours amount))
    :days (time/minus (time/instant) (time/days amount))
    :weeks (time/minus (time/instant) (time/weeks amount))
    :months (-> (time/offset-date-time)
              (time/minus (time/months amount))
              (time/instant))))

(defn standard-time-periods
  "Standard time periods used across monitoring services."
  []
  {:hour-24 (period->instant :hours 24)
   :days-7 (period->instant :days 7)
   :days-30 (period->instant :days 30)
   :months-12 (period->instant :months 12)})
