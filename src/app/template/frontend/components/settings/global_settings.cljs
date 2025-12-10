(ns app.template.frontend.components.settings.global-settings
  (:require
    [app.template.frontend.components.auth :refer [auth-component]]
    [app.template.frontend.components.button :refer [button change-theme]]
    [app.template.frontend.components.icons :refer [settings-icon-large]]
    [app.template.frontend.components.modal :refer [modal]]
    [uix.core :refer [$ defui use-state]]))

(defn calculate-panel-position []
  (let [settings-btn (.querySelector js/document ".settings-icon-btn")
        window-width (.-innerWidth js/window)
        panel-width 280]
    (if settings-btn
      (let [rect (.getBoundingClientRect settings-btn)
            btn-top (.-bottom rect)
            btn-left (.-left rect)
            btn-right (.-right rect)

            ;; Prefer positioning to the right side of the button when there's space
            ;; otherwise position it with sufficient margins from screen edges
            x-pos (cond
                    ;; If there's enough space to the right of the button
                    (< (+ btn-right panel-width 20) window-width)
                    btn-right

                    ;; If there's enough space to the left of the button
                    (> (- btn-left panel-width 20) 0)
                    (- btn-left panel-width)

                    ;; Center under the button if enough space
                    (< (+ (- btn-left 140) panel-width) window-width)
                    (- btn-left 140)

                    ;; Right-align to window with margin
                    :else
                    (- window-width panel-width 20))]
        {:x x-pos :y (+ btn-top 10)})
      ;; Fallback position if button not found
      {:x 100 :y 100})))

(defui settings-panel
  "Global settings panel with theme selector and list view settings"
  [{:keys [_global-settings?]}]
  (let [[expanded?, set-expanded] (use-state false)
        [panel-position, set-panel-position] (use-state nil)

        ;; Handle expanding/collapsing the panel
        handle-toggle (fn []
                        ;; Calculate position immediately when expanding
                        (when (not expanded?)
                          (set-panel-position (calculate-panel-position)))
                        (set-expanded (not expanded?)))]

    ($ :div
      ($ :div {:class "flex justify-between gap-4 px-4 py-1.5 text-primary"}
        ;; Settings icon button on the left
        ($ :div {:class "flex items-center"}
          ($ button {:btn-type :ghost
                     :class "ds-btn-circle settings-icon-btn"
                     :id "settings-panel-toggle"
                     :title "Settings"
                     :on-click handle-toggle}
            ($ settings-icon-large))))
      ;; Settings panel when expanded
      (when expanded?
        ($ :div {:class "flex items-center space-x-4"}
          ($ modal
            {:id "settings-panel-modal"
             :on-close #(set-expanded false)
             :draggable? true
             :initial-position panel-position
             :width "280px"
             :class "ds-card ds-shadow bg-base-100 rounded-md border-2 border-blue-500 overflow-hidden"
             :header ($ :div {:class "p-2 text-primary font-medium bg-primary-content rounded-t-md"}
                       "Settings")}

            ;; Settings content
            ($ :div {:class "p-2 text-gray-800"}
              ;; Theme selector
              ($ :div {:class "flex flex-col gap-4 mb-3 p-2"}
                ($ :div {:class "flex items-center justify-between"}
                  ($ :span {:class "font-medium mr-4 font-bold"} "Theme")
                  ($ change-theme))
                ($ auth-component)))))))))
