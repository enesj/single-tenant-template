(ns app.domain.frontend.expenses.pages.user.search.cards
  "Result card atom components and result group wrapper for search results."
  (:require
    [uix.core :refer [$ defui]]))

;; ---------------------------------------------------------------------------
;; Result card atoms
;; ---------------------------------------------------------------------------

(defui simple-card [{:keys [label subtitle on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :p {:class "text-base font-medium truncate"} (or label "\u2014"))
    (when subtitle
      ($ :p {:class "text-sm text-base-content/60 truncate mt-0.5"} subtitle))))

;; ---------------------------------------------------------------------------
;; Result group
;; ---------------------------------------------------------------------------

(defui result-group [{:keys [title badge-class items render-item]}]
  (when (seq items)
    ($ :div {:class "mb-5"}
      ($ :div {:class "flex items-center gap-2 mb-2"}
        ($ :span {:class (str "text-sm font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full " badge-class)}
          title)
        ($ :span {:class "text-sm text-base-content/40"} (count items)))
      ($ :div {:class "space-y-1.5"}
        (for [item items]
          (render-item item))))))
