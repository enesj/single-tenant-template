(ns app.admin.frontend.components.settings-views.rows
  (:require
    [app.admin.frontend.components.settings-views.utils :as utils]
    [app.admin.frontend.settings.definitions :as defs]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

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
        {:keys [class text]} (utils/default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (utils/lock-badge-props {:lock-val lock-val :lock-style lock-style})
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
            (utils/tristate-hint {:kind :default :current-val default-val})))

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
            (utils/tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style})))))))

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
        next-default (utils/next-tristate default-val)
        next-lock (utils/next-tristate lock-val)
        default-next-state (if (nil? next-default)
                             {:kind :inherit}
                             {:kind :default :value next-default})
        lock-next-state (if (nil? next-lock)
                          {:kind :inherit}
                          {:kind :lock :value next-lock})
        {:keys [class text]} (utils/default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (utils/lock-badge-props {:lock-val lock-val
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
            (utils/tristate-hint {:kind :default :current-val default-val})))

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
              (utils/tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style}))))))))

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
        next-default (utils/next-tristate default-val)
        next-lock (utils/next-tristate lock-val)
        default-next-state (if (nil? next-default)
                             {:kind :inherit}
                             {:kind :default :value next-default})
        lock-next-state (if (nil? next-lock)
                          {:kind :inherit}
                          {:kind :lock :value next-lock})
        {:keys [class text]} (utils/default-badge-props default-val)
        default-class class
        default-text text
        {:keys [class text]} (utils/lock-badge-props {:lock-val lock-val
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
            (utils/tristate-hint {:kind :default :current-val default-val})))

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
              (utils/tristate-hint {:kind :lock :current-val lock-val :lock-style lock-style}))))))))

;; =============================================================================
;; Per-Page Select Setting Row
;; =============================================================================

(defui per-page-setting-row
  "Render a per-page setting row with select dropdown for Default and Lock.

   Props:
   - :entity-kw
   - :default-val (nil or integer)
   - :lock-val (nil or integer)
   - :editing? boolean
   - :lock-style :admin | :user
   - :on-change fn [entity-kw :per-page new-state] where new-state is {:kind :inherit} | {:kind :default :value int} | {:kind :lock :value int}"
  [{:keys [entity-kw default-val lock-val editing? lock-style on-change]}]
  (let [help-text (defs/setting-help :per-page)
        options defs/per-page-options
        lock-style (or lock-style :user)
        editing? (boolean editing?)
        clickable? (and editing? (fn? on-change))]
    ($ :div {:class "ds-tooltip ds-tooltip-top w-full"
             :data-tip help-text}
      ($ :div {:class "flex items-center gap-2 p-2 rounded-lg bg-base-200 w-full"}
        ($ :span {:class "text-sm font-medium min-w-[120px]"}
          (defs/setting-label :per-page))

        ;; Default control
        ($ :div {:class "flex items-center gap-1"}
          ($ :span {:class "text-xs text-base-content/60"} "Default:")
          (if clickable?
            ($ :select
              {:id (str "per-page-default-" (name entity-kw))
               :class "w-20 px-2 py-1 border border-gray-300 rounded text-sm"
               :value (or default-val "")
               :on-change (fn [e]
                            (let [v (.. e -target -value)]
                              (if (= v "")
                                (on-change entity-kw :per-page {:kind :inherit})
                                (on-change entity-kw :per-page {:kind :default :value (js/parseInt v)}))))}
              ($ :option {:value ""} "—")
              (for [opt options]
                ($ :option {:key opt :value opt} opt)))
            ($ :span {:class "ds-badge ds-badge-sm ds-badge-info"}
              (if default-val (str default-val) "—"))))

        ($ :span {:class "mx-1 text-base-content/30"} "|")

        ;; Lock control
        ($ :div {:class "flex items-center gap-1"}
          ($ :span {:class "text-xs text-base-content/60"} "Lock:")
          (if clickable?
            ($ :select
              {:id (str "per-page-lock-" (name entity-kw))
               :class "w-20 px-2 py-1 border border-gray-300 rounded text-sm"
               :value (or lock-val "")
               :on-change (fn [e]
                            (let [v (.. e -target -value)]
                              (if (= v "")
                                (on-change entity-kw :per-page {:kind :inherit})
                                (on-change entity-kw :per-page {:kind :lock :value (js/parseInt v)}))))}
              ($ :option {:value ""} "—")
              (for [opt options]
                ($ :option {:key opt :value opt} opt)))
            ($ :span {:class (str "ds-badge ds-badge-sm "
                               (if lock-val
                                 (if (= lock-style :admin) "ds-badge-warning" "ds-badge-primary")
                                 "ds-badge-ghost"))}
              (if lock-val (str "🔒 " lock-val) "—"))))))))

