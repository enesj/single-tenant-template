(ns app.admin.frontend.pages.unified-settings.page
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.settings-shell :as shell]
    [app.admin.frontend.components.settings-views.cards :as cards]
    [app.admin.frontend.events.settings :as admin-settings-events]
    [app.admin.frontend.events.unified-settings :as unified-events]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.admin.frontend.pages.unified-settings.editors :as editors]
    [app.admin.frontend.pages.unified-settings.view-mode :as view-mode]
    [app.admin.frontend.settings.definitions :as defs]
    [app.shared.model-naming :as model-naming]
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as timbre]
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
        on-admin-list-config-change (fn [entity-name setting-key value]
                                      (rf/dispatch [::admin-settings-events/set-list-config-setting-draft
                                                    entity-name setting-key value]))
        on-admin-action-gate-change (fn [entity-name action-key gate-id]
                                      (rf/dispatch [::admin-settings-events/set-action-gate-draft
                                                    entity-name action-key gate-id]))
        normalize-form-field-id (fn [x]
                                  (some-> x model-naming/ensure-app-keyword))
        form-field->storage (fn [x]
                              (some-> x normalize-form-field-id model-naming/app-keyword->db name))
        normalize-form-field-list (fn [xs]
                                    (->> (or xs [])
                                      (keep form-field->storage)
                                      distinct
                                      vec))
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
        on-user-list-config-change (fn [entity-kw setting-key value]
                                     (rf/dispatch [::user-settings-events/set-list-config-setting-draft
                                                   entity-kw setting-key value]))
        on-user-action-gate-change (fn [entity-kw action-key gate-id]
                                     (rf/dispatch [::user-settings-events/set-action-gate-draft
                                                   entity-kw action-key gate-id]))
        on-user-reset (fn [entity-kw]
                        (rf/dispatch [::user-settings-events/reset-entity-display-draft entity-kw]))
        ;; User form-fields/table-columns handlers
        on-user-form-field-toggle (fn [entity-kw field-type field-name]
                                    (rf/dispatch [::user-settings-events/toggle-form-field-draft
                                                  entity-kw field-type field-name]))
        on-user-form-fields-reset (fn [entity-kw]
                                    (rf/dispatch [::user-settings-events/reset-form-fields-draft entity-kw]))
        on-user-table-column-toggle (fn [entity-kw list-type col-name]
                                      (rf/dispatch [::user-settings-events/toggle-table-column-in-list-draft
                                                    entity-kw list-type col-name]))
        on-user-table-columns-reset (fn [entity-kw]
                                      (rf/dispatch [::user-settings-events/reset-columns-draft entity-kw]))
        on-user-table-column-label-set (fn [entity-kw col-name label]
                                         (rf/dispatch [::user-settings-events/set-table-column-label-draft
                                                       entity-kw col-name label]))]
    (if-not selected-entity
      ($ :div {:class "ds-alert ds-alert-info"}
        ($ :span "Select an entity to edit its settings."))
      (case scope
        :admin
        (let [table-columns-config (or admin-table-columns {})
              form-fields-config (or admin-form-fields {})
              settings (get admin-config selected-entity)]
          ($ :div {:class "max-w-4xl"}
            ;; Tab bar for admin scope
            ($ editors/config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "form-fields"
              ($ editors/form-fields-editor
                {:entity-kw selected-entity
                 :form-fields-config form-fields-config
                 :on-toggle (fn [entity-kw field-type field-name]
                              ;; Admin form-fields use immediate save via PATCH.
                              ;; Normalize snake_case and kebab-case ids to one canonical field.
                              (let [current-config (get form-fields-config entity-kw {})
                                    current-fields (normalize-form-field-list (get current-config field-type))
                                    field-str (form-field->storage field-name)
                                    current-field-set (set current-fields)
                                    new-fields (if (contains? current-field-set field-str)
                                                 (vec (remove #{field-str} current-fields))
                                                 (conj current-fields field-str))
                                    new-config (assoc current-config field-type new-fields)]
                                (rf/dispatch [::admin-settings-events/update-form-fields-entity
                                              entity-kw new-config])))})

              "table-columns"
              ($ :<>
                ($ cards/columns-policy-card
                  {:entity-kw selected-entity
                   :table-config (get table-columns-config selected-entity)
                   :column-defaults (:column-defaults settings)
                   :column-locks (:column-locks settings)
                   :editing? true
                   :lock-style :admin
                   :on-column-change on-admin-column-change
                   :on-column-visibility-bulk on-admin-column-visibility-bulk})
                ($ editors/table-columns-editor
                  {:entity-kw selected-entity
                   :table-columns-config table-columns-config
                   :on-toggle (fn [entity-kw list-type col-name]
                                ;; Admin table-columns use immediate save via PATCH
                                (let [col->str (fn [c]
                                                 (cond
                                                   (nil? c) nil
                                                   (keyword? c) (name c)
                                                   (string? c) c
                                                   :else (str c)))
                                      cleanup-available (fn [cfg]
                                                          (let [avail-set (set (map col->str (or (:available-columns cfg) [])))
                                                                prune (fn [xs]
                                                                        (->> (or xs [])
                                                                          (map col->str)
                                                                          (filter avail-set)
                                                                          distinct
                                                                          vec))
                                                                prune-map (fn [m]
                                                                            (reduce-kv
                                                                              (fn [acc k v]
                                                                                (if (contains? avail-set (col->str k))
                                                                                  (assoc acc k v)
                                                                                  acc))
                                                                              {}
                                                                              (or m {})))]
                                                            (-> cfg
                                                              (update :always-visible prune)
                                                              (update :default-visible-columns prune)
                                                              (update :filterable-columns prune)
                                                              (update :sortable-columns prune)
                                                              (update :computed-fields prune-map)
                                                              (update :column-config prune-map)
                                                              (update :column-metadata prune-map))))
                                      current-config (get table-columns-config entity-kw {})
                                      current-cols (vec (map col->str (or (get current-config list-type) [])))
                                      col-set (set current-cols)
                                      col-str (col->str col-name)
                                      new-cols (if (contains? col-set col-str)
                                                 (vec (remove #{col-str} current-cols))
                                                 (conj current-cols col-str))
                                      new-config (cond-> (assoc current-config list-type new-cols)
                                                   (= list-type :available-columns) cleanup-available)]
                                  (rf/dispatch [::admin-settings-events/update-table-columns-entity
                                                entity-kw new-config])))
                   :on-set-list (fn [entity-kw list-type cols]
                                  (let [col->str (fn [c]
                                                   (cond
                                                     (nil? c) nil
                                                     (keyword? c) (name c)
                                                     (string? c) c
                                                     :else (str c)))
                                        cleanup-available (fn [cfg]
                                                            (let [avail-set (set (map col->str (or (:available-columns cfg) [])))
                                                                  prune (fn [xs]
                                                                          (->> (or xs [])
                                                                            (map col->str)
                                                                            (filter avail-set)
                                                                            distinct
                                                                            vec))
                                                                  prune-map (fn [m]
                                                                              (reduce-kv
                                                                                (fn [acc k v]
                                                                                  (if (contains? avail-set (col->str k))
                                                                                    (assoc acc k v)
                                                                                    acc))
                                                                                {}
                                                                                (or m {})))]
                                                              (-> cfg
                                                                (update :always-visible prune)
                                                                (update :default-visible-columns prune)
                                                                (update :filterable-columns prune)
                                                                (update :sortable-columns prune)
                                                                (update :computed-fields prune-map)
                                                                (update :column-config prune-map)
                                                                (update :column-metadata prune-map))))
                                        current-config (get table-columns-config entity-kw {})
                                        cols' (->> (or cols [])
                                                (map col->str)
                                                (remove nil?)
                                                vec)
                                        new-config (cond-> (assoc current-config list-type cols')
                                                     (= list-type :available-columns) cleanup-available)]
                                    (rf/dispatch [::admin-settings-events/update-table-columns-entity
                                                  entity-kw new-config])))
                   :on-set-label (fn [entity-kw col-name label]
                                   (let [col->str (fn [c]
                                                    (cond
                                                      (nil? c) nil
                                                      (keyword? c) (name c)
                                                      (string? c) c
                                                      :else (str c)))
                                         col-key-variants (fn [column-name]
                                                            (let [col (col->str column-name)
                                                                  kebab (some-> col (str/replace "_" "-"))
                                                                  snake (some-> col (str/replace "-" "_"))]
                                                              (->> [col
                                                                    kebab
                                                                    snake
                                                                    (some-> col keyword)
                                                                    (some-> kebab keyword)
                                                                    (some-> snake keyword)]
                                                                (remove nil?)
                                                                distinct
                                                                vec)))
                                         normalized-label (some-> label str str/trim)
                                         current-config (get table-columns-config entity-kw {})
                                         metadata (or (:column-metadata current-config) {})
                                         current-entry (some (fn [k]
                                                               (let [sentinel ::not-found
                                                                     value (get metadata k sentinel)]
                                                                 (when-not (= value sentinel) value)))
                                                         (col-key-variants col-name))
                                         cleaned-metadata (reduce dissoc metadata (col-key-variants col-name))
                                         preserved-entry (some-> (or current-entry {}) (dissoc :label))
                                         new-metadata (if (str/blank? (or normalized-label ""))
                                                        (cond-> cleaned-metadata
                                                          (seq preserved-entry) (assoc (col->str col-name) preserved-entry))
                                                        (assoc cleaned-metadata (col->str col-name) (assoc (or current-entry {}) :label normalized-label)))
                                         new-config (cond-> (assoc current-config :column-metadata new-metadata)
                                                      (empty? new-metadata) (dissoc :column-metadata))]
                                     (rf/dispatch [::admin-settings-events/update-table-columns-entity
                                                   entity-kw new-config])))}))

              ;; default: view-options
              ($ editors/admin-entity-editor
                {:entity-kw selected-entity
                 :settings settings
                 :on-change on-admin-change
                 :on-display-settings-bulk on-admin-display-settings-bulk
                 :on-list-config-change on-admin-list-config-change
                 :on-action-gate-change on-admin-action-gate-change}))))

        :user
        (let [view-options (get-in user-draft [:view-options selected-entity])
              entity-config (get-in user-draft [:entities selected-entity])
              table-config (get-in user-draft [:table-columns selected-entity])
              form-fields-config (:form-fields user-draft)
              table-columns-config (:table-columns user-draft)]
          ($ :div {:class "max-w-4xl"}
            ;; Tab bar for user scope
            ($ editors/config-tabs {:tab tab :on-tab-change on-tab-change})

            ;; Tab content
            (case tab
              "form-fields"
              ($ editors/form-fields-editor
                {:entity-kw selected-entity
                 :form-fields-config form-fields-config
                 :on-toggle on-user-form-field-toggle
                 :on-reset on-user-form-fields-reset})

              "table-columns"
              ($ :<>
                ($ cards/columns-policy-card
                  {:entity-kw selected-entity
                   :table-config table-config
                   :column-defaults (:column-defaults view-options)
                   :column-locks (:column-locks view-options)
                   :editing? true
                   :lock-style :user
                   :on-column-change on-user-column-change
                   :on-column-visibility-bulk on-user-column-visibility-bulk})
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
                                                  entity-kw list-type cols'])))
                   :on-set-label on-user-table-column-label-set}))

              ;; default: view-options
              ($ editors/user-entity-editor
                {:entity-kw selected-entity
                 :view-options view-options
                 :entity-config entity-config
                 :admin-view-options (get admin-config selected-entity)
                 :on-change on-user-change
                 :on-display-settings-bulk on-user-display-settings-bulk
                 :on-list-config-change on-user-list-config-change
                 :on-action-gate-change on-user-action-gate-change
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
                                             :load-admin? true
                                             :load-user? (= page-scope :user)}])
        js/undefined)
      [page-scope])

    ($ shell/settings-shell
      {:page-title (case page-scope
                     :user "User Settings"
                     "Admin Settings")
       :page-description (case page-scope
                           :user "Manage domain-owned defaults and locks for user-facing pages"
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
