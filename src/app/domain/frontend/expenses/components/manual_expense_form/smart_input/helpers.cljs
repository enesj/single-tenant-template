(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
  "Pure helper functions for the smart expense input."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [safe-parse-number]]
    [clojure.string :as str]))

(defn payer-default-id
  [payers]
  (or (some #(when (or (:is-default %) (:isDefault %)) (:id %)) payers)
    (:id (first payers))))

(defn compute-items-total
  "Sum line totals for all items."
  [items]
  (reduce (fn [acc item]
            (let [qty (or (safe-parse-number (:qty item)) 1)
                  price (or (safe-parse-number (:unit-price item)) 0)]
              (+ acc (* qty price))))
    0 items))

(defn prepare-submit-items
  "Convert internal items to backend-expected format."
  [items]
  (vec
    (keep (fn [{:keys [label qty unit-price]}]
            (let [q (or (safe-parse-number qty) 1)
                  p (or (safe-parse-number unit-price) 0)
                  total (* q p)]
              (when (and (not (str/blank? (str label))) (pos? total))
                {:raw_label (str label)
                 :qty q
                 :unit_price p
                 :line_total total})))
      items)))

(defn prepare-submit-values
  [{:keys [items context currency purchased-at payer-id notes]}]
  (let [prepared (prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond-> {:payer_id payer-id
             :purchased_at purchased-at
             :currency currency
             :total_amount total
             :items prepared}
      (:supplier context) (assoc :supplier_id (get-in context [:supplier :id]))
      (:store context) (assoc :store_id (get-in context [:store :id]))
      (:category context) (assoc :expense_category_id (get-in context [:category :id]))
      (not (str/blank? notes)) (assoc :notes notes))))

(defn entity-type-label
  "Translated entity type label."
  [t entity-type]
  (case entity-type
    :supplier (t :smart-expense/entity-supplier)
    :store    (t :smart-expense/entity-store)
    :category (t :smart-expense/entity-category)
    :article  (t :smart-expense/entity-article)
    ""))

(defn validate-form
  [t {:keys [items context payer-id]}]
  (let [prepared (prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond
      (empty? prepared)
      {:ok? false :error (t :smart-expense/err-no-items)}

      (empty? context)
      {:ok? false :error (t :smart-expense/err-no-context)}

      (str/blank? (str payer-id))
      {:ok? false :error (t :smart-expense/err-no-payer)}

      (<= total 0)
      {:ok? false :error (t :smart-expense/err-no-total)}

      :else {:ok? true})))

(def context-search-order
  [:supplier :store :category])

(defn focused-search-types
  [context article-mode?]
  (if article-mode?
    [:article]
    (vec (concat
           (remove #(contains? context %) context-search-order)
           [:article]))))

(defn search-placeholder
  [t context active-search? article-mode?]
  (if article-mode?
    (t :smart-expense/article-mode-ph)
    (let [types (focused-search-types context false)
          labels (map #(entity-type-label t %) types)
          prefix (if active-search?
                   (t :smart-expense/search-prefix)
                   (t :smart-expense/start-with-prefix))
          or-conn (t :smart-expense/or-connector)]
      (str prefix
        (case (count labels)
          0 (entity-type-label t :article)
          1 (first labels)
          2 (str (first labels) or-conn (second labels))
          (str (str/join ", " (butlast labels)) or-conn (last labels)))
        "..."))))

(defn current-related-context
  [context]
  (cond
    (:store context) {:entity-type :store :entity-id (get-in context [:store :id])}
    (:supplier context) {:entity-type :supplier :entity-id (get-in context [:supplier :id])}
    :else nil))

(defn build-supplier-color-map
  "Build `{supplier-id-string → palette-slot}` so that supplier chips and
   their stores share a hue. Slots are assigned in first-seen supplier
   order, skipping missing ids and duplicate suppliers, so the first N
   valid suppliers get distinct colors and assignment stays stable across
   renders as long as the supplier list order does."
  [suppliers palette]
  (let [n (count palette)]
    (if (or (zero? n) (empty? suppliers))
      {}
      (->> suppliers
        (keep (fn [supplier]
                (some-> (:id supplier) str)))
        distinct
        (map-indexed (fn [i supplier-id]
                       [supplier-id (nth palette (mod i n))]))
        (into {})))))

(defn- store-supplier-id
  [item]
  (or (get-in item [:entity :supplier-id])
    (get-in item [:entity :supplier_id])
    (:supplier-id item)
    (:supplier_id item)))

(defn colorize-quick-pick-groups
  "Inject a per-item `:chip-class` so supplier rows and the store rows
   that belong to them share a hue. Categories/articles pass through
   untouched. Items whose supplier is missing from the color map (e.g.
   stores with no supplier id) are left without an override and fall
   back to the default chip style."
  [groups supplier-color-map]
  (mapv
    (fn [{:keys [entity-type items] :as group}]
      (assoc group :items
        (mapv
          (fn [item]
            (let [supplier-id-str (case entity-type
                                    :supplier (some-> (:id item) str)
                                    :store (some-> (store-supplier-id item) str)
                                    nil)
                  slot (when supplier-id-str
                         (get supplier-color-map supplier-id-str))
                  klass (when slot
                          (case entity-type
                            :supplier (:supplier slot)
                            :store (:store slot)
                            nil))]
              (cond-> item
                klass (assoc :chip-class klass))))
          items)))
    groups))

(defn supplier-chip-class
  "Return the supplier-variant chip class for the given supplier id, or
   nil if the id is not in the color map."
  [supplier-color-map supplier-id]
  (some-> (get supplier-color-map (some-> supplier-id str)) :supplier))

(defn store-chip-class
  "Return the store-variant chip class for the given supplier id, or nil
   if the supplier id is not in the color map."
  [supplier-color-map supplier-id]
  (some-> (get supplier-color-map (some-> supplier-id str)) :store))
