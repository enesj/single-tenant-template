(ns app.domain.frontend.expenses.components.user-expense-form.normalization
  "Normalization + validation helpers for the user expense form.

  This namespace is shared between the different UI wrappers (create, edit,
  receipt approval)."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local
                                                                 format-decimal
                                                                 line-items-total
                                                                 new-line-item
                                                                 safe-parse-number]]
    [clojure.string :as str]))

(def ^:private amount-tolerance 0.01)

(defn- pad-two
  [value]
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

(defn prepare-line-items
  "Internal helper: prepare/validate raw line items from the UI.

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
      {:ok? false
       :error (str "Total (" (or (format-decimal effective-total) effective-total)
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
