(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input
  "Progressive smart input for manual expense entry.

  Phase 1 (Context): Single big input with unified autocomplete across
  suppliers, stores, categories, articles. Chips accumulate above the input.
  Entering a number sets the total and transitions to Phase 2.

  Phase 2 (Review): Chips summary, editable total/currency, pre-filled
  defaults (payer, date), optional notes & line items. Submit."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [current-datetime-local format-decimal line-items-total
             new-line-item safe-parse-number]]
    [app.domain.frontend.expenses.components.form-fields.line-items
     :refer [line-items-input]]
    [app.domain.frontend.expenses.components.manual-expense-form.search :as search]
    app.domain.frontend.expenses.events.user-expenses.quick-search
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-ref use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ─────────────────────────────────────────────
;; Constants
;; ─────────────────────────────────────────────

(def ^:private chip-styles
  {:supplier "bg-blue-100 text-blue-800 border-blue-200"
   :store    "bg-emerald-100 text-emerald-800 border-emerald-200"
   :category "bg-purple-100 text-purple-800 border-purple-200"
   :article  "bg-amber-100 text-amber-800 border-amber-200"})

(def ^:private type-button-styles
  {:supplier "bg-blue-50 hover:bg-blue-100 text-blue-700 border-blue-200"
   :store    "bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border-emerald-200"
   :category "bg-purple-50 hover:bg-purple-100 text-purple-700 border-purple-200"
   :article  "bg-amber-50 hover:bg-amber-100 text-amber-700 border-amber-200"})

(def ^:private create-events
  {:supplier :user-expenses/create-supplier-modal
   :store    :user-expenses/create-store-modal
   :category :user-expenses/create-expense-category-modal
   :article  :user-expenses/create-article-modal})

(def ^:private create-field-names
  {:supplier :display_name
   :store    :display_name
   :category :name
   :article  :canonical_name})

(def ^:private currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def ^:private line-item-columns
  [{:id :raw_label :label "Label" :type :text :placeholder "e.g. Milk" :width "min-w-[180px]"}
   {:id :qty :label "Qty" :type :number :step "0.001" :min "0" :width "w-[100px]"}
   {:id :unit_price :label "Price" :type :number :step "0.01" :min "0" :width "w-[100px]"}
   {:id :line_total :label "Total" :type :number :step "0.01" :min "0" :width "w-[100px]"}])

;; ─────────────────────────────────────────────
;; Helpers
;; ─────────────────────────────────────────────

