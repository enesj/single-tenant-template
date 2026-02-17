(ns app.domain.frontend.expenses.events.user-expenses.reports
  "User expense reports events for `/expenses/reports` widgets."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private reports-path [:user-expenses :reports])

(def ^:private refresh-filter-keys
  #{:months-back
    :supplier-id
    :category-id
    :subcategory-id
    :expense-category-id
    :manufacturer-id
    :month-a
    :month-b})

(defn- ->positive-int
  [value fallback]
  (let [parsed (cond
                 (number? value) value
                 (string? value) (js/parseInt value 10)
                 :else fallback)]
    (if (and (number? parsed)
          (not (js/isNaN parsed))
          (pos? parsed))
      (int parsed)
      fallback)))

(defn- normalize-id
  [value]
  (let [v (some-> value str str/trim)]
    (when (seq v) v)))

(defn- normalize-id-values
  [value]
  (let [values (cond
                 (nil? value) []
                 (sequential? value) value
                 :else [value])]
    (->> values
      (keep normalize-id)
      distinct
      vec)))

(defn- normalize-id-filter
  [value]
  (let [values (normalize-id-values value)]
    (cond
      (empty? values) nil
      (= 1 (count values)) (first values)
      :else values)))

(defn- normalize-month
  [value]
  (let [v (some-> value str str/trim)]
    (when (seq v) v)))

