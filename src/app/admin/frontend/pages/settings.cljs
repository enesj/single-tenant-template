(ns app.admin.frontend.pages.settings
  "Admin page displaying all hardcoded list view settings from view-options.edn,
   form-fields.edn, and table-columns.edn with editing capabilities"
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.events.settings :as settings-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-effect use-state]]
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

(def ^:private action-setting-keys
  [:show-add-button?
   :show-batch-edit?
   :show-batch-delete?])

(def ^:private all-setting-keys
  (into display-setting-keys action-setting-keys))

;; Domain organization for entities
(def ^:private domain-groups
  {:user-management
   {:title "User Management"
    :description "Manage users and administrators"
    :icon "👥"
    :entities #{:users :admins}
    :color "primary"}

   :security-audit
   {:title "Security & Audit"
    :description "System audit trail and security monitoring"
    :icon "🔒"
    :entities #{:audit-logs :login-events}
    :color "secondary"}

   :expenses
   {:title "Expenses Management"
    :description "Track expenses, receipts, and suppliers"
    :icon "💰"
    :entities #{:expenses :receipts :suppliers :payers :articles :article-aliases :price-observations}
    :color "accent"}})

(defn- setting-label
  "Convert a setting key to a human-readable label"
  [setting-key]
  (-> setting-key
    name
    (str/replace #"\?" "")
    (str/replace #"-" " ")
    str/capitalize))

(defn- get-entity-domain
  "Find which domain an entity belongs to"
  [entity-key]
  (some (fn [[domain-key domain-config]]
          (when (contains? (:entities domain-config) entity-key)
            domain-key))
    domain-groups))

(defn- group-entities-by-domain
  "Group entities by their domains"
  [sorted-entities]
  (reduce (fn [acc [entity-key settings]]
            (if-let [domain-key (get-entity-domain entity-key)]
              (update acc domain-key conj [entity-key settings])
              (update acc :other conj [entity-key settings])))
    {}
    sorted-entities))

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
  [{:keys [entity-name settings editing? on-change setting-keys]}]
  (let [setting-keys (or setting-keys display-setting-keys)
        hardcoded-settings (select-keys settings setting-keys)
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

(defui settings-summary
  "Summary statistics for all hardcoded settings"
  [{:keys [all-view-options]}]
  (let [entity-count (count all-view-options)
        entities-with-hardcoded (filter
                                  (fn [[_ settings]]
                                    (some #(contains? settings %) all-setting-keys))
                                  all-view-options)
        hardcoded-entity-count (count entities-with-hardcoded)
        total-hardcoded-settings (reduce
                                   (fn [acc [_ settings]]
                                     (+ acc (count (select-keys settings all-setting-keys))))
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
        " and control which list view toggles and action buttons are locked for each entity page.")
      ($ :p {:class "text-sm mb-2"}
        "Domain groupings and classifications are configured in the application code within this settings page.")
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
                                all-setting-keys
                                (or setting-keys display-setting-keys))]
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
                                   :setting-keys combined-setting-keys}))))))

;; =============================================================================
;; Form Fields Editor Components
;; =============================================================================

(defui field-list-editor
  "Editable list of fields for create/edit/required"
  [{:keys [label fields available-fields on-change editing?]}]
  (let [[local-fields set-local-fields!] (use-state (set fields))]
    (use-effect
      (fn []
        (set-local-fields! (set fields))
        js/undefined)
      [fields])
    ($ :div {:class "mb-4"}
      ($ :label {:class "text-sm font-medium mb-2 block"} label)
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [field available-fields]
          (let [is-selected? (contains? local-fields field)]
            ($ :button {:key (name field)
                        :type "button"
                        :class (str "ds-badge ds-badge-lg cursor-pointer transition-all "
                                 (if is-selected?
                                   "ds-badge-primary"
                                   "ds-badge-outline ds-badge-ghost")
                                 (when-not editing? " opacity-60 cursor-not-allowed"))
                        :disabled (not editing?)
                        :on-click (fn [_]
                                    (when editing?
                                      (let [new-fields (if is-selected?
                                                         (disj local-fields field)
                                                         (conj local-fields field))]
                                        (set-local-fields! new-fields)
                                        (when on-change
                                          (on-change (vec new-fields))))))}
              (name field))))))))

