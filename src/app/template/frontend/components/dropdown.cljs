(ns app.template.frontend.components.dropdown
  "Generic reusable dropdown component with action groups and items"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [uix.core :refer [$ defui] :as uix]
    ["react-dom" :as react-dom]))

;; =============================================================================
;; Core Components
;; =============================================================================

(defui loading-spinner
  "Small loading spinner for individual actions"
  [{:keys [size class]}]
  (let [size-class (case size
                     :xs "ds-loading-xs"
                     :sm "ds-loading-sm"
                     :md "ds-loading-md"
                     :lg "ds-loading-lg"
                     "ds-loading-xs")]
    ($ :span {:class (str "ds-loading ds-loading-spinner " size-class " " (or class ""))})))

(defui dropdown-item
  "Generic dropdown item with consistent styling and behavior"
  [{:keys [id icon label description on-click disabled? loading? variant class-override children tooltip tooltip-position]
    :or {tooltip-position :right}}]
  (let [base-classes "block text-sm px-3 py-2 rounded cursor-pointer transition-colors duration-200"
        variant-classes (case variant
                          :success "text-success hover:bg-success/10"
                          :error "text-error hover:bg-error/10"
                          :info "text-info hover:bg-info/10"
                          :warning "text-warning hover:bg-warning/10"
                          :primary "text-primary hover:bg-primary/10"
                          :secondary "text-secondary hover:bg-secondary/10"
                          "hover:bg-base-300")
        disabled-classes (when disabled? "opacity-50 cursor-not-allowed")
        final-classes (str base-classes " " variant-classes " " disabled-classes " " (or class-override ""))
        tooltip-class (case tooltip-position
                        :left "ds-tooltip-left"
                        :right "ds-tooltip-right"
                        :top "ds-tooltip-top"
                        :bottom "ds-tooltip-bottom"
                        "ds-tooltip-right")]

    ($ :div {:class "py-0.5"}
      ;; Wrap in tooltip container so disabled items still show explanation
      ($ :div {:class (str "ds-tooltip " tooltip-class)
               :data-tip tooltip
               :title tooltip}
        ($ :a {:id id
               :class final-classes
               :role "menuitem"
               :tabIndex 0
               :aria-disabled (boolean disabled?)
               :onClick (when on-click
                          (fn [e]
                            (when (.-stopPropagation e)
                              (.stopPropagation e))
                            (.preventDefault e)
                            (when (not disabled?)
                              (on-click e))))
               :onKeyDown (fn [e]
                            (when (and on-click (not disabled?) (#{13 32} (.-keyCode e))) ; Enter or Space
                              (when (.-stopPropagation e)
                                (.stopPropagation e))
                              (.preventDefault e)
                              (on-click e)))}
          (if children
            children
            ($ :div {:class "flex items-center gap-2"}
              (when loading? ($ loading-spinner {:size :xs}))
              (when icon
                (if (string? icon)
                  ($ :span {:class "text-base"} icon)
                  icon))
              ($ :div {}
                ($ :div {:class "font-semibold"} label)
                (when description
                  ($ :div {:class "text-xs text-base-content/70"} description))))))))))

(defui dropdown-group
  "Group of related dropdown items with optional section header"
  [{:keys [title children class]}]
  (if title
    ($ :div {:class (str "mb-2 " (or class ""))}
      ($ :div {:class "text-xs text-base-content/60 font-semibold mb-1 px-3"} title)
      ($ :<> {} children))
    ($ :div {:class (str "mb-1 " (or class ""))}
      children)))

(defui dropdown-divider
  "Visual separator between dropdown sections"
  [{:keys [class]}]
  ($ :hr {:class (str "my-2 border-base-300 " (or class ""))}))

;; =============================================================================
;; Main Dropdown Component
;; =============================================================================

(defui dropdown
  "Generic dropdown component with flexible positioning and styling

  Props:
  - trigger: Component or element to trigger dropdown (required)
  - children: Dropdown content (required)
  - position: :auto (default), :popover, :portal, :manual
  - class: Additional CSS classes
  - dropdown-class: CSS classes for dropdown content
  - open?: External control of open state (for manual positioning)
  - on-toggle: Callback when dropdown opens/closes
  - auto-close?: Close on click outside (default: true)
  - id: Unique identifier for dropdown
  - draggable?: Enable pointer-based dragging (portal-only; optional)
  "
  [{:keys [trigger children position class dropdown-class open? on-toggle auto-close? id draggable?]}]
  (let [position-type (or position :auto)
        auto-close? (if (nil? auto-close?) true auto-close?)
        drag-enabled? (and draggable? (= :portal position-type))

        ;; State for auto positioning
        [dropdown-open? set-dropdown-open!] (uix/use-state false)
        dropdown-ref (uix/use-ref nil)
        [drag-state set-drag-state] (uix/use-state {:left nil :top nil :user-position? false})
        drag-offset-ref (uix/use-ref {:dx 0 :dy 0})
        dragging-ref (uix/use-ref false)
        pointer-id-ref (uix/use-ref nil)
        user-moved-ref (uix/use-ref false)
        pointer-capture-el-ref (uix/use-ref nil)

        ;; Use external or internal open state
        is-open? (if (some? open?) open? dropdown-open?)
        toggle-dropdown! (if on-toggle on-toggle set-dropdown-open!)

        ;; Generate unique IDs
        dropdown-id (or id (str "dropdown-" (random-uuid)))
        trigger-id (str dropdown-id "-trigger")

        ;; Handle dropdown toggle
        handle-toggle (fn [e]
                        (.stopPropagation e)
                        (toggle-dropdown! (not is-open?)))

        ;; Handle click outside for auto-close
        handle-click-outside (fn [e]
                               (when (and auto-close? is-open?)
                                 (let [target (.-target e)
                                       dropdown-node @dropdown-ref
                                       rendered-dropdown (.getElementById js/document dropdown-id)
                                       trigger-node (.getElementById js/document trigger-id)
                                       inside-dropdown? (or (and dropdown-node (.contains dropdown-node target))
                                                          (and rendered-dropdown (.contains rendered-dropdown target)))
                                       inside-trigger? (and trigger-node (.contains trigger-node target))]
                                   (when (and (not inside-dropdown?) (not inside-trigger?))
                                     (toggle-dropdown! false)))))

        ;; Position utility for popover/portal
        position-popover (fn [button-element popover-element & [apply-style?]]
                           (let [apply-style? (if (nil? apply-style?) true apply-style?)]
                             (when (and button-element popover-element (#{:popover :portal} position-type))
                               (let [button-rect (.getBoundingClientRect button-element)
                                     popover-width 224       ; Default dropdown width

                                     ;; Calculate optimal position
                                     left-pos (- (.-left button-rect) popover-width)
                                     right-pos (+ (.-left button-rect) (.-width button-rect))
                                     final-left (if (< left-pos 16)
                                                  right-pos
                                                  left-pos)
                                     top-pos (+ (.-top button-rect) (.-height button-rect) 4)
                                     computed {:left final-left :top top-pos}]

                                 (when apply-style?
                                   (set! (.. popover-element -style -left) (str final-left "px"))
                                   (set! (.. popover-element -style -top) (str top-pos "px"))
                                   (set! (.. popover-element -style -position) "fixed"))
                                 computed))))

        ;; Handle popover positioning on toggle
        handle-popover-toggle (fn [e]
                                (.stopPropagation e)
                                (let [button-el (.-target e)
                                      dropdown-el (.getElementById js/document dropdown-id)]
                                  (when dropdown-el
                                    (js/setTimeout #(position-popover button-el dropdown-el) 10))))

        ;; Drag helpers (portal only)
        get-dropdown-element (fn []
                               (or @dropdown-ref
                                 (.getElementById js/document dropdown-id)))]
    (letfn [(stop-drag! [e]
              (when @dragging-ref
                (when e
                  (.preventDefault e)
                  (.stopPropagation e))
                (when-let [pointer-el @pointer-capture-el-ref]
                  (let [pointer-id @pointer-id-ref
                        release-fn (.-releasePointerCapture pointer-el)]
                    (when (and pointer-id release-fn)
                      (.releasePointerCapture pointer-el pointer-id))))
                (reset! pointer-capture-el-ref nil)
                (reset! dragging-ref false)
                (reset! pointer-id-ref nil)
                (.removeEventListener js/document "pointermove" handle-pointer-move)
                (.removeEventListener js/document "pointerup" stop-drag!)
                (.removeEventListener js/document "pointercancel" stop-drag!)))

            (handle-pointer-move [e]
              (when (and drag-enabled? @dragging-ref (= (.-pointerId e) @pointer-id-ref))
                (if (zero? (.-buttons e))
                  (stop-drag! e)
                  (do
                    (.preventDefault e)
                    (let [{:keys [dx dy]} @drag-offset-ref
                          raw-left (- (.-clientX e) dx)
                          raw-top (- (.-clientY e) dy)
                          dropdown-el (get-dropdown-element)
                          dropdown-rect (when dropdown-el (.getBoundingClientRect dropdown-el))
                          dropdown-width (if dropdown-rect (.-width dropdown-rect) 0)
                          dropdown-height (if dropdown-rect (.-height dropdown-rect) 0)
                          viewport-width (.-innerWidth js/window)
                          viewport-height (.-innerHeight js/window)
                          max-left (max 0 (- viewport-width dropdown-width))
                          max-top (max 0 (- viewport-height dropdown-height))
                          bounded-left (-> raw-left (max 0) (min max-left))
                          bounded-top (-> raw-top (max 0) (min max-top))]
                      (reset! user-moved-ref true)
                      (set-drag-state (fn [state]
                                        (-> state
                                          (assoc :left bounded-left
                                            :top bounded-top
                                            :user-position? true)))))))))

            (start-drag [e]
              (when drag-enabled?
                (let [button (.-button e)]
                  (when (or (nil? button) (= 0 button))
                    (.preventDefault e)
                    (.stopPropagation e)
                    (when-let [dropdown-el (get-dropdown-element)]
                      (let [rect (.getBoundingClientRect dropdown-el)
                            pointer-id (.-pointerId e)]
                        (reset! drag-offset-ref {:dx (- (.-clientX e) (.-left rect))
                                                 :dy (- (.-clientY e) (.-top rect))})
                        (reset! pointer-id-ref pointer-id)
                        (reset! dragging-ref true)
                        (let [capture-el (.-currentTarget e)]
                          (reset! pointer-capture-el-ref capture-el)
                          (when (.-setPointerCapture capture-el)
                            (.setPointerCapture capture-el pointer-id)))
                        (.addEventListener js/document "pointermove" handle-pointer-move)
                        (.addEventListener js/document "pointerup" stop-drag!)
                        (.addEventListener js/document "pointercancel" stop-drag!)))))))]
      ;; Effect for click outside listener - fixed dependencies
      (uix/use-effect
        (fn []
          (when auto-close?
            (.addEventListener js/document "mousedown" handle-click-outside)
            #(.removeEventListener js/document "mousedown" handle-click-outside)))
        [handle-click-outside auto-close? is-open?])

      ;; Effect to handle portal positioning - fixed dependencies
      (uix/use-effect
        (fn []
          (when (and is-open? (= :portal position-type))
            (let [dropdown-el (.getElementById js/document dropdown-id)
                  trigger-el (.getElementById js/document trigger-id)]
              (when (and dropdown-el trigger-el)
                (let [pos (position-popover trigger-el dropdown-el (not drag-enabled?))]
                  (when (and drag-enabled? pos (number? (:left pos)) (number? (:top pos)) (not @user-moved-ref))
                    (reset! user-moved-ref false)
                    (set-drag-state (fn [state]
                                      (-> state
                                        (assoc :left (:left pos)
                                          :top (:top pos)
                                          :user-position? false))))))))))
        [position-popover is-open? dropdown-id trigger-id position-type drag-enabled?])

      ;; Reset drag state when dropdown closes
      (uix/use-effect
        (fn []
          (when (or (not is-open?) (not drag-enabled?))
            (stop-drag! nil)
            (when (or (:left drag-state) (:top drag-state) (:user-position? drag-state))
              (set-drag-state (constantly {:left nil :top nil :user-position? false})))
            (reset! user-moved-ref false)))
        [stop-drag! is-open? drag-enabled? drag-state])

      ;; Cleanup pointer listeners on unmount
      (uix/use-effect
        (fn []
          (fn []
            (stop-drag! nil)))
        [stop-drag!])

      ;; Render different dropdown types
      (case position-type
        ;; Popover positioning (use fixed coordinates; no reliance on native popover)
        :popover
        ($ :div {:class (str "inline-block " (or class ""))}
          ;; Trigger with popover target
          ($ :div {:onClick handle-popover-toggle}
            (if (map? trigger)
              (uix/clone-element trigger {:popoverTarget dropdown-id :id trigger-id})
              trigger))

          ;; Popover content
          ($ :div {:popover ""
                   :id dropdown-id
                   :ref (fn [el] (reset! dropdown-ref el))
                   :class (str "bg-base-200 rounded-box shadow-xl border border-base-300 p-3 "
                            (or dropdown-class ""))
                   :style {:position "fixed" :z-index 1000}}
            children))

        ;; Simple positioning using fixed positioning (alternative to portal)
        :portal
        ($ :div {:class (str "inline-block z-[9999] " (or class ""))
                 :style {:z-index 9999}}
          ;; Trigger toggles open and positions content
          ($ :div {:id trigger-id
                   :onClick (fn [e]
                              (.stopPropagation e)
                              (toggle-dropdown! (not is-open?))
                              (when-not is-open?
                                (reset! user-moved-ref false)
                                (let [btn (.-currentTarget e)]
                                  (js/setTimeout
                                    (fn []
                                      (let [el (.getElementById js/document dropdown-id)
                                            pos (position-popover btn el (not drag-enabled?))]
                                        (when (and drag-enabled? pos)
                                          (set-drag-state (fn [_]
                                                            {:left (:left pos)
                                                             :top (:top pos)
                                                             :user-position? false})))))
                                    0))))}
            trigger)

          ;; Content rendered with fixed positioning to bypass container clipping
          (when is-open?
            (when-let [portal-target (.-body js/document)]
              (react-dom/createPortal
                (let [have-position? (and drag-enabled?
                                       (number? (:left drag-state))
                                       (number? (:top drag-state)))
                      base-style {:position "fixed" :z-index 20000}
                      portal-style (cond-> base-style
                                     drag-enabled? (assoc :left (str (or (:left drag-state) 0) "px")
                                                     :top (str (or (:top drag-state) 0) "px")
                                                     :touch-action "none")
                                     (and drag-enabled? (not have-position?)) (assoc :opacity 0 :pointer-events "none"))]
                  ($ :div {:id dropdown-id
                           :ref (fn [el] (reset! dropdown-ref el))
                           :class (str "bg-base-200 rounded-box shadow-xl border border-base-300 p-3 " (or dropdown-class ""))
                           :style portal-style}
                    (when drag-enabled?
                      ($ :div {:class "cursor-move select-none px-3 py-2 text-xs text-base-content/60 border-b border-base-300 flex items-center gap-2"
                               :onPointerDown start-drag}
                        ($ :span {:aria-hidden true :class "opacity-60"} "⠿")
                        ($ :span {:class "tracking-wide"} "Drag to reposition")))
                    children))
                portal-target))))

        ;; Manual positioning (controlled externally)
        :manual
        ($ :<> {}
          ($ :div {:onClick handle-toggle} trigger)
          (when is-open? children))

        ;; Auto positioning with CSS (default)
        :auto
        ($ :div {:class (str "relative inline-block z-[9999] " (or class ""))
                 :style {:z-index 9999}}
          ;; Trigger button
          ($ :div {:id trigger-id :onClick handle-toggle} trigger)

          ;; Dropdown content with CSS positioning
          (when is-open?
            ($ :div {:id dropdown-id
                     :ref (fn [el] (reset! dropdown-ref el))
                     :class (str "absolute right-0 z-[9999] mt-2 origin-top-right rounded-md "
                              "bg-base-200 shadow-lg ring-1 ring-black ring-opacity-5 "
                              "focus:outline-none w-56 " (or dropdown-class ""))
                     :style {:z-index 9999}
                     :role "menu"
                     :aria-orientation "vertical"
                     :aria-labelledby trigger-id}
              ($ :div {:class "py-3" :role "none"}
                children))))))))

;; =============================================================================
;; Convenience Components
;; =============================================================================

(defui action-dropdown
  "Pre-configured dropdown for action menus with consistent styling"
  [{:keys [entity-id trigger-label actions loading-states position class draggable?]
    :or {trigger-label "⋮"
         position :portal
         draggable? true}}]
  (let [trigger ($ button {:btn-type :ghost
                           :shape "circle"
                           :class "!w-10 !h-10"
                           :title "Actions"
                           :style {:font-size "18px" :font-weight "bold"}}
                  trigger-label)]

    ($ dropdown {:trigger trigger
                 :position position
                 :class class
                 :draggable? draggable?}
      (for [[group-idx {:keys [group-title items]}] (map-indexed vector actions)]
        ($ :<> {:key group-idx}
          ($ dropdown-group {:title group-title}
            (for [[item-idx {:keys [id icon label variant on-click disabled? loading-key tooltip tooltip-position]}] (map-indexed vector items)]
              (let [is-loading? (when loading-key (get loading-states loading-key))]
                ($ dropdown-item {:key item-idx
                                  :id (when entity-id (str id "-" entity-id))
                                  :icon icon
                                  :label label
                                  :variant variant
                                  :loading? is-loading?
                                  :disabled? (or disabled? is-loading?)
                                  :tooltip tooltip
                                  :tooltip-position tooltip-position
                                  :on-click on-click}))))
          (when (< group-idx (dec (count actions)))
            ($ dropdown-divider)))))))
