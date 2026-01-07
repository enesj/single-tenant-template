(ns app.domain.frontend.expenses.components.expense-form
  "Reusable expense form components using the template form system.

   Uses the master-detail-form wrapper for edit modal orchestration."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 format-decimal
                                                                 line-items-input
                                                                 line-items-total
                                                                 new-line-item
                                                                 safe-parse-number
                                                                 supplier-select-with-inline-create
                                                                 total-amount-input]]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.template.frontend.components.form :refer [form form-fields]]
    [app.template.frontend.components.form.base :as base]
    [app.template.frontend.components.form.master-detail :refer [master-detail-form]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Constants & Data
;; =============================================================================

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def line-item-columns
  [{:id :raw_label
    :label "Label"
    :type :text
    :placeholder "e.g. Milk, Bread"}
   {:id :qty
    :label "Qty"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-24"}
   {:id :unit_price
    :label "Unit Price"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}
   {:id :line_total
    :label "Line Total"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}])

(defn get-expense-form-spec
  ([suppliers payers]
   (get-expense-form-spec suppliers payers nil))
  ([suppliers payers {:keys [new-supplier-default-display-name receipt-approval? receipt receipt-id]}]
   (let [receipt-id* (or receipt-id (:id receipt))
         receipt-id-str (some-> receipt-id* str)
         receipt-supplier-guess (some-> (:supplier-guess receipt) str str/trim not-empty)
         receipt-total-guess (:total-amount-guess receipt)
         totals-match? (:total-guess-equals-lines-total-guess? receipt)]
     [{:id :supplier_id
       :type :select
       :component supplier-select-with-inline-create
       :label "Supplier"
       :required true
       :placeholder "Select supplier"
       :create-default-display-name new-supplier-default-display-name
       :receipt-id receipt-id-str
       :receipt-supplier-guess receipt-supplier-guess
       :options (map (fn [s]
                       {:value (:id s)
                        :label (select-options/supplier-label s)})
                  suppliers)}
      {:id :payer_id
       :type :select
       :label "Payer"
       :required true
       :placeholder "Select payer"
       :options (map (fn [p]
                       {:value (:id p)
                        :label (str (:label p)
                                 (when (:type p)
                                   (str " (" (:type p) ")")))})
                  payers)}
      {:id :purchased_at
       :type :datetime-local ;; standard input type
       :label "Purchased at"
       :required true}
      {:id :total_amount
       :component total-amount-input ;; Custom component
       :label "Total amount"
       :required true
       ;; Receipt-approval UX: total is auto-derived.
       :show-use-total? (not receipt-approval?)
       :receipt-total-guess receipt-total-guess
       :totals-match? totals-match?}
      {:id :currency
       :type :select
       :label "Currency"
       :required true
       :options currency-options}
      {:id :notes
       :type :textarea
       :label "Notes"
       :required false
       :placeholder "Optional notes"}
      {:id :items
       :component line-items-input ;; Custom component
       :label "Line Items"
       :columns line-item-columns
       :style (if receipt-approval? {:maxHeight "260px"} {:maxHeight "300px"})
       :overflow-y-class "overflow-y-auto"
       :scrollbar-gutter-stable? true}])))

;; =============================================================================
;; Normalization & Validation Helpers
;; =============================================================================

(def ^:private amount-tolerance 0.01)

(defn- pad-two [value]
  (let [s (str value)]
    (if (< (count s) 2) (str "0" s) s)))

(defn- datetime-local
  "Coerce an ISO-ish timestamp into a datetime-local input value (YYYY-MM-DDTHH:MM).
   Falls back to `current-datetime-local` when parsing fails and `fallback?` is true."
  ([value]
   (datetime-local value false))
  ([value fallback?]
   (let [s (some-> value str)]
     (cond
       (str/blank? s)
       (when fallback? (current-datetime-local))

       ;; Already looks like datetime-local
       (re-matches #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$" s)
       s

       ;; ISO-ish string (may include seconds / timezone). Prefer a real parse.
       :else
       (try
         (let [d (js/Date. s)
               t (.getTime d)]
           (if (js/isNaN t)
             (when fallback? (current-datetime-local))
             (str (.getFullYear d)
               "-" (pad-two (inc (.getMonth d)))
               "-" (pad-two (.getDate d))
               "T" (pad-two (.getHours d))
               ":" (pad-two (.getMinutes d)))))
         (catch :default _
           (when fallback? (current-datetime-local))))))))

(defn normalize-receipt-data
  "Normalize extracted receipt data into form initial values."
  [receipt]
  (let [debug? (and (exists? js/window)
                 (boolean (.-__DEBUG_RECEIPT_PREFILL__ js/window)))
        extract0 (or (:raw-extract-json receipt)
                   (:raw_extract_json receipt)
                   (:receipts/raw-extract-json receipt)
                   (:receipts/raw_extract_json receipt))
        raw-extract (cond
                      (map? extract0) extract0
                      (nil? extract0) {}
                      (string? extract0) (try
                                           (js->clj (js/JSON.parse extract0) :keywordize-keys true)
                                           (catch :default _ {}))
                      :else (js->clj extract0 :keywordize-keys true))

        ;; Worker persists a wrapper map like:
        ;; {:provider ... :response ... :extraction {:merchant ... :totals ... :items ...}}
        extraction (or (:extraction raw-extract)
                     (:receipt-extraction raw-extract))
        extract (if (map? extraction) extraction raw-extract)

        _ (when debug?
            (js/console.log "[receipt->expense] normalize"
              (clj->js {:wrapper-keys (map str (keys raw-extract))
                        :extraction-keys (map str (keys extract))})))

        totals (let [t (or (:totals extract)
                         (:totals_extract extract)
                         (:totals-extract extract)
                         {})]
                 (if (map? t) t {}))

        items0 (or (:items extract)
                 (:line_items extract)
                 (:line-items extract)
                 (:receipt_items extract)
                 (:receipt-items extract))
        items (when (sequential? items0) items0)

        purchased-at (or (:purchased_at extract)
                       (:purchased-at extract)
                       (:date extract)
                       (:purchased_at_guess receipt)
                       (:purchased-at-guess receipt)
                       (:receipts/purchased_at_guess receipt)
                       (:receipts/purchased-at-guess receipt))

        total-amount0 (or (:total_amount totals)
                        (:total-amount totals)
                        (:total totals)
                        (:total_amount extract)
                        (:total-amount extract)
                        (:total_amount_guess receipt)
                        (:total-amount-guess receipt)
                        (:receipts/total_amount_guess receipt)
                        (:receipts/total-amount-guess receipt))
        total-amount (safe-parse-number total-amount0)

        currency0 (or (:currency totals)
                    (:currency extract)
                    (:currency_guess receipt)
                    (:currency-guess receipt)
                    (:receipts/currency_guess receipt)
                    (:receipts/currency-guess receipt)
                    "BAM")
        currency (cond
                   (keyword? currency0) (name currency0)
                   (string? currency0) currency0
                   (some? currency0) (str currency0)
                   :else "BAM")

        supplier (or (:supplier-guess-supplier receipt)
                   (:supplier_guess_supplier receipt)
                   (:receipts/supplier-guess-supplier receipt))
        supplier-id (when (map? supplier) (:id supplier))

        normalize-item (fn [item]
                         (let [id (random-uuid)
                               raw-label (or (:raw_label item)
                                           (:raw-label item)
                                           (:label item)
                                           (:name item))
                               qty (safe-parse-number (:qty item))
                               unit-price (safe-parse-number (or (:unit_price item) (:unit-price item)))
                               line-total (safe-parse-number (or (:line_total item) (:line-total item)))]
                           {:id (str id)
                            :raw_label (or (some-> raw-label str) "")
                            :qty (if (number? qty) (str qty) "")
                            :unit_price (if (number? unit-price) (format-decimal unit-price) "")
                            :line_total (if (number? line-total) (format-decimal line-total) "")
                            :line_total_auto? true}))
        filename (or (:original-filename receipt)
                   (:original_filename receipt)
                   (:storage-key receipt)
                   (:storage_key receipt)
                   "(unknown)")]
    {:supplier_id supplier-id
     :payer_id nil
     :purchased_at (datetime-local purchased-at true)
     :total_amount (if (number? total-amount) (format-decimal total-amount) "")
     :currency currency
     :notes (str "Extracted from receipt: " filename)
     :items (if (seq items)
              (mapv normalize-item items)
              [(new-line-item)])}))

(defn- receipt-merchant-name
  [receipt]
  (let [extract0 (or (:raw-extract-json receipt)
                   (:raw_extract_json receipt)
                   (:receipts/raw-extract-json receipt)
                   (:receipts/raw_extract_json receipt))
        raw-extract (cond
                      (map? extract0) extract0
                      (nil? extract0) {}
                      (string? extract0) (try
                                           (js->clj (js/JSON.parse extract0) :keywordize-keys true)
                                           (catch :default _ {}))
                      :else (js->clj extract0 :keywordize-keys true))
        extraction (or (:extraction raw-extract)
                     (:receipt-extraction raw-extract))
        extract (if (map? extraction) extraction raw-extract)
        merchant (or (:merchant extract)
                   (:merchant_info extract)
                   (:merchant-info extract))
        merchant-name0 (cond
                         (map? merchant) (:name merchant)
                         (string? merchant) merchant
                         :else nil)
        merchant-name (some-> merchant-name0 str str/trim not-empty)
        supplier-guess (or (:supplier_guess receipt)
                         (:supplier-guess receipt)
                         (:receipts/supplier_guess receipt)
                         (:receipts/supplier-guess receipt))]
    (or merchant-name (some-> supplier-guess str str/trim not-empty))))

(defn- prepare-line-items
  "Prepare/validate raw line items from the UI.
   Keeps items that have a non-blank label and a parseable line_total.
   Coerces numeric fields where possible."
  [items]
  (keep (fn [{:keys [id raw_label qty unit_price line_total]}]
          (let [parsed-total (safe-parse-number line_total)
                qty-num (safe-parse-number qty)
                unit-num (safe-parse-number unit_price)
                raw-label* (some-> raw_label str)
                id* (some-> id str)]
            (when (and (not (str/blank? raw-label*)) (number? parsed-total))
              (cond-> {:raw_label raw-label*
                       :line_total parsed-total}
                (and id* (not (str/blank? id*))) (assoc :id id*)
                (number? qty-num) (assoc :qty qty-num)
                (number? unit-num) (assoc :unit_price unit-num)))))
    (or items [])))

(defn normalize-initial-data
  "Normalize an expense entity (from the entity store or detail fetch) into form initial values."
  [expense]
  (letfn [(normalize-line-item [item]
            (let [item (if (map? item) item {})
                  id (or (:id item)
                       (:expense_item_id item)
                       (:expense-item-id item)
                       (random-uuid))
                  raw-label (or (:raw_label item) (:raw-label item) "")
                  qty (:qty item)
                  unit-price (or (:unit_price item) (:unit-price item))
                  line-total (or (:line_total item) (:line-total item))
                  auto? (if (contains? item :line_total_auto?)
                          (not (false? (:line_total_auto? item)))
                          true)]
              {:id (str id)
               :raw_label (if (some? raw-label) (str raw-label) "")
               :qty (cond
                      (string? qty) qty
                      (number? qty) (str qty)
                      (nil? qty) ""
                      :else (str qty))
               :unit_price (cond
                             (string? unit-price) unit-price
                             (number? unit-price) (format-decimal unit-price)
                             (nil? unit-price) ""
                             :else (str unit-price))
               :line_total (cond
                             (string? line-total) line-total
                             (number? line-total) (format-decimal line-total)
                             (nil? line-total) ""
                             :else (str line-total))
               :line_total_auto? auto?}))]
    (let [supplier-id (or (:supplier_id expense) (:supplier-id expense) (:expenses/supplier_id expense))
          payer-id (or (:payer_id expense) (:payer-id expense) (:expenses/payer_id expense))
          purchased-at (or (:purchased_at expense) (:purchased-at expense) (:expenses/purchased_at expense))
          total-amount (or (:total_amount expense) (:total-amount expense) (:expenses/total_amount expense))
          currency (or (:currency expense) "BAM")
          notes (or (:notes expense) "")
          items (or (:items expense) (:expenses/items expense) [])
          normalized-items (if (seq items)
                             (mapv normalize-line-item items)
                             [(new-line-item)])]
      {:supplier_id supplier-id
       :payer_id payer-id
       :purchased_at (datetime-local purchased-at true)
       :total_amount total-amount
       :currency currency
       :notes notes
       :items normalized-items})))

(defn validate-expense-values
  "Validate expense form values. Returns {:ok? true} or {:ok? false :error \"...\"}."
  [values]
  (let [supplier-id (:supplier_id values)
        payer-id (:payer_id values)
        purchased-at (:purchased_at values)
        prepared-items (vec (prepare-line-items (:items values)))
        computed-total (line-items-total prepared-items)
        parsed-total (safe-parse-number (:total_amount values))
        effective-total (or parsed-total (when (pos? computed-total) computed-total))
        total-diff (when (and (number? parsed-total)
                           (number? computed-total)
                           (pos? computed-total))
                     (js/Math.abs (- parsed-total computed-total)))
        total-mismatch? (and total-diff (> total-diff amount-tolerance))]
    (cond
      (or (str/blank? (str supplier-id))
        (str/blank? (str payer-id))
        (str/blank? (str purchased-at)))
      {:ok? false :error "Supplier, payer, and date are required."}

      (empty? prepared-items)
      {:ok? false :error "Add at least one line item with a label and total."}

      (or (nil? effective-total) (<= effective-total 0))
      {:ok? false :error "Enter a total amount greater than 0."}

      total-mismatch?
      {:ok? false :error (str "Total (" (or (format-decimal effective-total) effective-total)
                           ") must match line items (" (or (format-decimal computed-total) computed-total) ").")}

      :else
      {:ok? true})))

(defn validate-receipt-review-values
  "Validate values for saving reviewed receipt data.

  Differs from `validate-expense-values` by NOT requiring expense-only fields
  (e.g. payer)."
  [values]
  (let [supplier-id (:supplier_id values)
        purchased-at (:purchased_at values)
        prepared-items (vec (prepare-line-items (:items values)))
        computed-total (line-items-total prepared-items)
        parsed-total (safe-parse-number (:total_amount values))
        effective-total (or parsed-total (when (pos? computed-total) computed-total))
        total-diff (when (and (number? parsed-total)
                           (number? computed-total)
                           (pos? computed-total))
                     (js/Math.abs (- parsed-total computed-total)))
        total-mismatch? (and total-diff (> total-diff amount-tolerance))]
    (cond
      (or (str/blank? (str supplier-id))
        (str/blank? (str purchased-at)))
      {:ok? false :error "Supplier and date are required."}

      (empty? prepared-items)
      {:ok? false :error "Add at least one line item with a label and total."}

      (or (nil? effective-total) (<= effective-total 0))
      {:ok? false :error "Enter a total amount greater than 0."}

      total-mismatch?
      {:ok? false
       :error (str "Total (" (or (format-decimal effective-total) effective-total)
                ") must match line items (" (or (format-decimal computed-total) computed-total) ").")}

      :else
      {:ok? true})))

(defn prepare-expense-submit-values
  "Prepare expense form values for submission."
  [values]
  (let [prepared-items (vec (prepare-line-items (:items values)))
        computed-total (line-items-total prepared-items)
        parsed-total (safe-parse-number (:total_amount values))
        effective-total (or parsed-total (when (pos? computed-total) computed-total))]
    {:supplier_id (:supplier_id values)
     :payer_id (:payer_id values)
     :purchased_at (:purchased_at values)
     :currency (:currency values)
     :notes (:notes values)
     :total_amount effective-total
     :items prepared-items}))

;; =============================================================================
;; Form Body Wrapper (shared between add/edit)
;; =============================================================================

(defui expense-form-body
  "Internal form body component. Used by both add and edit modals."
  [{:keys [mode initial-data on-submit on-cancel _loading?
           new-supplier-default-display-name receipt-approval? receipt receipt-id]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        form-error (use-subscribe [:expenses/entries-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Memoize entity-spec to avoid recreating on every render
        entity-spec (use-memo
                     #(get-expense-form-spec suppliers payers
                        {:new-supplier-default-display-name new-supplier-default-display-name
                         :receipt-approval? receipt-approval?
                         :receipt receipt
                         :receipt-id receipt-id})
                     [suppliers payers new-supplier-default-display-name receipt-approval? receipt receipt-id])

        ;; Memoize initial values so fork/form doesn't reset on every render
        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        handle-submit (fn [{:keys [values]}]
                        (let [validation-result (validate-expense-values values)]
                          (if (:ok? validation-result)
                            (do
                              (set-validation-error! nil)
                              (on-submit (prepare-expense-submit-values values)))
                            (set-validation-error! (:error validation-result)))))]

    ;; Load dependencies
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (or validation-error form-error))))

      ($ form
        {:entity-name "expense"
         :entity-spec entity-spec
         :editing (= mode :edit)
         :initial-values form-initial-values
         :on-cancel on-cancel
         :on-submit handle-submit
         :save-disabled? (fn [values]
                           (empty? (prepare-line-items (:items values))))
         :button-text (if (= mode :edit) "Update Expense" "Save Expense")}))))

