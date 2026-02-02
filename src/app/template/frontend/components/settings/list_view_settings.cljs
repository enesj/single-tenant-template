(ns app.template.frontend.components.settings.list-view-settings
  (:require
    [app.admin.frontend.subs.config :as admin-subs]
    [app.template.frontend.subs.core :as core-subs]
    [app.template.frontend.components.icons :refer [filter-icon]]
    [app.template.frontend.utils.shared :as template-utils]
    [app.template.frontend.events.list.settings :as settings-events]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.template.frontend.utils.column-config :as column-config]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- toggle-column!
  "Toggle column visibility for the given entity and field.

   - Vector-config entities (admin tables) use the admin event.
   - Legacy/template entities use template list settings events."
  [vector-mode? entity-kw field-id]
  (rf/dispatch
    (if vector-mode?
      [:admin/toggle-column-visibility entity-kw field-id]
      [::settings-events/toggle-column-visibility entity-kw field-id])))

(defn- toggle-field-filtering! [entity-kw field-id]
  (rf/dispatch [::settings-events/toggle-field-filtering entity-kw field-id]))

(defui column-visibility-settings
  "Controls for column visibility"
  [{:keys [entity-name current-entity-name _global-settings?] :as props}]
  (let [entity-type (or current-entity-name entity-name)
        entity-kw (if (keyword? entity-type) entity-type (keyword entity-type))
        admin-config-loaded? (use-subscribe [:admin/config-loaded?])
        vector-mode? (and admin-config-loaded?
                       (column-config/vector-config? entity-kw))
        provided-entity-spec (:entity-spec props)
        ;; Call the hook unconditionally (Rules of Hooks). Use as fallback.
        {:keys [entity-spec] :as _spec-hook} (template-utils/use-entity-spec entity-kw :admin)
        effective-entity-spec (or provided-entity-spec entity-spec)
        entity-fields (cond
                        (and (map? effective-entity-spec) (:fields effective-entity-spec)) (:fields effective-entity-spec)
                        (sequential? effective-entity-spec) effective-entity-spec
                        (map? effective-entity-spec) (vals effective-entity-spec)
                        :else [])
        entity-config (use-subscribe [::admin-subs/entity-config entity-kw])
        ;; Unified visible columns subscription applies policy defaults/locks and route-aware sources.
        visible-columns (use-subscribe [::ui-subs/visible-columns entity-kw])
        locked-columns (use-subscribe [::ui-subs/locked-visible-columns entity-kw])
        locked-column-set (set (keys (or locked-columns {})))
        column-order (use-subscribe [::settings-events/column-order entity-kw])
        ;; Filterable columns come from config (if present). If missing, treat all as filter-configurable.
        filterable-columns (or (use-subscribe [::ui-subs/filterable-fields entity-kw]) [])
        filterable-state (or (use-subscribe [::settings-events/filterable-fields entity-kw]) {})
        ;; IMPORTANT: Normalize field ids to app-style keywords.
        ;; Specs and rows sometimes use namespaced keys (e.g. :admins/email),
        ;; while view-options/table-columns/prefs are keyed by non-namespaced app keys (e.g. :email).
        ;; If we don't normalize here, toggles dispatch updates but the UI reads a different key,
        ;; making it look like "nothing toggles".
        normalize-key (fn [k]
                        ;; Canonicalize to a simple app keyword (no namespace).
                        ;; Example: :admins/email -> :email, :created_at -> :created-at
                        (model-naming/ensure-app-keyword (kw/ensure-name k)))
        filterable-column-set (when (seq filterable-columns)
                                (into #{} (keep normalize-key) filterable-columns))
        filterable-state-normalized (into {}
                                      (keep (fn [[k v]]
                                              (when-let [nk (normalize-key k)]
                                                [nk v])))
                                      filterable-state)
        ;; Back-compat: always-visible also locks visibility.
        always-visible-set (set (map normalize-key (or (:always-visible entity-config) [])))
        ordered-entity-fields (column-config/order-fields entity-fields column-order)
        ordered-field-ids (->> ordered-entity-fields
                            (map (fn [field]
                                   (-> (column-config/field->id field)
                                     normalize-key)))
                            (remove nil?)
                            vec)
        [dragging-field-id set-dragging-field-id!] (use-state nil)
        dispatch-reorder! (fn [new-order]
                            (rf/dispatch
                              (if vector-mode?
                                [:admin/reorder-columns entity-kw new-order]
                                [::settings-events/set-column-order entity-kw new-order])))
        reorder-fields! (fn [from-id to-id]
                          (let [from-index (.indexOf ordered-field-ids from-id)
                                to-index (.indexOf ordered-field-ids to-id)]
                            (when (and (>= from-index 0)
                                    (>= to-index 0)
                                    (not= from-index to-index))
                              (dispatch-reorder!
                                (column-config/reorder-vector ordered-field-ids from-index to-index)))))]

    (when (seq entity-fields)
      ($ :div
        ;; For compact mode, use horizontal layout
        ($ :div {:class "flex flex-row flex-wrap gap-1"}
          ;; For each field in the entity, show a toggle to enable/disable visibility
          (for [field ordered-entity-fields
                :let [field-id (-> (cond
                                     (map? field) (:id field)
                                     (keyword? field) field
                                     (string? field) (keyword field)
                                     :else field)
                                 normalize-key)
                      raw-label (when (map? field) (:label field))
                      metadata-label (get-in entity-config [:column-metadata field-id :label])
                      field-label (or raw-label metadata-label (when field-id (-> field-id name str/capitalize)))
                      field-name (name field-id)
                      ;; Disable toggle for locked columns (policy locks + always-visible)
                      is-column-configurable? (and (not (contains? always-visible-set field-id))
                                                (not (contains? locked-column-set field-id)))
                      ;; If config doesn't specify filterable columns, treat all columns as filter-configurable.
                      is-filter-configurable? (if (set? filterable-column-set)
                                                (contains? filterable-column-set field-id)
                                                true)
                      ;; Determine visibility from the (possibly sparse) visible map (defaults to true)
                      is-column-visible? (let [sentinel ::not-found
                                               v (get visible-columns field-id sentinel)]
                                           (if (not= v sentinel) v true))
                      ;; Determine filterable state from per-entity overrides
                      is-field-filterable? (and is-filter-configurable?
                                             (get filterable-state-normalized field-id true))
                      ;; System timestamps should not show drag handles
                      is-system-column? (#{:created-at :updated-at} field-id)]
                :when field-id]
            ($ :div {:key (str "column-toggle-" field-name)
                     :id (str "col-toggle-row-" (name entity-kw) "-" field-name)
                     :class (str "relative inline-flex items-center gap-1"
                              (when (= dragging-field-id field-id) " opacity-60"))
                     :on-drag-over (fn [e]
                                     (.preventDefault e))
                     :on-drop (fn [e]
                                (.preventDefault e)
                                (let [dt (.-dataTransfer e)
                                      dragged-id (or dragging-field-id
                                                   (when dt
                                                     (.getData dt "text/plain"))
                                                   nil)
                                      normalized-dragged (cond
                                                           (keyword? dragged-id) (normalize-key dragged-id)
                                                           (string? dragged-id) (normalize-key (keyword dragged-id))
                                                           :else nil)]
                                  (when normalized-dragged
                                    (reorder-fields! normalized-dragged field-id))
                                  (set-dragging-field-id! nil)))}
              (when-not is-system-column?
                ($ :span {:id (str "col-drag-" (name entity-kw) "-" field-name)
                          :class "cursor-grab select-none text-gray-400 px-1"
                          :title "Drag to reorder"
                          :draggable true
                          :on-click (fn [e] (.stopPropagation e))
                          :on-drag-start (fn [e]
                                           (set-dragging-field-id! field-id)
                                           (when-let [dt (.-dataTransfer e)]
                                             (.setData dt "text/plain" (name field-id))))
                          :on-drag-end (fn [_e]
                                         (set-dragging-field-id! nil))}
                  "⋮⋮"))
              ;; Main column visibility button
              ($ :button {:id (str "col-toggle-" (name entity-kw) "-" field-name)
                          :key (str "btn-" field-name)
                          :class (str "btn btn-sm m-1 mt-2 font-light pr-4 "
                                   (cond
                                     is-column-visible? "font-semibold"
                                     :else "font-light")
                                   (when (not is-column-configurable?)
                                     " cursor-not-allowed"))
                          :disabled (not is-column-configurable?)
                          :title (when (not is-column-configurable?)
                                   (cond
                                     (contains? always-visible-set field-id) "This column is always visible"
                                     (contains? locked-column-set field-id) "This column visibility is locked by policy"
                                     :else "This column is not configurable"))
                          :on-click (fn [_e]
                                      (when is-column-configurable?
                                        (toggle-column! vector-mode? entity-kw field-id)))}
                field-label)

              ;; Filter icon overlay - show if column is visible and filterable
              (when (and is-column-visible? is-filter-configurable?)
                ($ :div {:class "absolute right-1 top-1/2 transform -translate-y-1/2 flex items-center"}
                  ($ filter-icon {:id (str "filter-toggle-" (name entity-kw) "-" field-name)
                                  :active? (and is-filter-configurable? is-field-filterable?)
                                  :disabled? (not is-filter-configurable?)
                                  :title (when (not is-filter-configurable?)
                                           "Filtering is not available for this column")
                                  :on-click (fn [e]
                                              (.stopPropagation e) ; Prevent triggering the parent button's click
                                              (when is-filter-configurable?
                                                ;; Dispatch the filter toggle helper
                                                (toggle-field-filtering! entity-kw field-id)))}))))))))))

(defui list-view-settings-panel
  "Controls for table display options"
  [{:keys [entity-name _show-timestamps? _show-edit? _show-delete? _show-highlights? _show-select? _show-filtering?
           global-settings? current-entity-name compact? entity-spec hardcoded-display-settings
           per-page on-per-page-change rows-per-page-options]}]

  (let [;; Normalize entity name to keyword consistently
        entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))

        ;; Subscribe to entity-specific display settings - SINGLE subscription
        entity-settings (use-subscribe [::ui-subs/entity-display-settings entity-kw])

        ;; Current browser overrides that can mask policy defaults.
        local-display-prefs (or (use-subscribe [::ui-subs/entity-display-prefs entity-kw]) {})
        local-overrides-count (count local-display-prefs)

        ;; Always call hooks unconditionally
        entity-type-sub (use-subscribe [::core-subs/entity-type])
        ;; Then use their results conditionally
        curr-entity-name (or current-entity-name entity-type-sub)

        ;; Current setting values - directly from entity-settings subscription
        curr-show-edit? (:show-edit? entity-settings)
        curr-show-delete? (:show-delete? entity-settings)
        curr-show-highlights? (:show-highlights? entity-settings)
        curr-show-select? (:show-select? entity-settings)
        curr-show-pagination? (:show-pagination? entity-settings)
        curr-show-timestamps? (:show-timestamps? entity-settings)
        curr-show-filtering? (:show-filtering? entity-settings)
        curr-show-add-button? (:show-add-button? entity-settings)
        curr-show-batch-edit? (:show-batch-edit? entity-settings)
        curr-show-batch-delete? (:show-batch-delete? entity-settings)

        ;; Helper component for toggle buttons
        ;; Locked settings (from resolver) are hidden from the UI
        toggle-button (fn [{:keys [label is-active? event-key hardcoded-key]}]
                        (let [;; Check if this setting is locked (hardcoded at policy level)
                              is-locked? (and hardcoded-display-settings
                                           hardcoded-key
                                           (contains? hardcoded-display-settings hardcoded-key))
                              toggle-id (str "toggle-" (-> label str/lower-case (str/replace #"\s+" "-")) "-" (name entity-kw))]
                          ;; Hide the control entirely if it's locked
                          (when (not is-locked?)
                            ($ :div {:id toggle-id
                                     :class "flex justify-between items-center p-1 rounded-md cursor-pointer"
                                     :on-click #(rf/dispatch [event-key entity-kw])}
                              ($ :span {:class (str "text-sm "
                                                 (if is-active? "font-bold" "font-light"))}
                                label)))))]
    ($ :div {:id "panel"
             :class "flex flex-col gap-4"}

      (when (seq local-display-prefs)
        ($ :div {:id (str "local-overrides-" (name entity-kw))
                 :class "flex items-center justify-between gap-2 p-2 rounded-lg bg-base-200"}
          ($ :span {:class "text-xs text-base-content/70"}
            "Local overrides are active in this browser; they can override Defaults until cleared.")
          ($ :button {:type "button"
                      :id (str "btn-clear-local-display-prefs-" (name entity-kw))
                      :class "btn btn-xs btn-ghost"
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (rf/dispatch [::ui-events/clear-display-prefs entity-kw]))}
            (str "Clear local overrides"
              (when (pos? local-overrides-count)
                (str " (" local-overrides-count ")"))))))

      ;; Entity header for global settings
      (when (not compact?)
        ($ :div {:id "entity-controls-header"
                 :class "mb-2"}
          ($ :span {:class "text-sm"}
            (str "Controls for " (str/capitalize (name curr-entity-name))))
          ($ :div {:class "border-t border-gray-200 my-2"})))

      ;; FIRST ROW: Main controls (Edit, Delete, Highlights, Selection, Timestamps, Table Width, Rows Per Page)
      ($ :div {:id "main-controls-row"
               :class "flex flex-row flex-wrap gap-4 items-center p-3 bg-base-100 rounded-lg shadow-sm"}

        ;; Edit control
        (toggle-button {:label "Edit"
                        :is-active? curr-show-edit?
                        :event-key ::ui-events/toggle-edit
                        :hardcoded-key :show-edit?})

        ;; Delete control
        (toggle-button {:label "Delete"
                        :is-active? curr-show-delete?
                        :event-key ::ui-events/toggle-delete
                        :hardcoded-key :show-delete?})

        ;; Highlights control
        (toggle-button {:label "Highlights"
                        :is-active? curr-show-highlights?
                        :event-key ::ui-events/toggle-highlights
                        :hardcoded-key :show-highlights?})

        ;; Selection control
        (toggle-button {:label "Selection"
                        :is-active? curr-show-select?
                        :event-key ::ui-events/toggle-select
                        :hardcoded-key :show-select?})

        ;; Pagination control
        (toggle-button {:label "Pagination"
                        :is-active? curr-show-pagination?
                        :event-key ::ui-events/toggle-pagination
                        :hardcoded-key :show-pagination?})

        ;; Timestamps control
        (toggle-button {:label "Timestamps"
                        :is-active? curr-show-timestamps?
                        :event-key ::ui-events/toggle-timestamps
                        :hardcoded-key :show-timestamps?})

        ;; Filtering control (global master switch)
        (toggle-button {:label "Filtering"
                        :is-active? curr-show-filtering?
                        :event-key ::ui-events/toggle-filtering
                        :hardcoded-key :show-filtering?})

        ;; Add button control
        (toggle-button {:label "Add Button"
                        :is-active? curr-show-add-button?
                        :event-key ::ui-events/toggle-add-button
                        :hardcoded-key :show-add-button?})

        ;; Batch Edit control
        (toggle-button {:label "Batch Edit"
                        :is-active? curr-show-batch-edit?
                        :event-key ::ui-events/toggle-batch-edit
                        :hardcoded-key :show-batch-edit?})

        ;; Batch Delete control
        (toggle-button {:label "Batch Delete"
                        :is-active? curr-show-batch-delete?
                        :event-key ::ui-events/toggle-batch-delete
                        :hardcoded-key :show-batch-delete?})

        ;; Table width control
        (let [current-width (use-subscribe [::settings-events/table-width entity-kw])
              [temp-width, set-temp-width] (use-state (str current-width))]
          ($ :div {:id "table-width-control"
                   :class "flex items-center gap-2 p-1 rounded-md"}
            ($ :span {:class "text-sm font-medium"} "Table Width:")
            ($ :input {:id (str "table-width-input-" (name entity-kw))
                       :type "number"
                       :min "800"
                       :max "3000"
                       :step "100"
                       :value temp-width
                       :class "w-20 px-2 py-1 border border-gray-300 rounded text-sm"
                       :on-change (fn [e]
                                    (let [new-value (.. e -target -value)]
                                      (set-temp-width new-value)))
                       :on-blur (fn [e]
                                  (let [new-value (.. e -target -value)]
                                    (when (and new-value (not= new-value (str current-width)))
                                      (rf/dispatch (column-config/update-table-width-event entity-kw (js/parseInt new-value))))))
                       :on-key-down (fn [e]
                                      (when (= (.-key e) "Enter")
                                        (let [new-value (.. e -target -value)]
                                          (when (and new-value (not= new-value (str current-width)))
                                            (rf/dispatch (column-config/update-table-width-event entity-kw (js/parseInt new-value)))
                                            (.blur (.-target e))))))})))

        ;; Rows per page control - moved from pagination component
        (when (and per-page on-per-page-change rows-per-page-options)
          ($ :div {:id "rows-per-page-control"
                   :class "flex items-center gap-2 p-1 rounded-md"}
            ($ :span {:class "text-sm font-medium"} "Rows per page:")
            ($ :select {:id (str "rows-per-page-" (name entity-kw))
                        :value per-page
                        :class "w-20 px-2 py-1 border border-gray-300 rounded text-sm"
                        :on-change #(on-per-page-change (js/parseInt (.. % -target -value)))}
              (for [option rows-per-page-options]
                ($ :option {:key option :value option} option))))))

      ;; SECOND ROW: Column visibility settings
      ($ :div {:id "column-visibility-row"
               :class "p-3 bg-base-100 rounded-lg shadow-sm"}

        ;; Section header
        ($ :div {:id "column-visibility-header"
                 :class "mb-3"}
          ($ :span {:class "text-sm font-medium"} "Column Visibility")
          ($ :div {:class "border-t border-gray-200 my-1"}))

        ;; Column visibility settings
        ($ :div {:id "column-visibility-section"
                 :class "flex flex-row flex-wrap gap-1"}
          ($ column-visibility-settings {:entity-name entity-name
                                         :current-entity-name current-entity-name
                                         :entity-spec entity-spec
                                         :global-settings? global-settings?}))))))
