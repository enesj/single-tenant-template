(ns app.template.frontend.pages.expenses-dashboard
  "User-facing expense dashboard page.
   Shows personal expense summary, recent expenses, and quick actions for expense management."
  (:require
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.subs.user-expenses])) ;; side-effect load for subscriptions

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn format-money
  "Format a number with currency; falls back gracefully if currency is missing."
  [amount currency]
  (cond
    (nil? amount) "--"
    :else (try
            (.toLocaleString (js/Number amount) "en-US"
              #js {:style "currency"
                   :currency (or currency "USD")
                   :minimumFractionDigits 2
                   :maximumFractionDigits 2})
            (catch :default _
              (str (or currency "$") " " (.toFixed (js/Number amount) 2))))))

(defn format-date
  "Short human-friendly date for purchased_at timestamps."
  [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US"
      #js {:month "short" :day "numeric"})))

(defn status-label [expense]
  (if (:is_posted expense) "posted" "pending"))

;; ========================================================================
;; Stat Card Component
;; ========================================================================

(defui stat-card [{:keys [title value subtitle icon trend loading? error]}]
  ($ :div {:class "bg-white rounded-xl shadow-sm border border-slate-100 p-5"}
    ($ :div {:class "flex items-start justify-between"}
      ($ :div
        ($ :p {:class "text-sm font-medium text-slate-600"} title)
        (cond
          error ($ :p {:class "text-sm text-red-600 mt-1"} error)
          loading? ($ :div {:class "mt-1 space-y-2"}
                     ($ :div {:class "h-5 w-24 bg-slate-100 animate-pulse rounded"})
                     ($ :div {:class "h-3 w-32 bg-slate-100 animate-pulse rounded"}))
          :else ($ :p {:class "text-2xl font-bold text-slate-900 mt-1"} value))
        (when (and subtitle (not loading?) (not error))
          ($ :p {:class "text-xs text-slate-500 mt-1"} subtitle)))
      (when icon
        ($ :div {:class "p-2 bg-slate-100 rounded-lg"}
          ($ :span {:class "text-xl"} icon))))
    (when (and trend (not loading?) (not error))
      ($ :div {:class "mt-3 flex items-center text-xs"}
        ($ :span {:class (if (pos? (:change trend)) "text-emerald-600" "text-red-600")}
          (str (when (pos? (:change trend)) "+") (:change trend) "%"))
        ($ :span {:class "text-slate-500 ml-1"} "vs last month")))))

;; ========================================================================
;; Quick Action Component
;; ========================================================================

(defui quick-action [{:keys [title description icon on-click]}]
  ($ :button {:class "flex items-center gap-4 p-4 bg-white rounded-xl shadow-sm border border-slate-100 hover:shadow-md hover:border-slate-200 transition-all text-left w-full"
              :on-click on-click}
    ($ :div {:class "p-3 bg-blue-50 rounded-lg text-blue-600"}
      ($ :span {:class "text-2xl"} icon))
    ($ :div
      ($ :h3 {:class "font-semibold text-slate-900"} title)
      ($ :p {:class "text-sm text-slate-600"} description))))

;; ========================================================================
;; Recent Expense Row Component
;; ========================================================================

(defui expense-row [{:keys [supplier amount date category status]}]
  ($ :div {:class "flex items-center justify-between py-3 border-b border-slate-100 last:border-0"}
    ($ :div {:class "flex items-center gap-3"}
      ($ :div {:class "w-10 h-10 bg-slate-100 rounded-lg flex items-center justify-center text-lg"}
        (case category
          "groceries" "🛒"
          "utilities" "💡"
          "transport" "🚗"
          "dining" "🍽️"
          "shopping" "🛍️"
          "📋"))
      ($ :div
        ($ :p {:class "font-medium text-slate-900"} supplier)
        ($ :p {:class "text-xs text-slate-500"} (or date "--"))))
    ($ :div {:class "text-right"}
      ($ :p {:class "font-semibold text-slate-900"} amount)
      (when status
        ($ :span {:class (str "ds-badge ds-badge-xs "
                           (case status
                             "posted" "ds-badge-success"
                             "pending" "ds-badge-warning"
                             "ds-badge-ghost"))}
          status)))))

(defui expense-row-skeleton []
  ($ :div {:class "flex items-center justify-between py-3 border-b border-slate-100 last:border-0"}
    ($ :div {:class "flex items-center gap-3"}
      ($ :div {:class "w-10 h-10 bg-slate-100 rounded-lg animate-pulse"})
      ($ :div {:class "space-y-2"}
        ($ :div {:class "h-4 w-32 bg-slate-100 rounded animate-pulse"})
        ($ :div {:class "h-3 w-20 bg-slate-100 rounded animate-pulse"})))
    ($ :div {:class "space-y-2 text-right"}
      ($ :div {:class "h-4 w-16 bg-slate-100 rounded animate-pulse ml-auto"})
      ($ :div {:class "h-3 w-14 bg-slate-100 rounded animate-pulse ml-auto"}))))

;; ========================================================================
;; Main Dashboard Page
;; ========================================================================

(defui expenses-dashboard-page []
  (let [user (use-subscribe [:current-user])
        user-name (or (:full_name user) (:full-name user) "there")
        summary (or (use-subscribe [:user-expenses/summary]) {})
        summary-loading? (boolean (use-subscribe [:user-expenses/summary-loading?]))
        summary-error (use-subscribe [:user-expenses/summary-error])
        by-month (or (use-subscribe [:user-expenses/by-month]) [])
        by-month-loading? (boolean (use-subscribe [:user-expenses/by-month-loading?]))
        recent (or (use-subscribe [:user-expenses/recent]) [])
        recent-loading? (boolean (use-subscribe [:user-expenses/recent-loading?]))
        recent-error (use-subscribe [:user-expenses/recent-error])
        currency-totals (or (:currency-totals summary) {})
        primary-currency (or (some #(when (contains? currency-totals %) %) [:BAM :USD :EUR])
                           (-> currency-totals keys first))
        primary-currency-str (when primary-currency (name primary-currency))
        total-sum (reduce + 0 (vals currency-totals))
        total-expenses (or (:total-expenses summary) 0)
        avg-spend (when (pos? total-expenses) (/ total-sum total-expenses))
        latest-month (first by-month)
        this-month-total (:total latest-month)
        this-month-currency (:currency latest-month)
        last-30-count (:recent-count summary)]
    ($ :div {:class "min-h-screen bg-slate-50"}
      ;; Header
      ($ :header {:class "bg-white border-b border-slate-200"}
        ($ :div {:class "max-w-6xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-slate-900"}
                (str "Hello, " user-name "!"))
              ($ :p {:class "text-sm text-slate-600"} "Here's your expense overview"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :primary
                         :on-click #(rf/dispatch [:navigate-to "/expenses/upload"])}
                "📷 Upload Receipt")
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses/list"])}
                "View All")))))

      ;; Error banner if needed
      (when (or summary-error recent-error)
        ($ :div {:class "max-w-6xl mx-auto px-4 mt-4"}
          ($ :div {:class "bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg"}
            ($ :p {:class "text-sm font-medium"}
              (or summary-error recent-error "Unable to load expense data.")))))

      ;; Main content
      ($ :main {:class "max-w-6xl mx-auto px-4 py-6"}
        ;; Stats Grid
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8"}
          ($ stat-card {:title "This Month"
                        :value (format-money this-month-total (or this-month-currency primary-currency-str "USD"))
                        :icon "💰"
                        :loading? (or summary-loading? by-month-loading?)
                        :subtitle "posted expenses"})
          ($ stat-card {:title "Total Expenses"
                        :value total-expenses
                        :icon "📋"
                        :loading? summary-loading?
                        :subtitle "all time"})
          ($ stat-card {:title "Last 30 days"
                        :value (or last-30-count 0)
                        :icon "⏳"
                        :loading? summary-loading?
                        :subtitle "expenses created"})
          ($ stat-card {:title "Avg per expense"
                        :value (if avg-spend (format-money avg-spend primary-currency-str) "--")
                        :icon "📊"
                        :loading? summary-loading?
                        :subtitle (str "currency " (or primary-currency-str "USD"))}))

        ;; Two column layout
        ($ :div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
          ;; Recent Expenses
          ($ :div {:class "lg:col-span-2"}
            ($ :div {:class "bg-white rounded-xl shadow-sm border border-slate-100"}
              ($ :div {:class "p-4 border-b border-slate-100 flex items-center justify-between"}
                ($ :h2 {:class "font-semibold text-slate-900"} "Recent Expenses")
                ($ :a {:href "/expenses/list" :class "text-sm text-blue-600 hover:text-blue-700"} "View all →"))
              ($ :div {:class "p-4"}
                (cond
                  recent-loading?
                  (for [i (range 3)] ($ expense-row-skeleton {:key i}))

                  (seq recent)
                  (for [expense recent]
                    (let [supplier (or (:supplier_display_name expense)
                                     (:supplier_normalized_key expense)
                                     "Unknown supplier")
                          amount (format-money (:total_amount expense)
                                   (or (:currency expense) primary-currency-str))
                          date (format-date (:purchased_at expense))
                          status (status-label expense)
                          category (or (:payer_type expense) "expense")]
                      ($ expense-row {:key (or (:id expense) (str supplier date))
                                      :supplier supplier
                                      :amount amount
                                      :date date
                                      :status status
                                      :category category})))

                  :else
                  ($ :p {:class "text-sm text-slate-500"}
                    "No expenses yet. Try adding one!")))))

          ;; Quick Actions
          ($ :div {:class "space-y-4"}
            ($ :h2 {:class "font-semibold text-slate-900"} "Quick Actions")
            ($ quick-action {:title "Upload Receipt"
                             :description "Scan or upload a receipt"
                             :icon "📷"
                             :on-click #(rf/dispatch [:navigate-to "/expenses/upload"])})
            ($ quick-action {:title "Add Expense"
                             :description "Manual expense entry"
                             :icon "✏️"
                             :on-click #(rf/dispatch [:navigate-to "/expenses/new"])})
            ($ quick-action {:title "View Reports"
                             :description "Monthly summaries"
                             :icon "📊"
                             :on-click #(rf/dispatch [:navigate-to "/expenses/reports"])})))))))
