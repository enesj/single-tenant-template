(ns app.template.frontend.components.list.table
  (:require
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon
                                                    filter-icon]]
    [app.template.frontend.components.list.cells :as cells]
    [app.template.frontend.events.list.batch :as batch-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.i18n :as i18n]
    [app.template.frontend.utils.column-config :as column-config]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :as uix :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui table-header [{:keys [label sortable? on-click sort-direction sort-priority filter-on-click filter-active? show-filtering? is-field-filterable? header-id active-inline-filter? field-id column-width]}]
  ($ :div
    {:class (str (when sortable? "cursor-pointer ")
              "flex h-full min-h-[4.5rem] w-full min-w-0 flex-col items-start")
     :on-click on-click
     :id header-id
     :style (when column-width {:width column-width :minWidth column-width})}
    ($ :span {:class "min-w-0 w-full whitespace-normal break-words leading-tight"}
      (if (or (string? label) (number? label))
        ($ :span {:class "font-bold"} (str label))
        label))
    ($ :span {:class "mt-auto flex items-center gap-2"}
      ($ :span {:class "flex w-4 justify-center"}
        (when (and show-filtering? is-field-filterable?)
          ($ filter-icon {:on-click filter-on-click
                          :active? (or filter-active? active-inline-filter?)
                          :field-id field-id})))
      ($ :span {:class "text-sm font-bold text-gray-600 inline-block w-4 text-center align-middle select-none"
                :style {:minWidth "1em"}}
        (cond
          (and sortable? sort-direction)
          (case sort-direction
            :asc "↑"
            :desc "↓"
            "")
          sortable? " "
          :else ""))
      (when sort-priority
        ($ :span {:class "rounded-full bg-base-200 px-2 py-0.5 text-[10px] font-semibold text-warning"
                  :title (str "Sort priority " sort-priority)}
          (str sort-priority))))))

