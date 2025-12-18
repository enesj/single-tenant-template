(ns app.template.frontend.components.dropdown.group
  (:require [uix.core :refer [$ defui]]))

(defui dropdown-group
  "Group of related dropdown items with optional section header"
  [{:keys [title children class]}]
  (if title
    ($ :div {:class (str "mb-2 " (or class ""))}
      ($ :div {:class "text-xs text-base-content/60 font-semibold mb-1 px-3"} title)
      ($ :<> {} children))
    ($ :div {:class (str "mb-1 " (or class ""))}
      children)))
