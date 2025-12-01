(ns app.template.frontend.components.table
  (:require
    [app.shared.keywords :as kw]
    [app.template.frontend.components.icons :refer [settings-icon]]
    [app.template.frontend.components.settings.list-view-settings :refer [list-view-settings-panel]]
    [app.template.frontend.events.list.settings :as settings-events]
    [taoensso.timbre :as log]
    [uix.core :as uix :refer [$ defui]]
    [uix.dom]
    [uix.re-frame :refer [use-subscribe]]))

(defn get-array-length [cells]
  (cond
    (array? cells) (.-length cells)
    (nil? cells) 0
    (seqable? cells) (count cells)
    :else 1))

(defn ensure-seq [cells]
  (cond
    (array? cells) (array-seq cells)
    (nil? cells) []
    :else (if (seqable? cells) cells [cells])))

(defui resizable-cell
  "A table cell with a resizable handle"
  [{:keys [is-header? index width on-resize children resizable? fixed-width colspan sticky? sticky-position sticky-z-index]}]
  (let [cell-type (if is-header? :th :td)
        [resizing? set-resizing!] (uix/use-state false)
        [cell-width set-cell-width] (uix/use-state (or width "auto"))

        handle-mouse-down (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e)
                            (set-resizing! true)

                            (let [start-x (.-clientX e)
                                  header-element (.. e -target -parentElement)
                                  start-width (.-offsetWidth header-element)

                                  handle-mouse-move (fn [move-event]
                                                      (.preventDefault move-event)
                                                      (let [current-x (.-clientX move-event)
                                                            delta (- current-x start-x)
                                                            new-width (+ start-width delta)]
                                                        (when (> new-width 50) ; Minimum width
                                                          (let [width-px (str new-width "px")]
                                                            (set-cell-width width-px)
                                                            (when on-resize
                                                              (on-resize index new-width))))))

                                  cleanup-fn (atom nil)
                                  handle-mouse-up (fn [up-event]
                                                    (.preventDefault up-event)
                                                    (set-resizing! false)
                                                    (when @cleanup-fn
                                                      (@cleanup-fn)))]
                              (reset! cleanup-fn
                                (fn []
                                  (.removeEventListener js/document "mousemove" handle-mouse-move)
                                  (.removeEventListener js/document "mouseup" handle-mouse-up)))

                              (.addEventListener js/document "mousemove" handle-mouse-move)
                              (.addEventListener js/document "mouseup" handle-mouse-up)))

        ;; Apply sticky positioning if specified
        sticky-style (when sticky?
                       (merge
                         {:position "sticky"
                          :z-index (or sticky-z-index 20)
                          :overflow "visible"}
                         (case sticky-position
                           :left {:left 0
                                  :background "linear-gradient(90deg, hsl(var(--b1)) 0%, hsl(var(--b1))/0.98 40%, hsl(var(--b2))/0.92 70%, hsl(var(--b2))/0.75 85%, rgba(255,255,255,0.1) 100%)"
                                  :backdrop-filter "blur(12px) saturate(1.8)"
                                  :border-right "1px solid hsl(var(--b3))/0.4"
                                  :box-shadow "8px 0 24px -8px rgba(0, 0, 0, 0.12), 4px 0 8px -2px rgba(0, 0, 0, 0.08), inset -1px 0 0 hsl(var(--b3))/0.15"}
                           :right {:right 0
                                   :background "linear-gradient(270deg, hsl(var(--b1)) 0%, hsl(var(--b1))/0.98 40%, hsl(var(--b2))/0.92 70%, hsl(var(--b2))/0.75 85%, rgba(255,255,255,0.1) 100%)"
                                   :backdrop-filter "blur(12px) saturate(1.8)"
                                   :border-left "1px solid hsl(var(--b3))/0.4"
                                   :box-shadow "-8px 0 24px -8px rgba(0, 0, 0, 0.12), -4px 0 8px -2px rgba(0, 0, 0, 0.08), inset 1px 0 0 hsl(var(--b3))/0.15"}
                           {})))]

    ($ cell-type
      {:key index
       :class (str "table-cell relative "
                (when sticky? "transition-all duration-200 hover:backdrop-blur-[16px] ") ; Smooth hover transition for sticky columns
                (when resizing? "select-none ")
                (when-not is-header? "p-2")
                (when is-header? "px-3 py-4 text-left font-medium text-base-content border-r border-base-300/30 last:border-r-0"))
       :colSpan (when (and colspan (not is-header?)) colspan)
       :data-column-index (when-not is-header? index)
       :style (merge
                {:overflow "visible"}
                (if fixed-width
                  {:width fixed-width
                   :min-width fixed-width
                   :max-width fixed-width
                   :box-sizing "border-box"
                   :position "relative"}
                  {:width cell-width
                   :min-width "50px"
                   :box-sizing "border-box"
                   :position "relative"})
                sticky-style)}

      ;; Cell content - different layout for headers vs data cells
      (if is-header?
        ;; Header cells: render text directly without flex wrapper
        ($ :span {:class (str "whitespace-nowrap truncate block "
                           (when sticky? "font-bold text-primary drop-shadow-md text-shadow "))} ; Enhanced text styling for sticky headers
          children
          ;; Add sticky indicator icon for header cells with enhanced styling
          (when sticky?
            ($ :span {:class "ml-1 text-xs opacity-90 drop-shadow-md"
                      :style {:text-shadow "0 1px 2px rgba(0,0,0,0.1)"}}
              (case sticky-position
                :left "📌"
                :right "📌"
                ""))))
        ;; Data cells: use flex layout as before
        ($ :div {:class "flex items-center h-full"}
          children))

      ;; Resizer handle (only for headers and only if resizable)
      (when (and is-header? resizable? (not sticky?))       ; No resizer on sticky columns
        ($ :div
          {:class "absolute top-0 right-0 h-full w-2 cursor-col-resize hover:bg-blue-300 active:bg-blue-500 z-10"
           :on-mouse-down handle-mouse-down})))))

