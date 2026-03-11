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
    app.domain.frontend.expenses.events.user-expenses.quick-search
    [clojure.string :as str]
    [re-frame.core :as rf]
    ["react-dom" :as react-dom]
    [uix.core :refer [$ defui use-effect use-ref use-state]]
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

;; ─────────────────────────────────────────────
;; Helpers
;; ─────────────────────────────────────────────

(defn- payer-default-id
  [payers]
  (or (some #(when (or (:is-default %) (:isDefault %)) (:id %)) payers)
    (:id (first payers))))

(defn- compute-items-total
  "Sum line totals for all items."
  [items]
  (reduce (fn [acc item]
            (let [qty (or (safe-parse-number (:qty item)) 1)
                  price (or (safe-parse-number (:unit-price item)) 0)]
              (+ acc (* qty price))))
    0 items))

(defn- prepare-submit-items
  "Convert internal items to backend-expected format."
  [items]
  (vec
    (keep (fn [{:keys [label qty unit-price]}]
            (let [q (or (safe-parse-number qty) 1)
                  p (or (safe-parse-number unit-price) 0)
                  total (* q p)]
              (when (and (not (str/blank? (str label))) (pos? total))
                {:raw_label (str label)
                 :qty q
                 :unit_price p
                 :line_total total})))
      items)))

(defn- prepare-submit-values
  [{:keys [items context currency purchased-at payer-id notes]}]
  (let [prepared (prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond-> {:payer_id payer-id
             :purchased_at purchased-at
             :currency currency
             :total_amount total
             :items prepared}
      (:supplier context) (assoc :supplier_id (get-in context [:supplier :id]))
      (:store context) (assoc :store_id (get-in context [:store :id]))
      (:category context) (assoc :expense_category_id (get-in context [:category :id]))
      (not (str/blank? notes)) (assoc :notes notes))))

(defn- validate-form
  [{:keys [items context payer-id]}]
  (let [prepared (prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond
      (empty? prepared)
      {:ok? false :error "Add at least one item with a price."}

      (empty? context)
      {:ok? false :error "Select at least one: supplier, store, or category."}

      (str/blank? (str payer-id))
      {:ok? false :error "Payer is required."}

      (<= total 0)
      {:ok? false :error "Total must be greater than 0."}

      :else {:ok? true})))

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
  [{:keys [results loading? highlight-idx on-select on-create input-text anchor-ref]}]
  (let [visible-results (take 10 results)
        [pos set-pos!] (use-state nil)]

    ;; Measure anchor position whenever dropdown content changes
    (use-effect
      (fn []
        (when-let [el (and anchor-ref @anchor-ref)]
          (let [rect (.getBoundingClientRect el)]
            (set-pos! {:top (+ (.-bottom rect) 8)
                       :left (.-left rect)
                       :width (.-width rect)})))
        js/undefined)
      [anchor-ref (count visible-results) loading? input-text])

    (when (and pos
            (or (seq visible-results) loading? (and input-text (not (str/blank? input-text)))))
      (when-let [portal-target (.-body js/document)]
        (react-dom/createPortal
          ($ :div {:class (str "bg-white rounded-2xl shadow-2xl border border-base-200 "
                            "max-h-[600px] overflow-y-auto")
                   :style {:position "fixed"
                           :z-index 99999
                           :top (str (:top pos) "px")
                           :left (str (:left pos) "px")
                           :width (str (:width pos) "px")}
                   :on-click (fn [e] (.stopPropagation e))}
            ;; Loading indicator
            (when loading?
              ($ :div {:class "flex items-center gap-3 px-5 py-4 text-base-content/40"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                ($ :span {:class "text-base"} "Searching...")))

            ;; Search results (capped at 10)
            (when (seq visible-results)
              (for [[idx result] (map-indexed vector visible-results)]
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
                    (when-let [price (:last_price result)]
                      ($ :span {:class "text-sm text-base-content/50 font-mono"}
                        (format-decimal price)))
                    ($ :span {:class (str "text-xs px-2.5 py-1 rounded-full border font-medium "
                                       (get chip-styles etype ""))}
                      label)))))

            ;; "Create new" option
            (when (and input-text (not (str/blank? input-text)))
              ($ :button {:type "button"
                          :class (str "w-full text-left px-5 py-4 flex items-center gap-4 "
                                   "border-t border-base-200 transition-colors cursor-pointer "
                                   (if (= highlight-idx (count visible-results))
                                     "bg-primary/10"
                                     "hover:bg-base-100"))
                          :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                      (on-create input-text))}
                ($ :span {:class "text-2xl flex-none w-8 text-center text-primary"} "+")
                ($ :span {:class "text-lg"}
                  "Create "
                  ($ :span {:class "font-bold"} (str "\u201C" input-text "\u201D"))))))
          portal-target)))))

