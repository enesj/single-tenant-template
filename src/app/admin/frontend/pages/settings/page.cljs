(ns app.admin.frontend.pages.settings.page
  (:require
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.events.settings :as settings-events]
    [app.admin.frontend.pages.settings.tabs :as settings-tabs]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Main Admin Settings Content
;; =============================================================================

(defui admin-settings-content
  "Main content for the settings overview page"
  []
  (let [;; View options state
        editable-view-options (use-subscribe [::settings-events/editable-view-options])
        view-options-dirty? (use-subscribe [::settings-events/view-options-dirty?])
        config-view-options (use-subscribe [:admin/all-view-options])
        all-view-options (if (seq editable-view-options)
                           editable-view-options
                           config-view-options)

        ;; Form fields state
        form-fields (use-subscribe [::settings-events/form-fields])
        form-fields-loading? (use-subscribe [::settings-events/form-fields-loading?])

        ;; Table columns state
        table-columns (use-subscribe [::settings-events/table-columns])
        table-columns-loading? (use-subscribe [::settings-events/table-columns-loading?])

        ;; Common state
        loading? (use-subscribe [::settings-events/loading?])
        saving? (use-subscribe [::settings-events/saving?])
        error (use-subscribe [::settings-events/error])
        editing? (use-subscribe [::settings-events/editing?])
        config-tab (use-subscribe [::settings-events/config-tab])

        ;; Local state (persisted)
        domain-tab (use-subscribe [::settings-events/domain-tab])
        set-domain-tab! (fn [tab] (rf/dispatch [::settings-events/set-domain-tab tab]))
        render-main-tab (fn [label key]
                          (tabs/tab-link {:label label
                                          :active? (= config-tab key)
                                          :on-select #(rf/dispatch [::settings-events/set-config-tab key])}))

        handle-toggle-edit (fn [e]
                             (when e (.preventDefault e))
                             (rf/dispatch [::settings-events/toggle-editing]))

        handle-view-option-change (fn [entity-name setting-key new-value]
                                    (rf/dispatch [::settings-events/set-view-option-draft
                                                  entity-name
                                                  setting-key
                                                  new-value]))

        handle-view-options-save (fn [e]
                                   (when e (.preventDefault e))
                                   (rf/dispatch [::settings-events/save-view-options]))

        handle-view-options-discard (fn [e]
                                      (when e (.preventDefault e))
                                      (rf/dispatch [::settings-events/reset-view-options-draft]))

        handle-form-fields-save (fn [entity-name config]
                                  (rf/dispatch [::settings-events/update-form-fields-entity
                                                entity-name config]))

        handle-table-columns-save (fn [entity-name config]
                                    (rf/dispatch [::settings-events/update-table-columns-entity
                                                  entity-name config]))]

    ;; Load data on mount
    (use-effect
      (fn []
        (rf/dispatch [::settings-events/load-view-options])
        (rf/dispatch [::settings-events/load-form-fields])
        (rf/dispatch [::settings-events/load-table-columns])
        js/undefined)
      [])

    ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
      ;; Page header
      ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-6"}
        ($ :div {:class "flex items-center gap-4"}
          ($ :div {:class "p-3 rounded-full bg-gradient-to-br from-primary/20 to-secondary/20"}
            ($ :svg {:class "h-8 w-8 text-primary" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))
          ($ :div
            ($ :h1 {:class "text-2xl font-bold text-base-content"} "Admin UI Configuration")
            ($ :p {:class "text-base-content/70"} "Manage view options, form fields, and table columns for all entity pages"))))

      ;; Error alert
      (when error
        ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-4"}
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span error))))

      ;; Loading indicator
      (when loading?
        ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-4"}
          ($ :div {:class "ds-alert ds-alert-info"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm mr-2"})
            ($ :span "Loading settings from server..."))))

      ;; Main content
      ($ :div {:class "px-4 sm:px-6 lg:px-8"}
        ;; Edit mode toggle and main tabs
        ($ :div {:class "flex items-center justify-between mb-6"}
          ;; Main config tabs
          ($ :div {:class "ds-tabs ds-tabs-boxed"}
            (render-main-tab "📋 View Options" "view-options")
            (render-main-tab "📝 Form Fields" "form-fields")
            (render-main-tab "📊 Table Columns" "table-columns"))

          ;; Actions
          ($ :div {:class "flex items-center gap-2"}
            (when (and editing? (= config-tab "view-options"))
              ($ :button {:type "button"
                          :class (str "ds-btn ds-btn-sm ds-btn-ghost"
                                   (when (or loading? saving? (not view-options-dirty?)) " ds-btn-disabled"))
                          :on-click handle-view-options-discard
                          :disabled (or loading? saving? (not view-options-dirty?))}
                "Discard changes"))

            (when (and editing? (= config-tab "view-options"))
              ($ :button {:type "button"
                          :class (str "ds-btn ds-btn-sm ds-btn-primary"
                                   (when (or loading? saving? (not view-options-dirty?)) " ds-btn-disabled"))
                          :on-click handle-view-options-save
                          :disabled (or loading? saving? (not view-options-dirty?))}
                (if saving?
                  ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                  "Save settings")))

            ($ :button {:type "button"
                        :class (str "ds-btn ds-btn-sm "
                                 (if editing? "ds-btn-warning" "ds-btn-primary")
                                 (when (or loading? saving?) " ds-btn-disabled"))
                        :on-click handle-toggle-edit
                        :disabled (or loading? saving?)}
              (if (and saving? (not (= config-tab "view-options")))
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                (if editing?
                  "Stop Editing"
                  "Edit Settings")))))

        ;; Edit mode instructions
        (when editing?
          ($ :div {:class "ds-alert ds-alert-warning mb-6"}
            ($ :div
              ($ :h4 {:class "font-bold"} "Edit Mode Active")
              ($ :p {:class "text-sm"}
                (case config-tab
                  "view-options" "Click on any setting to cycle through: Enabled → Disabled → Remove. Click 'Save settings' to persist."
                  "form-fields" "Click fields to toggle them in each list. Click 'Save Changes' to persist."
                  "table-columns" "Click columns to toggle them in each list. Click 'Save Changes' to persist."
                  "Changes are saved immediately.")))))

        ;; Main tab content rendering
        (cond
          (= config-tab "view-options")
          ($ settings-tabs/view-options-tab-content
            {:all-view-options all-view-options
             :editing? editing?
             :on-change handle-view-option-change
             :active-domain-tab domain-tab
             :set-domain-tab! set-domain-tab!})

          (= config-tab "form-fields")
          ($ settings-tabs/form-fields-tab-content
            {:form-fields form-fields
             :editing? editing?
             :on-save handle-form-fields-save
             :loading? form-fields-loading?})

          (= config-tab "table-columns")
          ($ settings-tabs/table-columns-tab-content
            {:table-columns table-columns
             :editing? editing?
             :on-save handle-table-columns-save
             :loading? table-columns-loading?}))))))

