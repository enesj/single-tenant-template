(ns app.domain.frontend.expenses.components.form-fields
  "Custom form fields for expense form"
  (:require
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.shared.type-conversion :as type-conv]
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.components.form.fields.select :refer [select-input]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn- pad-two
  [value]
  (let [s (str value)]
    (if (< (count s) 2)
      (str "0" s)
      s)))

(defn current-datetime-local
  "Returns current date/time in ISO local format for datetime-local inputs."
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

(defn new-line-item
  "Creates a new empty line item with a random ID."
  []
  {:id (str (random-uuid))
   :raw_label ""
   :qty ""
   :unit_price ""
   :line_total ""
   ;; When true, :line_total is treated as derived from qty * unit_price.
   ;; When false, user edits to :line_total are preserved.
   :line_total_auto? true})

(defn format-decimal
  "Format a number to a 2 decimal place string for inputs."
  [n]
  (when (some? n)
    (.toFixed n 2)))

(defn safe-parse-number
  "Parse either a string or number, returning nil for invalid inputs."
  [value]
  (cond
    (number? value) value
    (string? value) (type-conv/parse-number value)
    :else nil))

(defn- recalc-line-total-if-possible
  "Auto-calc line total (qty * unit price) when possible and line total is not manually overridden."
  [item]
  (let [qty-num (safe-parse-number (:qty item))
        unit-num (safe-parse-number (:unit_price item))
        auto? (if (contains? item :line_total_auto?)
                (true? (:line_total_auto? item))
                true)]
    (if (and auto? (number? qty-num) (number? unit-num))
      (let [product (* qty-num unit-num)
            formatted (format-decimal product)]
        (assoc item :line_total formatted :line_total_auto? true))
      item)))

(defn update-line-item
  "Updates a specific field in a line item and recalculates total if possible."
  [items item-id key value]
  (mapv (fn [item]
          (if (= item-id (:id item))
            (let [item* (assoc item key value)
                  item* (if (= key :line_total)
                          ;; If user clears line_total, allow auto mode again.
                          (assoc item* :line_total_auto? (str/blank? (str value)))
                          item*)]
              (recalc-line-total-if-possible item*))
            item))
    items))

(defn remove-line-item
  "Removes a line item, ensuring at least one empty item remains."
  [items item-id]
  (let [remaining (vec (remove #(= item-id (:id %)) items))]
    (if (seq remaining)
      remaining
      [(new-line-item)])))

(defn line-items-total
  "Calculates the sum of all line item totals."
  [items]
  (->> items
    (map (fn [{:keys [line_total]}] (safe-parse-number line_total)))
    (remove nil?)
    (reduce + 0)))

;; =============================================================================
;; Components
;; =============================================================================

(defui line-items-input
  [{:keys [value on-change error field-spec]}]
  (let [items (if (seq value) value [(new-line-item)])
        columns (:columns field-spec)
        field-key (let [id (:id field-spec)]
                    (cond
                      (keyword? id) (name id)
                      (string? id) id
                      :else "items"))
        input-id (fn [item-id col-id]
                   (str field-key "-" item-id "-" (name col-id)))

        add-item (fn []
                   (on-change (conj items (new-line-item))))

        remove-item (fn [id]
                      (on-change (remove-line-item items id)))

        handle-line-change (fn [item-id key]
                             (fn [e]
                               (on-change
                                 (update-line-item items item-id key (.. e -target -value)))))]
    ($ :div {:class "space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :h2 {:class "text-lg font-semibold"} (:label field-spec))
        ($ :button {:id (str "btn-add-" field-key "-line-item")
                    :class "ds-btn ds-btn-ghost ds-btn-sm"
                    :type "button"
                    :on-click add-item}
          "Add line item"))
      ($ :div {:class "overflow-x-auto max-h-[320px] overflow-y-auto"}
        ($ :table {:class "ds-table w-full"}
          ($ :thead
            ($ :tr
              (for [col columns
                    :let [label (:label col)
                          width (:width col)]]
                ($ :th {:key (:id col)
                        :class (or width "")}
                  label))
              ($ :th "")))
          ($ :tbody
            (for [item items
                  :let [item-id (:id item)]]
              ($ :tr {:key item-id}
                (for [col columns
                      :let [col-id (:id col)
                            val (get item col-id)
                            type (:type col)
                            placeholder (:placeholder col)
                            step (:step col)
                            min-val (:min col)]]
                  ($ :td {:key col-id}
                    ($ common/input
                      (cond-> {:id (input-id item-id col-id)
                               :class "ds-input ds-input-bordered w-full"
                               :value (or val "")
                               :type (name (or type :text))
                               :on-change (handle-line-change item-id col-id)}
                        placeholder (assoc :placeholder placeholder)
                        step (assoc :step step)
                        min-val (assoc :min min-val)))))
                ($ :td
                  ($ :button {:id (str "btn-remove-" field-key "-line-item-" item-id)
                              :class "text-xs text-error"
                              :type "button"
                              :on-click #(remove-item item-id)}
                    "Remove")))))))
      (when error
        ($ :div {:class "text-error text-sm mt-1"} error)))))

(def ^:private amount-tolerance 0.01)

(defui total-amount-input
  [{:keys [id value on-change values error]}]
  (let [field-key (let [id* id]
                    (cond
                      (keyword? id*) (name id*)
                      (string? id*) id*
                      :else "total_amount"))
        input-id (str "expense-" field-key)
        items (:items values)
        computed-total (line-items-total items)
        parsed-total (safe-parse-number value)
        [auto-total? set-auto-total!] (use-state true)
        total-diff (when (and (number? parsed-total) (number? computed-total) (pos? computed-total))
                     (js/Math.abs (- parsed-total computed-total)))
        total-mismatch? (and total-diff (> total-diff amount-tolerance))]
    (use-effect
      (fn []
        (when (and auto-total? (pos? computed-total) (not= parsed-total computed-total))
          (on-change computed-total))
        js/undefined)
      [auto-total? computed-total parsed-total on-change])
    ($ :div {:class "space-y-1"}
      ($ :div {:class "flex gap-2"}
        ($ :input {:id input-id
                   :class "ds-input ds-input-bordered w-full"
                   :type "number"
                   :step "0.01"
                   :value (or value "")
                   :on-change (fn [e]
                                (set-auto-total! false)
                                (on-change (safe-parse-number (.. e -target -value))))})
        (when (pos? computed-total)
          ($ :button {:id (str "btn-use-total-" input-id)
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :type "button"
                      :on-click (fn []
                                  (set-auto-total! true)
                                  (on-change computed-total))}
            "Use total")))
      (when (pos? computed-total)
        ($ :p {:class "text-xs text-base-content/60"}
          (str "Line items total: " (or (format-decimal computed-total) "0.00"))
          (when total-mismatch?
            ($ :span {:class "text-error ml-2"} "(does not match total)"))))
      (when error
        ($ :div {:id (str input-id "-error")
                 :class "text-error text-sm mt-1"}
          error)))))

;; =============================================================================
;; Admin Select Components (Suppliers / Articles)
;; =============================================================================

(defn- options-from-items
  [items label-fn]
  (->> (or items [])
    (map (fn [item]
           {:value (:id item)
            :label (label-fn item)}))
    (remove (fn [opt] (nil? (:value opt))))
    (sort-by :label)
    vec))

(defn- dom-id
  [v]
  (cond
    (string? v) v
    (keyword? v) (name v)
    (symbol? v) (name v)
    (nil? v) nil
    :else (str v)))

(defui supplier-select-with-inline-create
  [{:keys [id label error required inline class on-change value form-id formId field-spec]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        creating? (use-subscribe [:expenses/supplier-inline-create-loading?])
        create-error (use-subscribe [:expenses/supplier-inline-create-error])
        [open? set-open!] (use-state false)
        [display-name set-display-name!] (use-state "")
        [local-error set-local-error!] (use-state nil)

        form-id* (or form-id formId)
        field-key (or (some-> (:id field-spec) name)
                    (some-> id dom-id)
                    "supplier_id")
        base-id (or form-id* "expense-form")

        default-display-name (some-> (or (:create-default-display-name field-spec)
                                       (:new-supplier-default-display-name field-spec)
                                       (:default-display-name field-spec))
                               str
                               str/trim
                               not-empty)

        select-id (or (when (string? id) id)
                    (str base-id "-" field-key "-select"))
        error-id (str select-id "-error")
        add-btn-id (str "btn-add-supplier-" base-id)

        modal-id (str base-id "-" field-key "-create-supplier-modal")
        name-input-id (str base-id "-" field-key "-create-supplier-display-name-input")
        cancel-btn-id (str "btn-cancel-supplier-create-" base-id)
        save-btn-id (str "btn-save-supplier-create-" base-id)

        label-class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))
        wrapper-class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))

        field-error-msg (cond
                          (nil? error) nil
                          (string? error) error
                          (map? error) (or (:message error) (str error))
                          :else (str error))
        opts (vec (or (:options field-spec)
                    (options-from-items suppliers select-options/supplier-label)))

        open-modal (fn []
                     (set-local-error! nil)
                     (set-display-name! (or default-display-name ""))
                     (rf/dispatch [::suppliers-events/clear-inline-create])
                     (set-open! true))
        close-modal (fn []
                      (set-open! false)
                      (set-display-name! "")
                      (set-local-error! nil)
                      (rf/dispatch [::suppliers-events/clear-inline-create]))
        submit (fn []
                 (let [name* (str/trim (str display-name))]
                   (cond
                     (str/blank? name*)
                     (set-local-error! "Display name is required.")

                     creating?
                     nil

                     :else
                     (do
                       (set-local-error! nil)
                       (rf/dispatch [::suppliers-events/create-inline
                                     {:display_name name*}
                                     (fn [supplier]
                                       (when (and supplier (fn? on-change))
                                         (when-let [new-id (:id supplier)]
                                           (on-change (str new-id))))
                                       (close-modal))])))))]

    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])

    (use-effect
      (fn []
        (when (and open?
                (str/blank? (str display-name))
                (not (str/blank? (str default-display-name))))
          (set-display-name! default-display-name))
        js/undefined)
      [display-name open? default-display-name])

    ($ :div {:class wrapper-class}
      ($ common/label {:text (or label "Supplier")
                       :class label-class
                       :for select-id
                       :required required})
      ($ :div {:class (when inline "flex-1 w-full text-left")}
        ($ :div {:class "flex items-start gap-2"}
          ($ :div {:class (str "flex-1 " (or class ""))}
            ($ common/select {:id select-id
                              :value value
                              :options opts
                              :on-change on-change}))
          ($ :button {:id add-btn-id
                      :type "button"
                      :class "ds-btn ds-btn-ghost ds-btn-sm"
                      :on-click open-modal}
            "New"))
        (when field-error-msg
          ($ :div {:id error-id
                   :class "text-error text-sm mt-1"}
            field-error-msg)))

      (when open?
        ($ modal {:id modal-id
                  :on-close close-modal
                  :draggable? false
                  :width "520px"
                  :header "New supplier"}
          ($ :div {:class "p-6 space-y-4"}
            (when (or local-error create-error)
              ($ :div {:class "ds-alert ds-alert-error"}
                ($ :span (or local-error create-error))))

            ;; NOTE: This modal is rendered inside the main expense form (<form>).
            ;; Nested <form> elements cause the parent form to submit and close the page/modal.
            ;; Keep this as a <div> and manually handle submit interactions.
            ($ :div {:class "space-y-2"}
              ($ :label {:class "ds-label" :for name-input-id}
                "Display name")
              ($ :input {:id name-input-id
                         :class "ds-input ds-input-bordered w-full"
                         :type "text"
                         :value display-name
                         :auto-focus true
                         :on-key-down (fn [e]
                                        (when (= "Enter" (.-key e))
                                          (.preventDefault e)
                                          (.stopPropagation e)
                                          (submit)))
                         :on-change (fn [e]
                                      (set-display-name! (.. e -target -value)))})

              ($ :div {:class "flex justify-end gap-2 pt-4"}
                ($ :button {:id cancel-btn-id
                            :type "button"
                            :class "ds-btn"
                            :on-click close-modal}
                  "Cancel")
                ($ :button {:id save-btn-id
                            :type "button"
                            :class "ds-btn ds-btn-primary"
                            :disabled (or creating?
                                        (str/blank? (str/trim (str display-name))))
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (submit))}
                  (if creating? "Saving..." "Create"))))))))))

(defui supplier-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (options-from-items suppliers select-options/supplier-label)]
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui article-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [articles (use-subscribe [:expenses/articles])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (options-from-items articles select-options/article-label)]
    (use-effect
      (fn []
        (rf/dispatch [::articles-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui expense-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [expenses (use-subscribe [:expenses/expenses])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (options-from-items expenses select-options/expense-label)]
    (use-effect
      (fn []
        (rf/dispatch [:app.domain.frontend.expenses.events.expenses/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))