(defn- payer-default-id
  [payers]
  (or (some #(when (or (:is-default %) (:isDefault %)) (:id %)) payers)
    (:id (first payers))))

(defn- prepare-items
  [items]
  (vec
    (keep (fn [{:keys [raw_label qty unit_price line_total]}]
            (let [parsed-total (safe-parse-number line_total)]
              (when (and (not (str/blank? (str raw_label)))
                      (number? parsed-total))
                (cond-> {:raw_label (str raw_label) :line_total parsed-total}
                  (number? (safe-parse-number qty))
                  (assoc :qty (safe-parse-number qty))
                  (number? (safe-parse-number unit_price))
                  (assoc :unit_price (safe-parse-number unit_price))))))
      (or items []))))

(defn- validate-form
  [{:keys [chips total-amount payer-id itemized? items]}]
  (let [has-context? (seq chips)
        parsed-total (safe-parse-number total-amount)
        prepared-items (when itemized? (prepare-items items))
        computed-total (when itemized? (line-items-total (or prepared-items [])))
        effective-total (if itemized?
                          (when (and computed-total (pos? computed-total)) computed-total)
                          parsed-total)]
    (cond
      (not has-context?)
      {:ok? false :error "Add at least one: supplier, store, category, or article."}

      (str/blank? (str payer-id))
      {:ok? false :error "Payer is required."}

      (or (nil? effective-total) (<= effective-total 0))
      {:ok? false :error "Enter a total amount greater than 0."}

      (and itemized? (empty? prepared-items))
      {:ok? false :error "Add at least one line item with a label and total."}

      :else {:ok? true})))

(defn- prepare-submit-values
  [{:keys [chips total-amount currency purchased-at payer-id notes itemized? items]}]
  (let [prepared-items (prepare-items items)
        computed-total (line-items-total prepared-items)
        parsed-total (safe-parse-number total-amount)
        effective-total (if itemized? computed-total parsed-total)]
    (cond-> {:payer_id payer-id
             :purchased_at purchased-at
             :currency currency
             :total_amount effective-total}
      (:supplier chips) (assoc :supplier_id (get-in chips [:supplier :id]))
      (:store chips) (assoc :store_id (get-in chips [:store :id]))
      (:category chips) (assoc :expense_category_id (get-in chips [:category :id]))
      (and (not itemized?) (:article chips))
      (assoc :article_id (get-in chips [:article :id]))
      (not (str/blank? notes)) (assoc :notes notes)
      (and itemized? (seq prepared-items)) (assoc :items prepared-items))))

;; ─────────────────────────────────────────────
;; Sub-components
;; ─────────────────────────────────────────────

(defui entity-chip
  [{:keys [entity-type label on-remove size]}]
  (let [{:keys [icon]} (search/entity-type-info entity-type)
        style-class (get chip-styles entity-type "bg-base-200 text-base-content")
        large? (not= size :sm)]
    ($ :span {:class (str "inline-flex items-center gap-2 rounded-full font-medium border "
                       "transition-all select-none "
                       (if large? "px-4 py-2.5 text-base " "px-3 py-1.5 text-sm ")
                       style-class)}
      ($ :span {:class (if large? "text-lg" "text-base")} icon)
      ($ :span label)
      (when on-remove
        ($ :button {:type "button"
                    :class "ml-1 opacity-60 hover:opacity-100 text-lg leading-none transition-opacity"
                    :on-click (fn [e] (.preventDefault e) (.stopPropagation e) (on-remove))}
          "\u00D7")))))

(defui autocomplete-dropdown
  [{:keys [results loading? highlight-idx on-select on-create input-text]}]
  (when (or (seq results) loading? (and input-text (not (str/blank? input-text))))
    ($ :div {:class (str "absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl "
                      "shadow-2xl border border-base-200 max-h-80 overflow-y-auto z-50")}
      ;; Loading indicator
      (when loading?
        ($ :div {:class "flex items-center gap-3 px-5 py-4 text-base-content/40"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
          ($ :span {:class "text-base"} "Searching...")))

      ;; Search results
      (when (seq results)
        (for [[idx result] (map-indexed vector results)]
          (let [etype (keyword (or (:entity-type result) (:entity_type result)))
                {:keys [icon label]} (search/entity-type-info etype)
                highlighted? (= idx highlight-idx)]
            ($ :button {:key (str (name etype) "-" (:id result))
                        :type "button"
                        :class (str "w-full text-left px-5 py-4 flex items-center gap-4 "
                                 "transition-colors cursor-pointer "
                                 (if highlighted?
                                   "bg-primary/10"
                                   "hover:bg-base-100"))
                        :on-mouse-enter (fn [_])
                        :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                    (on-select (assoc result :entity-type etype)))}
              ($ :span {:class "text-2xl flex-none w-8 text-center"} icon)
              ($ :span {:class "flex-1 text-lg font-medium truncate"} (:label result))
              ($ :span {:class (str "text-xs px-2.5 py-1 rounded-full border font-medium "
                                 (get chip-styles etype ""))}
                label)))))

      ;; "Create new" option
      (when (and input-text (not (str/blank? input-text)))
        ($ :button {:type "button"
                    :class (str "w-full text-left px-5 py-4 flex items-center gap-4 "
                             "border-t border-base-200 transition-colors cursor-pointer "
                             (if (= highlight-idx (count results))
                               "bg-primary/10"
                               "hover:bg-base-100"))
                    :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                (on-create input-text))}
          ($ :span {:class "text-2xl flex-none w-8 text-center text-primary"} "+")
          ($ :span {:class "text-lg"}
            "Create "
            ($ :span {:class "font-bold"} (str "\u201C" input-text "\u201D"))))))))

