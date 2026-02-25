(ns app.domain.backend.expenses.services.user-expense-reports
  "Compatibility facade for user expense reports.

  New code should depend on focused namespaces under
  `app.domain.backend.expenses.services.user-expense-reports.*`."
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.categories :as categories]
    [app.domain.backend.expenses.services.user-expense-reports.filters :as filters]
    [app.domain.backend.expenses.services.user-expense-reports.items :as items]
    [app.domain.backend.expenses.services.user-expense-reports.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expense-reports.time :as time]))

(defn get-user-top-suppliers
  [db user-id opts]
  (suppliers/top db user-id opts))

(defn get-user-supplier-stores
  [db user-id opts]
  (suppliers/stores db user-id opts))

(defn get-user-supplier-monthly-trends
  [db user-id opts]
  (suppliers/monthly-trends db user-id opts))

(defn get-user-supplier-deep-dive
  [db user-id opts]
  (suppliers/deep-dive db user-id opts))

(defn get-user-day-of-week-spending-pattern
  [db user-id opts]
  (time/day-of-week db user-id opts))

(defn get-user-top-item-spending
  [db user-id opts]
  (items/top-spending db user-id opts))

(defn get-user-top-item-breakdown
  [db user-id alias-id opts]
  (items/top-breakdown db user-id alias-id opts))

(defn get-user-monthly-comparison
  [db user-id opts]
  (time/monthly-comparison db user-id opts))

(defn get-user-expense-size-distribution
  [db user-id opts]
  (time/size-distribution db user-id opts))

(defn get-user-daily-heatmap
  [db user-id opts]
  (time/daily-heatmap db user-id opts))

(defn get-user-report-filter-options
  [db user-id opts]
  (filters/options db user-id opts))

(defn get-user-category-allocation
  [db user-id opts]
  (categories/allocation db user-id opts))
