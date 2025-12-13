(ns app.admin.frontend.pages.user-settings
  "Admin page for editing domain-owned, user-facing UI defaults.

  These settings are stored in `src/app/domain/frontend/expenses/config/*` via
  the admin API and are used by the non-admin (user-facing) routes."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.template.frontend.settings.resolver :as resolver]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

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

(def ^:private domain-groups
  {:expenses {:title "Expenses"
              :icon "💰"
              :entities #{:expenses}}})

(defn- setting-label [setting-key]
  (-> setting-key
    name
    (str/replace #"\?" "")
    (str/replace #"-" " ")
    str/capitalize))

(defn- entity-title [entity-kw draft]
  (or (get-in draft [:entities entity-kw :title])
    (-> entity-kw name (str/replace #"-" " ") str/capitalize)))

(defn- get-entity-domain [entity-kw]
  (some (fn [[domain-key {:keys [entities]}]]
          (when (contains? entities entity-kw)
            domain-key))
    domain-groups))

(defn- group-entities [entities]
  (reduce (fn [acc entity-kw]
            (if-let [domain (get-entity-domain entity-kw)]
              (update acc domain (fnil conj []) entity-kw)
              acc))
    {}
    entities))

(defn- next-setting-state
  "Cycle: inherit → default true → default false → lock true → lock false → inherit"
  [{:keys [kind value]}]
  (case kind
    :inherit {:kind :default :value true}
    :default (if (true? value)
               {:kind :default :value false}
               {:kind :lock :value true})
    :lock (if (true? value)
            {:kind :lock :value false}
            {:kind :inherit})
    ;; :immutable or unknown
    {:kind :inherit}))

(defn- state-badge
  [{:keys [kind value]}]
  (case kind
    :immutable
    {:class (str "ds-badge ds-badge-sm "
              (if (true? value) "ds-badge-success" "ds-badge-error"))
     :text (str "Enforced " (if (true? value) "On" "Off"))}

    :lock
    {:class (str "ds-badge ds-badge-sm "
              (if (true? value) "ds-badge-success" "ds-badge-error"))
     :text (str "Locked " (if (true? value) "On" "Off"))}

    :default
    {:class (str "ds-badge ds-badge-sm "
              (if (true? value) "ds-badge-success" "ds-badge-error"))
     :text (str "Default " (if (true? value) "On" "Off"))}

    ;; inherit
    {:class "ds-badge ds-badge-ghost ds-badge-sm"
     :text "Inherit"}))

(defn- next-state-hint
  [{:keys [kind value]}]
  (case kind
    :lock (str "→ Locked " (if (true? value) "On" "Off"))
    :default (str "→ Default " (if (true? value) "On" "Off"))
    :inherit "→ Inherit"
    ""))

(defui setting-badge
  [{:keys [entity-kw setting-key draft-defaults draft-locks immutable-locks]}]
  (let [immutable? (contains? immutable-locks setting-key)
        immutable-val (get immutable-locks setting-key)

        lock? (contains? draft-locks setting-key)
        lock-val (get draft-locks setting-key)

        default? (contains? draft-defaults setting-key)
        default-val (get draft-defaults setting-key)

        state (cond
                immutable? {:kind :immutable :value immutable-val}
                lock? {:kind :lock :value lock-val}
                default? {:kind :default :value default-val}
                :else {:kind :inherit})

        next-state (when-not immutable?
                     (next-setting-state state))

        {:keys [class text]} (state-badge state)

        click! (fn [e]
                 (.preventDefault e)
                 (when-not immutable?
                   (rf/dispatch [::user-settings-events/set-display-setting-draft
                                 entity-kw
                                 setting-key
                                 next-state])))]
    ($ :button
      {:type "button"
       :class (str "flex items-center gap-2 p-2 rounded-lg w-full text-left bg-base-200 "
                (if immutable?
                  "opacity-60 cursor-not-allowed"
                  "hover:bg-base-300 transition-colors"))
       :disabled immutable?
       :on-click click!}
      ($ :span {:class "text-sm font-medium min-w-[120px]"}
        (setting-label setting-key))
      ($ :span {:class class} text)
      (when-not immutable?
        ($ :span {:class "text-xs text-base-content/50 ml-auto"}
          (next-state-hint next-state))))))

(defui entity-view-options-card
  [{:keys [entity-kw draft]}]
  (let [view-options (get-in draft [:view-options entity-kw])
        entity-config (get-in draft [:entities entity-kw])

        ;; Feature constraints are always enforced and cannot be overridden.
        immutable-locks (resolver/feature-constraints->locks (:features entity-config))

        draft-defaults (or (get view-options :display-defaults) {})
        draft-locks (or (get view-options :display-locks) {})]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} (entity-title entity-kw draft))
          ($ :button {:type "button"
                      :class "ds-btn ds-btn-xs ds-btn-ghost"
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (rf/dispatch [::user-settings-events/reset-entity-display-draft entity-kw]))}
            "Reset"))
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
          (for [setting-key all-setting-keys]
            ($ setting-badge {:key (str (name entity-kw) "-" (name setting-key))
                              :entity-kw entity-kw
                              :setting-key setting-key
                              :draft-defaults draft-defaults
                              :draft-locks draft-locks
                              :immutable-locks immutable-locks})))))))

(defui entity-columns-card
  [{:keys [entity-kw draft table-columns-config]}]
  (let [entity-config (get table-columns-config entity-kw)
        available (vec (:available-columns entity-config))
        hidden (set (:default-hidden-columns entity-config))
        visible (set (remove hidden available))]
    ($ :div {:class "ds-card bg-base-100 shadow-md"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h3 {:class "ds-card-title text-lg"} (entity-title entity-kw draft))
          ($ :button {:type "button"
                      :class "ds-btn ds-btn-xs ds-btn-ghost"
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (rf/dispatch [::user-settings-events/reset-columns-draft entity-kw]))}
            "Reset"))

        (if (seq available)
          ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
            (for [col available]
              (let [checked? (contains? visible col)]
                ($ :label {:key (str (name entity-kw) "-" (name col))
                           :class "flex items-center gap-2 p-2 rounded-lg bg-base-200"}
                  ($ :input {:type "checkbox"
                             :class "ds-checkbox ds-checkbox-sm"
                             :checked checked?
                             :on-change (fn [e]
                                          (.preventDefault e)
                                          (rf/dispatch [::user-settings-events/toggle-column-visibility-draft
                                                        entity-kw
                                                        col]))})
                  ($ :span {:class "text-sm"}
                    (name col))))))
          ($ :p {:class "text-sm text-base-content/60"}
            "No table column config available for this entity."))))))

