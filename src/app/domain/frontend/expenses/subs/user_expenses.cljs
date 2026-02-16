(ns app.domain.frontend.expenses.subs.user-expenses
  "Subscriptions for user-facing expense dashboard data."
  (:require
    [re-frame.core :as rf]))

;; Summary
(rf/reg-sub
  :user-expenses/summary
  (fn [db _]
    (get-in db [:user-expenses :summary :data])))

(rf/reg-sub
  :user-expenses/summary-loading?
  (fn [db _]
    (get-in db [:user-expenses :summary :loading?])))

(rf/reg-sub
  :user-expenses/summary-error
  (fn [db _]
    (get-in db [:user-expenses :summary :error])))

;; Recent expenses list
(rf/reg-sub
  :user-expenses/recent
  (fn [db _]
    (get-in db [:user-expenses :recent :items])))

(rf/reg-sub
  :user-expenses/recent-loading?
  (fn [db _]
    (get-in db [:user-expenses :recent :loading?])))

(rf/reg-sub
  :user-expenses/recent-error
  (fn [db _]
    (get-in db [:user-expenses :recent :error])))

(rf/reg-sub
  :user-expenses/recent-limit
  (fn [db _]
    (get-in db [:user-expenses :recent :limit])))

(rf/reg-sub
  :user-expenses/recent-offset
  (fn [db _]
    (get-in db [:user-expenses :recent :offset])))

(rf/reg-sub
  :user-expenses/recent-page
  (fn [db _]
    (or (get-in db [:user-expenses :recent :page]) 1)))

(rf/reg-sub
  :user-expenses/recent-total
  (fn [db _]
    (get-in db [:user-expenses :recent :total])))

(rf/reg-sub
  :user-expenses/recent-total-pages
  (fn [db _]
    (let [limit (get-in db [:user-expenses :recent :limit])
          limit* (if (and (number? limit)
                       (not (js/isNaN limit))
                       (pos? limit))
                   limit
                   25)
          total (get-in db [:user-expenses :recent :total])
          total* (when (and (number? total)
                         (not (js/isNaN total))
                         (>= total 0))
                   total)
          page (get-in db [:user-expenses :recent :page])
          page* (if (and (number? page)
                      (not (js/isNaN page))
                      (pos? page))
                  (int page)
                  1)
          items (get-in db [:user-expenses :recent :items])
          from-total (when (some? total*)
                       (max 1 (int (js/Math.ceil (/ total* limit*)))))
          fallback (if (seq items) page* 1)]
      (apply max (remove nil? [1 page* from-total fallback])))))

;; Aggregations
(rf/reg-sub
  :user-expenses/by-month
  (fn [db _]
    (get-in db [:user-expenses :by-month :data])))

(rf/reg-sub
  :user-expenses/by-month-loading?
  (fn [db _]
    (get-in db [:user-expenses :by-month :loading?])))

(rf/reg-sub
  :user-expenses/by-supplier
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :data])))

(rf/reg-sub
  :user-expenses/by-supplier-loading?
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :loading?])))

;; Current expense detail
(rf/reg-sub
  :user-expenses/current-expense
  (fn [db _]
    (get-in db [:user-expenses :current-expense :data])))

(rf/reg-sub
  :user-expenses/current-expense-loading?
  (fn [db _]
    (get-in db [:user-expenses :current-expense :loading?])))

(rf/reg-sub
  :user-expenses/current-expense-error
  (fn [db _]
    (get-in db [:user-expenses :current-expense :error])))

;; Form state
(rf/reg-sub
  :user-expenses/form-loading?
  (fn [db _]
    (get-in db [:user-expenses :form :loading?])))

(rf/reg-sub
  :user-expenses/form-error
  (fn [db _]
    (get-in db [:user-expenses :form :error])))

;; Suppliers and payers for forms
(rf/reg-sub
  :user-expenses/suppliers
  (fn [db _]
    (get-in db [:user-expenses :suppliers :items])))

(rf/reg-sub
  :user-expenses/categories
  (fn [db _]
    (get-in db [:user-expenses :categories :items])))

(rf/reg-sub
  :user-expenses/subcategories
  (fn [db _]
    (get-in db [:user-expenses :subcategories :items])))

(rf/reg-sub
  :user-expenses/expense-categories
  (fn [db _]
    (get-in db [:user-expenses :expense-categories :items])))

(rf/reg-sub
  :user-expenses/manufacturers
  (fn [db _]
    (get-in db [:user-expenses :manufacturers :items])))

