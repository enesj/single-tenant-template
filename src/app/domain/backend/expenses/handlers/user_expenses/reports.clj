(ns app.domain.backend.expenses.handlers.user-expenses.reports
  "User-scoped reporting handlers for `/api/v1/expenses/reports/*`."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.user-expense-reports :as reports]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset YearMonth]))

(def ^:private invalid ::invalid)

(def ^:private month-pattern
  #"^\d{4}-(0[1-9]|1[0-2])$")

(defn- parse-int-param
  [params k default-value min-value max-value]
  (let [raw (h/get-param params k)
        parsed (if (nil? raw)
                 default-value
                 (try
                   (some-> raw str parse-long)
                   (catch Exception _ invalid)))]
    (if (or (nil? parsed) (= invalid parsed))
      invalid
      (-> parsed long (max min-value) (min max-value)))))

(defn- parse-instant-param
  [raw]
  (if (str/blank? (str (or raw "")))
    nil
    (or
      (try
        (Instant/parse (str raw))
        (catch Exception _ nil))
      (try
        (-> (OffsetDateTime/parse (str raw)) .toInstant)
        (catch Exception _ nil))
      (try
        (-> (LocalDateTime/parse (str raw))
          (.atZone (ZoneId/systemDefault))
          .toInstant)
        (catch Exception _ nil))
      (try
        (-> (LocalDate/parse (str raw))
          (.atStartOfDay ZoneOffset/UTC)
          .toInstant)
        (catch Exception _ nil))
      invalid)))

(defn- parse-uuid-param
  [params k]
  (let [raw (h/get-param params k)]
    (if (str/blank? (str (or raw "")))
      nil
      (or (h/try-parse-uuid raw) invalid))))

(defn- parse-month-param
  [params k]
  (let [month (some-> (h/get-param params k) str str/trim)]
    (cond
      (str/blank? month) nil
      (re-matches month-pattern month) month
      :else invalid)))

(defn- month->year-month
  [month]
  (try
    (YearMonth/parse month)
    (catch Exception _ invalid)))

(defn- parse-common-report-opts
  [params]
  (let [from (parse-instant-param (h/get-param params :from))
        to (parse-instant-param (h/get-param params :to))
        supplier-id (parse-uuid-param params :supplier_id)
        payer-id (parse-uuid-param params :payer_id)
        month (parse-month-param params :month)
        currency-raw (some-> (h/get-param params :currency) str str/trim str/upper-case)
        currency (when-not (str/blank? currency-raw) currency-raw)]
    (cond
      (= invalid from) {:error (h/json-response {:error "Invalid from date/time"} 400)}
      (= invalid to) {:error (h/json-response {:error "Invalid to date/time"} 400)}
      (= invalid supplier-id) {:error (h/json-response {:error "Invalid supplier_id"} 400)}
      (= invalid payer-id) {:error (h/json-response {:error "Invalid payer_id"} 400)}
      (= invalid month) {:error (h/json-response {:error "Invalid month format (expected YYYY-MM)"} 400)}
      (and month (= invalid (month->year-month month)))
      {:error (h/json-response {:error "Invalid month format (expected YYYY-MM)"} 400)}

      :else
      {:opts (cond-> {}
               from (assoc :from from)
               to (assoc :to to)
               supplier-id (assoc :supplier-id supplier-id)
               payer-id (assoc :payer-id payer-id)
               month (assoc :month month)
               currency (assoc :currency currency))})))

(defn- with-user-report-access
  [request on-success]
  (if-let [user-id (h/get-user-id request)]
    (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
      forbidden
      (on-success user-id))
    (h/unauthorized-response)))

(defn supplier-deep-dive-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)
                alias-limit (parse-int-param params :alias_limit 10 1 100)]
            (cond
              error error
              (= invalid alias-limit) (h/json-response {:error "Invalid alias_limit"} 400)
              (nil? (:supplier-id opts)) (h/json-response {:error "supplier_id is required"} 400)
              :else
              (h/json-response
                {:data (reports/get-user-supplier-deep-dive
                         db
                         user-id
                         (assoc opts :alias-limit alias-limit))})))
          (catch Exception e
            (log/error e "Error getting supplier deep-dive report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get supplier deep-dive report"} 500)))))))

(defn day-of-week-spending-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)]
            (if error
              error
              (h/json-response {:data (reports/get-user-day-of-week-spending-pattern db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting day-of-week spending report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get day-of-week spending report"} 500)))))))

(defn top-items-spending-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)
                limit (parse-int-param params :limit 20 1 100)]
            (cond
              error error
              (= invalid limit) (h/json-response {:error "Invalid limit"} 400)
              :else
              (h/json-response {:data (reports/get-user-top-item-spending db user-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting top items spending report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get top items spending report"} 500)))))))

(defn monthly-comparison-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)
                month-a (parse-month-param params :month_a)
                month-b (parse-month-param params :month_b)]
            (cond
              error error
              (nil? month-a) (h/json-response {:error "month_a is required (YYYY-MM)"} 400)
              (nil? month-b) (h/json-response {:error "month_b is required (YYYY-MM)"} 400)
              (= invalid month-a) (h/json-response {:error "Invalid month_a format (expected YYYY-MM)"} 400)
              (= invalid month-b) (h/json-response {:error "Invalid month_b format (expected YYYY-MM)"} 400)
              (= invalid (month->year-month month-a)) (h/json-response {:error "Invalid month_a format (expected YYYY-MM)"} 400)
              (= invalid (month->year-month month-b)) (h/json-response {:error "Invalid month_b format (expected YYYY-MM)"} 400)
              :else
              (h/json-response
                {:data (reports/get-user-monthly-comparison
                         db
                         user-id
                         (assoc opts :month-a month-a :month-b month-b))})))
          (catch Exception e
            (log/error e "Error getting monthly comparison report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get monthly comparison report"} 500)))))))

(defn expense-size-distribution-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)]
            (if error
              error
              (h/json-response {:data (reports/get-user-expense-size-distribution db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting expense size distribution report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get expense size distribution report"} 500)))))))

(defn daily-heatmap-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)]
            (if error
              error
              (h/json-response {:data (reports/get-user-daily-heatmap db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting daily heatmap report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get daily heatmap report"} 500)))))))

(defn category-allocation-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)]
            (if error
              error
              (h/json-response {:data (reports/get-user-category-allocation db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting category allocation report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get category allocation report"} 500)))))))
