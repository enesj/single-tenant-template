(ns app.admin.frontend.pages.user-settings
  "Admin page for editing domain-owned, user-facing UI defaults.

  These settings are seeded from the domain config EDN files and persisted via
  the admin API for use by the non-admin (user-facing) routes."
  {:clj-kondo/ignore [:unused-namespace :unused-referred-var :unused-private-var]}
  (:require
    [app.admin.frontend.components.tabs :as tabs]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.template.frontend.settings.resolver :as resolver]
    [app.template.frontend.utils.timestamp :as timestamp]
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
   :show-highlights?])

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








