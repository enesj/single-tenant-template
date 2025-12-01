(ns app.template.frontend.pages.about
  (:require
    [app.template.frontend.routes :as routes]
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui]]))

(defui about-page []
  ($ :div {:class "container mx-auto px-4 py-8"}
    ($ :h1 {:class "text-4xl font-bold mb-6"} "About Us")
    ($ :div {:class "ds-card bg-base-100 shadow-xl mb-8"}
      ($ :div {:class "ds-card-body"}
        ($ :h2 {:class "ds-card-title text-4xl"} "Our Application")
        ($ :p {:class "py-2"}
          "A modern web application built with Clojure and ClojureScript for managing transactions and items efficiently.")
        ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-6"}
          ($ :div
            ($ :h3 {:class "font-semibold text-2xl mb-3"} "Features")
            ($ :ul {:class "ds-menu ds-menu-lg bg-base-200 rounded-box"}
              ($ :li {:class "text-lg"} "Efficient item management")
              ($ :li {:class "text-lg"} "Transaction tracking")
              ($ :li {:class "text-lg"} "Modern, responsive interface")
              ($ :li {:class "text-lg"} "Secure data handling")))
          ($ :div
            ($ :h3 {:class "font-semibold text-2xl mb-3"} "Technology Stack")
            ($ :ul {:class "ds-menu ds-menu-lg bg-base-200 rounded-box"}
              ($ :li {:class "text-lg"} "Clojure backend")
              ($ :li {:class "text-lg"} "ClojureScript frontend")
              ($ :li {:class "text-lg"} "Re-frame for state management")
              ($ :li {:class "text-lg"} "Reitit for routing")))
          ($ :li {:class "text-lg"} "Reitit for routing"))))))
($ :div {:class "flex justify-center"}
  ($ button {:btn-type :primary
             :on-click (fn []
                         (js/window.history.pushState nil nil "/")
                         (routes/init-routes!))}
    "Back to Home"))
