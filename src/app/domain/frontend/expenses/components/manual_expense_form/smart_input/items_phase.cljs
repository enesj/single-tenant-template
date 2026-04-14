(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.items-phase
  "Phase 1 rendering: item entry with search, quick picks, and running total."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [format-decimal]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.components
     :refer [autocomplete-dropdown combination-picks entity-chip item-row quick-picks type-picker]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
     :refer [entity-type-label search-placeholder]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defui items-phase-view
  "Renders Phase 1 (items entry) of the smart expense form.

  Shows search input for articles, item rows with qty/price,
  quick-pick suggestions, and a running total."
  [{:keys [t items context input-text input-ref article-mode?
           dropdown-open? highlight-idx type-picker-text creating?
           search-results quick-search-loading? cooccurring-pick-items
           focused-quick-pick-groups available-search-types items-total
           currency total-dropdown-count focus-item-id
           filtered-combos
           ;; handlers
           on-input-change on-input-keydown on-select-result
           on-create-inline on-type-pick on-remove-context
           on-update-item on-remove-item on-focus-input
           on-cancel-type-picker on-set-error on-focus-handled
           on-apply-combination]}]
  ($ :div {:class "space-y-4"}

    ;; Welcome prompt
    (when (and (empty? items) (empty? context))
      ($ :div {:class "text-center py-2"}
        ($ :p {:class "text-2xl font-semibold text-base-content/80 mb-1"}
          (t :smart-expense/title))
        ($ :p {:class "text-base text-base-content/50"}
          (t :smart-expense/subtitle))))

    ;; Expense combination quick-picks (filtered by current context)
    ($ combination-picks
      {:t t
       :combos filtered-combos
       :on-apply-combination on-apply-combination})

      ;; Selected context chips
      (when (seq context)
        ($ :div {:class "flex flex-wrap gap-2"}
          (for [[entity-type chip] context]
            ($ entity-chip {:key (str "items-phase-" (name entity-type))
                            :entity-type entity-type
                            :label (:label chip)
                            :on-remove #(on-remove-context entity-type)
                            :size :sm}))))

      ;; Items list
      (when (seq items)
        ($ :div {:class "space-y-2"}
          (for [item items]
            ($ item-row {:key (:id item)
                         :item item
                         :on-change on-update-item
                         :on-remove on-remove-item
                         :on-enter-price on-focus-input
                         :auto-focus-qty? (= focus-item-id (:id item))
                         :on-focus-handled on-focus-handled}))))

      ;; The BIG search input
      ($ :div {:class "relative"
               :on-click (fn [e] (.stopPropagation e))}
        ($ :input {:ref input-ref
                   :id "smart-expense-input"
                   :type "text"
                   :auto-focus true
                   :class (str "w-full "
                            (if (or (seq items) (seq context))
                              "text-lg p-4 rounded-xl border-2 border-base-300 "
                              "text-xl sm:text-2xl p-5 sm:p-6 rounded-2xl border-2 border-base-300 ")
                            "focus:border-primary focus:outline-none focus:shadow-lg "
                            "focus:shadow-primary/10 "
                            "transition-all bg-white placeholder:text-base-content/30")
                   :placeholder (search-placeholder t context (or (seq items) (seq context)) article-mode?)
                   :value input-text
                   :on-change on-input-change
                   :on-key-down on-input-keydown
                   :auto-complete "off"})

        ;; Autocomplete dropdown (portal)
        (when dropdown-open?
          ($ autocomplete-dropdown
            {:t t
             :results (or search-results [])
             :loading? (and quick-search-loading? (empty? search-results))
             :highlight-idx highlight-idx
             :on-select on-select-result
             :on-create on-create-inline
             :input-text (str/trim input-text)
             :anchor-ref input-ref})))

      ;; Co-occurring article suggestions (article mode)
      (when (seq cooccurring-pick-items)
        ($ :div {:class "space-y-2"}
          ($ :p {:class "text-sm text-base-content/50"}
            (t :smart-expense/frequently-together))
          ($ quick-picks
            {:t t
             :entity-type :article
             :items cooccurring-pick-items
             :on-select on-select-result})))

      ;; Normal quick pick groups (when not in article mode)
      (when (seq focused-quick-pick-groups)
        ($ :div {:class "space-y-4"}
          (for [{:keys [entity-type items]} focused-quick-pick-groups]
            ($ :div {:key (str "items-phase-" (name entity-type))
                     :class "space-y-2"}
              ($ :p {:class "text-sm text-base-content/50"}
                (str (t :smart-expense/pick-prefix) (entity-type-label t entity-type)))
              ($ quick-picks
                {:t t
                 :entity-type entity-type
                 :items items
                 :on-select on-select-result})))))

      ;; Type picker
      (when type-picker-text
        ($ type-picker
          {:t t
           :text type-picker-text
           :on-pick on-type-pick
           :on-cancel on-cancel-type-picker
           :creating? creating?
           :allowed-types available-search-types}))

      ;; Running total
      (when (seq items)
        ($ :div {:class "flex items-center justify-between pt-2"}
          ($ :p {:class "text-base text-base-content/50"}
            (t :smart-expense/item-count (count items)))
          ($ :p {:class "text-lg font-bold"}
            (format-decimal items-total) " " currency)))

      ;; Hint text
      (when (nil? type-picker-text)
        ($ :p {:class "text-center text-sm text-base-content/35 mt-2"}
          (cond
            article-mode?
            (t :smart-expense/hint-article-mode)

            (seq items)
            (t :smart-expense/hint-has-items)

            :else
            (t :smart-expense/hint-empty))))))