(defn- pad2
  [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defn- month-key-from-date
  [d]
  (str (.getUTCFullYear d) "-" (pad2 (inc (.getUTCMonth d)))))

(defn- shift-months
  [d months]
  (let [copy (js/Date. (.getTime d))]
    (.setUTCMonth copy (+ (.getUTCMonth copy) months))
    copy))

(defn- default-month-range
  []
  (let [now (js/Date.)
        month-b (month-key-from-date now)
        month-a (month-key-from-date (shift-months now -1))]
    {:month-a month-a
     :month-b month-b}))

(defn- default-report-filters
  []
  (merge
    {:months-back 6
     :supplier-id nil
     :category-id nil
     :subcategory-id nil
     :expense-category-id nil
     :manufacturer-id nil
     :day-of-week nil
     :category-key nil
     :amount-bucket nil
     :selected-day nil
     :top-items-limit 20
     :show-uncategorized? true}
    (default-month-range)))

(defn- report-range-params
  [months-back]
  (let [months* (->positive-int months-back 6)
        now (js/Date.)
        from (js/Date. (.getTime now))]
    (.setUTCDate from 1)
    (.setUTCHours from 0 0 0 0)
    (.setUTCMonth from (- (.getUTCMonth from) (max 1 months*)))
    {:from (.toISOString from)
     :to (.toISOString now)}))

(defn- common-report-params
  [db]
  (let [{:keys [months-back
                supplier-id
                category-id
                subcategory-id
                expense-category-id
                manufacturer-id]} (get-in db (conj reports-path :filters))
        range-params (report-range-params months-back)
        supplier-id* (normalize-id-filter supplier-id)
        category-id* (normalize-id-filter category-id)
        subcategory-id* (normalize-id-filter subcategory-id)
        expense-category-id* (normalize-id-filter expense-category-id)
        manufacturer-id* (normalize-id-filter manufacturer-id)]
    (cond-> range-params
      supplier-id* (assoc :supplier_id supplier-id*)
      category-id* (assoc :category_id category-id*)
      subcategory-id* (assoc :subcategory_id subcategory-id*)
      expense-category-id* (assoc :expense_category_id expense-category-id*)
      manufacturer-id* (assoc :manufacturer_id manufacturer-id*))))

(defn- start-load
  [db report-key]
  (-> db
    (assoc-in (conj reports-path report-key :loading?) true)
    (assoc-in (conj reports-path report-key :error) nil)))

(defn- finish-success
  [db report-key value]
  (-> db
    (assoc-in (conj reports-path report-key :loading?) false)
    (assoc-in (conj reports-path report-key :error) nil)
    (assoc-in (conj reports-path report-key :data) value)))

(defn- finish-failure
  [db report-key error]
  (-> db
    (assoc-in (conj reports-path report-key :loading?) false)
    (assoc-in (conj reports-path report-key :error) (http/extract-error-message error))))

(defn- fetch-fx
  [db report-key uri params on-success on-failure]
  {:db (start-load db report-key)
   :http-xhrio (x/xhrio db
                 {:method :get
                  :uri uri
                  :params params
                  :on-success on-success
                  :on-failure on-failure})})

(defn- normalize-filter-value
  [k value]
  (case k
    :months-back (->positive-int value 6)
    :supplier-id (normalize-id-filter value)
    :category-id (normalize-id-filter value)
    :subcategory-id (normalize-id-filter value)
    :expense-category-id (normalize-id-filter value)
    :manufacturer-id (normalize-id-filter value)
    :month-a (normalize-month value)
    :month-b (normalize-month value)
    :day-of-week (some-> (->positive-int value nil) int)
    :category-key (normalize-id value)
    :amount-bucket (normalize-id value)
    :selected-day (normalize-month value)
    :show-uncategorized? (boolean value)
    value))

(rf/reg-event-fx
  :user-expenses/init-reports
  common-interceptors
  (fn [{:keys [db]} _]
    (let [defaults (default-report-filters)
          existing (or (get-in db (conj reports-path :filters)) {})
          filters (merge defaults (into {} (remove (comp nil? val) existing)))]
      {:db (assoc-in db (conj reports-path :filters) filters)
       :dispatch-n [[:user-expenses/fetch-categories]
                    [:user-expenses/fetch-subcategories]
                    [:user-expenses/fetch-expense-categories]
                    [:user-expenses/fetch-manufacturers]
                    [:user-expenses/reports-refresh]]})))

(rf/reg-event-fx
  :user-expenses/reports-refresh
  common-interceptors
  (fn [{:keys [db]} _]
    (let [months-back (->positive-int (get-in db (conj reports-path :filters :months-back)) 6)]
      {:dispatch-n [[:user-expenses/fetch-summary]
                    [:user-expenses/fetch-by-month {:months-back months-back}]
                    [:user-expenses/fetch-by-supplier {:limit 25}]
                    [:user-expenses/fetch-report-filter-options]
                    [:user-expenses/fetch-report-supplier-deep-dive]
                    [:user-expenses/fetch-report-day-of-week]
                    [:user-expenses/fetch-report-top-items]
                    [:user-expenses/fetch-report-monthly-comparison]
                    [:user-expenses/fetch-report-size-distribution]
                    [:user-expenses/fetch-report-daily-heatmap]
                    [:user-expenses/fetch-report-category-allocation]]})))

(rf/reg-event-fx
  :user-expenses/reports-set-filter
  common-interceptors
  (fn [{:keys [db]} [k value]]
    (let [value* (normalize-filter-value k value)
          db* (assoc-in db (conj reports-path :filters k) value*)]
      (cond-> {:db db*}
        (contains? refresh-filter-keys k) (assoc :dispatch [:user-expenses/reports-refresh])))))

(rf/reg-event-db
  :user-expenses/reports-toggle-day-of-week
  common-interceptors
  (fn [db [iso-day]]
    (let [day* (some-> (->positive-int iso-day nil) int)
          current (get-in db (conj reports-path :filters :day-of-week))
          next-value (when-not (= current day*) day*)]
      (assoc-in db (conj reports-path :filters :day-of-week) next-value))))

(rf/reg-event-db
  :user-expenses/reports-toggle-amount-bucket
  common-interceptors
  (fn [db [bucket-key]]
    (let [bucket* (normalize-id bucket-key)
          current (get-in db (conj reports-path :filters :amount-bucket))
          next-value (when-not (= current bucket*) bucket*)]
      (assoc-in db (conj reports-path :filters :amount-bucket) next-value))))

(rf/reg-event-db
  :user-expenses/reports-toggle-category
  common-interceptors
  (fn [db [category-key]]
    (let [category* (normalize-id category-key)
          current (get-in db (conj reports-path :filters :category-key))
          next-value (when-not (= current category*) category*)]
      (assoc-in db (conj reports-path :filters :category-key) next-value))))

(rf/reg-event-db
  :user-expenses/reports-toggle-selected-day
  common-interceptors
  (fn [db [day]]
    (let [day* (normalize-month day)
          current (get-in db (conj reports-path :filters :selected-day))
          next-value (when-not (= current day*) day*)]
      (assoc-in db (conj reports-path :filters :selected-day) next-value))))

(rf/reg-event-fx
  :user-expenses/reports-clear-local-filters
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (conj reports-path :filters :supplier-id) nil)
           (assoc-in (conj reports-path :filters :category-id) nil)
           (assoc-in (conj reports-path :filters :subcategory-id) nil)
           (assoc-in (conj reports-path :filters :expense-category-id) nil)
           (assoc-in (conj reports-path :filters :manufacturer-id) nil)
           (assoc-in (conj reports-path :filters :day-of-week) nil)
           (assoc-in (conj reports-path :filters :category-key) nil)
           (assoc-in (conj reports-path :filters :amount-bucket) nil)
           (assoc-in (conj reports-path :filters :selected-day) nil))
     :dispatch [:user-expenses/reports-refresh]}))

(rf/reg-event-fx
  :user-expenses/fetch-report-filter-options
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :filter-options
      endpoints/reports-filter-options-endpoint
      (common-report-params db)
      [:user-expenses/fetch-report-filter-options-success]
      [:user-expenses/fetch-report-filter-options-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-filter-options-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :filter-options (or (:data response) {}))))

