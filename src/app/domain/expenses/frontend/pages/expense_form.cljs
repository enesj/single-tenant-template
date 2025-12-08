(ns app.domain.expenses.frontend.pages.expense-form
  "Manual expense entry form for the expenses domain"
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.domain.expenses.frontend.events.expenses :as expenses-events]
    [app.domain.expenses.frontend.events.payers :as payers-events]
    [app.domain.expenses.frontend.events.suppliers :as suppliers-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- pad-two
  [value]
  (let [s (str value)]
    (if (< (count s) 2)
      (str "0" s)
      s)))

(defn- current-datetime-local
  []
  (let [now (js/Date.)]
    (str (.getFullYear now)
      "-"
      (pad-two (inc (.getMonth now)))
      "-"
      (pad-two (.getDate now))
      "T"
      (pad-two (.getHours now))
      ":"
      (pad-two (.getMinutes now)))))

(defn- new-line-item
  []
  {:id (str (random-uuid))
   :raw_label ""
   :qty ""
   :unit_price ""
   :line_total ""})

(defn- format-decimal
  "Format a number to a 2 decimal place string for inputs."
  [n]
  (when (some? n)
    (.toFixed n 2)))

(defn- recalc-line-total-if-possible
  "When qty and unit price are both present and line-total is blank, auto-calc it."
  [item]
  (let [qty-num (parse-number (:qty item))
        unit-num (parse-number (:unit_price item))
        line-str (:line_total item)
        should-auto? (and qty-num unit-num (str/blank? (str line-str)))]
    (if should-auto?
      (assoc item :line_total (format-decimal (* qty-num unit-num)))
      item)))

(defn- update-line-item
  [items item-id key value]
  (mapv (fn [item]
          (if (= item-id (:id item))
            (-> item
              (assoc key value)
              recalc-line-total-if-possible)
            item))
    items))

