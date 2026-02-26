(ns app.shared.specs.entities
  "Malli specs for entities configuration files.

   Used by:
   - src/app/domain/frontend/expenses/config/entities.edn (user-facing domain list pages)
   - src/app/admin/frontend/config/entities.edn (admin entity registry preload)

   These two files have different shapes, so we provide separate schemas and
   validators for each."
  (:require
    [app.shared.specs.view-options :as view-options]
    [malli.core :as m]
    [malli.error :as me]))

;; =============================================================================
;; Common
;; =============================================================================

(def EntityKey :keyword)

(defn- validate*
  [schema data]
  (if (m/validate schema data)
    {:valid? true :data data}
    {:valid? false
     :errors (me/humanize (m/explain schema data))}))

;; =============================================================================
;; User-facing domain entities.edn
;; =============================================================================

(def UserEntityConfig
  "Currently used for simple per-entity metadata like a page title.

  Keep this schema open to allow future domain-specific extensions."
  [:map {:closed false}
   [:title {:optional true} :string]])

(def UserEntitiesFile
  "Top-level map of entity keyword -> config map."
  [:map-of EntityKey UserEntityConfig])

(defn validate-user-entities
  "Validate user-facing entities.edn data.

  Returns:
  - {:valid? true :data data} on success
  - {:valid? false :errors [...]} on failure"
  [data]
  (validate* UserEntitiesFile data))

;; =============================================================================
;; Admin entities.edn (entity registry preload)
;; =============================================================================

(def AdminDisplaySettings
  "Admin entity :display-settings. Reuses the shared DisplayTogglesMap schema
  (show-*? booleans + :per-page) from specs.view-options — single source of truth."
  view-options/DisplayTogglesMap)

(def AdminFeatures
  "Admin entity :features.

  This is intentionally open - feature keys evolve as capabilities are added."
  [:map {:closed false}
   [:read-only? {:optional true} :boolean]
   [:batch-operations? {:optional true} :boolean]
   [:deletion-constraints? {:optional true} :boolean]
   [:security-wrapper? {:optional true} :boolean]])

(def AdminComponents
  "Admin entity :components.

  Values can be symbols, vars, functions, or vectors of them depending on
  when/where the config is loaded."
  [:map {:closed false}
   [:actions {:optional true} :any]
   [:custom-actions {:optional true} :any]
   [:modals {:optional true} [:vector :any]]
   [:custom-header {:optional true} :any]])

(def AdminEntityConfig
  "Schema for a single admin entity definition."
  [:map {:closed false}
   [:entity-key {:optional true} :keyword]
   [:page-title {:optional true} :string]
   [:page-description {:optional true} :string]
   [:adapter-init-fn {:optional true} :any]
   [:display-settings {:optional true} AdminDisplaySettings]
   [:features {:optional true} AdminFeatures]
   [:components {:optional true} AdminComponents]
   [:custom-content {:optional true} [:map {:closed false}]]])

(def AdminEntitiesFile
  [:map-of EntityKey AdminEntityConfig])

(defn- check-admin-entity-key-consistency
  "Ensure that if an entry includes :entity-key, it matches the top-level key.

  Returns nil if OK, otherwise returns a seq of problems."
  [data]
  (let [problems (for [[k cfg] data
                       :let [declared (:entity-key cfg)]
                       :when (and (some? declared) (not= k declared))]
                   {:entity k
                    :problem (str ":entity-key mismatch: expected " k " but got " declared)
                    :fix "Update :entity-key to match the top-level map key."})]
    (when (seq problems)
      problems)))

(defn validate-admin-entities
  "Validate admin entities.edn data.

  Returns:
  - {:valid? true :data data} on success
  - {:valid? false :errors [...]} on failure"
  [data]
  (validate* AdminEntitiesFile data))

(defn validate-admin-entities-strict
  "Validate admin entities.edn with additional consistency checks."
  [data]
  (let [schema-result (validate-admin-entities data)
        consistency (check-admin-entity-key-consistency data)]
    (cond
      (not (:valid? schema-result))
      schema-result

      (seq consistency)
      {:valid? false
       :errors [(str "Admin entities.edn consistency issues: " consistency)]
       :warnings consistency
       :data data}

      :else
      schema-result)))


