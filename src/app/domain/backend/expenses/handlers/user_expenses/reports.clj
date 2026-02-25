(ns app.domain.backend.expenses.handlers.user-expenses.reports
  "User-scoped reporting handlers for `/api/v1/expenses/reports/*`."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.user-expense-reports.categories :as report-categories]
    [app.domain.backend.expenses.services.user-expense-reports.filters :as report-filters]
    [app.domain.backend.expenses.services.user-expense-reports.items :as report-items]
    [app.domain.backend.expenses.services.user-expense-reports.suppliers :as report-suppliers]
    [app.domain.backend.expenses.services.user-expense-reports.time :as report-time]
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
  (let [raw (h/get-param params k)
        raw-values (cond
                     (nil? raw) []
                     (sequential? raw) raw
                     (string? raw) (str/split raw #",")
                     :else [raw])
        normalized-values (->> raw-values
                            (map #(some-> % str str/trim))
                            (remove str/blank?)
                            vec)]
    (if (empty? normalized-values)
      nil
      (let [parsed-values (mapv h/try-parse-uuid normalized-values)]
        (if (some nil? parsed-values)
          invalid
          (if (= 1 (count parsed-values))
            (first parsed-values)
            parsed-values))))))

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
        category-id (parse-uuid-param params :category_id)
        subcategory-id (parse-uuid-param params :subcategory_id)
        expense-category-id (parse-uuid-param params :expense_category_id)
        manufacturer-id (parse-uuid-param params :manufacturer_id)
        month (parse-month-param params :month)
        currency-raw (some-> (h/get-param params :currency) str str/trim str/upper-case)
        currency (when-not (str/blank? currency-raw) currency-raw)]
    (cond
      (= invalid from) {:error (h/json-response {:error "Invalid from date/time"} 400)}
      (= invalid to) {:error (h/json-response {:error "Invalid to date/time"} 400)}
      (= invalid supplier-id) {:error (h/json-response {:error "Invalid supplier_id"} 400)}
      (= invalid payer-id) {:error (h/json-response {:error "Invalid payer_id"} 400)}
      (= invalid category-id) {:error (h/json-response {:error "Invalid category_id"} 400)}
      (= invalid subcategory-id) {:error (h/json-response {:error "Invalid subcategory_id"} 400)}
      (= invalid expense-category-id) {:error (h/json-response {:error "Invalid expense_category_id"} 400)}
      (= invalid manufacturer-id) {:error (h/json-response {:error "Invalid manufacturer_id"} 400)}
      (= invalid month) {:error (h/json-response {:error "Invalid month format (expected YYYY-MM)"} 400)}
      (and month (= invalid (month->year-month month)))
      {:error (h/json-response {:error "Invalid month format (expected YYYY-MM)"} 400)}

      :else
      {:opts (cond-> {}
               from (assoc :from from)
               to (assoc :to to)
               supplier-id (assoc :supplier-id supplier-id)
               payer-id (assoc :payer-id payer-id)
               category-id (assoc :category-id category-id)
               subcategory-id (assoc :subcategory-id subcategory-id)
               expense-category-id (assoc :expense-category-id expense-category-id)
               manufacturer-id (assoc :manufacturer-id manufacturer-id)
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
                {:data (report-suppliers/deep-dive
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
              (h/json-response {:data (report-time/day-of-week db user-id opts)})))
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
              (h/json-response {:data (report-items/top-spending db user-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting top items spending report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get top items spending report"} 500)))))))

(defn top-item-breakdown-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)
                alias-id (or (h/try-parse-uuid (get-in request [:path-params :alias-id]))
                           (h/try-parse-uuid (get-in request [:parameters :path :alias-id])))
                limit (parse-int-param params :limit 50 1 200)]
            (cond
              error error
              (nil? alias-id) (h/json-response {:error "Invalid alias id"} 400)
              (= invalid limit) (h/json-response {:error "Invalid limit"} 400)
              :else
              (h/json-response
                {:data (report-items/top-breakdown db user-id alias-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting top item breakdown"
              {:user-id user-id
               :alias-id (or (get-in request [:path-params :alias-id])
                           (get-in request [:parameters :path :alias-id]))
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get top item breakdown"} 500)))))))

(defn top-suppliers-handler
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
              (h/json-response {:data (report-suppliers/top db user-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting top suppliers report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get top suppliers report"} 500)))))))

(defn supplier-stores-handler
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
              (nil? (:supplier-id opts)) (h/json-response {:error "supplier_id is required"} 400)
              (sequential? (:supplier-id opts)) (h/json-response {:error "supplier_id must be a single UUID"} 400)
              :else
              (h/json-response
                {:data (report-suppliers/stores db user-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting supplier stores report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get supplier stores report"} 500)))))))

(defn supplier-monthly-trends-handler
  [db]
  (fn [request]
    (with-user-report-access
      request
      (fn [user-id]
        (try
          (let [params (:query-params request)
                {:keys [error opts]} (parse-common-report-opts params)
                limit (parse-int-param params :limit 10 1 50)]
            (cond
              error error
              (= invalid limit) (h/json-response {:error "Invalid limit"} 400)
              :else
              (h/json-response {:data (report-suppliers/monthly-trends db user-id (assoc opts :limit limit))})))
          (catch Exception e
            (log/error e "Error getting supplier monthly trends report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get supplier monthly trends report"} 500)))))))

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
                {:data (report-time/monthly-comparison
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
              (h/json-response {:data (report-time/size-distribution db user-id opts)})))
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
              (h/json-response {:data (report-time/daily-heatmap db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting daily heatmap report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get daily heatmap report"} 500)))))))

(defn filter-options-handler
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
              (h/json-response {:data (report-filters/options db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting report filter options"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get report filter options"} 500)))))))

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
              (h/json-response {:data (report-categories/allocation db user-id opts)})))
          (catch Exception e
            (log/error e "Error getting category allocation report"
              {:user-id user-id
               :query-params (:query-params request)
               :message (.getMessage e)})
            (h/json-response {:error "Failed to get category allocation report"} 500)))))))
