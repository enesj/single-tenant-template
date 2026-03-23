(ns app.mobile.frontend.pages.reports
  "Mobile reports — category breakdown, monthly spending, day-of-week patterns."
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
  :mobile/fetch-reports
  (fn [_ _]
    {:fx [[:dispatch [:mobile/fetch-report-category]]
          [:dispatch [:mobile/fetch-report-monthly]]
          [:dispatch [:mobile/fetch-report-day-of-week]]]}))

(rf/reg-event-fx
  :mobile/fetch-report-category
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/reports/by-category"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/report-loaded :by-category]
                  :on-failure [:mobile/report-failed :by-category]}}))

(rf/reg-event-fx
  :mobile/fetch-report-monthly
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/by-month"
                  :params {:months_back 6}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/report-loaded :by-month]
                  :on-failure [:mobile/report-failed :by-month]}}))

(rf/reg-event-fx
  :mobile/fetch-report-day-of-week
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/reports/day-of-week"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/report-loaded :day-of-week]
                  :on-failure [:mobile/report-failed :day-of-week]}}))

(rf/reg-event-db
  :mobile/report-loaded
  (fn [db [_ report-type response]]
    (assoc-in db [:mobile :reports report-type] (:data response))))

(rf/reg-event-db
  :mobile/report-failed
  (fn [db [_ report-type _error]]
    (assoc-in db [:mobile :reports report-type] [])))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/report
  (fn [db [_ report-type]]
    (get-in db [:mobile :reports report-type])))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- format-amount [n]
  (when n
    (.toLocaleString (js/Number. n) "de-DE"
      #js {:minimumFractionDigits 2 :maximumFractionDigits 2})))

(def ^:private day-names ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])

;; ========================================================================
;; Components
;; ========================================================================

(defui category-report [{:keys [data t]}]
  (when (seq data)
    (let [top-10 (take 10 data)
          max-val (apply max (map #(or (:total_amount %) 0) top-10))]
      ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
        ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
          (t :mobile/by-category "By Article Category"))
        ($ :div {:class "space-y-3"}
          (for [cat top-10]
            (let [total (or (:total_amount cat) 0)
                  pct (if (pos? max-val) (* 100 (/ total max-val)) 0)]
              ($ :div {:key (str (:category_name cat) "-" (:subcategory_name cat))}
                ($ :div {:class "flex items-center justify-between text-sm"}
                  ($ :span {:class "truncate flex-1 mr-2"}
                    (or (:subcategory_name cat)
                      (:category_name cat)
                      (t :mobile/uncategorized "Uncategorized")))
                  ($ :div {:class "text-right whitespace-nowrap"}
                    ($ :span {:class "font-medium"} (format-amount total))
                    ($ :span {:class "text-xs text-base-content/50 ml-1"}
                      (str "(" (:item_count cat) ")"))))
                ($ :div {:class "w-full bg-base-200 rounded-full h-2 mt-1"}
                  ($ :div {:class "bg-secondary rounded-full h-2 transition-all"
                           :style {:width (str (max 2 pct) "%")}}))))))))))

(defui monthly-report [{:keys [data t]}]
  (when (seq data)
    (let [bam-data (filter #(= "BAM" (:currency %)) data)
          max-val (apply max (cons 0 (map #(or (:total %) 0) bam-data)))]
      ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
        ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
          (t :mobile/monthly-spending "Monthly Spending"))
        ($ :div {:class "space-y-2"}
          (for [month bam-data]
            (let [total (or (:total month) 0)
                  pct (if (pos? max-val) (* 100 (/ total max-val)) 0)]
              ($ :div {:key (:month month)}
                ($ :div {:class "flex items-center justify-between text-xs mb-0.5"}
                  ($ :span {:class "text-base-content/60"} (:month month))
                  ($ :span {:class "font-medium"} (str (format-amount total) " BAM")))
                ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                  ($ :div {:class "bg-primary rounded-full h-2 transition-all"
                           :style {:width (str (max 2 pct) "%")}}))))))))))

(defui day-of-week-report [{:keys [data t]}]
  (let [;; API returns [{currency: "BAM", days: [...]}] — extract BAM days
        bam-entry (some #(when (= "BAM" (:currency %)) %) data)
        days (or (:days bam-entry) [])]
    (when (seq days)
      (let [max-val (apply max (map #(or (:total_amount %) 0) days))]
        ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
          ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
            (t :mobile/by-day "By Day of Week"))
          ($ :div {:class "flex gap-1 h-32"}
            (for [day days]
              (let [total (or (:total_amount day) 0)
                    pct (if (pos? max-val) (* 100 (/ total max-val)) 0)
                    day-idx (dec (or (:iso_day_of_week day) 1))
                    day-name (get day-names day-idx "?")]
                ($ :div {:key day-idx :class "flex-1 flex flex-col items-center"}
                  ($ :div {:class "w-full flex-1 flex items-end"}
                    ($ :div {:class "w-full bg-primary rounded-t transition-all"
                             :style {:height (str (max 4 pct) "%")}}))
                  ($ :span {:class "text-[10px] text-base-content/50 mt-1"} day-name))))))))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui reports-page []
  (let [t (use-t)
        by-category (use-subscribe [:mobile/report :by-category])
        by-month (use-subscribe [:mobile/report :by-month])
        day-of-week (use-subscribe [:mobile/report :day-of-week])
        loaded? (or by-category by-month day-of-week)]

    ;; Fetch on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-reports]))
      [])

    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-reports "Reports")})

      ($ :div {:class "p-4 space-y-4 pb-24"}
        ;; Loading
        (when-not loaded?
          ($ :div {:class "space-y-4"}
            (for [i (range 3)]
              ($ :div {:key i :class "bg-base-100 rounded-xl p-4 shadow-sm animate-pulse h-40"}))))

        ;; Reports
        ($ category-report {:data by-category :t t})
        ($ monthly-report {:data by-month :t t})
        ($ day-of-week-report {:data day-of-week :t t})))))
