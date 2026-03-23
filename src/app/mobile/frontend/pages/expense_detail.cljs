(ns app.mobile.frontend.pages.expense-detail
  "Mobile expense detail — read-only view of a single expense."
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
  :mobile/fetch-expense
  (fn [_ [_ expense-id]]
    {:http-xhrio {:method :get
                  :uri (str "/api/v1/expenses/" expense-id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/expense-loaded]
                  :on-failure [:mobile/expense-load-failed]}}))

(rf/reg-event-db
  :mobile/expense-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :expense-detail] (:data response))))

(rf/reg-event-db
  :mobile/expense-load-failed
  (fn [db [_ error]]
    (assoc-in db [:mobile :expense-detail-error]
              (or (get-in error [:response :error]) "Failed to load expense"))))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/expense-detail
  (fn [db _]
    (get-in db [:mobile :expense-detail])))

(rf/reg-sub
  :mobile/expense-detail-error
  (fn [db _]
    (get-in db [:mobile :expense-detail-error])))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- format-date [s]
  (when s
    (try
      (let [d (js/Date. s)]
        (str (.getDate d) "." (inc (.getMonth d)) "." (.getFullYear d) " "
          (.toLocaleTimeString d "de-DE" #js {:hour "2-digit" :minute "2-digit"})))
      (catch :default _ (str s)))))

(defn- format-amount [n currency]
  (when n
    (str (.toFixed (js/parseFloat (str n)) 2) " " (or currency "BAM"))))

(defn- field-row [label value]
  (when value
    ($ :div {:class "flex justify-between py-2 border-b border-base-200"}
      ($ :span {:class "text-sm text-base-content/60"} label)
      ($ :span {:class "text-sm font-medium text-right max-w-[60%] truncate"} value))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui expense-detail-page []
  (let [t (use-t)
        route (use-subscribe [:mobile/current-route])
        expense-id (get-in route [:path-params :id])
        expense (use-subscribe [:mobile/expense-detail])
        error (use-subscribe [:mobile/expense-detail-error])]

    ;; Fetch on mount
    (uix/use-effect
      (fn []
        (when expense-id
          (rf/dispatch [:mobile/fetch-expense expense-id])))
      [expense-id])

    ($ :<>
      ($ mobile-header {:title (t :mobile/expense-detail "Expense Detail") :show-back? true})

      ($ :div {:class "p-4 pb-24 space-y-4"}
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span error)))

        ;; Loading
        (when (and (not expense) (not error))
          ($ :div {:class "flex items-center justify-center h-48"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})))

        (when expense
          (let [total (or (:total_amount expense) (:total-amount expense))
                currency (or (:currency expense) "BAM")
                supplier (or (:supplier_display_name expense) (:supplier-display-name expense))
                store (or (:store_display_name expense) (:store-display-name expense))
                payer (or (:payer_label expense) (:payer-label expense))
                category (or (:expense_category_name expense) (:expense-category-name expense))
                date (or (:purchased_at expense) (:purchased-at expense))
                notes (or (:notes expense))
                items (or (:items expense) [])]

            ($ :<>
              ;; Amount header
              ($ :div {:class "text-center py-4"}
                ($ :p {:class "text-3xl font-bold"} (format-amount total currency))
                (when supplier
                  ($ :p {:class "text-base text-base-content/60 mt-1"} supplier)))

              ;; Details card
              ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
                (field-row (t :common/date "Date") (format-date date))
                (field-row (t :common/supplier "Supplier") supplier)
                (field-row (t :common/store "Store") store)
                (field-row (t :common/payer "Payer") payer)
                (field-row (t :common/category "Category") category)
                (field-row (t :common/currency "Currency") currency)
                (when (seq notes)
                  (field-row (t :common/notes "Notes") notes)))

              ;; Line items
              (when (seq items)
                ($ :div {:class "bg-base-100 rounded-xl p-4 shadow-sm"}
                  ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-3"}
                    (t :common/line-items "Line Items"))
                  ($ :div {:class "space-y-2"}
                    (for [[idx item] (map-indexed vector items)]
                      (let [label (or (:description item) (:raw_label item) (:label item) "—")
                            qty (or (:qty item) (:quantity item))
                            unit-price (or (:unit_price item) (:unit-price item))
                            line-total (or (:line_total item) (:total item))]
                        ($ :div {:key idx :class "py-1.5 border-b border-base-200 last:border-0"}
                          ($ :div {:class "flex items-center justify-between"}
                            ($ :span {:class "text-sm truncate flex-1 mr-2"} label)
                            (when line-total
                              ($ :span {:class "text-sm font-medium whitespace-nowrap"}
                                (format-amount line-total currency))))
                          (when (or qty unit-price)
                            ($ :p {:class "text-xs text-base-content/50 mt-0.5"}
                              (str
                                (when qty (str qty " x "))
                                (when unit-price (format-amount unit-price currency))))))))))))))))))
