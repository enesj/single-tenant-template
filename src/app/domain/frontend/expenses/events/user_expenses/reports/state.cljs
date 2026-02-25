(ns app.domain.frontend.expenses.events.user-expenses.reports.state
  "Report filter state management events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.reports.helpers :as h]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

(def ^:private refresh-filter-keys
  #{:months-back
    :supplier-id
    :category-id
    :subcategory-id
    :expense-category-id
    :manufacturer-id
    :month-a
    :month-b})

(defn- normalize-filter-value
  [k value]
  (case k
    :months-back (h/->positive-int value 6)
    :supplier-id (h/normalize-id-filter value)
    :category-id (h/normalize-id-filter value)
    :subcategory-id (h/normalize-id-filter value)
    :expense-category-id (h/normalize-id-filter value)
    :manufacturer-id (h/normalize-id-filter value)
    :month-a (h/normalize-month value)
    :month-b (h/normalize-month value)
    :day-of-week (some-> (h/->positive-int value nil) int)
    :category-key (h/normalize-id value)
    :amount-bucket (h/normalize-id value)
    :selected-day (h/normalize-month value)
    :show-uncategorized? (boolean value)
    value))

(rf/reg-event-fx
  :user-expenses/init-reports
  common-interceptors
  (fn [{:keys [db]} _]
    (let [defaults (h/default-report-filters)
          existing (or (get-in db (conj h/reports-path :filters)) {})
          filters (merge defaults (into {} (remove (comp nil? val) existing)))]
      {:db (assoc-in db (conj h/reports-path :filters) filters)
       :dispatch-n [[:user-expenses/fetch-categories]
                    [:user-expenses/fetch-subcategories]
                    [:user-expenses/fetch-expense-categories]
                    [:user-expenses/fetch-manufacturers]
                    [:user-expenses/reports-refresh]]})))

(rf/reg-event-fx
  :user-expenses/reports-refresh
  common-interceptors
  (fn [{:keys [db]} _]
    (let [months-back (h/->positive-int (get-in db (conj h/reports-path :filters :months-back)) 6)
          expanded-supplier-id (-> (get-in db (conj h/reports-path :filters :expanded-supplier-id))
                                 h/normalize-id)
          expanded-top-item-alias-id (-> (get-in db (conj h/reports-path :filters :expanded-top-item-alias-id))
                                       h/normalize-id)]
      {:dispatch-n (cond-> [[:user-expenses/fetch-summary]
                            [:user-expenses/fetch-by-month {:months-back months-back}]
                            [:user-expenses/fetch-by-supplier {:limit 25}]
                            [:user-expenses/fetch-report-filter-options]
                            [:user-expenses/fetch-report-supplier-deep-dive]
                            [:user-expenses/fetch-report-day-of-week]
                            [:user-expenses/fetch-report-top-items]
                            [:user-expenses/fetch-report-monthly-comparison]
                            [:user-expenses/fetch-report-size-distribution]
                            [:user-expenses/fetch-report-daily-heatmap]
                            [:user-expenses/fetch-report-top-suppliers]
                            [:user-expenses/fetch-report-supplier-monthly-trends]
                            [:user-expenses/fetch-report-category-allocation]]
                     expanded-supplier-id (conj [:user-expenses/fetch-report-supplier-stores])
                     expanded-top-item-alias-id (conj [:user-expenses/fetch-report-top-item-breakdown]))})))

(rf/reg-event-fx
  :user-expenses/reports-set-filter
  common-interceptors
  (fn [{:keys [db]} [k value]]
    (let [value* (normalize-filter-value k value)
          db* (assoc-in db (conj h/reports-path :filters k) value*)]
      (cond-> {:db db*}
        (contains? refresh-filter-keys k) (assoc :dispatch [:user-expenses/reports-refresh])))))

(rf/reg-event-db
  :user-expenses/reports-toggle-day-of-week
  common-interceptors
  (fn [db [iso-day]]
    (let [day* (some-> (h/->positive-int iso-day nil) int)
          current (get-in db (conj h/reports-path :filters :day-of-week))
          next-value (when-not (= current day*) day*)]
      (assoc-in db (conj h/reports-path :filters :day-of-week) next-value))))

(rf/reg-event-db
  :user-expenses/reports-toggle-amount-bucket
  common-interceptors
  (fn [db [bucket-key]]
    (let [bucket* (h/normalize-id bucket-key)
          current (get-in db (conj h/reports-path :filters :amount-bucket))
          next-value (when-not (= current bucket*) bucket*)]
      (assoc-in db (conj h/reports-path :filters :amount-bucket) next-value))))

