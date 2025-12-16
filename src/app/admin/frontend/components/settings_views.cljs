(ns app.admin.frontend.components.settings-views
  "Shared view and edit components for settings pages.
   
   Provides reusable components for:
   - Setting badges (display setting states)
   - Entity settings cards (group settings by entity)
   - Settings overview (read-only view of all settings)
   - Settings editor (edit settings for selected entity)"
  (:require
    [app.admin.frontend.settings.definitions :as defs]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Setting State Helpers
;; =============================================================================

(defn- next-tristate
  "Cycle nil → true → false → nil"
  [v]
  (cond
    (nil? v) true
    (true? v) false
    (false? v) nil
    :else nil))

(defn- default-badge-props
  "Badge for a default value (nil/true/false or :mixed)."
  [default-val]
  (cond
    (= default-val :mixed)
    {:class "ds-badge ds-badge-ghost ds-badge-sm" :text "Mixed"}

    (true? default-val)
    {:class "ds-badge ds-badge-success ds-badge-sm" :text "Default On"}

    (false? default-val)
    {:class "ds-badge ds-badge-error ds-badge-sm" :text "Default Off"}

    :else
    {:class "ds-badge ds-badge-ghost ds-badge-sm" :text "Inherit"}))

(defn- lock-badge-props
  "Badge for a lock value.

   Options:
   - lock-style :admin | :user
   - immutable? / immutable-val: when immutable, display as Enforced and disable controls"
  [{:keys [lock-val lock-style immutable? immutable-val]}]
  (let [lock-style (or lock-style :user)
        effective-lock (if immutable? immutable-val lock-val)]
    (cond
    (= effective-lock :mixed)
    {:class "ds-badge ds-badge-ghost ds-badge-sm"
     :text "Mixed"}

      immutable?
      {:class (str "ds-badge ds-badge-sm "
                (if (true? effective-lock) "ds-badge-success" "ds-badge-error"))
       :text (str "Enforced " (if (true? effective-lock) "On" "Off"))}

      (true? effective-lock)
      {:class "ds-badge ds-badge-success ds-badge-sm"
       :text (case lock-style
               :admin "Enabled"
               "Locked On")}

      (false? effective-lock)
      {:class "ds-badge ds-badge-error ds-badge-sm"
       :text (case lock-style
               :admin "Disabled"
               "Locked Off")}

      :else
      {:class "ds-badge ds-badge-ghost ds-badge-sm"
       :text (case lock-style
               :admin "Not set"
               "Inherit")})))

(defn- tristate-hint
  "Small helper text showing the next value in the cycle."
  [{:keys [kind current-val lock-style]}]
  (let [lock-style (or lock-style :user)
        next-val (next-tristate (if (= current-val :mixed) nil current-val))]
    (case kind
      :default
      (str "→ " (cond
                  (true? next-val) "Default On"
                  (false? next-val) "Default Off"
                  :else "Inherit"))

      :lock
      (str "→ " (cond
                  (true? next-val) (case lock-style :admin "Enabled" "Locked On")
                  (false? next-val) (case lock-style :admin "Disabled" "Locked Off")
                  :else (case lock-style :admin "Not set" "Inherit")))
      nil)))

(defn- uniform-or-mixed
  "If all values are the same, returns that value; otherwise returns :mixed.

  Values are expected to be one of nil/true/false." 
  [vals]
  (let [vals (vec vals)]
    (cond
      (empty? vals) nil
      (apply = vals) (first vals)
      :else :mixed)))

(defui bulk-tristate-row
  "A row with the same Default/Lock control UI, but applies to many keys.

  Props:
  - :label
  - :default-val, :lock-val (nil/true/false/:mixed)
  - :editing? boolean
  - :lock-style :admin|:user
  - :on-default-click fn []
  - :on-lock-click fn []
  - :help-text string"
  [{:keys [label default-val lock-val editing? lock-style on-default-click on-lock-click help-text]}]
  (let [lock-style (or lock-style :user)
        editing? (boolean editing?)
        clickable-default? (and editing? (fn? on-default-click))
        clickable-lock? (and editing? (fn? on-lock-click))
        {:keys [class text]} (default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (lock-badge-props {:lock-val lock-val :lock-style lock-style})
        lock-class class
        lock-text text]
    ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
             :data-tip (or help-text "Apply this to all items in this section.")}
      ($ :div {:class "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full"}
        ($ :span {:class "text-sm font-medium min-w-[160px]"}
          label)

        ;; Default control
        (if clickable-default?
          ($ :button
            {:type "button"
             :class default-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-default-click))}
            default-text)
          ($ :span {:class default-class} default-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50"}
            (tristate-hint {:kind :default :current-val default-val})))

        ($ :span {:class "mx-1 text-base-content/30"} "|")

        ;; Lock control
        (if clickable-lock?
          ($ :button
            {:type "button"
             :class lock-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-lock-click))}
            lock-text)
          ($ :span {:class lock-class} lock-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50 ml-auto"}
            (tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style})))))))

