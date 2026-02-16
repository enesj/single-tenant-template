(ns app.template.frontend.components.list
  (:require
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.batch-edit :refer [batch-edit-inline]]
    [app.template.frontend.components.filter :refer [filter-form]]
    [app.template.frontend.components.filter.helpers :as filter-helpers]
    [app.template.frontend.components.filter.ui :refer [compact-active-filters]]
    [app.template.frontend.components.form :refer [form]]
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
    [app.template.frontend.subs.form :as form-subs]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.template.frontend.utils.column-config :as column-config]
    [app.template.frontend.utils.id :as id-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :as uix :refer [$ defui use-effect use-state]]
    [uix.dom]
    [uix.re-frame :refer [use-subscribe]]))

(defn- entity-spec-fields
  [entity-spec]
  (cond
    (and (map? entity-spec) (sequential? (:fields entity-spec)))
    (:fields entity-spec)

    (sequential? entity-spec)
    entity-spec

    :else
    []))

(defn- resolve-row-field
  [item entity-name fld]
  (let [direct (get item fld)]
    (if (some? direct)
      direct
      (let [by-db (when (keyword? fld)
                    (get item (model-naming/app-keyword->db fld)))]
        (if (some? by-db)
          by-db
          (let [ns-key (when (and entity-name fld)
                         (keyword (name entity-name) (name fld)))
                by-ns (when ns-key (get item ns-key))]
            (if (some? by-ns)
              by-ns
              (some (fn [[k v]]
                      (when (and (keyword? k)
                              (= (name k) (name fld)))
                        v))
                item))))))))

(defn- infer-filter-type
  [filter-value]
  (cond
    (vector? filter-value)
    :select

    (and (map? filter-value)
      (or (contains? filter-value :min)
        (contains? filter-value :max)))
    :number-range

    (and (map? filter-value)
      (or (contains? filter-value :from)
        (contains? filter-value :to)))
    :date-range

    :else
    :text))

(defn- apply-override-filters
  [rows active-filters]
  (if (empty? active-filters)
    rows
    (vec
      (filter (fn [item]
                (every? (fn [[field-id filter-value]]
                          (filter-helpers/matches-filter? {:item item
                                                           :field-id field-id
                                                           :filter-value filter-value
                                                           :filter-type (infer-filter-type filter-value)}))
                  active-filters))
        rows))))

(defn- sort-override-rows
  [rows {:keys [sort-config entity-name entity-spec]}]
  (let [{:keys [field direction]} sort-config
        field (some-> field model-naming/ensure-app-keyword)
        field-spec (some (fn [spec]
                           (when (= (some-> (:id spec) model-naming/ensure-app-keyword name)
                                   (some-> field name))
                             spec))
                     (entity-spec-fields entity-spec))
        date-field? (contains? #{"datetime-local" "date" "time"}
                      (some-> field-spec :input-type kw/ensure-name))
        normalize (fn [v]
                    (cond
                      (nil? v) nil
                      (string? v)
                      (if date-field?
                        (let [d (try (js/Date. v) (catch :default _ nil))]
                          (if (and d (not (js/isNaN (.getTime d))))
                            (.getTime d)
                            (str/lower-case v)))
                        (str/lower-case v))
                      (boolean? v) (if v 1 0)
                      (instance? js/Date v) (.getTime v)
                      :else v))]
    (if (and field direction)
      (let [sorted (sort-by (fn [item]
                              (let [v (normalize (resolve-row-field item entity-name field))
                                    nil-key (if (some? v) 1 0)]
                                [nil-key v]))
                     rows)]
        (if (= direction :desc)
          (reverse sorted)
          sorted))
      rows)))

