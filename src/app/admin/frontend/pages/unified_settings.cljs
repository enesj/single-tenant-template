(ns app.admin.frontend.pages.unified-settings
  "Unified admin settings page with scope switching.

   This page provides a single UI for both admin and user settings,
   with the ability to switch between scopes in edit mode.

   View mode: Shows overview of both admin and user settings
   Edit mode: Shows one scope at a time with scope switcher"
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.settings-shell :as shell]
    [app.admin.frontend.components.settings-views :as views]
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.events.settings :as admin-settings-events]
    [app.admin.frontend.events.unified-settings :as unified-events]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.admin.frontend.settings.definitions :as defs]
    [app.template.frontend.settings.resolver :as resolver]
    [clojure.set :as set]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; View Mode Content - Overview of Both Scopes
;; =============================================================================

(defui admin-entity-card-for-overview
  "Entity card for admin overview - shows current settings (read-only)."
  [{:keys [entity-kw settings]}]
  ($ views/admin-entity-settings-card
    {:entity-name entity-kw
     :settings settings
     :editing? false
     :setting-keys defs/all-setting-keys}))

(defui user-entity-card-for-overview
  "Entity card for user overview - shows current settings (read-only)."
  [{:keys [entity-kw view-options entity-config]}]
  (let [;; Feature constraints are always enforced and cannot be overridden.
        immutable-locks (resolver/feature-constraints->locks (:features entity-config))
        draft-defaults (or (get-in view-options [:display-defaults]) {})
        draft-locks (or (get-in view-options [:display-locks]) {})]
    ($ views/user-entity-settings-card
      {:entity-kw entity-kw
       :draft-defaults draft-defaults
       :draft-locks draft-locks
       :immutable-locks immutable-locks
       :setting-keys defs/all-setting-keys})))

(defui scope-section-overview
  "Overview section for a single scope."
  [{:keys [title icon scope-config domain-groups render-entity-card]}]
  (let [entities (sort (keys scope-config))
        grouped (defs/group-entities-by-domain entities)]
    ($ :div {:class "mb-8"}
      ;; Section header
      ($ :div {:class "flex items-center gap-2 mb-4 pb-2 border-b border-base-300"}
        ($ :span {:class "text-xl"} icon)
        ($ :h2 {:class "text-lg font-bold"} title))

      (if (empty? entities)
        ($ :p {:class "text-base-content/60 italic pl-4"} "No settings configured")
        ($ :div {:class "space-y-6"}
          (for [[domain-key entity-keys] (sort-by first grouped)]
            (let [domain-config (or (get domain-groups domain-key)
                                  {:title "Other" :icon "📦" :color "neutral"})]
              ($ :div {:key (name domain-key) :class "space-y-4"}
                ($ :h3 {:class "text-base font-semibold flex items-center gap-2"}
                  ($ :span (:icon domain-config))
                  (:title domain-config))
                ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4 pl-4"}
                  (for [entity-kw (sort entity-keys)]
                    (render-entity-card entity-kw)))))))))))

(defui view-mode-content
  "Content for view mode - shows overview of both scopes."
  [{:keys [page-scope admin-config user-draft]}]
  (let [user-view-options (or (:view-options user-draft) {})
        user-entities (or (:entities user-draft) {})]
    ($ :div {:class "space-y-8"}
      (case page-scope
        :user
        ($ scope-section-overview
          {:title "User Settings"
           :icon "👤"
           :scope-config user-view-options
           :domain-groups defs/user-domain-groups
           :render-entity-card (fn [entity-kw]
                                 ($ user-entity-card-for-overview
                                   {:key (name entity-kw)
                                    :entity-kw entity-kw
                                    :view-options (get user-view-options entity-kw)
                                    :entity-config (get user-entities entity-kw)}))})

        ;; default: admin
        ($ scope-section-overview
          {:title "Admin Settings"
           :icon "⚙️"
           :scope-config admin-config
           :domain-groups defs/admin-domain-groups
           :render-entity-card (fn [entity-kw]
                                 ($ admin-entity-card-for-overview
                                   {:key (name entity-kw)
                                    :entity-kw entity-kw
                                    :settings (get admin-config entity-kw)}))})))))

