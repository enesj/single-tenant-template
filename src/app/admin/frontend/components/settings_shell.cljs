(ns app.admin.frontend.components.settings-shell
  "Shared settings shell component for admin and user settings pages.
   
   Provides unified UI structure with:
   - Page header
   - View/Edit mode toggle
   - Scope switcher (Admin vs User) in edit mode
   - Entity/page switcher in edit mode
   - Save/Discard buttons
   - Tabs for different config sections"
  (:require
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.settings.definitions :as defs]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Scope Switcher Component
;; =============================================================================

(defui scope-switcher
  "Switcher to toggle between Admin and User scope in edit mode."
  [{:keys [scope on-scope-change disabled?]}]
  ($ :div {:class "ds-btn-group"}
    ($ :button
      {:type "button"
       :class (str "ds-btn ds-btn-sm "
                (if (= scope :admin) "ds-btn-active" "ds-btn-ghost")
                (when disabled? " ds-btn-disabled"))
       :disabled disabled?
       :on-click (fn [e]
                   (.preventDefault e)
                   (when on-scope-change
                     (on-scope-change :admin)))}
      "⚙️ Admin")
    ($ :button
      {:type "button"
       :class (str "ds-btn ds-btn-sm "
                (if (= scope :user) "ds-btn-active" "ds-btn-ghost")
                (when disabled? " ds-btn-disabled"))
       :disabled disabled?
       :on-click (fn [e]
                   (.preventDefault e)
                   (when on-scope-change
                     (on-scope-change :user)))}
      "👤 User")))

;; =============================================================================
;; Entity Selector Component
;; =============================================================================

(defui entity-selector
  "Dropdown to select which entity to edit in edit mode."
  [{:keys [scope selected-entity on-entity-change entities disabled?]}]
  (let [scope-entities (or entities (defs/entities-for-scope scope))
        sorted-entities (sort scope-entities)]
    ($ :div {:class "flex items-center gap-2"}
      ($ :label {:class "text-sm font-medium text-base-content/70"} "Entity:")
      ($ :select
        {:class "ds-select ds-select-sm ds-select-bordered"
         :value (or (some-> selected-entity name) "")
         :disabled disabled?
         :on-change (fn [e]
                      (let [val (.-value (.-target e))]
                        (when (and on-entity-change (seq val))
                          (on-entity-change (keyword val)))))}
        ($ :option {:value ""} "Select entity...")
        (for [entity sorted-entities]
          ($ :option {:key (name entity) :value (name entity)}
            (defs/entity-title entity)))))))

;; =============================================================================
;; Mode Toggle Component
;; =============================================================================

(defui mode-toggle
  "Toggle between View and Edit mode."
  [{:keys [mode on-mode-change disabled?]}]
  (let [is-view? (= mode :view)]
    ($ :button
      {:type "button"
       :class (str "ds-btn ds-btn-sm "
                (if is-view? "ds-btn-primary" "ds-btn-warning")
                (when disabled? " ds-btn-disabled"))
       :disabled disabled?
       :on-click (fn [e]
                   (.preventDefault e)
                   (when on-mode-change
                     (on-mode-change (if is-view? :edit :view))))}
      (if is-view? "Edit Settings" "Stop Editing"))))

;; =============================================================================
;; Save/Discard Buttons Component
;; =============================================================================

(defui save-discard-buttons
  "Save and Discard buttons for edit mode."
  [{:keys [dirty? saving? loading? on-save on-discard]}]
  ($ :div {:class "flex items-center gap-2"}
    ($ :button
      {:type "button"
       :class (str "ds-btn ds-btn-sm ds-btn-ghost"
                (when (or (not dirty?) saving? loading?) " ds-btn-disabled"))
       :disabled (or (not dirty?) saving? loading?)
       :on-click (fn [e]
                   (.preventDefault e)
                   (when on-discard (on-discard)))}
      "Discard changes")
    ($ :button
      {:type "button"
       :class (str "ds-btn ds-btn-sm ds-btn-primary"
                (when (or (not dirty?) saving? loading?) " ds-btn-disabled"))
       :disabled (or (not dirty?) saving? loading?)
       :on-click (fn [e]
                   (.preventDefault e)
                   (when on-save (on-save)))}
      (if saving?
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
        "Save settings"))))

;; =============================================================================
;; Page Header Component
;; =============================================================================

(defui page-header
  "Page header with title, description, and icon."
  [{:keys [title description]}]
  ($ :div {:class "flex items-center gap-4"}
    ($ :div {:class "p-3 rounded-full bg-gradient-to-br from-primary/20 to-secondary/20"}
      ($ :svg {:class "h-8 w-8 text-primary" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                  :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                  :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))
    ($ :div
      ($ :h1 {:class "text-2xl font-bold text-base-content"} title)
      ($ :p {:class "text-base-content/70"} description))))

;; =============================================================================
;; Config Tabs Component
;; =============================================================================

(defui config-tabs
  "Tabs for different config sections."
  [{:keys [tab on-tab-change tabs]}]
  ($ :div {:class "ds-tabs ds-tabs-boxed"}
    (for [{:keys [key label]} tabs
          :let [key-str (if (keyword? key) (name key) (str key))]]
      (tabs/tab-link {:id (str "tab-settings-config-" key-str)
                      :key key
                      :label label
                      :active? (= tab key)
                      :on-select #(when on-tab-change (on-tab-change key))}))))

;; =============================================================================
;; Edit Mode Instructions
;; =============================================================================

(defui edit-mode-instructions
  "Instructions displayed when in edit mode."
  [{:keys [scope tab]}]
  (let [admin? (= scope :admin)]
    ($ :div {:class "ds-alert ds-alert-warning mb-6"}
      ($ :div
        ($ :h4 {:class "font-bold"} "Edit Mode Active")
        ($ :p {:class "text-sm"}
          (cond
            (and admin? (= tab "view-options"))
            "Editing admin view options (policy defaults/locks). Click a setting to cycle through states, then click 'Save settings' to persist."

            (and admin? (= tab "form-fields"))
            "Editing admin form fields. Changes are saved immediately."

            (and admin? (= tab "table-columns"))
            "Editing admin table columns. Changes are saved immediately."

            (and (not admin?) (= tab "view-options"))
            "Editing domain-owned user UI defaults/locks. Click a setting to cycle through states, then click 'Save settings' to persist."

            (and (not admin?) (= tab "form-fields"))
            "Editing domain-owned form fields. Toggle fields, then click 'Save settings' to persist."

            (and (not admin?) (= tab "table-columns"))
            "Editing domain-owned table columns. Toggle columns, then click 'Save settings' to persist."

            :else
            "Click settings to modify. Save to persist changes."))))))

;; =============================================================================
;; Main Settings Shell Component
;; =============================================================================

(defui settings-shell
  "Main shell component for settings pages.
   
   Props:
   - :page-title - Title shown in header
   - :page-description - Description shown under title
   - :mode - :view | :edit
   - :on-mode-change - fn [new-mode]
   - :scope - :admin | :user (current editing scope)
   - :on-scope-change - fn [new-scope]
   - :selected-entity - keyword or nil (current entity being edited)
   - :on-entity-change - fn [entity-kw]
   - :available-entities - set of entity keywords (optional, defaults to scope entities)
   - :tab - current tab key (e.g. 'view-options')
   - :on-tab-change - fn [tab-key]
   - :tabs - [{:key 'view-options' :label '📋 View Options'} ...]
   - :dirty? - boolean
   - :saving? - boolean
   - :loading? - boolean
   - :error - error message or nil
   - :on-save - fn []
   - :on-discard - fn []
   - :children - content to render"
  [{:keys [page-title page-description mode on-mode-change
           scope on-scope-change selected-entity on-entity-change available-entities
           tab on-tab-change tabs
           dirty? saving? loading? error
           on-save on-discard
           show-scope-switcher?
           children]}]
  (let [is-edit? (= mode :edit)
        show-scope-switcher? (if (some? show-scope-switcher?) show-scope-switcher? true)]
    ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
      ;; Page header
      ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-6"}
        ($ page-header {:title page-title :description page-description}))

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

      ;; Main content area
      ($ :div {:class "px-4 sm:px-6 lg:px-8"}
        ;; Top toolbar: tabs + mode + save/discard
        ($ :div {:class "flex items-center justify-between mb-6 flex-wrap gap-4"}
          ;; Left side: tabs
          (when (seq tabs)
            ($ config-tabs {:tab tab :on-tab-change on-tab-change :tabs tabs}))

          ;; Right side: mode toggle + save/discard
          ($ :div {:class "flex items-center gap-4"}
            (when (and is-edit? dirty?)
              ($ save-discard-buttons
                {:dirty? dirty?
                 :saving? saving?
                 :loading? loading?
                 :on-save on-save
                 :on-discard on-discard}))
            ($ mode-toggle
              {:mode mode
               :on-mode-change on-mode-change
               :disabled? (or loading? saving?)})))

        ;; Edit mode: scope + entity selectors
        (when is-edit?
          ($ :div {:class "flex items-center gap-6 mb-6 flex-wrap"}
            ;; Scope switcher
            (when show-scope-switcher?
              ($ :div {:class "flex items-center gap-2"}
                ($ :label {:class "text-sm font-medium text-base-content/70"} "Scope:")
                ($ scope-switcher
                  {:scope scope
                   :on-scope-change on-scope-change
                   :disabled? (or loading? saving?)})))
            ;; Entity selector
            ($ entity-selector
              {:scope scope
               :selected-entity selected-entity
               :on-entity-change on-entity-change
               :entities available-entities
               :disabled? (or loading? saving?)})))

        ;; Edit mode instructions
        (when is-edit?
          ($ edit-mode-instructions {:scope scope :tab tab}))

        ;; Children content
        children))))