(defn- remove-line-item
  [items item-id]
  (let [remaining (vec (remove #(= item-id (:id %)) items))]
    (if (seq remaining)
      remaining
      [(new-line-item)])))

(defn- parse-number
  [value]
  (when (and (some? value)
          (not (str/blank? (str value))))
    (let [n (js/parseFloat value)]
      (when-not (js/isNaN n) n))))

(defn- prepare-line-items
  [items]
  (keep (fn [{:keys [raw_label article_id qty unit_price line_total]}]
          (let [parsed-total (parse-number line_total)]
            (when (and (not (str/blank? raw_label)) parsed-total)
              (let [qty-num (parse-number qty)
                    unit-num (parse-number unit_price)
                    base {:raw_label raw_label
                          :line_total parsed-total}
                    base (cond-> base
                           qty-num (assoc :qty qty-num)
                           unit-num (assoc :unit_price unit-num))
                    article-id (when (and article_id
                                       (not (str/blank? (str article_id))))
                                 article_id)]
                (if article-id
                  (assoc base :article_id article-id)
                  base)))))
    items))

(defn- line-items-total
  [items]
  (->> items
    (map (fn [{:keys [line_total]}] (parse-number line_total)))
    (remove nil?)
    (reduce + 0)))

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def ^:private amount-tolerance 0.01)

(defn- handle-submit!
  [{:keys [supplier-id payer-id purchased-at total-amount currency notes line-items prepared-items computed-total set-validation-error!]}]
  (let [parsed-total (parse-number total-amount)
        effective-total (or parsed-total (when (pos? computed-total) computed-total))
        has-items? (seq prepared-items)
        diff (when (and effective-total (pos? computed-total))
               (js/Math.abs (- effective-total computed-total)))]
    (cond
      (or (str/blank? supplier-id)
        (str/blank? payer-id)
        (str/blank? purchased-at))
      (set-validation-error! "Supplier, payer, and purchased date are required.")

      (not has-items?)
      (set-validation-error! "Add at least one line item with a label and line total.")

      (or (nil? effective-total) (<= effective-total 0))
      (set-validation-error! "Enter a total amount greater than 0 (or fill from line items).")

      (and (pos? computed-total) (> diff amount-tolerance))
      (set-validation-error!
        (str "Total amount (" (format-decimal effective-total)
          ") must match line items total (" (format-decimal computed-total) ")."))

      :else
      (do
        (set-validation-error! nil)
        (rf/dispatch [::expenses-events/create-entry
                      {:supplier_id supplier-id
                       :payer_id payer-id
                       :purchased_at purchased-at
                       :currency currency
                       :notes notes
                       :total_amount effective-total
                       :items (vec prepared-items)}])))))

(defui admin-expense-form-page []
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        loading? (use-subscribe [:expenses/form-loading?])
        form-error (use-subscribe [:expenses/form-error])
        [supplier-id set-supplier-id!] (use-state nil)
        [payer-id set-payer-id!] (use-state nil)
        [purchased-at set-purchased-at!] (use-state (current-datetime-local))
        [total-amount set-total-amount!] (use-state "")
        [currency set-currency!] (use-state "BAM")
        [notes set-notes!] (use-state "")
        [line-items set-line-items!] (use-state [(new-line-item)])
        [validation-error set-validation-error!] (use-state nil)]
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load {:limit 100}])
        (rf/dispatch [::payers-events/load {}])
        js/undefined)
      [])
    (use-effect
      (fn []
        (when (and (seq suppliers) (not supplier-id))
          (set-supplier-id! (:id (first suppliers))))
        (when (and (seq payers) (not payer-id))
          (set-payer-id! (:id (first payers)))))
      [payer-id supplier-id suppliers payers])
    (let [prepared-items (prepare-line-items line-items)
          computed-total (line-items-total prepared-items)
          parsed-total (parse-number total-amount)
          total-mismatch? (and parsed-total (pos? computed-total)
                            (> (js/Math.abs (- parsed-total computed-total)) amount-tolerance))
          handle-line-change (fn [item-id key]
                               (fn [e]
                                 (set-line-items!
                                   (update-line-item line-items item-id key (.. e -target -value)))))
          remove-item (fn [item-id]
                        (set-line-items! (remove-line-item line-items item-id)))
          add-item (fn []
                     (set-line-items! (conj line-items (new-line-item))))]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/expenses"} "Expenses"))
                ($ :li ($ :span "New"))))
            ($ :h1 {:class "text-2xl font-bold"} "Create Expense"))
          ($ :div {:class "flex gap-2"}
            ($ :button {:class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(rf/dispatch [:admin/navigate-client "/admin/expenses"])}
              "Cancel")
            ($ :button {:class (str "ds-btn ds-btn-primary ds-btn-sm" (when loading? " ds-btn-disabled"))
                        :type "button"
                        :disabled loading?
                        :on-click #(handle-submit!
                                     {:supplier-id supplier-id
                                      :payer-id payer-id
                                      :purchased-at purchased-at
                                      :total-amount total-amount
                                      :currency currency
                                      :notes notes
                                      :line-items line-items
                                      :prepared-items prepared-items
                                      :computed-total computed-total
                                      :set-validation-error! set-validation-error!})}
              (if loading?
                ($ :span {:class "flex items-center gap-2"}
                  ($ :span "Saving")
                  ($ :span {:class "ds-loading ds-loading-spinner text-current"}))
                "Save Expense"))))

        (when validation-error
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span validation-error)))
        (when form-error
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span form-error)))

        ($ :div {:class "grid gap-4 md:grid-cols-2"}
          ($ :div {:class "space-y-3"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Supplier"))
            ($ :select {:class "select select-bordered w-full"
                        :value (or supplier-id "")
                        :on-change #(set-supplier-id! (.. % -target -value))}
              ($ :option {:value "" :disabled true} "Select supplier")
              (for [{:keys [id display_name]} suppliers]
                ($ :option {:key id :value id} display_name))))
          ($ :div {:class "space-y-3"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Payer"))
            ($ :select {:class "select select-bordered w-full"
                        :value (or payer-id "")
                        :on-change #(set-payer-id! (.. % -target -value))}
              ($ :option {:value "" :disabled true} "Select payer")
              (for [{:keys [id label type]} payers]
                ($ :option {:key id :value id}
                  (str label (when type (str " (" type ")"))))))))

        ($ :div {:class "grid gap-4 md:grid-cols-3"}
          ($ :div {:class "space-y-2"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Purchased at"))
            ($ :input {:class "input input-bordered w-full"
                       :type "datetime-local"
                       :value purchased-at
                       :on-change #(set-purchased-at! (.. % -target -value))}))
          ($ :div {:class "space-y-2"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Total amount"))
            ($ :div {:class "flex gap-2"}
              ($ :input {:class "input input-bordered w-full"
                         :type "number"
                         :step "0.01"
                         :value total-amount
                         :on-change #(set-total-amount! (.. % -target -value))})
              (when (pos? computed-total)
                ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                            :type "button"
                            :on-click #(set-total-amount! (format-decimal computed-total))}
                  "Use total")))
            (when (pos? computed-total)
              ($ :p {:class "text-xs text-base-content/60"}
                (str "Line items total: " (shared/format-value computed-total "0" false))
                (when total-mismatch?
                  ($ :span {:class "text-error ml-2"} "(does not match total)")))))
          ($ :div {:class "space-y-2"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Currency"))
            ($ :select {:class "select select-bordered w-full"
                        :value currency
                        :on-change #(set-currency! (.. % -target -value))}
              (for [{:keys [value label]} currency-options]
                ($ :option {:key value :value value} label)))))

        ($ :div {:class "space-y-2"}
          ($ :label {:class "label"}
            ($ :span {:class "label-text"} "Notes"))
          ($ :textarea {:class "textarea textarea-bordered w-full"
                        :rows 3
                        :value notes
                        :placeholder "Optional notes"
                        :on-change #(set-notes! (.. % -target -value))}))

        ($ :div {:class "space-y-4"}
          ($ :div {:class "flex items-center justify-between"}
            ($ :h2 {:class "text-lg font-semibold"} "Line Items")
            ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                        :type "button"
                        :on-click #(add-item)}
              "Add line item"))
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "table w-full"}
              ($ :thead
                ($ :tr
                  ($ :th "Label")
                  ($ :th "Qty")
                  ($ :th "Unit Price")
                  ($ :th "Line Total")
                  ($ :th "")))
              ($ :tbody
                (for [{:keys [id raw_label qty unit_price line_total]} line-items]
                  ($ :tr {:key id}
                    ($ :td
                      ($ :input {:class "input input-bordered w-full"
                                 :type "text"
                                 :value raw_label
                                 :placeholder "e.g. Milk, Bread"
                                 :on-change ((handle-line-change id :raw_label))}))
                    ($ :td
                      ($ :input {:class "input input-bordered w-full"
                                 :type "number"
                                 :step "0.01"
                                 :min "0"
                                 :value qty
                                 :on-change ((handle-line-change id :qty))}))
                    ($ :td
                      ($ :input {:class "input input-bordered w-full"
                                 :type "number"
                                 :step "0.01"
                                 :min "0"
                                 :value unit_price
                                 :on-change ((handle-line-change id :unit_price))}))
                    ($ :td
                      ($ :input {:class "input input-bordered w-full"
                                 :type "number"
                                 :step "0.01"
                                 :min "0"
                                 :value line_total
                                 :on-change ((handle-line-change id :line_total))}))
                    ($ :td
                      ($ :button {:class "text-xs text-error"
                                  :type "button"
                                  :on-click #(remove-item id)}
                        "Remove"))))))))))))