(defui type-picker
  [{:keys [text on-pick on-cancel creating? has-supplier?]}]
  ($ :div {:class "mt-4 p-6 bg-base-100 rounded-2xl border border-base-200"}
    ($ :p {:class "text-lg text-base-content/70 mb-4"}
      "What is "
      ($ :span {:class "font-bold text-base-content"} (str "\u201C" text "\u201D"))
      "?")
    ($ :div {:class "flex flex-wrap gap-3"}
      (for [entity-type [:supplier :store :category :article]]
        (let [{:keys [icon label]} (search/entity-type-info entity-type)
              btn-class (get type-button-styles entity-type)
              disabled? (or creating?
                          ;; Stores need a supplier first
                          (and (= entity-type :store) (not has-supplier?)))]
          ($ :button {:key (name entity-type)
                      :type "button"
                      :disabled disabled?
                      :class (str "px-6 py-3.5 text-lg rounded-xl font-medium border "
                               "transition-all flex items-center gap-2 "
                               (if disabled? "opacity-40 cursor-not-allowed " "")
                               btn-class)
                      :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                  (on-pick entity-type text))}
            (if (and creating? (not disabled?))
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
              ($ :span icon))
            ($ :span label)))))
    (when (not has-supplier?)
      ($ :p {:class "text-sm text-base-content/40 mt-3 italic"}
        "Select a supplier first to create a store"))
    ($ :button {:type "button"
                :class "mt-3 text-sm text-base-content/50 hover:text-base-content/70 transition-colors"
                :on-click (fn [e] (.preventDefault e) (on-cancel))}
      "Cancel")))

;; ─────────────────────────────────────────────
;; Main component
;; ─────────────────────────────────────────────

