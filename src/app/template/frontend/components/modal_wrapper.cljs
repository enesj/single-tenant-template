(ns app.template.frontend.components.modal-wrapper
  "Reusable modal wrapper component for consistent UI"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui]]))

(defui modal-wrapper
  "Reusable modal wrapper with consistent styling and behavior.

  Props:
  - visible? - Boolean to control modal visibility
  - title - String for modal header title
  - size - Keyword for modal size (:small, :medium, :large, :extra-large)
  - on-close - Event vector to dispatch when closing modal
  - close-button-id - String ID for close button (for testing)
  - children - Modal content to render inside the wrapper"
  [{:keys [visible? title size on-close close-button-id children]}]

  (when visible?
    ($ :div {:class "ds-modal ds-modal-open"}
      ($ :div {:class (str "ds-modal-box "
                        (case size
                          :small "max-w-md"
                          :medium "max-w-2xl"
                          :large "max-w-4xl"
                          :extra-large "max-w-6xl"
                          "max-w-2xl"))} ; default to medium

        ;; Modal header with title and close button
        ($ :div {:class "flex justify-between items-center mb-4"}
          ($ :h3 {:class "font-bold text-lg"} title)
          ($ button {:id (or close-button-id "btn-close-modal")
                     :btn-type :ghost
                     :class "ds-btn-sm ds-btn-circle"
                     :on-click #(dispatch on-close)}
            "✕"))

        ;; Modal content
        children))))
