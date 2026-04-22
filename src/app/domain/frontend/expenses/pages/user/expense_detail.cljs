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

(defn- expense-receipt-id
  [expense]
  (or (:receipt-id expense)
    (:receipt_id expense)))

(defn- manual-expense?
  [expense]
  (nil? (expense-receipt-id expense)))

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
            (for [{:keys [id raw-label qty unit-price line-total]} items]
              ($ :tr {:key (str id)}
                ($ :td raw-label)
                ($ :td {:class "text-right font-mono"} (or qty "—"))
                ($ :td {:class "text-right font-mono"} (when unit-price (format-money unit-price currency)))
                ($ :td {:class "text-right font-mono font-medium"} (format-money line-total currency)))))))
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

    ;; Data is always normalized to kebab-case by convert-db-keys->app-keys.
    ;; Avoid mixing snake_case + kebab-case symbols in :keys — they compile
    ;; to the same JS identifier and shadow each other.
    (let [{:keys [supplier-display-name
                  payer-label
                  total-amount
                  original-amount
                  bam-amount
                  exchange-rate
                  rate-fetched-at
                  currency
                  purchased-at
                  notes
                  items
                  created-at]} expense
          supplier-name supplier-display-name
          payer-name payer-label
          total total-amount
          original-total original-amount
          bam-total bam-amount
          current-rate exchange-rate
          rate-fetched rate-fetched-at
          purchased purchased-at
          created created-at
          linked-receipt-id (some-> (expense-receipt-id expense) str)
          direct-editable? (and expense (manual-expense? expense))
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
                  (when (and expense can-write? direct-editable?)
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
              (when created
                ($ :div {:class "text-sm text-base-content/60"}
                  (str (t :expense-detail/created) " " (format-short-date created))))

              (when linked-receipt-id
                ($ :div {:class "ds-alert ds-alert-info"}
                  ($ :span "This expense was created from a receipt. Edit the receipt to change the extracted expense data.")))

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
                  (when linked-receipt-id
                    ($ button {:btn-type :outline
                               :size :sm
                               :id (str "btn-edit-linked-receipt-" linked-receipt-id)
                               :on-click #(rf/dispatch [:navigate-to (str "/receipts/" linked-receipt-id)])}
                      "Edit receipt"))
                  ($ button {:btn-type :error
                             :size :sm
                             :id "btn-delete-expense"
                             :on-click #(rf/dispatch [:user-expenses/delete-expense expense-id])}
                    (t :expense-detail/btn-delete)))))))))))
