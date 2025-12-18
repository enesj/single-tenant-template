(ns app.admin.frontend.components.settings-views.editor
  (:require
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.settings.definitions :as defs]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Settings Editor - Edit Mode for Single Entity
;; =============================================================================

(defui entity-settings-editor
  "Full editor for a single entity's settings.
   Shows all possible settings with current values and edit controls.

   Props:
   - :scope - :admin | :user
   - :entity-kw - entity keyword
   - :settings - current settings map
   - :on-change - fn [entity-kw setting-key new-value] (admin) or fn [entity-kw setting-key new-state] (user)
   - :on-reset - fn [entity-kw] - reset to saved values"
  [{:keys [scope entity-kw settings on-change on-reset]}]
  (if (= scope :admin)
    ;; Admin style: simple true/false/nil cycle
    ($ cards/admin-entity-settings-card
      {:entity-name entity-kw
       :settings settings
       :editing? true
       :on-change on-change
       :setting-keys defs/all-setting-keys})
    ;; User style: defaults/locks cycle
    (let [view-options (get settings :view-options)
          entity-view-options (get view-options entity-kw)
          draft-defaults (or (:display-defaults entity-view-options) {})
          draft-locks (or (:display-locks entity-view-options) {})]
      ($ cards/user-entity-settings-card
        {:entity-kw entity-kw
         :draft-defaults draft-defaults
         :draft-locks draft-locks
         :immutable-locks {}  ; TODO: get from entity config features
         :on-change on-change
         :on-reset on-reset
         :setting-keys defs/all-setting-keys}))))

