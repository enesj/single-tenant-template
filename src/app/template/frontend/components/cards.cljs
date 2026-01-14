(ns app.template.frontend.components.cards
  "Card-based display components for admin interface"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui]]))

(defui quick-actions-card
  "Reusable quick actions card with gradient styling and action buttons.

   Props:
   - :title - Card title (default: 'Quick Actions')
   - :icon-path - SVG path for the header icon
   - :icon-gradient - Icon gradient classes (default: 'from-accent to-secondary')
   - :bg-gradient - Background gradient classes (default: 'from-accent/10 to-secondary/10')
   - :actions - Vector of action maps with :label, :on-click, :button-class, and :icon-path
   - :footer-stats - Optional map with :label and :value for footer stats
   - :container-class - Additional classes for the container"
  [{:keys [title icon-path icon-gradient bg-gradient actions footer-stats container-class]
    :or {title "Quick Actions"
         icon-path "M13 10V3L4 14h7v7l9-11h-7z"
         icon-gradient "from-accent to-secondary"
         bg-gradient "from-accent/10 to-secondary/10"
         actions []
         container-class ""}}]
  ($ :div {:class (str "bg-gradient-to-br " bg-gradient " rounded-2xl shadow-xl border border-accent/20 p-8 hover:shadow-2xl transition-all duration-300 " container-class)}
    ;; Header with icon and title
    ($ :div {:class "flex items-center gap-3 mb-6"}
      ($ :div {:class (str "w-10 h-10 bg-gradient-to-br " icon-gradient " rounded-lg flex items-center justify-center shadow-md")}
        ($ :svg {:class "w-5 h-5 text-white" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon-path})))
      ($ :h3 {:class "text-2xl font-bold text-base-content"} title))

    ;; Action buttons
    ($ :div {:class "space-y-4"}
      (for [action actions]
        ($ :div {:key (:label action) :class "group"}
          (let [btn-type (case (:button-class action)
                           "ds-btn-primary" :primary
                           "ds-btn-secondary" :secondary
                           "ds-btn-accent" :accent
                           "ds-btn-success" :success
                           "ds-btn-warning" :warning
                           "ds-btn-error" :error
                           "ds-btn-outline" :outline
                           "ds-btn-ghost" :ghost
                           :primary)]
            ($ button {:btn-type btn-type
                       :class "ds-btn-block ds-btn-lg shadow-lg hover:shadow-xl transition-all duration-300 group-hover:scale-[1.02]"
                       :on-click (:on-click action)}
              (when (:icon-path action)
                ($ :svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d (:icon-path action)})))
              (:label action))))))

    ;; Optional footer stats
    (when footer-stats
      ($ :div {:class "mt-6 pt-6 border-t border-base-content/10"}
        ($ :div {:class "flex items-center justify-between text-sm"}
          ($ :span {:class "text-base-content/60"} (:label footer-stats))
          ($ :span {:class "text-base-content font-medium"} (:value footer-stats)))))))

(defui chart-list-card
  "Consistent card wrapper for charts and lists with optional scrolling.

   Props:
   - :title - Card title
   - :subtitle - Card subtitle
   - :max-height - Optional max height class for scrollable content (e.g. 'max-h-96')
   - :scroll-y - Enable vertical scrolling (default: false)
   - :children - Card content
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle max-height scroll-y children container-class]
    :or {scroll-y false
         container-class ""}}]
  ($ :div {:class (str "ds-card bg-base-100 shadow-xl p-6 " container-class)}
    ($ :div {:class "mb-4"}
      ($ :h3 {:class "text-lg font-semibold text-gray-900"} title)
      (when subtitle
        ($ :p {:class "text-sm text-gray-600"} subtitle)))

    ($ :div {:class (str "space-y-3 "
                      (when max-height max-height)
                      (when scroll-y "overflow-y-auto"))}
      children)))
