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
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- toggle-column! [_vector-mode? entity-kw field-id]
  (rf/dispatch [:admin/toggle-column-visibility entity-kw field-id]))

(defn- toggle-field-filtering! [entity-kw field-id]
  (rf/dispatch [::settings-events/toggle-field-filtering entity-kw field-id]))

(defui column-visibility-settings
  "Controls for column visibility"
  [{:keys [entity-name current-entity-name global-settings?]}]
  (let [entity-type (or current-entity-name entity-name)
        entity-kw (if (keyword? entity-type) entity-type (keyword entity-type))
        ;; Use modern admin entity spec subscription
        {:keys [entity-spec]} (template-utils/use-entity-spec entity-kw :admin)
        entity-fields (cond
                        (and (map? entity-spec) (:fields entity-spec)) (:fields entity-spec)
                        (sequential? entity-spec) entity-spec
                        (map? entity-spec) (vals entity-spec)
                        :else [])
        entity-config (use-subscribe [::admin-subs/entity-config entity-kw])
        visible-columns (or (use-subscribe [::admin-subs/visible-columns entity-kw]) [])
        filterable-columns (or (use-subscribe [:admin/filterable-columns entity-kw]) [])
        filterable-state (or (use-subscribe [::settings-events/filterable-fields entity-kw]) {})
        normalize-key (fn [k]
                        (cond
                          (nil? k) nil
                          (keyword? k) k
                          (string? k) (keyword k)
                          :else (keyword (str k))))
        visible-column-set (into #{} (keep normalize-key) visible-columns)
        filterable-column-set (into #{} (keep normalize-key) filterable-columns)
        filterable-state-normalized (into {}
                                      (keep (fn [[k v]]
                                              (when-let [nk (normalize-key k)]
                                                [nk v])))
                                      filterable-state)
        always-visible-set (set (map normalize-key (or (:always-visible entity-config) [])))]

    (when (seq entity-fields)
      ($ :div
        ;; For compact mode, use horizontal layout
        ($ :div {:class "flex flex-row flex-wrap gap-1"}
          ;; For each field in the entity, show a toggle to enable/disable visibility
          (for [field entity-fields
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
                      ;; Disable toggle for always-visible columns
                      is-column-configurable? (not (contains? always-visible-set field-id))
                      ;; Check if this column is in the filterable-columns set
                      is-filter-configurable? (contains? filterable-column-set field-id)
                      ;; Determine visibility directly from subscription
                      is-column-visible? (contains? visible-column-set field-id)
                      ;; Determine filterable state from per-entity overrides
                      is-field-filterable? (and is-filter-configurable?
                                             (get filterable-state-normalized field-id true))]
                :when field-id]
            ($ :div {:key (str "column-toggle-" field-name)
                     :class "relative inline-block"}
              ;; Main column visibility button
              ($ :button {:key (str "btn-" field-name)
                          :class (str "btn btn-sm m-1 mt-2 font-light pr-4 "
                                   (cond
                                     is-column-visible? "font-semibold"
                                     :else "font-light")
                                   (when (not is-column-configurable?)
                                     " cursor-not-allowed"))
                          :disabled (not is-column-configurable?)
                          :title (when (not is-column-configurable?)
                                   "This column is not configurable")
                          :on-click (fn [e]
                                      (when is-column-configurable?
                                        (toggle-column! true entity-kw field-id)))}
                field-label)

              ;; Filter icon overlay - show if column is visible and filterable
              (when (and is-column-visible? is-filter-configurable?)
                ($ :div {:class "absolute right-1 top-1/2 transform -translate-y-1/2 flex items-center"}
                  ($ filter-icon {:active? (and is-filter-configurable? is-field-filterable?)
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
  [{:keys [entity-name show-timestamps? show-edit? show-delete? show-highlights? show-select? show-filtering?
           global-settings? current-entity-name compact? entity-spec hardcoded-display-settings
           per-page on-per-page-change rows-per-page-options]}]

  (let [;; Normalize entity name to keyword consistently
        entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))

        ;; Subscribe to entity-specific display settings - SINGLE subscription
        entity-settings (use-subscribe [::ui-subs/entity-display-settings entity-kw])

        ;; Always call hooks unconditionally
        entity-type-sub (use-subscribe [::core-subs/entity-type])
        ;; Then use their results conditionally
        curr-entity-name (or current-entity-name entity-type-sub)

        controls (:controls entity-settings)
        {:keys [show-timestamps-control? show-edit-control?
                show-delete-control? show-highlights-control?
                show-select-control?]} (or controls {})

        current-map (use-subscribe [::settings-events/filterable-fields entity-kw])

        ;; Current setting values - directly from entity-settings subscription
        curr-show-timestamps? (:show-timestamps? entity-settings)
        show-timestamp-filter-icon? (and (:created-at current-map) curr-show-timestamps?)
        curr-show-edit? (:show-edit? entity-settings)
        curr-show-delete? (:show-delete? entity-settings)
        curr-show-highlights? (:show-highlights? entity-settings)
        curr-show-select? (:show-select? entity-settings)
        curr-show-filtering? (:show-filtering? entity-settings)
        curr-show-pagination? (:show-pagination? entity-settings)

        ;; Helper component for toggle buttons
        ;; Hardcoded settings are now completely hidden from the UI
        toggle-button (fn [{:keys [label is-active? control-visible? event-key field-keys show-filter-icon? hardcoded-key]}]
                        (let [should-show (if (nil? control-visible?) true control-visible?)
                              ;; Check if this control is hardcoded at page level
                              is-hardcoded? (and hardcoded-display-settings
                                              hardcoded-key
                                              (contains? hardcoded-display-settings hardcoded-key))
                              toggle-id (str "toggle-" (-> label str/lower-case (str/replace #"\s+" "-")))]
                          ;; Hide the control entirely if it's hardcoded
                          (when (and should-show (not is-hardcoded?))
                            ($ :div {:id toggle-id
                                     :class "flex justify-between items-center p-1 rounded-md cursor-pointer"
                                     :on-click #(rf/dispatch [event-key entity-kw])}
                              ($ :span {:class (str "text-sm "
                                                 (if is-active? "font-bold" "font-light"))}
                                label)
                              (when (and show-filter-icon? is-active?)
                                ($ :div {:class "flex items-center gap-1"}
                                  (let [on-click (fn [e]
                                                   (.stopPropagation e)
                                                   (doseq [field-key field-keys]
                                                     (toggle-field-filtering! entity-kw field-key)))]
                                    ($ filter-icon {:active? (if (= label "Timestamps") show-timestamp-filter-icon? curr-show-filtering?)
                                                    :on-click on-click}))))))))]
    ($ :div {:id "panel"
             :class "flex flex-col gap-4"}

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
                        :control-visible? show-edit-control?
                        :event-key ::ui-events/toggle-edit
                        :hardcoded-key :show-edit?})

        ;; Delete control
        (toggle-button {:label "Delete"
                        :is-active? curr-show-delete?
                        :control-visible? show-delete-control?
                        :event-key ::ui-events/toggle-delete
                        :hardcoded-key :show-delete?})

        ;; Highlights control
        (toggle-button {:label "Highlights"
                        :is-active? curr-show-highlights?
                        :control-visible? show-highlights-control?
                        :event-key ::ui-events/toggle-highlights
                        :hardcoded-key :show-highlights?})

        ;; Selection control
        (toggle-button {:label "Selection"
                        :is-active? curr-show-select?
                        :control-visible? show-select-control?
                        :event-key ::ui-events/toggle-select
                        :hardcoded-key :show-select?})

        ;; Timestamps control
        (toggle-button {:label "Timestamps"
                        :is-active? curr-show-timestamps?
                        :control-visible? show-timestamps-control?
                        :event-key ::ui-events/toggle-timestamps
                        :show-filter-icon? true
                        :field-keys [:created-at :updated-at]
                        :hardcoded-key :show-timestamps?})

        ;; Pagination control
        (toggle-button {:label "Pagination"
                        :is-active? curr-show-pagination?
                        :control-visible? true
                        :event-key ::ui-events/toggle-pagination
                        :hardcoded-key :show-pagination?})

        ;; Table width control
        (let [current-width (use-subscribe [::settings-events/table-width entity-kw])
              [temp-width, set-temp-width] (use-state (str current-width))]
          ($ :div {:id "table-width-control"
                   :class "flex items-center gap-2 p-1 rounded-md"}
            ($ :span {:class "text-sm font-medium"} "Table Width:")
            ($ :input {:type "number"
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
            ($ :select {:id "rows-per-page"
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
