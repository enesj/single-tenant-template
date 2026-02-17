(ns app.domain.frontend.expenses.pages.user.expense-reports.utils
  (:require
    [clojure.string :as str]))

(defn ->number
  [value]
  (cond
    (number? value) value
    (string? value) (let [parsed (js/parseFloat value)]
                      (when-not (js/isNaN parsed) parsed))
    :else nil))

(defn normalize-id-list
  [value]
  (let [values (cond
                 (nil? value) []
                 (sequential? value) value
                 :else [value])]
    (->> values
      (map #(some-> % str str/trim))
      (remove str/blank?)
      distinct
      vec)))

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

(defn format-amount-only
  [amount]
  (let [amount* (->number amount)]
    (if (nil? amount*)
      "—"
      (try
        (.toLocaleString (js/Number amount*) "en-US"
          #js {:minimumFractionDigits 2
               :maximumFractionDigits 2})
        (catch :default _
          (.toFixed (js/Number amount*) 2))))))

(defn format-percent
  [value]
  (let [n (->number value)]
    (if (nil? n)
      "—"
      (str (.toFixed (js/Number n) 1) "%"))))

(defn format-int
  [value]
  (let [n (->number value)]
    (if (nil? n)
      "0"
      (.toLocaleString (js/Number n) "en-US"))))

(defn month-label
  [month]
  (let [[year month-part] (some-> month str (str/split #"-"))
        month-idx (some-> month-part js/parseInt)
        short-month (get ["" "Jan" "Feb" "Mar" "Apr" "May" "Jun"
                          "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
                      month-idx
                      month)]
    (if (and year month-part)
      (str short-month " " year)
      (or month "—"))))

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

(defn month-options
  [by-month month-a month-b]
  (let [months (->> (or by-month [])
                 (keep :month)
                 (map str)
                 distinct
                 sort
                 reverse
                 vec)
        selected (->> [month-a month-b]
                   (keep #(some-> % str/trim))
                   (remove str/blank?)
                   vec)]
    (vec (distinct (concat selected months)))))

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

(defn aggregate-category-allocation
  [rows]
  (let [category-currency-rows (->> (or rows [])
                                 (reduce
                                   (fn [acc row]
                                     (let [category-key (:category_key row)
                                           currency (:currency row)]
                                       (if (seq (str category-key))
                                         (assoc acc [category-key currency]
                                           {:category_key category-key
                                            :category_name (:category_name row)
                                            :total_amount (or (->number (:total_amount row)) 0)
                                            :line_count (or (->number (:line_count row)) 0)})
                                         acc)))
                                   {})
                                 vals)
        aggregated (->> category-currency-rows
                     (reduce
                       (fn [acc row]
                         (let [category-key (:category_key row)
                               base (get acc category-key {:category_key category-key
                                                           :category_name (:category_name row)
                                                           :total_amount 0
                                                           :line_count 0})]
                           (assoc acc category-key
                             (-> base
                               (update :total_amount + (or (->number (:total_amount row)) 0))
                               (update :line_count + (or (->number (:line_count row)) 0))))))
                       {})
                     vals
                     vec)
        total (reduce + 0 (map #(or (->number (:total_amount %)) 0) aggregated))]
    (->> aggregated
      (mapv
        (fn [row]
          (let [row-total (or (->number (:total_amount row)) 0)
                allocation-pct (if (pos? total) (* 100 (/ row-total total)) 0)]
            (assoc row :allocation_pct allocation-pct))))
      (sort-by :total_amount >)
      vec)))

(defn aggregate-subcategory-allocation
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [category-key (:category_key row)
              subcategory-key (or (:subcategory_key row) "uncategorized")
              subcategory-name (or (:subcategory_name row) "Uncategorized")
              subcategory-total (or (->number (:subcategory_total_amount row)) 0)]
          (if (seq (str category-key))
            (let [base (get-in acc [category-key subcategory-key]
                         {:id subcategory-key
                          :name subcategory-name
                          :total_amount 0})]
              (assoc-in acc [category-key subcategory-key]
                        (-> base
                          (assoc :name subcategory-name)
                          (update :total_amount + subcategory-total))))
            acc)))
      {})
    (reduce-kv
      (fn [acc category-key sub-map]
        (assoc acc category-key
          (->> (vals sub-map)
            (sort-by :total_amount >)
            vec)))
      {})))

(defn bucket-range
  [bucket-key]
  (case bucket-key
    "lt_10" [nil 10]
    "10_25" [10 25]
    "25_50" [25 50]
    "50_100" [50 100]
    "100_200" [100 200]
    "gte_200" [200 nil]
    [nil nil]))

(defn in-bucket?
  [amount bucket-key]
  (let [[min-value max-value] (bucket-range bucket-key)
        amount* (or (->number amount) 0)]
    (and (or (nil? min-value) (<= min-value amount*))
      (or (nil? max-value) (< amount* max-value)))))

(defn filter-top-items-by-bucket
  [rows bucket-key]
  (if (str/blank? (str (or bucket-key "")))
    (vec (or rows []))
    (->> (or rows [])
      (filter #(in-bucket? (:total_amount %) bucket-key))
      vec)))

(defn heat-intensity-class
  [ratio]
  (cond
    (>= ratio 0.8) "bg-primary text-primary-content shadow-sm scale-105 font-bold"
    (>= ratio 0.55) "bg-primary/80 text-primary-content font-semibold"
    (>= ratio 0.3) "bg-primary/40 text-base-content font-medium"
    (pos? ratio) "bg-primary/10 text-base-content"
    :else "bg-base-100 text-base-content/30 hover:bg-base-200 transition-colors"))

(defn default-sort-direction
  [sort-type]
  (if (= sort-type :number) :desc :asc))

(defn toggle-sort-config
  [current-sort column sort-type]
  (if (= column (:column current-sort))
    (update current-sort :direction #(if (= % :asc) :desc :asc))
    {:column column
     :direction (default-sort-direction sort-type)
     :type sort-type}))

(defn blank-sort-value?
  [value]
  (or (nil? value)
    (and (string? value) (str/blank? value))))

(defn compare-sort-values
  [left right sort-type]
  (cond
    (and (blank-sort-value? left) (blank-sort-value? right)) 0
    (blank-sort-value? left) 1
    (blank-sort-value? right) -1
    (= sort-type :number)
    (let [left-num (->number left)
          right-num (->number right)]
      (cond
        (and (nil? left-num) (nil? right-num)) 0
        (nil? left-num) 1
        (nil? right-num) -1
        :else (compare left-num right-num)))
    :else
    (compare (-> left str str/lower-case)
      (-> right str str/lower-case))))

(defn sort-rows-by-config
  [rows {:keys [column direction type]}]
  (let [direction-multiplier (if (= direction :desc) -1 1)]
    (->> (or rows [])
      (map-indexed (fn [idx row] {:idx idx :row row}))
      (sort (fn [a b]
              (let [cmp (compare-sort-values
                          (get (:row a) column)
                          (get (:row b) column)
                          type)]
                (if (zero? cmp)
                  (compare (:idx a) (:idx b))
                  (* direction-multiplier cmp)))))
      (mapv :row))))

(defn sortable-column-label
  [label sort-config column]
  (let [active? (= column (:column sort-config))
        indicator (if (= :asc (:direction sort-config)) "↑" "↓")]
    (if active?
      (str label " " indicator)
      label)))