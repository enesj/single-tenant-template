(ns app.admin.frontend.settings.definitions
  "Shared settings definitions registry for admin and user settings pages.
   
   This namespace provides a single source of truth for:
   - All supported display toggle keys
   - All supported action setting keys  
   - Domain groupings for entities (System, Domain, Other)
   - Helper functions for setting labels and entity titles"
  (:require
    [clojure.string :as str]
    ;; Domain registry for domain-specific groups
    [app.domain.frontend.registry :as domain-registry]))

;; =============================================================================
;; Setting Keys - the full set of supported settings
;; =============================================================================

(def display-setting-keys
  "Display settings that can be configured in view-options.
   These control visibility of UI elements in list views."
  [:show-edit?
   :show-delete?
   :show-select?
   :show-filtering?
   :show-pagination?
   :show-highlights?
   :show-timestamps?])

(def action-setting-keys
  "Action settings that can be configured in view-options.
   These control availability of action buttons in list views."
  [:show-add-button?
   :show-batch-edit?
   :show-batch-delete?])

(def per-page-options
  "Available options for rows per page setting."
  [5 10 20 25 50 100])

(def default-per-page
  "Default rows per page value."
  25)

(def all-setting-keys
  "Combined list of all display and action setting keys."
  (into display-setting-keys action-setting-keys))

;; =============================================================================
;; Setting Definitions - metadata for each setting
;; =============================================================================

(def setting-definitions
  "Map of setting key to definition with label, tooltip help text, and default value."
  {:show-edit?        {:label "Edit"
                       :help "When enabled, shows an Edit button on each row allowing users to modify the record. When disabled or locked off, records are read-only in the list view."
                       :default true
                       :type :toggle}
   :show-delete?      {:label "Delete"
                       :help "When enabled, shows a Delete button on each row allowing users to remove the record. When disabled, records cannot be deleted from the list view."
                       :default true
                       :type :toggle}
   :show-select?      {:label "Selection"
                       :help "When enabled, shows checkboxes on each row for selecting multiple records. Used for batch operations like bulk delete or export."
                       :default true
                       :type :toggle}
   :show-filtering?   {:label "Filtering"
                       :help "When enabled, shows filter controls above the table allowing users to filter data by column values. Supports text search and dropdown filters."
                       :default true
                       :type :toggle}
   :show-pagination?  {:label "Pagination"
                       :help "When enabled, shows pagination controls (page numbers, prev/next) below the table. When disabled, all records are shown on a single page."
                       :default true
                       :type :toggle}
   :show-highlights?  {:label "Highlights"
                       :help "When enabled, rows can be visually highlighted based on certain conditions (e.g., new records, important items). Adds color coding to the table."
                       :default true
                       :type :toggle}
   :show-timestamps?  {:label "Timestamps"
                       :help "When enabled, shows 'Created At' and 'Updated At' timestamp columns in the table. Useful for auditing when records were modified."
                       :default true
                       :type :toggle}
   :show-add-button?  {:label "Add Button"
                       :help "When enabled, shows an 'Add' or 'New' button in the table header allowing users to create new records. When disabled, new records cannot be created from this view."
                       :default true
                       :type :toggle}
   :show-batch-edit?  {:label "Batch Edit"
                       :help "When enabled, allows editing multiple selected records at once. Requires row selection to be enabled. Useful for bulk updates."
                       :default true
                       :type :toggle}
   :show-batch-delete? {:label "Batch Delete"
                        :help "When enabled, allows deleting multiple selected records at once. Requires row selection to be enabled. Shows a 'Delete Selected' action."
                        :default true
                        :type :toggle}
   :per-page          {:label "Rows per page"
                       :help "Default number of rows to display per page in the list view. Users can change this at runtime, but this sets the initial value."
                       :default 25
                       :type :select
                       :options [5 10 20 25 50 100]}})

;; =============================================================================
;; Domain Groups - organization of entities by functional area
;; =============================================================================

(def admin-domain-groups
  "Domain groupings for ADMIN settings scope.
   Template/admin groups are defined here; domain-specific groups are merged from registry."
  (merge
    ;; Template/admin groups (core infrastructure)
    {:user-management
     {:title "User Management"
      :description "Manage users and administrators"
      :icon "👥"
      :entities #{:users :admins}
      :color "primary"
      :scope :admin}

     :security-audit
     {:title "Security & Audit"
      :description "System audit trail and security monitoring"
      :icon "🔒"
      :entities #{:audit-logs :login-events}
      :color "secondary"
      :scope :admin}}
    ;; Domain-specific admin groups from registry
    (domain-registry/all-admin-domain-groups)))

(def user-domain-groups
  "Domain groupings for USER settings scope.
   Domain-specific user groups are loaded from registry."
  (domain-registry/all-user-domain-groups))

(def all-domain-groups
  "Combined domain groups for both scopes."
  (merge admin-domain-groups user-domain-groups))

;; =============================================================================
;; Scope Definitions
;; =============================================================================

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn setting-label
  "Get human-readable label for a setting key.
   Uses definition if available, otherwise converts key to title case."
  [setting-key]
  (or (get-in setting-definitions [setting-key :label])
    (-> setting-key
      name
      (str/replace #"\?" "")
      (str/replace #"-" " ")
      str/capitalize)))

(defn setting-help
  "Get help text for a setting key."
  [setting-key]
  (get-in setting-definitions [setting-key :help]))

(defn entity-title
  "Get human-readable title for an entity keyword."
  [entity-kw]
  (when entity-kw
    (-> entity-kw
      name
      (str/replace #"-" " ")
      str/capitalize)))

(defn get-entity-domain
  "Find which domain an entity belongs to.
   Returns the domain key or nil if not found."
  [entity-kw]
  (some (fn [[domain-key domain-config]]
          (when (contains? (:entities domain-config) entity-kw)
            domain-key))
    all-domain-groups))

(defn group-entities-by-domain
  "Group a collection of entity entries by their domain.
   Input: seq of [entity-key settings] or seq of entity keywords
   Output: map of {domain-key -> [[entity-key settings] ...]} or {domain-key -> [entity-key ...]}"
  [entities]
  (let [is-tuple? (and (sequential? (first entities))
                    (= 2 (count (first entities))))]
    (reduce (fn [acc entity-or-tuple]
              (let [entity-key (if is-tuple? (first entity-or-tuple) entity-or-tuple)]
                (if-let [domain-key (get-entity-domain entity-key)]
                  (update acc domain-key (fnil conj []) entity-or-tuple)
                  (update acc :other (fnil conj []) entity-or-tuple))))
      {}
      entities)))

(defn entities-for-scope
  "Get all entity keywords for a given scope (:admin or :user)."
  [scope]
  (let [domain-groups (case scope
                        :admin admin-domain-groups
                        :user user-domain-groups
                        {})]
    (->> domain-groups
      vals
      (mapcat :entities)
      set)))

(defn domain-groups-for-scope
  "Get domain groups for a given scope."
  [scope]
  (case scope
    :admin admin-domain-groups
    :user user-domain-groups
    {}))

(defn domain-color-classes
  "Get CSS classes for a domain's color scheme."
  [domain-color]
  (case domain-color
    "primary" "from-primary/10 to-primary/5 border-primary/20"
    "secondary" "from-secondary/10 to-secondary/5 border-secondary/20"
    "accent" "from-accent/10 to-accent/5 border-accent/20"
    "from-neutral/10 to-neutral/5 border-neutral/20"))
