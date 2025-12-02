(ns app.template.frontend.components.list.table
  (:require
    [app.shared.keywords :as kw]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon
                                                    filter-icon]]
    [app.template.frontend.components.list.rows :refer [select-all-checkbox]]
    [app.template.frontend.events.list.batch :as batch-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.subs.ui :as ui-subs]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

(defui table-header [{:keys [label sortable? on-click sort-direction filter-on-click filter-active? show-filtering? is-field-filterable? header-id active-inline-filter? field-id column-width]}]

  ($ :div
    {:class (str (when sortable? "cursor-pointer ")
              "flex items-center gap-2")
     :on-click on-click
     :id header-id
     ;; NEW: Apply column width from admin metadata if specified
     :style (when column-width {:width column-width :minWidth column-width})}
    ;; Always render label + filter icon placeholder in a flex row with consistent width
    ($ :span {:class "inline-flex items-center gap-1"}
      (if (or (string? label) (number? label))
        ($ :span {:class "font-bold"} (str label))
        label)
      ;; Always reserve space for the filter icon, show or hide based on conditions
      ($ :span {:class "w-4 flex justify-center"}
        (if (and show-filtering? is-field-filterable?)
          ;; Show the actual filter icon when conditions are met
          ($ filter-icon {:on-click filter-on-click
                          :active? (or filter-active? active-inline-filter?)
                          :field-id field-id})
          ;; Empty placeholder to maintain layout when filter icon is hidden
          nil)))
    ;; Always reserve space for the sort arrow
    ($ :span {:class "text-sm font-bold text-gray-600 inline-block w-4 text-center align-middle select-none"
              :style {:minWidth "1em"}}
      (cond
        (and sortable? sort-direction)
        (case sort-direction
          :asc "↑"
          :desc "↓"
          "")
        sortable? " "                                       ;; placeholder for unsorted sortable columns
        :else ""))))

(defui action-header-buttons
  "Action buttons for the table header"
  [{:keys [entity-name selected-ids show-batch-edit?]}]
  (let [has-selection? (seq selected-ids)
        selection-count (count selected-ids)
        has-multiple-selection? (and has-selection? (>= selection-count 2))
        show-batch-edit? (if (nil? show-batch-edit?) true show-batch-edit?)

        ;; Define a separate function to handle batch delete confirmation
        handle-batch-delete-confirm (fn []
                                      (rf/dispatch [::selection-events/delete-selected entity-name selected-ids]))

        ;; Create a function that will only show the dialog when clicked
        handle-batch-delete-click (fn [e]
                                    (when has-multiple-selection?
                                      (.stopPropagation e)
                                      (let [msg (str "Do you want to delete " selection-count " records?")]
                                        (confirm-dialog/show-confirm
                                          {:message msg
                                           :title "Confirm Batch Delete"
                                           :on-confirm handle-batch-delete-confirm
                                           :on-cancel nil}))))

        ;; Handle three dots menu click to open batch actions panel
        handle-three-dots-click (fn [e]
                                  (when has-multiple-selection?
                                    (.stopPropagation e)
                                    ;; Dispatch entity-specific batch actions based on entity type
                                    (case entity-name
                                      :users (rf/dispatch [:admin/show-batch-user-actions selected-ids])
                                      :tenants (rf/dispatch [:admin/show-batch-tenant-actions selected-ids])
                                      ;; Default fallback - could be extended for other entities
                                      (log/warn "No batch actions defined for entity:" entity-name))))]
    ($ :div {:class "flex items-center gap-2 overflow-visible flex-shrink-0"}
      ;; Edit button - now enabled only when multiple items are selected AND batch edit is allowed
      (when show-batch-edit?
        ($ button
          {:btn-type :primary
           :shape "circle"
           :disabled (not has-multiple-selection?)
           :id (str "btn-batch-edit-" (kw/ensure-name entity-name))
           :class "!w-10 !h-10"
           :title (cond
                    has-multiple-selection?
                    (str "Edit " selection-count " selected items")

                    has-selection?
                    "Select at least 2 items to enable batch edit"

                    :else
                    "Select items to edit")
           :on-click #(when has-multiple-selection?
                        (rf/dispatch [::batch-events/show-batch-edit-inline entity-name selected-ids]))
           :children ($ edit-icon)}))

      ;; Delete button - respect batch edit visibility toggle as well
      (when show-batch-edit?
        ($ button
          {:btn-type :danger
           :shape "circle"
           :disabled (not has-multiple-selection?)
           :id (str "btn-batch-delete-" (kw/ensure-name entity-name))
           :class "!w-10 !h-10"
           :title (cond
                    has-multiple-selection?
                    (str "Delete " selection-count " selected items")

                    has-selection?
                    "Select at least 2 items to enable batch delete"

                    :else
                    "Select items to delete")
           :on-click handle-batch-delete-click
           :children ($ delete-icon)}))

      ;; Three dots menu button - only visible when 2+ items selected and batch ops enabled
      (when (and show-batch-edit? has-multiple-selection?)
        ($ button
          {:btn-type :ghost
           :shape "circle"
           :id (str "btn-batch-actions-" (kw/ensure-name entity-name))
           :class "!w-10 !h-10"
           :title (str "More actions for " selection-count " selected items")
           :on-click handle-three-dots-click
           :children ($ :span {:class "text-lg font-bold"} "⋯")})))))

