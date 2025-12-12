(ns app.domain.frontend.expenses.components.form-fields
  "Custom form fields for expense form"
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.icons :refer [delete-icon]]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

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
   :line_total ""})

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
  "When qty and unit price are both present and line-total is blank, auto-calc it."
  [item]
  (let [qty-num (safe-parse-number (:qty item))
        unit-num (safe-parse-number (:unit_price item))
        line-str (:line_total item)]
    (if (and (number? qty-num) (number? unit-num) (str/blank? (str line-str)))
      (let [product (* qty-num unit-num)]
        (assoc item :line_total (format-decimal product)))
      item)))

(defn update-line-item
  "Updates a specific field in a line item and recalculates total if possible."
  [items item-id key value]
  (mapv (fn [item]
          (if (= item-id (:id item))
            (-> item
              (assoc key value)
              recalc-line-total-if-possible)
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
        ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                    :type "button"
                    :on-click add-item}
          "Add line item"))
      ($ :div {:class "overflow-x-auto"}
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
                      (cond-> {:class "ds-input ds-input-bordered w-full"
                               :value (or val "")
                               :type (name (or type :text))
                               :on-change (handle-line-change item-id col-id)}
                        placeholder (assoc :placeholder placeholder)
                        step (assoc :step step)
                        min-val (assoc :min min-val)))))
                ($ :td
                  ($ :button {:class "text-xs text-error"
                              :type "button"
                              :on-click #(remove-item item-id)}
                    "Remove")))))))
      (when error
        ($ :div {:class "text-error text-sm mt-1"} error)))))

(def ^:private amount-tolerance 0.01)

(defui total-amount-input
  [{:keys [value on-change values error]}]
  (let [items (:items values)
        computed-total (line-items-total items)
        parsed-total (safe-parse-number value)
        total-diff (when (and (number? parsed-total) (number? computed-total) (pos? computed-total))
                     (js/Math.abs (- parsed-total computed-total)))
        total-mismatch? (and total-diff (> total-diff amount-tolerance))]
    ($ :div {:class "space-y-1"}
      ($ :div {:class "flex gap-2"}
        ($ :input {:class "ds-input ds-input-bordered w-full"
                   :type "number"
                   :step "0.01"
                   :value (or value "")
                   :on-change #(on-change (.. % -target -value))})
        (when (pos? computed-total)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :type "button"
                      :on-click #(on-change (format-decimal computed-total))}
            "Use total")))
      (when (pos? computed-total)
        ($ :p {:class "text-xs text-base-content/60"}
          (str "Line items total: " (shared/format-value computed-total "0" false))
          (when total-mismatch?
            ($ :span {:class "text-error ml-2"} "(does not match total)"))))
      (when error
        ($ :div {:class "text-error text-sm mt-1"} error)))))
