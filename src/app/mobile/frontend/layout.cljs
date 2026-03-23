(ns app.mobile.frontend.layout
  "Mobile app shell — header + scrollable content + bottom tab bar."
  (:require
    [app.mobile.frontend.components.bottom-tabs :refer [bottom-tabs]]
    [uix.core :refer [$ defui]]))

(defui mobile-layout [{:keys [children]}]
  ($ :div {:class "flex flex-col min-h-screen bg-base-200"}
    ;; Scrollable content area with bottom padding for tab bar
    ($ :main {:class "flex-1 overflow-y-auto pb-20"}
      children)
    ;; Fixed bottom tab bar
    ($ bottom-tabs)))
