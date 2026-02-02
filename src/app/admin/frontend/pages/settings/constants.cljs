(ns app.admin.frontend.pages.settings.constants
  (:require
    [clojure.string :as str]
    ;; Domain registry for domain-specific groups
    [app.domain.frontend.registry :as domain-registry]))

;; Display settings that can be hardcoded in view-options.edn
(def display-setting-keys
  [:show-edit?
   :show-delete?
   :show-select?
   :show-filtering?
   :show-pagination?
   :show-highlights?])

(def action-setting-keys
  [:show-add-button?
   :show-batch-edit?
   :show-batch-delete?])

(def all-setting-keys
  (into display-setting-keys action-setting-keys))

;; Domain organization for entities
(def domain-groups
  "Domain organization for entities.
   Template/admin groups are defined here; domain-specific groups are merged from registry."
  (merge
    ;; Template/admin groups (core infrastructure)
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
      :color "secondary"}}
    ;; Domain-specific groups from registry
    (domain-registry/all-admin-domain-groups)))

(defn setting-label
  "Convert a setting key to a human-readable label"
  [setting-key]
  (-> setting-key
    name
    (str/replace #"\?" "")
    (str/replace #"-" " ")
    str/capitalize))

(defn get-entity-domain
  "Find which domain an entity belongs to"
  [entity-key]
  (some (fn [[domain-key domain-config]]
          (when (contains? (:entities domain-config) entity-key)
            domain-key))
    domain-groups))

(defn group-entities-by-domain
  "Group entities by their domains"
  [sorted-entities]
  (reduce (fn [acc [entity-key settings]]
            (if-let [domain-key (get-entity-domain entity-key)]
              (update acc domain-key conj [entity-key settings])
              (update acc :other conj [entity-key settings])))
    {}
    sorted-entities))

