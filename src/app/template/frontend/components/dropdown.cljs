(ns app.template.frontend.components.dropdown
  "Generic reusable dropdown component with action groups and items.
   Thin wrapper around domain-specific dropdown sub-components."
  (:require
    [app.template.frontend.components.dropdown.action :as action]
    [app.template.frontend.components.dropdown.core :as core]
    [app.template.frontend.components.dropdown.group :as group]
    [app.template.frontend.components.dropdown.item :as item]))

;; Re-export components for backward compatibility
(def loading-spinner core/loading-spinner)
(def dropdown-divider core/dropdown-divider)
(def dropdown core/dropdown)

(def dropdown-item item/dropdown-item)

(def dropdown-group group/dropdown-group)

(def action-dropdown action/action-dropdown)