(defn- apply-rows-override-transforms
  [{:keys [rows active-filters sort-config entity-name entity-spec]}]
  (-> rows
    (apply-override-filters active-filters)
    (sort-override-rows {:sort-config sort-config
                         :entity-name entity-name
                         :entity-spec entity-spec})
    vec))

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
        ;; Use page-level display settings as defaults, but let user state override them
        merged-display-settings (let [subscribed-settings (use-subscribe [::ui-subs/entity-display-settings entity-name])
                                      merged (merge display-settings subscribed-settings)]
                                  merged)
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

    ;; Handle single item selection toggle
    (let [handle-select-change (fn [item-id selected?]
                                 (rf/dispatch [::selection-events/select-item entity-name item-id selected?]))

          ;; Handle select all / deselect all
          handle-select-all (fn [select-all?]
                              (rf/dispatch [::selection-events/select-all entity-name effective-items select-all?]))

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
                               ;; Ensure we don't reuse stale form state between opens.
                               (rf/dispatch [::form-events/cancel-form entity-kw])
                               (if use-modal-forms?
                                 ;; Modal behavior (default or custom): keep table visible.
                                 (set-add-modal-open! true)
                                 ;; Inline behavior
                                 (do
                                   (rf/dispatch [::config-events/set-show-add-form true])
                                   (rf/dispatch [::config-events/set-editing nil])))))

          handle-add-modal-close (fn []
                                   (rf/dispatch [::crud-events/clear-error entity-kw])
                                   (rf/dispatch [::form-events/clear-form-errors entity-kw])
                                   (rf/dispatch [::form-events/cancel-form entity-kw])
                                   (set-add-modal-open! false))

          handle-add-modal-success (fn []
                                     (set-add-modal-open! false)
                                     (when on-add-success
                                       (on-add-success)))

          handle-edit-click (fn [item]
                              (rf/dispatch [::crud-events/clear-error entity-kw])
                              (rf/dispatch [::form-events/clear-form-errors entity-kw])
                              (if use-modal-forms?
                                ;; Modal behavior (default or custom): keep table visible.
                                (do
                                  ;; Ensure the modal form starts from item values (not stale form state).
                                  (rf/dispatch [::form-events/cancel-form entity-kw])
                                  (set-edit-modal-item! item)
                                  (set-edit-modal-open! true))
                                ;; Inline behavior
                                (rf/dispatch [::config-events/set-editing (id-utils/extract-entity-id item)])))

          handle-edit-modal-close (fn []
                                    (rf/dispatch [::form-events/cancel-form entity-kw])
                                    (set-edit-modal-open! false)
                                    (set-edit-modal-item! nil))

          handle-edit-modal-success (fn []
                                      (rf/dispatch [::form-events/cancel-form entity-kw])
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
                              :column-order column-order
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
                                              :all-items effective-items
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
          ;; Modal for custom add form
          (when add-modal-open?
            ($ modal-wrapper
              {:visible? true
               :title (str "Add " title)
               :size :large
               :on-close handle-add-modal-close
               :close-button-id (str "btn-close-add-modal-" (kw/ensure-name entity-name))}
              (if has-custom-add-form?
                (render-add-form {:entity-name entity-name
                                  :entity-spec entity-spec
                                  :on-success handle-add-modal-success
                                  :on-cancel handle-add-modal-close})
                (let [effective-form-spec (or form-entity-spec
                                            (when (vector? entity-spec) entity-spec)
                                            (when (map? entity-spec)
                                              (let [fields (:fields entity-spec)]
                                                (cond
                                                  (vector? fields) fields
                                                  (sequential? fields) (vec fields)
                                                  :else nil)))
                                            [])
                      default-values (reduce (fn [acc field-spec]
                                               (if-let [default-value (:default-value field-spec)]
                                                 (assoc acc (keyword (:id field-spec)) default-value)
                                                 acc))
                                       {}
                                       effective-form-spec)]
                  ($ form
                    {:key (str "modal-add-" (kw/ensure-name entity-name))
                     :entity-name entity-kw
                     :entity-spec effective-form-spec
                     :editing false
                     :initial-values default-values
                     :on-cancel handle-add-modal-close})))))

          ;; Modal for edit form (custom or default)
          (when (and edit-modal-open? edit-modal-item)
            (let [item-clj (if (map? edit-modal-item)
                             edit-modal-item
                             (js->clj edit-modal-item :keywordize-keys true))
                  item-id (id-utils/extract-entity-id item-clj)
                  initial-values (into {}
                                   (map (fn [[k v]]
                                          ;; Convert namespaced keys to simple keys for form fields
                                          (let [simple-key (if (and (keyword? k) (namespace k))
                                                             (keyword (name k))
                                                             k)]
                                            [simple-key v]))
                                     item-clj))
                  effective-form-spec (or form-entity-spec-edit
                                        form-entity-spec
                                        entity-spec)
                  handle-default-submit (fn [{:keys [dirty values] :as payload}]
                                          (let [changed-values (-> values
                                                                 (select-keys (cons :id (keys dirty))))]
                                            (rf/dispatch [::form-events/submit-form
                                                          (assoc payload
                                                            :values changed-values
                                                            :entity-name entity-kw
                                                            :editing true)])
                                            (rf/dispatch [::form-events/set-submitted entity-kw true])))]
              ($ modal-wrapper
                {:visible? true
                 :title (str "Edit " title)
                 :size :large
                 :draggable? true
                 :on-close handle-edit-modal-close
                 :close-button-id (str "btn-close-edit-modal-" (kw/ensure-name entity-name))}
                (if has-custom-edit-form?
                  (render-edit-form item-clj
                    {:entity-name entity-name
                     :entity-spec entity-spec
                     :on-success handle-edit-modal-success
                     :on-cancel handle-edit-modal-close})
                  ($ form
                    {:key (str "modal-edit-" (kw/ensure-name entity-name) "-" (or item-id "unknown"))
                     :entity-name entity-kw
                     :entity-spec effective-form-spec
                     :editing true
                     :initial-values initial-values
                     :on-cancel handle-edit-modal-close
                     :on-submit handle-default-submit})))))

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
                (let [items-vec effective-items
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
                                       (when use-modal-forms?
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
