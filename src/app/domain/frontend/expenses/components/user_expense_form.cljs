(ns app.domain.frontend.expenses.components.user-expense-form
  "User-facing expense modal forms.

  Similar UX to the admin expenses modal forms, but wired to user-scoped
  events/endpoints (\"/api/v1/expenses\")."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 format-decimal
                                                                 line-items-input
                                                                 line-items-total
                                                                 new-line-item
                                                                 safe-parse-number]]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.template.frontend.components.form :refer [form]]
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

(defn get-expense-form-spec
  [suppliers payers]
  [{:id :supplier_id
    :type :select
    :label "Supplier"
    :required true
    :placeholder "Select supplier"
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
    :columns line-item-columns}])

;; =============================================================================
;; Helpers
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

(defn- normalize-initial-data
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

;; =============================================================================
;; Form Body
;; =============================================================================

(defui user-expense-form-body
  [{:keys [mode initial-data on-submit on-cancel]}]
  (let [suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        form-error (use-subscribe [:user-expenses/form-error])
        [validation-error set-validation-error!] (use-state nil)

        ;; Memoize entity-spec to avoid recreating on every render.
        ;; Only rebuild when suppliers or payers content actually changes.
        entity-spec (use-memo
                      #(get-expense-form-spec suppliers payers)
                      [suppliers payers])

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
                        (let [supplier-id (:supplier_id values)
                              payer-id (:payer_id values)
                              purchased-at (:purchased_at values)
                              currency (:currency values)
                              notes (:notes values)
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
                            (set-validation-error! "Supplier, payer, and date are required.")

                            (empty? prepared-items)
                            (set-validation-error! "Add at least one line item with a label and total.")

                            (or (nil? effective-total) (<= effective-total 0))
                            (set-validation-error! "Enter a total amount greater than 0.")

                            total-mismatch?
                            (set-validation-error!
                              (str "Total (" (or (format-decimal effective-total) effective-total)
                                ") must match line items (" (or (format-decimal computed-total) computed-total) ")."))

                            :else
                            (do
                              (set-validation-error! nil)
                              (on-submit {:supplier_id supplier-id
                                          :payer_id payer-id
                                          :purchased_at purchased-at
                                          :currency currency
                                          :notes notes
                                          :total_amount effective-total
                                          :items prepared-items})))))]

    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

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
         :button-text (if (= mode :edit) "Update Expense" "Save Expense")}))))

;; =============================================================================
;; Modal wrappers
;; =============================================================================

(defui user-expense-add-form-modal
  [{:keys [on-success on-cancel]}]
  ($ user-expense-form-body
    {:mode :create
     :initial-data {:purchased_at (current-datetime-local)
                    :items [(new-line-item)]}
     :on-cancel on-cancel
     :on-submit (fn [form-data]
                  (rf/dispatch [:user-expenses/create-expense-modal form-data on-success]))}))

(defui user-expense-edit-form-modal
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [expense-id* (some-> expense-id str)
        [requested? set-requested!] (use-state false)
        current-expense (use-subscribe [:user-expenses/current-expense])
        loading? (boolean (use-subscribe [:user-expenses/current-expense-loading?]))
        error (use-subscribe [:user-expenses/current-expense-error])
        current-id (some-> (or (:id current-expense) (:expenses/id current-expense)) str)
        detail-loaded? (and expense-id* current-id (= current-id expense-id*))
        effective-data (if detail-loaded? current-expense initial-data)
        normalized-data (use-memo
                          #(normalize-initial-data effective-data)
                          [effective-data])]

    (use-effect
      (fn []
        (when expense-id*
          (rf/dispatch [:user-expenses/fetch-expense expense-id*]))
        (set-requested! true)
        js/undefined)
      [expense-id*])

    ($ :div {:class "space-y-2"}
      (when (and requested? error)
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span error)))

      (cond
        detail-loaded?
        ($ user-expense-form-body
          {:key (str "user-expense-edit-" expense-id*)
           :mode :edit
           :initial-data normalized-data
           :on-cancel on-cancel
           :on-submit (fn [form-data]
                        (rf/dispatch [:user-expenses/update-expense-modal expense-id* form-data on-success]))})

        (or (not requested?) loading?)
        ($ :div {:class "text-sm text-base-content/60"}
          "Loading expense…")

        :else
        ;; If detail fetch fails, fall back to the list row data (may not include items).
        ($ user-expense-form-body
          {:key (str "user-expense-edit-" expense-id* "-fallback")
           :mode :edit
           :initial-data normalized-data
           :on-cancel on-cancel
           :on-submit (fn [form-data]
                        (rf/dispatch [:user-expenses/update-expense-modal expense-id* form-data on-success]))})))))
