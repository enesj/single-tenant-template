(ns app.template.frontend.components.list
  (:require
    [app.domain.frontend.expenses.authz :as expenses-authz]
    [app.shared.keywords :as kw]
    [app.template.frontend.components.batch-edit :refer [batch-edit-inline]]
    [app.template.frontend.components.filter :refer [filter-form]]
    [app.template.frontend.components.filter.ui :refer [compact-active-filters]]
    [app.template.frontend.components.list.handlers :as list-handlers]
    [app.template.frontend.components.list.modals :as list-modals]
    [app.template.frontend.components.list.overrides :as overrides]
    [app.template.frontend.components.list.rows :refer [render-row]]
    [app.template.frontend.components.list.table :refer [make-table-headers]]
    [app.template.frontend.components.list.ui :refer [add-item-section
                                                      header-section]]
    [app.template.frontend.components.messages :refer [error-alert]]
    [app.template.frontend.components.pagination :refer [pagination]]
    [app.template.frontend.components.table :refer [table]]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.batch :as batch-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.events.list.settings :as settings-events]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.subs.form :as form-subs]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.template.frontend.utils.column-config :as column-config]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui use-effect use-state]]
    [uix.dom]
    [uix.re-frame :refer [use-subscribe]]))

(defn- apply-rows-override-transforms
  [{:keys [rows active-filters sort-config entity-name entity-spec]}]
  (overrides/apply-rows-override-transforms {:rows rows
                                             :active-filters active-filters
                                             :sort-config sort-config
                                             :entity-name entity-name
                                             :entity-spec entity-spec}))

(defn- selected-item?
  [selected-ids item]
  (overrides/selected-item? selected-ids item))

(defn- apply-selection-visibility
  [rows selected-ids {:keys [show-selected-rows? show-unselected-rows?]}]
  (overrides/apply-selection-visibility rows selected-ids {:show-selected-rows? show-selected-rows?
                                                           :show-unselected-rows? show-unselected-rows?}))

(defn- normalize-gate-id
  [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else nil))

(defn- gate-allows-action?
  [gate-id {:keys [expenses-role can-write? power-user?]}]
  (let [gate-id (normalize-gate-id gate-id)]
    (cond
      (nil? gate-id) true
      (= gate-id :expenses/can-write) (boolean can-write?)
      (= gate-id :expenses/power-user) (boolean power-user?)
      (expenses-authz/can? expenses-role gate-id) true
      :else false)))