(defui type-picker
  "Picker shown when user types a name that doesn't match existing entities.
   `allowed-types` optionally restricts which entity types are shown."
  [{:keys [text on-pick on-cancel creating? allowed-types]}]
  ($ :div {:class "mt-4 p-6 bg-base-100 rounded-2xl border border-base-200"}
    ($ :p {:class "text-lg text-base-content/70 mb-4"}
      "What is "
      ($ :span {:class "font-bold text-base-content"} (str "\u201C" text "\u201D"))
      "?")
    ($ :div {:class "flex flex-wrap gap-3"}
      (for [entity-type (if (seq allowed-types)
                          allowed-types
                          [:article :supplier :store :category])]
        (let [{:keys [icon label]} (search/entity-type-info entity-type)
              btn-class (get type-button-styles entity-type)]
          ($ :button {:key (name entity-type)
                      :type "button"
                      :disabled creating?
                      :class (str "px-6 py-3.5 text-lg rounded-xl font-medium border "
                               "transition-all flex items-center gap-2 "
                               (when creating? "opacity-40 cursor-not-allowed ")
                               btn-class)
                      :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                  (on-pick entity-type text))}
            (if creating?
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
              ($ :span icon))
            ($ :span label)))))
    ($ :button {:type "button"
                :class "mt-3 text-sm text-base-content/50 hover:text-base-content/70 transition-colors"
                :on-click (fn [e] (.preventDefault e) (on-cancel))}
      "Cancel")))

(defn- top-items-for-type
  "Return up to `n` items for the given entity type from local data."
  [entity-type suppliers stores expense-categories n]
  (let [name-fn (get-in search/entity-type-config [entity-type :name-fn])]
    (->> (case entity-type
           :supplier suppliers
           :store stores
           :category expense-categories
           [])
      (take n)
      (mapv (fn [item]
              {:id (:id item)
               :label (or (when name-fn (name-fn item)) "")
               :entity-type entity-type
               :entity item})))))

(defui quick-picks
  "Shows top N options as clickable chips when only one entity type is missing."
  [{:keys [entity-type items on-select]}]
  (let [{:keys [icon]} (search/entity-type-info entity-type)
        style (get chip-styles entity-type "bg-base-200 text-base-content")]
    (when (seq items)
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [{:keys [id label] :as item} items]
          ($ :button {:key (str (name entity-type) "-" id)
                      :type "button"
                      :class (str "inline-flex items-center gap-2 px-4 py-2.5 rounded-full "
                               "text-base font-medium border cursor-pointer "
                               "transition-all hover:shadow-md hover:scale-[1.02] "
                               style)
                      :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                  (on-select item))}
            ($ :span icon)
            ($ :span {:class "truncate max-w-[200px]"} label)))))))

