(ns app.admin.frontend.pages.unified-settings.editors
  (:require
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.settings.definitions :as defs]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.events.list.ui-state :as list-ui-events]
    [app.template.frontend.settings.resolver :as resolver]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.template.frontend.utils.column-config :as column-config]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

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
  "Editor for form-fields.edn - create/edit/batch-edit field lists.

  Field universe is derived from:
  1. Fields already referenced in :create-fields / :edit-fields / :batch-edit-fields / :field-config
  2. Model spec fields (from models.edn) as additional available options"
  [{:keys [entity-kw form-fields-config on-toggle on-reset]}]
  (let [entity-config (get form-fields-config entity-kw {})
        normalize-field-id (fn [x]
                             (some-> x model-naming/ensure-app-keyword))
        field-id->storage (fn [field-id]
                            (some-> field-id model-naming/app-keyword->db name))
        config-fields-raw (concat (:create-fields entity-config)
                            (:edit-fields entity-config)
                            (:batch-edit-fields entity-config)
                            (keys (or (:field-config entity-config) {})))
        config-field-ids (->> config-fields-raw
                           (keep normalize-field-id)
                           distinct
                           vec)
        config-display-by-id (reduce
                               (fn [acc raw]
                                 (if-let [field-id (normalize-field-id raw)]
                                   (assoc acc field-id (or (get acc field-id)
                                                         (field-id->storage field-id)))
                                   acc))
                               {}
                               config-fields-raw)
        create-fields (set (keep normalize-field-id (or (:create-fields entity-config) [])))
        edit-fields (set (keep normalize-field-id (or (:edit-fields entity-config) [])))
        batch-edit-fields-source (if (contains? entity-config :batch-edit-fields)
                                   (:batch-edit-fields entity-config)
                                   (:edit-fields entity-config))
        batch-edit-fields (set (keep normalize-field-id (or batch-edit-fields-source [])))
        config-field-set (set config-field-ids)
        ;; Model spec fields as fallback universe (base model specs, not config-overridden)
        all-form-specs (use-subscribe [:form-entity-specs])
        form-specs (get all-form-specs entity-kw)
        model-field-ids (->> (or form-specs [])
                          (keep (fn [f]
                                  (when (map? f)
                                    (some-> (:id f) normalize-field-id))))
                          distinct
                          vec)
        ;; Config fields first (preserving order), then model fields not yet in config
        available-field-ids (into (vec config-field-ids)
                              (remove config-field-set)
                              model-field-ids)
        display-name-for (fn [field-id]
                           (or (get config-display-by-id field-id)
                             (field-id->storage field-id)))
        input-id (fn [section-key field-name]
                   (str "form-fields-"
                     (name entity-kw)
                     "-"
                     (name section-key)
                     "-"
                     field-name))
        render-field-section (fn [{:keys [section-key title subtitle checked-fields]}]
                               ($ :div {:class "mb-4"
                                        :id (str "form-fields-section-" (name entity-kw) "-" (name section-key))}
                                 ($ :h4 {:class "text-sm font-semibold mb-2"} title)
                                 ($ :p {:class "text-xs text-base-content/60 mb-2"} subtitle)
                                 (if (seq available-field-ids)
                                   ($ :div {:class "grid grid-cols-2 sm:grid-cols-3 gap-2"}
                                     (for [field-id available-field-ids
                                           :let [field-name (display-name-for field-id)
                                                 checkbox-id (input-id section-key field-name)]]
                                       ($ :label {:key (str (name section-key) "-" field-name)
                                                  :htmlFor checkbox-id
                                                  :class "ds-tooltip ds-tooltip-top flex items-center gap-2 p-2 rounded-lg bg-base-200"
                                                  :data-tip (str "Toggle whether `" field-name "` is shown in the " title ".")}
                                         ($ :input {:id checkbox-id
                                                    :type "checkbox"
                                                    :class "ds-checkbox ds-checkbox-sm"
                                                    :checked (contains? checked-fields field-id)
                                                    :on-change #(on-toggle entity-kw section-key field-id)})
                                         ($ :span {:class "text-sm"} field-name))))
                                   ($ :p {:class "text-sm text-base-content/60"} "No columns available"))))]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} "Form Fields Configuration")
          (when on-reset
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-xs ds-btn-ghost"
                        :on-click #(on-reset entity-kw)}
              "Reset")))

        (render-field-section
          {:section-key :create-fields
           :title "Create Form Fields"
           :subtitle "Fields shown when creating a new record"
           :checked-fields create-fields})

        (render-field-section
          {:section-key :edit-fields
           :title "Edit Form Fields"
           :subtitle "Fields shown when editing an existing record"
           :checked-fields edit-fields})

        (render-field-section
          {:section-key :batch-edit-fields
           :title "Batch Edit Form Fields"
           :subtitle "Fields shown when updating multiple records at once"
           :checked-fields batch-edit-fields})))))