(rf/reg-event-db
  :user-expenses/reports-toggle-category
  common-interceptors
  (fn [db [category-key]]
    (let [category* (h/normalize-id category-key)
          current (get-in db (conj h/reports-path :filters :category-key))
          next-value (when-not (= current category*) category*)]
      (assoc-in db (conj h/reports-path :filters :category-key) next-value))))

(rf/reg-event-fx
  :user-expenses/reports-toggle-expanded-supplier
  common-interceptors
  (fn [{:keys [db]} [supplier-id]]
    (let [supplier-id* (h/normalize-id supplier-id)
          current (-> (get-in db (conj h/reports-path :filters :expanded-supplier-id))
                    h/normalize-id)
          next-value (when-not (= current supplier-id*) supplier-id*)
          db* (assoc-in db (conj h/reports-path :filters :expanded-supplier-id) next-value)]
      (if next-value
        {:db db*
         :dispatch [:user-expenses/fetch-report-supplier-stores]}
        {:db (-> db*
               (assoc-in (conj h/reports-path :supplier-stores :loading?) false)
               (assoc-in (conj h/reports-path :supplier-stores :error) nil)
               (assoc-in (conj h/reports-path :supplier-stores :data) []))}))))

(rf/reg-event-fx
  :user-expenses/reports-toggle-expanded-top-item
  common-interceptors
  (fn [{:keys [db]} [alias-id]]
    (let [alias-id* (h/normalize-id alias-id)
          current (-> (get-in db (conj h/reports-path :filters :expanded-top-item-alias-id))
                    h/normalize-id)
          next-value (when-not (= current alias-id*) alias-id*)
          db* (assoc-in db (conj h/reports-path :filters :expanded-top-item-alias-id) next-value)]
      (if next-value
        {:db (-> db*
               (assoc-in (conj h/reports-path :top-item-breakdown :loading?) true)
               (assoc-in (conj h/reports-path :top-item-breakdown :error) nil)
               (assoc-in (conj h/reports-path :top-item-breakdown :data) nil))
         :dispatch [:user-expenses/fetch-report-top-item-breakdown]}
        {:db (-> db*
               (assoc-in (conj h/reports-path :top-item-breakdown :loading?) false)
               (assoc-in (conj h/reports-path :top-item-breakdown :error) nil)
               (assoc-in (conj h/reports-path :top-item-breakdown :data) nil))}))))

(rf/reg-event-db
  :user-expenses/reports-toggle-selected-day
  common-interceptors
  (fn [db [day]]
    (let [day* (h/normalize-month day)
          current (get-in db (conj h/reports-path :filters :selected-day))
          next-value (when-not (= current day*) day*)]
      (assoc-in db (conj h/reports-path :filters :selected-day) next-value))))

(rf/reg-event-fx
  :user-expenses/reports-clear-local-filters
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in (conj h/reports-path :filters :supplier-id) nil)
           (assoc-in (conj h/reports-path :filters :category-id) nil)
           (assoc-in (conj h/reports-path :filters :subcategory-id) nil)
           (assoc-in (conj h/reports-path :filters :expense-category-id) nil)
           (assoc-in (conj h/reports-path :filters :manufacturer-id) nil)
           (assoc-in (conj h/reports-path :filters :day-of-week) nil)
           (assoc-in (conj h/reports-path :filters :category-key) nil)
           (assoc-in (conj h/reports-path :filters :amount-bucket) nil)
           (assoc-in (conj h/reports-path :filters :selected-day) nil)
           (assoc-in (conj h/reports-path :filters :expanded-supplier-id) nil)
           (assoc-in (conj h/reports-path :filters :expanded-top-item-alias-id) nil)
           (assoc-in (conj h/reports-path :supplier-stores :loading?) false)
           (assoc-in (conj h/reports-path :supplier-stores :error) nil)
           (assoc-in (conj h/reports-path :supplier-stores :data) [])
           (assoc-in (conj h/reports-path :top-item-breakdown :loading?) false)
           (assoc-in (conj h/reports-path :top-item-breakdown :error) nil)
           (assoc-in (conj h/reports-path :top-item-breakdown :data) nil))
     :dispatch [:user-expenses/reports-refresh]}))