(defui row
  {:prop-types {:cells {:type :array :required true}
                :class {:type :string}
                :num-columns {:type :number}
                :entity-name {:type :string}
                :row-index {:type :number}
                :is-header? {:type :boolean}
                :column-widths {:type :array}
                :on-column-resize {:type :function}
                :resizable-columns {:type :array}
                :fixed-width-columns {:type :array}
                :sticky-columns {:type :array}}}
  [{:keys [cells class num-columns is-header? entity-name row-index column-widths on-column-resize resizable-columns fixed-width-columns sticky-columns] :as props}]
  (let [cell-count (get-array-length cells)
        colspan (when (= cell-count 1) num-columns)
        cells-seq (ensure-seq cells)]

    ($ :tr {:class class
            :id (when (and entity-name row-index)
                  (str "row-" (kw/ensure-name entity-name) "-" row-index))}
      (map-indexed
        (fn [index cell]
          ;;_ (println "cell: " (cond
          ;;                      (fn? cell) (cell)
          ;;                      (vector? cell) (first cell) ;; Handle vector form of components
          ;;                      :else cell))
          ;; Determine if this column should be sticky
          (let [sticky-info (get sticky-columns index)
                sticky? (boolean sticky-info)
                sticky-position (when sticky? (:position sticky-info))
                sticky-z-index (when sticky?
                                 (cond
                                   (and (not is-header?) (= sticky-position :right)) 15000
                                   :else (:z-index sticky-info)))]
            ;; Always use resizable-cell to maintain consistent hook ordering
            ($ resizable-cell
              {:key index
               :is-header? is-header?
               :index index
               :width (get column-widths index)
               :on-resize on-column-resize
               :resizable? (and is-header? (get resizable-columns index true) (not sticky?)) ; Not resizable if sticky
               :fixed-width (get fixed-width-columns index nil)
               :sticky? sticky?
               :sticky-position sticky-position
               :sticky-z-index sticky-z-index
               :colspan (when colspan colspan)}
              (cond
                (fn? cell) (cell)
                (vector? cell) (first cell)                 ;; Handle vector form of components
                :else cell))))
        cells-seq))))

(def table-props
  {:headers {:type :array :required true}
   :rows {:type :array :required true}
   :row-key {:type :function :required true}
   :render-row {:type :function :required true}
   :entity-name {:type :string :required false}
   :entity-spec {:type :any :required false}
   :editing {:type :any :required false}
   :show-highlights? {:type :boolean :required false}
   :pagination {:type :any :required false}})