(defui display-setting-row
  "Render a single setting row with separate Default and Lock controls.

   Props:
   - :entity-kw
   - :setting-key
   - :default-val (nil/true/false)
   - :lock-val (nil/true/false)
   - :immutable? / :immutable-val (optional)
   - :editing? boolean
   - :lock-style :admin | :user
   - :on-change fn [entity-kw setting-key new-state] where new-state is {:kind :inherit} | {:kind :default :value bool} | {:kind :lock :value bool}"
  [{:keys [entity-kw setting-key default-val lock-val immutable? immutable-val editing? lock-style on-change]}]
  (let [help-text (defs/setting-help setting-key)
        lock-style (or lock-style :user)
        editing? (boolean editing?)
        clickable? (and editing? (fn? on-change) (not immutable?))
        next-default (next-tristate default-val)
        next-lock (next-tristate lock-val)
        default-next-state (if (nil? next-default)
                             {:kind :inherit}
                             {:kind :default :value next-default})
        lock-next-state (if (nil? next-lock)
                          {:kind :inherit}
                          {:kind :lock :value next-lock})
        {:keys [class text]} (default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (lock-badge-props {:lock-val lock-val
                                                :lock-style lock-style
                                                :immutable? immutable?
                                                :immutable-val immutable-val})
        lock-class class
        lock-text text]
    ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
             :data-tip help-text}
      ($ :div {:class "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full"}
        ($ :span {:class "text-sm font-medium min-w-[120px]"}
          (defs/setting-label setting-key))

        ;; Default control
        (if clickable?
          ($ :button
            {:type "button"
             :class default-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-change entity-kw setting-key default-next-state))}
            default-text)
          ($ :span {:class default-class} default-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50"}
            (tristate-hint {:kind :default :current-val default-val})))

        ($ :span {:class "mx-1 text-base-content/30"} "|")

        ;; Lock control
        (if clickable?
          ($ :button
            {:type "button"
             :class lock-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-change entity-kw setting-key lock-next-state))}
            lock-text)
          ($ :span {:class lock-class} lock-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50 ml-auto"}
            (if immutable?
              ""
              (tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style}))))))))

;; =============================================================================
;; Column Visibility Policy Row
;; =============================================================================