(defui table-columns-editor
  "Editor for table-columns.edn - structural column configuration."
  [{:keys [entity-kw table-columns-config on-toggle on-reset on-set-list on-set-label]}]
  (let [entity-config (get table-columns-config entity-kw {})
        available (or (:available-columns entity-config) [])
        always-visible (set (or (:always-visible entity-config) []))
        default-visible (set (or (:default-visible-columns entity-config) []))
        filterable (set (or (:filterable-columns entity-config) []))
        sortable (set (or (:sortable-columns entity-config) []))
        column-metadata (or (:column-metadata entity-config) {})

        ;; Build a stable, complete set of columns to show:
        ;; - DB-backed fields from models metadata (via :entity-specs)
        ;; - computed fields from models metadata
        ;; - computed fields declared in table-columns.edn (:computed-fields)
        ;; - any other columns referenced in lists/config (for resilience)
        all-specs (or (use-subscribe [:entity-specs]) {})
        spec-fields (get all-specs entity-kw)
        spec-ids (->> (or spec-fields [])
                   (keep (fn [f]
                           (when (map? f)
                             (:id f))))
                   vec)

        config-refs (->> (concat
                           (or (:available-columns entity-config) [])
                           (or (:default-visible-columns entity-config) [])
                           (or (:filterable-columns entity-config) [])
                           (or (:sortable-columns entity-config) [])
                           (or (:always-visible entity-config) [])
                           (keys (or (:computed-fields entity-config) {}))
                           (keys (or (:column-config entity-config) {}))
                           (keys (or (:column-metadata entity-config) {})))
                      (keep identity)
                      vec)

        infer-separator (fn [xs]
                          (let [names (map (fn [x]
                                             (cond
                                               (keyword? x) (name x)
                                               (string? x) x
                                               :else (str x)))
                                        (or xs []))
                                dash-count (count (filter #(str/includes? % "-") names))
                                underscore-count (count (filter #(str/includes? % "_") names))]
                            (cond
                              (and (pos? dash-count) (>= dash-count underscore-count)) "-"
                              (pos? underscore-count) "_"
                              :else "_")))
        sep (infer-separator (concat available config-refs))
        to-style (fn [x]
                   (let [s (cond
                             (nil? x) nil
                             (keyword? x) (name x)
                             (string? x) x
                             :else (str x))]
                     (when (seq s)
                       (case sep
                         "_" (str/replace s "-" "_")
                         "-" (str/replace s "_" "-")
                         s))))

        available* (->> available (keep to-style) vec)
        available-set (set available*)

        spec-order* (->> spec-ids (keep to-style) vec)
        computed-ids* (->> (keys (or (:computed-fields entity-config) {}))
                        (keep to-style)
                        vec)
        config-refs* (->> config-refs (keep to-style) vec)

        rest-spec (remove available-set spec-order*)
        seen (set (concat available* rest-spec))
        rest-config (->> (distinct (concat computed-ids* config-refs*))
                      (remove seen)
                      sort)

        all-cols (vec (concat available* rest-spec rest-config))

        [dragging-col set-dragging-col!] (use-state nil)
        [label-edits set-label-edits!] (use-state {})
        can-reorder? (and (fn? on-set-list) (seq available*))

        lookup-column-entry (fn [m col]
                              (resolver/lookup-column-entry m col))
        default-column-label (fn [col]
                               (-> col
                                 (str/replace "_" " ")
                                 (str/replace "-" " ")
                                 str/capitalize))
        normalize-label (fn [label]
                          (let [trimmed (some-> label str str/trim)]
                            (when-not (str/blank? (or trimmed ""))
                              trimmed)))
        effective-label-value (fn [col]
                                (if (contains? label-edits col)
                                  (get label-edits col)
                                  (or (some-> (lookup-column-entry column-metadata col)
                                        :label
                                        str
                                        str/trim)
                                    "")))
        commit-label! (fn [col raw-value]
                        (let [trimmed (normalize-label raw-value)]
                          (set-label-edits! dissoc col)
                          (when (fn? on-set-label)
                            (on-set-label entity-kw col (or trimmed "")))))

        reorder-available! (fn [from-col to-col]
                             (when can-reorder?
                               (let [from-index (.indexOf available* from-col)
                                     to-index (.indexOf available* to-col)]
                                 (when (and (>= from-index 0)
                                         (>= to-index 0)
                                         (not= from-index to-index))
                                   (on-set-list entity-kw
                                     :available-columns
                                     (column-config/reorder-vector
                                       available*
                                       from-index
                                       to-index))))))

        ;; Compute "all selected" states for each column type (for bulk toggles)
        in-table-all? (and (seq all-cols) (= (count available*) (count all-cols)))
        all-always-visible? (and (seq available*) (every? #(contains? always-visible %) available*))
        all-default-visible? (and (seq available*) (every? #(contains? default-visible %) available*))
        all-filterable? (and (seq available*) (every? #(contains? filterable %) available*))
        all-sortable? (and (seq available*) (every? #(contains? sortable %) available*))]

    (use-effect
      (fn []
        (set-label-edits! {})
        js/undefined)
      [entity-kw entity-config])

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
        ($ :p {:class "text-xs text-base-content/60 mb-3"}
          "Drag the handle next to a column to set the default column order for the list view.")

        (if (empty? all-cols)
          ($ :p {:class "text-sm text-base-content/60"} "No columns found")
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full table-fixed"}
              ($ :thead
                ($ :tr
                  ($ :th {:class "whitespace-nowrap"} "Column")
                  ($ :th {:class "whitespace-nowrap"} "Display Label")

                  ($ :th {:class "text-center whitespace-nowrap"}
                    ($ :div {:class "ds-tooltip ds-tooltip-bottom"
                             :data-tip "Whether this column is included in :available-columns (i.e. appears in the table and can be configured)."}
                      "In table"))

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
                (when (and (seq all-cols) (fn? on-set-list))
                  ($ :tr {:class "bg-base-200"}
                    ($ :th {:class "font-medium text-sm italic"} "Toggle All")
                    ($ :th {:class "text-center text-xs text-base-content/60"} "—")
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if in-table-all? "Remove all" "Add all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked in-table-all?
                                   :on-change (fn [_]
                                                (if in-table-all?
                                                  (on-set-list entity-kw :available-columns [])
                                                  (on-set-list entity-kw :available-columns all-cols)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-always-visible? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-always-visible?
                                   :on-change (fn [_]
                                                (if all-always-visible?
                                                  (on-set-list entity-kw :always-visible [])
                                                  (on-set-list entity-kw :always-visible available*)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-default-visible? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-default-visible?
                                   :on-change (fn [_]
                                                (if all-default-visible?
                                                  (on-set-list entity-kw :default-visible-columns [])
                                                  (on-set-list entity-kw :default-visible-columns available*)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-filterable? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-filterable?
                                   :on-change (fn [_]
                                                (if all-filterable?
                                                  (on-set-list entity-kw :filterable-columns [])
                                                  (on-set-list entity-kw :filterable-columns available*)))})))
                    ($ :th {:class "text-center"}
                      ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                               :data-tip (if all-sortable? "Deselect all" "Select all")}
                        ($ :input {:type "checkbox"
                                   :class "ds-checkbox ds-checkbox-sm"
                                   :checked all-sortable?
                                   :on-change (fn [_]
                                                (if all-sortable?
                                                  (on-set-list entity-kw :sortable-columns [])
                                                  (on-set-list entity-kw :sortable-columns available*)))}))))))
              ($ :tbody
                (for [col all-cols]
                  (let [in-table? (contains? available-set col)
                        enforced? (contains? always-visible col)
                        row-id (str "table-columns-row-" (name entity-kw) "-" col)
                        label-value (effective-label-value col)]
                    ($ :tr {:key col
                            :id row-id
                            :class (when (= dragging-col col) "opacity-70")
                            :on-drag-over (when (and can-reorder? in-table?)
                                            (fn [e] (.preventDefault e)))
                            :on-drop (when (and can-reorder? in-table?)
                                       (fn [e]
                                         (.preventDefault e)
                                         (let [dt (.-dataTransfer e)
                                               dragged (or dragging-col
                                                         (when dt
                                                           (.getData dt "text/plain")))
                                               dragged-col (to-style dragged)]
                                           (when (and (seq dragged-col) (not= dragged-col col))
                                             (reorder-available! dragged-col col))
                                           (set-dragging-col! nil))))}
                      ($ :td {:class "font-medium"}
                        ($ :div {:class "flex items-center gap-2"}
                          (when (and can-reorder? in-table?)
                            ($ :button {:type "button"
                                        :id (str "col-order-drag-" (name entity-kw) "-" col)
                                        :class "cursor-grab select-none text-base-content/40"
                                        :title "Drag to reorder"
                                        :draggable true
                                        :on-click (fn [e] (.stopPropagation e))
                                        :on-drag-start (fn [e]
                                                         (set-dragging-col! col)
                                                         (when-let [dt (.-dataTransfer e)]
                                                           (.setData dt "text/plain" col)))
                                        :on-drag-end (fn [_e]
                                                       (set-dragging-col! nil))}
                              "⋮⋮"))
                          ($ :span {:class (when-not in-table? "opacity-60")} col)
                          (when-not in-table?
                            ($ :span {:class "ds-badge ds-badge-xs ds-badge-ghost"}
                              "Not in table"))
                          (when enforced?
                            ($ :span {:class "ds-badge ds-badge-xs ds-badge-warning"}
                              "Always visible"))))
                      ($ :td {:class "align-middle"}
                        ($ :input {:type "text"
                                   :id (str "col-label-" (name entity-kw) "-" col)
                                   :class "ds-input ds-input-xs w-full"
                                   :placeholder (default-column-label col)
                                   :value label-value
                                   :on-change (fn [e]
                                                (set-label-edits! assoc col (.. e -target -value)))
                                   :on-blur (fn [e]
                                              (commit-label! col (.. e -target -value)))
                                   :on-key-down (fn [e]
                                                  (when (= "Enter" (.-key e))
                                                    (.blur (.-target e))))}))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip (if in-table?
                                             "Remove from table (removes other structural flags too)."
                                             "Add to table (adds to :available-columns).")}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked in-table?
                                     :on-change #(on-toggle entity-kw :available-columns col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column cannot be hidden in the table."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked enforced?
                                     :disabled (not in-table?)
                                     :on-change #(on-toggle entity-kw :always-visible col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column starts visible by default."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? default-visible col)
                                     :disabled (not in-table?)
                                     :on-change #(on-toggle entity-kw :default-visible-columns col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column can be used in filters."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? filterable col)
                                     :disabled (not in-table?)
                                     :on-change #(on-toggle entity-kw :filterable-columns col)})))
                      ($ :td {:class "text-center"}
                        ($ :div {:class "ds-tooltip ds-tooltip-top inline-block"
                                 :data-tip "If enabled, this column can be sorted."}
                          ($ :input {:type "checkbox"
                                     :class "ds-checkbox ds-checkbox-sm"
                                     :checked (contains? sortable col)
                                     :disabled (not in-table?)
                                     :on-change #(on-toggle entity-kw :sortable-columns col)}))))))))))))))

;; =============================================================================
;; Edit Mode Content - Single Scope Editor
;; =============================================================================

(defui admin-entity-editor
  "Editor for a single admin entity's settings."
  [{:keys [entity-kw settings on-change on-display-settings-bulk on-list-config-change on-action-gate-change]}]
  (let [local-display-prefs (use-subscribe [::ui-subs/entity-display-prefs entity-kw])
        clear-local-display-prefs! (fn [entity-kw]
                                     (rf/dispatch [::list-ui-events/clear-display-prefs entity-kw]))]
    ($ :div {:class "space-y-4"}
      ($ cards/admin-entity-settings-card
        {:entity-name entity-kw
         :settings settings
         :local-display-prefs local-display-prefs
         :on-clear-local-display-prefs clear-local-display-prefs!
         :editing? true
         :on-change on-change
         :on-display-settings-bulk on-display-settings-bulk
         :setting-keys defs/all-setting-keys})
      ($ cards/list-behavior-card
        {:entity-kw entity-kw
         :list-config (:list-config settings)
         :editing? true
         :on-setting-change on-list-config-change
         :on-action-gate-change on-action-gate-change}))))

(defui user-entity-editor
  "Editor for a single user entity's settings.

  admin-view-options: the admin-scope view-options for this entity.
  Used to show admin-imposed locks as inherited/immutable in the user editor."
  [{:keys [entity-kw view-options entity-config admin-view-options
           on-change on-display-settings-bulk on-reset on-list-config-change on-action-gate-change]}]
  (let [feature-locks (resolver/feature-constraints->locks (:features entity-config))
        admin-locks (:locks (resolver/parse-view-options (or admin-view-options {})))
        immutable-locks (merge feature-locks admin-locks)
        draft-defaults (or (:display-defaults view-options) {})
        draft-locks (or (:display-locks view-options) {})
        local-display-prefs (use-subscribe [::ui-subs/entity-display-prefs entity-kw])
        clear-local-display-prefs! (fn [entity-kw]
                                     (rf/dispatch [::list-ui-events/clear-display-prefs entity-kw]))]
    ($ :div {:class "space-y-4"}
      ($ cards/user-entity-settings-card
        {:entity-kw entity-kw
         :draft-defaults draft-defaults
         :draft-locks draft-locks
         :immutable-locks immutable-locks
         :local-display-prefs local-display-prefs
         :on-clear-local-display-prefs clear-local-display-prefs!
         :editing? true
         :on-change on-change
         :on-display-settings-bulk on-display-settings-bulk
         :on-reset on-reset
         :setting-keys defs/all-setting-keys})
      ($ cards/list-behavior-card
        {:entity-kw entity-kw
         :list-config (:list-config view-options)
         :editing? true
         :on-setting-change on-list-config-change
         :on-action-gate-change on-action-gate-change}))))