(defui form-fields-entity-editor
  "Editor for a single entity's form fields configuration"
  [{:keys [entity-name config editing? on-save]}]
  (let [create-fields (or (:create-fields config) [])
        edit-fields (or (:edit-fields config) [])
        ;; Collect all known fields from config
        all-fields (vec (distinct (concat create-fields edit-fields (keys (:field-config config)))))
        [local-config set-local-config!] (use-state config)
        has-changes? (not= local-config config)]

    (use-effect
      (fn []
        (set-local-config! config)
        js/undefined)
      [config])

    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (-> entity-name name str/capitalize))
          (when (and editing? has-changes?)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-primary ds-btn-sm"
                        :on-click (fn [_]
                                    (when on-save
                                      (on-save entity-name local-config)))}
              "Save Changes")))

        ($ field-list-editor
          {:label "Create Fields"
           :fields (:create-fields local-config)
           :available-fields all-fields
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :create-fields new-fields)))})

        ($ field-list-editor
          {:label "Edit Fields"
           :fields (:edit-fields local-config)
           :available-fields all-fields
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :edit-fields new-fields)))})

        ($ field-list-editor
          {:label "Required Fields"
           :fields (:required-fields local-config)
           :available-fields (distinct (concat (:create-fields local-config) (:edit-fields local-config)))
           :editing? editing?
           :on-change (fn [new-fields]
                        (set-local-config! (assoc local-config :required-fields new-fields)))})))))

;; =============================================================================
;; Table Columns Editor Components
;; =============================================================================

(defui column-list-editor
  "Editable list of columns"
  [{:keys [label columns available-columns on-change editing? help-text]}]
  (let [[local-columns set-local-columns!] (use-state (set columns))]
    (use-effect
      (fn []
        (set-local-columns! (set columns))
        js/undefined)
      [columns])
    ($ :div {:class "mb-4"}
      ($ :label {:class "text-sm font-medium mb-1 block"} label)
      (when help-text
        ($ :p {:class "text-xs text-base-content/60 mb-2"} help-text))
      ($ :div {:class "flex flex-wrap gap-2"}
        (for [col available-columns]
          (let [is-selected? (contains? local-columns col)]
            ($ :button {:key (name col)
                        :type "button"
                        :class (str "ds-badge ds-badge-lg cursor-pointer transition-all "
                                 (if is-selected?
                                   "ds-badge-secondary"
                                   "ds-badge-outline ds-badge-ghost")
                                 (when-not editing? " opacity-60 cursor-not-allowed"))
                        :disabled (not editing?)
                        :on-click (fn [_]
                                    (when editing?
                                      (let [new-cols (if is-selected?
                                                       (disj local-columns col)
                                                       (conj local-columns col))]
                                        (set-local-columns! new-cols)
                                        (when on-change
                                          (on-change (vec new-cols))))))}
              (name col))))))))

