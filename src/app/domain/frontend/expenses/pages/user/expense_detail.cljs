(ns app.domain.frontend.expenses.pages.user.expense-detail
  "User-facing expense detail view."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [app.template.frontend.utils.timestamp :as timestamp]
    [clojure.string :as str]
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

(defn format-short-date [date-str]
  (timestamp/format-timestamp-string date-str))

(defn format-decimal
  [value digits]
  (cond
    (nil? value) "—"
    :else (try
            (.toFixed (js/Number value) digits)
            (catch :default _
              (str value)))))

;; ========================================================================
;; Components
;; ========================================================================

(defui info-card [{:keys [label value icon]}]
  ($ :div {:class "bg-base-200 rounded-lg p-4"}
    ($ :div {:class "flex items-start gap-3"}
      (when icon
        ($ :span {:class "text-2xl"} icon))
      ($ :div
        ($ :span {:class "text-sm uppercase tracking-wide text-base-content/60"} label)
        ($ :p {:class "font-medium mt-1 text-base"} (or value "—"))))))

(defui line-item-table [{:keys [items currency]}]
  (let [t (use-t)]
    (if (seq items)
      ($ :div {:class "overflow-x-auto"}
        ($ :table {:class "ds-table ds-table-sm w-full"}
          ($ :thead
            ($ :tr
              ($ :th (t :expense-detail/col-description))
              ($ :th {:class "text-right"} (t :expense-detail/col-qty))
              ($ :th {:class "text-right"} (t :expense-detail/col-unit-price))
              ($ :th {:class "text-right"} (t :expense-detail/col-total))))
          ($ :tbody
            (for [{:keys [id
                          raw_label raw-label
                          qty
                          unit_price unit-price
                          line_total line-total]} items]
              (let [raw-label* (or raw-label raw_label)
                    unit-price* (or unit-price unit_price)
                    line-total* (or line-total line_total)]
                ($ :tr {:key (str id)}
                  ($ :td raw-label*)
                  ($ :td {:class "text-right font-mono"} (or qty "—"))
                  ($ :td {:class "text-right font-mono"} (when unit-price* (format-money unit-price* currency)))
                  ($ :td {:class "text-right font-mono font-medium"} (format-money line-total* currency))))))))
      ($ :p {:class "text-base-content/50 text-sm"} (t :expense-detail/no-items)))))

(defui conversion-breakdown
  [{:keys [currency original-amount bam-amount exchange-rate rate-fetched-at]}]
  (let [t (use-t)]
    ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-4"}
      ($ :div {:class "flex items-center justify-between mb-4"}
        ($ :div
          ($ :h3 {:class "font-semibold text-base"} (t :expense-detail/conversion-title))
          ($ :p {:class "text-sm text-base-content/60"} (t :expense-detail/conversion-desc))))
      ($ :div {:class "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4"}
        ($ info-card {:label (t :expense-detail/label-original-amount)
                      :value (format-money original-amount currency)
                      :icon "💱"})
        ($ info-card {:label (t :expense-detail/label-bam-amount)
                      :value (format-money bam-amount "BAM")
                      :icon "🇧🇦"})
        ($ info-card {:label (t :expense-detail/label-exchange-rate)
                      :value (when exchange-rate
                               (str "1 " currency " = " (format-decimal exchange-rate 4) " BAM"))
                      :icon "📈"})
        ($ info-card {:label (t :expense-detail/label-rate-fetched-at)
                      :value (format-short-date rate-fetched-at)
                      :icon "🕒"})))))

