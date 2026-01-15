(ns app.template.frontend.components.list
  (:require
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.keywords :as kw]
    [app.template.frontend.components.batch-edit :refer [batch-edit-inline]]
    [app.template.frontend.components.filter :refer [filter-form]]
    [app.template.frontend.components.filter.ui :refer [compact-active-filters]]
    [app.template.frontend.components.list.rows :refer [render-row]]
    [app.template.frontend.components.list.table :refer [make-table-headers]]
    [app.template.frontend.components.list.ui :refer [add-item-section
                                                      header-section]]
    [app.template.frontend.components.messages :refer [error-alert]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.pagination :refer [pagination]]
    [app.template.frontend.components.table :refer [table]]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.batch :as batch-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.events.list.settings :as settings-events]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.template.frontend.utils.column-config :as column-config]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui use-effect use-state]]
    [uix.dom]
    [uix.re-frame :refer [use-subscribe]]))

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
  [{:keys [entity-name entity-spec title display-settings filterable-columns per-page
           allow-add? allow-edit? allow-delete? disallowed-action-mode
           ;; New props for custom forms and modal support
           render-add-form render-edit-form form-display on-add-success on-edit-success]
    :as props}]
  (let [items (use-subscribe [::entity-subs/paginated-entities entity-name])
        loading? (use-subscribe [::entity-subs/loading? entity-name])
        error (use-subscribe [::entity-subs/error entity-name])
        total-pages (use-subscribe [::list-subs/total-pages entity-name])
        current-page (use-subscribe [::entity-subs/current-page entity-name])
        selected-ids (use-subscribe [::list-subs/selected-ids entity-name])
        editing (use-subscribe [::ui-subs/editing])
        show-add-form? (use-subscribe [::ui-subs/show-add-form])
        recently-updated-ids (use-subscribe [::ui-subs/recently-updated-entities entity-name])
        recently-created-ids (use-subscribe [::ui-subs/recently-created-entities entity-name])
        ;; Subscribe to hardcoded view-options for hiding settings panel controls
        ;; IMPORTANT: Only view-options.edn settings should hide controls, not entities.edn settings
        hardcoded-view-options (use-subscribe [::ui-subs/hardcoded-view-options entity-name])
        ;; Use page-level display settings as defaults, but let user state override them
        merged-display-settings (let [subscribed-settings (use-subscribe [::ui-subs/entity-display-settings entity-name])
                                      merged (merge display-settings subscribed-settings)]
                                  merged)
        ;; Subscribe to user's filterable field settings from settings panel
        filterable-fields-subscription (use-subscribe [::ui-subs/filterable-fields entity-name])
        user-filterable-settings (use-subscribe [::settings-events/filterable-fields entity-name])
        filterable-fields (or filterable-columns filterable-fields-subscription)
        entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
        ;; Vector-config is only enabled once admin config is loaded.
        ;; We still use the unified visible-columns subscription underneath so policy defaults/locks apply.
        admin-config-loaded? (use-subscribe [:admin/config-loaded?])
        ;; Domain/template config loaded? (for user routes like /expenses/*)
        template-config-loaded? (use-subscribe [::ui-subs/template-config-loaded?])
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
        existing-per-page (or (:per-page ui-state)
                            (get-in ui-state [:pagination :per-page]))
        effective-per-page (or existing-per-page
                             configured-per-page
                             10)
        {:keys [show-highlights?]} merged-display-settings
        ;; Subscribe to table width configuration for header alignment
        table-width (use-subscribe [::settings-events/table-width (some-> entity-name keyword)])

        ;; State management for inline filter
        [active-inline-filter, set-active-inline-filter] (use-state nil)
        [inline-filter-field-spec, set-inline-filter-field-spec] (use-state nil)
        [inline-filter-value, set-inline-filter-value] (use-state "")

        ;; Modal state management for custom forms
        ;; These are component-local to avoid conflicts between entity pages
        [add-modal-open? set-add-modal-open!] (use-state false)
        [edit-modal-open? set-edit-modal-open!] (use-state false)
        [edit-modal-item set-edit-modal-item!] (use-state nil)

        ;; Determine form display mode
        use-modal-forms? (= form-display :modal)
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
          (when (and configured-per-page missing-per-page?
                  ;; If :per-page comes in explicitly as a prop (admin pages), seed immediately.
                  ;; Otherwise wait until the relevant config is loaded so we don't lock in fallback defaults.
                  (or (= configured-per-page-source :prop)
                    ;; On admin routes, admin-config-loaded? gates; on user routes, template-config-loaded? gates.
                    admin-config-loaded? template-config-loaded?))
            (rf/dispatch [::ui-events/set-per-page entity-name configured-per-page])))
        (fn [] nil))
      [entity-name configured-per-page configured-per-page-source existing-per-page admin-config-loaded? template-config-loaded?])

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

    ;; Handle single item selection toggle
    (let [handle-select-change (fn [item-id selected?]
                                 (rf/dispatch [::selection-events/select-item entity-name item-id selected?]))

          ;; Handle select all / deselect all
          handle-select-all (fn [select-all?]
                              (rf/dispatch [::selection-events/select-all entity-name items select-all?]))

          ;; Handle inline filter click
          handle-inline-filter-click (fn [field-id field-spec]
                                       (if (= active-inline-filter field-id)
                                         ;; If clicking the same filter, close it
                                         (do
                                           (set-active-inline-filter nil)
                                           (set-inline-filter-field-spec nil)
                                           (set-inline-filter-value ""))
                                         ;; Otherwise, open the new filter with existing value
                                         (let [field-key (if (keyword? field-id) field-id (keyword field-id))
                                               existing-filter-value (get active-filters field-key)]
                                           (set-active-inline-filter field-id)
                                           (set-inline-filter-field-spec field-spec)
                                           (set-inline-filter-value (or existing-filter-value "")))))

          ;; Handle filter apply
          handle-filter-apply (fn [entity-type field-name filter-value keep-open?]
                                (rf/dispatch [::filter-events/apply-filter entity-type field-name filter-value keep-open?])
                                (when (not keep-open?)
                                  (set-active-inline-filter nil)
                                  (set-inline-filter-field-spec nil)
                                  (set-inline-filter-value "")))

          ;; Handle filter close
          handle-filter-close (fn []
                                (set-active-inline-filter nil)
                                (set-inline-filter-field-spec nil)
                                (set-inline-filter-value ""))

          ;; Modal handlers for custom forms
          handle-add-click (fn []
                             (when (not (false? allow-add?))
                               (rf/dispatch [::crud-events/clear-error entity-kw])
                               (rf/dispatch [::form-events/clear-form-errors entity-kw])
                               (if (and use-modal-forms? has-custom-add-form?)
                                 ;; Open add modal
                                 (set-add-modal-open! true)
                                 ;; Fall back to inline behavior
                                 (do
                                   (rf/dispatch [::config-events/set-show-add-form true])
                                   (rf/dispatch [::config-events/set-editing nil])))))

          handle-add-modal-close (fn []
                                   (set-add-modal-open! false))

          handle-add-modal-success (fn []
                                     (set-add-modal-open! false)
                                     (when on-add-success
                                       (on-add-success)))

          handle-edit-click (fn [item]
                              (rf/dispatch [::crud-events/clear-error entity-kw])
                              (rf/dispatch [::form-events/clear-form-errors entity-kw])
                              (if (and use-modal-forms? has-custom-edit-form?)
                                ;; Open edit modal with item
                                (do
                                  (set-edit-modal-item! item)
                                  (set-edit-modal-open! true))
                                ;; Fall back to inline behavior
                                (rf/dispatch [::config-events/set-editing (id-utils/extract-entity-id item)])))

          handle-edit-modal-close (fn []
                                    (set-edit-modal-open! false)
                                    (set-edit-modal-item! nil))

          handle-edit-modal-success (fn []
                                      (set-edit-modal-open! false)
                                      (set-edit-modal-item! nil)
                                      (when on-edit-success
                                        (on-edit-success)))

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
                              ;; Pass form-entity-specs as props to avoid hooks in loops
                              :form-entity-spec form-entity-spec
                              :form-entity-spec-edit form-entity-spec-edit
                              ;; Allow callers (e.g., admin pages) to fully override row actions.
                              ;; When provided, rows will render only this component for actions,
                              ;; and will not show the template's default action-buttons.
                              :actions-override (:render-actions props)
                              ;; Pass modal edit handler for action buttons to use
                              :on-edit-click (when (and use-modal-forms? has-custom-edit-form?)
                                               handle-edit-click))
                       merged-display-settings)

          ;; Debug removed
          _ nil

          render-row-fn (fn [item _]
                          (render-row base-props {:item item}))

          table-headers (make-table-headers (assoc base-props
                                              :all-items items
                                              :on-select-all handle-select-all
                                              :active-filters active-filters
                                              :show-timestamps? (:show-timestamps? merged-display-settings)
                                              :show-filtering? (:show-filtering? merged-display-settings)
                                              :show-batch-edit? (:show-batch-edit? merged-display-settings)
                                              :show-batch-delete? (:show-batch-delete? merged-display-settings)
                                              :filterable-fields filterable-fields
                                              :user-filterable-settings user-filterable-settings
                                              :visible-columns visible-columns
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
          ;; Modal for custom add form
          (when (and add-modal-open? has-custom-add-form?)
            ($ modal-wrapper
              {:visible? true
               :title (str "Add " title)
               :size :large
               :on-close handle-add-modal-close
               :close-button-id (str "btn-close-add-modal-" (kw/ensure-name entity-name))}
              (render-add-form {:entity-name entity-name
                                :entity-spec entity-spec
                                :on-success handle-add-modal-success
                                :on-cancel handle-add-modal-close})))

          ;; Modal for custom edit form
          (when (and edit-modal-open? has-custom-edit-form? edit-modal-item)
            ($ modal-wrapper
              {:visible? true
               :title (str "Edit " title)
               :size :large
               :on-close handle-edit-modal-close
               :close-button-id (str "btn-close-edit-modal-" (kw/ensure-name entity-name))}
              (render-edit-form edit-modal-item
                {:entity-name entity-name
                 :entity-spec entity-spec
                 :on-success handle-edit-modal-success
                 :on-cancel handle-edit-modal-close})))

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
                (let [items-vec (vec (or items []))
                      disallowed-mode (or disallowed-action-mode :hide)
                      disable-mode? (= disallowed-mode :disable)
                      policy-show-add-button? (not (false? (:show-add-button? merged-display-settings)))
                      allowed-add? (not (false? allow-add?))
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
                                       (when (and use-modal-forms? has-custom-add-form?)
                                         handle-add-click))})

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
                         ;; IMPORTANT: Pass the merged display settings for behavior
                       :display-settings merged-display-settings
                         ;; IMPORTANT: Pass hardcoded settings (page props + view-options) for settings panel control visibility
                       :page-display-settings hardcoded-view-options
                         ;; Pass rows per page props to table for settings panel
                       :per-page effective-per-page
                       :on-per-page-change #(rf/dispatch [::ui-events/set-per-page entity-name %])
                       :rows-per-page-options [5 10 20 25 50 100]})
                      ;; Display pagination controls within same container as table
                      ;; Check both pagination display setting and whether there are multiple pages
                    (when (and (get merged-display-settings :show-pagination? true)
                            (> total-pages 1))
                      ($ pagination
                        {:current-page current-page
                         :total-pages total-pages
                         :on-page-change #(rf/dispatch [::ui-events/set-current-page entity-name %])
                           ;; Pass rows per page data and options
                         :per-page effective-per-page
                         :on-per-page-change #(rf/dispatch [::ui-events/set-per-page entity-name %])
                         :rows-per-page-options [5 10 20 25 50 100]
                         :entity-name entity-name}))))))))))))
