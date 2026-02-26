(ns app.domain.frontend.expenses.components.expense-form.normalization
  "Normalization + validation helpers for the admin expense form.

  Shared between create, edit, and receipt approval UIs."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :refer [current-datetime-local
                                                                         format-decimal
                                                                         line-items-total
                                                                         new-line-item
                                                                         safe-parse-number]]
    [app.shared.adapters.normalization :as normalization]
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

(defn prepare-line-items
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
  (let [expense (normalization/convert-db-keys->app-keys expense)]
    (letfn [(normalize-line-item [item]
              (let [item (if (map? item) item {})
                    id (or (:id item)
                         (:expense-item-id item)
                         (random-uuid))
                    raw-label (or (:raw-label item) "")
                    qty (:qty item)
                    unit-price (:unit-price item)
                    line-total (:line-total item)
                    auto? (if (contains? item :line-total-auto?)
                            (not (false? (:line-total-auto? item)))
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
      (let [supplier-id (or (:supplier-id expense) (:expenses/supplier-id expense))
            payer-id (or (:payer-id expense) (:expenses/payer-id expense))
            purchased-at (or (:purchased-at expense) (:expenses/purchased-at expense))
            total-amount (or (:total-amount expense) (:expenses/total-amount expense))
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
         :items normalized-items}))))

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
