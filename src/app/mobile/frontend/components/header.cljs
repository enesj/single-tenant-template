(ns app.mobile.frontend.components.header
  "Contextual mobile header with optional back button and title."
  (:require
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defui mobile-header [{:keys [title show-back? on-back]}]
  ($ :header {:class "sticky top-0 z-40 bg-base-100 border-b border-base-300"}
    ($ :div {:class "flex items-center h-14 px-4"}
      (when show-back?
        ($ :button
          {:class "mr-3 p-1 -ml-1"
           :on-click (or on-back #(.back js/window.history))}
          ($ :svg {:class "w-6 h-6" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M15.75 19.5L8.25 12l7.5-7.5"}))))
      ($ :h1 {:class "text-lg font-semibold truncate"}
        title))))