(defui column-visibility-row
  "Render a single column visibility row with separate Default and Lock controls.

   Props:
   - :entity-kw
   - :column-key
   - :column-label
   - :default-val (nil/true/false)
   - :lock-val (nil/true/false)
   - :immutable? / :immutable-val (optional)
   - :editing? boolean
   - :lock-style :admin | :user
   - :help-text string
   - :on-change fn [entity-kw column-key new-state] where new-state is {:kind :inherit}|{:kind :default :value bool}|{:kind :lock :value bool}"
  [{:keys [entity-kw column-key column-label default-val lock-val immutable? immutable-val editing? lock-style help-text on-change]}]
  (let [lock-style (or lock-style :user)
        editing? (boolean editing?)
        clickable? (and editing? (fn? on-change) (not immutable?))
        next-default (next-tristate default-val)
        next-lock (next-tristate lock-val)
        default-next-state (if (nil? next-default)
                             {:kind :inherit}
                             {:kind :default :value next-default})
        lock-next-state (if (nil? next-lock)
                          {:kind :inherit}
                          {:kind :lock :value next-lock})
        {:keys [class text]} (default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (lock-badge-props {:lock-val lock-val
                                                :lock-style lock-style
                                                :immutable? immutable?
                                                :immutable-val immutable-val})
        lock-class class
        lock-text text
        tip (or help-text
              (str "Column visibility policy for " (name column-key)))]
    ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
             :data-tip tip}
      ($ :div {:class "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full"}
        ($ :span {:class "text-sm font-medium min-w-[160px]"}
          (or column-label (some-> column-key name str/capitalize)))

        ;; Default control
        (if clickable?
          ($ :button
            {:type "button"
             :class default-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-change entity-kw column-key default-next-state))}
            default-text)
          ($ :span {:class default-class} default-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50"}
            (tristate-hint {:kind :default :current-val default-val})))

        ($ :span {:class "mx-1 text-base-content/30"} "|")

        ;; Lock control
        (if clickable?
          ($ :button
            {:type "button"
             :class lock-class
             :on-click (fn [e]
                         (.preventDefault e)
                         (on-change entity-kw column-key lock-next-state))}
            lock-text)
          ($ :span {:class lock-class} lock-text))
        (when editing?
          ($ :span {:class "text-xs text-base-content/50 ml-auto"}
            (if immutable?
              ""
              (tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style}))))))))

;; =============================================================================
;; Admin-style Setting Badge (simple cycle: true -> false -> nil/remove)
;; =============================================================================

(defui admin-setting-badge
  "Badge showing a setting's status for admin settings - clickable to cycle.
   Cycles through: Enabled -> Disabled -> Not set (remove)
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
    ($ :div {:class (str "ds-tooltip ds-tooltip-top w-full")
             :data-tip help-text}
      ($ :div {:class (str "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full "
                        (when editing? "cursor-pointer hover:bg-base-300 transition-colors"))
               :on-click handle-click}
        ($ :span {:class "text-sm font-medium min-w-[120px]"}
          (defs/setting-label setting-key))
        (cond
          is-true?
          ($ :span {:class "ds-badge ds-badge-success ds-badge-sm"} "Enabled")

          is-false?
          ($ :span {:class "ds-badge ds-badge-error ds-badge-sm"} "Disabled")

          :else
          ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "Not set"))
        ;; Show edit hint when in edit mode
        (when editing?
          ($ :span {:class "text-xs text-base-content/50 ml-auto"}
            (cond
              is-true? "→ Disabled"
              is-false? "→ Remove"
              :else "→ Enabled")))))))

;; =============================================================================
;; Entity Settings Card (Admin Style)
;; =============================================================================

