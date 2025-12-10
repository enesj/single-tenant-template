(ns app.template.frontend.pages.expenses-list
  "User-facing expense list page with filtering and pagination."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.subs.user-expenses]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn format-money
  "Format a number with currency."
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
  "Full date format for list view."
  [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US"
      #js {:year "numeric" :month "short" :day "numeric"})))

(defn status-badge [expense]
  (let [posted? (:is_posted expense)]
    ($ :span {:class (str "ds-badge ds-badge-sm "
                       (if posted? "ds-badge-success" "ds-badge-warning"))}
      (if posted? "Posted" "Pending"))))

;; ========================================================================
;; Table Components
;; ========================================================================

(defui expense-table-row [{:keys [expense on-view on-edit]}]
  (let [{:keys [id supplier_display_name payer_label total_amount currency purchased_at]} expense]
    ($ :tr {:class "hover:bg-base-200 cursor-pointer"
            :on-click #(when on-view (on-view id))}
      ($ :td {:class "font-medium"} (or supplier_display_name "—"))
      ($ :td (or payer_label "—"))
      ($ :td {:class "text-right font-mono"} (format-money total_amount currency))
      ($ :td (format-date purchased_at))
      ($ :td (status-badge expense))
      ($ :td {:class "text-right"}
        ($ :div {:class "flex gap-1 justify-end"}
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn [e]
                                  (.stopPropagation e)
                                  (when on-view (on-view id)))}
            "View")
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn [e]
                                  (.stopPropagation e)
                                  (when on-edit (on-edit id)))}
            "Edit"))))))

(defui expense-table-skeleton []
  ($ :tr
    (for [i (range 6)]
      ($ :td {:key i}
        ($ :div {:class "h-4 bg-base-300 rounded animate-pulse"})))))

;; ========================================================================
;; Pagination Component
;; ========================================================================

(defui pagination [{:keys [total limit offset on-page-change]}]
  (let [current-page (inc (quot offset limit))
        total-pages (max 1 (js/Math.ceil (/ total limit)))
        can-prev? (> current-page 1)
        can-next? (< current-page total-pages)]
    ($ :div {:class "flex items-center justify-between mt-4"}
      ($ :span {:class "text-sm text-base-content/70"}
        (str "Showing " (inc offset) "-" (min (+ offset limit) total) " of " total))
      ($ :div {:class "ds-join"}
        ($ :button {:class (str "ds-join-item ds-btn ds-btn-sm" (when-not can-prev? " ds-btn-disabled"))
                    :disabled (not can-prev?)
                    :on-click #(on-page-change (- offset limit))}
          "«")
        ($ :span {:class "ds-join-item ds-btn ds-btn-sm ds-btn-active"}
          (str "Page " current-page " of " total-pages))
        ($ :button {:class (str "ds-join-item ds-btn ds-btn-sm" (when-not can-next? " ds-btn-disabled"))
                    :disabled (not can-next?)
                    :on-click #(on-page-change (+ offset limit))}
          "»")))))

;; ========================================================================
;; Filter Component
;; ========================================================================

(defui filters [{:keys [on-filter-change]}]
  (let [[show-filters? set-show-filters!] (use-state false)]
    ($ :div {:class "mb-4"}
      ($ :div {:class "flex items-center gap-2 mb-2"}
        ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                    :on-click #(set-show-filters! (not show-filters?))}
          (if show-filters? "Hide Filters" "Show Filters")
          ($ :span {:class "ml-1"} (if show-filters? "▲" "▼"))))
      (when show-filters?
        ($ :div {:class "bg-base-200 p-4 rounded-lg grid grid-cols-1 md:grid-cols-3 gap-4"}
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "Status"))
            ($ :select {:class "ds-select ds-select-sm ds-select-bordered w-full"
                        :on-change #(on-filter-change :is_posted (.. % -target -value))}
              ($ :option {:value ""} "All")
              ($ :option {:value "true"} "Posted")
              ($ :option {:value "false"} "Pending")))
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "From Date"))
            ($ :input {:type "date"
                       :class "ds-input ds-input-sm ds-input-bordered w-full"
                       :on-change #(on-filter-change :from (.. % -target -value))}))
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "To Date"))
            ($ :input {:type "date"
                       :class "ds-input ds-input-sm ds-input-bordered w-full"
                       :on-change #(on-filter-change :to (.. % -target -value))})))))))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expenses-list-page []
  (let [expenses (or (use-subscribe [:user-expenses/recent]) [])
        loading? (boolean (use-subscribe [:user-expenses/recent-loading?]))
        error (use-subscribe [:user-expenses/recent-error])
        total (or (use-subscribe [:user-expenses/recent-total]) 0)
        limit (or (use-subscribe [:user-expenses/recent-limit]) 25)
        offset (or (use-subscribe [:user-expenses/recent-offset]) 0)
        [current-filters set-current-filters!] (use-state {})
        
        handle-filter-change (fn [key value]
                               (let [new-filters (if (empty? value)
                                                   (dissoc current-filters key)
                                                   (assoc current-filters key value))]
                                 (set-current-filters! new-filters)
                                 (rf/dispatch [:user-expenses/fetch-recent
                                               (merge {:limit limit :offset 0} new-filters)])))
        
        handle-page-change (fn [new-offset]
                             (rf/dispatch [:user-expenses/fetch-recent
                                           (merge {:limit limit :offset new-offset} current-filters)]))
        
        handle-view (fn [id]
                      (rf/dispatch [:navigate-to (str "/expenses/" id)]))
        
        handle-edit (fn [id]
                      (rf/dispatch [:navigate-to (str "/expenses/" id "?edit=true")]))]
    
    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-6xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "My Expenses")
              ($ :p {:class "text-sm text-base-content/70"} "View and manage your expense history"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :primary
                         :on-click #(rf/dispatch [:navigate-to "/expenses/new"])}
                "+ New Expense")
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))
      
      ;; Error banner
      (when error
        ($ :div {:class "max-w-6xl mx-auto px-4 mt-4"}
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span error))))
      
      ;; Main content
      ($ :main {:class "max-w-6xl mx-auto px-4 py-6"}
        ;; Filters
        ($ filters {:on-filter-change handle-filter-change})
        
        ;; Table
        ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 overflow-hidden"}
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-zebra w-full"}
              ($ :thead
                ($ :tr
                  ($ :th "Supplier")
                  ($ :th "Payer")
                  ($ :th {:class "text-right"} "Amount")
                  ($ :th "Date")
                  ($ :th "Status")
                  ($ :th {:class "text-right"} "Actions")))
              ($ :tbody
                (cond
                  loading?
                  (for [i (range 5)]
                    ($ expense-table-skeleton {:key i}))
                  
                  (empty? expenses)
                  ($ :tr
                    ($ :td {:col-span 6 :class "text-center py-8 text-base-content/50"}
                      "No expenses found. Start by adding your first expense!"))
                  
                  :else
                  (for [expense expenses]
                    ($ expense-table-row {:key (:id expense)
                                          :expense expense
                                          :on-view handle-view
                                          :on-edit handle-edit})))))))
        
        ;; Pagination
        (when (and (not loading?) (pos? total))
          ($ pagination {:total total
                         :limit limit
                         :offset offset
                         :on-page-change handle-page-change}))))))
