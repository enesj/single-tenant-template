(ns app.template.frontend.components.dropdown.action
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.dropdown.core :as core]
    [app.template.frontend.components.dropdown.group :refer [dropdown-group]]
    [app.template.frontend.components.dropdown.item :refer [dropdown-item]]
    [uix.core :refer [$ defui]]))

(defui action-dropdown
  "Pre-configured dropdown for action menus with consistent styling"
  [{:keys [entity-id trigger-label actions loading-states position class draggable?]
    :or {trigger-label "⋮"
         position :portal
         draggable? true}}]
  (let [trigger ($ button {:id (when entity-id (str "actions-btn-" entity-id))
                           :btn-type :ghost
                           :shape "circle"
                           :class "!w-10 !h-10"
                           :title "Actions"
                           :style {:font-size "18px" :font-weight "bold"}}
                   trigger-label)]

    ($ core/dropdown {:id (when entity-id (str "actions-dropdown-" entity-id))
                      :trigger trigger
                      :position position
                      :class class
                      :draggable? draggable?}
      (for [[group-idx {:keys [group-title items]}] (map-indexed vector actions)]
        ($ :<> {:key group-idx}
          ($ dropdown-group {:title group-title}
            (for [[item-idx {:keys [id icon label variant on-click disabled? loading-key tooltip tooltip-position]}] (map-indexed vector items)]
              (let [is-loading? (when loading-key (get loading-states loading-key))]
                ($ dropdown-item {:key item-idx
                                  :id (when entity-id (str id "-" entity-id))
                                  :icon icon
                                  :label label
                                  :variant variant
                                  :loading? is-loading?
                                  :disabled? (or disabled? is-loading?)
                                  :tooltip tooltip
                                  :tooltip-position tooltip-position
                                  :on-click on-click}))))
          (when (< group-idx (dec (count actions)))
            ($ core/dropdown-divider)))))))
