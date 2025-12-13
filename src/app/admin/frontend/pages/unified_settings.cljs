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
    [app.admin.frontend.events.settings :as admin-settings-events]
    [app.admin.frontend.events.unified-settings :as unified-events]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.admin.frontend.settings.definitions :as defs]
    [app.template.frontend.settings.resolver :as resolver]
    [clojure.set :as set]
    [clojure.string :as str]
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
  [{:keys [page-scope admin-config user-config user-draft]}]
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
;; Edit Mode Content - Single Scope Editor
;; =============================================================================

(defui admin-entity-editor
  "Editor for a single admin entity's settings."
  [{:keys [entity-kw settings on-change on-column-change]}]
  (let [table-config (use-subscribe [:admin/table-config entity-kw])]
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
  [{:keys [scope selected-entity admin-config user-draft]}]
  (let [on-admin-change (fn [entity-name setting-key new-state]
                          (rf/dispatch [::admin-settings-events/set-display-setting-draft
                                        entity-name setting-key new-state]))
        on-admin-column-change (fn [entity-name column-key new-state]
                                 (rf/dispatch [::admin-settings-events/set-column-visibility-setting-draft
                                               entity-name column-key new-state]))
        on-user-change (fn [entity-kw setting-key new-state]
                         (rf/dispatch [::user-settings-events/set-display-setting-draft
                                       entity-kw setting-key new-state]))
        on-user-column-change (fn [entity-kw column-key new-state]
                                (rf/dispatch [::user-settings-events/set-column-visibility-setting-draft
                                              entity-kw column-key new-state]))
        on-user-reset (fn [entity-kw]
                        (rf/dispatch [::user-settings-events/reset-entity-display-draft entity-kw]))]
    (if-not selected-entity
      ($ :div {:class "ds-alert ds-alert-info"}
        ($ :span "Select an entity to edit its settings."))
      (case scope
        :admin
        ($ :div {:class "max-w-2xl"}
          ($ admin-entity-editor
            {:entity-kw selected-entity
             :settings (get admin-config selected-entity)
             :on-change on-admin-change
             :on-column-change on-admin-column-change}))

        :user
        (let [view-options (get-in user-draft [:view-options selected-entity])
              entity-config (get-in user-draft [:entities selected-entity])
              table-config (get-in user-draft [:table-columns selected-entity])]
          ($ :div {:class "max-w-2xl"}
            ($ user-entity-editor
              {:entity-kw selected-entity
               :view-options view-options
               :entity-config entity-config
               :table-config table-config
               :on-change on-user-change
               :on-column-change on-user-column-change
               :on-reset on-user-reset})))

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

        ;; User config
        user-draft (use-subscribe [::user-settings-events/draft])

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
                     (rf/dispatch [::unified-events/discard-current-scope]))]

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
           :user-draft user-draft})))))

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