(defui smart-expense-form
  "Progressive smart input for manual expense entry.

  Props:
  - :on-submit  fn called with prepared form data
  - :on-cancel  fn called when cancel is clicked
  - :submitting? boolean"
  [{:keys [on-submit on-cancel submitting?]}]
  (let [;; Subscriptions
        suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        expense-categories (or (use-subscribe [:user-expenses/expense-categories]) [])
        stores (or (use-subscribe [:user-expenses/stores]) [])
        articles (or (use-subscribe [:user-expenses/articles]) [])
        payers-loading? (boolean (use-subscribe [:user-expenses/payers-loading?]))

        ;; Quick search (backend)
        quick-search-results (use-subscribe [:user-expenses/quick-search-results])
        quick-search-loading? (use-subscribe [:user-expenses/quick-search-loading?])

        ;; Core state
        [phase set-phase!] (use-state :context)
        [chips set-chips!] (use-state {})
        [input-text set-input-text!] (use-state "")

        ;; Autocomplete UI
        [dropdown-open? set-dropdown-open!] (use-state false)
        [highlight-idx set-highlight-idx!] (use-state -1)
        [type-picker-text set-type-picker-text!] (use-state nil)
        [creating? set-creating!] (use-state false)

        ;; Form values
        [total-amount set-total-amount!] (use-state "")
        [currency set-currency!] (use-state "BAM")
        [purchased-at set-purchased-at!] (use-state (current-datetime-local))
        [payer-id set-payer-id!] (use-state nil)
        [notes set-notes!] (use-state "")
        [itemized? set-itemized!] (use-state false)
        [items set-items!] (use-state [(new-line-item)])
        [error set-error!] (use-state nil)
        [ready? set-ready!] (use-state false)

        input-ref (use-ref nil)

        ;; Backend results (already sorted by score)
        search-results (when (and dropdown-open? (seq quick-search-results))
                         quick-search-results)

        total-dropdown-count (+ (count (or search-results []))
                               (if (and (not (str/blank? input-text))
                                     (not (search/number-input? input-text))) 1 0))

        ;; Actions
        focus-input! (fn []
                       (when-let [el @input-ref]
                         (js/setTimeout #(.focus el) 60)))

        add-chip! (fn [entity-type id label-text entity]
                    (set-chips!
                      (fn [c]
                        (let [c* (assoc c entity-type {:id id :label label-text})
                              ;; Auto-fill supplier when store is picked
                              c* (if (and (= entity-type :store)
                                       (not (:supplier c*))
                                       entity)
                                   ;; Backend store results include supplier_id + supplier_display_name
                                   (let [sid (str (or (:supplier-id entity) (:supplier_id entity)
                                                    (:supplier-id entity)))
                                         s-name (or (:supplier-display-name entity)
                                                  (:supplier_display_name entity))
                                         ;; Try backend-provided name first, then local lookup
                                         supplier-label (or s-name
                                                          (when (not (str/blank? sid))
                                                            (some #(when (= (str (:id %)) sid)
                                                                     (or (:display-name %)
                                                                       (:display_name %) ""))
                                                              suppliers)))]
                                     (if (and (not (str/blank? sid)) supplier-label)
                                       (assoc c* :supplier
                                         {:id (js/parseInt sid 10) :label supplier-label})
                                       c*))
                                   c*)]
                          c*)))
                    (set-input-text! "")
                    (set-dropdown-open! false)
                    (set-highlight-idx! -1)
                    (set-type-picker-text! nil)
                    (rf/dispatch [:user-expenses/clear-quick-search])
                    (focus-input!))

        remove-chip! (fn [entity-type]
                       (set-chips!
                         (fn [c]
                           (let [c* (dissoc c entity-type)
                                 ;; Remove store when supplier removed
                                 c* (if (= entity-type :supplier)
                                      (dissoc c* :store)
                                      c*)]
                             c*))))

        handle-select-result (fn [result]
                               (let [etype (keyword (or (:entity-type result) (:entity_type result)))]
                                 (add-chip! etype (:id result) (:label result) result)))

        handle-create-inline (fn [text]
                               (set-type-picker-text! text)
                               (set-dropdown-open! false)
                               (set-input-text! ""))

        handle-type-pick (fn [entity-type text]
                           (let [create-event (get create-events entity-type)
                                 field-name (get create-field-names entity-type)
                                 params (cond-> {field-name text}
                                          (and (= entity-type :store)
                                            (get-in chips [:supplier :id]))
                                          (assoc :supplier_id (get-in chips [:supplier :id])))]
                             (set-creating! true)
                             (rf/dispatch
                               [create-event params
                                (fn [entity]
                                  (set-creating! false)
                                  (when (map? entity)
                                    (let [new-id (:id entity)
                                          new-label (case entity-type
                                                      :supplier (or (:display-name entity)
                                                                  (:display_name entity) text)
                                                      :store (or (:display-name entity)
                                                               (:display_name entity) text)
                                                      :category (or (:name entity) text)
                                                      :article (or (:canonical-name entity)
                                                                 (:canonical_name entity) text)
                                                      text)]
                                      (add-chip! entity-type new-id new-label entity))))])))

        handle-input-change (fn [e]
                              (let [v (.. e -target -value)]
                                (set-input-text! v)
                                (set-highlight-idx! -1)
                                (set-type-picker-text! nil)
                                (if (and (>= (count (str/trim v)) 2)
                                      (not (search/number-input? v)))
                                  (do (set-dropdown-open! true)
                                    (rf/dispatch [:user-expenses/quick-search (str/trim v)]))
                                  (do (set-dropdown-open! false)
                                    (rf/dispatch [:user-expenses/clear-quick-search])))))

        handle-input-keydown (fn [e]
                               (let [key (.-key e)]
                                 (cond
                                   (= key "Enter")
                                   (do (.preventDefault e)
                                     (let [text (str/trim input-text)]
                                       (cond
                                         ;; Number → set total, transition to review
                                         (search/number-input? text)
                                         (let [amount (search/parse-amount text)]
                                           (set-total-amount! (format-decimal amount))
                                           (set-input-text! "")
                                           (set-dropdown-open! false)
                                           (set-phase! :review))

                                         ;; Highlighted suggestion → select it
                                         (and dropdown-open?
                                           (>= highlight-idx 0)
                                           (< highlight-idx (count (or search-results []))))
                                         (handle-select-result (nth search-results highlight-idx))

                                         ;; Highlighted "Create" row
                                         (and dropdown-open?
                                           (= highlight-idx (count (or search-results [])))
                                           (not (str/blank? text)))
                                         (handle-create-inline text)

                                         ;; No highlight, has text → create flow
                                         (not (str/blank? text))
                                         (handle-create-inline text))))

                                   (= key "ArrowDown")
                                   (do (.preventDefault e)
                                     (when dropdown-open?
                                       (set-highlight-idx!
                                         (fn [i] (min (dec total-dropdown-count) (inc i))))))

                                   (= key "ArrowUp")
                                   (do (.preventDefault e)
                                     (when dropdown-open?
                                       (set-highlight-idx! (fn [i] (max -1 (dec i))))))

                                   (= key "Escape")
                                   (do (.preventDefault e)
                                     (set-dropdown-open! false)
                                     (set-highlight-idx! -1)
                                     (set-type-picker-text! nil))

                                   ;; Backspace on empty → remove last chip
                                   (and (= key "Backspace") (str/blank? input-text) (seq chips))
                                   (let [last-type (last (keys chips))]
                                     (remove-chip! last-type)))))

        handle-submit (fn [e]
                        (.preventDefault e)
                        (let [validation (validate-form
                                           {:chips chips
                                            :total-amount total-amount
                                            :payer-id payer-id
                                            :itemized? itemized?
                                            :items items})]
                          (if (:ok? validation)
                            (do (set-error! nil)
                              (when (fn? on-submit)
                                (on-submit (prepare-submit-values
                                             {:chips chips
                                              :total-amount total-amount
                                              :currency currency
                                              :purchased-at purchased-at
                                              :payer-id payer-id
                                              :notes notes
                                              :itemized? itemized?
                                              :items items}))))
                            (set-error! (:error validation)))))

        ;; Computed totals for itemized mode
        prepared-items (when itemized? (prepare-items items))
        computed-total (when itemized? (line-items-total (or prepared-items [])))]

    ;; Load reference data
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-stores {:limit 200 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-articles {:limit 200 :offset 0}])
        ;; Cleanup quick search on unmount
        (fn [] (rf/dispatch [:user-expenses/clear-quick-search])))
      [])

    ;; Set default payer once loaded
    (use-effect
      (fn []
        (when (and (not ready?)
                (or (seq payers) (not payers-loading?)))
          (set-ready! true)
          (set-payer-id! (some-> (payer-default-id payers) str)))
        js/undefined)
      [payers payers-loading? ready?])

    ;; Close dropdown on outside click
    (use-effect
      (fn []
        (let [handler (fn [_]
                        (set-dropdown-open! false)
                        (set-highlight-idx! -1))]
          (.addEventListener js/document "click" handler)
          (fn [] (.removeEventListener js/document "click" handler))))
      [])

    (if-not ready?
      ;; Loading state
      ($ :div {:class "flex flex-col items-center justify-center p-16 gap-4"}
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
        ($ :p {:class "text-base-content/50 text-lg"} "Loading..."))

      ;; Form
      ($ :form {:id "smart-expense-form"
                :on-submit handle-submit
                :class "space-y-6"}

        ;; Error banner
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-base flex items-center justify-between"}
            ($ :span error)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-sm"
                        :on-click #(set-error! nil)}
              "\u00D7")))

        ;; ═══════════════════════════════════════
        ;; PHASE 1: Context Collection
        ;; ═══════════════════════════════════════
        (when (= phase :context)
          ($ :div {:class "space-y-4"}

            ;; Welcome prompt
            (when (empty? chips)
              ($ :div {:class "text-center py-2"}
                ($ :p {:class "text-2xl font-semibold text-base-content/80 mb-1"}
                  "New Expense")
                ($ :p {:class "text-base text-base-content/50"}
                  "Start typing to search or create")))

            ;; Chips row
            (when (seq chips)
              ($ :div {:class "flex flex-wrap gap-2"}
                (for [[entity-type chip] chips]
                  ($ entity-chip {:key (name entity-type)
                                  :entity-type entity-type
                                  :label (:label chip)
                                  :on-remove #(remove-chip! entity-type)}))))

            ;; The BIG input
            ($ :div {:class "relative"
                     :on-click (fn [e] (.stopPropagation e))}
              ($ :input {:ref input-ref
                         :id "smart-expense-input"
                         :type "text"
                         :auto-focus true
                         :class (str "w-full text-xl sm:text-2xl p-5 sm:p-6 rounded-2xl "
                                  "border-2 border-base-300 "
                                  "focus:border-primary focus:outline-none focus:shadow-lg "
                                  "focus:shadow-primary/10 "
                                  "transition-all bg-white placeholder:text-base-content/30")
                         :placeholder (if (seq chips)
                                        "Add more, or type amount..."
                                        "Supplier, store, article, or amount...")
                         :value input-text
                         :on-change handle-input-change
                         :on-key-down handle-input-keydown
                         :auto-complete "off"})

              ;; Autocomplete dropdown
              (when dropdown-open?
                ($ autocomplete-dropdown
                  {:results (or search-results [])
                   :loading? quick-search-loading?
                   :highlight-idx highlight-idx
                   :on-select handle-select-result
                   :on-create handle-create-inline
                   :input-text (str/trim input-text)})))

            ;; Type picker
            (when type-picker-text
              ($ type-picker
                {:text type-picker-text
                 :on-pick handle-type-pick
                 :on-cancel #(set-type-picker-text! nil)
                 :creating? creating?
                 :has-supplier? (boolean (:supplier chips))}))

            ;; Amount already set indicator + Review button
            (when (and (seq chips) (not (str/blank? total-amount)))
              ($ :div {:class "mt-4 text-center"}
                ($ :p {:class "text-base text-base-content/50 mb-3"}
                  "Amount: "
                  ($ :span {:class "font-bold text-base-content"} total-amount " " currency))
                ($ :button {:type "button"
                            :class (str "ds-btn ds-btn-primary ds-btn-lg text-lg px-10 "
                                     "rounded-xl")
                            :on-click #(set-phase! :review)}
                  "Continue to Review")))

            ;; Hint text
            (when (nil? type-picker-text)
              ($ :p {:class "text-center text-sm text-base-content/35 mt-3"}
                (cond
                  (and (seq chips) (str/blank? total-amount))
                  "Enter a number to set the total"

                  (seq chips)
                  "Keep adding context or review your expense"

                  :else
                  "Search for existing items or type a new name")))))

        ;; ═══════════════════════════════════════
        ;; PHASE 2: Review
        ;; ═══════════════════════════════════════
        (when (= phase :review)
          ($ :div {:class "space-y-6"}

            ;; Chips summary + Add more
            ($ :div {:class "flex flex-wrap items-center gap-2"}
              (for [[entity-type chip] chips]
                ($ entity-chip {:key (name entity-type)
                                :entity-type entity-type
                                :label (:label chip)
                                :size :sm
                                :on-remove (fn []
                                             (remove-chip! entity-type)
                                             (when (empty? (dissoc chips entity-type))
                                               (set-phase! :context)))}))
              ($ :button {:type "button"
                          :class (str "inline-flex items-center gap-1.5 px-4 py-2 rounded-full "
                                   "text-sm font-medium border border-dashed border-base-300 "
                                   "text-base-content/40 hover:text-base-content/60 "
                                   "hover:border-base-content/30 transition-colors")
                          :on-click (fn [e] (.preventDefault e)
                                      (set-phase! :context)
                                      (focus-input!))}
                "+" " Add more"))

            ;; Big amount + currency
            ($ :div {:class "text-center space-y-2"}
              ($ :label {:class "text-base text-base-content/50 block"} "Total Amount")
              ($ :div {:class "flex items-center justify-center gap-3"}
                (if itemized?
                  ;; Read-only computed total
                  ($ :div {:class (str "text-4xl sm:text-5xl font-bold text-center p-4 w-72 h-20 "
                                    "rounded-2xl border-2 border-base-200 bg-base-100 "
                                    "flex items-center justify-center text-base-content/70")}
                    (or (some-> computed-total format-decimal) "0.00"))
                  ;; Editable total
                  ($ :input {:id "smart-expense-total"
                             :type "text"
                             :inputMode "decimal"
                             :auto-focus true
                             :class (str "text-4xl sm:text-5xl font-bold text-center p-4 "
                                      "w-72 h-20 rounded-2xl border-2 border-base-300 "
                                      "focus:border-primary focus:outline-none "
                                      "focus:shadow-lg focus:shadow-primary/10 transition-all")
                             :value total-amount
                             :placeholder "0.00"
                             :on-change (fn [e] (set-total-amount! (.. e -target -value)))}))
                ($ :select {:class (str "text-xl p-3 h-14 rounded-xl border-2 border-base-300 "
                                     "bg-white focus:border-primary cursor-pointer")
                            :value currency
                            :on-change (fn [e] (set-currency! (.. e -target -value)))}
                  (for [{:keys [value label]} currency-options]
                    ($ :option {:key value :value value} label)))))

            ;; Payer + Date row
            ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-4"}
              ;; Payer
              ($ :div
                ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Payer")
                ($ :select {:id "smart-expense-payer"
                            :class (str "w-full text-lg p-4 h-14 rounded-xl border-2 "
                                     "border-base-300 bg-white focus:border-primary "
                                     "cursor-pointer")
                            :value (or payer-id "")
                            :on-change (fn [e] (set-payer-id! (.. e -target -value)))}
                  ($ :option {:value ""} "Select payer...")
                  (for [p payers]
                    ($ :option {:key (:id p) :value (:id p)}
                      (str (:label p)
                        (when-let [t (or (:type p) (:payer-type-label p))]
                          (str " (" t ")")))))))
              ;; Date
              ($ :div
                ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Date")
                ($ :input {:id "smart-expense-date"
                           :type "datetime-local"
                           :class (str "w-full text-lg p-4 h-14 rounded-xl border-2 "
                                    "border-base-300 bg-white focus:border-primary")
                           :value purchased-at
                           :on-change (fn [e] (set-purchased-at! (.. e -target -value)))})))

            ;; Notes
            ($ :div
              ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Notes")
              ($ :textarea {:id "smart-expense-notes"
                            :class (str "w-full text-lg p-4 rounded-xl border-2 "
                                     "border-base-300 bg-white focus:border-primary "
                                     "resize-none")
                            :rows 2
                            :placeholder "Optional notes..."
                            :value (or notes "")
                            :on-change (fn [e] (set-notes! (.. e -target -value)))}))

            ;; Itemized toggle
            ($ :div {:class "flex items-center gap-3"}
              ($ :input {:id "smart-expense-itemized"
                         :type "checkbox"
                         :class "ds-toggle ds-toggle-primary ds-toggle-lg"
                         :checked itemized?
                         :on-change (fn [_]
                                      (let [has-data? (and itemized?
                                                        (some #(not (str/blank? (str (:raw_label %))))
                                                          items))]
                                        (when (or (not has-data?)
                                                (js/confirm "Clear line items?"))
                                          (if itemized?
                                            (do (set-itemized! false)
                                              (set-items! [(new-line-item)]))
                                            (set-itemized! true)))))})
              ($ :label {:for "smart-expense-itemized"
                         :class "text-lg cursor-pointer select-none"}
                "Add line items"))

            ;; Line items
            (when itemized?
              ($ :div {:class "space-y-3"}
                ($ :div {:class "border-2 border-base-200 rounded-2xl p-4"}
                  ($ line-items-input
                    {:id "smart-expense-items"
                     :value items
                     :columns line-item-columns
                     :style {:maxHeight "300px"}
                     :overflow-y-class "overflow-y-auto"
                     :scrollbar-gutter-stable? true
                     :on-change set-items!}))
                (when (and computed-total (pos? computed-total))
                  ($ :div {:class "text-right text-lg"}
                    ($ :span {:class "text-base-content/50"} "Items total: ")
                    ($ :span {:class "font-bold"} (format-decimal computed-total) " " currency)))))

            ;; Actions
            ($ :div {:class "flex justify-end gap-3 pt-4"}
              (when on-cancel
                ($ :button {:id "btn-cancel-smart-expense"
                            :type "button"
                            :class "ds-btn ds-btn-lg text-lg"
                            :disabled submitting?
                            :on-click (fn [e] (.preventDefault e) (on-cancel))}
                  "Cancel"))
              ($ :button {:id "btn-save-smart-expense"
                          :type "submit"
                          :class "ds-btn ds-btn-primary ds-btn-lg text-lg px-10"
                          :disabled (or submitting?
                                      (not (:ok? (validate-form
                                                   {:chips chips
                                                    :total-amount total-amount
                                                    :payer-id payer-id
                                                    :itemized? itemized?
                                                    :items items}))))}
                (if submitting? "Saving..." "Save Expense")))))))))

