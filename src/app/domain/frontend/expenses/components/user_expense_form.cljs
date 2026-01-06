(ns app.domain.frontend.expenses.components.user-expense-form
  "User-facing expense modal forms.

  Similar UX to the admin expenses modal forms, but wired to user-scoped
  events/endpoints (\"/api/v1/expenses\").

  Uses the master-detail-form wrapper for edit modal orchestration."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 format-decimal
                                                                 line-items-input
                                                                 line-items-total
                                                                 new-line-item
                                                                 safe-parse-number]]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.form.master-detail :refer [master-detail-form]]
    [app.template.frontend.components.modal :refer [modal]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Specs
;; =============================================================================

(def ^:private currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def ^:private line-item-columns
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

(defn- payer-default?
  [payer]
  (boolean
    (or (:is_default payer)
      (:is-default payer)
      (:isDefault payer))))

(defn- default-payer-id
  [payers]
  (let [payers (or payers [])]
    (or (some (fn [p]
                (when (payer-default? p)
                  (:id p)))
          payers)
      (:id (first payers)))))

(defn- dom-id
  [v]
  (cond
    (string? v) v
    (keyword? v) (name v)
    (symbol? v) (name v)
    (nil? v) nil
    :else (str v)))

(defui user-supplier-select-with-inline-create
  [{:keys [id label error required inline class on-change value form-id formId field-spec]}]
  (let [creating? (boolean (use-subscribe [:user-expenses/form-loading?]))
        create-error (use-subscribe [:user-expenses/form-error])
        [open? set-open!] (use-state false)
        [display-name set-display-name!] (use-state "")
        [local-error set-local-error!] (use-state nil)

        form-id* (or form-id formId)
        field-key (or (some-> (:id field-spec) name)
                    (some-> id dom-id)
                    "supplier_id")
        base-id (or form-id* "user-expense-form")

        default-display-name (some-> (or (:create-default-display-name field-spec)
                                       (:new-supplier-default-display-name field-spec)
                                       (:default-display-name field-spec))
                               str
                               str/trim
                               not-empty)

        select-id (or (when (string? id) id)
                    (str base-id "-" field-key "-select"))
        error-id (str select-id "-error")
        add-btn-id (str "btn-new-supplier-" base-id "-" field-key)

        modal-id (str base-id "-" field-key "-create-supplier-modal")
        name-input-id (str base-id "-" field-key "-create-supplier-display-name-input")
        cancel-btn-id (str "btn-cancel-supplier-create-" base-id "-" field-key)
        save-btn-id (str "btn-save-supplier-create-" base-id "-" field-key)

        label-class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))
        wrapper-class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))

        field-error-msg (cond
                          (nil? error) nil
                          (string? error) error
                          (map? error) (or (:message error) (str error))
                          :else (str error))
        opts (vec (or (:options field-spec) []))

        open-modal (fn []
                     (set-local-error! nil)
                     (set-display-name! (or default-display-name ""))
                     (set-open! true))
        close-modal (fn []
                      (set-open! false)
                      (set-display-name! "")
                      (set-local-error! nil))
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
                       (rf/dispatch [:user-expenses/create-supplier-modal
                                     {:display_name name*}
                                     (fn [supplier]
                                       (when (and (map? supplier) (fn? on-change))
                                         (when-let [new-id (:id supplier)]
                                           (on-change (str new-id))))
                                       (close-modal))])))))]

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
            "New supplier"))
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

(defn get-expense-form-spec
  ([suppliers payers]
   (get-expense-form-spec suppliers payers nil))
  ([suppliers payers {:keys [receipt-approval? supplier-guess]}]
   [{:id :supplier_id
     :type :select
     :component user-supplier-select-with-inline-create
     :label "Supplier"
     :required true
     :placeholder "Select supplier"
     :create-default-display-name (when receipt-approval?
                                    (some-> supplier-guess str str/trim not-empty))
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
     :type :datetime-local
     :label "Purchased at"
     :required true}
    {:id :total_amount
     :component app.domain.frontend.expenses.components.form-fields/total-amount-input
     :label "Total amount"
     :required true}
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
     :component line-items-input
     :label "Line Items"
     :columns line-item-columns
     :style (if receipt-approval? {:maxHeight "260px"} {:maxHeight "300px"})
     :overflow-y-class "overflow-y-auto"
     :scrollbar-gutter-stable? true}]))

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

        ;; Worker persists a wrapper map like:
        ;; {:provider ... :response ... :extraction {:merchant ... :totals ... :items ...}}
        extraction (or (:extraction raw-extract)
                     (:receipt-extraction raw-extract))
        extract (if (map? extraction) extraction raw-extract)

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
  "Normalize an expense entity (from the shared entity store) into form initial values."
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
;; Form Body
;; =============================================================================

