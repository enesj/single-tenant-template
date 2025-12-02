(ns app.admin.frontend.pages.settings
  "Admin page displaying all hardcoded list view settings from view-options.edn"
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.events.settings :as settings-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; Display settings that can be hardcoded in view-options.edn
(def ^:private display-setting-keys
  [:show-edit?
   :show-delete?
   :show-select?
   :show-filtering?
   :show-pagination?
   :show-highlights?
   :show-timestamps?])

(defn- setting-label
  "Convert a setting key to a human-readable label"
  [setting-key]
  (-> setting-key
    name
    (str/replace #"\?" "")
    (str/replace #"-" " ")
    str/capitalize))

(defui setting-badge
  "Badge showing a setting's status - clickable to cycle through states"
  [{:keys [entity-name setting-key value on-change editing?]}]
  (let [is-true? (true? value)
        is-false? (false? value)
        next-value (cond
                     is-true? false
                     is-false? nil  ; nil means remove
                     :else true)
        handle-click (fn [e]
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
        (setting-label setting-key))
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
  [{:keys [entity-name settings editing? on-change]}]
  (let [hardcoded-settings (select-keys settings display-setting-keys)
        has-any-hardcoded? (seq hardcoded-settings)
        ;; In edit mode, show all possible settings
        display-settings (if editing?
                           (reduce (fn [m k] (if (contains? m k) m (assoc m k nil)))
                                   hardcoded-settings
                                   display-setting-keys)
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

(defui settings-summary
  "Summary statistics for all hardcoded settings"
  [{:keys [all-view-options]}]
  (let [entity-count (count all-view-options)
        entities-with-hardcoded (filter
                                  (fn [[_ settings]]
                                    (some #(contains? settings %) display-setting-keys))
                                  all-view-options)
        hardcoded-entity-count (count entities-with-hardcoded)
        total-hardcoded-settings (reduce
                                   (fn [acc [_ settings]]
                                     (+ acc (count (select-keys settings display-setting-keys))))
                                   0
                                   all-view-options)]
    ($ :div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6"}
      ;; Total entities card
      ($ :div {:class "ds-stat bg-base-100 rounded-lg shadow"}
        ($ :div {:class "ds-stat-title"} "Total Entities")
        ($ :div {:class "ds-stat-value text-primary"} entity-count)
        ($ :div {:class "ds-stat-desc"} "In view-options.edn"))

      ;; Entities with hardcoded settings
      ($ :div {:class "ds-stat bg-base-100 rounded-lg shadow"}
        ($ :div {:class "ds-stat-title"} "With Hardcoded")
        ($ :div {:class "ds-stat-value text-secondary"} hardcoded-entity-count)
        ($ :div {:class "ds-stat-desc"} "Entities with locked settings"))

      ;; Total hardcoded settings
      ($ :div {:class "ds-stat bg-base-100 rounded-lg shadow"}
        ($ :div {:class "ds-stat-title"} "Total Hardcoded")
        ($ :div {:class "ds-stat-value text-accent"} total-hardcoded-settings)
        ($ :div {:class "ds-stat-desc"} "Individual settings locked")))))

(defui settings-legend
  "Legend explaining the setting states"
  []
  ($ :div {:class "ds-alert ds-alert-info mb-6"}
    ($ :div
      ($ :h4 {:class "font-bold mb-2"} "About Hardcoded Settings")
      ($ :p {:class "text-sm mb-2"}
        "Hardcoded settings are defined in "
        ($ :code {:class "bg-base-300 px-1 rounded"} "resources/public/admin/ui-config/view-options.edn")
        " and control which list view toggles are locked for each entity page.")
      ($ :div {:class "flex flex-wrap gap-4 mt-3"}
        ($ :div {:class "flex items-center gap-2"}
          ($ :span {:class "ds-badge ds-badge-success ds-badge-sm"} "Enabled")
          ($ :span {:class "text-sm"} "Setting is locked ON (users cannot disable)"))
        ($ :div {:class "flex items-center gap-2"}
          ($ :span {:class "ds-badge ds-badge-error ds-badge-sm"} "Disabled")
          ($ :span {:class "text-sm"} "Setting is locked OFF (users cannot enable)"))
        ($ :div {:class "flex items-center gap-2"}
          ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "Not set")
          ($ :span {:class "text-sm"} "Setting is user-configurable"))))))

(defui admin-settings-content
  "Main content for the settings overview page"
  []
  (let [;; Use editable view options from settings events (updates optimistically)
        ;; Falls back to config-loader cache if not yet loaded from backend
        editable-view-options (use-subscribe [::settings-events/editable-view-options])
        config-view-options (use-subscribe [:admin/all-view-options])
        all-view-options (if (seq editable-view-options) 
                           editable-view-options 
                           config-view-options)
        loading? (use-subscribe [::settings-events/loading?])
        saving? (use-subscribe [::settings-events/saving?])
        error (use-subscribe [::settings-events/error])
        editing? (use-subscribe [::settings-events/editing?])
        sorted-entities (sort-by first all-view-options)

        handle-toggle-edit (fn [e]
                             (.preventDefault e)
                             (log/info "Edit button clicked, dispatching toggle-editing")
                             (rf/dispatch [::settings-events/toggle-editing]))

        handle-change (fn [entity-name setting-key new-value]
                        (log/info "handle-change called" {:entity entity-name 
                                                          :setting setting-key 
                                                          :new-value new-value})
                        (if (nil? new-value)
                          (rf/dispatch [::settings-events/remove-entity-setting
                                        (name entity-name)
                                        (name setting-key)])
                          (rf/dispatch [::settings-events/update-entity-setting
                                        (name entity-name)
                                        (name setting-key)
                                        new-value])))]
    ;; Load view options from backend on mount
    (use-effect
      (fn []
        (rf/dispatch [::settings-events/load-view-options])
        js/undefined)
      [])

    ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
      ;; Page header
      ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "flex items-center gap-4"}
            ($ :div {:class "p-3 rounded-full bg-gradient-to-br from-primary/20 to-secondary/20"}
              ($ :svg {:class "h-8 w-8 text-primary" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))
            ($ :div
              ($ :h1 {:class "text-2xl font-bold text-base-content"} "List View Settings Overview")
              ($ :p {:class "text-base-content/70"} "View and manage hardcoded display settings for all entity pages")))

          ;; Edit toggle button
          ($ :button {:type "button"
                      :class (str "ds-btn "
                                  (if editing? "ds-btn-warning" "ds-btn-primary")
                                  (when (or loading? saving?) " ds-btn-disabled"))
                      :on-click handle-toggle-edit
                      :disabled (or loading? saving?)}
            (if saving?
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
              (if editing?
                "Cancel Editing"
                "Edit Settings")))))

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
        ;; Legend
        ($ settings-legend)

        ;; Edit mode instructions
        (when editing?
          ($ :div {:class "ds-alert ds-alert-warning mb-6"}
            ($ :div
              ($ :h4 {:class "font-bold"} "Edit Mode Active")
              ($ :p {:class "text-sm"}
                "Click on any setting to cycle through: Enabled → Disabled → Remove. "
                "Changes are saved immediately to view-options.edn."))))

        ;; Summary statistics
        ($ settings-summary {:all-view-options all-view-options})

        ;; Entity cards grid
        (if (seq sorted-entities)
          ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"}
            (for [[entity-name settings] sorted-entities]
              ($ entity-settings-card {:key entity-name
                                       :entity-name entity-name
                                       :settings settings
                                       :editing? editing?
                                       :on-change handle-change})))
          ($ :div {:class "ds-alert ds-alert-warning"}
            ($ :span "No view options found. Make sure view-options.edn is properly loaded.")))))))

(defui admin-settings-page
  "Admin settings overview page"
  []
  ($ layout/admin-layout
    ($ admin-settings-content)))
