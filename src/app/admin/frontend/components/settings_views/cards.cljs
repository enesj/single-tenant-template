(ns app.admin.frontend.components.settings-views.cards
  (:require
    [app.admin.frontend.components.settings-views.rows :as rows]
    [app.admin.frontend.components.settings-views.utils :as utils]
    [app.admin.frontend.settings.definitions :as defs]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Entity Settings Card (Admin Style)
;; =============================================================================

(defui columns-policy-card
  "Card for column visibility policy (defaults + locks).

  Props:
  - :entity-kw
  - :table-config (from table-columns.edn)
  - :column-defaults map
  - :column-locks map
  - :editing? boolean
  - :lock-style :admin | :user
  - :on-column-change fn [entity-kw column-key new-state]
  - :on-column-visibility-bulk fn [entity-kw column-keys new-state]"
  [{:keys [entity-kw table-config column-defaults column-locks editing? lock-style
           on-column-change on-column-visibility-bulk]}]
  (let [lock-style (or lock-style :user)
        editing? (boolean editing?)
        col-defaults (or column-defaults {})
        col-locks (or column-locks {})
        available-cols (->> (or (:available-columns table-config) [])
                         (map (fn [k]
                                (cond
                                  (keyword? k) k
                                  (string? k) (keyword k)
                                  :else (keyword (str k)))))
                         vec)
        always-visible (set (map (fn [k]
                                   (cond
                                     (keyword? k) k
                                     (string? k) (keyword k)
                                     :else (keyword (str k))))
                              (or (:always-visible table-config) [])))
        enforced-cols (->> available-cols (filter always-visible) vec)
        policy-cols (->> available-cols (remove always-visible) vec)
        ;; Policy maps may contain always-visible keys (historical/manual edits).
        ;; They are redundant because always-visible is enforced at a lower layer.
        policy-col-defaults (apply dissoc col-defaults always-visible)
        policy-col-locks (apply dissoc col-locks always-visible)
        col-metadata (or (:column-metadata table-config) {})]
    ($ :div {:class "ds-card bg-base-100 shadow-md hover:shadow-lg transition-shadow"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-2"}
          ($ :h3 {:class "ds-card-title text-lg"}
            "Columns")
          ($ :div {:class "flex items-center gap-2"}
            ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
              (str (count policy-col-defaults) " defaults"))
            ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
              (str (count policy-col-locks) " locks"))
            (when (seq enforced-cols)
              ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"}
                (str (count enforced-cols) " enforced")))))

        ($ :div {:class "text-xs text-base-content/60 mb-4"}
          ($ :div
            "Policy controls visibility defaults/locks (from "
            ($ :span {:class "font-mono"} "view-options.edn")
            "). Structural column behavior lives in "
            ($ :span {:class "font-mono"} "table-columns.edn")
            "."))

        (if-not (seq available-cols)
          ($ :div {:class "ds-alert ds-alert-info"}
            ($ :span (str "No columns configured for " (defs/entity-title entity-kw) ".")))
          ($ :div {:class "grid grid-cols-1 gap-2"}
            (when (seq enforced-cols)
              ($ :div {:class "mb-3 text-xs text-base-content/60"}
                "Some columns are marked as "
                ($ :span {:class "font-semibold"} "always visible")
                " in "
                ($ :span {:class "font-mono"} "table-columns.edn")
                ". They are enforced and cannot be changed here."))

            (when (and editing? (seq policy-cols) (fn? on-column-visibility-bulk))
              (let [bulk-default (utils/uniform-or-mixed
                                   (map (fn [c]
                                          (if (contains? col-defaults c) (get col-defaults c) nil))
                                     policy-cols))
                    bulk-lock (utils/uniform-or-mixed
                                (map (fn [c]
                                       (if (contains? col-locks c) (get col-locks c) nil))
                                  policy-cols))]
                ($ rows/bulk-tristate-row
                  {:label "All columns"
                   :default-val bulk-default
                   :lock-val bulk-lock
                   :editing? true
                   :lock-style lock-style
                   :help-text "Apply Default/Lock visibility to all configurable columns (always-visible columns are excluded)."
                   :on-default-click (fn []
                                      ;; cycle the current aggregate state
                                       (let [current (if (= bulk-default :mixed) nil bulk-default)
                                             next-val (utils/next-tristate current)
                                             next-state (if (nil? next-val)
                                                          {:kind :inherit}
                                                          {:kind :default :value next-val})]
                                         (on-column-visibility-bulk entity-kw policy-cols next-state)))
                   :on-lock-click (fn []
                                   ;; cycle the current aggregate lock state
                                    (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                          next-val (utils/next-tristate current)
                                          next-state (if (nil? next-val)
                                                       {:kind :inherit}
                                                       {:kind :lock :value next-val})]
                                      (on-column-visibility-bulk entity-kw policy-cols next-state)))})))

            (when (seq enforced-cols)
              (for [col enforced-cols
                    :when col]
                (let [label (or (get-in col-metadata [col :label])
                              (-> col name (str/replace #"[_-]" " ") str/capitalize))
                      tip "This column is always visible (enforced by table-columns.edn)."]
                  ($ :div {:key (str (name entity-kw) "-col-enforced-" (name col))
                           :class "ds-tooltip ds-tooltip-top w-full"
                           :data-tip tip}
                    ($ :div {:class "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full"}
                      ($ :span {:class "text-sm font-medium min-w-[160px]"} label)
                      ($ :span {:class "ds-badge ds-badge-success ds-badge-sm"} "Always visible")
                      ($ :span {:class "text-xs text-base-content/50 ml-auto"} "configured in table-columns"))))))

            (for [col policy-cols
                  :when col]
              (let [default-val (when (contains? col-defaults col) (get col-defaults col))
                    lock-val (when (contains? col-locks col) (get col-locks col))
                    label (or (get-in col-metadata [col :label])
                            (-> col name (str/replace #"[_-]" " ") str/capitalize))
                    tip (str "Controls the default and lock visibility for the '" label "' column.")]
                ($ rows/column-visibility-row
                  {:key (str (name entity-kw) "-col-" (name col))
                   :entity-kw entity-kw
                   :column-key col
                   :column-label label
                   :default-val default-val
                   :lock-val lock-val
                   :lock-style lock-style
                   :editing? editing?
                   :help-text tip
                   :on-change on-column-change})))))))))

(defui list-behavior-card
  "Card for declarative list behavior stored in view-options :list-config."
  [{:keys [entity-kw list-config editing? on-setting-change on-action-gate-change]}]
  (let [editing? (boolean editing?)
        list-config (or list-config {})
        action-gates (or (:action-gates list-config) {})]
    ($ :div {:class "ds-card bg-base-100 shadow-md hover:shadow-lg transition-shadow"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-2"}
          ($ :h3 {:class "ds-card-title text-lg"} "List Behavior")
          ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
            (str (count (remove nil? (vals action-gates))) " gates")))

        ($ :div {:class "text-xs text-base-content/60 mb-4"}
          "These options replace page-level list props such as modal mode, disallowed action mode, and runtime capability gates.")

        ($ :div {:class "grid grid-cols-1 gap-2"}
          ($ rows/enum-setting-row
            {:entity-kw entity-kw
             :setting-key :form-display
             :value (:form-display list-config)
             :editing? editing?
             :options (get defs/list-config-select-options :form-display)
             :help-text "Controls whether add/edit forms render inline or in a modal."
             :on-change on-setting-change})
          ($ rows/enum-setting-row
            {:entity-kw entity-kw
             :setting-key :disallowed-action-mode
             :value (:disallowed-action-mode list-config)
             :editing? editing?
             :options (get defs/list-config-select-options :disallowed-action-mode)
             :help-text "Controls whether runtime-disallowed actions are hidden or shown disabled."
             :on-change on-setting-change})
          (for [action-key defs/action-gate-order]
            ($ rows/action-gate-row
              {:key (str (name entity-kw) "-gate-" (name action-key))
               :entity-kw entity-kw
               :action-key action-key
               :gate-id (get action-gates action-key)
               :editing? editing?
               :options defs/action-gate-options
               :on-change on-action-gate-change})))))))

(defui admin-entity-settings-card
  "Card displaying all hardcoded settings for a single entity (admin style).

   Props:
   - :entity-name - entity keyword
   - :settings - map with :display-locks etc.
   - :local-display-prefs - current browser's local overrides ([:ui :entity-prefs <entity> :display])
   - :on-clear-local-display-prefs - fn [entity-kw]
   - :editing? - whether in edit mode
   - :on-change - fn [entity-name setting-key new-value]
   - :setting-keys - which setting keys to show (default: display-setting-keys)"
  [{:keys [entity-name settings
           local-display-prefs on-clear-local-display-prefs
           editing? on-change on-display-settings-bulk
           setting-keys]}]
  (let [setting-keys (or setting-keys defs/display-setting-keys)
        defaults (or (:display-defaults settings) {})
        locks (or (:display-locks settings) {})
        local-display-prefs (or local-display-prefs {})
        local-overrides-count (count local-display-prefs)
        has-any-defaults? (seq (select-keys defaults setting-keys))
        has-any-locks? (seq (select-keys locks setting-keys))]
    ($ :div {:class "ds-card bg-base-100 shadow-md hover:shadow-lg transition-shadow"}
      ($ :div {:class "ds-card-body p-4"}
        ;; Entity header
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (defs/entity-title entity-name))
          ($ :div {:class "flex items-center gap-2 flex-wrap justify-end"}
            (when (and (seq local-display-prefs) (fn? on-clear-local-display-prefs))
              ($ :button {:type "button"
                          :id (str "btn-clear-local-display-prefs-" (name entity-name))
                          :class "ds-btn ds-btn-xs ds-btn-ghost"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (on-clear-local-display-prefs entity-name))}
                (str "Clear local overrides"
                  (when (pos? local-overrides-count)
                    (str " (" local-overrides-count ")")))))
            (if has-any-defaults?
              ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
                (str (count (select-keys defaults setting-keys)) " defaults"))
              ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "No defaults"))
            (if has-any-locks?
              ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                (str (count (select-keys locks setting-keys)) " locks"))
              ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "No locks"))))

        (when (seq local-display-prefs)
          ($ :div {:class "text-xs text-base-content/60 mb-2"}
            "This browser has local list display overrides for this entity; they can override Defaults until cleared."))

        ;; Settings grid
        ($ :div {:class "grid grid-cols-1 gap-2"}
          (when (and editing? (seq setting-keys) (fn? on-display-settings-bulk))
            (let [bulk-default (utils/uniform-or-mixed
                                 (map (fn [k]
                                        (if (contains? defaults k) (get defaults k) nil))
                                   setting-keys))
                  bulk-lock (utils/uniform-or-mixed
                              (map (fn [k]
                                     (if (contains? locks k) (get locks k) nil))
                                setting-keys))]
              ($ rows/bulk-tristate-row
                {:label "All toggles"
                 :default-val bulk-default
                 :lock-val bulk-lock
                 :editing? true
                 :lock-style :admin
                 :help-text "Apply Default/Lock to all display toggles for this entity."
                 :on-default-click (fn []
                                     ;; cycle the current aggregate state
                                     (let [current (if (= bulk-default :mixed) nil bulk-default)
                                           next-val (utils/next-tristate current)
                                           next-state (if (nil? next-val)
                                                        {:kind :inherit}
                                                        {:kind :default :value next-val})]
                                       (on-display-settings-bulk entity-name setting-keys next-state)))
                 :on-lock-click (fn []
                                  ;; cycle the current aggregate lock state
                                  (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                        next-val (utils/next-tristate current)
                                        next-state (if (nil? next-val)
                                                     {:kind :inherit}
                                                     {:kind :lock :value next-val})]
                                    (on-display-settings-bulk entity-name setting-keys next-state)))})))
          (for [setting-key setting-keys
                :when (not= setting-key :per-page)]
            (let [default-val (when (contains? defaults setting-key) (get defaults setting-key))
                  lock-val (when (contains? locks setting-key) (get locks setting-key))]
              ($ rows/display-setting-row
                {:key (str entity-name "-" setting-key)
                 :entity-kw entity-name
                 :setting-key setting-key
                 :default-val default-val
                 :lock-val lock-val
                 :lock-style :admin
                 :editing? editing?
                 :on-change on-change})))
          ;; Per-page setting (uses select instead of toggle)
          (let [per-page-default (get defaults :per-page)
                per-page-lock (get locks :per-page)]
            ($ rows/per-page-setting-row
              {:key (str entity-name "-per-page")
               :entity-kw entity-name
               :default-val per-page-default
               :lock-val per-page-lock
               :lock-style :admin
               :editing? editing?
               :on-change on-change})))))))

