(ns app.domain.frontend.expenses.components.form-fields.helpers
  "Helper functions for expense form fields"
  (:require
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]))

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

(defn new-line-item
  "Creates a new empty line item with a random ID."
  []
  {:id (str (random-uuid))
   :raw_label ""
   :qty ""
   :unit_price ""
   :line_total ""
   :line_total_auto? true})

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

(defn options-from-items
  [items label-fn]
  (->> (or items [])
    (map (fn [item]
           {:value (:id item)
            :label (label-fn item)}))
    (remove (fn [opt] (nil? (:value opt))))
    (sort-by :label)
    vec))


