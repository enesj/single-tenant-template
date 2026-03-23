(ns app.mobile.frontend.pages.dashboard
  "Mobile dashboard — summary cards, monthly trend, top suppliers, categories."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Events
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-dashboard
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/dashboard"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/dashboard-loaded]
                  :on-failure [:mobile/dashboard-failed]}}))

(rf/reg-event-db
  :mobile/dashboard-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :dashboard] (:data response))))

(rf/reg-event-db
  :mobile/dashboard-failed
  (fn [db [_ error]]
    (assoc-in db [:mobile :dashboard-error]
              (or (get-in error [:response :error]) "Failed to load dashboard"))))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/dashboard
  (fn [db _]
    (get-in db [:mobile :dashboard])))

(rf/reg-sub
  :mobile/dashboard-error
  (fn [db _]
    (get-in db [:mobile :dashboard-error])))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- format-amount [n]
  (when n
    (.toLocaleString (js/Number. n) "de-DE"
      #js {:minimumFractionDigits 2 :maximumFractionDigits 2})))

(defn- pct-change [current previous]
  (when (and current previous (pos? previous))
    (let [change (* 100 (/ (- current previous) previous))]
      (.toFixed change 0))))

;; ========================================================================
;; Components
;; ========================================================================

(defui stat-card [{:keys [title value subtitle change-pct]}]
  ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
    ($ :p {:class "text-xs text-base-content/60 uppercase tracking-wide"} title)
    ($ :p {:class "text-2xl font-bold mt-1"} value)
    (when (or subtitle change-pct)
      ($ :div {:class "flex items-center gap-2 mt-1"}
        (when change-pct
          (let [positive? (>= (js/parseFloat change-pct) 0)]
            ($ :span {:class (str "text-xs font-medium "
                               (if positive? "text-success" "text-error"))}
              (str (when positive? "+") change-pct "%"))))
        (when subtitle
          ($ :span {:class "text-xs text-base-content/50"} subtitle))))))

(defui monthly-trend [{:keys [data t]}]
  (when (seq data)
    (let [max-val (apply max (map #(or (:total_bam %) 0) data))]
      ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
        ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
          (t :mobile/monthly-trend "Monthly Trend"))
        ($ :div {:class "space-y-2"}
          (for [month data]
            (let [total (or (:total_bam month) 0)
                  pct (if (pos? max-val) (* 100 (/ total max-val)) 0)]
              ($ :div {:key (:month_label month)}
                ($ :div {:class "flex items-center justify-between text-xs mb-0.5"}
                  ($ :span {:class "text-base-content/60"} (:month_label month))
                  ($ :span {:class "font-medium"} (str (format-amount total) " BAM")))
                ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                  ($ :div {:class "bg-primary rounded-full h-2 transition-all"
                           :style {:width (str (max 2 pct) "%")}}))))))))))

(defui top-list [{:keys [title items name-key amount-key]}]
  (when (seq items)
    ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
      ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"} title)
      ($ :div {:class "space-y-2"}
        (for [[idx item] (map-indexed vector items)]
          ($ :div {:key idx :class "flex items-center justify-between py-1"}
            ($ :div {:class "flex items-center gap-2 min-w-0 flex-1"}
              ($ :span {:class "text-xs text-base-content/40 w-4"} (str (inc idx) "."))
              ($ :span {:class "text-sm truncate"} (get item name-key)))
            ($ :span {:class "text-sm font-medium whitespace-nowrap ml-2"}
              (str (format-amount (get item amount-key)) " BAM"))))))))

(defui category-breakdown [{:keys [data t]}]
  (when (seq data)
    ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
      ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
        (t :mobile/categories "Categories"))
      ($ :div {:class "space-y-2"}
        (for [cat data]
          ($ :div {:key (:category_name cat)}
            ($ :div {:class "flex items-center justify-between text-sm"}
              ($ :span {:class "truncate flex-1"} (or (:category_name cat) "—"))
              ($ :span {:class "font-medium ml-2"}
                (str (format-amount (:total_spend_bam cat)) " BAM")))
            ($ :div {:class "w-full bg-base-200 rounded-full h-1.5 mt-1"}
              ($ :div {:class "bg-secondary rounded-full h-1.5"
                       :style {:width (str (or (:percentage cat) 0) "%")}}))))))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui dashboard-page []
  (let [t (use-t)
        dashboard (use-subscribe [:mobile/dashboard])
        error (use-subscribe [:mobile/dashboard-error])]

    ;; Fetch on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-dashboard]))
      [])

    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-dashboard "Home")})

      ($ :div {:class "p-4 space-y-4 pb-24"}
        ;; Error
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span error)))

        ;; Loading
        (when (and (not dashboard) (not error))
          ($ :div {:class "space-y-4"}
            (for [i (range 3)]
              ($ :div {:key i :class "bg-base-100 rounded-xl p-4 shadow-sm animate-pulse h-20"}))))

        (when dashboard
          (let [last-30 (:last-30-days dashboard)
                last-6m (:last-6-months dashboard)
                averages (:averages dashboard)]
            ($ :<>
              ;; Summary cards
              ($ :div {:class "grid grid-cols-2 gap-3"}
                ($ stat-card
                  {:title (t :mobile/total-30d "Last 30 days")
                   :value (str (format-amount (:total_spend_bam last-30)) " BAM")
                   :change-pct (pct-change (:total_spend_bam last-30) (:prev_spend_bam last-30))
                   :subtitle (t :mobile/vs-prev "vs previous")})
                ($ stat-card
                  {:title (t :mobile/expense-count "Expenses")
                   :value (str (:expense_count last-30))
                   :change-pct (pct-change (:expense_count last-30) (:prev_count last-30))
                   :subtitle (t :mobile/vs-prev "vs previous")}))

              ;; Averages
              (when averages
                ($ :div {:class "grid grid-cols-3 gap-3"}
                  ($ stat-card
                    {:title (t :mobile/daily-avg "Daily avg")
                     :value (str (format-amount (:daily_avg_30d averages)))})
                  ($ stat-card
                    {:title (t :mobile/weekly-avg "Weekly avg")
                     :value (str (format-amount (:weekly_avg_30d averages)))})
                  ($ stat-card
                    {:title (t :mobile/monthly-avg "Monthly avg")
                     :value (str (format-amount (:monthly_avg_6m averages)))})))

              ;; Monthly trend
              ($ monthly-trend {:data (:monthly-trend dashboard) :t t})

              ;; Top suppliers
              ($ top-list
                {:title (t :mobile/top-suppliers "Top Suppliers")
                 :items (:top-suppliers dashboard)
                 :name-key :display_name
                 :amount-key :total_spend_bam})

              ;; Category breakdown
              ($ category-breakdown {:data (:category-breakdown dashboard) :t t})

              ;; Biggest expense
              (when-let [biggest (:biggest-expense dashboard)]
                ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
                  ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-2"}
                    (t :mobile/biggest-expense "Biggest Expense"))
                  ($ :p {:class "text-lg font-bold"}
                    (str (format-amount (:bam_amount biggest)) " BAM"))
                  ($ :p {:class "text-sm text-base-content/60"}
                    (str (:supplier_name biggest)
                      (when (:store_name biggest) (str " — " (:store_name biggest))))))))))))))