;; =============================================================================
;; Receipt approval form (split actions)
;; =============================================================================

(defn- dirty?
  [dirty]
  (cond
    (nil? dirty) false
    (map? dirty) (seq dirty)
    (set? dirty) (seq dirty)
    (sequential? dirty) (seq dirty)
    :else true))

(defui receipt-approval-form
  [{:keys [receipt-id receipt initial-data on-cancel on-expense-saved on-review-saved]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        form-error (use-subscribe [:expenses/entries-error])
  [validation-error set-validation-error!] (use-state nil)

        entity-spec (use-memo
                      #(get-expense-form-spec suppliers payers
                         {:new-supplier-default-display-name (receipt-merchant-name receipt)
                          :receipt-approval? true
                          :receipt receipt
                          :receipt-id receipt-id})
                      [suppliers payers receipt receipt-id])

        form-initial-values (use-memo
                              (fn []
                                (let [default-values {:currency "BAM"
                                                      :purchased_at (current-datetime-local)
                                                      :items [(new-line-item)]}]
                                  (merge default-values initial-data)))
                              [initial-data])

        rid-str (or (some-> receipt-id str) "unknown")]

    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (or validation-error form-error))))

      ($ base/initialize-form
        {:entity-name "expense"
         :entity-spec entity-spec
         :editing false
         :initial-values form-initial-values
         :prevent-default? true
         :keywordize-keys true

         :on-submit (fn [{:keys [values]}]
                      (let [validation-result (validate-expense-values values)]
                        (if (:ok? validation-result)
                          (do
                            (set-validation-error! nil)
                            (rf/dispatch
                              [:app.domain.frontend.expenses.events.receipts/approve-receipt
                               receipt-id
                               (prepare-expense-submit-values values)
                               on-expense-saved]))
                          (set-validation-error! (:error validation-result)))))

         :render-fn
         (fn [{:keys [form-id handle-submit dirty submitting? values] :as form-props}]
           (let [expense-valid-now? (:ok? (validate-expense-values values))
                 receipt-valid-now? (:ok? (validate-receipt-review-values values))
                 can-save-receipt? (and receipt-valid-now? (dirty? dirty))]
             ($ :form {:id form-id
                       :on-submit handle-submit}
               ($ form-fields
                 (merge form-props
                   {:entity-name "expense"
                    :editing false
                    :values values
                    :form-id form-id
                    :entity-spec entity-spec}))

               ($ :div {:class "flex justify-end gap-2"}
                 ($ :button {:id (str "btn-cancel-receipt-approve-" rid-str)
                             :type "button"
                             :class "ds-btn"
                             :disabled submitting?
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (when (fn? on-cancel) (on-cancel)))}
                   "Cancel")
                 ($ :button {:id (str "btn-save-receipt-" rid-str)
                             :type "button"
                             :class "ds-btn ds-btn-outline"
                             :disabled (or submitting? (not can-save-receipt?))
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (.stopPropagation e)
                                         (let [validation-result (validate-receipt-review-values values)]
                                           (if (:ok? validation-result)
                                             (do
                                               (set-validation-error! nil)
                                               (rf/dispatch
                                                 [:app.domain.frontend.expenses.events.receipts/save-receipt-review
                                                  receipt-id
                                                  (prepare-expense-submit-values values)
                                                  on-review-saved]))
                                             (set-validation-error! (:error validation-result)))))}
                   "Save receipt")
                 ($ :button {:id (str "btn-save-expense-" rid-str)
                             :type "submit"
                             :class "ds-btn ds-btn-primary"
                             :disabled (or submitting? (not expense-valid-now?))}
                   "Save expense")))))}))))

