(ns app.template.frontend.pages.expense-detail
  "User-facing expense detail view."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn format-money
  [amount currency]
  (cond
    (nil? amount) "—"
    :else (try
            (.toLocaleString (js/Number amount) "en-US"
              #js {:style "currency"
                   :currency (or currency "USD")
                   :minimumFractionDigits 2
                   :maximumFractionDigits 2})
            (catch :default _
              (str (or currency "$") " " (.toFixed (js/Number amount) 2))))))

(defn format-date [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US"
      #js {:year "numeric" :month "long" :day "numeric" :hour "2-digit" :minute "2-digit"})))

(defn format-short-date [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US"
      #js {:year "numeric" :month "short" :day "numeric"})))

;; ========================================================================
;; Components
;; ========================================================================

(defui info-card [{:keys [label value icon]}]
  ($ :div {:class "bg-base-200 rounded-lg p-4"}
    ($ :div {:class "flex items-start gap-3"}
      (when icon
        ($ :span {:class "text-2xl"} icon))
      ($ :div
        ($ :span {:class "text-xs uppercase tracking-wide text-base-content/60"} label)
        ($ :p {:class "font-medium mt-1"} (or value "—"))))))

(defui line-item-table [{:keys [items currency]}]
  (if (seq items)
    ($ :div {:class "overflow-x-auto"}
      ($ :table {:class "ds-table ds-table-sm w-full"}
        ($ :thead
          ($ :tr
            ($ :th "Description")
            ($ :th {:class "text-right"} "Qty")
            ($ :th {:class "text-right"} "Unit Price")
            ($ :th {:class "text-right"} "Total")))
        ($ :tbody
          (for [{:keys [id raw_label qty unit_price line_total]} items]
            ($ :tr {:key (str id)}
              ($ :td raw_label)
              ($ :td {:class "text-right font-mono"} (or qty "—"))
              ($ :td {:class "text-right font-mono"} (when unit_price (format-money unit_price currency)))
              ($ :td {:class "text-right font-mono font-medium"} (format-money line_total currency)))))))
    ($ :p {:class "text-base-content/50 text-sm"} "No line items recorded.")))

(defui expense-detail-skeleton []
  ($ :div {:class "space-y-6 animate-pulse"}
    ($ :div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
      (for [i (range 4)]
        ($ :div {:key i :class "bg-base-200 rounded-lg h-20"})))
    ($ :div {:class "bg-base-200 rounded-lg h-48"})))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-detail-page []
  (let [current-route (use-subscribe [:current-route])
        expense-id (or (get-in current-route [:path-params :expense-id])
                       (get-in current-route [:parameters :path :expense-id]))
        expense (use-subscribe [:user-expenses/current-expense])
        loading? (boolean (use-subscribe [:user-expenses/current-expense-loading?]))
        error (use-subscribe [:user-expenses/current-expense-error])]
    
    ;; Fetch expense on mount
    (use-effect
      (fn []
        (when expense-id
          (rf/dispatch [:user-expenses/fetch-expense expense-id]))
        js/undefined)
      [expense-id])
    
    (let [{:keys [supplier_display_name payer_label total_amount currency
                  purchased_at notes is_posted items created_at]} expense]
      ($ :div {:class "min-h-screen bg-base-100"}
        ;; Header
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
            ($ :div {:class "flex items-center justify-between"}
              ($ :div
                ($ :div {:class "text-sm ds-breadcrumbs"}
                  ($ :ul
                    ($ :li ($ :a {:href "/expenses"} "Expenses"))
                    ($ :li ($ :a {:href "/expenses/list"} "All Expenses"))
                    ($ :li (or supplier_display_name "Detail"))))
                ($ :h1 {:class "text-xl sm:text-2xl font-bold"}
                  (or supplier_display_name "Expense Detail")))
              ($ :div {:class "flex gap-2"}
                ($ button {:btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses/list"])}
                  "Back")
                (when (and expense (not is_posted))
                  ($ button {:btn-type :outline
                             :on-click #(rf/dispatch [:navigate-to (str "/expenses/" expense-id "?edit=true")])}
                    "Edit"))
                ($ button {:btn-type :ghost
                           :on-click #(when expense-id
                                        (rf/dispatch [:user-expenses/fetch-expense expense-id]))}
                  "⟳")))))
        
        ;; Error
        (when error
          ($ :div {:class "max-w-4xl mx-auto px-4 mt-4"}
            ($ :div {:class "ds-alert ds-alert-error"}
              ($ :span error))))
        
        ;; Content
        ($ :main {:class "max-w-4xl mx-auto px-4 py-6"}
          (cond
            loading?
            ($ expense-detail-skeleton)
            
            (nil? expense)
            ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-8 text-center"}
              ($ :p {:class "text-base-content/50"} "Expense not found."))
            
            :else
            ($ :div {:class "space-y-6"}
              ;; Status badge
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class (str "ds-badge "
                                   (if is_posted "ds-badge-success" "ds-badge-warning"))}
                  (if is_posted "Posted" "Pending"))
                (when created_at
                  ($ :span {:class "text-sm text-base-content/60"}
                    (str "Created " (format-short-date created_at)))))
              
              ;; Info cards
              ($ :div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
                ($ info-card {:label "Supplier" :value supplier_display_name :icon "🏪"})
                ($ info-card {:label "Payer" :value payer_label :icon "👤"})
                ($ info-card {:label "Total" :value (format-money total_amount currency) :icon "💰"})
                ($ info-card {:label "Date" :value (format-short-date purchased_at) :icon "📅"}))
              
              ;; Notes
              (when (and notes (not (clojure.string/blank? notes)))
                ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-4"}
                  ($ :h3 {:class "font-semibold mb-2"} "Notes")
                  ($ :p {:class "text-base-content/80 whitespace-pre-wrap"} notes)))
              
              ;; Line items
              ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-4"}
                ($ :div {:class "flex items-center justify-between mb-4"}
                  ($ :h3 {:class "font-semibold"} "Line Items")
                  ($ :span {:class "text-sm text-base-content/60"}
                    (str (count items) " items")))
                ($ line-item-table {:items items :currency currency}))
              
              ;; Actions
              ($ :div {:class "flex gap-2 justify-end pt-4 border-t"}
                (when (not is_posted)
                  ($ button {:btn-type :error
                             :size :sm
                             :on-click #(rf/dispatch [:user-expenses/delete-expense expense-id])}
                    "Delete"))
                (when (not is_posted)
                  ($ button {:btn-type :primary
                             :size :sm
                             :on-click #(rf/dispatch [:user-expenses/post-expense expense-id])}
                    "Mark as Posted"))))))))))
