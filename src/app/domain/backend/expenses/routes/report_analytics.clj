(ns app.domain.backend.expenses.routes.report-analytics
  "Admin analytics routes for reports that group by global entities.

  These reports aggregate expense data across all users/tenants
  (or optionally scoped to a single tenant via ?tenant_id query param).
  Endpoints: supplier deep-dive, top suppliers, supplier stores,
  supplier monthly trends, top items, top item breakdown,
  monthly comparison, and category allocation."
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.categories :as report-categories]
    [app.domain.backend.expenses.services.user-expense-reports.filters :as report-filters]
    [app.domain.backend.expenses.services.user-expense-reports.items :as report-items]
    [app.domain.backend.expenses.services.user-expense-reports.suppliers :as report-suppliers]
    [app.domain.backend.expenses.services.user-expense-reports.time :as report-time]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.string :as str])
  (:import
    [java.time Instant LocalDate LocalDateTime OffsetDateTime YearMonth ZoneId ZoneOffset]
    [java.util UUID]))

(def ^:private to-app shared-db/to-app)

;; ---------------------------------------------------------------------------
;; Parameter parsing (matches user-handler parsing but without auth coupling)
;; ---------------------------------------------------------------------------

(def ^:private month-pattern
  #"^\d{4}-(0[1-9]|1[0-2])$")

(defn- try-parse-uuid [s]
  (try (UUID/fromString (str s)) (catch Exception _ nil)))

(defn- parse-int-param
  [qp k default-value min-value max-value]
  (let [raw (get qp (name k) (get qp k))
        parsed (if (nil? raw) default-value (try (some-> raw str parse-long) (catch Exception _ nil)))]
    (some-> parsed long (max min-value) (min max-value))))

(defn- parse-instant-param
  [raw]
  (when-not (str/blank? (str (or raw "")))
    (or
      (try (Instant/parse (str raw)) (catch Exception _ nil))
      (try (-> (OffsetDateTime/parse (str raw)) .toInstant) (catch Exception _ nil))
      (try (-> (LocalDateTime/parse (str raw)) (.atZone (ZoneId/systemDefault)) .toInstant) (catch Exception _ nil))
      (try (-> (LocalDate/parse (str raw)) (.atStartOfDay ZoneOffset/UTC) .toInstant) (catch Exception _ nil)))))

(defn- parse-uuid-csv
  [qp k]
  (let [raw (get qp (name k) (get qp k))]
    (when raw
      (let [parts (if (string? raw) (str/split raw #",") [raw])
            uuids (keep #(some-> % str str/trim not-empty try-parse-uuid) parts)]
        (when (seq uuids)
          (if (= 1 (count uuids)) (first uuids) (vec uuids)))))))

(defn- parse-month-param
  [qp k]
  (let [raw (some-> (get qp (name k) (get qp k)) str str/trim)]
    (when (and (seq raw) (re-matches month-pattern raw))
      raw)))

(defn- parse-opts
  [qp]
  (let [from (parse-instant-param (get qp "from" (get qp :from)))
        to (parse-instant-param (get qp "to" (get qp :to)))
        tenant-id (parse-uuid-csv qp :tenant_id)
        supplier-id (parse-uuid-csv qp :supplier_id)
        payer-id (parse-uuid-csv qp :payer_id)
        category-id (parse-uuid-csv qp :category_id)
        subcategory-id (parse-uuid-csv qp :subcategory_id)
        expense-category-id (parse-uuid-csv qp :expense_category_id)
        manufacturer-id (parse-uuid-csv qp :manufacturer_id)
        month (parse-month-param qp :month)
        currency-raw (some-> (get qp "currency" (get qp :currency)) str str/trim str/upper-case)
        currency (when (seq currency-raw) currency-raw)]
    (cond-> {}
      from (assoc :from from)
      to (assoc :to to)
      tenant-id (assoc :tenant-id tenant-id)
      supplier-id (assoc :supplier-id supplier-id)
      payer-id (assoc :payer-id payer-id)
      category-id (assoc :category-id category-id)
      subcategory-id (assoc :subcategory-id subcategory-id)
      expense-category-id (assoc :expense-category-id expense-category-id)
      manufacturer-id (assoc :manufacturer-id manufacturer-id)
      month (assoc :month month)
      currency (assoc :currency currency))))

;; ---------------------------------------------------------------------------
;; Handlers — admin context (no user-id scoping)
;; ---------------------------------------------------------------------------

(defn top-suppliers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            limit (or (parse-int-param qp :limit 20 1 100) 20)]
        (utils/success-response
          {:data (to-app (report-suppliers/top db nil (assoc opts :limit limit)))})))
    "Failed to fetch top suppliers"))

(defn supplier-deep-dive-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            alias-limit (or (parse-int-param qp :alias_limit 10 1 100) 10)
            supplier-id (parse-uuid-csv qp :supplier_id)]
        (if-not supplier-id
          (utils/error-response "supplier_id is required" 400)
          (utils/success-response
            {:data (to-app (report-suppliers/deep-dive db nil (assoc opts :alias-limit alias-limit)))}))))
    "Failed to fetch supplier deep-dive"))