(defui table
  {:prop-types table-props}
  [{:keys [headers rows row-key render-row editing entity-name entity-spec display-settings page-display-settings
           per-page on-per-page-change rows-per-page-options] :as props}]
  (let [header-cells (ensure-seq headers)
        header-count (count header-cells)
        ;; FIXED: Use proper nil check instead of falsy check to handle explicit false values
        show-highlights? (if (contains? props :show-highlights?)
                           (:show-highlights? props)
                           true)
        [column-widths set-column-widths] (uix/use-state (vec (repeat header-count nil)))
        [settings-panel-visible? set-settings-panel-visible!] (uix/use-state false)
        [hovering-icon? set-hovering-icon!] (uix/use-state false)

        ;; Subscribe to table width configuration
        table-width (use-subscribe [::settings-events/table-width (some-> entity-name keyword)])

        ;; Determine which columns should be resizable
        ;; By default, all columns are resizable except select, edit, and delete columns
        resizable-columns (let [header-count (count header-cells)]
                            (vec
                              (map-indexed
                                (fn [idx _]
                                  ;; First column (select) and last column (actions) are not resizable
                                  (not (or (= idx 0)
                                         (= idx (- header-count 1)))))
                                header-cells)))

        ;; Set fixed widths for non-resizable columns
        fixed-width-columns (let [header-count (count header-cells)]
                              (vec
                                (map-indexed
                                  (fn [idx _]
                                    (cond
                                      ;; Select column gets fixed width
                                      (= idx 0) "50px"
                                      ;; Actions column gets more width for circular buttons
                                      (= idx (- header-count 1)) "150px"
                                      ;; All other columns are dynamically sized
                                      :else nil))
                                  header-cells)))

        ;; Configure sticky columns (first and last columns)
        sticky-columns (let [header-count (count header-cells)]
                         (vec
                           (map-indexed
                             (fn [idx _]
                               (cond
                                 ;; First column (select) sticks to left
                                 (= idx 0) {:position :left :z-index 200}
                                 ;; Last column (actions) sticks to right; body rows bump z-index further
                                 (= idx (- header-count 1)) {:position :right :z-index 300}
                                 ;; Other columns are not sticky
                                 :else nil))
                             header-cells)))

        handle-column-resize (fn [index width]
                               (set-column-widths
                                 (fn [current-widths]
                                   (assoc current-widths index (str width "px")))))

        ;; Use entity name for list-view-settings if available
        effective-entity-name (when entity-name (keyword entity-name))

        ;; FIXED: Use page-display-settings (page props only) for settings panel control visibility
        ;; This ensures user preferences don't affect which controls are shown/hidden
        hardcoded-settings (or page-display-settings {})]
    ($ :div {:id (when entity-name (str "table-" (kw/ensure-name entity-name)))}
      ($ :div {:class "overflow-x-auto max-w-full"
               :style {:max-width (str table-width "px")
                       :overflow-y "visible"}}
        ($ :table {:class "ds-table relative border-collapse" :style {:table-layout "auto" :border-spacing "0" :min-width "800px"}}
          ($ :thead
            ($ row
              {:key "header"
               :cells header-cells
               :class "text-base"
               :is-header? true
               :num-columns (count header-cells)
               :entity-name entity-name
               :column-widths column-widths
               :resizable-columns resizable-columns
               :fixed-width-columns fixed-width-columns
               :sticky-columns sticky-columns
               :on-column-resize handle-column-resize})

            ;; List view settings row - always visible between header and body
            ($ :tr {:class "list-view-settings-row bg-base-200"}
              ($ :td {:colSpan header-count :class "px-2 py-1 border-t border-b"}
                ($ :div {:class "flex flex-nowrap gap-2 justify-left overflow-x-auto"}
                  ($ :span {:id "btn-settings-toggle" :class "flex items-center text-primary ml-2 p-1 cursor-pointer relative z-100"
                            :on-mouse-enter #(set-hovering-icon! true)
                            :on-mouse-leave #(set-hovering-icon! false)
                            :on-click #(set-settings-panel-visible! (not settings-panel-visible?))}
                    ($ settings-icon {:active? settings-panel-visible?}))
                  (when (and hovering-icon? (not settings-panel-visible?))
                    ($ :span {:class "relative whitespace-nowrap px-2 py-1 rounded shadow-md z-10"}
                      "Click to set columns visibility"))
                  (when settings-panel-visible?
                    ($ list-view-settings-panel {:entity-name effective-entity-name
                                                 :current-entity-name effective-entity-name
                                                 :entity-spec entity-spec
                                                 :compact? true
                                                 ;; FIXED: Pass only page props to control visibility
                                                 ;; - :show-timestamps? false in page props → hides control entirely
                                                 ;; - :show-timestamps? true/missing in page props → shows control (user can toggle)
                                                 ;; - Other settings when false in page props → lock the control (can't be enabled)
                                                 :hardcoded-display-settings hardcoded-settings
                                                 ;; Pass rows per page props so it's always available
                                                 :per-page per-page
                                                 :on-per-page-change on-per-page-change
                                                 :rows-per-page-options rows-per-page-options}))))))
          ($ :tbody
            (map-indexed
              (fn [idx row-data]
                (let [row-id (row-key row-data)
                      rendered-result (render-row row-data editing)

;; Extract cells and highlight flags from the result
                      cells (if (and (map? rendered-result) (:cells rendered-result))
                              (:cells rendered-result)
                              rendered-result)
                      recently-updated? (boolean
                                          (and (map? rendered-result)
                                            (:recently-updated? rendered-result)))
                      recently-created? (boolean
                                          (and (map? rendered-result)
                                            (:recently-created? rendered-result)))
                      ;; Apply different highlight classes based on status and if highlights are shown
                      highlight-class (if show-highlights?
                                        (cond
                                          recently-updated? " bg-green-200/50" ;; More visible green for updates
                                          recently-created? " bg-blue-200/50" ;; More visible blue for new items
                                          :else "")
                                        "")]
                  ($ row
                    {:key (str "row-" idx "-" row-id)
                     :cells cells
                     :class (str "ds-hover" highlight-class)
                     :num-columns header-count
                     :is-header? false
                     :entity-name entity-name
                     :row-index idx
                     :column-widths column-widths
                     :resizable-columns resizable-columns
                     :fixed-width-columns fixed-width-columns
                     :sticky-columns sticky-columns})))
              (ensure-seq rows))))))))

;; Removed the batch edit form from here
