(ns app.admin.frontend.renderers.modals
  "Helpers for rendering entity-specific modal components"
  (:require
    [uix.core :refer [$]]))

(defn render-modals
  "Render a collection of modal components."
  [modal-components]
  (when (and modal-components (seq modal-components))
    (into []
      (map-indexed (fn [idx modal-component]
                     (when (fn? modal-component)
                       ($ modal-component {:key (str "admin-entity-modal-" idx)}))))
      modal-components)))