(defui list-view
  "Renders a list of items with pagination, add form, and error handling.
   
   Supports both inline and modal form display modes:
   - :form-display :inline (default) - Forms replace the table when active
   - :form-display :modal - Forms show in modal overlay, table stays visible
   
   Custom form renderers:
   - :render-add-form - fn that receives props and returns add form UI
   - :render-edit-form - fn that receives (item props) and returns edit form UI
   
   Permissions:
   - :allow-add? - When false, hides add by default (see :disallowed-action-mode)
   - :allow-edit? - When false, hides edit by default (see :disallowed-action-mode)
   - :allow-delete? - When false, hides delete by default (see :disallowed-action-mode)
   - :disallowed-action-mode - :hide (default) or :disable (show disabled)

   Modal callbacks:
   - :on-add-success - Called after successful add (closes modal, can trigger refresh)
   - :on-edit-success - Called after successful edit (closes modal, can trigger refresh)"
  [{:keys [entity-name
           entity-spec
           title
           display-settings
           filterable-columns
           per-page
           rows-override
           pagination-override
           allow-add?
           disallowed-action-mode
           render-add-form
           render-edit-form
           form-display
           on-add-success
           on-edit-success]
    :as props}]
  (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
        items (use-subscribe [::entity-subs/paginated-entities entity-name])
        loading? (use-subscribe [::entity-subs/loading? entity-name])
        error (use-subscribe [::entity-subs/error entity-name])
        total-pages (use-subscribe [::list-subs/total-pages entity-name])
        current-page (use-subscribe [::entity-subs/current-page entity-name])
        selected-ids (use-subscribe [::list-subs/selected-ids entity-name])
        editing (use-subscribe [::ui-subs/editing])
        show-add-form? (use-subscribe [::ui-subs/show-add-form])
        form-success? (use-subscribe [::form-subs/form-success entity-kw])
        form-submitted? (use-subscribe [::form-subs/submitted? entity-kw])
        recently-updated-ids (use-subscribe [::ui-subs/recently-updated-entities entity-name])
        recently-created-ids (use-subscribe [::ui-subs/recently-created-entities entity-name])
        ;; Subscribe to hardcoded view-options for hiding settings panel controls
        ;; IMPORTANT: Only view-options.edn settings should hide controls, not entities.edn settings
        hardcoded-view-options (use-subscribe [::ui-subs/hardcoded-view-options entity-name])
        resolved-list-config (use-subscribe [::ui-subs/entity-list-config entity-name])
        expenses-role (use-subscribe [:expenses/user-role])
        can-write? (use-subscribe [:expenses/can-write?])
        power-user? (use-subscribe [:expenses/power-user?])
        gate-ctx {:expenses-role expenses-role
                  :can-write? can-write?
                  :power-user? power-user?}
        merged-display-settings (let [subscribed-settings (use-subscribe [::ui-subs/entity-display-settings entity-name])
                                      merged (merge (or display-settings {}) subscribed-settings)
                                      select-allowed? (gate-allows-action?
                                                        (get-in resolved-list-config [:action-gates :select])
                                                        gate-ctx)]
                                  (cond-> merged
                                    (false? select-allowed?) (assoc :show-select? false)))
        ;; Subscribe to user's filterable field settings from settings panel
        filterable-fields-subscription (use-subscribe [::ui-subs/filterable-fields entity-name])
        user-filterable-settings (use-subscribe [::settings-events/filterable-fields entity-name])
        filterable-fields (or filterable-columns filterable-fields-subscription)
        ;; Vector-config is only enabled once admin config is loaded.
        ;; We still use the unified visible-columns subscription underneath so policy defaults/locks apply.
        admin-config-loaded? (use-subscribe [:admin/config-loaded?])
        template-config-loaded? (use-subscribe [::ui-subs/template-config-loaded?])
        current-route (use-subscribe [:current-route])
        route-name (get-in current-route [:data :name])
        admin-route? (and route-name (boolean (re-find #"^admin" (name route-name))))
        vector-mode? (and admin-config-loaded?
                       (column-config/vector-config? entity-kw))
        visible-columns-raw (use-subscribe (column-config/visible-columns-source vector-mode? entity-kw))
        visible-columns (column-config/get-visible-columns vector-mode? entity-kw visible-columns-raw)
        sort-config (use-subscribe [::list-subs/sort-config entity-name])
        active-filters (use-subscribe [::list-subs/active-filters entity-name])
        batch-edit-inline-state (use-subscribe [::list-subs/batch-edit-inline entity-name])
        ui-state (use-subscribe [::list-subs/entity-ui-state entity-name])
        ;; Keep form-entity-specs for add/edit forms only. Table rendering
        ;; uses the provided entity-spec (vector-config) exclusively.
        form-entity-spec (use-subscribe [:form-entity-specs/by-name (keyword entity-name)])
        form-entity-spec-edit (use-subscribe [:form-entity-specs/by-name (keyword entity-name) true])
        configured-per-page-source (cond
                                     (some? per-page) :prop
                                     (some? (:per-page merged-display-settings)) :display-settings
                                     :else nil)
        configured-per-page (let [raw (or per-page (:per-page merged-display-settings))
                                  parsed (cond
                                           (number? raw) raw
                                           (string? raw) (js/parseInt raw 10)
                                           :else nil)]
                              (when (and (number? parsed) (pos? parsed)) parsed))
        local-display-prefs (or (use-subscribe [::ui-subs/entity-display-prefs entity-name]) {})
        local-per-page? (contains? local-display-prefs :per-page)
        per-page-config-ready? (if admin-route? admin-config-loaded? template-config-loaded?)
        configured-per-page-usable? (or (= configured-per-page-source :prop)
                                      local-per-page?
                                      per-page-config-ready?)
        existing-per-page (or (:per-page ui-state)
                            (get-in ui-state [:pagination :per-page]))
        effective-per-page (or existing-per-page
                             (when configured-per-page-usable? configured-per-page)
                             10)
        raw-items (vec (or (if (some? rows-override)
                             rows-override
                             items)
                         []))
        effective-items (if (some? rows-override)
                          (apply-rows-override-transforms
                            {:rows raw-items
                             :active-filters active-filters
                             :sort-config sort-config
                             :entity-name entity-kw
                             :entity-spec entity-spec})
                          raw-items)
        visible-items (apply-selection-visibility effective-items selected-ids merged-display-settings)
        selected-count (count selected-ids)
        visible-selected-count (count (filter #(selected-item? selected-ids %) visible-items))
        hidden-selected-count (max 0 (- selected-count visible-selected-count))
        pagination-current-page (or (:current-page pagination-override)
                                  current-page
                                  1)
        pagination-total-pages (max 1
                                 (or (:total-pages pagination-override)
                                   total-pages
                                   1))
        on-page-change (or (:on-page-change pagination-override)
                         #(rf/dispatch [::ui-events/set-current-page entity-name %]))
        on-per-page-change (or (:on-per-page-change pagination-override)
                             #(rf/dispatch [::ui-events/set-per-page entity-name %]))
        {:keys [show-highlights?]} merged-display-settings
        table-width (use-subscribe [::settings-events/table-width (some-> entity-name keyword)])

        ;; Column order preferences (drag-and-drop settings)
        column-order (use-subscribe [::settings-events/column-order entity-name])

        ;; State management for inline filter
        [active-inline-filter, set-active-inline-filter] (use-state nil)
        [inline-filter-field-spec, set-inline-filter-field-spec] (use-state nil)
        [inline-filter-value, set-inline-filter-value] (use-state "")

        ;; Modal state management for custom forms
        ;; These are component-local to avoid conflicts between entity pages
        [add-modal-open? set-add-modal-open!] (use-state false)
        [edit-modal-open? set-edit-modal-open!] (use-state false)
        [edit-modal-item set-edit-modal-item!] (use-state nil)

        effective-form-display (or (:form-display resolved-list-config)
                                 form-display
                                 :inline)
        effective-disallowed-action-mode (or (:disallowed-action-mode resolved-list-config)
                                           disallowed-action-mode
                                           :hide)
        effective-allow-add? (and (not (false? allow-add?))
                               (gate-allows-action? (get-in resolved-list-config [:action-gates :add]) gate-ctx))
        effective-allow-edit? (and (not (false? (:allow-edit? props)))
                                (gate-allows-action? (get-in resolved-list-config [:action-gates :edit]) gate-ctx))
        effective-allow-delete? (and (not (false? (:allow-delete? props)))
                                  (gate-allows-action? (get-in resolved-list-config [:action-gates :delete]) gate-ctx))

        ;; Determine form display mode
        use-modal-forms? (= effective-form-display :modal)
        has-custom-add-form? (some? render-add-form)
        has-custom-edit-form? (some? render-edit-form)]

    ;; Store the current entity type in the app state when it changes
    (use-effect
      (fn []
        ;; Set the current entity type when the component mounts or entity-name changes
        (rf/dispatch [::filter-events/set-current-entity-type entity-name])
        ;; Clear filter modal state when entity changes
        (rf/dispatch [::filter-events/clear-filter-modal])
        ;; NOTE: Removed clear-filter dispatch to preserve filters when switching entities
        ;; Clear inline filter state
        (set-active-inline-filter nil)
        (set-inline-filter-field-spec nil)
        (set-inline-filter-value "")
        ;; Clear modal state when entity changes
        (set-add-modal-open! false)
        (set-edit-modal-open! false)
        (set-edit-modal-item! nil)
        ;; Return cleanup function (optional)
        (fn [] nil))
      [entity-name])

    ;; Seed per-page once per entity when the list has no existing per-page.
    ;; Wait until the relevant config is loaded so we don't lock in fallback defaults (e.g., 25).
    (use-effect
      (fn []
        (let [missing-per-page? (nil? existing-per-page)]
          (when (and configured-per-page missing-per-page? configured-per-page-usable?)
            (rf/dispatch [::ui-events/set-per-page entity-name configured-per-page])))
        (fn [] nil))
      [entity-name
       configured-per-page
       configured-per-page-source
       configured-per-page-usable?
       existing-per-page
       admin-route?
       admin-config-loaded?
       template-config-loaded?
       local-per-page?])

    ;; Sync inline filter value with active filters when they change
    (use-effect
      (fn []
        (when active-inline-filter
          (let [field-key (if (keyword? active-inline-filter) active-inline-filter (keyword active-inline-filter))
                new-filter-value (get active-filters field-key)
                desired (or new-filter-value "")]
            ;; Only update local state when it actually changes to prevent loops
            (when (not= inline-filter-value desired)
              (set-inline-filter-value desired)))))
      [active-filters active-inline-filter inline-filter-value])

    ;; Auto-close default modal edit form after success.
    ;; (Custom modal edit forms are expected to call the provided :on-success callback.)
    (use-effect
      (fn []
        (when (and use-modal-forms?
                edit-modal-open?
                (not has-custom-edit-form?)
                form-success?
                form-submitted?)
          ;; Give the success alert a brief moment to show, then close.
          (js/setTimeout
            (fn []
              (rf/dispatch [::form-events/cancel-form entity-kw])
              (set-edit-modal-open! false)
              (set-edit-modal-item! nil)
              (when on-edit-success
                (on-edit-success)))
            500))
        js/undefined)
      [on-edit-success use-modal-forms? edit-modal-open? has-custom-edit-form? form-success? form-submitted? entity-kw])

    ;; Auto-close default modal add form after success.
    ;; (Custom modal add forms are expected to call the provided :on-success callback.)
    (use-effect
      (fn []
        (when (and use-modal-forms?
                add-modal-open?
                (not has-custom-add-form?)
                form-success?
                form-submitted?)
          ;; Give the success alert a brief moment to show, then close.
          (js/setTimeout
            (fn []
              (rf/dispatch [::form-events/cancel-form entity-kw])
              (set-add-modal-open! false)
              (when on-add-success
                (on-add-success)))
            500))
        js/undefined)
      [on-add-success use-modal-forms? add-modal-open? has-custom-add-form? form-success? form-submitted? entity-kw])

    ;; Handle events/actions via extracted handler module.
    (let [{:keys [handle-select-change
                  handle-select-all
                  handle-inline-filter-click
                  handle-filter-apply
                  handle-filter-close
                  handle-add-click
                  handle-add-modal-close
                  handle-add-modal-success
                  handle-edit-click
                  handle-edit-modal-close
                  handle-edit-modal-success]}
          (list-handlers/build-handlers
            {:entity-name entity-name
             :entity-kw entity-kw
             :allow-add? effective-allow-add?
             :use-modal-forms? use-modal-forms?
             :has-custom-add-form? has-custom-add-form?
             :has-custom-edit-form? has-custom-edit-form?
             :on-add-success on-add-success
             :on-edit-success on-edit-success
             :active-inline-filter active-inline-filter
             :active-filters active-filters
             :visible-items visible-items
             :set-active-inline-filter set-active-inline-filter
             :set-inline-filter-field-spec set-inline-filter-field-spec
             :set-inline-filter-value set-inline-filter-value
             :set-add-modal-open! set-add-modal-open!
             :set-edit-modal-open! set-edit-modal-open!
             :set-edit-modal-item! set-edit-modal-item!})

          base-props (merge (assoc props
                              :editing editing
                              :set-editing! #(rf/dispatch [::config-events/set-editing %])
                              :recently-updated-ids recently-updated-ids
                              :recently-created-ids recently-created-ids
                              :sort-field (:field sort-config)
                              :sort-direction (:direction sort-config)
                              :selected-ids selected-ids
                              :on-select-change handle-select-change
                              :show-add-form? show-add-form?
                              :set-show-add-form! #(rf/dispatch [::config-events/set-show-add-form %])
                              :visible-columns visible-columns
                              :column-order column-order
                              :allow-add? effective-allow-add?
                              :allow-edit? effective-allow-edit?
                              :allow-delete? effective-allow-delete?
                              :disallowed-action-mode effective-disallowed-action-mode
                              :form-display effective-form-display
                              ;; Pass form-entity-specs as props to avoid hooks in loops
                              :form-entity-spec form-entity-spec
                              :form-entity-spec-edit form-entity-spec-edit
                              ;; Allow callers (e.g., admin pages) to fully override row actions.
                              ;; When provided, rows will render only this component for actions,
                              ;; and will not show the template's default action-buttons.
                              :actions-override (:render-actions props)
                              ;; Always pass edit handler for action buttons to trigger inline editing
                              :on-edit-click handle-edit-click
                              ;; Pass custom edit form renderer for inline mode
                              :render-edit-form (when (and has-custom-edit-form? (not use-modal-forms?))
                                                  render-edit-form)
                              :on-edit-success on-edit-success)
                       merged-display-settings)

          ;; Debug removed
          _ nil

          render-row-fn (fn [item _]
                          (render-row base-props {:item item}))

          table-headers (make-table-headers (assoc base-props
                                              :all-items visible-items
                                              :on-select-all handle-select-all
                                              :active-filters active-filters

                                              :show-filtering? (:show-filtering? merged-display-settings)
                                              :show-batch-edit? (:show-batch-edit? merged-display-settings)
                                              :show-batch-delete? (:show-batch-delete? merged-display-settings)
                                              :filterable-fields filterable-fields
                                              :user-filterable-settings user-filterable-settings
                                              :visible-columns visible-columns
                                              :column-order column-order
                                              :active-inline-filter active-inline-filter
                                              :on-inline-filter-click handle-inline-filter-click
                                              ;; Table should render strictly from the supplied entity-spec
                                              ;; (vector-config). No fallback to form specs.
                                              :entity-spec entity-spec))

          ;; Determine if we should show inline add form (non-modal mode only)
          show-inline-add-form? (and show-add-form? (not use-modal-forms?))]

      ;; Return the component UI
      ($ :div {:class "w-full flex justify-start items-start"
               :id (str "table-" (kw/ensure-name entity-name))}
        ($ :div {:class "p-2 w-full"}
          ;; Modal rendering moved to extracted module.
          (list-modals/render-add-modal
            {:add-modal-open? add-modal-open?
             :title title
             :entity-name entity-name
             :entity-kw entity-kw
             :entity-spec entity-spec
             :form-entity-spec form-entity-spec
             :has-custom-add-form? has-custom-add-form?
             :render-add-form render-add-form
             :handle-add-modal-close handle-add-modal-close
             :handle-add-modal-success handle-add-modal-success})

          (list-modals/render-edit-modal
            {:edit-modal-open? edit-modal-open?
             :edit-modal-item edit-modal-item
             :title title
             :entity-name entity-name
             :entity-kw entity-kw
             :entity-spec entity-spec
             :form-entity-spec form-entity-spec
             :form-entity-spec-edit form-entity-spec-edit
             :has-custom-edit-form? has-custom-edit-form?
             :render-edit-form render-edit-form
             :handle-edit-modal-close handle-edit-modal-close
             :handle-edit-modal-success handle-edit-modal-success})

          ;; Remove the old modal filter form rendering
          nil

          ;; Batch edit popup
          nil

          ;; Inline batch edit form - shown at the top of the list when active

          ($ :div {:class "ds-divider"})                    ;; Divider A (after header)

          ;; Always-visible compact active filters (when not showing inline filter form)
          (when (and (seq active-filters) (not active-inline-filter))
            ($ compact-active-filters
              {:entity-type entity-name
               :active-filters active-filters
               :on-clear-filter (fn [field-id]
                                  (rf/dispatch [::filter-events/clear-filter entity-name field-id]))}))

          ;; Render inline filter form when active
          (when (and active-inline-filter inline-filter-field-spec)
            ($ filter-form
              {:entity-type entity-name
               :field-spec inline-filter-field-spec
               :initial-value inline-filter-value
               :on-close handle-filter-close
               :on-apply handle-filter-apply
               :on-field-switch (fn [new-field]
                                  ;; Clear current filter first
                                  (rf/dispatch [::filter-events/clear-filter entity-name])
                                  ;; Switch to the new field
                                  (set-active-inline-filter (keyword (:id new-field)))
                                  (set-inline-filter-field-spec new-field)
                                  (set-inline-filter-value ""))}))

          (when (and (:open? batch-edit-inline-state)
                  (seq selected-ids)
                  entity-name)
            ($ batch-edit-inline
              {:entity-name (keyword entity-name)
               :selected-ids selected-ids
               :on-close #(rf/dispatch [::batch-events/hide-batch-edit-inline entity-name])}))

          (when error
            ($ error-alert
              {:error error
               :entity-name entity-name}))
          (cond
            loading?                                        ;; Display loading message if loading
            ($ :div ($ :span "Loading..."))
            :else                                           ;; Otherwise, display the list
            ($ :div
              (if show-inline-add-form?
                ;; If show-add-form? is true (inline mode), display the add item section
                ($ add-item-section base-props)
                ;; Otherwise, display the table with pagination in same container
                (let [items-vec visible-items
                      disallowed-mode effective-disallowed-action-mode
                      disable-mode? (= disallowed-mode :disable)
                      policy-show-add-button? (not (false? (:show-add-button? merged-display-settings)))
                      allowed-add? effective-allow-add?
                      show-add-button? (and policy-show-add-button? (or allowed-add? disable-mode?))
                      add-disabled? (and policy-show-add-button? disable-mode? (not allowed-add?))]
                  ($ :div {:class "w-full" :style {:max-width (str table-width "px")}}  ;; Wrapper to contain header, table and pagination together with table width constraint
                      ;; Header section moved inside table wrapper for proper alignment
                    ($ header-section
                      {:title title
                       :show-add-form? show-add-form?
                       :set-show-add-form! #(rf/dispatch [::config-events/set-show-add-form %])
                       :set-editing! #(rf/dispatch [::config-events/set-editing %])
                       :entity-name entity-name
                       :show-add-button? show-add-button?
                       :add-disabled? add-disabled?
                       ;; Allow callers to provide a direct add click handler (e.g., navigate to upload),
                       ;; otherwise fall back to the modal-mode handler when using custom add forms.
                       :on-add-click (or (:on-add-click props)
                                       (when use-modal-forms?
                                         handle-add-click))})

                    ($ :div {:id (str "selected-count-" (kw/ensure-name entity-name))
                             :class "flex items-center gap-2 mb-2 text-sm text-base-content/70"}
                      ($ :span {:class "font-semibold"}
                        (str selected-count " selected"))
                      (when (pos? hidden-selected-count)
                        ($ :span
                          (str "(" hidden-selected-count " hidden by current list view)"))))

                    ($ :div {:class "ds-divider"})                    ;; Divider after header
                    ($ table
                      {:headers table-headers
                       :rows items-vec
                       :row-key (fn [item]
                                  (id-utils/extract-entity-id item))
                       :entity-name entity-name
                       :entity-spec entity-spec
                       :show-highlights? show-highlights?
                       :render-row render-row-fn
                       :render-row-expansion (:render-row-expansion props)
                         ;; IMPORTANT: Pass the merged display settings for behavior
                       :display-settings merged-display-settings
                         ;; IMPORTANT: Pass hardcoded settings (page props + view-options) for settings panel control visibility
                       :page-display-settings hardcoded-view-options
                         ;; Pass rows per page props to table for settings panel
                       :per-page effective-per-page
                       :on-per-page-change on-per-page-change
                       :rows-per-page-options [5 10 20 25 50 100]})
                      ;; Display pagination controls within same container as table
                      ;; Check both pagination display setting and whether there are multiple pages
                    (when (and (get merged-display-settings :show-pagination? true)
                            (> pagination-total-pages 1))
                      ($ pagination
                        {:current-page pagination-current-page
                         :total-pages pagination-total-pages
                         :on-page-change on-page-change
                         ;; Pass rows per page data and options
                         :per-page effective-per-page
                         :on-per-page-change on-per-page-change
                         :rows-per-page-options [5 10 20 25 50 100]
                         :entity-name entity-name}))))))))))))
