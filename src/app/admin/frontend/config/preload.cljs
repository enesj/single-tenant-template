(ns app.admin.frontend.config.preload
  "Preload critical admin configs so vector-based tables avoid fallback mode while async fetch completes."
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [cljs.reader :as reader]
    [shadow.resource :as resource]))

(defn- transform-inverted-config
  "Transform inverted config format to internal format.
   The inverted format specifies what to HIDE/DISABLE rather than what to SHOW/ENABLE."
  [entity-config]
  (let [available (:available-columns entity-config)]
    (if (or (:default-hidden-columns entity-config)
          (:unfilterable-columns entity-config)
          (:unsortable-columns entity-config))
      ;; New inverted format - transform it
      (let [;; Convert hidden to visible
            visible-columns (if-let [hidden (:default-hidden-columns entity-config)]
                              (vec (remove (set hidden) available))
                              (:default-visible-columns entity-config))

            ;; Convert unfilterable to filterable
            filterable-columns (if-let [unfilterable (:unfilterable-columns entity-config)]
                                 (vec (remove (set unfilterable) available))
                                 (:filterable-columns entity-config))

            ;; Convert unsortable to sortable
            sortable-columns (if-let [unsortable (:unsortable-columns entity-config)]
                               (vec (remove (set unsortable) available))
                               (:sortable-columns entity-config))]

        (-> entity-config
            ;; Remove inverted keys
          (dissoc :default-hidden-columns :unfilterable-columns :unsortable-columns)
            ;; Add standard keys
          (assoc :default-visible-columns visible-columns
            :filterable-columns filterable-columns
            :sortable-columns sortable-columns)))
      ;; Fallback - return as is
      entity-config)))

(defonce ^:private preloaded-table-columns
  (let [resource-content (resource/inline "app/admin/frontend/config/table-columns.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)
            ;; Transform each entity config
            transformed (into {} (map (fn [[k v]]
                                        [k (transform-inverted-config v)])
                                   parsed))]
        transformed))))

(defonce ^:private preloaded-view-options
  (let [resource-content (resource/inline "app/admin/frontend/config/view-options.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)]
        parsed))))

(defonce ^:private preloaded-form-fields
  (let [resource-content (resource/inline "app/admin/frontend/config/form-fields.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)]
        parsed))))

(defonce ^:private preloaded-entities
  (let [resource-content (resource/inline "app/admin/frontend/config/entities.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)]
        (into {}
          (map (fn [[entity-key cfg]]
                 (let [registry-entry (get entity-registry/entity-registry entity-key)
                       registry-init-fn (:init-fn registry-entry)
                       registry-actions (:actions registry-entry)
                       registry-custom-actions (:custom-actions registry-entry)
                       registry-modals (:modals registry-entry)]
                   [entity-key
                    (cond-> cfg
                      registry-init-fn (assoc :adapter-init-fn registry-init-fn)
                      (or registry-actions registry-custom-actions registry-modals)
                      (update :components
                        (fn [components]
                          (let [components (or components {})]
                            (cond-> components
                              registry-actions (assoc :actions registry-actions)
                              registry-custom-actions (assoc :custom-actions registry-custom-actions)
                              registry-modals (assoc :modals registry-modals))))))]))
            parsed))))))

(doseq [[source config-type]
        [[preloaded-table-columns :table-columns]
         [preloaded-view-options :view-options]
         [preloaded-form-fields :form-fields]]]
  (doseq [[entity-key entity-config] source]
    ;; Register all preloaded configs, not just :users
    (config-loader/register-preloaded-config! config-type entity-key entity-config)))

(when preloaded-entities
  (entity-registry/register-entities! preloaded-entities))
