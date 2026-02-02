(ns app.admin.frontend.components.settings-views.badges
  (:require
    [app.admin.frontend.settings.definitions :as defs]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Admin-style Setting Badge (simple cycle: true -> false -> nil/remove)
;; =============================================================================

(defui admin-setting-badge
  "Badge showing a setting's status for admin settings - clickable to cycle.
   Cycles through: Locked On -> Locked Off -> Inherit (remove)
   Includes tooltip with setting description.

   Props:
   - :entity-name - entity keyword
   - :setting-key - setting keyword
   - :value - current value (true/false/nil)
   - :editing? - whether in edit mode
   - :on-change - fn [entity-name setting-key new-value]"
  [{:keys [entity-name setting-key value editing? on-change]}]
  (let [is-true? (true? value)
        is-false? (false? value)
        help-text (defs/setting-help setting-key)
        next-value (cond
                     is-true? false
                     is-false? nil  ; nil means remove
                     :else true)
        handle-click (fn [_e]
                       (when (and editing? on-change)
                         (on-change entity-name setting-key next-value)))]
    ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
             :data-tip help-text}
      ($ :div {:class (str "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full "
                        (when editing? "cursor-pointer hover:bg-base-300 transition-colors"))
               :on-click handle-click}
        ($ :span {:class "text-sm font-medium min-w-[120px]"}
          (defs/setting-label setting-key))
        (cond
          is-true?
          ($ :span {:class "ds-badge ds-badge-success ds-badge-sm"} "Locked On")

          is-false?
          ($ :span {:class "ds-badge ds-badge-error ds-badge-sm"} "Locked Off")

          :else
          ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "Inherit"))
        ;; Show edit hint when in edit mode
        (when editing?
          ($ :span {:class "text-xs text-base-content/50 ml-auto"}
            (cond
              is-true? "→ Locked Off"
              is-false? "→ Remove"
              :else "→ Locked On")))))))
