(ns app.template.frontend.components.list.cells
  "Reactive cell components that subscribe directly to display settings.
   
   These components handle their own visibility by subscribing to display settings,
   eliminating the need for prop drilling through multiple layers.
   
   Pattern:
   - Each cell component uses `use-display-settings` to get its visibility
   - Parent components don't need to pass visibility props
   - Changes to settings immediately reflect in the UI"
  (:require
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.keywords :as kw]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.common :refer [checkbox]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.hooks.display-settings :refer [use-display-settings
                                                          use-action-visibility]]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui]]))

;; =============================================================================
;; Selection Cell Components
;; =============================================================================

(defui select-checkbox
  "Checkbox for selecting a single row."
  [{:keys [entity-name item selected-ids on-select-change]}]
  (let [item-id (id-utils/extract-entity-id item)
        is-selected (contains? selected-ids item-id)]
    ($ :div {:class "flex justify-center items-center py-2"}
      ($ checkbox
        {:id (str "select-" (kw/ensure-name entity-name) "-" item-id)
         :checked is-selected
         :on-change #(on-select-change item-id (.. % -target -checked))}))))

(defui reactive-selection-cell
  "A reactive cell that subscribes to show-select? and conditionally renders the checkbox.
   This ensures the selection checkbox visibility responds immediately to toggling in the settings panel.
   
   Uses the unified display settings hook for reactivity."
  [{:keys [entity-name item selected-ids on-select-change]}]
  (let [{:keys [show-select?]} (use-display-settings entity-name)]
    (if show-select?
      ($ select-checkbox
        {:entity-name entity-name
         :item item
         :selected-ids selected-ids
         :on-select-change on-select-change})
      ;; Hidden placeholder to maintain table structure
      ($ :div {:style {:width "0px" :padding "0px" :margin "0px"}
               :class "hidden-cell"}))))

(defui select-all-checkbox
  "Three-state checkbox for selecting all rows in a table."
  [{:keys [entity-name all-items selected-ids on-select-all]}]
  (let [total-count (count all-items)
        selected-count (count selected-ids)
        indeterminate? (and (> selected-count 0) (< selected-count total-count))
        checked? (and (> total-count 0) (= selected-count total-count))]
    ($ :div {:class "flex justify-center items-center"}
      ($ checkbox
        {:id (str "select-all-" (kw/ensure-name entity-name))
         :indeterminate? indeterminate?
         :checked checked?
         :on-change #(on-select-all (.. % -target -checked))}))))

(defui reactive-select-all-header
  "A reactive header cell that subscribes to show-select? and conditionally renders the select-all checkbox.
   This ensures the select-all checkbox visibility responds immediately to toggling in the settings panel."
  [{:keys [entity-name all-items selected-ids on-select-all]}]
  (let [{:keys [show-select?]} (use-display-settings entity-name)]
    (if show-select?
      ($ :div {:class "flex justify-center items-center py-2"}
        ($ select-all-checkbox
          {:entity-name entity-name
           :all-items all-items
           :selected-ids selected-ids
           :on-select-all on-select-all}))
      ;; Hidden placeholder to maintain table structure
      ($ :div {:style {:width "0px" :padding "0px" :margin "0px"}
               :class "hidden-cell"}))))

;; =============================================================================
;; Action Cell Components
;; =============================================================================

(defui edit-button
  "Edit button for a single row.
   
   Props:
   - entity-name: Entity type keyword
   - item-id: ID of the item
   - item: Full item data (optional, for custom edit handlers)
   - on-edit-click: Optional custom click handler fn that receives the item"
  [{:keys [entity-name item-id item on-edit-click]}]
  (let [entity-name-lower (kw/lower-name entity-name)
        handle-edit-click (fn [_e]
                            (rf/dispatch [::crud-events/clear-error (kw/ensure-keyword entity-name)])
                            (rf/dispatch [::form-events/clear-form-errors (kw/ensure-keyword entity-name)])
                            (if on-edit-click
                              ;; Use custom handler (for modal edit)
                              (on-edit-click item)
                              ;; Default inline edit behavior
                              (rf/dispatch [::config-events/set-editing item-id])))]
    ($ button
      {:id (str "btn-edit-" entity-name-lower "-" item-id)
       :btn-type :primary
       :shape "circle"
       :on-click handle-edit-click}
      ($ edit-icon))))

(defui delete-button
  "Delete button for a single row with confirmation dialog."
  [{:keys [entity-name item-id]}]
  (let [entity-name-lower (kw/lower-name entity-name)
        handle-delete-confirm (fn []
                                (rf/dispatch [::crud-events/delete-entity entity-name item-id]))
        handle-delete-click (fn [e]
                              (.stopPropagation e)
                              (confirm-dialog/show-confirm
                                {:message "Do you want to delete this record?"
                                 :title "Confirm Delete"
                                 :on-confirm handle-delete-confirm
                                 :on-cancel nil}))]
    ($ button
      {:id (str "btn-delete-" entity-name-lower "-" item-id)
       :btn-type :danger
       :shape "circle"
       :on-click handle-delete-click}
      ($ delete-icon))))

#_(defui reactive-action-cell
    "A reactive cell that subscribes to show-edit? and show-delete? settings.
   
   Renders edit/delete buttons based on current display settings,
   plus any custom actions provided.
   
   Props:
   - entity-name: keyword for the entity type
   - item: the row item data
   - custom-actions: (optional) fn that receives item and returns additional action buttons
   - on-edit-click: (optional) custom edit handler fn (for modal edit)"
    [{:keys [entity-name item custom-actions on-edit-click]}]
    (let [{:keys [show-edit? show-delete?]} (use-action-visibility entity-name)
          item-id (id-utils/extract-entity-id item)]
      ($ :div {:class "flex items-center gap-2"}
        ;; Edit button (when enabled)
        (when show-edit?
          ($ edit-button
            {:entity-name entity-name
             :item-id item-id
             :item item
             :on-edit-click on-edit-click}))
        ;; Delete button (when enabled)
        (when show-delete?
          ($ delete-button
            {:entity-name entity-name
             :item-id item-id}))
        ;; Custom actions (when provided) - rendered alongside default buttons
        (when custom-actions
          (custom-actions item)))))

(defui action-buttons
  "Legacy action buttons component for backward compatibility.
   Renders action buttons using explicit show-edit?/show-delete? props.
   
   Props:
   - entity-name: Entity type keyword
   - item: Full item data
   - show-edit?: Whether to show edit button
   - show-delete?: Whether to show delete button
   - custom-actions: Optional custom actions render fn
   - on-edit-click: Optional custom edit handler fn (for modal edit)"
  [{:keys [entity-name item show-edit? show-delete? custom-actions on-edit-click]}]
  (let [item-id (id-utils/extract-entity-id item)]
    ($ :div {:class "flex items-center gap-2"}
      (when show-edit?
        ($ edit-button
          {:entity-name entity-name
           :item-id item-id
           :item item
           :on-edit-click on-edit-click}))
      (when show-delete?
        ($ delete-button
          {:entity-name entity-name
           :item-id item-id}))
      (when custom-actions
        (custom-actions item)))))

;; =============================================================================
;; Timestamp Cell Components
;; =============================================================================

(defn format-timestamp
  "Format a timestamp value for display."
  [value]
  (when value
    (let [date (js/Date. value)
          month (.toLocaleString date "en-US" #js {:month "short"})
          day (.getDate date)
          hours (.getHours date)
          minutes (.getMinutes date)
          formatted-time (str (when (< hours 10) "0") hours ":" (when (< minutes 10) "0") minutes)]
      ($ :div
        ($ :span {:class "text-primary"} (str month " " day))
        ($ :span {:class "ml-1"} formatted-time)))))

(defui timestamp-cell
  "Display a timestamp value."
  [{:keys [value]}]
  ($ :span {:class "whitespace-nowrap"}
    (format-timestamp value)))

#_(defui reactive-timestamps-cell
    "A reactive cell that subscribes to show-timestamps? and visible-columns settings.
   Renders created_at and/or updated_at values based on visibility settings.
   
   Props:
   - entity-name: keyword for the entity type
   - item: the row item data
   - visible-columns: map of column visibility settings"
    [{:keys [entity-name item visible-columns]}]
    (let [{:keys [show-timestamps?]} (use-display-settings entity-name)]
      (when show-timestamps?
        (let [;; Helper to check column visibility
              column-visible? (fn [key]
                                (let [value (get visible-columns key ::not-found)]
                                  (if (= value ::not-found) true value)))
              ;; Extract timestamp values
              created-at (or (:created-at item)
                           (get item (keyword (str (kw/ensure-name entity-name) "/created-at"))))
              updated-at (or (:updated-at item)
                           (get item (keyword (str (kw/ensure-name entity-name) "/updated-at"))))]
          ($ :<>
            (when (column-visible? :created-at)
              ($ timestamp-cell {:value created-at}))
            (when (column-visible? :updated-at)
              ($ timestamp-cell {:value updated-at})))))))
