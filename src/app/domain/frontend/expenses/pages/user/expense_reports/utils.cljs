(ns app.domain.frontend.expenses.pages.user.expense-reports.utils)

(defn ->number
  [value]
  (cond
    (number? value) value
    (string? value) (let [parsed (js/parseFloat value)]
                      (when-not (js/isNaN parsed) parsed))
    :else nil))

(defn format-money
  [amount currency]
  (let [amount* (->number amount)
        currency* (or (some-> currency name)
                    (some-> currency str)
                    "USD")]
    (if (nil? amount*)
      "—"
      (try
        (.toLocaleString (js/Number amount*) "en-US"
          #js {:style "currency"
               :currency currency*
               :minimumFractionDigits 2
               :maximumFractionDigits 2})
        (catch :default _
          (str currency* " " (.toFixed (js/Number amount*) 2)))))))

(defn format-int
  [value]
  (let [n (->number value)]
    (if (nil? n)
      "0"
      (.toLocaleString (js/Number n) "en-US"))))

(defn supplier-options
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [supplier-id (some-> (:supplier_id row) str)
              supplier-name (or (:supplier_name row) "Unknown supplier")]
          (if (seq supplier-id)
            (if (contains? acc supplier-id)
              acc
              (assoc acc supplier-id supplier-name))
            acc)))
      {})
    (mapv (fn [[id name]] {:id id :name name}))
    (sort-by :name)))

(defn aggregate-day-pattern
  [groups]
  (->> (or groups [])
    (mapcat (fn [group] (or (:days group) [])))
    (reduce
      (fn [acc row]
        (let [iso-day (int (or (:iso_day_of_week row) 0))
              base (get acc iso-day {:iso_day_of_week iso-day
                                     :day_key (:day_key row)
                                     :day_label (:day_label row)
                                     :total_amount 0
                                     :expense_count 0})]
          (assoc acc iso-day
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :iso_day_of_week)
    vec))

(defn aggregate-size-buckets
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [bucket-key (:bucket_key row)
              base (get acc bucket-key {:bucket_key bucket-key
                                        :bucket_label (:bucket_label row)
                                        :sort_order (or (:sort_order row) 0)
                                        :total_amount 0
                                        :expense_count 0})]
          (assoc acc bucket-key
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :sort_order)
    vec))

(defn aggregate-heatmap
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [day (:day row)
              base (get acc day {:day day
                                 :iso_day_of_week (:iso_day_of_week row)
                                 :total_amount 0
                                 :expense_count 0})]
          (assoc acc day
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :day)
    vec))

(defn heat-intensity-class
  [ratio]
  (cond
    (>= ratio 0.8) "bg-primary text-primary-content shadow-sm scale-105 font-bold"
    (>= ratio 0.55) "bg-primary/80 text-primary-content font-semibold"
    (>= ratio 0.3) "bg-primary/40 text-base-content font-medium"
    (pos? ratio) "bg-primary/10 text-base-content"
    :else "bg-base-100 text-base-content/30 hover:bg-base-200 transition-colors"))


