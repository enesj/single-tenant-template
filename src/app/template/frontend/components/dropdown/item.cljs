(ns app.template.frontend.components.dropdown.item
  (:require
    [app.template.frontend.components.dropdown.core :as core]
    [uix.core :refer [$ defui]]))

(defui dropdown-item
  "Generic dropdown item with consistent styling and behavior"
  [{:keys [id icon label description on-click disabled? loading? variant class-override children tooltip tooltip-position]
    :or {tooltip-position :right}}]
  (let [base-classes "block text-sm px-3 py-2 rounded cursor-pointer transition-colors duration-200"
        variant-classes (case variant
                          :success "text-success hover:bg-success/10"
                          :error "text-error hover:bg-error/10"
                          :info "text-info hover:bg-info/10"
                          :warning "text-warning hover:bg-warning/10"
                          :primary "text-primary hover:bg-primary/10"
                          :secondary "text-secondary hover:bg-secondary/10"
                          "hover:bg-base-300")
        disabled-classes (when disabled? "opacity-50 cursor-not-allowed")
        final-classes (str base-classes " " variant-classes " " disabled-classes " " (or class-override ""))
        tooltip-class (case tooltip-position
                        :left "ds-tooltip-left"
                        :right "ds-tooltip-right"
                        :top "ds-tooltip-top"
                        :bottom "ds-tooltip-bottom"
                        "ds-tooltip-right")]

    ($ :div {:class "py-0.5"}
      ($ :div {:class (str "ds-tooltip " tooltip-class)
               :data-tip tooltip
               :title tooltip}
        ($ :a {:id id
               :class final-classes
               :role "menuitem"
               :tabIndex 0
               :aria-disabled (boolean disabled?)
               :onClick (when on-click
                          (fn [e]
                            (when (.-stopPropagation e)
                              (.stopPropagation e))
                            (.preventDefault e)
                            (when (not disabled?)
                              (on-click e))))
               :onKeyDown (fn [e]
                            (when (and on-click (not disabled?) (#{13 32} (.-keyCode e))) ; Enter or Space
                              (when (.-stopPropagation e)
                                (.stopPropagation e))
                              (.preventDefault e)
                              (on-click e)))}
          (if children
            children
            ($ :div {:class "flex items-center gap-2"}
              (when loading? ($ core/loading-spinner {:size :xs}))
              (when icon
                (if (string? icon)
                  ($ :span {:class "text-base"} icon)
                  icon))
              ($ :div {}
                ($ :div {:class "font-semibold"} label)
                (when description
                  ($ :div {:class "text-xs text-base-content/70"} description))))))))))
