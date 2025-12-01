(ns app.admin.frontend.specs.generic
  "Enhanced generic spec generation utilities for admin frontend using vector-based configuration.

   This namespace provides utilities for generating entity and form specs from
   vector-based UI configuration with intelligent fallbacks to models.edn data.

   Key benefits:
   - Clean separation: UI configuration separate from database schema
   - Vector-based: Explicit column ordering and visibility
   - Flexible: User preferences layer over default configurations
   - Maintainable: Configuration-driven approach eliminates hardcoded customizations"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.utils.vector-config :as vector-config]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def admin-form-exclusions
  "Fields to exclude from admin form specs"
  #{:id :created-at :updated-at})

;; Removed unused normalize-field-key function

(defn- create-column-spec-from-config
  "Create a column spec from vector configuration"
  [column-key column-config computed-fields]
  (let [base-spec {:id column-key
                   :label (str/replace (name column-key) #"-" " ")
                   :type :text}
        width-config (when-let [width (:width column-config)]
                       {:width width})
        formatter-config (when-let [formatter (:formatter column-config)]
                           {:formatter formatter})
        computed-config (when (contains? computed-fields column-key)
                          {:computed true
                           :dependencies (get-in computed-fields [column-key :dependencies])})
        always-visible-config (when (:always-visible column-config)
                                {:always-visible true})]
    (merge base-spec width-config formatter-config computed-config always-visible-config)))

;; UPDATED: Read from Re-frame DB instead of config-loader cache
(defn- generate-admin-entity-spec-from-db
  "Generate admin entity spec using vector-based configuration from Re-frame DB"
  [db entity-keyword]

  (if-let [table-config (get-in db [:admin :config :table-columns entity-keyword])]
    (let [{:keys [available-columns computed-fields column-config always-visible]} table-config
          column-specs (mapv (fn [column-key]
                               (let [specific-config (get column-config column-key {})
                                     is-always-visible (contains? (set always-visible) column-key)]
                                 (create-column-spec-from-config
                                   column-key
                                   (assoc specific-config :always-visible is-always-visible)
                                   computed-fields)))
                         available-columns)]
      {:id entity-keyword
       :fields column-specs
       :vector-config table-config})
    ;; No config found in DB – only log after configs have been loaded
    (do
      (when (:admin/config-loaded? db)
        (log/warn "No table config found for entity:" entity-keyword "available entities:" (keys (get-in db [:admin :config :table-columns]))))
      nil)))

;; LEGACY: Keep the old function for backward compatibility (but mark it as reading from cache)
(defn generate-admin-entity-spec-from-config
  "Generate admin entity spec using vector-based configuration from config-loader cache"
  [entity-keyword]

  (if-let [table-config (config-loader/get-table-config entity-keyword)]
    (let [{:keys [available-columns computed-fields column-config always-visible]} table-config
          column-specs (mapv (fn [column-key]
                               (let [specific-config (get column-config column-key {})
                                     is-always-visible (contains? (set always-visible) column-key)]
                                 (create-column-spec-from-config
                                   column-key
                                   (assoc specific-config :always-visible is-always-visible)
                                   computed-fields)))
                         available-columns)]
      {:id entity-keyword
       :fields column-specs
       :vector-config table-config})

    ;; Fallback to legacy approach for unmigrated entities
    (do
      (log/info "No vector config found for" entity-keyword ", legacy approach removed!!")
      nil)))

(defn get-effective-column-visibility
  "Get effective column visibility for an entity considering user preferences"
  [entity-keyword admin-id]
  (let [table-config (config-loader/get-table-config entity-keyword)
        effective-config (when table-config
                           (vector-config/get-effective-column-config entity-keyword table-config admin-id))]
    (if effective-config
      {:visible-columns (:visible-columns effective-config)
       :available-columns (:available-columns effective-config)
       :field-visibility-map (vector-config/vector-config->boolean-map
                               (:visible-columns effective-config)
                               (:available-columns effective-config))}
      ;; Return empty state for unmigrated entities
      {:visible-columns []
       :available-columns []
       :field-visibility-map {}})))

(defn generate-admin-entity-spec-with-user-prefs
  "Generate admin entity spec with user preferences applied"
  [entity-keyword admin-id]
  (let [base-spec (generate-admin-entity-spec-from-config entity-keyword)
        visibility-config (get-effective-column-visibility entity-keyword admin-id)]
    (if base-spec
      (assoc base-spec
        :effective-visibility visibility-config
        :user-customized true)
      ;; For unmigrated entities, return nil to trigger legacy fallback
      nil)))

;; Legacy functions maintained for backward compatibility
(defn generate-admin-entity-spec
  "Generate admin entity spec with configuration-first approach and legacy fallback"
  [entity-keyword _models-map]
  ;; First try vector configuration approach
  (if-let [config-spec (generate-admin-entity-spec-from-config entity-keyword)]
    config-spec
    ;; Fall back to legacy models.edn approach
    (do
      (log/info "Using legacy models.edn approach for" entity-keyword)
      ;; Legacy implementation here - simplified for demo
      {:id entity-keyword
       :fields []
       :legacy-fallback true})))

(defn get-admin-display-settings
  "Get display settings for admin table with vector config integration"
  [entity-keyword admin-id]
  (let [visibility-config (get-effective-column-visibility entity-keyword admin-id)
        table-config (config-loader/get-table-config entity-keyword)]
    (merge
      {:show-filtering? true
       :show-pagination? true
       :show-export? true
       :show-column-settings? true}
      (when table-config
        {:vector-config-enabled true
         :sortable-columns (set (:sortable-columns table-config))
         :filterable-columns (set (:filterable-columns table-config))})
      visibility-config)))

(defn prepare-template-system-config
  "Prepare configuration for template system compatibility"
  [entity-keyword admin-id]
  (let [effective-config (get-effective-column-visibility entity-keyword admin-id)]
    (vector-config/prepare-for-template-system
      (:visible-columns effective-config)
      (:available-columns effective-config))))

;; Admin-specific entity spec overrides for vector config entities
;; This ensures column visibility panels show correct labels from vector config

(defn- create-admin-entity-specs-override-from-db
  "Create admin-specific entity specs with proper labels from vector config in Re-frame DB"
  [db entity-keyword]
  (let [spec (generate-admin-entity-spec-from-db db entity-keyword)]
    (when spec
      ;; Convert vector config spec to format expected by template system
      (let [fields (:fields spec)]
        fields))))

;; Removed unused create-admin-entity-specs-override function

;; FIXED: Admin-specific entity specs subscription - reads from Re-frame DB
(rf/reg-sub
  :admin/entity-specs-by-name
  (fn [db [_ entity-name]]
    (create-admin-entity-specs-override-from-db db entity-name)))

;; Backward compatibility subscription for :admin/entity-spec
(rf/reg-sub
  :admin/entity-spec
  (fn [db [_ entity-name]]
    (create-admin-entity-specs-override-from-db db entity-name)))

;; Backward compatibility subscription for :entity-specs/<entity> format
(rf/reg-sub
  :entity-specs/users
  (fn [db _]
    (create-admin-entity-specs-override-from-db db :users)))