;; =============================================================================
;; Config Tabs and Editors for User Scope
;; =============================================================================

(defui config-tabs
  "Tab bar for switching between config types."
  [{:keys [tab on-tab-change]}]
  ($ :div {:class "ds-tabs ds-tabs-boxed mb-6"}
    (tabs/tab-link {:label "📋 View Options"
                    :active? (= tab "view-options")
                    :on-select #(on-tab-change "view-options")})
    (tabs/tab-link {:label "📝 Entities"
                    :active? (= tab "entities")
                    :on-select #(on-tab-change "entities")})
    (tabs/tab-link {:label "📄 Form Fields"
                    :active? (= tab "form-fields")
                    :on-select #(on-tab-change "form-fields")})
    (tabs/tab-link {:label "📊 Table Columns"
                    :active? (= tab "table-columns")
                    :on-select #(on-tab-change "table-columns")})))

(defui entity-config-editor
  "Editor for entities.edn - entity title."
  [{:keys [entity-kw entities-config on-title-change on-reset]}]
  (let [entity-config (get entities-config entity-kw {})
        title (or (:title entity-config) "")]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} "Entity Configuration")
          (when on-reset
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-xs ds-btn-ghost"
                        :on-click #(on-reset entity-kw)}
              "Reset")))
        ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
                 :data-tip "Controls the display name used in headings/navigation for this entity."}
          ($ :div {:class "form-control"}
            ($ :label {:class "label"}
              ($ :span {:class "label-text"} "Display Title"))
            ($ :input {:type "text"
                       :class "ds-input ds-input-bordered w-full"
                       :value title
                       :on-change (fn [e]
                                    (on-title-change entity-kw (-> e .-target .-value)))})))))))

(defui form-fields-editor
  "Editor for form-fields.edn - create/edit field lists."
  [{:keys [entity-kw form-fields-config table-columns-config on-toggle on-reset]}]
  (let [entity-config (get form-fields-config entity-kw {})
        table-config (get table-columns-config entity-kw {})
        available-cols (or (:available-columns table-config) [])
        create-fields (set (or (:create-fields entity-config) []))
        edit-fields (set (or (:edit-fields entity-config) []))]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} "Form Fields Configuration")
          (when on-reset
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-xs ds-btn-ghost"
                        :on-click #(on-reset entity-kw)}
              "Reset")))

        ;; Create Fields
        ($ :div {:class "mb-4"}
          ($ :h4 {:class "text-sm font-semibold mb-2"} "Create Form Fields")
          ($ :p {:class "text-xs text-base-content/60 mb-2"}
            "Fields shown when creating a new record")
          (if (seq available-cols)
            ($ :div {:class "grid grid-cols-2 sm:grid-cols-3 gap-2"}
              (for [col available-cols]
                ($ :label {:key (str "create-" col)
                           :class "ds-tooltip ds-tooltip-top flex items-center gap-2 p-2 rounded-lg bg-base-200"
                           :data-tip (str "Toggle whether “" col "” is shown in the Create form.")}
                  ($ :input {:type "checkbox"
                             :class "ds-checkbox ds-checkbox-sm"
                             :checked (contains? create-fields col)
                             :on-change #(on-toggle entity-kw :create-fields col)})
                  ($ :span {:class "text-sm"} col))))
            ($ :p {:class "text-sm text-base-content/60"} "No columns available")))

        ;; Edit Fields
        ($ :div
          ($ :h4 {:class "text-sm font-semibold mb-2"} "Edit Form Fields")
          ($ :p {:class "text-xs text-base-content/60 mb-2"}
            "Fields shown when editing an existing record")
          (if (seq available-cols)
            ($ :div {:class "grid grid-cols-2 sm:grid-cols-3 gap-2"}
              (for [col available-cols]
                ($ :label {:key (str "edit-" col)
                           :class "ds-tooltip ds-tooltip-top flex items-center gap-2 p-2 rounded-lg bg-base-200"
                           :data-tip (str "Toggle whether “" col "” is shown in the Edit form.")}
                  ($ :input {:type "checkbox"
                             :class "ds-checkbox ds-checkbox-sm"
                             :checked (contains? edit-fields col)
                             :on-change #(on-toggle entity-kw :edit-fields col)})
                  ($ :span {:class "text-sm"} col))))
            ($ :p {:class "text-sm text-base-content/60"} "No columns available")))))))