(defui item-row
  "A single item row with inline qty and price inputs."
  [{:keys [item on-change on-remove on-enter-price]}]
  (let [{:keys [id label qty unit-price]} item
        qty-ref (use-ref nil)
        price-ref (use-ref nil)
        line-total (* (or (safe-parse-number qty) 1)
                     (or (safe-parse-number unit-price) 0))]
    ($ :div {:class (str "flex items-center gap-3 py-3 px-4 bg-white rounded-xl "
                      "border border-base-200 group")}
      ;; Article icon + name
      ($ :span {:class "text-xl flex-none"} "📦")
      ($ :span {:class "flex-1 text-base font-medium truncate min-w-0"} label)

      ;; Qty input
      ($ :div {:class "flex items-center gap-1.5"}
        ($ :label {:class "text-xs text-base-content/40"} "×")
        ($ :input {:ref qty-ref
                   :type "number"
                   :step "1"
                   :min "0"
                   :class (str "w-16 text-center text-base p-2 h-10 rounded-lg border "
                            "border-base-300 focus:border-primary focus:outline-none")
                   :value (str qty)
                   :on-change (fn [e] (on-change id :qty (.. e -target -value)))
                   :on-key-down (fn [e]
                                  (when (= (.-key e) "Enter")
                                    (.preventDefault e)
                                    (when-let [el @price-ref] (.focus el))))}))

      ;; Price input
      ($ :div {:class "flex items-center gap-1.5"}
        ($ :label {:class "text-xs text-base-content/40"} "@")
        ($ :input {:ref price-ref
                   :type "number"
                   :step "0.01"
                   :min "0"
                   :class (str "w-24 text-center text-base p-2 h-10 rounded-lg border "
                            "border-base-300 focus:border-primary focus:outline-none")
                   :value (str unit-price)
                   :on-change (fn [e] (on-change id :unit-price (.. e -target -value)))
                   :on-key-down (fn [e]
                                  (when (= (.-key e) "Enter")
                                    (.preventDefault e)
                                    (when on-enter-price (on-enter-price))))}))

      ;; Line total display
      ($ :span {:class "text-sm font-mono text-base-content/50 w-20 text-right"}
        (when (pos? line-total) (format-decimal line-total)))

      ;; Remove button
      ($ :button {:type "button"
                  :class (str "opacity-0 group-hover:opacity-60 hover:!opacity-100 "
                           "text-lg leading-none transition-opacity ml-1")
                  :on-click (fn [e] (.preventDefault e) (.stopPropagation e) (on-remove id))}
        "\u00D7"))))

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
        stores (or (use-subscribe [:user-expenses/stores]) [])
        expense-categories (or (use-subscribe [:user-expenses/expense-categories]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        payers-loading? (boolean (use-subscribe [:user-expenses/payers-loading?]))

        ;; Quick search (backend)
        quick-search-results (use-subscribe [:user-expenses/quick-search-results])
        quick-search-loading? (use-subscribe [:user-expenses/quick-search-loading?])

        ;; Core state
        [phase set-phase!] (use-state :items)       ;; :items or :context
        [items set-items!] (use-state [])            ;; [{:id :article-id :label :qty :unit-price}]
        [context set-context!] (use-state {})        ;; {:supplier {:id :label} :store ... :category ...}
        [input-text set-input-text!] (use-state "")

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

        ;; Backend results (already sorted by score, capped at 10 for display)
        search-results (when (and dropdown-open? (seq quick-search-results))
                         (take 10 quick-search-results))

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
                                    :unit-price (if last-price (format-decimal last-price) "")}]
                      (set-items! (fn [prev] (conj prev new-item)))
                      (set-input-text! "")
                      (set-dropdown-open! false)
                      (set-highlight-idx! -1)
                      (set-type-picker-text! nil)
                      (rf/dispatch [:user-expenses/clear-quick-search])))

        remove-item! (fn [item-id]
                       (set-items! (fn [prev] (vec (remove #(= item-id (:id %)) prev)))))

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
                           (let [c* (assoc c entity-type {:id id :label label-text})
                                 ;; Auto-fill supplier when store is picked
                                 c* (if (and (= entity-type :store)
                                          (not (:supplier c*))
                                          entity)
                                      (let [sid (str (or (:supplier-id entity) (:supplier_id entity)))
                                            s-name (or (:supplier-display-name entity)
                                                     (:supplier_display_name entity))
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

        remove-context! (fn [entity-type]
                          (set-context!
                            (fn [c]
                              (let [c* (dissoc c entity-type)
                                    c* (if (= entity-type :supplier) (dissoc c* :store) c*)]
                                c*))))

        handle-select-result (fn [result]
                               (let [etype (keyword (or (:entity-type result) (:entity_type result)))]
                                 (if (= phase :items)
                                   ;; In items phase — articles become items, others become context
                                   (if (= etype :article)
                                     (add-item! (:id result) (:label result)
                                       (or (:last-price result) (:last_price result)))
                                     (add-context! etype (:id result) (:label result) result))
                                   ;; In context phase — all become context chips
                                   (add-context! etype (:id result) (:label result) result))))

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
                                         ;; Empty input + has items → transition to context phase
                                         (and (str/blank? text) (= phase :items) (seq items))
                                         (do (set-dropdown-open! false)
                                           (set-phase! :context)
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
                                     (set-items! (fn [prev] (vec (butlast prev))))

                                     (and (= phase :context) (seq context))
                                     (let [last-type (last (keys context))]
                                       (remove-context! last-type))))))

        handle-submit (fn [e]
                        (.preventDefault e)
                        (let [validation (validate-form
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
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}])
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
        ;; PHASE 1: Items Entry
        ;; ═══════════════════════════════════════
        (when (= phase :items)
          ($ :div {:class "space-y-4"}

            ;; Welcome prompt
            (when (empty? items)
              ($ :div {:class "text-center py-2"}
                ($ :p {:class "text-2xl font-semibold text-base-content/80 mb-1"}
                  "New Expense")
                ($ :p {:class "text-base text-base-content/50"}
                  "Search for articles to add")))

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
                                  (if (seq items)
                                    "text-lg p-4 rounded-xl border-2 border-base-300 "
                                    "text-xl sm:text-2xl p-5 sm:p-6 rounded-2xl border-2 border-base-300 ")
                                  "focus:border-primary focus:outline-none focus:shadow-lg "
                                  "focus:shadow-primary/10 "
                                  "transition-all bg-white placeholder:text-base-content/30")
                         :placeholder (if (seq items)
                                        "Add another article... (Enter to finish)"
                                        "Search for an article...")
                         :value input-text
                         :on-change handle-input-change
                         :on-key-down handle-input-keydown
                         :auto-complete "off"})

              ;; Autocomplete dropdown (portal)
              (when dropdown-open?
                ($ autocomplete-dropdown
                  {:results (or search-results [])
                   :loading? quick-search-loading?
                   :highlight-idx highlight-idx
                   :on-select handle-select-result
                   :on-create handle-create-inline
                   :input-text (str/trim input-text)
                   :anchor-ref input-ref})))

            ;; Type picker
            (when type-picker-text
              ($ type-picker
                {:text type-picker-text
                 :on-pick handle-type-pick
                 :on-cancel #(set-type-picker-text! nil)
                 :creating? creating?}))

            ;; Running total
            (when (seq items)
              ($ :div {:class "flex items-center justify-between pt-2"}
                ($ :p {:class "text-base text-base-content/50"}
                  (str (count items) " item" (when (> (count items) 1) "s")))
                ($ :p {:class "text-lg font-bold"}
                  (format-decimal items-total) " " currency)))

            ;; Hint text
            (when (nil? type-picker-text)
              ($ :p {:class "text-center text-sm text-base-content/35 mt-2"}
                (if (seq items)
                  "Press Enter on empty to continue →"
                  "Type an article name to start")))))

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
              "\u2190 Back to items")

            ;; Items summary
            ($ :div {:class "bg-base-100 rounded-2xl border border-base-200 p-4"}
              ($ :div {:class "flex items-center justify-between mb-3"}
                ($ :span {:class "font-semibold text-base"}
                  (str "Items (" (count items) ")"))
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
                                  :on-remove #(remove-context! entity-type)}))))

            ;; Context search input — only show when something is still missing
            (let [missing (vec (remove #(contains? context %) [:supplier :store :category]))
                  placeholder (when (seq missing)
                                (str "Search "
                                  (str/join ", " (map #(name %) missing))
                                  "..."))
                  single-missing? (= 1 (count missing))]
              (when (seq missing)
                (let [missing-set (set missing)
                      ;; Filter results to only show entity types still needed
                      filtered-results (filterv
                                         (fn [r]
                                           (let [et (keyword (or (:entity-type r) (:entity_type r)))]
                                             (contains? missing-set et)))
                                         (or search-results []))]
                  ($ :<>
                    ;; Quick picks — show top 5 when exactly one type remains
                    (when (and single-missing? (str/blank? input-text))
                      (let [solo-type (first missing)
                            {:keys [label]} (search/entity-type-info solo-type)
                            picks (top-items-for-type
                                    solo-type suppliers stores expense-categories 10)]
                        (when (seq picks)
                          ($ :div {:class "space-y-2"}
                            ($ :p {:class "text-sm text-base-content/50"}
                              (str "Pick " label))
                            ($ quick-picks
                              {:entity-type solo-type
                               :items picks
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
                                                (let [t (name (first missing))]
                                                  (str "Or search "
                                                    (if (str/ends-with? t "y")
                                                      (str (subs t 0 (dec (count t))) "ies")
                                                      (str t "s"))
                                                    "..."))
                                                placeholder)
                                 :value input-text
                                 :on-change handle-input-change
                                 :on-key-down handle-input-keydown
                                 :auto-complete "off"})

                      ;; Autocomplete dropdown (portal) — filtered to missing types only
                      (when dropdown-open?
                        ($ autocomplete-dropdown
                          {:results filtered-results
                           :loading? quick-search-loading?
                           :highlight-idx highlight-idx
                           :on-select handle-select-result
                           :on-create handle-create-inline
                           :input-text (str/trim input-text)
                           :anchor-ref input-ref})))

                    ;; Type picker — only missing context types
                    (when type-picker-text
                      ($ type-picker
                        {:text type-picker-text
                         :on-pick handle-type-pick
                         :on-cancel #(set-type-picker-text! nil)
                         :creating? creating?
                         :allowed-types missing}))))))

            ;; Payer + Date + Currency row
            ($ :div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4"}
              ;; Payer
              ($ :div
                ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Payer")
                ($ :select {:id "smart-expense-payer"
                            :class (str "w-full text-lg p-4 h-14 rounded-xl border-2 "
                                     "border-base-300 bg-white focus:border-primary cursor-pointer")
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
                           :on-change (fn [e] (set-purchased-at! (.. e -target -value)))}))
              ;; Currency
              ($ :div
                ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Currency")
                ($ :select {:class (str "w-full text-lg p-4 h-14 rounded-xl border-2 border-base-300 "
                                     "bg-white focus:border-primary cursor-pointer")
                            :value currency
                            :on-change (fn [e] (set-currency! (.. e -target -value)))}
                  (for [{:keys [value label]} currency-options]
                    ($ :option {:key value :value value} label)))))

            ;; Notes
            ($ :div
              ($ :label {:class "text-base text-base-content/50 mb-1.5 block"} "Notes")
              ($ :textarea {:id "smart-expense-notes"
                            :class (str "w-full text-lg p-4 rounded-xl border-2 "
                                     "border-base-300 bg-white focus:border-primary resize-none")
                            :rows 2
                            :placeholder "Optional notes..."
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
                  "Cancel"))
              ($ :button {:id "btn-save-smart-expense"
                          :type "submit"
                          :class "ds-btn ds-btn-primary ds-btn-lg text-lg px-10"
                          :disabled (or submitting?
                                      (not (:ok? (validate-form
                                                   {:items items
                                                    :context context
                                                    :payer-id payer-id}))))}
                (if submitting? "Saving..." "Save Expense")))))))))
