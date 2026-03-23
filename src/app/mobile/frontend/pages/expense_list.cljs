(ns app.mobile.frontend.pages.expense-list
  "Mobile expense list — searchable, paginated list of expenses."
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
  :mobile/fetch-expenses
  (fn [{:keys [db]} [_ {:keys [search append?]}]]
    (let [offset (if append?
                   (count (get-in db [:mobile :expenses :data] []))
                   0)]
      {:db (assoc-in db [:mobile :expenses :loading?] true)
       :http-xhrio {:method :get
                    :uri "/api/v1/expenses"
                    :params (cond-> {:limit 20
                                     :offset offset
                                     :order-by "purchased-at"
                                     :order-dir "desc"}
                              (seq search) (assoc :supplier-display-name search))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/expenses-loaded append?]
                    :on-failure [:mobile/expenses-failed]}})))

(rf/reg-event-db
  :mobile/expenses-loaded
  (fn [db [_ append? response]]
    (let [new-data (:data response)
          existing (if append? (get-in db [:mobile :expenses :data] []) [])]
      (-> db
        (assoc-in [:mobile :expenses :data] (vec (concat existing new-data)))
        (assoc-in [:mobile :expenses :total] (:total response))
        (assoc-in [:mobile :expenses :loading?] false)))))

(rf/reg-event-db
  :mobile/expenses-failed
  (fn [db [_ error]]
    (-> db
      (assoc-in [:mobile :expenses :loading?] false)
      (assoc-in [:mobile :expenses :error]
        (or (get-in error [:response :error]) "Failed to load expenses")))))

;; ========================================================================
;; Subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/expenses
  (fn [db _]
    (get-in db [:mobile :expenses :data] [])))

(rf/reg-sub
  :mobile/expenses-total
  (fn [db _]
    (get-in db [:mobile :expenses :total] 0)))

(rf/reg-sub
  :mobile/expenses-loading?
  (fn [db _]
    (get-in db [:mobile :expenses :loading?] false)))

(rf/reg-sub
  :mobile/expenses-error
  (fn [db _]
    (get-in db [:mobile :expenses :error])))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- format-date [s]
  (when s
    (try
      (let [d (js/Date. s)]
        (str (.getDate d) "." (inc (.getMonth d)) "." (.getFullYear d)))
      (catch :default _ (str s)))))

(defn- format-amount [n currency]
  (when n
    (str (.toFixed (js/parseFloat (str n)) 2) " " (or currency "BAM"))))

;; ========================================================================
;; Components
;; ========================================================================

(defui expense-card [{:keys [expense]}]
  (let [id (or (:id expense) (str (random-uuid)))
        supplier (or (:supplier_display_name expense)
                   (:supplier-display-name expense) "—")
        total (or (:total_amount expense) (:total-amount expense))
        currency (or (:currency expense) "BAM")
        date (or (:purchased_at expense) (:purchased-at expense))
        category (or (:expense_category_name expense)
                   (:expense-category-name expense))
        store (or (:store_display_name expense)
                (:store-display-name expense))]
    ($ :button
      {:class "flex items-center w-full p-3 bg-base-100 rounded-xl shadow-sm active:bg-base-200 transition-colors"
       :on-click #(rf/dispatch [:mobile/navigate (str "/m/expenses/" id)])}
      ($ :div {:class "flex-1 text-left min-w-0"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :p {:class "text-sm font-medium truncate flex-1 mr-2"} supplier)
          ($ :p {:class "text-sm font-bold whitespace-nowrap"} (format-amount total currency)))
        ($ :div {:class "flex items-center gap-2 mt-0.5"}
          ($ :span {:class "text-xs text-base-content/50"} (format-date date))
          (when store
            ($ :span {:class "text-xs text-base-content/40"} (str "@ " store)))
          (when category
            ($ :span {:class "ds-badge ds-badge-ghost ds-badge-xs ml-auto"} category))))
      ($ :svg {:class "w-4 h-4 text-base-content/30 ml-2 flex-shrink-0" :fill "none" :viewBox "0 0 24 24" :stroke-width "2" :stroke "currentColor"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M8.25 4.5l7.5 7.5-7.5 7.5"})))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui expenses-page []
  (let [t (use-t)
        expenses (use-subscribe [:mobile/expenses])
        total (use-subscribe [:mobile/expenses-total])
        loading? (use-subscribe [:mobile/expenses-loading?])
        error (use-subscribe [:mobile/expenses-error])
        [search-text set-search-text!] (uix/use-state "")
        search-timer (uix/use-ref nil)]

    ;; Fetch on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-expenses {}]))
      [])

    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-expenses "Expenses")})

      ($ :div {:class "p-4 space-y-3 pb-24"}
        ;; Search bar
        ($ :div {:class "relative"}
          ($ :svg {:class "absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-base-content/40"
                   :fill "none" :viewBox "0 0 24 24" :stroke-width "2" :stroke "currentColor"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                      :d "M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"}))
          ($ :input {:type "text"
                     :class "ds-input ds-input-bordered w-full pl-10"
                     :placeholder (t :mobile/search-expenses "Search by supplier...")
                     :value search-text
                     :on-change (fn [e]
                                  (let [v (.. e -target -value)]
                                    (set-search-text! v)
                                    ;; Debounce search
                                    (when-let [timer @search-timer]
                                      (js/clearTimeout timer))
                                    (reset! search-timer
                                      (js/setTimeout
                                        #(rf/dispatch [:mobile/fetch-expenses {:search v}])
                                        300))))}))

        ;; Error
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span error)))

        ;; Expense list
        (if (and (empty? expenses) (not loading?))
          ($ :div {:class "text-center py-12"}
            ($ :p {:class "text-base-content/50 text-sm"}
              (t :mobile/no-expenses "No expenses found")))
          ($ :div {:class "space-y-2"}
            (for [expense expenses]
              ($ expense-card {:key (or (:id expense) (random-uuid))
                               :expense expense}))))

        ;; Load more
        (when (and (seq expenses) (< (count expenses) total))
          ($ :div {:class "text-center pt-2"}
            ($ :button
              {:class (str "ds-btn ds-btn-ghost ds-btn-sm "
                        (when loading? "ds-loading"))
               :disabled loading?
               :on-click #(rf/dispatch [:mobile/fetch-expenses {:search search-text :append? true}])}
              (when-not loading?
                (t :mobile/load-more "Load more")))))

        ;; Loading indicator
        (when (and loading? (empty? expenses))
          ($ :div {:class "space-y-2"}
            (for [i (range 5)]
              ($ :div {:key i :class "bg-base-100 rounded-xl p-4 shadow-sm animate-pulse h-16"}))))))))