;; =============================================================================
;; Modal Wrappers
;; =============================================================================

(defui expense-add-form-modal
  [{:keys [receipt-id initial-data on-success on-review-saved on-cancel]}]
  (let [loading? (use-subscribe [:expenses/form-loading?])
        receipt (use-subscribe [:expenses/receipt receipt-id])
        default-supplier-display-name (use-memo
                                        #(when receipt (receipt-merchant-name receipt))
                                        [receipt])

        ;; If we have a receipt, normalize its data as initial values
        receipt-initial-data (use-memo
                               #(when receipt (normalize-receipt-data receipt))
                               [receipt])

        ;; Merge provided initial-data with receipt data
        merged-initial-data (use-memo
                              #(merge receipt-initial-data initial-data)
                              [receipt-initial-data initial-data])]
    (if receipt-id
      ($ receipt-approval-form
        {:receipt-id receipt-id
         :receipt receipt
         :initial-data merged-initial-data
         :on-cancel on-cancel
         :on-review-saved on-review-saved
         :on-expense-saved on-success})

      ($ expense-form-body
        {:mode :create
         :loading? loading?
         :initial-data merged-initial-data
         :new-supplier-default-display-name default-supplier-display-name
         :receipt-approval? false
         :receipt nil
         :receipt-id nil
         :on-cancel on-cancel
         :on-submit (fn [form-data]
                      (rf/dispatch [::expenses-events/create-entry-modal form-data on-success]))}))))

