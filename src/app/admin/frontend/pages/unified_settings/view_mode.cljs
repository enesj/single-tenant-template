(ns app.admin.frontend.pages.unified-settings.view-mode
  (:require
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.settings.definitions :as defs]
    [app.template.frontend.settings.resolver :as resolver]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; View Mode Content - Overview of Both Scopes
;; =============================================================================

(defui admin-entity-card-for-overview
  "Entity card for admin overview - shows current settings (read-only)."
  [{:keys [entity-kw settings]}]
  ($ :div {:class "space-y-4"}
    ($ cards/admin-entity-settings-card
      {:entity-name entity-kw
       :settings settings
       :editing? false
       :setting-keys defs/all-setting-keys})
    ($ cards/list-behavior-card
      {:entity-kw entity-kw
       :list-config (:list-config settings)
       :editing? false})))

(defui user-entity-card-for-overview
  "Entity card for user overview - shows current settings (read-only)."
  [{:keys [entity-kw view-options entity-config]}]
  (let [;; Feature constraints are always enforced and cannot be overridden.
        immutable-locks (resolver/feature-constraints->locks (:features entity-config))
        draft-defaults (or (get-in view-options [:display-defaults]) {})
        draft-locks (or (get-in view-options [:display-locks]) {})]
    ($ :div {:class "space-y-4"}
      ($ cards/user-entity-settings-card
        {:entity-kw entity-kw
         :draft-defaults draft-defaults
         :draft-locks draft-locks
         :immutable-locks immutable-locks
         :setting-keys defs/all-setting-keys})
      ($ cards/list-behavior-card
        {:entity-kw entity-kw
         :list-config (:list-config view-options)
         :editing? false}))))

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
  (let [user-entity-keys (defs/entities-for-scope :user)
        user-view-options (merge (zipmap user-entity-keys (repeat {}))
                            (or (:view-options user-draft) {}))
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