(defui table-columns-editor
  "Editor for table-columns.edn - structural column configuration."
  [{:keys [entity-kw table-columns-config on-toggle on-reset]}]
  (let [entity-config (get table-columns-config entity-kw {})
        available (or (:available-columns entity-config) [])
        always-visible (set (or (:always-visible entity-config) []))
        default-visible (set (or (:default-visible-columns entity-config) []))
        filterable (set (or (:filterable-columns entity-config) []))
        sortable (set (or (:sortable-columns entity-config) []))]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} "Table Columns Configuration")
          (when on-reset
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-xs ds-btn-ghost"
                        :on-click #(on-reset entity-kw)}
              "Reset")))

        ($ :div {:class "text-xs text-base-content/60 mb-3"}
          ($ :p "“Always Visible” columns are structurally enforced by "
            ($ :code {:class "px-1"} "table-columns.edn")
            ". They will always show up in the table (even if “Default Visible” is unchecked), and they won’t be configurable from the “View Options” policy tab."))

        (if (empty? available)
          ($ :p {:class "text-sm text-base-content/60"} "No columns configured")
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full table-fixed"}
              ($ :thead
                ($ :tr
                  ($ :th {:class "whitespace-nowrap"} "Column")

                  ;; IMPORTANT: don't apply ds-tooltip directly to <th>.
                  ;; DaisyUI tooltips set display/position styles that can break table-cell layout,
                  ;; leading to header/body column misalignment.
                  ($ :th {:class "text-center whitespace-nowrap"}
                    ($ :div {:class "ds-tooltip ds-tooltip-bottom"
                             :data-tip "Structurally enforced always visible; users cannot hide these."}
                      "Always Visible"))
                  ($ :th {:class "text-center whitespace-nowrap"}
                    ($ :div {:class "ds-tooltip ds-tooltip-bottom"
                             :data-tip "Visible by default when the table loads."}
                      "Default Visible"))
                  ($ :th {:class "text-center whitespace-nowrap"}
                    ($ :div {:class "ds-tooltip ds-tooltip-bottom"
                             :data-tip "Can be used in filter controls."}
                      "Filterable"))
                  ($ :th {:class "text-center whitespace-nowrap"}
                    ($ :div {:class "ds-tooltip ds-tooltip-bottom"
                             :data-tip "Can be sorted by clicking the column header."}
                      "Sortable"))))
              ($ :tbody
                (for [col available]
                  (let [enforced? (contains? always-visible col)]
                    ($ :tr {:key col}
                      ($ :td {:class "font-medium"}
                        ($ :div {:class "flex items-center gap-2"}
                          ($ :span col)
                          (when enforced?
                            ($ :span {:class "ds-badge ds-badge-xs ds-badge-warning"}
                              "Always visible"))))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column cannot be hidden in the table."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked enforced?
                                     :on-change #(on-toggle entity-kw :always-visible col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column starts visible by default."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? default-visible col)
                                     :on-change #(on-toggle entity-kw :default-visible-columns col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column can be used in filters."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? filterable col)
                                     :on-change #(on-toggle entity-kw :filterable-columns col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column can be sorted."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? sortable col)
                                     :on-change #(on-toggle entity-kw :sortable-columns col)}))))))))))))))

;; =============================================================================
;; Edit Mode Content - Single Scope Editor
;; =============================================================================

(defui admin-entity-editor
  "Editor for a single admin entity's settings."
  [{:keys [entity-kw settings on-change on-column-change]}]
  (let [table-config-from-ui (use-subscribe [:admin/table-config entity-kw])
        table-configs-from-settings (use-subscribe [::admin-settings-events/table-columns])
        table-config-from-settings (get table-configs-from-settings entity-kw)
        ;; Merge both sources:
        ;; - UI cache often has richer metadata (labels, formatters)
        ;; - settings payload is the authoritative structural config (incl. :always-visible)
        ;;
        ;; This also prevents regressions where "always visible" columns show up as
        ;; policy defaults ("Default On") because the UI cache didn't include :always-visible.
        table-config (merge (or table-config-from-ui {}) (or table-config-from-settings {}))]
    ($ views/admin-entity-settings-card
      {:entity-name entity-kw
       :settings settings
       :editing? true
       :on-change on-change
       :setting-keys defs/all-setting-keys
       :table-config table-config
       :on-column-change on-column-change})))

(defui user-entity-editor
  "Editor for a single user entity's settings."
  [{:keys [entity-kw view-options entity-config table-config on-change on-column-change on-reset]}]
  (let [immutable-locks (resolver/feature-constraints->locks (:features entity-config))
        draft-defaults (or (:display-defaults view-options) {})
        draft-locks (or (:display-locks view-options) {})
        draft-col-defaults (or (:column-defaults view-options) {})
        draft-col-locks (or (:column-locks view-options) {})]
    ($ views/user-entity-settings-card
      {:entity-kw entity-kw
       :draft-defaults draft-defaults
       :draft-locks draft-locks
       :draft-column-defaults draft-col-defaults
       :draft-column-locks draft-col-locks
       :immutable-locks immutable-locks
       :editing? true
       :on-change on-change
       :on-column-change on-column-change
       :on-reset on-reset
       :setting-keys defs/all-setting-keys
       :table-config table-config})))

(defui edit-mode-content
  "Content for edit mode - shows editor for selected entity."
  [{:keys [scope selected-entity admin-config user-draft tab on-tab-change
           admin-form-fields admin-table-columns]}]
  (let [;; Admin handlers
        on-admin-change (fn [entity-name setting-key new-state]
                          (rf/dispatch [::admin-settings-events/set-display-setting-draft
                                        entity-name setting-key new-state]))
        on-admin-column-change (fn [entity-name column-key new-state]
                                 (rf/dispatch [::admin-settings-events/set-column-visibility-setting-draft
                                               entity-name column-key new-state]))
        ;; User handlers
        on-user-change (fn [entity-kw setting-key new-state]
                         (rf/dispatch [::user-settings-events/set-display-setting-draft
                                       entity-kw setting-key new-state]))
        on-user-column-change (fn [entity-kw column-key new-state]
                                (rf/dispatch [::user-settings-events/set-column-visibility-setting-draft
                                              entity-kw column-key new-state]))
        on-user-reset (fn [entity-kw]
                        (rf/dispatch [::user-settings-events/reset-entity-display-draft entity-kw]))
        ;; User entity/form-fields/table-columns handlers
        on-user-title-change (fn [entity-kw title]
                               (rf/dispatch [::user-settings-events/set-entity-title-draft entity-kw title]))
        on-user-entity-reset (fn [entity-kw]
                               (rf/dispatch [::user-settings-events/reset-entity-draft entity-kw]))
        on-user-form-field-toggle (fn [entity-kw field-type field-name]
                                    (rf/dispatch [::user-settings-events/toggle-form-field-draft
                                                  entity-kw field-type field-name]))
        on-user-form-fields-reset (fn [entity-kw]
                                    (rf/dispatch [::user-settings-events/reset-form-fields-draft entity-kw]))
        on-user-table-column-toggle (fn [entity-kw list-type col-name]
                                      (rf/dispatch [::user-settings-events/toggle-table-column-in-list-draft
                                                    entity-kw list-type col-name]))
        on-user-table-columns-reset (fn [entity-kw]
                                      (rf/dispatch [::user-settings-events/reset-columns-draft entity-kw]))]
    (if-not selected-entity
      ($ :div {:class "ds-alert ds-alert-info"}
        ($ :span "Select an entity to edit its settings."))
      (case scope
        :admin
        (let [table-columns-config (or admin-table-columns {})
              form-fields-config (or admin-form-fields {})]
          ($ :div {:class "max-w-4xl"}
            ;; Tab bar for admin scope
            ($ config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "entities"
              ($ :div {:class "ds-alert ds-alert-info"}
                ($ :span "Entity configuration is not available for admin scope. Admin entities are defined in code."))

              "form-fields"
              ($ form-fields-editor
                {:entity-kw selected-entity
                 :form-fields-config form-fields-config
                 :table-columns-config table-columns-config
                 :on-toggle (fn [entity-kw field-type field-name]
                              ;; Admin form-fields use immediate save via PATCH
                              (let [current-config (get form-fields-config entity-kw {})
                                    current-fields (set (or (get current-config field-type) []))
                                    field-str (if (keyword? field-name) (name field-name) (str field-name))
                                    new-fields (if (contains? current-fields field-str)
                                                 (vec (remove #{field-str} current-fields))
                                                 (conj (vec current-fields) field-str))
                                    new-config (assoc current-config field-type new-fields)]
                                (rf/dispatch [::admin-settings-events/update-form-fields-entity
                                              entity-kw new-config])))})

              "table-columns"
              ($ table-columns-editor
                {:entity-kw selected-entity
                 :table-columns-config table-columns-config
                 :on-toggle (fn [entity-kw list-type col-name]
                              ;; Admin table-columns use immediate save via PATCH
                              (let [current-config (get table-columns-config entity-kw {})
                                    current-cols (set (or (get current-config list-type) []))
                                    col-str (if (keyword? col-name) (name col-name) (str col-name))
                                    new-cols (if (contains? current-cols col-str)
                                               (vec (remove #{col-str} current-cols))
                                               (conj (vec current-cols) col-str))
                                    new-config (assoc current-config list-type new-cols)]
                                (rf/dispatch [::admin-settings-events/update-table-columns-entity
                                              entity-kw new-config])))})

              ;; default: view-options
              ($ admin-entity-editor
                {:entity-kw selected-entity
                 :settings (get admin-config selected-entity)
                 :on-change on-admin-change
                 :on-column-change on-admin-column-change}))))

        :user
        (let [view-options (get-in user-draft [:view-options selected-entity])
              entity-config (get-in user-draft [:entities selected-entity])
              table-config (get-in user-draft [:table-columns selected-entity])
              entities-config (:entities user-draft)
              form-fields-config (:form-fields user-draft)
              table-columns-config (:table-columns user-draft)]
          ($ :div {:class "max-w-4xl"}
            ;; Tab bar for user scope
            ($ config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "entities"
              ($ entity-config-editor
                {:entity-kw selected-entity
                 :entities-config entities-config
                 :on-title-change on-user-title-change
                 :on-reset on-user-entity-reset})

              "form-fields"
              ($ form-fields-editor
                {:entity-kw selected-entity
                 :form-fields-config form-fields-config
                 :table-columns-config table-columns-config
                 :on-toggle on-user-form-field-toggle
                 :on-reset on-user-form-fields-reset})

              "table-columns"
              ($ table-columns-editor
                {:entity-kw selected-entity
                 :table-columns-config table-columns-config
                 :on-toggle on-user-table-column-toggle
                 :on-reset on-user-table-columns-reset})

              ;; default: view-options
              ($ user-entity-editor
                {:entity-kw selected-entity
                 :view-options view-options
                 :entity-config entity-config
                 :table-config table-config
                 :on-change on-user-change
                 :on-column-change on-user-column-change
                 :on-reset on-user-reset}))))

        ($ :div {:class "ds-alert ds-alert-warning"}
          ($ :span "Unknown scope"))))))

;; =============================================================================
;; Main Page Component
;; =============================================================================

(defui unified-settings-content
  "Main content component for unified settings page."
  [{:keys [page-scope] :or {page-scope :admin}}]
  (let [;; Unified state
        mode (use-subscribe [::unified-events/mode])
        scope (use-subscribe [::unified-events/scope])
        selected-entity (use-subscribe [::unified-events/selected-entity])

        ;; Derived state
        dirty? (use-subscribe [::unified-events/current-scope-dirty?])
        saving? (use-subscribe [::unified-events/current-scope-saving?])
        loading? (use-subscribe [::unified-events/current-scope-loading?])
        error (use-subscribe [::unified-events/current-scope-error])

        ;; Admin config
        admin-config (use-subscribe [::unified-events/admin-view-options])
        admin-form-fields (use-subscribe [::admin-settings-events/form-fields])
        admin-table-columns (use-subscribe [::admin-settings-events/table-columns])
        admin-tab (use-subscribe [::admin-settings-events/config-tab])

        ;; User config
        user-draft (use-subscribe [::user-settings-events/draft])

        ;; Tab state for user scope config editing
        user-tab (use-subscribe [::user-settings-events/tab])

        ;; Current tab based on scope
        tab (case scope :admin admin-tab user-tab)

        ;; Available entities for current scope (union of configured + known groups)
        available-entities (case scope
                             :admin (set/union (defs/entities-for-scope :admin)
                                      (set (keys (or admin-config {}))))
                             :user (set/union (defs/entities-for-scope :user)
                                     (set (keys (or (get user-draft :view-options) {}))))
                             (defs/entities-for-scope page-scope))

        ;; Handlers
        on-mode-change (fn [new-mode]
                         (rf/dispatch [::unified-events/set-mode new-mode]))
        on-scope-change (fn [new-scope]
                          (rf/dispatch [::unified-events/set-scope new-scope]))
        on-entity-change (fn [entity-kw]
                           (rf/dispatch [::unified-events/set-selected-entity entity-kw]))
        on-save (fn []
                  (rf/dispatch [::unified-events/save-current-scope]))
        on-discard (fn []
                     (rf/dispatch [::unified-events/discard-current-scope]))
        on-tab-change (fn [new-tab]
                        (case scope
                          :admin (rf/dispatch [::admin-settings-events/set-config-tab new-tab])
                          :user (rf/dispatch [::user-settings-events/set-tab new-tab])))]

    ;; Initialize on mount
    (use-effect
      (fn []
        (rf/dispatch [::unified-events/init {:initial-scope page-scope
                                             :fixed-scope page-scope
                                             :load-admin? (= page-scope :admin)
                                             :load-user? (= page-scope :user)}])
        js/undefined)
      [page-scope])

    ($ shell/settings-shell
      {:page-title (case page-scope
                     :user "User Settings"
                     "Admin Settings")
       :page-description (case page-scope
                           :user "Manage defaults and locks for user-facing pages"
                           "Manage defaults and locks for admin pages")
       :mode mode
       :on-mode-change on-mode-change
       :scope scope
       :on-scope-change on-scope-change
       :selected-entity selected-entity
       :on-entity-change on-entity-change
       :available-entities available-entities
       :show-scope-switcher? false
       :dirty? dirty?
       :saving? saving?
       :loading? loading?
       :error error
       :on-save on-save
       :on-discard on-discard}

      ;; Content based on mode
      (if (= mode :view)
        ($ view-mode-content
          {:page-scope page-scope
           :admin-config admin-config
           :user-config (get user-draft :view-options {})
           :user-draft user-draft})
        ($ edit-mode-content
          {:scope scope
           :selected-entity selected-entity
           :admin-config admin-config
           :admin-form-fields admin-form-fields
           :admin-table-columns admin-table-columns
           :user-draft user-draft
           :tab tab
           :on-tab-change on-tab-change})))))

(defui unified-settings-page
  "Shared settings page with admin layout.

   Props:
   - :page-scope  :admin | :user (fixed for this route)"
  [{:keys [page-scope] :or {page-scope :admin}}]
  ($ layout/admin-layout
    ($ unified-settings-content {:page-scope page-scope})))

(defui admin-settings-page
  []
  ($ unified-settings-page {:page-scope :admin}))

(defui user-settings-page
  []
  ($ unified-settings-page {:page-scope :user}))
