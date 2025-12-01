(ns app.shared.frontend.components.action-buttons
  "Shared action button primitive for list/table rows"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :as uix :refer [$ defui]]))

(defn- tooltip-attrs [{:keys [tooltip tooltip-class tooltip-position wrapper-class]}]
  (let [position (or tooltip-position "top")
        message (or tooltip "Delete this record")]
    (cond-> {:class (str "ds-tooltip ds-tooltip-" position " pointer-events-auto z-50")
             :title message
             :data-tip message}
      tooltip-class (update :class #(str % " " tooltip-class))
      (nil? tooltip) (assoc :data-tip message)
      wrapper-class (update :class #(str % " " wrapper-class)))))

(defn- render-button [{:keys [id btn-type shape class disabled? aria-disabled? icon on-click]}]
  ($ button
    (cond-> {:id id
             :btn-type (or btn-type :primary)
             :on-click on-click}
      shape (assoc :shape shape)
      class (assoc :class class)
      (some? disabled?) (assoc :disabled disabled?)
      (some? aria-disabled?) (assoc :aria-disabled aria-disabled?))
    icon))

(defui action-buttons
  "Render a standard edit/delete action cluster with optional custom actions.
   `:edit` and `:delete` accept maps describing button behavior."
  [{:keys [item edit delete custom-actions container-class]
    :or {container-class "flex items-center gap-2"}}]
  (let [edit-props (when edit (merge {:btn-type :primary} edit))
        delete-props (when delete (merge {:btn-type :error} delete))
        edit-visible? (get edit-props :visible? true)
        delete-visible? (get delete-props :visible? true)
        edit-disabled? (true? (:disabled? edit-props))
        delete-disabled? (true? (:disabled? delete-props))
        edit-click (if edit-disabled?
                     (or (:on-disabled-click edit-props)
                       (fn [e] (.stopPropagation e)))
                     (:on-click edit-props))
        delete-click (if delete-disabled?
                       (or (:on-disabled-click delete-props)
                         (fn [e] (.stopPropagation e)))
                       (:on-click delete-props))]
    ($ :div {:class container-class}
      (when (and edit-props edit-visible?)
        (render-button (assoc edit-props :on-click edit-click)))
      (when (and delete-props delete-visible?)
        ($ :div (tooltip-attrs delete-props)
          (render-button (assoc delete-props :on-click delete-click))))
      (when custom-actions
        (custom-actions item)))))
