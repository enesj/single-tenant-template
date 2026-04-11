(ns app.template.frontend.components.modal-wrapper
  "Reusable modal wrapper component for consistent UI"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.draggable :refer [use-draggable]]
    [app.template.frontend.components.icons :refer [cancel-icon]]
    [app.template.frontend.ui.z-scale :as z]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui use-effect]]))

(defn- dispatch-close!
  [on-close]
  (cond
    (fn? on-close) (on-close)
    (vector? on-close) (dispatch on-close)
    (and (sequential? on-close) (seq on-close)) (dispatch (vec on-close))
    :else nil))

(defui ^:private modal-wrapper-open
  "Reusable modal wrapper with consistent styling and behavior.

  Props:
  - id - Optional string id for the modal box (for testing)
  - visible? - Boolean to control modal visibility
  - title - String for modal header title (ignored when :header is provided)
  - header - Optional custom header node (replaces default title/breadcrumbs block)
  - breadcrumbs - Vector of maps with :label and :href keys (optional)
  - size - Keyword for modal size (:small, :medium, :large, :extra-large)
  - resizable? - Boolean to enable resize (default: false)
  - on-close - Event vector to dispatch when closing modal
  - close-button-id - String ID for close button (for testing)
  - class - Optional extra classes for the modal box
  - content-class - Optional extra classes for the modal content container
  - draggable? - Boolean to enable dragging the modal via its header (default: false)
  - initial-position - {:x n :y n} position used when draggable? is true; partial maps are allowed
  - width - Optional string width (e.g. 450px)
  - z-index - Optional number z-index for the modal overlay
  - backdrop-opacity - Optional number 0-100 to control backdrop darkness
  - backdrop-id - Optional string id for the backdrop container
  - header-class - Optional extra classes for the header container
  - children - Modal content to render inside the wrapper"
  [{:keys [id
           title
           header
           breadcrumbs
           size
           resizable?
           on-close
           close-button-id
           class
           content-class
           draggable?
           initial-position
           width
           z-index
           backdrop-opacity
           backdrop-id
           header-class
           children]
    :or {draggable? false
         z-index z/modal-backdrop}}]

  (let [base-id (when (string? id)
                  (if (.endsWith id "-modal")
                    (subs id 0 (- (count id) 6))
                    id))
        resolved-backdrop-id (or backdrop-id
                               (when base-id (str base-id "-backdrop")))
        initial-x (when (and (map? initial-position)
                          (number? (:x initial-position)))
                    (:x initial-position))
        initial-y (when (and (map? initial-position)
                          (number? (:y initial-position)))
                    (:y initial-position))

        ;; Always call use-draggable unconditionally to avoid hooks violation.
        ;; Only apply its outputs when draggable? is true.
        {:keys [form-ref header-props drag-state set-position]} (use-draggable
                                                                  {:initial-x initial-x
                                                                   :initial-y initial-y
                                                                   :initial-position (when (and (number? initial-x)
                                                                                             (number? initial-y))
                                                                                       {:x initial-x :y initial-y})})
        actual-form-ref (when draggable? form-ref)
        actual-header-props (when draggable? header-props)
        actual-drag-state (when draggable? drag-state)

        resolved-backdrop-style (when (number? backdrop-opacity)
                                  {:background-color (str "rgba(0,0,0," (/ backdrop-opacity 100) ")")})

        ;; If a modal is resizable we must not constrain it via fixed max-w/max-h
        ;; size presets; instead clamp it to the viewport.
        resolved-size-class (if resizable?
                              "max-w-[95vw] max-h-[95vh]"
                              (case size
                                :small "max-w-md max-h-[32rem]"
                                :medium "max-w-2xl max-h-[40rem]"
                                :large "max-w-4xl max-h-[48rem]"
                                :extra-large "max-w-6xl max-h-[64rem]"
                                "max-w-2xl max-h-[40rem]"))

        resolved-box-style (merge
                             (when resizable?
                               {:resize "both"
                                ;; resize requires overflow != visible; keep this hidden so
                                ;; we don’t create a second scrollbar outside the content.
                                :overflow "hidden"
                                :minWidth "400px"
                                :minHeight "300px"
                                :maxWidth "95vw"
                                :maxHeight "95vh"})
                             (when (string? width)
                               {:width width})
                             (when draggable?
                               {:position "fixed"
                                :top (str (:pos-y actual-drag-state) "px")
                                :left (str (:pos-x actual-drag-state) "px")
                                :margin 0}))]

    (use-effect
      (fn []
        (when (and draggable?
                (exists? js/window))
          (when-let [el (.-current form-ref)]
            (let [rect (.getBoundingClientRect el)
                  modal-width (.-width rect)
                  modal-height (.-height rect)
                  window-width (.-innerWidth js/window)
                  window-height (.-innerHeight js/window)
                  centered-x (max 20 (/ (- window-width modal-width) 2))
                  centered-y (max 20 (/ (- window-height modal-height) 2))
                  resolved-x (or initial-x centered-x)
                  resolved-y (or initial-y centered-y)]
              (set-position resolved-x resolved-y))))
        js/undefined)
      [draggable? initial-x initial-y set-position form-ref])

    ($ :div {:class "ds-modal ds-modal-open"
             :id resolved-backdrop-id
             :style (merge
                      {:z-index z-index}
                      resolved-backdrop-style)
             :on-click (fn [_e]
                         (dispatch-close! on-close))}
      ($ :div {:id id
               :ref actual-form-ref
               :class (str "ds-modal-box p-0 flex flex-col "
                        resolved-size-class
                        (when (seq class) (str " " class)))
               :style resolved-box-style
               :role "dialog"
               :aria-modal "true"
               :on-click (fn [e]
                           (.stopPropagation e))}

        ;; Modal header with title and close button - sticky
        ($ :div (merge
                  {:class (str "flex justify-between items-center px-6 py-4 border-b border-base-200 sticky top-0 bg-base-100 z-10"
                            (when (seq header-class) (str " " header-class))
                            (when draggable? " cursor-move select-none"))}
                  (when draggable?
                    (-> actual-header-props
                      (dissoc :class)
                      (update :style (fn [s]
                                       (merge s {:touch-action "none"}))))))
          ($ :div {:class "min-w-0 flex-1"}
            (if header
              header
              ($ :div
                (when (seq breadcrumbs)
                  ($ :div {:class "text-sm ds-breadcrumbs mb-1"}
                    ($ :ul
                      (for [{:keys [label href]} breadcrumbs]
                        ($ :li {:key label}
                          (if href
                            ($ :a {:href href} label)
                            label))))))
                ($ :h3 {:class "font-bold text-lg"} title))))
          ($ button {:id (or close-button-id "btn-close-modal")
                     :btn-type :ghost
                     :class "ds-btn-sm ds-btn-circle"
                     :on-click (fn []
                                 (dispatch-close! on-close))}
            ($ cancel-icon)))

        ;; Modal content - scrollable
        ($ :div {:class (str "px-6 py-4 overflow-y-auto flex-1"
                          (when (seq content-class) (str " " content-class)))}
          children)))))

(defui modal-wrapper
  "Public wrapper that only mounts the modal (and its hooks) when visible.

  See modal-wrapper-open docstring for supported props."
  [{:keys [visible?] :as props}]
  (when visible?
    ($ modal-wrapper-open props)))
