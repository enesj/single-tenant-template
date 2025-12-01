(ns app.template.frontend.components.form.fields.array-input
  "Array input field component for array values"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.form.validation :as validation]
    [app.template.frontend.components.icons :refer [delete-icon plus-icon]]
    [uix.core :refer [$ defui use-state]]))

(defui array-input
  [{:keys [id label error required inline on-change value fork-errors]}]
  (let [base-class "ds-input ds-input-primary"
        label-class "ds-label"
        error-class "text-error"
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        current-values (if (vector? value) value (if (nil? value) [] [value]))
        [values set-values] (use-state current-values)
        add-item (fn []
                   (let [new-values (conj values "")]
                     (set-values new-values)
                     (on-change new-values)))
        remove-item (fn [index]
                      (let [new-values (vec (concat (take index values) (drop (inc index) values)))]
                        (set-values new-values)
                        (on-change new-values)))
        update-item (fn [index new-value]
                      (let [new-values (assoc values index new-value)]
                        (set-values new-values)
                        (on-change new-values)))]
    ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4" " flex flex-col items-start gap-4"))}
      ($ common/label {:text (str label " (Array)")
                       :for id
                       :required required
                       :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))})
      ($ :div {:class (when inline "flex-1 text-left")}
        ($ :div {:class "space-y-2"}
          (doseq [[index item-value] (map-indexed vector values)]
            ($ :div {:key index, :class "flex items-center gap-2"}
              ($ common/input
                {:class (str base-class " flex-1")
                 :type "text"
                 :value item-value
                 :placeholder (str "Item " (inc index))
                 :on-change #(update-item index (.. % -target -value))})
              ($ button
                {:btn-type :danger
                 :size "sm"
                 :shape "circle"
                 :on-click #(remove-item index)}
                ($ delete-icon))))
          ($ :div {:class "flex items-center mt-2"}
            ($ button
              {:btn-type :secondary
               :size "sm"
               :on-click add-item}
              "Add Item" ($ plus-icon))))
        (when field-error
          ($ :div {:class (str "mt-1 " error-class) :role "alert"}
            (if (string? field-error) field-error (:message field-error))))
        ($ :div {:class "text-xs text-gray-500 mt-1"}
          "Add multiple values by clicking 'Add Item'")))))
