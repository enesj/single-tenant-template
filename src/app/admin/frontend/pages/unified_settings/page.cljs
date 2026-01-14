(ns app.admin.frontend.pages.unified-settings.page
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.settings-shell :as shell]
    [app.admin.frontend.events.settings :as admin-settings-events]
    [app.admin.frontend.events.unified-settings :as unified-events]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.admin.frontend.pages.unified-settings.editors :as editors]
    [app.admin.frontend.pages.unified-settings.view-mode :as view-mode]
    [app.admin.frontend.settings.definitions :as defs]
    [taoensso.timbre :as timbre]
    [clojure.set :as set]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Edit Mode Content - Single Scope Editor
;; =============================================================================

(defui edit-mode-content
  [{:keys [scope selected-entity admin-config user-draft tab on-tab-change
           admin-form-fields admin-table-columns]}]
  (let [;; Admin handlers
        on-admin-change (fn [entity-name setting-key new-state]
                          (rf/dispatch [::admin-settings-events/set-display-setting-draft
                                        entity-name setting-key new-state]))
        on-admin-display-settings-bulk (fn [entity-name setting-keys new-state]
                                         (rf/dispatch [::admin-settings-events/set-display-settings-bulk
                                                       entity-name setting-keys new-state]))
        on-admin-column-change (fn [entity-name column-key new-state]
                                 (rf/dispatch [::admin-settings-events/set-column-visibility-setting-draft
                                               entity-name column-key new-state]))
        on-admin-column-visibility-bulk (fn [entity-name column-keys new-state]
                                          (rf/dispatch [::admin-settings-events/set-column-visibility-bulk
                                                        entity-name column-keys new-state]))
        ;; User handlers
        on-user-change (fn [entity-kw setting-key new-state]
                         (rf/dispatch [::user-settings-events/set-display-setting-draft
                                       entity-kw setting-key new-state]))
        on-user-display-settings-bulk (fn [entity-kw setting-keys new-state]
                                        (rf/dispatch [::user-settings-events/set-display-settings-bulk
                                                      entity-kw setting-keys new-state]))
        on-user-column-change (fn [entity-kw column-key new-state]
                                (rf/dispatch [::user-settings-events/set-column-visibility-setting-draft
                                              entity-kw column-key new-state]))
        on-user-column-visibility-bulk (fn [entity-kw column-keys new-state]
                                         (rf/dispatch [::user-settings-events/set-column-visibility-bulk
                                                       entity-kw column-keys new-state]))
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
            ($ editors/config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "entities"
              ($ :div {:class "ds-alert ds-alert-info"}
                ($ :span "Entity configuration is not available for admin scope. Admin entities are defined in code."))

              "form-fields"
              ($ editors/form-fields-editor
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
              ($ editors/table-columns-editor
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
                                              entity-kw new-config])))
                 :on-set-list (fn [entity-kw list-type cols]
                                (let [current-config (get table-columns-config entity-kw {})
                                      cols' (->> (or cols [])
                                              (map (fn [c] (if (keyword? c) (name c) (str c))))
                                              vec)
                                      new-config (assoc current-config list-type cols')]
                                  (rf/dispatch [::admin-settings-events/update-table-columns-entity
                                                entity-kw new-config])))})

              ;; default: view-options
              ($ editors/admin-entity-editor
                {:entity-kw selected-entity
                 :settings (get admin-config selected-entity)
                 :on-change on-admin-change
                 :on-display-settings-bulk on-admin-display-settings-bulk
                 :on-column-change on-admin-column-change
                 :on-column-visibility-bulk on-admin-column-visibility-bulk}))))

        :user
        (let [view-options (get-in user-draft [:view-options selected-entity])
              entity-config (get-in user-draft [:entities selected-entity])
              table-config (get-in user-draft [:table-columns selected-entity])
              entities-config (:entities user-draft)
              form-fields-config (:form-fields user-draft)
              table-columns-config (:table-columns user-draft)]
          ($ :div {:class "max-w-4xl"}
            ;; Tab bar for user scope
            ($ editors/config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "entities"
              ($ editors/entity-config-editor
                {:entity-kw selected-entity
                 :entities-config entities-config
                 :on-title-change on-user-title-change
                 :on-reset on-user-entity-reset})

              "form-fields"
              ($ editors/form-fields-editor
                {:entity-kw selected-entity
                 :form-fields-config form-fields-config
                 :table-columns-config table-columns-config
                 :on-toggle on-user-form-field-toggle
                 :on-reset on-user-form-fields-reset})

              "table-columns"
              ($ editors/table-columns-editor
                {:entity-kw selected-entity
                 :table-columns-config table-columns-config
                 :on-toggle on-user-table-column-toggle
                 :on-reset on-user-table-columns-reset
                 :on-set-list (fn [entity-kw list-type cols]
                                (let [cols' (->> (or cols [])
                                              (map (fn [c] (if (keyword? c) (name c) (str c))))
                                              vec)]
                                  (rf/dispatch [::user-settings-events/set-table-column-list-draft
                                                entity-kw list-type cols'])))})

              ;; default: view-options
              ($ editors/user-entity-editor
                {:entity-kw selected-entity
                 :view-options view-options
                 :entity-config entity-config
                 :table-config table-config
                 :on-change on-user-change
                 :on-column-change on-user-column-change
                 :on-display-settings-bulk on-user-display-settings-bulk
                 :on-column-visibility-bulk on-user-column-visibility-bulk
                 :on-reset on-user-reset}))))

        ($ :div {:class "ds-alert ds-alert-warning"}
          ($ :span "Unknown scope"))))))

;; =============================================================================
;; Main Page Component
;; =============================================================================

(defui unified-settings-content
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
        admin-scope-entities (defs/entities-for-scope :admin)
        admin-config-scoped (select-keys (or admin-config {}) admin-scope-entities)
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
                             :admin admin-scope-entities
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
      (do
        (timbre/info "Rendering unified-settings-content"
          {:mode mode
           :scope scope
           :selected-entity selected-entity
           :admin-config-entities (keys admin-config-scoped)})
        (if (= mode :view)
          ($ view-mode/view-mode-content
            {:page-scope page-scope
             :admin-config admin-config-scoped
             :user-config (get user-draft :view-options {})
             :user-draft user-draft})
          ($ edit-mode-content
            {:scope scope
             :selected-entity selected-entity
             :admin-config admin-config-scoped
             :admin-form-fields admin-form-fields
             :admin-table-columns admin-table-columns
             :user-draft user-draft
             :tab tab
             :on-tab-change on-tab-change}))))))

(defui unified-settings-page
  [{:keys [page-scope] :or {page-scope :admin}}]
  ($ layout/admin-layout
    ($ unified-settings-content {:page-scope page-scope})))

(defui admin-settings-page
  []
  ($ unified-settings-page {:page-scope :admin}))

(defui user-settings-page
  []
  ($ unified-settings-page {:page-scope :user}))