(defui table-columns-entity-editor
  "Editor for a single entity's table columns configuration"
  [{:keys [entity-name config editing? on-save]}]
  (let [available-columns (or (:available-columns config) [])
        [local-config set-local-config!] (use-state config)
        has-changes? (not= local-config config)]

    (use-effect
      (fn []
        (set-local-config! config)
        js/undefined)
      [config])

    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (-> entity-name name str/capitalize))
          (when (and editing? has-changes?)
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-primary ds-btn-sm"
                        :on-click (fn [_]
                                    (when on-save
                                      (on-save entity-name local-config)))}
              "Save Changes")))

        ;; Available columns (read-only reference)
        ($ :div {:class "mb-4"}
          ($ :label {:class "text-sm font-medium mb-2 block"} "Available Columns")
          ($ :div {:class "flex flex-wrap gap-1"}
            (for [col available-columns]
              ($ :span {:key (name col)
                        :class "ds-badge ds-badge-sm ds-badge-outline"}
                (name col)))))

        ($ column-list-editor
          {:label "Default Hidden"
           :columns (:default-hidden-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns hidden by default (users can show them)"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :default-hidden-columns new-cols)))})

        ($ column-list-editor
          {:label "Always Visible"
           :columns (:always-visible local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that cannot be hidden"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :always-visible new-cols)))})

        ($ column-list-editor
          {:label "Unfilterable"
           :columns (:unfilterable-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that cannot be filtered"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :unfilterable-columns new-cols)))})

        ($ column-list-editor
          {:label "Unsortable"
           :columns (:unsortable-columns local-config)
           :available-columns available-columns
           :editing? editing?
           :help-text "Columns that cannot be sorted"
           :on-change (fn [new-cols]
                        (set-local-config! (assoc local-config :unsortable-columns new-cols)))})))))

;; =============================================================================
;; Main Content Tabs
;; =============================================================================

(defui view-options-tab-content
  "Content for the view options tab"
  [{:keys [all-view-options editing? on-change active-domain-tab set-domain-tab!]}]
  (let [sorted-entities (sort-by first all-view-options)
        grouped-entities (group-entities-by-domain sorted-entities)]
    ($ :div
      ;; Domain tabs
      ($ :div {:class "ds-tabs ds-tabs-bordered mb-6"}
        (tabs/tab-link {:label "🏠 System"
                        :active? (= active-domain-tab "system")
                        :on-select #(set-domain-tab! "system")})
        (tabs/tab-link {:label "💼 Domain"
                        :active? (= active-domain-tab "domain")
                        :on-select #(set-domain-tab! "domain")})
        (when (get grouped-entities :other)
          (tabs/tab-link {:label "📦 Other"
                          :active? (= active-domain-tab "other")
                          :on-select #(set-domain-tab! "other")})))

      ;; Tab content
      (cond
        (= active-domain-tab "system")
        ($ :div {:class "space-y-8"}
          (when-let [entities (get grouped-entities :user-management)]
            ($ domain-section {:domain-key :user-management
                               :domain-config (get domain-groups :user-management)
                               :entities entities
                               :editing? editing?
                               :on-change on-change
                               :show-actions? true}))
          (when-let [entities (get grouped-entities :security-audit)]
            ($ domain-section {:domain-key :security-audit
                               :domain-config (get domain-groups :security-audit)
                               :entities entities
                               :editing? editing?
                               :on-change on-change
                               :show-actions? true})))

        (= active-domain-tab "domain")
        ($ :div {:class "space-y-8"}
          (when-let [entities (get grouped-entities :expenses)]
            ($ domain-section {:domain-key :expenses
                               :domain-config (get domain-groups :expenses)
                               :entities entities
                               :editing? editing?
                               :on-change on-change
                               :show-actions? true})))

        (= active-domain-tab "other")
        (when-let [entities (get grouped-entities :other)]
          ($ :div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"}
            (for [[entity-name settings] entities]
              ($ entity-settings-card {:key entity-name
                                       :entity-name entity-name
                                       :settings settings
                                       :editing? editing?
                                       :on-change on-change
                                       :setting-keys all-setting-keys}))))))))

(defui form-fields-tab-content
  "Content for the form fields tab"
  [{:keys [form-fields editing? on-save loading?]}]
  (let [sorted-entities (sort-by first form-fields)]
    ($ :div
      (when loading?
        ($ :div {:class "flex items-center justify-center py-8"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

      (when-not loading?
        ($ :div
          ($ :div {:class "ds-alert ds-alert-info mb-6"}
            ($ :p {:class "text-sm"}
              "Configure which fields appear in create and edit forms for each entity. "
              "Required fields must be filled before submission."))

          ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
            (for [[entity-name config] sorted-entities]
              ($ form-fields-entity-editor {:key entity-name
                                            :entity-name entity-name
                                            :config config
                                            :editing? editing?
                                            :on-save on-save}))))))))

(defui table-columns-tab-content
  "Content for the table columns tab"
  [{:keys [table-columns editing? on-save loading?]}]
  (let [sorted-entities (sort-by first table-columns)]
    ($ :div
      (when loading?
        ($ :div {:class "flex items-center justify-center py-8"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

      (when-not loading?
        ($ :div
          ($ :div {:class "ds-alert ds-alert-info mb-6"}
            ($ :p {:class "text-sm"}
              "Configure table column visibility and behavior. Hidden columns can still be shown by users. "
              "Always visible columns cannot be hidden. Unfilterable/unsortable columns have those features disabled."))

          ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
            (for [[entity-name config] sorted-entities]
              ($ table-columns-entity-editor {:key entity-name
                                              :entity-name entity-name
                                              :config config
                                              :editing? editing?
                                              :on-save on-save}))))))))

;; =============================================================================
;; Main Admin Settings Content
;; =============================================================================

(defui admin-settings-content
  "Main content for the settings overview page"
  []
  (let [;; View options state
        editable-view-options (use-subscribe [::settings-events/editable-view-options])
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

        ;; Local state
        [domain-tab set-domain-tab!] (use-state "system")
        render-main-tab (fn [label key]
                          (tabs/tab-link {:label label
                                          :active? (= config-tab key)
                                          :on-select #(rf/dispatch [::settings-events/set-config-tab key])}))

        handle-toggle-edit (fn [e]
                             (when e (.preventDefault e))
                             (rf/dispatch [::settings-events/toggle-editing]))

        handle-view-option-change (fn [entity-name setting-key new-value]
                                    (if (nil? new-value)
                                      (rf/dispatch [::settings-events/remove-entity-setting
                                                    (name entity-name)
                                                    (name setting-key)])
                                      (rf/dispatch [::settings-events/update-entity-setting
                                                    (name entity-name)
                                                    (name setting-key)
                                                    new-value])))

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

          ;; Edit toggle button
          ($ :button {:type "button"
                      :class (str "ds-btn ds-btn-sm "
                               (if editing? "ds-btn-warning" "ds-btn-primary")
                               (when (or loading? saving?) " ds-btn-disabled"))
                      :on-click handle-toggle-edit
                      :disabled (or loading? saving?)}
            (if saving?
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
              (if editing?
                "Stop Editing"
                "Edit Settings"))))

        ;; Edit mode instructions
        (when editing?
          ($ :div {:class "ds-alert ds-alert-warning mb-6"}
            ($ :div
              ($ :h4 {:class "font-bold"} "Edit Mode Active")
              ($ :p {:class "text-sm"}
                (case config-tab
                  "view-options" "Click on any setting to cycle through: Enabled → Disabled → Remove. Changes are saved immediately."
                  "form-fields" "Click fields to toggle them in each list. Click 'Save Changes' to persist."
                  "table-columns" "Click columns to toggle them in each list. Click 'Save Changes' to persist."
                  "Changes are saved immediately.")))))

        ;; Main tab content rendering
        (cond
          (= config-tab "view-options")
          ($ view-options-tab-content
            {:all-view-options all-view-options
             :editing? editing?
             :on-change handle-view-option-change
             :active-domain-tab domain-tab
             :set-domain-tab! set-domain-tab!})

          (= config-tab "form-fields")
          ($ form-fields-tab-content
            {:form-fields form-fields
             :editing? editing?
             :on-save handle-form-fields-save
             :loading? form-fields-loading?})

          (= config-tab "table-columns")
          ($ table-columns-tab-content
            {:table-columns table-columns
             :editing? editing?
             :on-save handle-table-columns-save
             :loading? table-columns-loading?}))))))

(defui admin-settings-page
  "Admin settings overview page"
  []
  ($ layout/admin-layout
    ($ admin-settings-content)))