(defui user-expense-form-body
  [{:keys [mode initial-data on-submit on-cancel receipt-approval? supplier-guess]}]
  (let [suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        form-error (use-subscribe [:user-expenses/form-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Memoize entity-spec to avoid recreating on every render.
        ;; Only rebuild when suppliers or payers content actually changes.
        entity-spec (use-memo
                      #(get-expense-form-spec suppliers payers
                         {:receipt-approval? receipt-approval?
                          :supplier-guess supplier-guess})
                      [suppliers payers receipt-approval? supplier-guess])

        ;; Memoize initial values so fork/form doesn't reset on every render.
        ;; Use initial-data identity as the dependency (it's passed from parent).
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

    ($ :div {:class "space-y-4"}
      (when (or validation-error form-error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (or validation-error form-error))))

      ($ form
        {:entity-name "user-expense"
         :entity-spec entity-spec
         :editing (= mode :edit)
         :initial-values form-initial-values
         :on-cancel on-cancel
         :on-submit handle-submit
         :save-disabled? (fn [values]
                           (empty? (prepare-line-items (:items values))))
         :button-text (if (= mode :edit) "Update Expense" "Save Expense")}))))

;; =============================================================================
;; Modal wrappers
;; =============================================================================

(defui user-expense-add-form-modal
  [{:keys [receipt-id receipt on-success on-cancel]}]
  (let [payers (or (use-subscribe [:user-expenses/payers]) [])
        payers-loading? (boolean (use-subscribe [:user-expenses/payers-loading?]))
        [requested? set-requested!] (use-state false)
        [prepared-initial-data set-prepared-initial-data!] (use-state nil)
        supplier-guess (some-> receipt :supplier-guess)
        receipt-initial-data (use-memo
                               #(when receipt (normalize-receipt-data receipt))
                               [receipt])
        merged-initial-data (use-memo
                              #(merge {:purchased_at (current-datetime-local)
                                       :items [(new-line-item)]}
                                 receipt-initial-data)
                              [receipt-initial-data])]

    ;; Load dependencies (suppliers/payers) early so we can set defaults before the form mounts.
    (use-effect
      (fn []
        (set-requested! true)
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ;; Lock in initial values once payers have loaded so Fork doesn't reset mid-edit.
    (use-effect
      (fn []
        (when (and requested?
                (nil? prepared-initial-data)
                (or (seq payers) (not payers-loading?)))
          (let [existing-payer-id (some-> (:payer_id merged-initial-data) str str/trim not-empty)
                default-id (some-> (default-payer-id payers) str str/trim not-empty)
                prepared (cond-> merged-initial-data
                           (and (nil? existing-payer-id) default-id)
                           (assoc :payer_id default-id))]
            (set-prepared-initial-data! prepared)))
        js/undefined)
      [requested? prepared-initial-data payers payers-loading? merged-initial-data])

    (if (nil? prepared-initial-data)
      ($ :div {:class "flex justify-center p-6"}
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))

      ($ user-expense-form-body
        {:mode :create
         :receipt-approval? (boolean receipt-id)
         :supplier-guess supplier-guess
         :initial-data prepared-initial-data
         :on-cancel on-cancel
         :on-submit (fn [form-data]
                      (if receipt-id
                        (rf/dispatch [:user-expenses/approve-receipt receipt-id form-data on-success])
                        (rf/dispatch [:user-expenses/create-expense-modal form-data on-success])))}))))

(defui user-expense-edit-form-modal
  "Edit user expense modal using master-detail-form wrapper for detail orchestration."
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [expense-id-str (some-> expense-id str)
        ;; Subscribe to detail state
        current-expense (use-subscribe [:user-expenses/current-expense])
        detail-loading? (boolean (use-subscribe [:user-expenses/current-expense-loading?]))
        detail-error (use-subscribe [:user-expenses/current-expense-error])
        suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])

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
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ master-detail-form
      {:mode :edit
       :entity-name "user-expense"
       :entity-spec entity-spec
       :entity-id expense-id-str

       ;; Detail orchestration
       :load-detail! (fn [id] (rf/dispatch [:user-expenses/fetch-expense id]))
       :select-detail current-expense
       :detail-loading? detail-loading?
       :detail-error detail-error

       ;; Data transformation
       :normalize-initial-data normalize-initial-data
       :validate-values validate-expense-values
       :prepare-submit-values prepare-expense-submit-values

       ;; Callbacks
       :on-submit (fn [prepared-data]
                    (rf/dispatch [:user-expenses/update-expense-modal expense-id-str prepared-data on-success]))
       :on-cancel on-cancel

       ;; Optional
       :initial-row-data initial-data
       :default-values default-values
       :button-text "Update Expense"})))