;; Supplier detail (used by the user suppliers modal)
(rf/reg-sub
  :user-expenses/supplier-detail
  (fn [db [_ supplier-id]]
    (get-in db [:user-expenses :suppliers :detail :by-id (some-> supplier-id str)])))

(rf/reg-sub
  :user-expenses/supplier-detail-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :suppliers :detail :loading?]))))

(rf/reg-sub
  :user-expenses/supplier-detail-error
  (fn [db _]
    (get-in db [:user-expenses :suppliers :detail :error])))

(rf/reg-sub
  :user-expenses/supplier-detail-expenses
  (fn [db _]
    (get-in db [:user-expenses :suppliers :detail :expenses])))

(rf/reg-sub
  :user-expenses/supplier-detail-expenses-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :suppliers :detail :expenses-loading?]))))

(rf/reg-sub
  :user-expenses/supplier-detail-expenses-error
  (fn [db _]
    (get-in db [:user-expenses :suppliers :detail :expenses-error])))

(rf/reg-sub
  :user-expenses/supplier-detail-article-aliases
  (fn [db _]
    (get-in db [:user-expenses :suppliers :detail :aliases])))

(rf/reg-sub
  :user-expenses/supplier-detail-article-aliases-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :suppliers :detail :aliases-loading?]))))

(rf/reg-sub
  :user-expenses/supplier-detail-article-aliases-error
  (fn [db _]
    (get-in db [:user-expenses :suppliers :detail :aliases-error])))

(rf/reg-sub
  :user-expenses/payers
  (fn [db _]
    (get-in db [:user-expenses :payers :items])))

(rf/reg-sub
  :user-expenses/payers-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :payers :loading?]))))

(rf/reg-sub
  :user-expenses/payer-types
  (fn [db _]
    (get-in db [:user-expenses :payer-types :items])))

(rf/reg-sub
  :user-expenses/payer-types-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :payer-types :loading?]))))

;; Upload state
(rf/reg-sub
  :user-expenses/upload-loading?
  (fn [db _]
    (get-in db [:user-expenses :upload :loading?])))

(rf/reg-sub
  :user-expenses/upload-error
  (fn [db _]
    (get-in db [:user-expenses :upload :error])))

(rf/reg-sub
  :user-expenses/upload-notice
  (fn [db _]
    (get-in db [:user-expenses :upload :notice])))

(rf/reg-sub
  :user-expenses/upload-batch
  (fn [db _]
    (get-in db [:user-expenses :upload :batch])))

(rf/reg-sub
  :user-expenses/upload-payer-id
  (fn [db _]
    (get-in db [:user-expenses :upload :payer-id])))

(rf/reg-sub
  :user-expenses/recent-receipts
  (fn [db _]
    (get-in db [:user-expenses :receipts :items])))

;; Receipts inbox
(rf/reg-sub
  :user-expenses/receipts
  (fn [db _]
    (get-in db [:user-expenses :receipts :items])))

(rf/reg-sub
  :user-expenses/receipts-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :loading?])))

(rf/reg-sub
  :user-expenses/receipts-error
  (fn [db _]
    (get-in db [:user-expenses :receipts :error])))

(rf/reg-sub
  :user-expenses/receipt
  (fn [db [_ receipt-id]]
    (get-in db [:user-expenses :receipts :by-id receipt-id])))

(rf/reg-sub
  :user-expenses/receipt-detail-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :detail-loading?])))

(rf/reg-sub
  :user-expenses/receipt-action-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :action-loading?])))

(rf/reg-sub
  :user-expenses/receipt-detail-modal-open?
  (fn [db _]
    (true? (get-in db [:user-expenses :receipts :detail-modal :open?]))))

(rf/reg-sub
  :user-expenses/receipt-detail-modal-id
  (fn [db _]
    (get-in db [:user-expenses :receipts :detail-modal :entity-id])))

;; Settings
(rf/reg-sub
  :user-expenses/settings
  (fn [db _]
    (get-in db [:user-expenses :settings :data])))

(rf/reg-sub
  :user-expenses/settings-loading?
  (fn [db _]
    (get-in db [:user-expenses :settings :loading?])))

(rf/reg-sub
  :user-expenses/settings-saving?
  (fn [db _]
    (get-in db [:user-expenses :settings :saving?])))

(defn- reports-data
  [db report-key field]
  (get-in db [:user-expenses :reports report-key field]))