(defui expense-edit-form-modal
  "Edit expense modal using master-detail-form wrapper for detail orchestration."
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [expense-id-str (some-> expense-id str)
        ;; Subscribe to detail state
        detail-entity (use-subscribe [:expenses/entry expense-id-str])
        detail-loading? (boolean (use-subscribe [:expenses/entry-detail-loading?]))
        detail-error (use-subscribe [:expenses/entries-error])
        suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])

        ;; Memoize entity-spec
        entity-spec (use-memo
                      #(get-expense-form-spec suppliers payers)
                      [suppliers payers])

        ;; Default values for expense form
        ;; Memoized to keep identity stable across renders (prevents fork resets).
        default-values (use-memo
                         (fn []
                           {:currency "BAM"
                            :purchased_at (current-datetime-local)
                            :items [(new-line-item)]})
                         [])]

    ;; Load dependencies (suppliers/payers)
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ master-detail-form
      {:mode :edit
       :entity-name "expense"
       :entity-spec entity-spec
       :entity-id expense-id-str

       ;; Detail orchestration
       :load-detail! (fn [id] (rf/dispatch [::expenses-events/load-detail id]))
       :select-detail detail-entity
       :detail-loading? detail-loading?
       :detail-error detail-error

       ;; Data transformation
       :normalize-initial-data normalize-initial-data
       :validate-values validate-expense-values
       :prepare-submit-values prepare-expense-submit-values

       ;; Callbacks
       :on-submit (fn [prepared-data]
                    (rf/dispatch [::expenses-events/update-entry-modal expense-id-str prepared-data on-success]))
       :on-cancel on-cancel

       ;; Optional
       :initial-row-data initial-data
       :default-values default-values
       :button-text "Update Expense"})))