(defui action-header-buttons
  "Action buttons for the table header"
  [{:keys [entity-name selected-ids show-batch-edit? show-batch-delete? on-batch-delete]}]
  (let [has-selection? (seq selected-ids)
        selection-count (count selected-ids)
        has-multiple-selection? (and has-selection? (>= selection-count 2))
        show-batch-edit? (if (nil? show-batch-edit?) true show-batch-edit?)
        ;; show-batch-delete? defaults to true if not specified, independent of show-batch-edit?
        show-batch-delete? (if (nil? show-batch-delete?) true show-batch-delete?)

        ;; Define a separate function to handle batch delete confirmation
        ;; Route entity-specific batch deletes through their admin endpoints
        ;; Allow callers to override batch delete for domain/user-specific endpoints.
        handle-batch-delete-confirm (fn []
                                      (cond
                                        on-batch-delete
                                        (on-batch-delete (vec selected-ids))

                                        :else
                                        (case entity-name
                                          :audit-logs (rf/dispatch [:admin/bulk-delete-audit-logs (vec selected-ids)])
                                          :login-events (rf/dispatch [:admin/bulk-delete-login-events (vec selected-ids)])
                                          ;; Default to generic template batch delete
                                          (rf/dispatch [::selection-events/delete-selected entity-name selected-ids]))))

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
                                      :audit-logs (rf/dispatch [:admin/show-batch-audit-actions selected-ids])
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

      ;; Delete button - now controlled independently by show-batch-delete?
      (when show-batch-delete?
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

;; Re-export reactive-select-all-header from cells module for backward compatibility
(def reactive-select-all-header cells/reactive-select-all-header)

(defn- make-table-headers-
  [{:keys [entity-spec entity-name show-filtering? show-batch-edit? show-batch-delete? on-batch-delete
           sorts all-items selected-ids on-select-all active-filters
           filterable-fields user-filterable-settings visible-columns column-order
           active-inline-filter on-inline-filter-click]}]
  (let [;; Start with base headers from entity spec fields
        entity-fields (cond
                        (and (map? entity-spec) (contains? entity-spec :fields))
                        (:fields entity-spec)

                        (map? entity-spec)
                        (->> entity-spec
                          ;; Sort the map entries by display-order first, then extract values
                          ;; This ensures we preserve the order we want
                          (sort-by (fn [[_ field-spec]]
                                     (or (get-in field-spec [:admin :display-order]) 999)))
                          (map second)
                          (filter map?)
                          (filter #(contains? % :id)))

                        (sequential? entity-spec) entity-spec
                        :else [])

        ordered-entity-fields (column-config/order-fields entity-fields column-order)
        locale (or (use-subscribe [:locale]) :bs)
        t (fn [translation-key]
            (i18n/translate locale translation-key))

        normalize-col (fn [col]
                        ;; Canonicalize to a simple app keyword (no namespace).
                        ;; Example: :admins/email -> :email, :created_at -> :created-at
                        (model-naming/ensure-app-keyword (kw/ensure-name col)))

        timestamp-field-ids (into #{}
                              (keep (comp normalize-col :id))
                              (filter map? ordered-entity-fields))

        ;; Filter out the raw timestamp fields to avoid duplication.
        ;; Timestamp columns are rendered via the timestamp header/cell helpers below,
        ;; and are controlled solely by Column Visibility.
        filtered-entity-fields (remove (fn [field]
                                         (contains? #{:created-at :updated-at}
                                           (normalize-col (:id field))))
                                 ordered-entity-fields)

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
        ;; IMPORTANT: if admin config returns an empty collection for an entity with no
        ;; vector-config, treat it as "no restriction" (default sortable). Only restrict
        ;; when the config provides a non-empty set of sortable columns.
        sortable-set (let [sc (use-subscribe [:admin/sortable-columns entity-name])]
                       ;; Normalize string/keyword column keys and only restrict sorting
                       ;; when a non-empty set is provided.
                       (when (seq sc)
                         (into #{} (keep normalize-col) sc)))

        filterable-set (cond
                         ;; Support boolean-map style configs (normalize keys)
                         (map? filterable-fields)
                         (into {}
                           (keep (fn [[k v]]
                                   (when-let [nk (normalize-col k)]
                                     [nk v])))
                           filterable-fields)

                         ;; Support vector/set style configs. Treat empty as "no restriction".
                         (coll? filterable-fields)
                         (let [xs (into [] (keep normalize-col) filterable-fields)]
                           (when (seq xs)
                             (set xs)))

                         :else nil)

        sort-state-by-field (into {}
                              (keep-indexed (fn [idx {:keys [field direction]}]
                                              (when-let [field-id (normalize-col field)]
                                                [field-id {:direction direction
                                                           :priority (inc idx)}])))
                              (or sorts []))

        base-headers (mapv (fn [field]
                             (let [field-id (normalize-col (:id field))
                                   sort-state (get sort-state-by-field field-id)
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
                                                  #(rf/dispatch [::ui-events/set-sort-field entity-name field-id {:append? (.-shiftKey %)}]))
                                      :sort-direction (:direction sort-state)
                                      :sort-priority (:priority sort-state)
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

        ;; Timestamp headers for created_at and updated_at - controlled solely by Column Visibility
        timestamp-headers (let [created-key :created-at
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
                                                      :else sentinel)))
                                column-visible? (fn [key]
                                                  (let [value (resolve-setting visible-columns key)]
                                                    (if (= value sentinel) true value)))
                                field-filterable? (fn [key]
                                                    (let [user-setting (resolve-setting (or user-filterable-settings {}) key)]
                                                      (if (not= user-setting sentinel)
                                                        ;; User has explicitly set filterable setting - use it
                                                        user-setting
                                                        ;; No user setting, check vector config for filterable columns
                                                        (cond
                                                          (map? filterable-set) (boolean (get filterable-set key true))
                                                          (set? filterable-set) (contains? filterable-set key)
                                                          :else true))))
                                filter-active? (fn [key]
                                                 (let [legacy (get legacy-map key)]
                                                   (or (contains? active-filters key)
                                                     (and legacy (contains? active-filters legacy)))))
                                inline-active? (fn [key]
                                                 (let [legacy (get legacy-map key)]
                                                   (boolean
                                                     (some #(= active-inline-filter %)
                                                       (remove nil? [key legacy])))))
                                created-sort-state (get sort-state-by-field created-key)
                                updated-sort-state (get sort-state-by-field updated-key)
                                has-created? (contains? timestamp-field-ids created-key)
                                has-updated? (contains? timestamp-field-ids updated-key)]
                            [(fn []
                               (when (and has-created? (column-visible? created-key))
                                 ($ table-header
                                   {:key "header-created-at"
                                    :header-id "header-created-at"
                                    :label (t :common/created)
                                    :sortable? true
                                    :on-click #(rf/dispatch [::ui-events/set-sort-field entity-name created-key {:append? (.-shiftKey %)}])
                                    :sort-direction (:direction created-sort-state)
                                    :sort-priority (:priority created-sort-state)
                                    :filter-on-click #(do
                                                        (.stopPropagation %)
                                                        (when on-inline-filter-click
                                                          (on-inline-filter-click created-key {:id "created-at" :label (t :common/created-at) :input-type "datetime"})))
                                    :filter-active? (filter-active? created-key)
                                    :active-inline-filter? (inline-active? created-key)
                                    :show-filtering? show-filtering?
                                    :is-field-filterable? (field-filterable? created-key)
                                    :field-id created-key})))
                             (fn []
                               (when (and has-updated? (column-visible? updated-key))
                                 ($ table-header
                                   {:key "header-updated-at"
                                    :header-id "header-updated-at"
                                    :label (t :common/updated)
                                    :sortable? true
                                    :on-click #(rf/dispatch [::ui-events/set-sort-field entity-name updated-key {:append? (.-shiftKey %)}])
                                    :sort-direction (:direction updated-sort-state)
                                    :sort-priority (:priority updated-sort-state)
                                    :filter-on-click #(do
                                                        (.stopPropagation %)
                                                        (when on-inline-filter-click
                                                          (on-inline-filter-click updated-key {:id "updated-at" :label (t :common/updated-at) :input-type "datetime"})))
                                    :filter-active? (filter-active? updated-key)
                                    :active-inline-filter? (inline-active? updated-key)
                                    :show-filtering? show-filtering?
                                    :is-field-filterable? (field-filterable? updated-key)
                                    :field-id updated-key})))])

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
                              :show-batch-edit? show-batch-edit?
                              :show-batch-delete? show-batch-delete?
                              :on-batch-delete on-batch-delete})))]]

    ;; Combine all headers in the correct order: select, base fields, timestamps, actions
    ;; Always include the select header - the reactive-select-all-header component
    ;; handles visibility internally by subscribing to show-select?
    (vec (concat
           select-header
           filtered-base-headers
           filtered-timestamp-headers
           action-header))))

(defn make-table-headers
  [{:keys [entity-spec entity-name show-filtering? show-batch-edit? show-batch-delete? on-batch-delete
           sorts sort-field sort-direction all-items selected-ids on-select-all active-filters
           filterable-fields user-filterable-settings visible-columns column-order
           active-inline-filter on-inline-filter-click]}]
  (make-table-headers- {:entity-spec entity-spec
                        :entity-name entity-name
                        :show-filtering? show-filtering?
                        :show-batch-edit? show-batch-edit?
                        :show-batch-delete? show-batch-delete?
                        :on-batch-delete on-batch-delete
                        :sorts (or sorts
                                 (cond-> []
                                   (and sort-field sort-direction)
                                   (conj {:field sort-field
                                          :direction sort-direction})))
                        :all-items all-items
                        :selected-ids selected-ids
                        :on-select-all on-select-all
                        :active-filters active-filters
                        :filterable-fields filterable-fields
                        :user-filterable-settings user-filterable-settings
                        :visible-columns visible-columns
                        :column-order column-order
                        :active-inline-filter active-inline-filter
                        :on-inline-filter-click on-inline-filter-click}))
