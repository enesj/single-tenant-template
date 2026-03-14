(ns app.domain.frontend.expenses.pages.user.search.cards
  "Result card atom components and result group wrapper for search results."
  (:require
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [uix.core :refer [$ defui]]))

;; ---------------------------------------------------------------------------
;; Result card atoms
;; ---------------------------------------------------------------------------

(defui expense-card [{:keys [item t on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :div {:class "flex items-center justify-between gap-2"}
      ($ :div {:class "min-w-0 flex-1"}
        ($ :p {:class "text-base font-medium truncate"}
          (or (:supplier_display_name item) "\u2014"))
        ($ :p {:class "text-sm text-base-content/60 truncate mt-0.5"}
          (str (h/format-date (:purchased_at item))
            (when (:payer_label item) (str " \u00B7 " (:payer_label item))))))
      ($ :div {:class "flex-shrink-0 text-right"}
        ($ :p {:class "text-base font-semibold"}
          (h/format-amount (:total_amount item) (:currency item)))
        ($ :span {:class (str "text-sm px-1.5 py-0.5 rounded "
                           (if (:is_posted item)
                             "bg-success/10 text-success"
                             "bg-warning/10 text-warning"))}
          (if (:is_posted item) (t :search/posted) (t :search/pending)))))))

(defui receipt-card [{:keys [item t on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :div {:class "min-w-0"}
      ($ :p {:class "text-base font-medium truncate"}
        (or (:original_filename item) "\u2014"))
      ($ :p {:class "text-sm text-base-content/60 truncate mt-0.5"}
        (str (or (:supplier_guess item) "")
          (when (:store_guess item) (str " \u00B7 " (:store_guess item))))))))

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