;; =============================================================================
;; Entity Settings Card (User Style)
;; =============================================================================

(defui user-entity-settings-card
  "Card displaying all settings for a single entity (user style with defaults/locks).

   Props:
   - :entity-kw - entity keyword
   - :entity-title - optional custom title
   - :draft-defaults - map of default settings
   - :draft-locks - map of locked settings
   - :immutable-locks - map of immutable (feature constraint) locks
   - :local-display-prefs - current browser's local overrides ([:ui :entity-prefs <entity> :display])
   - :on-clear-local-display-prefs - fn [entity-kw]
   - :on-change - fn [entity-kw setting-key new-state]
   - :on-display-settings-bulk - fn [entity-kw setting-keys new-state]
   - :on-reset - fn [entity-kw] - reset to saved values
   - :setting-keys - which setting keys to show"
  [{:keys [entity-kw entity-title draft-defaults draft-locks
           immutable-locks local-display-prefs on-clear-local-display-prefs
           on-change on-display-settings-bulk
           on-reset setting-keys editing?]}]
  (let [setting-keys (or setting-keys defs/all-setting-keys)
        editing? (boolean editing?)
        local-display-prefs (or local-display-prefs {})
        local-overrides-count (count local-display-prefs)]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (or entity-title (defs/entity-title entity-kw)))
          ($ :div {:class "flex items-center gap-2"}
            (when (and (seq local-display-prefs) (fn? on-clear-local-display-prefs))
              ($ :button {:type "button"
                          :id (str "btn-clear-local-display-prefs-" (name entity-kw))
                          :class "ds-btn ds-btn-xs ds-btn-ghost"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (on-clear-local-display-prefs entity-kw))}
                (str "Clear local overrides" (when (pos? local-overrides-count)
                                               (str " (" local-overrides-count ")")))))
            (when on-reset
              ($ :button {:type "button"
                          :id (str "btn-reset-user-settings-" (name entity-kw))
                          :class "ds-btn ds-btn-xs ds-btn-ghost"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (on-reset entity-kw))}
                "Reset"))))

        (when (seq local-display-prefs)
          ($ :div {:class "text-xs text-base-content/60 mb-2"}
            "This browser has local list display overrides for this entity; they can override Defaults until cleared."))

        ($ :div {:class "grid grid-cols-1 gap-2"}
          (let [defaults (or draft-defaults {})
                locks (or draft-locks {})
                immutable (or immutable-locks {})
                editable-setting-keys (->> setting-keys
                                        (remove (fn [k] (contains? immutable k)))
                                        vec)]
            ($ :<>
              (when (and editing? (seq editable-setting-keys) (fn? on-display-settings-bulk))
                (let [bulk-default (utils/uniform-or-mixed
                                     (map (fn [k]
                                            (if (contains? defaults k) (get defaults k) nil))
                                       editable-setting-keys))
                      bulk-lock (utils/uniform-or-mixed
                                  (map (fn [k]
                                         (if (contains? locks k) (get locks k) nil))
                                    editable-setting-keys))
                      help (if (seq immutable)
                             "Apply Default/Lock to all editable toggles for this entity (excludes enforced feature constraints)."
                             "Apply Default/Lock to all toggles for this entity.")]
                  ($ rows/bulk-tristate-row
                    {:label "All toggles"
                     :default-val bulk-default
                     :lock-val bulk-lock
                     :editing? true
                     :lock-style :user
                     :help-text help
                     :on-default-click (fn []
                                        ;; cycle the current aggregate state
                                         (let [current (if (= bulk-default :mixed) nil bulk-default)
                                               next-val (utils/next-tristate current)
                                               next-state (if (nil? next-val)
                                                            {:kind :inherit}
                                                            {:kind :default :value next-val})]
                                           (on-display-settings-bulk entity-kw editable-setting-keys next-state)))
                     :on-lock-click (fn []
                                     ;; cycle the current aggregate lock state
                                      (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                            next-val (utils/next-tristate current)
                                            next-state (if (nil? next-val)
                                                         {:kind :inherit}
                                                         {:kind :lock :value next-val})]
                                        (on-display-settings-bulk entity-kw editable-setting-keys next-state)))})))

              (for [setting-key setting-keys
                    :when (not= setting-key :per-page)]
                (let [default-val (when (contains? defaults setting-key) (get defaults setting-key))
                      lock-val (when (contains? locks setting-key) (get locks setting-key))
                      immutable? (contains? immutable setting-key)
                      immutable-val (get immutable setting-key)]
                  ($ rows/display-setting-row
                    {:key (str (name entity-kw) "-" (name setting-key))
                     :entity-kw entity-kw
                     :setting-key setting-key
                     :default-val default-val
                     :lock-val lock-val
                     :immutable? immutable?
                     :immutable-val immutable-val
                     :lock-style :user
                     :editing? editing?
                     :on-change on-change})))
              ;; Per-page setting (uses select instead of toggle)
              (let [per-page-default (get defaults :per-page)
                    per-page-lock (get locks :per-page)]
                ($ rows/per-page-setting-row
                  {:key (str (name entity-kw) "-per-page")
                   :entity-kw entity-kw
                   :default-val per-page-default
                   :lock-val per-page-lock
                   :lock-style :user
                   :editing? editing?
                   :on-change on-change})))))))))

