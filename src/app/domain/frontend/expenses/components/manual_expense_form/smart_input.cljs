(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input
  "Progressive smart input for manual expense entry.

  Phase 1 (Items): Search articles, each gets inline qty/price with defaults.
  Enter adds the next item. Enter on empty finishes items entry.

  Phase 2 (Context): Items summary, search for store/supplier/category,
  payer/date/currency/notes. Submit."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [current-datetime-local format-decimal safe-parse-number]]
    [app.domain.frontend.expenses.components.manual-expense-form.search :as search]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.components
     :refer [autocomplete-dropdown build-quick-pick-groups entity-chip
             item-row phase-two-quick-pick-groups quick-picks type-picker]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants
     :refer [create-events create-field-names currency-options]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
     :refer [compute-items-total current-related-context entity-type-label
             focused-search-types payer-default-id prepare-submit-values
             search-placeholder validate-form]]
    [app.template.frontend.i18n :refer [use-t]]
    app.domain.frontend.expenses.events.user-expenses.quick-add-search
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref use-state]]
    [uix.re-frame :refer [use-subscribe]]))

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
  (let [t (use-t)
        ;; Subscriptions
        suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        stores (or (use-subscribe [:user-expenses/stores]) [])
        expense-categories (or (use-subscribe [:user-expenses/expense-categories]) [])
        articles (or (use-subscribe [:user-expenses/articles]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        payers-loading? (boolean (use-subscribe [:user-expenses/payers-loading?]))

        ;; Quick Add search (backend)
        quick-search-results (use-subscribe [:user-expenses/quick-add-search-results :all])
        quick-search-loading? (use-subscribe [:user-expenses/quick-add-search-loading? :all])
        quick-add-related (use-subscribe [:user-expenses/quick-add-related])
        cooccurring-articles (use-subscribe [:user-expenses/cooccurring-articles])
        context-suggestions (use-subscribe [:user-expenses/context-suggestions])

        ;; Core state
        [phase set-phase!] (use-state :items)       ;; :items or :context
        [items set-items!] (use-state [])            ;; [{:id :article-id :label :qty :unit-price}]
        [context set-context!] (use-state {})        ;; {:supplier {:id :label} :store ... :category ...}
        [input-text set-input-text!] (use-state "")
        [article-mode? set-article-mode!] (use-state false) ;; narrows search to articles after first pick

        ;; Autocomplete UI
        [dropdown-open? set-dropdown-open!] (use-state false)
        [highlight-idx set-highlight-idx!] (use-state -1)
        [type-picker-text set-type-picker-text!] (use-state nil)
        [creating? set-creating!] (use-state false)

        ;; Form values (Phase 2)
        [currency set-currency!] (use-state "BAM")
        [purchased-at set-purchased-at!] (use-state (current-datetime-local))
        [payer-id set-payer-id!] (use-state nil)
        [notes set-notes!] (use-state "")
        [error set-error!] (use-state nil)
        [ready? set-ready!] (use-state false)

        input-ref (use-ref nil)

        ;; Search results: show immediate local matches while dedicated backend search loads.
        available-search-types (focused-search-types context article-mode?)
        local-search-results (when (and dropdown-open?
                                     (>= (count (str/trim input-text)) 2))
                               (-> (search/search-all-entities
                                     input-text
                                     {:suppliers suppliers
                                      :stores stores
                                      :categories expense-categories
                                      :articles articles}
                                     {:selected-supplier-id (some-> context :supplier :id)})
                                 (search/filter-results-by-entity-types available-search-types)))
        filtered-quick-search-results (search/filter-results-by-entity-types quick-search-results available-search-types)
        search-results (when dropdown-open?
                         (search/merge-search-results local-search-results filtered-quick-search-results 10))
        selected-supplier-id (some-> context :supplier :id)
        related-context (current-related-context context)
        related-matches? (and related-context
                           (= (:entity-type quick-add-related) (:entity-type related-context))
                           (= (:entity-id quick-add-related) (some-> (:entity-id related-context) str)))
        related-stores (if (and related-matches? (= :supplier (:entity-type related-context)))
                         (get-in quick-add-related [:related :stores] stores)
                         stores)
        related-articles (if related-matches?
                           (or (get-in quick-add-related [:related :articles]) articles)
                           articles)
        ;; Co-occurring article suggestions (article-mode) or normal quick picks
        focused-quick-pick-groups (when (and (str/blank? input-text)
                                          (not article-mode?)
                                          (< (count available-search-types) 4))
                                    (build-quick-pick-groups
                                      available-search-types
                                      suppliers
                                      related-stores
                                      expense-categories
                                      related-articles
                                      selected-supplier-id))
        cooccurring-pick-items (when (and article-mode?
                                       (str/blank? input-text)
                                       (seq cooccurring-articles))
                                 (->> cooccurring-articles
                                   (mapv (fn [a]
                                           {:id (or (:id a) "")
                                            :label (or (:label a) "")
                                            :entity-type :article
                                            :last-price (:last_price a)
                                            :last-price-source (:last_price_source a)
                                            :entity a}))))

        total-dropdown-count (+ (count (or search-results []))
                               (if (and (not (str/blank? input-text))
                                     (not (search/number-input? input-text))) 1 0))

        ;; Computed totals
        items-total (compute-items-total items)

        ;; Actions
        focus-input! (fn []
                       (when-let [el @input-ref]
                         (js/setTimeout #(.focus el) 60)))

        add-item! (fn [article-id label-text last-price]
                    (let [new-item {:id (str (random-uuid))
                                    :article-id article-id
                                    :label label-text
                                    :qty "1"
                                    :unit-price (if last-price (format-decimal last-price) "")}
                          next-items (conj (or items []) new-item)
                          all-article-ids (->> next-items
                                            (keep :article-id)
                                            vec)]
                      (set-items! (fn [prev] (conj prev new-item)))
                      (set-input-text! "")
                      (set-dropdown-open! false)
                      (set-highlight-idx! -1)
                      (set-type-picker-text! nil)
                      (set-article-mode! true)
                      (rf/dispatch [:user-expenses/clear-quick-add-search :all])
                      (rf/dispatch [:user-expenses/fetch-cooccurring-articles
                                    all-article-ids selected-supplier-id])))

        remove-item! (fn [item-id]
                       (set-items! (fn [prev]
                                     (let [next-items (vec (remove #(= item-id (:id %)) prev))]
                                       (when (empty? next-items)
                                         (set-article-mode! false)
                                         (rf/dispatch [:user-expenses/clear-cooccurring-articles]))
                                       next-items))))

        update-item! (fn [item-id field value]
                       (set-items!
                         (fn [prev]
                           (mapv (fn [item]
                                   (if (= item-id (:id item))
                                     (assoc item field value)
                                     item))
                             prev))))

        add-context! (fn [entity-type id label-text entity]
                       (set-context!
                         (fn [c]
                           (let [base-chip {:id id :label label-text}
                                 supplier-chip (when-let [sid (or (:supplier-id entity) (:supplier_id entity))]
                                                 {:id sid
                                                  :label (or (:supplier-display-name entity)
                                                           (:supplier_display_name entity)
                                                           (some #(when (= (str (:id %)) (str sid))
                                                                    (or (:display-name %)
                                                                      (:display_name %) ""))
                                                             suppliers))})]
                             (case entity-type
                               :supplier
                               (let [c* (assoc c :supplier base-chip)]
                                 (if (and (:store c*)
                                       (not= (str (or (get-in c* [:store :supplier-id])
                                                    (get-in c* [:store :supplier_id])))
                                         (str id)))
                                   (dissoc c* :store)
                                   c*))

                               :store
                               (cond-> (assoc c :store (assoc base-chip
                                                         :supplier-id (or (:supplier-id entity) (:supplier_id entity))
                                                         :supplier-display-name (or (:supplier-display-name entity)
                                                                                  (:supplier_display_name entity))))
                                 supplier-chip (assoc :supplier supplier-chip))

                               (assoc c entity-type base-chip)))))
                       (set-input-text! "")
                       (set-dropdown-open! false)
                       (set-highlight-idx! -1)
                       (set-type-picker-text! nil)
                       (rf/dispatch [:user-expenses/clear-quick-add-search :all])
                       (focus-input!))

        remove-context! (fn [entity-type]
                          (set-context!
                            (fn [c]
                              (let [c* (dissoc c entity-type)
                                    c* (if (= entity-type :supplier) (dissoc c* :store) c*)]
                                c*))))

        handle-select-result (fn [result]
                               (let [etype (keyword (or (:entity-type result) (:entity_type result)))
                                     ;; Merge inner :entity fields to top level so add-context!
                                     ;; can read supplier_id, supplier_display_name etc.
                                     entity (merge (:entity result) result)]
                                 (if (= etype :article)
                                   (add-item! (:id result) (:label result)
                                     (or (:last-price result) (:last_price result)))
                                   (add-context! etype (:id result) (:label result) entity))))

        handle-create-inline (fn [text]
                               (set-type-picker-text! text)
                               (set-dropdown-open! false)
                               (set-input-text! ""))

        handle-type-pick (fn [entity-type text]
                           (let [create-event (get create-events entity-type)
                                 field-name (get create-field-names entity-type)
                                 params (cond-> {field-name text}
                                          (and (= entity-type :store)
                                            (get-in context [:supplier :id]))
                                          (assoc :supplier_id (get-in context [:supplier :id])))]
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
                                      (if (= entity-type :article)
                                        (add-item! new-id new-label nil)
                                        (add-context! entity-type new-id new-label entity)))))])))

        handle-input-change (fn [e]
                              (let [v (.. e -target -value)
                                    supplier-id (some-> context :supplier :id)]
                                (set-input-text! v)
                                (set-highlight-idx! -1)
                                (set-type-picker-text! nil)
                                (if (and (>= (count (str/trim v)) 2)
                                      (not (search/number-input? v)))
                                  (do (set-dropdown-open! true)
                                    (rf/dispatch [:user-expenses/quick-add-search :all (str/trim v)
                                                  {:supplier_id supplier-id}]))
                                  (do (set-dropdown-open! false)
                                    (rf/dispatch [:user-expenses/clear-quick-add-search :all])))))

        handle-input-keydown (fn [e]
                               (let [key (.-key e)]
                                 (cond
                                   (= key "Enter")
                                   (do (.preventDefault e)
                                     (let [text (str/trim input-text)]
                                       (cond
                                         ;; Empty input + has items → transition to context phase
                                         (and (str/blank? text) (= phase :items) (seq items))
                                         (let [article-ids (->> items (keep :article-id) vec)]
                                           (set-article-mode! false)
                                           (set-dropdown-open! false)
                                           (set-phase! :context)
                                           (rf/dispatch [:user-expenses/clear-cooccurring-articles])
                                           (when (seq article-ids)
                                             (rf/dispatch [:user-expenses/fetch-context-suggestions article-ids]))
                                           (js/setTimeout
                                             (fn [] (when-let [el @input-ref] (.focus el)))
                                             100))

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

                                   ;; Backspace on empty → remove last item (items phase) or context chip
                                   (and (= key "Backspace") (str/blank? input-text))
                                   (cond
                                     (and (= phase :items) (seq items))
                                     (set-items! (fn [prev]
                                                   (let [next-items (vec (butlast prev))]
                                                     (when (empty? next-items)
                                                       (set-article-mode! false)
                                                       (rf/dispatch [:user-expenses/clear-cooccurring-articles]))
                                                     next-items)))

                                     (and (= phase :context) (seq context))
                                     (let [last-type (last (keys context))]
                                       (remove-context! last-type))))))

        handle-submit (fn [e]
                        (.preventDefault e)
                        (let [validation (validate-form t
                                           {:items items
                                            :context context
                                            :payer-id payer-id})]
                          (if (:ok? validation)
                            (do (set-error! nil)
                              (when (fn? on-submit)
                                (on-submit (prepare-submit-values
                                             {:items items
                                              :context context
                                              :currency currency
                                              :purchased-at purchased-at
                                              :payer-id payer-id
                                              :notes notes}))))
                            (set-error! (:error validation)))))]

    ;; Load reference data
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-stores {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-articles {:limit 200 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}])
        ;; Cleanup quick add search on unmount
        (fn []
          (rf/dispatch [:user-expenses/clear-quick-add-search :all])
          (rf/dispatch [:user-expenses/clear-quick-add-related])
          (rf/dispatch [:user-expenses/clear-cooccurring-articles])
          (rf/dispatch [:user-expenses/clear-context-suggestions])))
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

    ;; Load related records for focused quick picks when context narrows the search.
    (use-effect
      (fn []
        (let [limit (if (= 1 (count available-search-types)) 10 5)]
          (if (and related-context (< (count available-search-types) 4))
            (rf/dispatch [:user-expenses/fetch-quick-add-related
                          (:entity-type related-context)
                          (:entity-id related-context)
                          limit])
            (rf/dispatch [:user-expenses/clear-quick-add-related])))
        js/undefined)
      [available-search-types related-context (count available-search-types)])

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
        ($ :p {:class "text-base-content/50 text-lg"} (t :smart-expense/loading)))

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
        ;; PHASE 1: Items Entry
        ;; ═══════════════════════════════════════
        (when (= phase :items)
          ($ :div {:class "space-y-4"}

            ;; Welcome prompt
            (when (and (empty? items) (empty? context))
              ($ :div {:class "text-center py-2"}
                ($ :p {:class "text-2xl font-semibold text-base-content/80 mb-1"}
                  (t :smart-expense/title))
                ($ :p {:class "text-base text-base-content/50"}
                  (t :smart-expense/subtitle))))

            ;; Selected context chips
            (when (seq context)
              ($ :div {:class "flex flex-wrap gap-2"}
                (for [[entity-type chip] context]
                  ($ entity-chip {:key (str "items-phase-" (name entity-type))
                                  :entity-type entity-type
                                  :label (:label chip)
                                  :on-remove #(remove-context! entity-type)
                                  :size :sm}))))

            ;; Items list
            (when (seq items)
              ($ :div {:class "space-y-2"}
                (for [item items]
                  ($ item-row {:key (:id item)
                               :item item
                               :on-change update-item!
                               :on-remove remove-item!
                               :on-enter-price focus-input!}))))

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
                         :on-change handle-input-change
                         :on-key-down handle-input-keydown
                         :auto-complete "off"})

              ;; Autocomplete dropdown (portal)
              (when dropdown-open?
                ($ autocomplete-dropdown
                  {:t t
                   :results (or search-results [])
                   :loading? (and quick-search-loading? (empty? search-results))
                   :highlight-idx highlight-idx
                   :on-select handle-select-result
                   :on-create handle-create-inline
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
                   :on-select handle-select-result})))

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
                       :on-select handle-select-result})))))

            ;; Type picker
            (when type-picker-text
              ($ type-picker
                {:t t
                 :text type-picker-text
                 :on-pick handle-type-pick
                 :on-cancel #(set-type-picker-text! nil)
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

        ;; ═══════════════════════════════════════
        ;; PHASE 2: Context + Review
        ;; ═══════════════════════════════════════
        (when (= phase :context)
          ($ :div {:class "space-y-6"}

            ;; Back to items link
            ($ :button {:type "button"
                        :class "text-sm text-primary hover:text-primary/80 transition-colors"
                        :on-click (fn [e] (.preventDefault e)
                                    (set-phase! :items)
                                    (focus-input!))}
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
                        (str q " \u00D7 " (format-decimal p))))))))

            ;; Context chips (already selected)
            (when (seq context)
              ($ :div {:class "flex flex-wrap gap-2"}
                (for [[entity-type chip] context]
                  ($ entity-chip {:key (name entity-type)
                                  :entity-type entity-type
                                  :label (:label chip)
                                  :on-remove #(remove-context! entity-type)}))))

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
                      quick-pick-groups (when (str/blank? input-text)
                                          (phase-two-quick-pick-groups missing
                                            context-suggestions
                                            suppliers
                                            stores
                                            expense-categories
                                            articles
                                            selected-supplier-id))
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
                              {:t t
                               :entity-type entity-type
                               :items items
                               :on-select handle-select-result})))))

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
                                 :on-change handle-input-change
                                 :on-key-down handle-input-keydown
                                 :auto-complete "off"})

                      ;; Autocomplete dropdown (portal) — filtered to missing types only
                      (when dropdown-open?
                        ($ autocomplete-dropdown
                          {:t t
                           :results filtered-results
                           :loading? (and quick-search-loading? (empty? filtered-results))
                           :highlight-idx highlight-idx
                           :on-select handle-select-result
                           :on-create handle-create-inline
                           :input-text (str/trim input-text)
                           :anchor-ref input-ref})))

                    ;; Type picker — only missing context types
                    (when type-picker-text
                      ($ type-picker
                        {:t t
                         :text type-picker-text
                         :on-pick handle-type-pick
                         :on-cancel #(set-type-picker-text! nil)
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
                                            (set-payer-id! (str (:id p))))}
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
                            :on-change (fn [e] (set-payer-id! (.. e -target -value)))}
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
                           :on-change (fn [e] (set-purchased-at! (.. e -target -value)))}))
              ;; Currency
              ($ :div
                ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} (t :smart-expense/currency-label))
                ($ :select {:class (str "w-full text-lg p-4 h-14 rounded-xl border-2 border-base-300 "
                                     "bg-white focus:border-primary cursor-pointer")
                            :value currency
                            :on-change (fn [e] (set-currency! (.. e -target -value)))}
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
                            :on-change (fn [e] (set-notes! (.. e -target -value)))}))

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
                          :disabled (or submitting?
                                      (not (:ok? (validate-form t
                                                   {:items items
                                                    :context context
                                                    :payer-id payer-id}))))}
                (if submitting? (t :smart-expense/saving) (t :smart-expense/save))))))))))
