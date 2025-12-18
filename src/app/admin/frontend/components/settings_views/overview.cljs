(ns app.admin.frontend.components.settings-views.overview
  (:require
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.settings.definitions :as defs]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Domain Section
;; =============================================================================

(defui domain-section
  "Render a domain section header with its entities.

   Props:
   - :domain-config - map with :title, :description, :icon, :color
   - :children - content to render inside the section"
  [{:keys [domain-config children]}]
  (let [color-classes (defs/domain-color-classes (:color domain-config))]
    ($ :div {:class "mb-8 last:mb-0"}
      ;; Domain header
      ($ :div {:class (str "flex items-center gap-3 mb-4 p-4 rounded-lg bg-gradient-to-r "
                        color-classes " border")}
        ($ :span {:class "text-2xl"} (:icon domain-config))
        ($ :div
          ($ :h2 {:class "text-xl font-bold text-base-content"} (:title domain-config))
          ($ :p {:class "text-sm text-base-content/70"} (:description domain-config))))
      ;; Children
      ($ :div {:class "pl-4"}
        children))))

;; =============================================================================
;; Settings Overview - Read-Only View
;; =============================================================================

(defui scope-overview-section
  "Overview section for a single scope (admin or user).
   Shows all entities with their current settings (read-only).

   Props:
   - :scope - :admin | :user
   - :config - map of entity-kw -> settings for this scope
   - :title - section title
   - :icon - emoji icon"
  [{:keys [scope config title icon]}]
  (let [domain-groups (defs/domain-groups-for-scope scope)
        entities (keys config)
        grouped (defs/group-entities-by-domain entities)]
    ($ :div {:class "mb-8"}
      ;; Section header
      ($ :div {:class "flex items-center gap-2 mb-4 pb-2 border-b border-base-300"}
        ($ :span {:class "text-xl"} icon)
        ($ :h2 {:class "text-lg font-bold"} title))

      ;; Entities by domain
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
                    (let [entity-config (get config entity-kw)]
                      ($ cards/admin-entity-settings-card
                        {:key (name entity-kw)
                         :entity-name entity-kw
                         :settings entity-config
                         :editing? false
                         :setting-keys defs/all-setting-keys}))))))))))))

(defui settings-overview
  "Complete settings overview showing both admin and user scopes.
   This is the read-only view mode content.

   Props:
   - :admin-config - map of entity-kw -> settings for admin scope
   - :user-config - map of entity-kw -> settings for user scope"
  [{:keys [admin-config user-config]}]
  ($ :div {:class "space-y-8"}
    ($ scope-overview-section
      {:scope :admin
       :config admin-config
       :title "Admin Settings"
       :icon "⚙️"})
    ($ scope-overview-section
      {:scope :user
       :config user-config
       :title "User Settings"
       :icon "👤"})))

