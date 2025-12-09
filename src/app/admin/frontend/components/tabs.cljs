(ns app.admin.frontend.components.tabs
  "Reusable tab helpers for DaisyUI tabs that avoid navigation jumps"
  (:require
    [uix.core :refer [$]]))

(defn tab-link
  "Render a DaisyUI tab link that prevents default navigation and invokes `on-select`.
  Accepts optional `:class`, `:href`, and `:data-testid` keys."
  [{:keys [label active? on-select class href data-testid]}]
  ($ :a {:class (str "ds-tab "
                  (when active? "ds-tab-active ")
                  (when class class))
         :href (or href "#")
         :data-testid data-testid
         :on-click (fn [e]
                     (.preventDefault e)
                     (when on-select
                       (on-select)))}
    label))