(defui admin-entity-settings-card
  "Card displaying all hardcoded settings for a single entity (admin style).
   
   Props:
   - :entity-name - entity keyword
   - :settings - map with :display-locks etc.
   - :editing? - whether in edit mode
   - :on-change - fn [entity-name setting-key new-value]
   - :setting-keys - which setting keys to show (default: display-setting-keys)"
  [{:keys [entity-name settings editing? on-change on-display-settings-bulk
           setting-keys table-config
           on-column-change on-column-visibility-bulk]}]
  (let [setting-keys (or setting-keys defs/display-setting-keys)
        defaults (or (:display-defaults settings) {})
        locks (or (:display-locks settings) {})
        has-any-defaults? (seq (select-keys defaults setting-keys))
        has-any-locks? (seq (select-keys locks setting-keys))
        col-defaults (or (:column-defaults settings) {})
        col-locks (or (:column-locks settings) {})
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
        ;; Entity header
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (defs/entity-title entity-name))
          ($ :div {:class "flex items-center gap-2"}
            (if has-any-defaults?
              ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
                (str (count (select-keys defaults setting-keys)) " defaults"))
              ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "No defaults"))
            (if has-any-locks?
              ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                (str (count (select-keys locks setting-keys)) " locks"))
              ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"} "No locks"))))

        ;; Settings grid
        ($ :div {:class "grid grid-cols-1 gap-2"}
          (when (and editing? (seq setting-keys) (fn? on-display-settings-bulk))
            (let [bulk-default (uniform-or-mixed
                                 (map (fn [k]
                                        (if (contains? defaults k) (get defaults k) nil))
                                   setting-keys))
                  bulk-lock (uniform-or-mixed
                              (map (fn [k]
                                     (if (contains? locks k) (get locks k) nil))
                                setting-keys))]
              ($ bulk-tristate-row
                {:label "All toggles"
                 :default-val bulk-default
                 :lock-val bulk-lock
                 :editing? true
                 :lock-style :admin
                 :help-text "Apply Default/Lock to all display toggles for this entity."
                 :on-default-click (fn []
                                    ;; cycle the current aggregate state
                                    (let [current (if (= bulk-default :mixed) nil bulk-default)
                                          next-val (next-tristate current)
                                          next-state (if (nil? next-val)
                                                      {:kind :inherit}
                                                      {:kind :default :value next-val})]
                                      (on-display-settings-bulk entity-name setting-keys next-state)))
                 :on-lock-click (fn []
                                 ;; cycle the current aggregate lock state
                                 (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                       next-val (next-tristate current)
                                       next-state (if (nil? next-val)
                                                   {:kind :inherit}
                                                   {:kind :lock :value next-val})]
                                   (on-display-settings-bulk entity-name setting-keys next-state)))})))
          (for [setting-key setting-keys]
            (let [default-val (when (contains? defaults setting-key) (get defaults setting-key))
                  lock-val (when (contains? locks setting-key) (get locks setting-key))]
              ($ display-setting-row
                {:key (str entity-name "-" setting-key)
                 :entity-kw entity-name
                 :setting-key setting-key
                 :default-val default-val
                 :lock-val lock-val
                 :lock-style :admin
                 :editing? editing?
                 :on-change on-change}))))

        (when (seq available-cols)
          ($ :div {:class "mt-4"}
            ($ :div {:class "flex items-center justify-between mb-2"}
              ($ :h4 {:class "text-sm font-semibold"} "Columns")
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
                  (str (count policy-col-defaults) " defaults"))
                ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                  (str (count policy-col-locks) " locks"))
                (when (seq enforced-cols)
                  ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"}
                    (str (count enforced-cols) " enforced")))))

            (when (seq enforced-cols)
              ($ :div {:class "mb-3 text-xs text-base-content/60"}
                "Some columns are marked as "
                ($ :span {:class "font-semibold"} "always visible")
                " in "
                ($ :span {:class "font-mono"} "table-columns.edn")
                ". They are enforced and cannot be changed here."))

            ($ :div {:class "grid grid-cols-1 gap-2"}
              (when (and editing? (seq policy-cols) (fn? on-column-visibility-bulk))
                (let [bulk-default (uniform-or-mixed
                                     (map (fn [c]
                                            (if (contains? col-defaults c) (get col-defaults c) nil))
                                       policy-cols))
                      bulk-lock (uniform-or-mixed
                                  (map (fn [c]
                                         (if (contains? col-locks c) (get col-locks c) nil))
                                    policy-cols))]
                  ($ bulk-tristate-row
                    {:label "All columns"
                     :default-val bulk-default
                     :lock-val bulk-lock
                     :editing? true
                     :lock-style :admin
                     :help-text "Apply Default/Lock visibility to all configurable columns (always-visible columns are excluded)."
                     :on-default-click (fn []
                                        ;; cycle the current aggregate state
                                        (let [current (if (= bulk-default :mixed) nil bulk-default)
                                              next-val (next-tristate current)
                                              next-state (if (nil? next-val)
                                                          {:kind :inherit}
                                                          {:kind :default :value next-val})]
                                          (on-column-visibility-bulk entity-name policy-cols next-state)))
                     :on-lock-click (fn []
                                     ;; cycle the current aggregate lock state
                                     (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                           next-val (next-tristate current)
                                           next-state (if (nil? next-val)
                                                       {:kind :inherit}
                                                       {:kind :lock :value next-val})]
                                       (on-column-visibility-bulk entity-name policy-cols next-state)))})))
              (when (seq enforced-cols)
                (for [col enforced-cols
                      :when col]
                  (let [label (or (get-in col-metadata [col :label])
                                (-> col name (str/replace #"[_-]" " ") str/capitalize))
                        tip (str "This column is always visible (enforced by table-columns.edn).")]
                    ($ :div {:key (str (name entity-name) "-col-enforced-" (name col))
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
                  ($ column-visibility-row
                    {:key (str (name entity-name) "-col-" (name col))
                     :entity-kw entity-name
                     :column-key col
                     :column-label label
                     :default-val default-val
                     :lock-val lock-val
                     :lock-style :admin
                     :editing? editing?
                     :help-text tip
                     :on-change on-column-change}))))))))))

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
   - :on-change - fn [entity-kw setting-key new-state]
     - :on-display-settings-bulk - fn [entity-kw setting-keys new-state]
   - :on-reset - fn [entity-kw] - reset to saved values
   - :setting-keys - which setting keys to show"
  [{:keys [entity-kw entity-title draft-defaults draft-locks draft-column-defaults draft-column-locks
       immutable-locks on-change on-display-settings-bulk on-column-visibility-bulk
      on-reset setting-keys editing? table-config on-column-change]}]
  (let [setting-keys (or setting-keys defs/all-setting-keys)
        editing? (boolean editing?)
        col-defaults (or draft-column-defaults {})
        col-locks (or draft-column-locks {})
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
        policy-col-defaults (apply dissoc col-defaults always-visible)
        policy-col-locks (apply dissoc col-locks always-visible)
        col-metadata (or (:column-metadata table-config) {})]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"}
            (or entity-title (defs/entity-title entity-kw)))
          (when on-reset
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-xs ds-btn-ghost"
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (on-reset entity-kw))}
              "Reset")))
        ($ :div {:class "grid grid-cols-1 gap-2"}
          (let [defaults (or draft-defaults {})
                locks (or draft-locks {})
                immutable (or immutable-locks {})
                editable-setting-keys (->> setting-keys
                                        (remove (fn [k] (contains? immutable k)))
                                        vec)]
            (when (and editing? (seq editable-setting-keys) (fn? on-display-settings-bulk))
              (let [bulk-default (uniform-or-mixed
                                   (map (fn [k]
                                          (if (contains? defaults k) (get defaults k) nil))
                                     editable-setting-keys))
                    bulk-lock (uniform-or-mixed
                                (map (fn [k]
                                       (if (contains? locks k) (get locks k) nil))
                                  editable-setting-keys))
                    help (if (seq immutable)
                           "Apply Default/Lock to all editable toggles for this entity (excludes enforced feature constraints)."
                           "Apply Default/Lock to all toggles for this entity.")]
                ($ bulk-tristate-row
                  {:label "All toggles"
                   :default-val bulk-default
                   :lock-val bulk-lock
                   :editing? true
                   :lock-style :user
                   :help-text help
                   :on-default-click (fn []
                                      ;; cycle the current aggregate state
                                      (let [current (if (= bulk-default :mixed) nil bulk-default)
                                            next-val (next-tristate current)
                                            next-state (if (nil? next-val)
                                                        {:kind :inherit}
                                                        {:kind :default :value next-val})]
                                        (on-display-settings-bulk entity-kw editable-setting-keys next-state)))
                   :on-lock-click (fn []
                                   ;; cycle the current aggregate lock state
                                   (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                         next-val (next-tristate current)
                                         next-state (if (nil? next-val)
                                                     {:kind :inherit}
                                                     {:kind :lock :value next-val})]
                                     (on-display-settings-bulk entity-kw editable-setting-keys next-state)))})))

            (for [setting-key setting-keys]
              (let [default-val (when (contains? defaults setting-key) (get defaults setting-key))
                    lock-val (when (contains? locks setting-key) (get locks setting-key))
                    immutable? (contains? immutable setting-key)
                    immutable-val (get immutable setting-key)]
                ($ display-setting-row
                  {:key (str (name entity-kw) "-" (name setting-key))
                   :entity-kw entity-kw
                   :setting-key setting-key
                   :default-val default-val
                   :lock-val lock-val
                   :immutable? immutable?
                   :immutable-val immutable-val
                   :lock-style :user
                   :editing? editing?
                   :on-change on-change})))))

        (when (seq available-cols)
          ($ :div {:class "mt-4"}
            ($ :div {:class "flex items-center justify-between mb-2"}
              ($ :h4 {:class "text-sm font-semibold"} "Columns")
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class "ds-badge ds-badge-info ds-badge-sm"}
                  (str (count policy-col-defaults) " defaults"))
                ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
                  (str (count policy-col-locks) " locks"))
                (when (seq enforced-cols)
                  ($ :span {:class "ds-badge ds-badge-ghost ds-badge-sm"}
                    (str (count enforced-cols) " enforced")))))

            (when (seq enforced-cols)
              ($ :div {:class "mb-3 text-xs text-base-content/60"}
                "Some columns are marked as "
                ($ :span {:class "font-semibold"} "always visible")
                " in "
                ($ :span {:class "font-mono"} "table-columns.edn")
                ". They are enforced and cannot be changed here."))

            ($ :div {:class "grid grid-cols-1 gap-2"}
              (when (and editing? (seq policy-cols) (fn? on-column-visibility-bulk))
                (let [bulk-default (uniform-or-mixed
                                     (map (fn [c]
                                            (if (contains? col-defaults c) (get col-defaults c) nil))
                                       policy-cols))
                      bulk-lock (uniform-or-mixed
                                  (map (fn [c]
                                         (if (contains? col-locks c) (get col-locks c) nil))
                                    policy-cols))]
                  ($ bulk-tristate-row
                    {:label "All columns"
                     :default-val bulk-default
                     :lock-val bulk-lock
                     :editing? true
                     :lock-style :user
                     :help-text "Apply Default/Lock visibility to all configurable columns (always-visible columns are excluded)."
                     :on-default-click (fn []
                                        ;; cycle the current aggregate state
                                        (let [current (if (= bulk-default :mixed) nil bulk-default)
                                              next-val (next-tristate current)
                                              next-state (if (nil? next-val)
                                                          {:kind :inherit}
                                                          {:kind :default :value next-val})]
                                          (on-column-visibility-bulk entity-kw policy-cols next-state)))
                     :on-lock-click (fn []
                                     ;; cycle the current aggregate lock state
                                     (let [current (if (= bulk-lock :mixed) nil bulk-lock)
                                           next-val (next-tristate current)
                                           next-state (if (nil? next-val)
                                                       {:kind :inherit}
                                                       {:kind :lock :value next-val})]
                                       (on-column-visibility-bulk entity-kw policy-cols next-state)))})))
              (when (seq enforced-cols)
                (for [col enforced-cols
                      :when col]
                  (let [label (or (get-in col-metadata [col :label])
                                (-> col name (str/replace #"[_-]" " ") str/capitalize))
                        tip (str "This column is always visible (enforced by table-columns.edn).")]
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
                  ($ column-visibility-row
                    {:key (str (name entity-kw) "-col-" (name col))
                     :entity-kw entity-kw
                     :column-key col
                     :column-label label
                     :default-val default-val
                     :lock-val lock-val
                     :lock-style :user
                     :editing? editing?
                     :help-text tip
                     :on-change on-column-change}))))))))))

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
                      ($ admin-entity-settings-card
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
    ($ admin-entity-settings-card
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
      ($ user-entity-settings-card
        {:entity-kw entity-kw
         :draft-defaults draft-defaults
         :draft-locks draft-locks
         :immutable-locks {}  ; TODO: get from entity config features
         :on-change on-change
         :on-reset on-reset
         :setting-keys defs/all-setting-keys}))))