(defui user-settings-content
  []
  (let [draft (use-subscribe [::user-settings-events/draft])
        dirty? (boolean (use-subscribe [::user-settings-events/dirty?]))
        tab (use-subscribe [::user-settings-events/tab])
        loading? (boolean (use-subscribe [::user-settings-events/loading?]))
        saving? (boolean (use-subscribe [::user-settings-events/saving?]))
        error (use-subscribe [::user-settings-events/error])
        last-saved (use-subscribe [::user-settings-events/last-saved])
        table-columns-config (use-subscribe [::user-settings-events/table-columns-config])
        entities (->> (keys table-columns-config)
                   (filter keyword?)
                   sort
                   vec)
        grouped (group-entities entities)]

    (use-effect
      (fn []
        (rf/dispatch [::user-settings-events/init])
        js/undefined)
      [])

    ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
      ;; Header
      ($ :div {:class "px-4 sm:px-6 lg:px-8 mb-6"}
        ($ :div {:class "flex items-start justify-between gap-4"}
          ($ :div
            ($ :h1 {:class "text-2xl font-bold text-base-content"} "User Settings")
            ($ :p {:class "text-base-content/70"}
              "Edit user-facing defaults for the Expenses pages. These settings are saved to the domain config files."))

          ($ :div {:class "flex items-center gap-2"}
            ($ :button {:type "button"
                        :class (str "ds-btn ds-btn-sm ds-btn-ghost"
                                 (when (or (not dirty?) saving? loading?) " ds-btn-disabled"))
                        :disabled (or (not dirty?) saving? loading?)
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (rf/dispatch [::user-settings-events/discard-draft]))}
              "Discard")
            ($ :button {:type "button"
                        :class (str "ds-btn ds-btn-sm ds-btn-primary"
                                 (when (or (not dirty?) saving? loading?) " ds-btn-disabled"))
                        :disabled (or (not dirty?) saving? loading?)
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (rf/dispatch [::user-settings-events/save]))}
              (if saving? "Saving..." "Save")))))

      ;; Tabs + content
      ($ :div {:class "px-4 sm:px-6 lg:px-8"}
        ($ :div {:class "ds-tabs ds-tabs-boxed mb-6"}
          (tabs/tab-link {:label "📋 View options"
                          :active? (= tab "view-options")
                          :on-select #(rf/dispatch [::user-settings-events/set-tab "view-options"])})
          (tabs/tab-link {:label "📊 Table columns"
                          :active? (= tab "table-columns")
                          :on-select #(rf/dispatch [::user-settings-events/set-tab "table-columns"])})

          (when last-saved
            ($ :div {:class "ml-auto text-xs text-base-content/60 self-center"}
              (str "Saved " (js/Date. last-saved)))))

        (when loading?
          ($ :div {:class "ds-alert ds-alert-info mb-6"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
            ($ :span "Loading user UI config...")))

        (when error
          ($ :div {:class "ds-alert ds-alert-error mb-6"}
            ($ :span error)))

        (when (and (not loading?) (empty? entities))
          ($ :div {:class "ds-alert ds-alert-warning"}
            ($ :span "No configurable entities found for user UI defaults.")))

        (when (seq entities)
          (case tab
            "table-columns"
            ($ :div {:class "space-y-8"}
              (for [[domain-key entity-ks] (sort-by (comp name first) grouped)]
                (let [{:keys [title icon]} (get domain-groups domain-key {:title "Other" :icon "📦"})]
                  ($ :div {:key (name domain-key)}
                    ($ :h2 {:class "text-xl font-bold mb-3"}
                      (str icon " " title))
                    ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
                      (for [entity-kw (sort entity-ks)]
                        ($ entity-columns-card {:key (name entity-kw)
                                                :entity-kw entity-kw
                                                :draft draft
                                                :table-columns-config table-columns-config})))))))

            ;; default: view-options
            ($ :div {:class "space-y-8"}
              (for [[domain-key entity-ks] (sort-by (comp name first) grouped)]
                (let [{:keys [title icon]} (get domain-groups domain-key {:title "Other" :icon "📦"})]
                  ($ :div {:key (name domain-key)}
                    ($ :h2 {:class "text-xl font-bold mb-3"}
                      (str icon " " title))
                    ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4"}
                      (for [entity-kw (sort entity-ks)]
                        ($ entity-view-options-card {:key (name entity-kw)
                                                     :entity-kw entity-kw
                                                     :draft draft})))))))))))))

(defui admin-user-settings-page
  []
  ($ layout/admin-layout
    {:children ($ user-settings-content)}))
