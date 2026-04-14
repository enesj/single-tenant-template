(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.components
  "Sub-components for the smart expense input."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [format-decimal safe-parse-number]]
    [app.domain.frontend.expenses.components.manual-expense-form.search :as search]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants
     :refer [chip-styles type-button-styles]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
     :refer [entity-type-label]]
    [app.template.frontend.ui.z-scale :as z]
    [clojure.string :as str]
    ["react-dom" :as react-dom]
    [uix.core :refer [$ defui use-effect use-ref use-state]]))

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
  [{:keys [t results loading? highlight-idx on-select on-create input-text anchor-ref]}]
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
                           :z-index z/modal-portal-child
                           :top (str (:top pos) "px")
                           :left (str (:left pos) "px")
                           :width (str (:width pos) "px")}
                   :on-click (fn [e] (.stopPropagation e))}
            ;; Loading indicator
            (when loading?
              ($ :div {:class "flex items-center gap-3 px-5 py-4 text-base-content/40"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                ($ :span {:class "text-base"} (t :smart-expense/searching))))

            ;; Search results (capped at 10)
            (when (seq visible-results)
              (for [[idx result] (map-indexed vector visible-results)]
                (let [etype (keyword (or (:entity-type result) (:entity_type result)))
                      {:keys [icon]} (search/entity-type-info etype)
                      etype-label (entity-type-label t etype)
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
                    (when-let [price (search/result-last-price result)]
                      (let [global-price? (search/global-last-price? result)
                            tooltip (when (search/global-last-price? result)
                                      (when-let [sname (search/result-last-price-supplier-name result)]
                                        (t :smart-expense/price-from sname)))]
                        ($ :span {:class (str "text-sm font-mono "
                                           (if global-price?
                                             "text-amber-600 font-semibold"
                                             "text-base-content/50")
                                           (when tooltip " cursor-help"))
                                  :title tooltip}
                          (format-decimal price))))
                    ($ :span {:class (str "text-xs px-2.5 py-1 rounded-full border font-medium "
                                       (get chip-styles etype ""))}
                      etype-label)))))

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
                  (t :smart-expense/create-prefix)
                  ($ :span {:class "font-bold"} (str "\u201C" input-text "\u201D"))))))
          portal-target)))))

(defui type-picker
  "Picker shown when user types a name that doesn't match existing entities.
   `allowed-types` optionally restricts which entity types are shown."
  [{:keys [t text on-pick on-cancel creating? allowed-types]}]
  ($ :div {:class "mt-4 p-6 bg-base-100 rounded-2xl border border-base-200"}
    ($ :p {:class "text-lg text-base-content/70 mb-4"}
      (t :smart-expense/what-is (str "\u201C" text "\u201D")))
    ($ :div {:class "flex flex-wrap gap-3"}
      (for [entity-type (if (seq allowed-types)
                          allowed-types
                          [:article :supplier :store :category])]
        (let [{:keys [icon]} (search/entity-type-info entity-type)
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
            ($ :span (entity-type-label t entity-type))))))
    ($ :button {:type "button"
                :class "mt-3 text-sm text-base-content/50 hover:text-base-content/70 transition-colors"
                :on-click (fn [e] (.preventDefault e) (on-cancel))}
      (t :smart-expense/cancel))))

(defn top-items-for-type
  "Return up to `n` items for the given entity type from local data."
  [entity-type suppliers stores expense-categories articles n selected-supplier-id]
  (let [name-fn (get-in search/entity-type-config [entity-type :name-fn])
        base-items (case entity-type
                     :supplier suppliers
                     :store (if selected-supplier-id
                              (filter #(= (str selected-supplier-id)
                                         (str (or (:supplier-id %) (:supplier_id %))))
                                stores)
                              stores)
                     :category expense-categories
                     :article articles
                     [])]
    (->> base-items
      (take n)
      (mapv (fn [item]
              {:id (:id item)
               :label (or (when name-fn (name-fn item)) "")
               :entity-type entity-type
               :entity item})))))

