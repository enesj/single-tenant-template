(ns app.admin.frontend.components.tabs
  "Reusable tab helpers for DaisyUI tabs that avoid navigation jumps"
  (:require
    [uix.core :refer [$]]))

(defn tab-link
  "Render a DaisyUI tab link that prevents default navigation and invokes `on-select`.
  Accepts optional `:id`, `:class`, `:href`, and `:data-testid` keys."
  [{:keys [id label active? on-select class href data-testid] :as opts}]
  (let [react-key (:key opts)]
    ($ :a (cond-> {:class (str "ds-tab "
                            (when active? "ds-tab-active ")
                            (when class class))
                   :href (or href "#")
                   :data-testid data-testid
                   :on-click (fn [e]
                               (.preventDefault e)
                               (when on-select
                                 (on-select)))}
            id (assoc :id id)
            react-key (assoc :key react-key))
      label)))
