(ns app.template.frontend.components.draggable
  (:require
   [uix.core :refer [use-callback use-effect use-ref use-state]]))

(defn use-draggable
  "Pointer-driven draggable behavior for modal-like components.

   Params:
   - initial-position: {:x n :y n} starting position
   - initial-x / initial-y: numeric fallback for starting position

   Returns:
   - form-ref: ref to the draggable element (optional use by caller)
   - header-props: props to attach to the header (pointer + mouse fallback)
   - drag-state: {:pos-x n :pos-y n :dragging? bool}
   - set-position: (fn [x y]) programmatic reposition
  "
  [{:keys [initial-x initial-y initial-position]}]
  (let [form-ref (use-ref nil)
        default-x 20
        default-y 20

        [pos-x pos-y] (if (and (map? initial-position)
                            (number? (:x initial-position))
                            (number? (:y initial-position)))
                        [(:x initial-position) (:y initial-position)]
                        [(or initial-x default-x) (or initial-y default-y)])

        [drag-state set-drag-state] (use-state {:dragging? false
                                                :offset-x 0
                                                :offset-y 0
                                                :pos-x pos-x
                                                :pos-y pos-y})

        pointer-id-ref (use-ref nil)
        capture-el-ref (use-ref nil)

        set-position (use-callback
                       (fn [x y]
                         (set-drag-state (fn [s]
                                           (-> s (assoc :pos-x x :pos-y y)))))
                       [])

        stop-drag! (use-callback
                     (fn [e]
                       (when (:dragging? drag-state)
                         (when e
                           (when (.-preventDefault e) (.preventDefault e))
                           (when (.-stopPropagation e) (.stopPropagation e)))
                         (when-let [el @capture-el-ref]
                           (when-let [pid @pointer-id-ref]
                             (when (.-releasePointerCapture el)
                               (.releasePointerCapture el pid))))
                         (set! (.-current capture-el-ref) nil)
                         (set! (.-current pointer-id-ref) nil)
                         (set-drag-state (fn [s] (assoc s :dragging? false)))))
                     [drag-state])

        handle-pointer-move (use-callback
                              (fn [e]
                                (when (:dragging? drag-state)
                                  (let [{:keys [offset-x offset-y]} drag-state
                                        x (- (.-clientX e) offset-x)
                                        y (- (.-clientY e) offset-y)]
                                    (set-drag-state (fn [s]
                                                      (-> s
                                                        (assoc :pos-x x
                                                          :pos-y y)))))))
                              [drag-state])

        start-drag (use-callback
                     (fn [e]
                       ;; ignore clicks on close buttons or interactive controls inside header
                       (let [target (.-target e)
                             no-drag? (or (= (.-id target) "close-button")
                                        (some? (when (.-closest target)
                                                 (.closest target "button, [role=button], .ds-btn, .ds-btn-circle"))))]
                         (when-not no-drag?
                           (let [btn (.-button e)]
                             (when (or (nil? btn) (= 0 btn))
                               (when (.-preventDefault e) (.preventDefault e))
                               (when (.-stopPropagation e) (.stopPropagation e))
                               (let [current-x (:pos-x drag-state)
                                     current-y (:pos-y drag-state)
                                     pid (.-pointerId e)
                                     el (.-current form-ref)
                                     dx (- (.-clientX e) current-x)
                                     dy (- (.-clientY e) current-y)]
                                 (set! (.-current pointer-id-ref) pid)
                                 (set! (.-current capture-el-ref) el)
                                 (when (and el (.-setPointerCapture el) pid)
                                   (.setPointerCapture el pid))
                                 (set-drag-state (fn [s]
                                                   (-> s
                                                     (assoc :dragging? true
                                                       :offset-x dx
                                                       :offset-y dy))))))))))
                     [drag-state])

        ;; Fallback for environments without pointer events
        start-drag-mouse (use-callback
                           (fn [e]
                             (set! (.-pointerId e) 1) ; synthetic id for consistency
                             (start-drag e))
                           [start-drag])]

    ;; Attach global listeners for pointer movement + cleanup
    (use-effect
      (fn []
        (js/document.addEventListener "pointermove" handle-pointer-move)
        (js/document.addEventListener "pointerup" stop-drag!)
        (js/document.addEventListener "pointercancel" stop-drag!)
        ;; Fallbacks
        (js/document.addEventListener "mousemove" handle-pointer-move)
        (js/document.addEventListener "mouseup" stop-drag!)
        (fn []
          (js/document.removeEventListener "pointermove" handle-pointer-move)
          (js/document.removeEventListener "pointerup" stop-drag!)
          (js/document.removeEventListener "pointercancel" stop-drag!)
          (js/document.removeEventListener "mousemove" handle-pointer-move)
          (js/document.removeEventListener "mouseup" stop-drag!)))
      [handle-pointer-move stop-drag!])

    {:form-ref form-ref
     :header-props {:on-pointer-down start-drag
                    :on-mouse-down start-drag-mouse
                    :class "cursor-move select-none"
                    :style {:touch-action "none"}}
     :drag-state drag-state
     :set-position set-position}))