(defn build-quick-pick-groups
  [missing-types suppliers stores expense-categories articles selected-supplier-id]
  (let [limit (if (= 1 (count missing-types)) 10 5)]
    (->> missing-types
      (map (fn [entity-type]
             {:entity-type entity-type
              :items (top-items-for-type entity-type
                       suppliers
                       stores
                       expense-categories
                       articles
                       limit
                       selected-supplier-id)}))
      (filter #(seq (:items %)))
      vec)))

(defn phase-two-quick-pick-groups
  [missing-types context-suggestions suppliers stores expense-categories articles selected-supplier-id]
  (let [limit (if (= 1 (count missing-types)) 10 5)
        local-groups-by-type (->> (build-quick-pick-groups missing-types
                                    suppliers
                                    stores
                                    expense-categories
                                    articles
                                    selected-supplier-id)
                               (map (juxt :entity-type identity))
                               (into {}))
        store-matches-supplier? (fn [s]
                                  (or (nil? selected-supplier-id)
                                    (= (str selected-supplier-id)
                                      (str (or (:supplier-id s) (:supplier_id s))))))
        wrap-store (fn [s]
                     {:id (:id s)
                      :label (or (:label s) (:display-name s) (:display_name s) "")
                      :entity-type :store
                      :entity s})
        dedupe-by-id (fn [items]
                       (let [seen (volatile! #{})]
                         (->> items
                           (keep (fn [item]
                                   (let [id (:id item)]
                                     (when-not (contains? @seen id)
                                       (vswap! seen conj id)
                                       item))))
                           vec)))]
    (->> missing-types
      (keep (fn [entity-type]
              (let [raw-suggested (case entity-type
                                    :supplier (:suppliers context-suggestions)
                                    :store (:stores context-suggestions)
                                    :category (:categories context-suggestions)
                                    [])]
                (cond
                  ;; Stores: merge history-ranked picks with the full
                  ;; supplier-filtered pool. History rows appear first
                  ;; (so the user sees frequent picks at a glance) and the
                  ;; remaining supplier stores fill the rest of the slots.
                  (= entity-type :store)
                  (let [history-items (->> raw-suggested
                                        (filter store-matches-supplier?)
                                        (mapv wrap-store))
                        pool-items (->> stores
                                     (filter store-matches-supplier?)
                                     (mapv wrap-store))
                        merged (->> (concat history-items pool-items)
                                 dedupe-by-id
                                 (take limit)
                                 vec)]
                    (when (seq merged)
                      {:entity-type :store :items merged}))

                  ;; Other types keep the existing precedence: history
                  ;; suggestions win when present, otherwise fall back
                  ;; to the local quick picks.
                  (seq raw-suggested)
                  {:entity-type entity-type
                   :items (mapv (fn [suggestion]
                                  {:id (:id suggestion)
                                   :label (:label suggestion)
                                   :entity-type entity-type
                                   :entity suggestion})
                            raw-suggested)}

                  :else
                  (get local-groups-by-type entity-type)))))
      vec)))

(defui quick-picks
  "Shows top N options as clickable chips when only one entity type is missing."
  [{:keys [entity-type items on-select]}]
  (let [{:keys [icon]} (search/entity-type-info entity-type)
        default-style (get chip-styles entity-type "bg-base-200 text-base-content")]
    (when (seq items)
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [{:keys [id label chip-class] :as item} items]
          ($ :button {:key (str (name entity-type) "-" id)
                      :type "button"
                      :class (str "inline-flex items-center gap-2 px-4 py-2.5 rounded-full "
                               "text-base font-medium border cursor-pointer "
                               "transition-all hover:shadow-md hover:scale-[1.02] "
                               (or chip-class default-style))
                      :on-click (fn [e] (.preventDefault e) (.stopPropagation e)
                                  (on-select item))}
            ($ :span icon)
            ($ :span {:class "truncate max-w-[200px]"} label)))))))

(defui item-row
  "A single item row with inline qty and price inputs."
  [{:keys [item on-change on-remove on-enter-price auto-focus-qty? on-focus-handled]}]
  (let [{:keys [id label qty unit-price]} item
        qty-ref (use-ref nil)
        price-ref (use-ref nil)
        ;; Latest-callback ref: lets the use-effect read the current
        ;; `on-focus-handled` without listing it as a dep (it's recreated
        ;; every parent render, so adding it would re-run the effect on
        ;; every render).
        handled-ref (use-ref nil)
        line-total (* (or (safe-parse-number qty) 1)
                     (or (safe-parse-number unit-price) 0))
        input-class (str "text-center text-lg font-semibold p-2 h-12 rounded-lg "
                      "border-2 border-base-300 bg-white "
                      "focus:border-primary focus:outline-none focus:shadow-sm "
                      "transition-colors")
        label-class "text-sm font-bold text-base-content/60 select-none"]
    (reset! handled-ref on-focus-handled)
    ;; One-shot focus signal from parent: when this row is the freshly-added
    ;; item, focus the qty input and select its value so the user can type the
    ;; new quantity immediately, then notify the parent to clear the signal.
    (use-effect
      (fn []
        (when (and auto-focus-qty? @qty-ref)
          (js/setTimeout
            (fn []
              (when-let [el @qty-ref]
                (.focus el)
                (when (.-select el) (.select el))))
            30)
          (when-let [cb @handled-ref] (cb)))
        js/undefined)
      [auto-focus-qty?])
    ($ :div {:class (str "flex items-center gap-3 py-3 px-4 bg-base-100 rounded-xl "
                      "border-2 border-base-200 group")}
      ;; Article icon + name
      ($ :span {:class "text-xl flex-none"} "📦")
      ($ :span {:class "flex-1 text-base font-medium truncate min-w-0"} label)

      ;; Qty input
      ($ :div {:class "flex items-center gap-2"}
        ($ :label {:class label-class} "\u00D7")
        ($ :input {:ref qty-ref
                   :type "number"
                   :step "1"
                   :min "0"
                   :class (str "w-20 " input-class)
                   :value (str qty)
                   :on-focus (fn [e] (when-let [el (.-target e)]
                                       (when (.-select el) (.select el))))
                   :on-change (fn [e] (on-change id :qty (.. e -target -value)))
                   :on-key-down (fn [e]
                                  (when (= (.-key e) "Enter")
                                    (.preventDefault e)
                                    (when-let [el @price-ref]
                                      (.focus el)
                                      (when (.-select el) (.select el)))))}))

      ;; Price input
      ($ :div {:class "flex items-center gap-2"}
        ($ :label {:class label-class} "@")
        ($ :input {:ref price-ref
                   :type "number"
                   :step "0.01"
                   :min "0"
                   :class (str "w-28 " input-class)
                   :value (str unit-price)
                   :on-focus (fn [e] (when-let [el (.-target e)]
                                       (when (.-select el) (.select el))))
                   :on-change (fn [e] (on-change id :unit-price (.. e -target -value)))
                   :on-key-down (fn [e]
                                  (when (= (.-key e) "Enter")
                                    (.preventDefault e)
                                    (when on-enter-price (on-enter-price))))}))

      ;; Line total display
      ($ :span {:class "text-base font-mono font-bold text-base-content w-24 text-right"}
        (when (pos? line-total) (format-decimal line-total)))

      ;; Remove button
      ($ :button {:type "button"
                  :class (str "opacity-0 group-hover:opacity-60 hover:!opacity-100 "
                           "text-lg leading-none transition-opacity ml-1")
                  :on-click (fn [e] (.preventDefault e) (.stopPropagation e) (on-remove id))}
        "\u00D7"))))