(rf/reg-event-db
  :user-expenses/fetch-report-filter-options-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch report filter options" {:error error})
    (finish-failure db :filter-options error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-supplier-deep-dive
  common-interceptors
  (fn [{:keys [db]} _]
    (let [supplier-id (-> (get-in db (conj reports-path :filters :supplier-id))
                        normalize-id-values
                        first)]
      (if-not supplier-id
        {:db (finish-success db :supplier-deep-dive nil)}
        (fetch-fx db
          :supplier-deep-dive
          endpoints/reports-supplier-deep-dive-endpoint
          (assoc (common-report-params db) :supplier_id supplier-id)
          [:user-expenses/fetch-report-supplier-deep-dive-success]
          [:user-expenses/fetch-report-supplier-deep-dive-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-deep-dive-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :supplier-deep-dive (:data response))))

(rf/reg-event-db
  :user-expenses/fetch-report-supplier-deep-dive-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch supplier deep-dive report" {:error error})
    (finish-failure db :supplier-deep-dive error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-day-of-week
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :day-of-week
      endpoints/reports-day-of-week-endpoint
      (common-report-params db)
      [:user-expenses/fetch-report-day-of-week-success]
      [:user-expenses/fetch-report-day-of-week-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-day-of-week-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :day-of-week (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-day-of-week-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch day-of-week report" {:error error})
    (finish-failure db :day-of-week error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-top-items
  common-interceptors
  (fn [{:keys [db]} _]
    (let [limit* (->positive-int (get-in db (conj reports-path :filters :top-items-limit)) 20)]
      (fetch-fx db
        :top-items
        endpoints/reports-top-items-endpoint
        (assoc (common-report-params db) :limit limit*)
        [:user-expenses/fetch-report-top-items-success]
        [:user-expenses/fetch-report-top-items-failure]))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-items-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :top-items (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-top-items-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch top-items report" {:error error})
    (finish-failure db :top-items error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-monthly-comparison
  common-interceptors
  (fn [{:keys [db]} _]
    (let [{:keys [month-a month-b]} (get-in db (conj reports-path :filters))
          month-a* (normalize-month month-a)
          month-b* (normalize-month month-b)]
      (if (or (not month-a*) (not month-b*))
        {:db (-> db
               (assoc-in (conj reports-path :monthly-comparison :loading?) false)
               (assoc-in (conj reports-path :monthly-comparison :error) "Select both months to compare.")
               (assoc-in (conj reports-path :monthly-comparison :data) nil))}
        (fetch-fx db
          :monthly-comparison
          endpoints/reports-monthly-comparison-endpoint
          (assoc (common-report-params db)
            :month_a month-a*
            :month_b month-b*)
          [:user-expenses/fetch-report-monthly-comparison-success]
          [:user-expenses/fetch-report-monthly-comparison-failure])))))

(rf/reg-event-db
  :user-expenses/fetch-report-monthly-comparison-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :monthly-comparison (:data response))))

(rf/reg-event-db
  :user-expenses/fetch-report-monthly-comparison-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch monthly-comparison report" {:error error})
    (finish-failure db :monthly-comparison error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-size-distribution
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :size-distribution
      endpoints/reports-size-distribution-endpoint
      (common-report-params db)
      [:user-expenses/fetch-report-size-distribution-success]
      [:user-expenses/fetch-report-size-distribution-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-size-distribution-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :size-distribution (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-size-distribution-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch size-distribution report" {:error error})
    (finish-failure db :size-distribution error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-daily-heatmap
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :daily-heatmap
      endpoints/reports-daily-heatmap-endpoint
      (common-report-params db)
      [:user-expenses/fetch-report-daily-heatmap-success]
      [:user-expenses/fetch-report-daily-heatmap-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-daily-heatmap-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :daily-heatmap (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-daily-heatmap-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch daily-heatmap report" {:error error})
    (finish-failure db :daily-heatmap error)))

(rf/reg-event-fx
  :user-expenses/fetch-report-category-allocation
  common-interceptors
  (fn [{:keys [db]} _]
    (fetch-fx db
      :category-allocation
      endpoints/reports-category-allocation-endpoint
      (common-report-params db)
      [:user-expenses/fetch-report-category-allocation-success]
      [:user-expenses/fetch-report-category-allocation-failure])))

(rf/reg-event-db
  :user-expenses/fetch-report-category-allocation-success
  common-interceptors
  (fn [db [response]]
    (finish-success db :category-allocation (vec (or (:data response) [])))))

(rf/reg-event-db
  :user-expenses/fetch-report-category-allocation-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch category-allocation report" {:error error})
    (finish-failure db :category-allocation error)))
