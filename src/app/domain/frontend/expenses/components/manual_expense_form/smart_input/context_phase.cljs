(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.context-phase
  "Phase 2 rendering: context/review with items summary, context search,
  payer/date/currency/notes, and submit."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [format-decimal safe-parse-number]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.components
     :refer [autocomplete-dropdown entity-chip phase-two-quick-pick-groups
             quick-picks type-picker]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
     :refer [colorize-quick-pick-groups entity-type-label]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defui context-phase-view
  "Renders Phase 2 (context + review) of the smart expense form.

  Shows items summary, context chips, search for missing context
  (supplier/store/category), payer/date/currency/notes fields,
  and submit/cancel buttons."
  [{:keys [t items context input-text input-ref
           dropdown-open? highlight-idx type-picker-text creating?
           search-results quick-search-loading? context-suggestions
           items-total currency currency-options
           payers payer-id purchased-at notes submitting?
           on-cancel supplier-color-map
           suppliers stores expense-categories articles
           ;; handlers
           on-input-change on-input-keydown on-select-result
           on-create-inline on-type-pick on-remove-context
           on-set-phase on-focus-input on-set-payer-id
           on-set-purchased-at on-set-currency on-set-notes
           on-cancel-type-picker submit-disabled?]}]
  ($ :div {:class "space-y-6"}

    ;; Back to items link
    ($ :button {:type "button"
                :class "text-sm text-primary hover:text-primary/80 transition-colors"
                :on-click (fn [e] (.preventDefault e)
                            (on-set-phase :items)
                            (on-focus-input))}
      (t :smart-expense/back-to-items))

    ;; Items summary
    ($ :div {:class "bg-base-100 rounded-2xl border border-base-200 p-4"}
      ($ :div {:class "flex items-center justify-between mb-3"}
        ($ :span {:class "font-semibold text-base"}
          (t :smart-expense/items-summary (count items)))
        ($ :span {:class "text-lg font-bold"}
          (format-decimal items-total) " " currency))
      ($ :div {:class "space-y-1.5"}
        (for [item items]
          (let [q (or (safe-parse-number (:qty item)) 1)
                p (or (safe-parse-number (:unit-price item)) 0)]
            ($ :div {:key (:id item)
                     :class "flex items-center gap-2 text-sm text-base-content/70"}
              ($ :span "📦")
              ($ :span {:class "flex-1 truncate"} (:label item))
              ($ :span {:class "font-mono"}
                (str q " × " (format-decimal p))))))))

    ;; Context chips (already selected)
    (when (seq context)
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [[entity-type chip] context]
          ($ entity-chip {:key (name entity-type)
                          :entity-type entity-type
                          :label (:label chip)
                          :on-remove #(on-remove-context entity-type)}))))

    ;; Context search input — only show when something is still missing
    (let [missing (vec (remove #(contains? context %) [:supplier :store :category]))
          placeholder (when (seq missing)
                        (str (t :smart-expense/search-prefix)
                          (str/join ", " (map #(entity-type-label t %) missing))
                          "..."))
          single-missing? (= 1 (count missing))]
      (when (seq missing)
        (let [missing-set (set missing)
              selected-supplier-id (some-> context :supplier :id)
              raw-quick-pick-groups (when (str/blank? input-text)
                                      (phase-two-quick-pick-groups missing
                                        context-suggestions
                                        suppliers
                                        stores
                                        expense-categories
                                        articles
                                        selected-supplier-id))
              quick-pick-groups (some-> raw-quick-pick-groups
                                  (colorize-quick-pick-groups supplier-color-map))
              filtered-results (filterv
                                 (fn [r]
                                   (let [et (keyword (or (:entity-type r) (:entity_type r)))]
                                     (contains? missing-set et)))
                                 (or search-results []))]
          ($ :<>
            (when (seq quick-pick-groups)
              ($ :div {:class "space-y-4"}
                (for [{:keys [entity-type items]} quick-pick-groups]
                  ($ :div {:key (name entity-type)
                           :class "space-y-2"}
                    ($ :p {:class "text-sm text-base-content/50"}
                      (str (t :smart-expense/pick-prefix) (entity-type-label t entity-type)))
                    ($ quick-picks
                      {:entity-type entity-type
                       :items items
                       :on-select on-select-result})))))

            ($ :div {:class "relative"
                     :on-click (fn [e] (.stopPropagation e))}
              ($ :input {:ref input-ref
                         :id "smart-expense-context-input"
                         :type "text"
                         :auto-focus (not single-missing?)
                         :class (str "w-full text-lg p-4 rounded-xl border-2 border-base-300 "
                                  "focus:border-primary focus:outline-none focus:shadow-lg "
                                  "focus:shadow-primary/10 "
                                  "transition-all bg-white placeholder:text-base-content/30")
                         :placeholder (if single-missing?
                                        (t :smart-expense/or-search-suffix
                                          (entity-type-label t (first missing)))
                                        placeholder)
                         :value input-text
                         :on-change on-input-change
                         :on-key-down on-input-keydown
                         :auto-complete "off"})

              ;; Autocomplete dropdown (portal) — filtered to missing types only
              (when dropdown-open?
                ($ autocomplete-dropdown
                  {:t t
                   :results filtered-results
                   :loading? (and quick-search-loading? (empty? filtered-results))
                   :highlight-idx highlight-idx
                   :on-select on-select-result
                   :on-create on-create-inline
                   :input-text (str/trim input-text)
                   :anchor-ref input-ref})))

            ;; Type picker — only missing context types
            (when type-picker-text
              ($ type-picker
                {:t t
                 :text type-picker-text
                 :on-pick on-type-pick
                 :on-cancel on-cancel-type-picker
                 :creating? creating?
                 :allowed-types missing}))))))

    ;; Pick Payer chips
    (when (and (str/blank? (str payer-id)) (seq payers))
      ($ :div {:class "space-y-2"}
        ($ :p {:class "text-sm text-base-content/50"} (t :smart-expense/pick-payer))
        ($ :div {:class "flex flex-wrap gap-2"}
          (for [p (take 5 payers)]
            ($ :button {:key (str "payer-" (:id p))
                        :type "button"
                        :class (str "inline-flex items-center gap-2 px-4 py-2.5 rounded-full "
                                 "text-base font-medium border cursor-pointer "
                                 "transition-all hover:shadow-md hover:scale-[1.02] "
                                 "bg-sky-50 hover:bg-sky-100 text-sky-800 border-sky-200")
                        :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                    (on-set-payer-id (str (:id p))))}
              ($ :span "💳")
              ($ :span {:class "truncate max-w-[200px]"} (:label p)))))))

    ;; Payer + Date + Currency row
    ($ :div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4"}
      ;; Payer
      ($ :div
        ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} (t :smart-expense/payer-label))
        ($ :select {:id "smart-expense-payer"
                    :class (str "w-full text-lg p-4 h-14 rounded-xl border-2 "
                             "border-base-300 bg-white focus:border-primary cursor-pointer")
                    :value (or payer-id "")
                    :on-change (fn [e] (on-set-payer-id (.. e -target -value)))}
          ($ :option {:value ""} (t :smart-expense/payer-select-ph))
          (for [p payers]
            ($ :option {:key (:id p) :value (:id p)}
              (str (:label p)
                (when-let [pt (or (:type p) (:payer-type-label p))]
                  (str " (" pt ")")))))))
      ;; Date
      ($ :div
        ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} (t :smart-expense/date-label))
        ($ :input {:id "smart-expense-date"
                   :type "datetime-local"
                   :class (str "w-full text-lg p-4 h-14 rounded-xl border-2 "
                            "border-base-300 bg-white focus:border-primary")
                   :value purchased-at
                   :on-change (fn [e] (on-set-purchased-at (.. e -target -value)))}))
      ;; Currency
      ($ :div
        ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} (t :smart-expense/currency-label))
        ($ :select {:class (str "w-full text-lg p-4 h-14 rounded-xl border-2 border-base-300 "
                             "bg-white focus:border-primary cursor-pointer")
                    :value currency
                    :on-change (fn [e] (on-set-currency (.. e -target -value)))}
          (for [{:keys [value label]} currency-options]
            ($ :option {:key value :value value} label)))))

    ;; Notes
    ($ :div
      ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} (t :smart-expense/notes-label))
      ($ :textarea {:id "smart-expense-notes"
                    :class (str "w-full text-lg p-4 rounded-xl border-2 "
                             "border-base-300 bg-white focus:border-primary resize-none")
                    :rows 2
                    :placeholder (t :smart-expense/notes-ph)
                    :value (or notes "")
                    :on-change (fn [e] (on-set-notes (.. e -target -value)))}))

    ;; Actions
    ($ :div {:class "flex justify-end gap-3 pt-4"}
      (when on-cancel
        ($ :button {:id "btn-cancel-smart-expense"
                    :type "button"
                    :class "ds-btn ds-btn-lg text-lg"
                    :disabled submitting?
                    :on-click (fn [e] (.preventDefault e) (on-cancel))}
          (t :smart-expense/cancel)))
      ($ :button {:id "btn-save-smart-expense"
                  :type "submit"
                  :class "ds-btn ds-btn-primary ds-btn-lg text-lg px-10"
                  :disabled (or submitting? submit-disabled?)}
        (if submitting? (t :smart-expense/saving) (t :smart-expense/save))))))
