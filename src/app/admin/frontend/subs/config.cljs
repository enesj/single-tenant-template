(ns app.admin.frontend.subs.config
  "Simplified subscriptions for vector-based column configuration"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [clojure.string :as str]
    [re-frame.core :as rf]))

;; =============================================================================
;; Core Column Configuration (Vector-based with Order Preservation)
;; =============================================================================

;; Get the entire config for an entity
(rf/reg-sub
  ::entity-config
  (fn [db [_ entity-name]]
    (get-in db [:admin :config :table-columns entity-name])))

;; Get visible columns as a vector (maintains order!)
(rf/reg-sub
  ::visible-columns
  (fn [db [_ entity-name]]
    (or (get-in db [:admin :config :table-columns entity-name :visible-columns])
       ;; Fallback to default if not set
      (get-in db [:admin :config :table-columns entity-name :default-visible-columns])
      [])))

;; Check if a specific column is visible
(rf/reg-sub
  ::column-visible?
  (fn [[_ entity-name _column-name]]
    (rf/subscribe [::visible-columns entity-name]))
  (fn [visible-columns [_ _ column-name]]
    (some #{column-name} visible-columns)))

;; Get all columns for an entity (ordered)
(rf/reg-sub
  ::all-columns
  (fn [db [_ entity-name]]
    (vec (get-in db [:admin :config :table-columns entity-name :available-columns] []))))

;; Get default visible columns (as vector)
(rf/reg-sub
  ::default-visible-columns
  (fn [db [_ entity-name]]
    (get-in db [:admin :config :table-columns entity-name :default-visible-columns] [])))

;; =============================================================================
;; Column Metadata and Labels
;; =============================================================================

;; Get column metadata (labels, types, etc.)
(rf/reg-sub
  ::column-metadata
  (fn [db [_ entity-name column-name]]
    (get-in db [:admin :config :table-columns entity-name :column-metadata column-name])))

;; Get formatted column label
(rf/reg-sub
  ::column-label
  (fn [[_ entity-name column-name]]
    (rf/subscribe [::column-metadata entity-name column-name]))
  (fn [metadata [_ _ column-name]]
    (or (:label metadata)
       ;; Auto-format column name if no label specified
      (-> column-name
        name
        (str/replace #"[_-]" " ")
        str/capitalize))))

;; Check if column visibility has been customized
(rf/reg-sub
  ::columns-customized?
  (fn [[_ entity-name]]
    [(rf/subscribe [::visible-columns entity-name])
     (rf/subscribe [::default-visible-columns entity-name])])
  (fn [[visible default] _]
    (not= visible default)))

;; =============================================================================
;; Legacy Compatibility (for existing components)
;; =============================================================================

;; Backward compatibility with old naming
(rf/reg-sub
  :admin/visible-columns
  (fn [[_ entity-keyword]]
    (rf/subscribe [::visible-columns entity-keyword]))
  (fn [visible-columns _]
    visible-columns))

;; Backward compatibility for table/config entity
(rf/reg-sub
  :admin/table-config
  (fn [[_ entity-keyword]]
    (rf/subscribe [::entity-config entity-keyword]))
  (fn [entity-config _]
    entity-config))

;; Admin entity metadata comes from the entity registry (preloaded from entities.edn)
(rf/reg-sub
  :admin/all-entity-configs
  (fn [_ _]
    (try
      @entity-registry/registered-entities
      (catch :default _
        {}))))

(rf/reg-sub
  :admin/entity-config
  (fn [[_ _entity-keyword]]
    (rf/subscribe [:admin/all-entity-configs]))
  (fn [all-configs [_ entity-keyword]]
    (get all-configs entity-keyword)))

;; =============================================================================
;; Column Statistics and Utilities
;; =============================================================================

;; Count visible columns
(rf/reg-sub
  ::visible-column-count
  (fn [[_ entity-name]]
    (rf/subscribe [::visible-columns entity-name]))
  (fn [visible-columns _]
    (count visible-columns)))

;; Count hidden columns
(rf/reg-sub
  ::hidden-column-count
  (fn [[_ entity-name]]
    [(rf/subscribe [::all-columns entity-name])
     (rf/subscribe [::visible-columns entity-name])])
  (fn [[all-columns visible-columns] _]
    (- (count all-columns) (count visible-columns))))

;; =============================================================================
;; Configuration Loading State
;; =============================================================================

(rf/reg-sub
  :admin/config-loaded?
  (fn [db _]
    (boolean (:admin/config-loaded? db))))

(rf/reg-sub
  :admin/config-loading?
  (fn [db _]
    (boolean (:admin/config-loading? db))))

;; =============================================================================
;; Entity Specs (Simplified)
;; =============================================================================

;; Generate entity specs dynamically from column config
(rf/reg-sub
  ::entity-spec
  (fn [[_ entity-name]]
    [(rf/subscribe [::all-columns entity-name])
     (rf/subscribe [::entity-config entity-name])])
  (fn [[all-columns entity-config] [_ entity-name]]
    (when (and all-columns entity-config)
      {:id (keyword entity-name)
       :fields (mapv (fn [column-key]
                       {:id column-key
                        :label (or (get-in entity-config [:column-metadata column-key :label])
                                 (-> column-key name (str/replace #"[_-]" " ") str/capitalize))
                        :type (get-in entity-config [:column-metadata column-key :type] :text)})
                 all-columns)
       :vector-config entity-config})))

;; Removed legacy fixed proxies (:entity-specs/<entity>, :form-entity-specs/<entity>).
;; Admin pages should use [:admin/entity-specs-by-name <entity>] which is generated
;; from the vector-config and kept in sync with settings/toggles.

;; =============================================================================
;; Advanced Configuration
;; =============================================================================

(rf/reg-sub
  ::filterable-columns
  (fn [[_ entity-name]]
    (rf/subscribe [::entity-config entity-name]))
  (fn [config _]
    (:filterable-columns config [])))

(rf/reg-sub
  ::sortable-columns
  (fn [[_ entity-name]]
    (rf/subscribe [::entity-config entity-name]))
  (fn [config _]
    (:sortable-columns config [])))

;; Backward compatibility
(rf/reg-sub
  :admin/filterable-columns
  (fn [[_ entity-keyword]]
    (rf/subscribe [::filterable-columns entity-keyword]))
  (fn [filterable-columns _]
    filterable-columns))

(rf/reg-sub
  :admin/sortable-columns
  (fn [[_ entity-keyword]]
    (rf/subscribe [::sortable-columns entity-keyword]))
  (fn [sortable-columns _]
    sortable-columns))

;; =============================================================================
;; View Options / Hardcoded Display Settings
;; =============================================================================

(rf/reg-sub
  :admin/all-view-options
  (fn [_ _]
    ;; View options are stored in config-cache atom, not re-frame db
    (config-loader/get-all-view-options)))

(rf/reg-sub
  :admin/view-options
  (fn [[_ _entity-keyword]]
    (rf/subscribe [:admin/all-view-options]))
  (fn [all-view-options [_ entity-keyword]]
    (get all-view-options entity-keyword)))
