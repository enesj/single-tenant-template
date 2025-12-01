(ns app.admin.frontend.utils.compatibility-bridge
  "Compatibility bridge for transitioning from hardcoded customizations to vector config"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.utils.vector-config :as vector-config]
    [taoensso.timbre :as log]))

;; Legacy customization map handling (simplified)
(def legacy-hardcoded-customizations
  "Hardcoded customizations that should be migrated to vector config"
  {:audit-logs {:action {:display-order 1 :width "120px"}
                :entity-type {:display-order 2 :width "100px"}
                :entity-name {:display-order 3 :width "200px"}}
   :users {:email {:display-order 1 :width "200px"}
           :first-name {:display-order 2 :width "120px"}
           :last-name {:display-order 3 :width "120px"}
           :status {:display-order 4 :width "100px"}}})

(defn convert-legacy-customizations-to-vector
  "Convert legacy display-order customizations to vector format"
  [legacy-customizations]
  (let [ordered-fields (->> legacy-customizations
                         (sort-by #(:display-order (second %)))
                         (mapv first))]
    {:default-visible-columns ordered-fields
     :available-columns ordered-fields
     :column-config (into {} (map (fn [[field-key field-config]]
                                    [field-key (dissoc field-config :display-order)])
                               legacy-customizations))}))

(defn migrate-legacy-entity-customizations
  "Migrate legacy hardcoded customizations to vector config"
  [entity-keyword]
  (when-let [legacy-config (get legacy-hardcoded-customizations entity-keyword)]
    (let [vector-config (convert-legacy-customizations-to-vector legacy-config)]
      (log/info "Migrated legacy customizations for" entity-keyword)
      vector-config)))

(defn get-merged-config
  "Get configuration with legacy migration and vector config merged"
  [entity-keyword]
  (let [vector-config (config-loader/get-table-config entity-keyword)
        legacy-config (migrate-legacy-entity-customizations entity-keyword)]
    (or vector-config legacy-config)))

;; Simplified merge logic replacing complex normalization
(defn simple-column-merge
  "Simple column configuration merge without complex snake_case/kebab-case handling"
  [base-config user-overrides]
  (merge base-config user-overrides))

(defn resolve-effective-config
  "Resolve effective configuration through simple three-layer merge"
  [entity-keyword admin-id]
  (let [default-config (get-merged-config entity-keyword)
        user-prefs (vector-config/get-user-preferences entity-keyword admin-id)
        effective-config (if (and default-config user-prefs)
                           (vector-config/merge-config-with-overrides
                             (:default-visible-columns default-config)
                             (:visible-columns user-prefs)
                             (:available-columns default-config))
                           (:default-visible-columns default-config []))]
    {:visible-columns effective-config
     :available-columns (:available-columns default-config [])
     :column-config (:column-config default-config {})
     :computed-fields (:computed-fields default-config {})}))

;; Phase out complex normalization functions
(defn ^:deprecated normalize-field-visibility
  "Deprecated: Use vector-config utilities instead"
  [field-map]
  (log/warn "normalize-field-visibility is deprecated, use vector-config utilities")
  field-map)

(defn ^:deprecated complex-merge-customizations
  "Deprecated: Use simple-column-merge instead"
  [& args]
  (log/warn "complex-merge-customizations is deprecated, use simple-column-merge")
  (apply merge args))
