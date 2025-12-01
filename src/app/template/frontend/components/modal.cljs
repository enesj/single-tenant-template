(ns app.template.frontend.components.modal
  (:require
    [app.template.frontend.components.draggable :refer [use-draggable]]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-effect use-state]]
    [goog.object :as gobj]))

(def modal-props
  {:id {:type :string :required true}
   :on-close {:type :function :required false}
   :draggable? {:type :boolean :required false}
   :initial-position {:type :map :required false}
   :width {:type :string :required false}
   :header {:type :any :required false}
   :z-index {:type :number :required false}
   :backdrop-opacity {:type :number :required false}
   :class {:type :string :required false}
   :header-class {:type :string :required false}
   :children {:type :any :required false}})

(defui modal
  "A reusable modal component with backdrop and optional draggability"
  {:prop-types modal-props}
  [{:keys [id
           on-close
           draggable?
           initial-position
           width
           header
           header-class
           z-index
           backdrop-opacity
           class
           children]
    :or {draggable? true
         width "400px"
         z-index 50
         backdrop-opacity 20}
    :as props}]
  (let [;; Generate backdrop ID that matches UI test expectations
        ;; Extract base ID without "-modal" suffix if present
        base-id (if (.endsWith id "-modal")
                  (subs id 0 (- (count id) 6))
                  id)
        backdrop-id (str base-id "-backdrop")

        initial-position-provided? (and (map? initial-position)
                                     (number? (:x initial-position))
                                     (number? (:y initial-position)))

        ;; Always call use-draggable unconditionally to avoid hooks violation
        ;; but only use its results when draggable? is true
        {:keys [form-ref header-props drag-state set-position]} (use-draggable
                                                                  {:initial-position (or initial-position {:x 100 :y 100})})

        ;; Only use draggable properties when draggable? is true
        actual-form-ref (when draggable? form-ref)
        actual-header-props (when draggable? header-props)
        actual-drag-state (when draggable? drag-state)

        ;; Add state to track if modal is manually closed
        [is-open set-is-open] (use-state true)

        ;; Helpers to ensure we never pass non-renderables to React
        react-element? (fn [v] (and (some? v) (object? v) (gobj/get v "$$typeof")))
        safe-node (fn safe-node [v]
                    (cond
                      (nil? v) nil
                      (or (string? v) (number? v) (react-element? v)) v
                      (keyword? v) (name v)
                      (symbol? v) (name v)
                      (boolean? v) (if v "true" "false")
                      (map? v) (str v)
                      (sequential? v) (into [] (map safe-node v))
                      :else (str v)))

        ;; Handle closing the modal
        handle-close (fn [e]
                       (.stopPropagation e)
                       (set-is-open false)
                       (when on-close (on-close)))

        default-header-class "flex justify-between items-center gap-4 border-b border-base-200 bg-base-200/40 px-6 py-4 rounded-t-lg mb-4"
        resolved-header-class (str (or header-class default-header-class)
                                (when draggable? " cursor-move select-none"))]

    (use-effect
      (fn []
        (when (and draggable?
                (not initial-position-provided?)
                (exists? js/window))
          (when-let [el (.-current form-ref)]
            (let [rect (.getBoundingClientRect el)
                  modal-width (.-width rect)
                  modal-height (.-height rect)
                  window-width (.-innerWidth js/window)
                  window-height (.-innerHeight js/window)
                  centered-x (max 20 (/ (- window-width modal-width) 2))
                  centered-y (max 20 (/ (- window-height modal-height) 2))]
              (set-position centered-x centered-y))))
        js/undefined)
      [draggable? initial-position-provided? set-position form-ref])

    ;; Only render the modal if it's open
    (when is-open
      ($ :div
        ;; Backdrop overlay - clicking this closes the modal
        ($ :div {:class "fixed inset-0 bg-black/20"
                 :id backdrop-id
                 :style {:z-index (dec z-index)}
                 :on-click handle-close})

        ;; Modal container
        ($ :div {:class (str "fixed bg-white rounded-lg overflow-hidden shadow-xl "
                          (when-not draggable? "top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 ")
                          class)
                 :ref actual-form-ref
                 :id id
                 :style (merge
                          {:z-index z-index
                           :width width}
                          (when draggable?
                            {:top (str (:pos-y actual-drag-state) "px")
                             :left (str (:pos-x actual-drag-state) "px")
                             :cursor (when (:dragging? actual-drag-state) "move")}))
                 :role "dialog"
                 :aria-modal "true"}

          ;; Header (if provided)
          (when header
            ($ :div (merge
                      {:class resolved-header-class}
                      (when draggable? actual-header-props))
              (safe-node header)))

          ;; Content
          (safe-node children))))))