(defui reactive-select-all-header
  "A reactive header cell that subscribes to show-select? and conditionally renders the select-all checkbox.
   This ensures the select-all checkbox visibility responds immediately to toggling in the settings panel."
  [{:keys [entity-name all-items selected-ids on-select-all]}]
  (let [;; Subscribe directly to entity display settings for reactivity
        entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
        entity-settings (use-subscribe [::ui-subs/entity-display-settings entity-key])
        show-select? (:show-select? entity-settings)]
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

(defn- make-table-headers- [{:keys [entity-spec entity-name show-timestamps? show-select? show-filtering? show-batch-edit? sort-field sort-direction all-items selected-ids on-select-all active-filters filterable-fields user-filterable-settings visible-columns active-inline-filter on-inline-filter-click]}]
  (let [;; Start with base headers from entity spec fields
        entity-fields (cond
                        (and (map? entity-spec) (contains? entity-spec :fields))
                        (:fields entity-spec)
                        (map? entity-spec) (->> entity-spec
                                             ;; Sort the map entries by display-order first, then extract values
                                             ;; This ensures we preserve the order we want
                                             (sort-by (fn [[_ field-spec]]
                                                        (or (get-in field-spec [:admin :display-order]) 999)))
                                             (map second)   ; Get the values in sorted order
                                             (filter map?)  ; Only keep maps
                                             (filter #(contains? % :id))) ; Only keep field definitions with :id
                        (sequential? entity-spec) entity-spec
                        :else [])

;; Filter out the raw timestamp fields to avoid duplication
        filtered-entity-fields (remove (fn [field]
                                         (let [field-id (keyword (:id field))]
                                           (#{:created-at :updated-at} field-id)))
                                 entity-fields)

        ;; Selection header using reactive component that subscribes to show-select?
        ;; This ensures the header updates immediately when the user toggles the Selection setting
        select-header [(fn []
                         ($ reactive-select-all-header
                           {:entity-name entity-name
                            :all-items all-items
                            :selected-ids selected-ids
                            :on-select-all on-select-all}))]

        ;; Create base headers from entity fields using vector-config only (no legacy :admin metadata)
        ;; Pre-compute filterable and sortable sets from vector-config (if available)
        sortable-set (let [sc (use-subscribe [:admin/sortable-columns entity-name])]
                       (when (coll? sc) (set sc)))
        filterable-set (cond
                         (map? filterable-fields) filterable-fields
                         (coll? filterable-fields) (set filterable-fields)
                         :else nil)

        base-headers (mapv (fn [field]
                             (let [field-id (keyword (:id field))
                                   ;; Use vector-config driven visibility map directly
                                   is-column-visible? (let [user-setting (get visible-columns field-id ::not-found)]
                                                        (if (not= user-setting ::not-found) user-setting true))
                                   ;; Filterable if user has enabled it in settings panel AND field is in filterable-set
                                   is-field-filterable? (let [user-setting (get user-filterable-settings field-id ::not-found)]
                                                          (if (not= user-setting ::not-found)
                                                          ;; User has explicitly set filterable setting - use it
                                                            user-setting
                                                          ;; No user setting, check vector config for filterable columns
                                                            (cond
                                                              (map? filterable-set) (boolean (get filterable-set field-id true))
                                                              (set? filterable-set) (contains? filterable-set field-id)
                                                              :else true)))
                                   ;; Sortable if listed in vector-config sortable-set, otherwise default true
                                   is-sortable? (if (set? sortable-set)
                                                  (contains? sortable-set field-id)
                                                  true)
                                   ;; Width comes from vector-config field only
                                   column-width (:width field)]

                               ;; Only include this header if the column is visible
                               (if is-column-visible?
                                 (fn []
                                   ($ table-header
                                     {:key (str "header-" (name field-id))
                                      :header-id (str "header-" (name field-id))
                                      :label (:label field)
                                      :sortable? is-sortable?
                                      :on-click (when is-sortable?
                                                  #(rf/dispatch [::ui-events/set-sort-field entity-name field-id]))
                                      :sort-direction (when (= sort-field field-id) sort-direction)
                                      :filter-on-click #(do
                                                          (.stopPropagation %)
                                                          (when on-inline-filter-click
                                                            (on-inline-filter-click field-id field)))
                                      :filter-active? (contains? active-filters field-id)
                                      :active-inline-filter? (= active-inline-filter field-id)
                                      :show-filtering? show-filtering?
                                      :is-field-filterable? is-field-filterable?
                                      :field-id field-id
                                      ;; NEW: Add column width styling if specified
                                      :column-width column-width}))
                                 ;; Return nil for invisible columns
                                 nil)))
                       filtered-entity-fields)

        ;; Filter out nil values (from columns that should be hidden)
        filtered-base-headers (filterv some? base-headers)

        ;; Timestamp headers for created_at and updated_at - using namespaced field names
        timestamp-headers (when show-timestamps?
                            (let [created-key :created-at
                                  created-legacy-key :created-at
                                  updated-key :updated-at
                                  updated-legacy-key :updated-at
                                  legacy-map {created-key created-legacy-key
                                              updated-key updated-legacy-key}
                                  sentinel ::not-found
                                  resolve-setting (fn [settings key]
                                                    (let [legacy (get legacy-map key)
                                                          current (get settings key sentinel)
                                                          legacy-val (if legacy (get settings legacy sentinel) sentinel)]
                                                      (cond
                                                        (not= current sentinel) current
                                                        (not= legacy-val sentinel) legacy-val
                                                        :else nil)))
                                  column-visible? (fn [key]
                                                    (let [value (resolve-setting visible-columns key)]
                                                      (if (nil? value) true value)))
                                  field-filterable? (fn [key]
                                                      (let [user-setting (resolve-setting (or filterable-fields {}) key)]
                                                        (if (not= user-setting ::not-found)
                                                          ;; User has explicitly set filterable setting - use it
                                                          user-setting
                                                          ;; No user setting, check vector config for filterable columns
                                                          (let [filterable-columns (use-subscribe [:app.template.frontend.subs.ui/filterable-fields entity-name])]
                                                            (if (seq filterable-columns)
                                                              ;; Use vector config filterable columns
                                                              (contains? (set filterable-columns) key)
                                                              ;; No vector config, default to true
                                                              true)))))
                                  filter-active? (fn [key]
                                                   (let [legacy (get legacy-map key)]
                                                     (or (contains? active-filters key)
                                                       (and legacy (contains? active-filters legacy)))))
                                  inline-active? (fn [key]
                                                   (let [legacy (get legacy-map key)]
                                                     (boolean
                                                       (some #(= active-inline-filter %)
                                                         (remove nil? [key legacy])))))
                                  namespaced (fn [key]
                                               (keyword (kw/ensure-name entity-name)
                                                 (kw/ensure-name key)))]
                              [(fn []
                                 (when (column-visible? created-key)
                                   ($ table-header
                                     {:key "header-created-at"
                                      :header-id "header-created-at"
                                      :label "Created"
                                      :sortable? true
                                      :on-click #(rf/dispatch [::ui-events/set-sort-field entity-name (namespaced created-key)])
                                      :sort-direction (when (= sort-field (namespaced created-key)) sort-direction)
                                      :filter-on-click #(do
                                                          (.stopPropagation %)
                                                          (when on-inline-filter-click
                                                            (on-inline-filter-click created-key {:id "created-at" :label "Created At" :input-type "datetime"})))
                                      :filter-active? (filter-active? created-key)
                                      :active-inline-filter? (inline-active? created-key)
                                      :show-filtering? show-filtering?
                                      :is-field-filterable? (field-filterable? created-key)
                                      :field-id created-key})))
                               (fn []
                                 (when (column-visible? updated-key)
                                   ($ table-header
                                     {:key "header-updated-at"
                                      :header-id "header-updated-at"
                                      :label "Updated"
                                      :sortable? true
                                      :on-click #(rf/dispatch [::ui-events/set-sort-field entity-name (namespaced updated-key)])
                                      :sort-direction (when (= sort-field (namespaced updated-key)) sort-direction)
                                      :filter-on-click #(do
                                                          (.stopPropagation %)
                                                          (when on-inline-filter-click
                                                            (on-inline-filter-click updated-key {:id "updated-at" :label "Updated At" :input-type "datetime"})))
                                      :filter-active? (filter-active? updated-key)
                                      :active-inline-filter? (inline-active? updated-key)
                                      :show-filtering? show-filtering?
                                      :is-field-filterable? (field-filterable? updated-key)
                                      :field-id updated-key})))]))

        ;; Filter out nil values from timestamp headers (from columns that should be hidden)
        filtered-timestamp-headers (filterv (fn [header-fn]
                                              (let [result (header-fn)]
                                                (some? result)))
                                     timestamp-headers)

        ;; Action header for edit and delete buttons with better overflow handling
        action-header [(fn []
                         ($ :div
                           {:key "header-actions"
                            :class "flex justify-start items-center overflow-visible min-h-[56px]"}
                           ($ action-header-buttons
                             {:entity-name entity-name
                              :selected-ids selected-ids
                              :show-batch-edit? show-batch-edit?})))]]

    ;; Combine all headers in the correct order: select, base fields, timestamps, actions
    ;; Always include the select header - the reactive-select-all-header component
    ;; handles visibility internally by subscribing to show-select?
    (vec (concat
           select-header
           filtered-base-headers
           filtered-timestamp-headers
           action-header))))

(defn make-table-headers [{:keys [entity-spec entity-name show-timestamps? show-select? show-filtering? show-batch-edit? sort-field sort-direction all-items selected-ids on-select-all active-filters filterable-fields user-filterable-settings visible-columns active-inline-filter on-inline-filter-click]}]
  (make-table-headers- {:entity-spec entity-spec
                        :entity-name entity-name
                        :show-timestamps? show-timestamps?
                        :show-select? show-select?
                        :show-filtering? show-filtering?
                        :show-batch-edit? show-batch-edit?
                        :sort-field sort-field
                        :sort-direction sort-direction
                        :all-items all-items
                        :selected-ids selected-ids
                        :on-select-all on-select-all
                        :active-filters active-filters
                        :filterable-fields filterable-fields
                        :user-filterable-settings user-filterable-settings
                        :visible-columns visible-columns
                        :active-inline-filter active-inline-filter
                        :on-inline-filter-click on-inline-filter-click}))
