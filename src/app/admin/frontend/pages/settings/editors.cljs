(ns app.admin.frontend.pages.settings.editors
  (:require
    [clojure.string :as str]
    [uix.core :refer [$ defui use-effect use-state]]))

;; =============================================================================
;; Form Fields Editor Components
;; =============================================================================

(defui field-list-editor
  "Editable list of fields for create/edit/required"
  [{:keys [label fields available-fields on-change editing?]}]
  (let [[local-fields set-local-fields!] (use-state (set fields))]
    (use-effect
      (fn []
        (set-local-fields! (set fields))
        js/undefined)
      [fields])
    ($ :div {:class "mb-4"}
      ($ :label {:class "text-sm font-medium mb-2 block"} label)
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [field available-fields]
          (let [is-selected? (contains? local-fields field)]
            ($ :button {:key (name field)
                        :type "button"
                        :class (str "ds-badge ds-badge-lg cursor-pointer transition-all "
                                 (if is-selected?
                                   "ds-badge-primary"
                                   "ds-badge-outline ds-badge-ghost")
                                 (when-not editing? " opacity-60 cursor-not-allowed"))
                        :disabled (not editing?)
                        :on-click (fn [_]
                                    (when editing?
                                      (let [new-fields (if is-selected?
                                                         (disj local-fields field)
                                                         (conj local-fields field))]
                                        (set-local-fields! new-fields)
                                        (when on-change
                                          (on-change (vec new-fields))))))}
              (name field))))))))

(defui form-fields-entity-editor
  "Editor for a single entity's form fields configuration"
  [{:keys [entity-name config editing? on-save]}]
  (let [create-fields (or (:create-fields config) [])
        edit-fields (or (:edit-fields config) [])
        ;; Collect all known fields from config
        all-fields (vec (distinct (concat create-fields edit-fields (keys (:field-config config)))))
        [local-config set-local-config!] (use-state config)
        has-changes? (not= local-config config)]

    (use-effect
      (fn []
        (set-local-config! config)
        js/undefined)
      [config])

    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (-> entity-name name str/capitalize))
          (when (and editing? has-changes?)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-primary ds-btn-sm"
                        :on-click (fn [_]
                                    (when on-save
                                      (on-save entity-name local-config)))}
              "Save Changes")))

        ($ field-list-editor
          {:label "Create Fields"
           :fields (:create-fields local-config)
           :available-fields all-fields
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :create-fields new-fields)))})

        ($ field-list-editor
          {:label "Edit Fields"
           :fields (:edit-fields local-config)
           :available-fields all-fields
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :edit-fields new-fields)))})

        ($ field-list-editor
          {:label "Required Fields"
           :fields (:required-fields local-config)
           :available-fields (distinct (concat (:create-fields local-config) (:edit-fields local-config)))
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :required-fields new-fields)))})))))

;; =============================================================================
;; Table Columns Editor Components
;; =============================================================================

(defui column-list-editor
  "Editable list of columns"
  [{:keys [label columns available-columns on-change editing? help-text]}]
  (let [[local-columns set-local-columns!] (use-state (set columns))]
    (use-effect
      (fn []
        (set-local-columns! (set columns))
        js/undefined)
      [columns])
    ($ :div {:class "mb-4"}
      ($ :label {:class "text-sm font-medium mb-1 block"} label)
      (when help-text
        ($ :p {:class "text-xs text-base-content/60 mb-2"} help-text))
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [col available-columns]
          (let [is-selected? (contains? local-columns col)]
            ($ :button {:key (name col)
                        :type "button"
                        :class (str "ds-badge ds-badge-lg cursor-pointer transition-all "
                                 (if is-selected?
                                   "ds-badge-secondary"
                                   "ds-badge-outline ds-badge-ghost")
                                 (when-not editing? " opacity-60 cursor-not-allowed"))
                        :disabled (not editing?)
                        :on-click (fn [_]
                                    (when editing?
                                      (let [new-cols (if is-selected?
                                                       (disj local-columns col)
                                                       (conj local-columns col))]
                                        (set-local-columns! new-cols)
                                        (when on-change
                                          (on-change (vec new-cols))))))}
              (name col))))))))

(defui table-columns-entity-editor
  "Editor for a single entity's table columns configuration"
  [{:keys [entity-name config editing? on-save]}]
  (let [available-columns (or (:available-columns config) [])
        [local-config set-local-config!] (use-state config)
        has-changes? (not= local-config config)]

    (use-effect
      (fn []
        (set-local-config! config)
        js/undefined)
      [config])

    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (-> entity-name name str/capitalize))
          (when (and editing? has-changes?)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-primary ds-btn-sm"
                        :on-click (fn [_]
                                    (when on-save
                                      (on-save entity-name local-config)))}
              "Save Changes")))

        ;; Available columns (read-only reference)
        ($ :div {:class "mb-4"}
          ($ :label {:class "text-sm font-medium mb-2 block"} "Available Columns")
          ($ :div {:class "flex flex-wrap gap-1"}
            (for [col available-columns]
              ($ :span {:key (name col)
                        :class "ds-badge ds-badge-sm ds-badge-outline"}
                (name col)))))

        ($ column-list-editor
          {:label "Default Visible"
           :columns (:default-visible-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns visible by default (users can hide them unless always visible)"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :default-visible-columns new-cols)))})

        ($ column-list-editor
          {:label "Always Visible"
           :columns (:always-visible local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that cannot be hidden"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :always-visible new-cols)))})

        ($ column-list-editor
          {:label "Filterable"
           :columns (:filterable-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that can be filtered"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :filterable-columns new-cols)))})

        ($ column-list-editor
          {:label "Sortable"
           :columns (:sortable-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that can be sorted"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :sortable-columns new-cols)))})))))

