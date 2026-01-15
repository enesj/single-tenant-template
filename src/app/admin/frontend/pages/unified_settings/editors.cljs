(ns app.admin.frontend.pages.unified-settings.editors
  (:require
    [app.admin.frontend.components.settings-views :as views]
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.settings.definitions :as defs]
    [app.template.frontend.settings.resolver :as resolver]
    [uix.core :refer [$ defui]]))

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
    (tabs/tab-link {:label "📄 Form Fields"
                    :active? (= tab "form-fields")
                    :on-select #(on-tab-change "form-fields")})
    (tabs/tab-link {:label "📊 Table Columns"
                    :active? (= tab "table-columns")
                    :on-select #(on-tab-change "table-columns")})))
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
  [{:keys [entity-kw table-columns-config on-toggle on-reset on-set-list]}]
  (let [entity-config (get table-columns-config entity-kw {})
        available (or (:available-columns entity-config) [])
        always-visible (set (or (:always-visible entity-config) []))
        default-visible (set (or (:default-visible-columns entity-config) []))
        filterable (set (or (:filterable-columns entity-config) []))
        sortable (set (or (:sortable-columns entity-config) []))
        ;; Compute "all selected" states for each column type
        all-always-visible? (and (seq available) (every? #(contains? always-visible %) available))
        all-default-visible? (and (seq available) (every? #(contains? default-visible %) available))
        all-filterable? (and (seq available) (every? #(contains? filterable %) available))
        all-sortable? (and (seq available) (every? #(contains? sortable %) available))]
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
                      "Sortable")))
                ;; Toggle All row in thead for visual grouping
                (when (and (seq available) (fn? on-set-list))
                  ($ :tr {:class "bg-base-200"}
                    ($ :th {:class "font-medium text-sm italic"} "Toggle All")
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-always-visible? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-always-visible?
                                   :on-change (fn [_]
                                                (if all-always-visible?
                                                  (on-set-list entity-kw :always-visible [])
                                                  (on-set-list entity-kw :always-visible available)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-default-visible? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-default-visible?
                                   :on-change (fn [_]
                                                (if all-default-visible?
                                                  (on-set-list entity-kw :default-visible-columns [])
                                                  (on-set-list entity-kw :default-visible-columns available)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-filterable? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-filterable?
                                   :on-change (fn [_]
                                                (if all-filterable?
                                                  (on-set-list entity-kw :filterable-columns [])
                                                  (on-set-list entity-kw :filterable-columns available)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-sortable? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-sortable?
                                   :on-change (fn [_]
                                                (if all-sortable?
                                                  (on-set-list entity-kw :sortable-columns [])
                                                  (on-set-list entity-kw :sortable-columns available)))}))))))
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
  [{:keys [entity-kw settings on-change on-display-settings-bulk]}]
  ($ views/admin-entity-settings-card
    {:entity-name entity-kw
     :settings settings
     :editing? true
     :on-change on-change
     :on-display-settings-bulk on-display-settings-bulk
     :setting-keys defs/all-setting-keys}))

(defui user-entity-editor
  "Editor for a single user entity's settings."
  [{:keys [entity-kw view-options entity-config on-change on-display-settings-bulk on-reset]}]
  (let [immutable-locks (resolver/feature-constraints->locks (:features entity-config))
        draft-defaults (or (:display-defaults view-options) {})
        draft-locks (or (:display-locks view-options) {})]
    ($ views/user-entity-settings-card
      {:entity-kw entity-kw
       :draft-defaults draft-defaults
       :draft-locks draft-locks
       :immutable-locks immutable-locks
       :editing? true
       :on-change on-change
       :on-display-settings-bulk on-display-settings-bulk
       :on-reset on-reset
       :setting-keys defs/all-setting-keys})))