(defn supplier-stores-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            limit (or (parse-int-param qp :limit 20 1 100) 20)
            supplier-id (parse-uuid-csv qp :supplier_id)]
        (if-not supplier-id
          (utils/error-response "supplier_id is required" 400)
          (utils/success-response
            {:data (to-app (report-suppliers/stores db nil (assoc opts :limit limit)))}))))
    "Failed to fetch supplier stores"))

(defn supplier-monthly-trends-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            limit (or (parse-int-param qp :limit 10 1 50) 10)]
        (utils/success-response
          {:data (to-app (report-suppliers/monthly-trends db nil (assoc opts :limit limit)))})))
    "Failed to fetch supplier monthly trends"))

(defn top-items-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            limit (or (parse-int-param qp :limit 20 1 100) 20)]
        (utils/success-response
          {:data (to-app (report-items/top-spending db nil (assoc opts :limit limit)))})))
    "Failed to fetch top items"))

(defn top-item-breakdown-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            alias-id (or (some-> (get-in request [:path-params :alias-id]) try-parse-uuid)
                       (some-> (get-in request [:parameters :path :alias-id]) try-parse-uuid))
            limit (or (parse-int-param qp :limit 50 1 200) 50)]
        (if-not alias-id
          (utils/error-response "Invalid alias id" 400)
          (utils/success-response
            {:data (to-app (report-items/top-breakdown db nil alias-id (assoc opts :limit limit)))}))))
    "Failed to fetch top item breakdown"))

(defn monthly-comparison-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)
            month-a (parse-month-param qp :month_a)
            month-b (parse-month-param qp :month_b)]
        (cond
          (nil? month-a) (utils/error-response "month_a is required (YYYY-MM)" 400)
          (nil? month-b) (utils/error-response "month_b is required (YYYY-MM)" 400)
          :else
          (utils/success-response
            {:data (to-app (report-time/monthly-comparison
                             db nil (assoc opts :month-a month-a :month-b month-b)))}))))
    "Failed to fetch monthly comparison"))

(defn category-allocation-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)]
        (utils/success-response
          {:data (to-app (report-categories/allocation db nil opts))})))
    "Failed to fetch category allocation"))

(defn filter-options-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts (parse-opts qp)]
        (utils/success-response
          {:data (to-app (report-filters/options db nil opts))})))
    "Failed to fetch filter options"))

;; ---------------------------------------------------------------------------
;; Route tree — mounted under /admin/api/expenses/reports/analytics
;; ---------------------------------------------------------------------------

(defn routes [db]
  ["/analytics"
   ["/top-suppliers" {:get (top-suppliers-handler db)}]
   ["/supplier-deep-dive" {:get (supplier-deep-dive-handler db)}]
   ["/supplier-stores" {:get (supplier-stores-handler db)}]
   ["/supplier-monthly-trends" {:get (supplier-monthly-trends-handler db)}]
   ["/top-items" {:get (top-items-handler db)}]
   ["/top-items/:alias-id/breakdown" {:get (top-item-breakdown-handler db)}]
   ["/monthly-comparison" {:get (monthly-comparison-handler db)}]
   ["/category-allocation" {:get (category-allocation-handler db)}]
   ["/filter-options" {:get (filter-options-handler db)}]])