(defui expense-detail-skeleton []
  ($ :div {:class "space-y-6 animate-pulse"}
    ($ :div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
      (for [i (range 4)]
        ($ :div {:key i :class "bg-base-200 rounded-lg h-20"})))
    ($ :div {:class "bg-base-200 rounded-lg h-48"})))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-detail-page
  [{:keys [expense-id in-modal?] :as _props}]
  (let [t (use-t)
        current-route (use-subscribe [:current-route])
        expense-id (or expense-id
                     (get-in current-route [:path-params :expense-id])
                     (get-in current-route [:parameters :path :expense-id]))
        expense (use-subscribe [:user-expenses/current-expense])
        loading? (boolean (use-subscribe [:user-expenses/current-expense-loading?]))
        error (use-subscribe [:user-expenses/current-expense-error])
        can-write? (use-subscribe [:expenses/can-write?])]

    ;; Fetch expense on mount
    (use-effect
      (fn []
        (when expense-id
          (rf/dispatch [:user-expenses/fetch-expense expense-id]))
        js/undefined)
      [expense-id])

    (let [{:keys [supplier_display_name supplier-display-name
                  payer_label payer-label
                  total_amount total-amount
                  original_amount original-amount
                  bam_amount bam-amount
                  exchange_rate exchange-rate
                  rate_fetched_at rate-fetched-at
                  currency
                  purchased_at purchased-at
                  notes
                  is_posted is-posted
                  items
                  created_at created-at]} expense
          supplier-name (or supplier-display-name supplier_display_name)
          payer-name (or payer-label payer_label)
          total (or total-amount total_amount)
          original-total (or original-amount original_amount)
          bam-total (or bam-amount bam_amount)
          current-rate (or exchange-rate exchange_rate)
          rate-fetched (or rate-fetched-at rate_fetched_at)
          purchased (or purchased-at purchased_at)
          created (or created-at created_at)
          posted? (true? (or is-posted is_posted))
          show-conversion? (and (some? currency)
                             (not= "BAM" currency)
                             (or original-total bam-total current-rate))]
      ($ :div {:class "min-h-screen bg-base-100"}
        ;; Header (hidden when in modal)
        (when-not in-modal?
          ($ :header {:class "bg-white border-b border-base-200"}
            ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
              ($ :div {:class "flex items-center justify-between"}
                ($ :div
                  ($ :div {:class "text-sm ds-breadcrumbs"}
                    ($ :ul
                      ($ :li ($ :a {:href "/expenses"} (t :expense-detail/breadcrumb-expenses)))
                      ($ :li ($ :a {:href "/expenses/list"} (t :expense-detail/breadcrumb-all)))
                      ($ :li (or supplier-name "Detail"))))
                  ($ :h1 {:class "text-xl sm:text-2xl font-bold"}
                    (or supplier-name "Expense Detail")))
                ($ :div {:class "flex gap-2"}
                  (when (and expense (not posted?) can-write?)
                    ($ button {:btn-type :outline
                               :id "btn-edit-expense"
                               :on-click #(rf/dispatch [:navigate-to (str "/expenses/" expense-id "?edit=true")])}
                      (t :expense-detail/btn-edit)))
                  ($ button {:btn-type :ghost
                             :on-click #(when expense-id
                                          (rf/dispatch [:user-expenses/fetch-expense expense-id]))}
                    "⟳"))))))

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
              ($ :p {:class "text-base-content/50"} (t :expense-detail/not-found)))

            :else
            ($ :div {:class "space-y-6"}
              ;; Status badge
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class (str "ds-badge "
                                   (if posted? "ds-badge-success" "ds-badge-warning"))}
                  (if posted? (t :expense-detail/status-posted) (t :expense-detail/status-pending)))
                (when created
                  ($ :span {:class "text-sm text-base-content/60"}
                    (str (t :expense-detail/created) " " (format-short-date created)))))

              ;; Info cards
              ($ :div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
                ($ info-card {:label (t :expense-detail/label-supplier) :value supplier-name :icon "🏪"})
                ($ info-card {:label (t :expense-detail/label-payer) :value payer-name :icon "👤"})
                ($ info-card {:label (t :expense-detail/label-total) :value (format-money total currency) :icon "💰"})
                ($ info-card {:label (t :expense-detail/label-date) :value (format-short-date purchased) :icon "📅"}))

              (when show-conversion?
                ($ conversion-breakdown
                  {:currency currency
                   :original-amount (or original-total total)
                   :bam-amount (or bam-total total)
                   :exchange-rate current-rate
                   :rate-fetched-at rate-fetched}))

              ;; Notes
              (when (and notes (not (str/blank? notes)))
                ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-4"}
                  ($ :h3 {:class "font-semibold mb-2 text-base"} (t :expense-detail/notes-title))
                  ($ :p {:class "text-base-content/80 whitespace-pre-wrap text-base"} notes)))

              ;; Line items
              ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-4"}
                ($ :div {:class "flex items-center justify-between mb-4"}
                  ($ :h3 {:class "font-semibold text-base"} (t :expense-detail/line-items-title))
                  ($ :span {:class "text-base text-base-content/60"}
                    (t :expense-detail/n-items (count items))))
                ($ line-item-table {:items items :currency currency}))

              ;; Actions - only show for member+ (can-write?)
              (when can-write?
                ($ :div {:class "flex gap-2 justify-end pt-4 border-t"}
                  (when (not posted?)
                    ($ button {:btn-type :error
                               :size :sm
                               :id "btn-delete-expense"
                               :on-click #(rf/dispatch [:user-expenses/delete-expense expense-id])}
                      (t :expense-detail/btn-delete)))
                  (when (not posted?)
                    ($ button {:btn-type :primary
                               :size :sm
                               :id "btn-post-expense"
                               :on-click #(rf/dispatch [:user-expenses/post-expense expense-id])}
                      (t :expense-detail/btn-post))))))))))))
