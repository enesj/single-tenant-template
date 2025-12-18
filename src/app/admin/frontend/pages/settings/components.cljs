(ns app.admin.frontend.pages.settings.components
  (:require
    [app.admin.frontend.pages.settings.constants :as c]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]))

(defui setting-badge
  "Badge showing a setting's status - clickable to cycle through states"
  [{:keys [entity-name setting-key value on-change editing?]}]
  (let [is-true? (true? value)
        is-false? (false? value)
        next-value (cond
                     is-true? false
                     is-false? nil  ; nil means remove
                     :else true)
        handle-click (fn [_e]
                       (log/info "Setting badge clicked" {:entity entity-name
                                                          :setting setting-key
                                                          :editing? editing?
                                                          :next-value next-value})
                       (when (and editing? on-change)
                         (on-change entity-name setting-key next-value)))]
    ($ :div {:class (str "flex items-center gap-2 p-2 rounded-lg bg-base-200 "
                      (when editing? "cursor-pointer hover:bg-base-300 transition-colors"))
             :on-click handle-click}
      ($ :span {:class "text-sm font-medium min-w-[120px]"}
        (c/setting-label setting-key))
      (cond
        is-true?
        ($ :span {:class "ds-badge ds-badge-success ds-badge-sm"}
          "Enabled")

        is-false?
        ($ :span {:class "ds-badge ds-badge-error ds-badge-sm"}
          "Disabled")

        :else
        ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"}
          "Not set"))
      ;; Show edit hint when in edit mode
      (when editing?
        ($ :span {:class "text-xs text-base-content/50 ml-auto"}
          (cond
            is-true? "→ Disabled"
            is-false? "→ Remove"
            :else "→ Enabled"))))))

(defui entity-settings-card
  "Card displaying all hardcoded settings for a single entity"
  [{:keys [entity-name settings editing? on-change setting-keys]}]
  (let [setting-keys (or setting-keys c/display-setting-keys)
        locks (or (:display-locks settings) {})
        hardcoded-settings (select-keys locks setting-keys)
        has-any-hardcoded? (seq hardcoded-settings)
        ;; In edit mode, show all possible settings
        display-settings (if editing?
                           (reduce (fn [m k] (if (contains? m k) m (assoc m k nil)))
                             hardcoded-settings
                             setting-keys)
                           hardcoded-settings)]
    ($ :div {:class "ds-card bg-base-100 shadow-md hover:shadow-lg transition-shadow"}
      ($ :div {:class "ds-card-body p-4"}
        ;; Entity header
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (-> entity-name name str/capitalize))
          (if has-any-hardcoded?
            ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
              (str (count hardcoded-settings) " hardcoded"))
            ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"}
              "No hardcoded settings")))

        ;; Settings grid
        (if (or has-any-hardcoded? editing?)
          ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
            (for [[setting-key value] (sort-by first display-settings)]
              ($ setting-badge {:key (str entity-name "-" setting-key)
                                :entity-name entity-name
                                :setting-key setting-key
                                :value value
                                :editing? editing?
                                :on-change on-change})))
          ($ :p {:class "text-base-content/60 text-sm italic"}
            "All settings are user-configurable"))))))

(defui domain-section
  "Render a domain section with its entities"
  [{:keys [_domain-key domain-config entities editing? on-change setting-keys show-actions?]}]
  (let [domain-color (get domain-config :color "neutral")
        color-classes (case domain-color
                        "primary" "from-primary/10 to-primary/5 border-primary/20"
                        "secondary" "from-secondary/10 to-secondary/5 border-secondary/20"
                        "accent" "from-accent/10 to-accent/5 border-accent/20"
                        "from-neutral/10 to-neutral/5 border-neutral/20")
        ;; Combine display and action keys if showing actions
        combined-setting-keys (if show-actions?
                                c/all-setting-keys
                                (or setting-keys c/display-setting-keys))]
    ($ :div {:class "mb-8 last:mb-0"}
      ;; Domain header
      ($ :div {:class (str "flex items-center gap-3 mb-4 p-4 rounded-lg bg-gradient-to-r "
                        color-classes " border")}
        ($ :span {:class "text-2xl"} (:icon domain-config))
        ($ :div
          ($ :h2 {:class "text-xl font-bold text-base-content"} (:title domain-config))
          ($ :p {:class "text-sm text-base-content/70"} (:description domain-config)))
        ($ :span {:class "ml-auto text-sm font-medium text-base-content/60"}
          (str (count entities) " entities")))

      ;; Entity cards grid for this domain
      ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pl-4"}
        (for [[entity-name settings] entities]
          ($ entity-settings-card {:key entity-name
                                   :entity-name entity-name
                                   :settings settings
                                   :editing? editing?
                                   :on-change on-change
                                   :setting-keys combined-setting-keys})))) ))