;; Reports filters
(rf/reg-sub
  :user-expenses/reports-filters
  (fn [db _]
    (get-in db [:user-expenses :reports :filters])))

(rf/reg-sub
  :user-expenses/reports-filter-months-back
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :months-back])))

(rf/reg-sub
  :user-expenses/reports-filter-supplier-id
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :supplier-id])))

(rf/reg-sub
  :user-expenses/reports-filter-day-of-week
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :day-of-week])))

(rf/reg-sub
  :user-expenses/reports-filter-category-key
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :category-key])))

(rf/reg-sub
  :user-expenses/reports-filter-amount-bucket
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :amount-bucket])))

(rf/reg-sub
  :user-expenses/reports-filter-selected-day
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :selected-day])))

(rf/reg-sub
  :user-expenses/reports-filter-month-a
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :month-a])))

(rf/reg-sub
  :user-expenses/reports-filter-month-b
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :month-b])))

(rf/reg-sub
  :user-expenses/reports-filter-show-uncategorized?
  (fn [db _]
    (get-in db [:user-expenses :reports :filters :show-uncategorized?])))

;; Supplier deep-dive report
(rf/reg-sub
  :user-expenses/report-supplier-deep-dive
  (fn [db _]
    (reports-data db :supplier-deep-dive :data)))

(rf/reg-sub
  :user-expenses/report-supplier-deep-dive-loading?
  (fn [db _]
    (boolean (reports-data db :supplier-deep-dive :loading?))))

(rf/reg-sub
  :user-expenses/report-supplier-deep-dive-error
  (fn [db _]
    (reports-data db :supplier-deep-dive :error)))

;; Day-of-week report
(rf/reg-sub
  :user-expenses/report-day-of-week
  (fn [db _]
    (or (reports-data db :day-of-week :data) [])))

(rf/reg-sub
  :user-expenses/report-day-of-week-loading?
  (fn [db _]
    (boolean (reports-data db :day-of-week :loading?))))

(rf/reg-sub
  :user-expenses/report-day-of-week-error
  (fn [db _]
    (reports-data db :day-of-week :error)))

;; Top-items report
(rf/reg-sub
  :user-expenses/report-top-items
  (fn [db _]
    (or (reports-data db :top-items :data) [])))

(rf/reg-sub
  :user-expenses/report-top-items-loading?
  (fn [db _]
    (boolean (reports-data db :top-items :loading?))))

(rf/reg-sub
  :user-expenses/report-top-items-error
  (fn [db _]
    (reports-data db :top-items :error)))

;; Monthly comparison report
(rf/reg-sub
  :user-expenses/report-monthly-comparison
  (fn [db _]
    (reports-data db :monthly-comparison :data)))

(rf/reg-sub
  :user-expenses/report-monthly-comparison-loading?
  (fn [db _]
    (boolean (reports-data db :monthly-comparison :loading?))))

(rf/reg-sub
  :user-expenses/report-monthly-comparison-error
  (fn [db _]
    (reports-data db :monthly-comparison :error)))

;; Size distribution report
(rf/reg-sub
  :user-expenses/report-size-distribution
  (fn [db _]
    (or (reports-data db :size-distribution :data) [])))

(rf/reg-sub
  :user-expenses/report-size-distribution-loading?
  (fn [db _]
    (boolean (reports-data db :size-distribution :loading?))))

(rf/reg-sub
  :user-expenses/report-size-distribution-error
  (fn [db _]
    (reports-data db :size-distribution :error)))

;; Daily heatmap report
(rf/reg-sub
  :user-expenses/report-daily-heatmap
  (fn [db _]
    (or (reports-data db :daily-heatmap :data) [])))

(rf/reg-sub
  :user-expenses/report-daily-heatmap-loading?
  (fn [db _]
    (boolean (reports-data db :daily-heatmap :loading?))))

(rf/reg-sub
  :user-expenses/report-daily-heatmap-error
  (fn [db _]
    (reports-data db :daily-heatmap :error)))

;; Category allocation report
(rf/reg-sub
  :user-expenses/report-category-allocation
  (fn [db _]
    (or (reports-data db :category-allocation :data) [])))

(rf/reg-sub
  :user-expenses/report-category-allocation-loading?
  (fn [db _]
    (boolean (reports-data db :category-allocation :loading?))))

(rf/reg-sub
  :user-expenses/report-category-allocation-error
  (fn [db _]
    (reports-data db :category-allocation :error)))
